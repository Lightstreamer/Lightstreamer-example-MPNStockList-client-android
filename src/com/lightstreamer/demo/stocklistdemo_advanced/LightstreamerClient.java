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
package com.lightstreamer.demo.stocklistdemo_advanced;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.lightstreamer.ls_client.mpn.MpnKey;
import com.lightstreamer.ls_client.mpn.MpnStatus;


public class LightstreamerClient {
    
    public interface StatusChangeListener {
        public void onStatusChange(int status);
    }
    
    private static final String TAG = "LSConnection";
    
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
    
    Map<String,MpnInfo> mpns = new HashMap<String,MpnInfo>();
    Map<String,PendingMpnOp> pendingMpns = new HashMap<String,PendingMpnOp>();
    
    Map<String,MpnStatusListener> mpnListeners = new HashMap<String,MpnStatusListener>();
    
    
    private AtomicBoolean expectingConnected = new AtomicBoolean(false);
    private AtomicBoolean pmEnabled = new AtomicBoolean(false);
    
    private boolean connected = false; // do not get/set this outside the eventsThread
    
    final private ExecutorService eventsThread = Executors.newSingleThreadExecutor();
    
    final private ConnectionInfo cInfo = new ConnectionInfo();
    final private LSClient client = new LSClient();

    private ClientListener currentListener = null;
    
    private StatusChangeListener statusListener;

    private int status;
    
    public int getStatus() {
        return status;
    }

    void setStatus(int status) {
        this.status = status;
        Log.i(TAG,statusToString(this.status)); 
        notifyStatusChanged();
    }

    public LightstreamerClient(String pushServerUrl) {
         this.cInfo.pushServerUrl = pushServerUrl;
         this.cInfo.adapter = "DEMO";
    }
    
    public void setListener(StatusChangeListener statusListener) {
        this.statusListener = statusListener;
    }
    
    public void start() {
        Log.d(TAG,"Connection enabled");
        if (expectingConnected.compareAndSet(false,true)) {
            this.startConnectionThread(false);
        }
    }
    
    public void stop() {
        Log.d(TAG,"Connection disabled");
        if (expectingConnected.compareAndSet(true,false)) {
            this.startConnectionThread(true);
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
    private void changeStatus(ClientListener caller, boolean connected, int status) {
        if (caller != this.currentListener) {
            return;
        }
        
        this.connected = connected;
        this.setStatus(status);
        
        
        if (connected != expectingConnected.get()) {
            this.startConnectionThread(false);
        }
    }
    
    private void notifyStatusChanged() {
        if (this.statusListener != null) {
            this.statusListener.onStatusChange(this.status);
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
                    setStatus(CONNECTING);
                    try {
                        currentListener = new ClientListener();
                        client.openConnection(cInfo, currentListener);
                        Log.d(TAG,"Connecting success");
                        connected = true;
                        
                        resubscribeAll();
                        handlePendingMpnOps();
                        
                    } catch (PushConnException e) {
                        Log.v(TAG, e.getMessage());
                    } catch (PushServerException e) {
                        Log.v(TAG, e.getMessage());
                    } catch (PushUserException e) {
                        Log.v(TAG, e.getMessage());
                    }
                    
                    if (!connected) {
                        try {
                            setStatus(WAITING);
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                        }
                    }
                    
                } else {
                    Log.v(TAG,"Disconnecting");
                    client.closeConnection();
                    setStatus(DISCONNECTED);
                    currentListener = null;
                    connected = false;
                }
                
            }     
        }
    }
    
    private class ConnectionEvent implements Runnable {

        private final ClientListener caller;
        private final boolean connected;
        private final int status;

        public ConnectionEvent(ClientListener caller, boolean connected, int status) {
            this.caller = caller;
            this.connected = connected;
            this.status = status;
        }
        
        @Override
        public void run() {
            changeStatus(this.caller, this.connected, this.status);
        }
        
    }
    
    
    private class ClientListener implements ConnectionListener {

        private final long randomId;
        private int lastConnectionStatus;

        public ClientListener() {
            randomId = Math.round(Math.random()*1000);
        }
        
        @Override
        public void onActivityWarning(boolean warn) {
            Log.d(TAG,randomId + " onActivityWarning " + warn);
            if (warn) {
                eventsThread.execute(new ConnectionEvent(this,true,STALLED));
            } else {
                eventsThread.execute(new ConnectionEvent(this,true,this.lastConnectionStatus));
            }
            
        }

        @Override
        public void onClose() {
            Log.d(TAG,randomId + " onClose");
            eventsThread.execute(new ConnectionEvent(this,false,DISCONNECTED));
        }

        @Override
        public void onConnectionEstablished() {
            Log.d(TAG,randomId + " onConnectionEstablished");
        }

        @Override
        public void onDataError(PushServerException pse) {
            Log.d(TAG,randomId + " onDataError: " + pse.getErrorCode() + " -> " + pse.getMessage());
        }

        @Override
        public void onEnd(int cause) {
            Log.d(TAG,randomId + " onEnd " + cause);
        }

