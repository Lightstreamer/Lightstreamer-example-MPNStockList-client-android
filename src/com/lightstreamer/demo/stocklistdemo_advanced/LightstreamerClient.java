package com.lightstreamer.demo.stocklistdemo_advanced;

import java.util.Iterator;
import java.util.LinkedList;
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
    private AtomicBoolean expectingConnected = new AtomicBoolean(false);
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

    public void setStatus(int status) {
        Log.i(TAG,statusToString(this.status)); 
        this.status = status;
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
    
    public void addSubscription(Subscription sub) {
        eventsThread.execute(new SubscriptionThread(sub,true));
    }
    public void removeSubscription(Subscription sub) {
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

        public void run() {
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
                    Log.d(TAG,"Can't remove subscription: Subscription not in: " + sub);
                    return;
                }
                
                Log.i(TAG,"Removing subscription " + sub);
                subscriptions.remove(sub);
                
            } else {
                if (!this.add) {
                    //already removed, exit now
                    Log.d(TAG,"Can't add subscription: Subscription already in: " + sub);
                    return;
                }
                Log.i(TAG,"Adding subscription " + sub);
                subscriptions.add(sub);
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
    
    
    public interface LightstreamerClientProxy {
         public void start();
         public void stop();
         public void addSubscription(Subscription sub);
         public void removeSubscription(Subscription sub);
    }
}
