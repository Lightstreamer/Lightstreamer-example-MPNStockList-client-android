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


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class StockListDemo extends ActionBarActivity implements StocksFragment.onStockSelectedListener {

    private boolean userDisconnect = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void hideActionBarTitle() {
        getActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.start).setVisible(this.userDisconnect);
        menu.findItem(R.id.stop).setVisible(!this.userDisconnect);
        
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       
        switch (item.getItemId()) {
            case R.id.stop:
                this.userDisconnect = true;
                supportInvalidateOptionsMenu();
                return true;
            case R.id.start:
                this.userDisconnect = false;
                supportInvalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
     
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

    
}
