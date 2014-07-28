package com.lightstreamer.demo.stocklistdemo_advanced;

import java.util.ArrayList;

import android.os.Handler;
import android.util.Log;
import android.widget.ListView;

import com.lightstreamer.ls_client.ExtendedTableInfo;
import com.lightstreamer.ls_client.HandyTableListener;
import com.lightstreamer.ls_client.SubscrException;
import com.lightstreamer.ls_client.SubscribedTableKey;
import com.lightstreamer.ls_client.UpdateInfo;

class MainSubscription implements Subscription, HandyTableListener {

    private static final String TAG = "MainSubscription";
    
    private SubscribedTableKey key;
    private ExtendedTableInfo tableInfo;
   

    private ArrayList<StockForList> list;

    private Context context = new Context();
    
    public MainSubscription(ArrayList<StockForList> list) {
        this.list = list;
        try {
            this.tableInfo = new ExtendedTableInfo(StocksFragment.items, "MERGE", StocksFragment.subscriptionFields , true);
            this.tableInfo.setDataAdapter("QUOTE_ADAPTER");
            this.tableInfo.setRequestedMaxFrequency(1);
        } catch (SubscrException e) {
            Log.wtf(TAG, "I'm pretty sure MERGE is compatible with the snapshot request!");
        }
    }
    
    public void changeContext(Handler handler, ListView listView) {
        this.context .handler = handler;
        this.context.listView = listView;
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
        final StockForList toUpdate = list.get(itemPos-1);
        toUpdate.update(newData,this.context);
    }
    
    
    public class Context {
        public Handler handler;
        public ListView listView;
    }
    
}