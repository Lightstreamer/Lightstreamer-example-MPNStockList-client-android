/*
 * Copyright (c) Lightstreamer Srl
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

import com.lightstreamer.demo.android.LightstreamerClient.LightstreamerClientProxy;
import com.lightstreamer.ls_client.mpn.MpnInfo;

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
            this.lsClient.retrieveMpnStatus(this.subscription.getTableInfo().getGroup());
        }
    }
    
    protected synchronized void activateMPN(MpnInfo info) {
        this.lsClient.activateMPN(info);
    }
    
    protected synchronized void deactivateMPN(MpnInfo info) {
        this.lsClient.deactivateMPN(info);
    }
    
    public synchronized void onResume() {
        //subscribe
        if (this.lsClient != null && this.subscription != null) {
            this.lsClient.addSubscription(this.subscription);
            subscribed = true;
            this.lsClient.retrieveMpnStatus(this.subscription.getTableInfo().getGroup());
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
