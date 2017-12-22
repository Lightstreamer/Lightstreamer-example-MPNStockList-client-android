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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.lightstreamer.client.ItemUpdate;
import com.lightstreamer.client.mpn.MpnSubscription;
import com.lightstreamer.client.mpn.util.MpnBuilder;

import android.app.Activity;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * A fragment displaying the details of a stock.
 */
public class DetailsFragment extends Fragment {
    
    public final static String[] numericFields = {"last_price", "pct_change","bid_quantity", "bid", "ask", "ask_quantity", "min", "max","open_price"};
    public final static String[] otherFields = {"stock_name", "time"};
    public final static String[] subscriptionFields = {"stock_name", "last_price", "time", "pct_change","bid_quantity", "bid", "ask", "ask_quantity", "min", "max","open_price"};
    public final static String[] mpnSubscriptionFields = {"stock_name", "last_price", "time"};
    
    private final SubscriptionFragment subscriptionHandling = new SubscriptionFragment();
    private Handler handler;
    HashMap<String, TextView> holder =  new HashMap<String, TextView>();
    Chart chart = new Chart();
    ToggleButton toggle;
    
    public static final String ARG_ITEM = "item";
    public static final String ARG_PN_CONTROLS = "pn_controls";
    
    int currentItem = 0;

    private ItemSubscription currentSubscription = null;
    private View toggleContainer;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        
        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
            currentItem = savedInstanceState.getInt(ARG_ITEM);
        }
        

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.details_view, container, false);
        
        toggle = (ToggleButton)view.findViewById(R.id.pn_switch);
        toggleContainer = view.findViewById(R.id.toggle_container);
        
        this.enablePN();

        holder.put("stock_name",(TextView)view.findViewById(R.id.d_stock_name));
        holder.put("last_price",(TextView)view.findViewById(R.id.d_last_price));
        holder.put("time",(TextView)view.findViewById(R.id.d_time));
        holder.put("pct_change",(TextView)view.findViewById(R.id.d_pct_change));
        holder.put("bid_quantity",(TextView)view.findViewById(R.id.d_bid_quantity));
        holder.put("bid",(TextView)view.findViewById(R.id.d_bid));
        holder.put("ask",(TextView)view.findViewById(R.id.d_ask));
        holder.put("ask_quantity",(TextView)view.findViewById(R.id.d_ask_quantity));
        holder.put("min",(TextView)view.findViewById(R.id.d_min));
        holder.put("max",(TextView)view.findViewById(R.id.d_max));
        holder.put("open_price",(TextView)view.findViewById(R.id.d_open_price));
        
        final XYPlot plot = (XYPlot) view.findViewById(R.id.mySimpleXYPlot);
        chart.setPlot(plot);
        
        plot.setOnTouchListener(new OnTouchListener(){
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                
                int action = event.getActionMasked();
                
                float touchY = event.getY();
                float touchX = event.getX();
                
                XYGraphWidget widget = plot.getGraphWidget();
                RectF gridRect = widget.getGridRect();
                if(gridRect.contains(touchX, touchY)){
                    
                    if (currentSubscription != null) {
                        double triggerVal = widget.getYVal(touchY);
                       
                        /*
                        chart.setMovingTriggerLine(triggerVal);
                        if (action == MotionEvent.ACTION_UP) {
                            triggerVal =  Math.round(triggerVal*100.0)/100.0;
                            chart.endMovingTriggerLine(triggerVal);
                            
                            Log.d(TAG,"Touch released @ " + triggerVal);
                            //go on the network only after the touch has been released
                            subscriptionHandling.activateMPN(getMpnInfo(triggerVal));
                        }
                        */
                        if (action == MotionEvent.ACTION_UP) {
                            triggerVal =  Math.round(triggerVal*100.0)/100.0;
                            currentSubscription.toggleTrigger(triggerVal);
                        }
                        
                    } else {
                        Log.v(TAG,"touch ignored");
                    }
                }
                
                return true;
            }
        });
        
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        chart.onResume(this.getActivity());
        
        Bundle args = getArguments();
        if (args != null) {
            updateStocksView(args.getInt(ARG_ITEM));
            enablePN();
        } else if (currentItem != 0) {
            updateStocksView(currentItem);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        chart.onPause();
        this.subscriptionHandling.onPause();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        chart.onResume(this.getActivity());
        
        this.subscriptionHandling.onResume();
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
    
    public void updateStocksView(int item) {
        if (item != currentItem || this.currentSubscription == null) {
            this.currentSubscription = new ItemSubscription("item"+item);
            this.subscriptionHandling.setSubscription(this.currentSubscription);
            
            currentItem = item;
        }
    }
    
    
    public int getCurrentStock() {
        return this.currentItem;
    }
    
    public void togglePN(ToggleButton toggle) {
        //TODO toggle status can be overridden by the onMpnStatusChanged: find a user friendly way to handle the case
        boolean on = toggle.isChecked();
        if (currentItem != 0) {
            if (on) {
                Log.v(TAG,"PN enabled for " + currentSubscription.getGroup());
                subscriptionHandling.activateMPN(currentSubscription);
            } else {
                Log.v(TAG,"PN disabled for " + currentSubscription.getGroup());
                subscriptionHandling.deactivateMPN(currentSubscription);
            }
            
        }
    }
    
    public void enablePN() {
        this.showToggle(true);
    }
    
    private void showToggle(final boolean show) {
        if (toggleContainer == null) {
            //onCreateView will set it (or the layout does not contain it)
            return;
        }
        
        if (currentSubscription != null) {
            
        }
        
        handler.post(new Runnable() {
            public void run() {
                if (toggleContainer == null) {
                    return;
                }
                toggleContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
        
        
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ARG_ITEM, currentItem);
    }
    
    
    class ItemSubscription {
        
        private static final String TRIGGER_HEAD = "Double.parseDouble($[2])";
        private static final String TRIGGER_LT = "<=";
        private static final String TRIGGER_GT = ">=";
    
        private final Stock stock;
        private final com.lightstreamer.client.Subscription stockSubscription;
        private volatile MpnSubscription tickSubscription;
        private final Map<Double, MpnSubscription> triggers = Collections.synchronizedMap(new HashMap<Double, MpnSubscription>());

        public ItemSubscription(String item) {
            stock = new Stock(item,numericFields,otherFields);
            stock.setHolder(holder);
            stock.setChart(chart);
                     
            stockSubscription = new com.lightstreamer.client.Subscription("MERGE", new String[] {item}, subscriptionFields);
            stockSubscription.setRequestedSnapshot("yes");
            stockSubscription.setDataAdapter("QUOTE_ADAPTER");
            stockSubscription.addListener(new Utils.VoidSubscriptionListener() {
                @Override
                public void onSubscription() {
                    Log.d(TAG, "Subscribed to " + getGroup());
                }
                
                @Override
                public void onSubscriptionError(int code, String message) {
                    Log.d(TAG, "Unable to subscribe to " + getGroup() + ": " + message);
                }
                
                @Override
                public void onUnsubscription() {
                    Log.d(TAG, "Unsubscribed from " + getGroup());
                }
                
                @Override
                public void onItemUpdate(ItemUpdate newData) {
                    Log.v(TAG,"Update for " + getGroup());
                    stock.update(newData, handler);
                }
            });
            
            /* resume MPN subscriptions */
            List<MpnSubscription> subs = LsClient.instance.getMpnSubscriptions();
            String thisGroup = getGroup();
            for (MpnSubscription sub : subs) {
                if (sub.getItemGroup().equals(thisGroup)) {
                    if (sub.getTriggerExpression() == null) {
                        /* resume tick subscription (it is the only one without a trigger condition) */
                        Log.d(TAG, "Resume MPN " + thisGroup);
                        setTickSubscription(sub);
                    } else {
                        /* resume triggers */
                        Log.d(TAG, "Resume MPN " + thisGroup + " trigger: " + sub.getTriggerExpression());
                        addTrigger(sub);
                    }
                }
            }
        }
        
        public void subscribeTick() {
            if (tickSubscription == null) {
                MpnSubscription info = createMpnSubscription();
                info.addListener(new Utils.VoidMpnSubscriptionListener() {
                    @Override
                    public void onSubscriptionError(int code, String message) {
                        Log.e(TAG, "Unable to subscribe MPN tick for " + getGroup() + ". Cause: " + message);
                        handler.post(new Runnable() {
                            public void run() {
                                if (toggle != null) {
                                    toggle.setChecked(false);
                                }
                            }
                        });
                    }
                });
                setTickSubscription(info);
            }
            LsClient.instance.subscribe(tickSubscription);
        }
        
        public void unsubscribeTick() {
            assert tickSubscription != null;
            LsClient.instance.unsubscribe(tickSubscription);
            setTickSubscription(null);
        }
        
        public void toggleTrigger(final double triggerVal) {
            MpnSubscription sub = triggers.remove(triggerVal);
            if (sub != null) {
                /* remove the old trigger */
                chart.removeTriggerLine(triggerVal);
                LsClient.instance.unsubscribe(sub);
                
            } else {
                /* add a new trigger */
                sub = createMpnSubscription();
                sub.addListener(new Utils.VoidMpnSubscriptionListener() {
                    @Override
                    public void onTriggered() {
                        Log.d(TAG, "Fire trigger " + triggerVal);
                        chart.setRedLine(triggerVal);
                    }
                });
                sub.setTriggerExpression(triggetToString(triggerVal, getLastPrice()));
                LsClient.instance.subscribe(sub);
                
                chart.setGreenLine(triggerVal);
                triggers.put(triggerVal, sub);                
            }
        }

        private MpnSubscription createMpnSubscription() {
            MpnSubscription sub = new MpnSubscription(stockSubscription);
            Map<String, String> data= new HashMap<String, String>();
            data.put("stock_name", "${stock_name}");
            data.put("last_price", "${last_price}");
            data.put("time", "${time}");
            data.put("item", stockSubscription.getItems()[0]);
            String format = new MpnBuilder()
                    .data(data)
                    .build();
            sub.setNotificationFormat(format);
            return sub;
        }
        
        public void subscribeStock() {
            Log.d(TAG, "Subscribing to " + getGroup() + "...");
            LsClient.instance.subscribe(stockSubscription);
        }
        
        public void unsubscribeStock() {
            Log.d(TAG, "Unsubscribing from " + getGroup() + "...");
            LsClient.instance.unsubscribe(stockSubscription);
        }
        
        public void onResume() {
            /* draw trigger lines */
            synchronized (triggers) {                
                for (Entry<Double, MpnSubscription> entry : triggers.entrySet()) {
                    double triggerVal = entry.getKey();
                    MpnSubscription sub = entry.getValue();
                    if (sub.isTriggered()) {
                        chart.setRedLine(triggerVal);
                    } else {
                        chart.setGreenLine(triggerVal);
                    }
                }
            }
            /* set label to PN button */
            final boolean arePushNotificationsEnabled = tickSubscription != null;
            handler.post(new Runnable() {
                public void run() {
                    if (toggle != null) { //toggle can be null if we're in landscape mode
                        toggle.setChecked(arePushNotificationsEnabled);
                    }
                }
            });
        }
        
        private void setTickSubscription(MpnSubscription sub) {
            this.tickSubscription = sub;
        }
        
        private void addTrigger(MpnSubscription sub) {
            String trigger = sub.getTriggerExpression();
            assert trigger != null;
            final double triggerDouble = Double.parseDouble(trigger.substring(TRIGGER_HEAD.length() + TRIGGER_GT.length()));
            if (! sub.isTriggered()) {
                sub.addListener(new Utils.VoidMpnSubscriptionListener() {
                    @Override
                    public void onTriggered() {
                        Log.d(TAG, "Fire trigger " + triggerDouble);
                        chart.setRedLine(triggerDouble);
                    }
                });
            }
            triggers.put(triggerDouble, sub);
        }
        
        private String triggetToString(double triggerVal, double current) {
            String trigger = null;
            if (triggerVal >= 0) {
                trigger = TRIGGER_HEAD;
                if (triggerVal < current) {
                    trigger += TRIGGER_LT; 
                } else {
                    trigger += TRIGGER_GT;
                }
                
                trigger += triggerVal;
            }
            
            return trigger; 
        }
        
        public String getGroup() {
            return stockSubscription.getItems()[0];
        }

        public double getLastPrice() {
            return this.stock.getLastPrice(); 
        }
    }

}
