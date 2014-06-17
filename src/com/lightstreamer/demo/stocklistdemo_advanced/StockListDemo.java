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


import com.lightstreamer.demo.stocklistdemo_advanced.LightstreamerClient.LightstreamerClientProxy;
import com.lightstreamer.demo.stocklistdemo_advanced.LightstreamerClient.StatusChangeListener;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;


public class StockListDemo extends ActionBarActivity implements StocksFragment.onStockSelectedListener, StatusChangeListener, LightstreamerClientProxy {

    private boolean userDisconnect = false;
    private LightstreamerClient lsClient = new LightstreamerClient("http://push.lightstreamer.com",this);
    
    private static final String TAG = "StockListDemo";
    
    private Handler handler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.handler = new Handler();
        
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.HONEYCOMB) {
            hideActionBarTitle();//TODO do it with styles
        }
        
        setContentView(R.layout.stocks);

        if (findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            StocksFragment firstFragment = new StocksFragment();

            firstFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();
        }
        
        
        
        
    }
    
    @Override
    public void onStop() {
        super.onStop();
        this.stop();
    }
    
    @Override 
    public void onStart() {
        super.onStart();
        if (!userDisconnect) {
            this.start();
        }
    }
    
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void hideActionBarTitle() {
        getActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.v(TAG,"Switch button: " + this.userDisconnect);
        menu.findItem(R.id.start).setVisible(this.userDisconnect);
        menu.findItem(R.id.stop).setVisible(!this.userDisconnect);
        
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       
        switch (item.getItemId()) {
            case R.id.stop:
                Log.i(TAG,"Stop");
                this.userDisconnect = true;
                supportInvalidateOptionsMenu();
                this.stop();
                return true;
            case R.id.start:
                Log.i(TAG,"Start");
                this.userDisconnect = false;
                supportInvalidateOptionsMenu();
                this.start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onStockSelected(int item) {

        DetailsFragment detailsFrag = (DetailsFragment)
                getSupportFragmentManager().findFragmentById(R.id.details_fragment);

        if (detailsFrag != null) {
          
            detailsFrag.updateStocksView(item);

        } else {
            DetailsFragment newFragment = new DetailsFragment();
            Bundle args = new Bundle();
            args.putInt(DetailsFragment.ARG_ITEM, item);
            newFragment.setArguments(args);
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
     
     
    //Status handling

    @Override
    public void onStatusChange(int status) {
        handler.post(new StatusChange(status));
        
    }
    
    private class StatusChange implements Runnable {

        private int status;

        public StatusChange(int status) {
            this.status = status;
        }

        @Override
        public void run() {
            ImageView statusIcon = (ImageView) findViewById(R.id.status_image);
            
            switch(status) {
            
                case LightstreamerClient.STALLED: {
                    statusIcon.setContentDescription(getResources().getString(R.string.status_stalled));
                    statusIcon.setImageResource(R.drawable.status_stalled);
                    break;
                }
                case LightstreamerClient.STREAMING: {
                    statusIcon.setContentDescription(getResources().getString(R.string.status_streaming));
                    statusIcon.setImageResource(R.drawable.status_connected_streaming);
                    break;
                }
                case LightstreamerClient.POLLING: {
                    statusIcon.setContentDescription(getResources().getString(R.string.status_polling));
                    statusIcon.setImageResource(R.drawable.status_connected_polling);
                    break;
                }
                case LightstreamerClient.DISCONNECTED: {
                    statusIcon.setContentDescription(getResources().getString(R.string.status_disconnected));
                    statusIcon.setImageResource(R.drawable.status_disconnected);
                    break;
                }
                case LightstreamerClient.CONNECTING: {
                    statusIcon.setContentDescription(getResources().getString(R.string.status_connecting));
                    statusIcon.setImageResource(R.drawable.status_disconnected);
                    break;
                }
                case LightstreamerClient.WAITING: {
                    statusIcon.setContentDescription(getResources().getString(R.string.status_waiting));
                    statusIcon.setImageResource(R.drawable.status_disconnected);
                    break;
                }
                default: {
                    Log.wtf(TAG, "Recevied unexpected connection status: " + status);
                    return;
                }
            }
        }
        
    }

    @Override
    public void start() {
        this.lsClient.start();
    }

    @Override
    public void stop() {
        this.lsClient.stop();
    }

    @Override
    public void addSubscription(Subscription sub) {
        this.lsClient.addSubscription(sub);
    }

    @Override
    public void removeSubscription(Subscription sub) {
        this.lsClient.removeSubscription(sub);
    }
    
    
    
}
