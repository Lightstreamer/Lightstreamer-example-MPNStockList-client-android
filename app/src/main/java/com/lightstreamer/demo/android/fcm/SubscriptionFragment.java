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
package com.lightstreamer.demo.android.fcm;

import static com.lightstreamer.demo.android.fcm.Utils.TAG;

import com.lightstreamer.demo.android.fcm.DetailsFragment.ItemSubscription;

import android.util.Log;

public class SubscriptionFragment {

    private ItemSubscription subscription;
    private boolean subscribed = false;
    private boolean running = false;
    
    synchronized void setSubscription(ItemSubscription subscription) {
        if (this.subscription != null && subscribed) {
            Log.d(TAG,"Replacing subscription");
            this.subscription.unsubscribeStock();
        }
        Log.d(TAG,"New subscription " + subscription);
        this.subscription = subscription;
        
        if (running) {
            this.subscription.subscribeStock();
        }
    }
    
    synchronized void activateMPN(ItemSubscription info) {
        info.subscribeTick();
    }
    
    synchronized void deactivateMPN(ItemSubscription info) {
        info.unsubscribeTick();
    }
    
    synchronized void onResume() {
        if (this.subscription != null) {
            this.subscription.subscribeStock();
            this.subscription.onResume();
            subscribed = true;
        }
        running = true;
    }
    
    synchronized void onPause() {
        if (this.subscription != null) {
            this.subscription.unsubscribeStock();
            this.subscription.onPause();
            subscribed = false;
        }
        running = false;
    }
}
