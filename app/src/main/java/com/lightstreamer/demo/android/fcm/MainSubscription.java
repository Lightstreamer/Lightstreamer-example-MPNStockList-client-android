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

import java.util.ArrayList;

import com.lightstreamer.client.ItemUpdate;

import android.os.Handler;
import android.util.Log;
import android.widget.ListView;

/**
 * A subscription of a list of stocks showed by {@link StocksFragment}.
 */
class MainSubscription {

    private final com.lightstreamer.client.Subscription listSubscription;
    private final Context context = new Context();
    
    public MainSubscription(final ArrayList<StockForList> list) {
        listSubscription = new com.lightstreamer.client.Subscription("MERGE", StocksFragment.items, StocksFragment.subscriptionFields);
        listSubscription.setRequestedSnapshot("yes");
        listSubscription.setDataAdapter("QUOTE_ADAPTER");
        listSubscription.setRequestedMaxFrequency("1");
        listSubscription.addListener(new Utils.VoidSubscriptionListener() {
          @Override
            public void onSubscription() {
                Log.d(TAG, "Subscribed to main subscription");
            }
            
            @Override
            public void onSubscriptionError(int code, String message) {
                Log.d(TAG, "Unable to subscribe to main subscription: " + message);
            }

            @Override
            public void onUnsubscription() {
                Log.d(TAG, "Unsubscribed from main subscription");
            }
            
            @Override
            public void onItemUpdate(ItemUpdate newData) {
                Log.v(TAG, "Update for main subscription");
                final StockForList toUpdate = list.get(newData.getItemPos() - 1);
                toUpdate.update(newData, context);
            }
        });
    }
    
    public void subscribe() {
        Log.d(TAG, "Subscribing to main subscription...");
        LsClient.instance.subscribe(listSubscription);
    }
    
    public void unsubscribe() {
        Log.d(TAG, "Unsubscribing from main subscription...");
        LsClient.instance.unsubscribe(listSubscription);
    }
    
    public void changeContext(Handler handler, ListView listView) {
        this.context.handler = handler;
        this.context.listView = listView;
    }
    
    public class Context {
        public Handler handler;
        public ListView listView;
    }
}