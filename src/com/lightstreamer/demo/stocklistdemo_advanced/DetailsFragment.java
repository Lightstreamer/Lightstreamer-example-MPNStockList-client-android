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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.lightstreamer.demo.stocklistdemo_advanced.LightstreamerClient.MpnStatusListener;
import com.lightstreamer.ls_client.ExtendedTableInfo;
import com.lightstreamer.ls_client.HandyTableListener;
import com.lightstreamer.ls_client.SubscrException;
import com.lightstreamer.ls_client.SubscribedTableKey;
import com.lightstreamer.ls_client.UpdateInfo;
import com.lightstreamer.ls_client.mpn.MpnInfo;

import android.app.Activity;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class DetailsFragment extends Fragment {
    
    private static final String TAG = "Details";
    
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
    
    int currentItem = -1;

    private ItemSubscription currentSubscription = null;

    private View toggleContainer;
    private boolean pnEnabled;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        
        pnEnabled = false;

        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
            currentItem = savedInstanceState.getInt(ARG_ITEM);
            pnEnabled = savedInstanceState.getBoolean(ARG_PN_CONTROLS);
        }
        

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.details_view, container, false);
        
        toggle = (ToggleButton)view.findViewById(R.id.pn_switch);
        toggleContainer = view.findViewById(R.id.toggle_container);
        
        this.enablePN(pnEnabled);

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
                if (!pnEnabled) {
                    //push notifications not enabled, ignore the touch
                    return false;
                }
                
                v.performClick();
                
                float touchY = event.getY();
                float touchX = event.getX();
                
                XYGraphWidget widget = plot.getGraphWidget();
                RectF gridRect = widget.getGridRect();
                if(gridRect.contains(touchX, touchY)){
                    Log.d(TAG,"chart touched");
                    if (currentSubscription != null) {
                        currentSubscription.setTrigger(widget.getYVal(touchY));
                        subscriptionHandling.activateMPN();
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

        Bundle args = getArguments();
        if (args != null) {
            updateStocksView(args.getInt(ARG_ITEM));
            enablePN(args.getBoolean(ARG_PN_CONTROLS));
        } else if (currentItem != -1) {
            updateStocksView(currentItem);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
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
        this.subscriptionHandling.onAttach(activity);
    }
    
    
    public void updateStocksView(int item) {
        if (item != currentItem || this.currentSubscription == null) {
            if (this.currentSubscription != null) {
                this.currentSubscription.disable();
            }
            this.currentSubscription = new ItemSubscription("item"+item);
            this.subscriptionHandling.setSubscription(this.currentSubscription);
            
            currentItem = item;
        }
    }
    
    public void togglePN(ToggleButton toggle) {
        //TODO toggle status can be overridden by the onMpnStatusChanged: find a user friendly way to handle the case
        boolean on = toggle.isChecked();
        if (currentItem != -1) {
            if (on) {
                Log.v(TAG,"PN enabled for item" + currentItem);
                this.subscriptionHandling.activateMPN();
            } else {
                Log.v(TAG,"PN disabled for item" +currentItem);
                this.subscriptionHandling.deactivateMPN();
            }
            
        }
    }
    
    public void enablePN(boolean enabled) {
        this.pnEnabled = enabled;
        this.showToggle(enabled);
    }

    private void showToggle(final boolean show) {
        
        if (toggleContainer == null) {
            //onCreateView will set it (or the layout does not contain it)
            return;
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
        outState.putBoolean(ARG_PN_CONTROLS, this.pnEnabled);
    }
    
    
    private class ItemSubscription implements Subscription {
    
        private final Stock stock;
        private ExtendedTableInfo tableInfo;
        private SubscribedTableKey key;
        private StockListener listener;
        private MpnInfo mpnInfo;
        
        public ItemSubscription(String item) {
            this.stock = new Stock(item,numericFields,otherFields);
            stock.setHolder(holder);
            stock.setChart(chart);
            stock.setToggle(toggle);
            
           
            this.listener = new StockListener(stock);
            
            try {
                this.tableInfo = new ExtendedTableInfo(new String[] {item}, "MERGE", subscriptionFields , true);
                this.tableInfo.setDataAdapter("QUOTE_ADAPTER");
            } catch (SubscrException e) {
                Log.wtf(TAG, "I'm pretty sure MERGE is compatible with the snapshot request!");
            }
        }

        public void setTrigger(Double yVal) {
            stock.setTrigger(yVal);
        }

        public void disable() {
            this.listener.disable();
        }

        @Override
        public HandyTableListener getTableListener() {
            return this.listener;
        }

        @Override
        public SubscribedTableKey getTableKey() {
            return  this.key;
        }

        @Override
        public ExtendedTableInfo getTableInfo() {
            return this.tableInfo;
        }

        @Override
        public void setTableKey(SubscribedTableKey key) {
            this.key = key;
        }

        @Override
        public MpnInfo getMpnInfo() {
            if (this.mpnInfo == null) {
                Map<String, String> data= new HashMap<String, String>();
                data.put("stock_name", "${stock_name}");
                data.put("last_price", "${last_price}");
                data.put("time", "${time}");
                data.put("item", tableInfo.getItems()[0]);
                
                ExtendedTableInfo clone = null;
                try {
                    clone = new ExtendedTableInfo(tableInfo.getItems(), "MERGE", mpnSubscriptionFields , false);
                } catch (SubscrException e) {
                    Log.wtf(TAG, "can't happen");
                }
                clone.setDataAdapter("QUOTE_ADAPTER");
                this.mpnInfo = new MpnInfo(clone,"Stock update",data);
                this.mpnInfo.setDelayWhileIdle("false");
                this.mpnInfo.setTimeToLive("300");
                
                this.mpnInfo.setTriggerExpression(stock.getTrigger());
                
            }
            
            //everything is fixed except the trigger
            this.mpnInfo.setTriggerExpression(stock.getTrigger());
            
            return this.mpnInfo;
        }

        @Override
        public MpnStatusListener getMpnStatusListener() {
            return this.listener;
        }
    }
    
    private class StockListener implements HandyTableListener, MpnStatusListener {
        
        private AtomicBoolean disabled = new AtomicBoolean(false);
        private final Stock stock;
        
        public StockListener(Stock stock) {
            this.stock = stock;
        }
        
        public void disable() {
            disabled.set(true);
        }
        
        @Override
        public void onRawUpdatesLost(int arg0, String arg1, int arg2) {
            Log.wtf(TAG,"Not expecting lost updates");
        }

        @Override
        public void onSnapshotEnd(int itemPos, String itemName) {
            Log.v(TAG,"Snapshot end for " + itemName);
        }

        @Override
        public void onUnsubscr(int itemPos, String itemName) {
            Log.v(TAG,"Unsubscribed " + itemName);
        }

        @Override
        public void onUnsubscrAll() {
            Log.v(TAG,"Unsubscribed all");
        }

        @Override
        public void onUpdate(int itemPos, String itemName, UpdateInfo newData) {
            if (disabled.get()) {
                return;
            }
            Log.v(TAG,"Update for " + itemName);
            this.stock.update(newData,handler);
        }

        @Override
        public void onMpnStatusChanged(final boolean activated, double trigger) {
            if (disabled.get()) {
                return;
            }
            Log.v(TAG,"Mpn status changed");
            this.stock.updateMpnStatus(activated, trigger, handler);
        }
        
    }

}
