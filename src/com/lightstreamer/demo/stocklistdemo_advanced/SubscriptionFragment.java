package com.lightstreamer.demo.stocklistdemo_advanced;

import com.lightstreamer.demo.stocklistdemo_advanced.LightstreamerClient.LightstreamerClientProxy;

import android.app.Activity;
import android.util.Log;

public class SubscriptionFragment /*extends Fragment*/ {

    private static final String TAG = "SubscriptionFragment";
    
    private LightstreamerClientProxy lsClient;
    private Subscription subscription;
    private boolean subscribed = false;
    private boolean running = false;
    
    protected synchronized void setSubscription(Subscription subscription) {
        if (this.subscription != null && subscribed) {
            Log.d(TAG,"Replacing subscription");
            this.lsClient.removeSubscription(this.subscription);
        }
        Log.d(TAG,"New subscription " + subscription);
        this.subscription = subscription;
        
        if (running) {
            this.lsClient.addSubscription(this.subscription);
        }
    }

    public synchronized void onResume() {
        //subscribe
        if (this.lsClient != null && this.subscription != null) {
            this.lsClient.addSubscription(this.subscription);
            subscribed = true;
        }
        running = true;
    }
    
    
    public synchronized void onPause() {
        //unsubscribe
        if (this.lsClient != null && this.subscription != null) {
            this.lsClient.removeSubscription(this.subscription);
            subscribed = false;
        }
        running = false;
    }
    
    public synchronized void onAttach(Activity activity) {
        try {
            lsClient = (LightstreamerClientProxy) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LightstreamerClientProxy");
        }
    }
}
