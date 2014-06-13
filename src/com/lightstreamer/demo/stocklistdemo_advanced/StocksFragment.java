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

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.lightstreamer.ls_client.ExtendedTableInfo;
import com.lightstreamer.ls_client.HandyTableListener;
import com.lightstreamer.ls_client.SubscrException;
import com.lightstreamer.ls_client.SubscribedTableKey;
import com.lightstreamer.ls_client.UpdateInfo;

public class StocksFragment extends SubscriptionFragment {
    
    onStockSelectedListener listener;
    
    public interface onStockSelectedListener {
        /** Called by HeadlinesFragment when a list item is selected */
        public void onStockSelected(int item);
    }
    
    private static final String TAG = "StocksFragment";
    
    private final static String[] items = {"item1", "item2", "item3",
            "item4", "item5", "item6", "item7", "item8", "item9", "item10",
            "item11", "item12", "item13", "item14", "item15"};
    ArrayList<Stock> list;

    public final static String[] fields = {"stock_name", "last_price", "time"};
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {

        return inflater.inflate(R.layout.list_view, container, false);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layout = R.layout.row_layout;

        list = new ArrayList<Stock>(items.length);
        for (int i = 0; i < items.length; i++) {
            list.add(new Stock(items[i]));
        }
        
        setListAdapter(new StocksAdapter(getActivity(), layout, list));
        
        this.setSubscription(new MainSubscription());
    }
    
    
    @Override
    public void onStart() {
        super.onStart();

        if (getFragmentManager().findFragmentById(R.id.details_fragment) != null) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        
    }
    
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            listener = (onStockSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
        
        
        
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Notify the parent activity of selected item
        listener.onStockSelected(position+1);
        
        // Set the item as checked to be highlighted when in two-pane layout
        getListView().setItemChecked(position, true);
    }
    
    
    
    
    private class MainSubscription implements Subscription, HandyTableListener {

        private SubscribedTableKey key;
        private ExtendedTableInfo tableInfo;
        
        public MainSubscription() {
            try {
                this.tableInfo = new ExtendedTableInfo(items, "MERGE", fields , true);
                this.tableInfo.setDataAdapter("QUOTE_ADAPTER");
                this.tableInfo.setRequestedMaxFrequency(1);
            } catch (SubscrException e) {
                Log.wtf(TAG, "I'm pretty sure MERGE is compatible with the snapshot request!");
            }
        }

        @Override
        public HandyTableListener getTableListener() {
            return this;
        }

        @Override
        public SubscribedTableKey getTableKey() {
            return key;
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
            Log.v(TAG,"Update for " + itemName);
        }
        
    }

}