        @Override
        public void onFailure(PushServerException pse) {
            Log.d(TAG,randomId + " onFailure: " + pse.getErrorCode() + " -> " + pse.getMessage());
        }

        @Override
        public void onFailure(PushConnException pce) {
            Log.d(TAG,randomId + " onFailure: " + pce.getMessage());
        }

        @Override
        public void onNewBytes(long num) {
            Log.v(TAG,randomId + " onNewBytes " + num);
        }

        @Override
        public void onSessionStarted(boolean isPolling) {
            Log.d(TAG,randomId + " onSessionStarted; isPolling: " + isPolling);
            if (isPolling) {
                this.lastConnectionStatus = POLLING;
            } else {
                this.lastConnectionStatus = STREAMING;
            }
            eventsThread.execute(new ConnectionEvent(this,true,this.lastConnectionStatus));
            
        }
        
    }
    

    
//Subscription handling    
    
    
    private void resubscribeAll() {
        if (subscriptions.size() == 0) {
            return;
        }
        
        Log.i(TAG,"Resubscribing " + subscriptions.size() + " subscriptions");
        
        ExecutorService tmpExecutor = Executors.newFixedThreadPool(subscriptions.size());
        
        //start batch
        try {
            client.batchRequests(subscriptions.size());

        } catch (SubscrException e) {
            //connection is closed, exit
            return;
        }
        
        Iterator<Subscription> subscriptionsIterator = subscriptions.iterator();
        while(subscriptionsIterator.hasNext()) {
            tmpExecutor.execute(new BatchSubscriptionThread(subscriptionsIterator.next()));
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
                    Log.d(TAG,"Can't add subscription: Subscription already in: " + sub);
                    return;
                }
                
                Log.i(TAG,"Removing subscription " + sub);
                subscriptions.remove(sub);
                String key = sub.getTableInfo().getGroup();
                mpnListeners.remove(key);
                
            } else {
                if (!this.add) {
                    //already removed, exit now
                    Log.d(TAG,"Can't remove subscription: Subscription not in: " + sub);
                    return;
                }
                Log.i(TAG,"Adding subscription " + sub);
                subscriptions.add(sub);
                
                MpnStatusListener listener = sub.getMpnStatusListener();
                if (listener != null)  {
                    String key = sub.getTableInfo().getGroup();
                    mpnListeners.put(key,listener);
                    
                    notifyMpnStatus(key);
                }
                
            }
            
            
            if (connected) {
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
        
        Log.d(TAG,"Subscribing " + sub);
        
        try {
            SubscribedTableKey key = client.subscribeTable(sub.getTableInfo(), sub.getTableListener(), false);
            sub.setTableKey(key);
        } catch (SubscrException e) {
            Log.d(TAG,"Connection was closed: " + e.getMessage());
        } catch (PushServerException e) {
            Log.d(TAG,"Subscription failed: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushUserException e) {
            Log.d(TAG,"Subscription refused: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushConnException e) {
            Log.d(TAG,"Connection problems: " + e.getMessage());
        }
    }
    private void doUnsubscription(Subscription sub) {
        
        Log.d(TAG,"Unsubscribing " + sub);
        
        try {
            client.unsubscribeTable(sub.getTableKey());
        } catch (SubscrException e) {
            Log.d(TAG,"Connection was closed: " + e.getMessage());
        } catch (PushServerException e) {
            Log.wtf(TAG,"Unsubscription failed: " + e.getErrorCode() + ": " + e.getMessage());
        } catch (PushConnException e) {
            Log.d(TAG,"Unubscription failed: " + e.getMessage());
        }
    }
    
//Mpn handling    
    

    public void enablePM(boolean enabled) {
        this.pmEnabled.set(enabled);
    }
    
    private void notifyMpnStatus(String key) {
        MpnStatusListener listener = mpnListeners.get(key);
        if (listener != null) {
            if (!mpns.containsKey(key)) {
                listener.onMpnStatusChanged(false,-1);
            } else {
                MpnInfo current = mpns.get(key);
                
                double triggerValue = -1;
                String trigger = current.getTriggerExpression();
                if (trigger != null) {
                    try {
                        triggerValue = Double.parseDouble(trigger.substring(Stock.TRIGGER_HEAD.length()+Stock.TRIGGER_GT.length()));
                    } catch(NumberFormatException e) {
                        Log.wtf(TAG, "Unexpected trigger set: " + trigger);
                    }
                }
                         
                listener.onMpnStatusChanged(true,triggerValue);
            }
        }
    }
    
    private void handlePendingMpnOps() { //called from the eventsThread
        for (Iterator<String> i = pendingMpns.keySet().iterator(); i.hasNext();) {
            String key = i.next();
            
            try {
                handlePendingMpnOp(pendingMpns.get(key));
            } catch (SubscrException e) {
            } catch (PushServerException e) {
            } catch (PushUserException e) {
            } catch (PushConnException e) {
            }
            
            notifyMpnStatus(key); //in any case notify status
        }
    }
    
    private void handlePendingMpnOp(PendingMpnOp op) 
        throws SubscrException, PushServerException, PushUserException, PushConnException { //called from the eventsThread
        
        retrieveCurrentMpnStatus(op.sub);
        
        MpnInfo info = op.sub.getMpnInfo();
        String key = op.sub.getTableInfo().getGroup();
        
        if (mpns.containsKey(key) == op.add) {
            //already in the desired state, remove pending op and exit
            pendingMpns.remove(key);
            if (op.add) {
                Log.d(TAG,"Can't add mpn subscription: mpn subscription already in: " + op.sub);
            } else {
                Log.d(TAG,"Can't remove mpn subscription: mpn subscription not in: " + op.sub);
            }
            return;
        }

        if (op.add) {
            client.activateMpn(info); //in case of failure we leave the pending op (we might want to change this behavior)
            mpns.put(key, info);
            
        } else {
            info = mpns.get(key); //get the info from the collection as we are sure that that one contains the mpnKey
            client.deactivateMpn(info.getMpnKey()); //in case of failure we leave the pending op (we might want to change this behavior)
            mpns.remove(key);
        }
        
        //remove from pending op and fire listener
        pendingMpns.remove(key);
     
    }
    
    //note that the TableInfo group will be used to uniquely identify its MpnInfo
    public synchronized void activateMPN(Subscription info) {
        eventsThread.execute(new MpnSubscriptionThread(info,true));
    }

    //note that the TableInfo group will be used to uniquely identify its MpnInfo
    public synchronized void deactivateMPN(Subscription info) {
        eventsThread.execute(new MpnSubscriptionThread(info,false));
    }
    
    public synchronized void retrieveMpnStatus(final Subscription info) {
        eventsThread.execute(new Runnable() {
            public void run() {
                try {
                    retrieveCurrentMpnStatus(info);
                } catch (PushConnException e) {
                } catch (SubscrException e) {
                } catch (PushServerException e) {
                } catch (PushUserException e) {
                }
            }
        });
    }

    void retrieveCurrentMpnStatus(Subscription sub) 
            throws PushConnException, SubscrException, PushServerException, PushUserException {
        String key = sub.getTableInfo().getGroup();
        
        if (mpns.containsKey(key)) {
            MpnKey mpnKey = mpns.get(key).getMpnKey();
            MpnStatus status;
            try {
                status = client.inquireMpnStatus(mpnKey);
            } catch (PushUserException e) {
                if (e.getErrorCode() == 45 || e.getErrorCode() == 46) {
                    //not active anymore
                    mpns.remove(key);
                    notifyMpnStatus(key);
                    return;
                } else {
                    throw e;
                }
            }
            
            if (status == MpnStatus.Suspended || status == MpnStatus.Triggered) {
                //deactivate useless subscription before handling the pending op
                client.deactivateMpn(mpnKey);
                mpns.remove(key);
                notifyMpnStatus(key);
            } else {
                //otherwise refresh the mpninfo we have
                MpnInfo info = client.inquireMpn(mpnKey);
                mpns.put(key, info);
                notifyMpnStatus(key);
            }
            
        }
    }
    
    
    private class MpnSubscriptionThread implements Runnable {

        Subscription sub;
        private boolean add;
        
        public MpnSubscriptionThread(Subscription sub, boolean add) {
            this.sub = sub;
            this.add = add;
        }
        
        private PendingMpnOp updatePendingStatus() {
            String key = sub.getTableInfo().getGroup();
            PendingMpnOp op;
            if (pendingMpns.containsKey(key)) {
                //something pending
                op = pendingMpns.get(key);
                //update the pending operation status
                op.add = this.add;
            } else {
                //fill pending
                op = new PendingMpnOp(sub,this.add);
                pendingMpns.put(key, op);
            }
            return op;
        }
        
        
        
        @Override
        public void run() {
            
            if (sub.getMpnInfo() == null) {
                //nothing to do
                return;
            }
            
            //save the pending operation in case we're not able to handle it now
            PendingMpnOp op = this.updatePendingStatus();
            
            if (!connected || !pmEnabled.get()) {
                //can't handle it now, pending subscriptions will be fired once re-connected
                return;
            }
            
            //status may have changed on the server, refresh it
            try {
                handlePendingMpnOp(op);
            } catch (PushConnException e) {
            } catch (SubscrException e) {
            } catch (PushServerException e) {
            } catch (PushUserException e) {
            }
            
            notifyMpnStatus(sub.getTableInfo().getGroup()); 
            
            //if an exception was thrown we fail and wait for the pending operations to be 
            //handled again later

            
            
        }
        
    }
    
    private class PendingMpnOp {
        
        public Subscription sub;
        public boolean add;
        
        public PendingMpnOp(Subscription sub, boolean add) {
            this.add = add;
            this.sub = sub;
        }
        
    }
    
    
    public interface MpnStatusListener {
        public void onMpnStatusChanged(boolean activated, double trigger);
    }

    public interface LightstreamerClientProxy {
        public void start();
        public void stop();
        public void addSubscription(Subscription sub);
        public void removeSubscription(Subscription sub);
        
        public void activateMPN(Subscription info);
        public void deactivateMPN(Subscription info); 
        public void retrieveMpnStatus(Subscription info);
        
   }
    
    
}
