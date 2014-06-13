package com.lightstreamer.demo.stocklistdemo_advanced;

import java.util.ArrayList;
import java.util.HashMap;

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
    
    
    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        HashMap<String,TextView> holder;
        
        if(row == null) {
            LayoutInflater inflater = this.activity.getLayoutInflater();
            row = inflater.inflate(R.layout.row_layout, parent, false);
            
            holder = new HashMap<String,TextView>();
            holder.put("stock_name",(TextView)row.findViewById(R.id.stock_name));
            holder.put("last_price",(TextView)row.findViewById(R.id.last_price));
            holder.put("time",(TextView)row.findViewById(R.id.time));
            
            row.setTag(holder);
            Stock stock = getItem(position);
            stock.setHolder(holder);
        } else {
            holder = (HashMap<String,TextView>)row.getTag();
        }
        
        
        return row;
    }
    
}
