package com.lightstreamer.demo.stocklistdemo_advanced;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class StocksAdapter extends ArrayAdapter<Stock> {

	private Activity activity; 
	
	
	public StocksAdapter(Activity activity, int layout, ArrayList<Stock> list) {
		super(activity,layout,list);
        this.activity = activity;
    }
	
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		StockHolder holder = null;
		
        if(row == null)
        {
            LayoutInflater inflater = this.activity.getLayoutInflater();
            row = inflater.inflate(R.layout.row_layout, parent, false);
            
            holder = new StockHolder();
            holder.stock_name = (TextView)row.findViewById(R.id.stock_name);
            holder.last_price = (TextView)row.findViewById(R.id.last_price);
            holder.time = (TextView)row.findViewById(R.id.time);
            
            row.setTag(holder);
        }
        else
        {
            holder = (StockHolder)row.getTag();
        }
        
        Stock stock = getItem(position);
        stock.setHolder(holder);
        
        return row;
	}
	
	protected static class StockHolder {
		protected TextView stock_name;
		protected TextView last_price;
        protected TextView time;
    }
	
}
