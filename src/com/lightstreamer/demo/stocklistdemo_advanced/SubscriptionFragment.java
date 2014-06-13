package com.lightstreamer.demo.stocklistdemo_advanced;

import com.lightstreamer.demo.stocklistdemo_advanced.LightstreamerClient.LightstreamerClientProxy;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import android.util.Log;

public class SubscriptionFragment extends ListFragment {

    private static final String TAG = "SubscriptionFragment";
    
    private LightstreamerClientProxy lsClient;
    private Subscription subscription;
    private boolean subscribed = false;
    
    protected synchronized void setSubscription(Subscription subscription) {
        if (this.subscription != null && subscribed) {
            Log.d(TAG,"Replacing subscription");
            this.lsClient.removeSubscription(this.subscription);
        }
        Log.d(TAG,"New subscription " + subscription);
        this.subscription = subscription;
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        //subscribe
        if (this.lsClient != null && this.subscription != null) {
            this.lsClient.addSubscription(this.subscription);
            subscribed = true;
        }
    }
    
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        //unsubscribe
        if (this.lsClient != null && this.subscription != null) {
            this.lsClient.removeSubscription(this.subscription);
            subscribed = false;
        }
    }
    
     public synchronized void onAttach(Activity activity) {
         super.onAttach(activity);
         
        try {
            lsClient = (LightstreamerClientProxy) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LightstreamerClientProxy");
        }
     }
}
