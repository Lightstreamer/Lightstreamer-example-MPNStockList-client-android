/*
 * Copyright 2014 Weswit Srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lightstreamer.demo.android;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.lightstreamer.ls_client.ConnectionInfo;
import com.lightstreamer.ls_client.ConnectionListener;
import com.lightstreamer.ls_client.LSClient;
import com.lightstreamer.ls_client.PushConnException;
import com.lightstreamer.ls_client.PushServerException;
import com.lightstreamer.ls_client.PushUserException;
import com.lightstreamer.ls_client.SubscrException;
import com.lightstreamer.ls_client.SubscribedTableKey;
import com.lightstreamer.ls_client.mpn.MpnInfo;
import com.lightstreamer.ls_client.mpn.MpnStatusInfo;
import com.lightstreamer.ls_client.mpn.MpnSubscription;
import com.lightstreamer.ls_client.mpn.MpnSubscriptionStatus;


public class LightstreamerClient {
    
    public interface StatusChangeListener {
        public void onStatusChange(int status);
    }
    
    private static final String TAG = "LS_CONN";
    private static final String TAG_MPN = "LS_MPN";
    private static final String TAG_SUB = "LS_SUB";
    
    public static final int STALLED = 4;
    public static final int STREAMING = 2;
    public static final int POLLING = 3;
    public static final int DISCONNECTED = 0;
    public static final int CONNECTING = 1;
    public static final int WAITING = 5;
    
    private static String statusToString(int status){
        switch(status) {
        
            case STALLED: {
                return "STALLED";
            }
            case STREAMING: {
                return "STREAMING";
            }
            case POLLING: {
                return "POLLING";
            }
            case DISCONNECTED: {
                return "DISCONNECTED";
            }
            case CONNECTING: {
                return "CONNECTING";
            }
            case WAITING: {
                return "WAITING";
            }
            default: {
                return "Unexpected";
            }
        
        }
    }
    
    private LinkedList<Subscription> subscriptions = new LinkedList<Subscription>();
    
    Map<String,Map<String,MpnSubscription>> mpnCache = new HashMap<String,Map<String,MpnSubscription>>();
    Map<String,Map<String,PendingOp>> mpnPendingCache = new HashMap<String,Map<String,PendingOp>>();
    Map<String,MpnStatusListener> mpnListeners = new HashMap<String,MpnStatusListener>();
    
    
    private AtomicBoolean expectingConnected = new AtomicBoolean(false);
    private AtomicBoolean pmEnabled = new AtomicBoolean(false);
    
    private boolean connected = false; // do not get/set this outside the eventsThread
    private boolean mpnStatusRetrieved = false; // do not get/set this outside the eventsThread
    
    final private ExecutorService eventsThread = Executors.newSingleThreadExecutor();
        //SubscriptionThread ConnectionThread ConnectionEvent retrieveMpnStatus MpnSubscriptionThread enablePN
    
    final private ConnectionInfo cInfo = new ConnectionInfo();
    final private LSClient client = new LSClient();

    private ClientListener currentListener = null;
    
    private StatusChangeListener statusListener;

    private int status;

    private final AtomicInteger connId = new AtomicInteger(0);
    
    public int getStatus() {
        return status;
    }

    private void setStatus(int status, int connId) {
        if (connId != this.connId.get()) {
            //this means that this event is from an old connection
            return;
        }
        
        this.status = status;
        Log.i(TAG,connId + ": " + statusToString(this.status)); 
        if (this.statusListener != null) {
            this.statusListener.onStatusChange(this.status);
        }
    }

    public LightstreamerClient() {
         this.cInfo.adapter = "DEMO";
    }
    
    public void setServer(String pushServerUrl) {
        this.cInfo.pushServerUrl = pushServerUrl;
    }
    
    public void setListener(StatusChangeListener statusListener) {
        this.statusListener = statusListener;
    }
    
    public synchronized void start() {
        Log.d(TAG,"Connection enabled");
        if (expectingConnected.compareAndSet(false,true)) {
            this.startConnectionThread(false);
        }
    }
    
    public synchronized void stop(boolean applyPause) {
        Log.d(TAG,"Connection disabled");
        if (expectingConnected.compareAndSet(true,false)) {
            this.startConnectionThread(applyPause);
        }
    }    
    
    public synchronized void addSubscription(Subscription sub) {
        eventsThread.execute(new SubscriptionThread(sub,true));
    }
    public synchronized void removeSubscription(Subscription sub) {
        eventsThread.execute(new SubscriptionThread(sub,false));
    }
        
    private void startConnectionThread(boolean wait) {
        eventsThread.execute(new ConnectionThread(wait));
    }
    
    //ClientListener calls it through eventsThread
    private void changeStatus(int connId, boolean connected, int status) {
        if (connId != this.connId.get()) {
            //this means that this event is from an old connection
            return;
        }
        
        this.connected = connected;
        
        if (connected != expectingConnected.get()) {
            this.startConnectionThread(false);
        }
    }
    
    private class ConnectionThread implements Runnable { 
        
        boolean wait = false;
        
        public ConnectionThread(boolean wait) {
            this.wait = wait;
        }

        public void run() { //called from the eventsThread
            //expectingConnected can be changed by outside events
            
            if(this.wait) {
                //waits to see if the user/app changes its mind
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                }
            }
            
            while(connected != expectingConnected.get()) { 
                
                if (!connected) {
                    connId.incrementAndGet(); //this is the only increment
                    setStatus(CONNECTING,connId.get());
                    mpnStatusRetrieved = false;
                    try {
                        currentListener = new ClientListener(connId.get());
                        client.openConnection(cInfo, currentListener);
                        Log.d(TAG,"Connecting success");
                        connected = true;
                        
                        resubscribeAll();
                        retrieveAllCurrentMpns();
                        handlePendingMpnOps();
                        
                    } catch (PushServerException e) {
                        Log.d(TAG,"Connection failed: " + e.getErrorCode() + ": " + e.getMessage());
                    } catch (PushUserException e) {
                        Log.d(TAG,"Connection refused: " + e.getErrorCode() + ": " + e.getMessage());
                    } catch (PushConnException e) {
                        Log.d(TAG,"Connection problems: " + e.getMessage());
                    }
                    
                    if (!connected) {
                        try {
                            setStatus(WAITING,connId.get());
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                        }
                    }
                    
                } else {
                    Log.v(TAG,"Disconnecting");
                    client.closeConnection();
                    setStatus(DISCONNECTED,connId.get());
                    currentListener = null;
                    connected = false;
                }
                
            }     
        }
    }
    
    private class ConnectionEvent implements Runnable {

        private final int connId;
        private final boolean connected;
        private final int status;

        public ConnectionEvent(int connId, boolean connected, int status) {
            this.connId = connId;
            this.connected = connected;
            this.status = status;
        }
        
        @Override
        public void run() {
            changeStatus(this.connId, this.connected, this.status);
        }
        
    }
    
    
    private class ClientListener implements ConnectionListener {

        private final int connId;
        private int lastConnectionStatus;

        public ClientListener(int connId) {
            this.connId = connId;
        }
        
        @Override
        public void onActivityWarning(boolean warn) {
            Log.d(TAG,connId + " onActivityWarning " + warn);
            if (warn) {
                setStatus(STALLED,this.connId);
                eventsThread.execute(new ConnectionEvent(this.connId,true,STALLED));
            } else {
                setStatus(this.lastConnectionStatus,this.connId);
                eventsThread.execute(new ConnectionEvent(this.connId,true,this.lastConnectionStatus));
            }
            
        }

        @Override
        public void onClose() {
            Log.d(TAG,connId + " onClose");
            setStatus(DISCONNECTED,this.connId);
            eventsThread.execute(new ConnectionEvent(this.connId,false,DISCONNECTED));
        }

        @Override
        public void onConnectionEstablished() {
            Log.d(TAG,connId + " onConnectionEstablished");
        }

        @Override
        public void onDataError(PushServerException pse) {
            Log.d(TAG,connId + " onDataError: " + pse.getErrorCode() + " -> " + pse.getMessage());
        }

        @Override
        public void onEnd(int cause) {
            Log.d(TAG,connId + " onEnd " + cause);
        }

        @Override
        public void onFailure(PushServerException pse) {
            Log.d(TAG,connId + " onFailure: " + pse.getErrorCode() + " -> " + pse.getMessage());
        }

        @Override
        public void onFailure(PushConnException pce) {
            Log.d(TAG,connId + " onFailure: " + pce.getMessage());
        }

        @Override
        public void onNewBytes(long num) {
            //Log.v(TAG,connId + " onNewBytes " + num);
        }

        @Override
        public void onSessionStarted(boolean isPolling) {
            Log.d(TAG,connId + " onSessionStarted; isPolling: " + isPolling);
            if (isPolling) {
                this.lastConnectionStatus = POLLING;
            } else {
                this.lastConnectionStatus = STREAMING;
            }
            setStatus(this.lastConnectionStatus,this.connId);
            eventsThread.execute(new ConnectionEvent(this.connId,true,this.lastConnectionStatus));
            
            
        }
        
    }
    

    
//Subscription handling    
    
    
    private void resubscribeAll() {
        if (subscriptions.size() == 0) {
            return;
        }
        
        Log.i(TAG_SUB,"Resubscribing " + subscriptions.size() + " subscriptions");
        
        ExecutorService tmpExecutor = Executors.newFixedThreadPool(subscriptions.size());
        
        //start batch
        try {
            client.batchRequests(subscriptions.size());

        } catch (SubscrException e) {
            //connection is closed, exit
            return;
        }
        
        for (Subscription sub : subscriptions) {
            tmpExecutor.execute(new BatchSubscriptionThread(sub));
        }
        
        
        //close batch
        client.closeBatch(); //should be useless
        
        tmpExecutor.shutdown();
        try {
            tmpExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }
    
    
    
    private class SubscriptionThread implements Runnable {

        Subscription sub;
        private boolean add;
        
        public SubscriptionThread(Subscription sub, boolean add) {
            this.sub = sub;
            this.add = add;
        }
        
        @Override
        public void run() {
            
            if (subscriptions.contains(sub)) {
                if (this.add) {
                    //already contained, exit now
                    Log.d(TAG_SUB,"Can't add subscription: Subscription already in: " + sub);
                    return;
                }
                
                Log.i(TAG_SUB,"Removing subscription " + sub);
                subscriptions.remove(sub);
                String key = sub.getTableInfo().getGroup();
                mpnListeners.remove(key);
                
            } else {
                if (!this.add) {
                    //already removed, exit now
                    Log.d(TAG_SUB,"Can't remove subscription: Subscription not in: " + sub);
                    return;
                }
                Log.i(TAG_SUB,"Adding subscription " + sub);
                subscriptions.add(sub);
                
                MpnStatusListener listener = sub.getMpnStatusListener();
                if (listener != null)  {
                    String key = sub.getTableInfo().getGroup();
                    mpnListeners.put(key,listener);
                }
                
            }
            
            
            if (connected && expectingConnected.get()) {
                if (this.add) {
                    doSubscription(sub);
                } else {
                    doUnsubscription(sub);
                }
            }
            
        }
        
    }
    
    private class BatchSubscriptionThread implements Runnable {
        
        Subscription sub;
        
        public BatchSubscriptionThread(Subscription sub) {
            this.sub = sub;
        }
        
        @Override
        public void run() {
            doSubscription(sub);
        }
        
    }
    
    private void doSubscription(Subscription sub) {
        
        Log.d(TAG_SUB,"Subscribing " + sub);
        
        try {
            SubscribedTableKey key = client.subscribeTable(sub.getTableInfo(), sub.getTableListener(), false);
            sub.setTableKey(key);
        } catch (SubscrException e) {
            Log.d(TAG_SUB,"Connection was closed: " + e.getMessage());
        } catch (PushServerException e) {
            Log.d(TAG_SUB,"Subscription failed: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushUserException e) {
            Log.d(TAG_SUB,"Subscription refused: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushConnException e) {
            Log.d(TAG_SUB,"Connection problems: " + e.getMessage());
        }
    }
    private void doUnsubscription(Subscription sub) {
        
        Log.d(TAG_SUB,"Unsubscribing " + sub);
        
        try {
            client.unsubscribeTable(sub.getTableKey());
        } catch (SubscrException e) {
            Log.d(TAG_SUB,"Connection was closed: " + e.getMessage());
        } catch (PushServerException e) {
            Log.wtf(TAG_SUB,"Unsubscription failed: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushConnException e) {
            Log.d(TAG_SUB,"Unubscription failed: " + e.getMessage());
        }
    }
    
//Mpn handling    
    

    public void enablePN(boolean enabled) {
        this.pmEnabled.set(enabled);
        if (enabled) {
            Log.i(TAG_MPN,"Enabling Mpn");
            eventsThread.execute(new Runnable() {
                public void run() {
                    if (connected && expectingConnected.get()) {
                        retrieveAllCurrentMpns();
                        handlePendingMpnOps();
                    }
                }
            });
        } 
    }
    
    public synchronized void retrieveMpnStatus(String key) {
        RetrieveMpnSubscriptionStatusThread statusThread = new RetrieveMpnSubscriptionStatusThread(key);
        eventsThread.execute(statusThread);
    }
    
  //note that the TableInfo group will be used to uniquely identify its MpnInfo
    public synchronized void activateMPN(MpnInfo info) {
        eventsThread.execute(new MpnSubscriptionThread(info,true));
    }

    //note that the TableInfo group will be used to uniquely identify its MpnInfo
    public synchronized void deactivateMPN(MpnInfo info) {
        eventsThread.execute(new MpnSubscriptionThread(info,false));
    }
    
    /*
     * called on session start or when push notifications are enabled on the app:
     * flushes triggered and suspended mpn subscriptions and retrieves the list
     * of active ones. Notifies listeners if any
     */
    private void retrieveAllCurrentMpns() { //from eventsThread 
        if (mpnStatusRetrieved || !pmEnabled.get()) {
            return;
        }
        
        
        
        //deactivate triggered subscriptions
        Log.d(TAG_MPN,"Deactivate triggered mpn subscriptions");
        try {
            this.client.deactivateMpn(MpnSubscriptionStatus.Triggered);
        } catch (SubscrException e) {
            Log.d(TAG_MPN,"Connection problems: " + e.getMessage());
        } catch (PushServerException e) {
            Log.d(TAG_MPN,"Request error: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushUserException e) {
             Log.d(TAG_MPN,"Request refused: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushConnException e) {
            Log.d(TAG_MPN,"Connection problems: " + e.getMessage());
        }
        
        //get remaining subscriptions (since I've just cleared Triggered subscriptions I assume these are all Active)
        Log.d(TAG_MPN,"Retrieving MPN subscription statuses");
        List<MpnSubscription>mpnList = null;
        try {
            mpnList = this.client.inquireAllMpn();
            mpnStatusRetrieved = true;
        } catch (SubscrException e) {
            Log.d(TAG_MPN,"Connection problems: " + e.getMessage());
        } catch (PushServerException e) {
            Log.d(TAG_MPN,"Request error: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushUserException e) {
             if (e.getErrorCode() == 45) {
                 mpnStatusRetrieved = true;
             } else {
                 Log.d(TAG_MPN,"Request refused: " + e.getErrorCode() + ": " + e.getMessage());
             }
        } catch (PushConnException e) {
            Log.d(TAG_MPN,"Connection problems: " + e.getMessage());
        }
            
        //populate active subscriptions cache
        mpnCache.clear();
        if (mpnList != null) {
             for (MpnSubscription mpnSub : mpnList) {
            	 MpnInfo info = mpnSub.getMpnInfo();
                 String key = info.getTableInfo().getGroup();
                 String trigger = info.getTriggerExpression();
                 if (trigger == null) {
                     trigger = "";
                 }
                 
                 addToMpnCache(key,trigger,mpnSub);
                 
                 MpnStatusListener listener = mpnListeners.get(key);
                 if (listener != null) {
                     notifyMpnStatusListener(true,trigger,listener);
                 }
             }
        }
    }
    
    private void addToMpnCache(String key, String trigger, MpnSubscription info) {
        Map<String,MpnSubscription> triggerList = mpnCache.get(key);
        if (triggerList == null) {
            triggerList = new HashMap<String,MpnSubscription>();
            mpnCache.put(key, triggerList);
        }
        triggerList.put(trigger, info);
    }
    
    private void removeFromMpnCache(String key, String trigger) {
        Map<String,MpnSubscription> triggerList = mpnCache.get(key);
        if (triggerList == null) {
            //not in list
            return;
        }
        
        triggerList.remove(trigger);
        
        if (triggerList.isEmpty()) {
            mpnCache.remove(key);
        }
    }
    
    private MpnSubscription getFromMpnCache(MpnInfo info) { 
    	Map<String,MpnSubscription> keyCache = mpnCache.get(info.getTableInfo().getGroup());
        if (keyCache == null) {
            return null;
        }
        return keyCache.get(info.getTriggerExpression());
    }
    
    private void handlePendingMpnOps() { //from eventsThread
        Log.d(TAG_MPN,"Handle pending MPN subscriptions");
        
        Iterator<Entry<String, Map<String, PendingOp>>> pendings = mpnPendingCache.entrySet().iterator();
        while (pendings.hasNext()) {
            
            Map<String, PendingOp> entry = pendings.next().getValue();
            Iterator<Entry<String, PendingOp>> pendingsForKey = entry.entrySet().iterator();
            while (pendingsForKey.hasNext()) {
                
                if (!connected || !expectingConnected.get()) {
                    //check before each request, just in case
                    return;
                }
                
                PendingOp toHandle = pendingsForKey.next().getValue();
                
                try {
                    boolean success = mpnSubscriptionActivation(toHandle);
                    if (success) {
                        pendingsForKey.remove();
                    }
                    
                } catch (SubscrException e) {
                    Log.d(TAG,"Connection problems: " + e.getMessage());
                } catch (PushServerException e) {
                    Log.d(TAG,"Request error: " + e.getErrorCode() + ": " + e.getMessage());
                } catch (PushUserException e) {
                     Log.d(TAG,"Request refused: " + e.getErrorCode() + ": " + e.getMessage());
                } catch (PushConnException e) {
                    Log.d(TAG,"Connection problems: " + e.getMessage());
                }      
                
            }
            
            if (entry.isEmpty()) {
                pendings.remove();
            }
            
            
        }
    }
    
    private void notifyMpnStatusListener(boolean activated, String trigger, MpnStatusListener listener) { 
        listener.onMpnStatusChanged(activated, trigger);
    }
    
    /*
     * checks the cache for key-related mpn subscrioptions then asks the server their status;
     * suspended/triggered are removed. A notification is sent to the listener
     */
    private void retrieveMpnSubscriptionStatus(String key) 
            throws SubscrException, PushServerException, PushUserException, PushConnException { //from eventsThread 
        
        if (!mpnStatusRetrieved || !pmEnabled.get()) {
            return;
        }
        
        Log.d(TAG_MPN,"Checking MPN subscriptions status for " + key);
        
        MpnStatusListener listener = mpnListeners.get(key);
        //if listener is null this makes no much sense, anyway we run it to eventually clear 
        //Suspended/Triggered subscriptions from the local cache
        
        Map <String,MpnSubscription> active = mpnCache.get(key);
        if (active != null) {
            Iterator<Map.Entry<String,MpnSubscription>> entries = active.entrySet().iterator();
            while (entries.hasNext()) {
                
                Map.Entry<String,MpnSubscription> entry = entries.next();
                
                MpnSubscription toCheck = entry.getValue();
                
                boolean alive = isMpnSubscriptionAlive(toCheck);
                if (!alive) {
                    entries.remove();
                    if (listener != null) {
                        notifyMpnStatusListener(false, toCheck.getMpnInfo().getTriggerExpression(), listener);
                    }
                } else if (listener != null) {
                    notifyMpnStatusListener(true, toCheck.getMpnInfo().getTriggerExpression(), listener);
                }
            }
        }
    }
    
    private boolean isMpnSubscriptionAlive(MpnSubscription toCheck) 
            throws SubscrException, PushServerException, PushUserException, PushConnException { //from eventsThread 
        
    	MpnStatusInfo statusInfo;
    	try {
    		statusInfo = toCheck.checkStatus();
        } catch (PushUserException e) {
             if (e.getErrorCode() == 45 || e.getErrorCode() == 46) {
                 //not active anymore
                 return false;
             } else {
                 throw e;
             }
        } 
        
        //if the current status is not active we can deactivate this subscription
    	return statusInfo.getSubscriptionStatus() == MpnSubscriptionStatus.Active;
        
    }
    
    
    
    private boolean mpnSubscriptionActivation(PendingOp op) 
            throws SubscrException, PushServerException, PushUserException, PushConnException { //from eventsThread {
        
        if (!mpnStatusRetrieved || !pmEnabled.get()) {
            return false;
        }

        //using this version a call to the server to verify the current status is performed
        /*if(isMpnSubscriptionAlive(op.info) == op.activate) {
            if (!op.activate) {
                //might be in cache, we should remove it
            }
        */
       
        
        //check if we're already in the desired status
        MpnSubscription cachedInfo = getFromMpnCache(op.info);
        if ((cachedInfo!=null) == op.activate) { //this version avoids an extra request to the server
            Log.d(TAG_MPN,"MPN subscriptions status for " + op.key + "-> " + op.trigger + " already in the desired status");
           
        } else {
        	
        	String key = op.info.getTableInfo().getGroup();
            String trigger = op.info.getTriggerExpression();
 
            if (op.activate) {
                Log.d(TAG_MPN,"Activating MPN subscriptions status for " + op.key + " -> " + op.trigger);
                MpnSubscription mpnSub = client.activateMpn(op.info,false);
                
                addToMpnCache(key, trigger, mpnSub);
                Log.d(TAG_MPN,"MPN subscription activation for " + op.key + " --> " + op.trigger + " OK");
            } else {
                Log.d(TAG_MPN,"Deactivating MPN subscriptions status for " + op.key + " -> " + op.trigger);
                cachedInfo.deactivate();
                removeFromMpnCache(key,trigger);
                Log.d(TAG_MPN,"MPN subscription activation/deactivation for " + op.key + " --> " + op.trigger + " OK");
            }
        }
    
        MpnStatusListener listener = mpnListeners.get(op.key);
        if (listener != null) {
            notifyMpnStatusListener(op.activate, op.trigger, listener);
        }

        return true;        
    }
    
    
    
    private class PendingOp {
        public final MpnInfo info;
        public final String key;
        public final String trigger;
        public boolean activate;

        public PendingOp(MpnInfo info, boolean activate) {
            this.info = info;
            this.key = info.getTableInfo().getGroup();
            this.trigger = info.getTriggerExpression();
            this.activate = activate;
        }
    }
    
    
    
    private class MpnSubscriptionThread implements Runnable {

       
        private PendingOp pendingOp;

        public MpnSubscriptionThread(MpnInfo info, boolean activate) {
            this.pendingOp = new PendingOp(info,activate);
        }

        @Override
        public void run() {
            Map<String,PendingOp> pendingForKey = mpnPendingCache.get(pendingOp.key);
            if (pendingForKey != null) {
                pendingForKey.remove(pendingOp.trigger);
            }
            
            if (connected && expectingConnected.get()) {
               try {
                    boolean success = mpnSubscriptionActivation(pendingOp);
                    if (success) {
                        return;
                    }
                    
                } catch (SubscrException e) {
                    Log.d(TAG,"Connection problems: " + e.getMessage());
                } catch (PushServerException e) {
                    Log.d(TAG,"Request error: " + e.getErrorCode() + ": " + e.getMessage());
                } catch (PushUserException e) {
                     Log.d(TAG,"Request refused: " + e.getErrorCode() + ": " + e.getMessage());
                } catch (PushConnException e) {
                    Log.d(TAG,"Connection problems: " + e.getMessage());
                }   
            }
            
            Log.d(TAG_MPN,"Delaying MPN subscription activation/deactivation for " + pendingOp.key+ " --> " + pendingOp.trigger);
            
            //in case of exception or premature exit the pending op is cached
            if (pendingForKey == null) {
                pendingForKey = new HashMap<String,PendingOp>();
                mpnPendingCache.put(pendingOp.key,pendingForKey);
            }
            pendingForKey.put(pendingOp.trigger, pendingOp);
            
            
        }
        
    }
    
    private class RetrieveMpnSubscriptionStatusThread implements Runnable {

        private String key;

        public RetrieveMpnSubscriptionStatusThread(String key) {
            this.key = key;
        }

        @Override
        public void run() {
            if (!connected || !expectingConnected.get()) {
                return;
            }
                        
            try {
                retrieveMpnSubscriptionStatus(key);
            } catch (SubscrException e) {
                Log.d(TAG,"Connection problems: " + e.getMessage());
            } catch (PushServerException e) {
                Log.d(TAG,"Request error: " + e.getErrorCode() + ": " + e.getMessage());
            } catch (PushUserException e) {
                 Log.d(TAG,"Request refused: " + e.getErrorCode() + ": " + e.getMessage());
            } catch (PushConnException e) {
                Log.d(TAG,"Connection problems: " + e.getMessage());
            }
            
        }
    }
    
    
    public interface MpnStatusListener {
        public void onMpnStatusChanged(boolean activated, String trigger);
    }

    public interface LightstreamerClientProxy {
        public void start();
        public void stop(boolean applyPause);
        public void addSubscription(Subscription sub);
        public void removeSubscription(Subscription sub);
        
        public void activateMPN(MpnInfo info);
        public void deactivateMPN(MpnInfo info); 
        public void retrieveMpnStatus(String key);
        
   }
    
    
}
