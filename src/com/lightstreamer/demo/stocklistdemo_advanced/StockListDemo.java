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


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;


public class StockListDemo extends FragmentActivity implements StocksFragment.onStockSelectedListener {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
	 
	 
	 public void onStockSelected(int item) {

	        DetailsFragment detailsFrag = (DetailsFragment)
	                getSupportFragmentManager().findFragmentById(R.id.details_fragment);

	        if (detailsFrag != null) {
	          
	        	detailsFrag.updateStocksView(item);

	        } else {
	            // If the frag is not available, we're in the one-pane layout and must swap frags...

	            // Create fragment and give it an argument for the selected article
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
