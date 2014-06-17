package com.lightstreamer.demo.stocklistdemo_advanced;

import java.text.DecimalFormat;

import android.os.Handler;
import android.view.View;
import android.widget.ListView;

import com.lightstreamer.demo.stocklistdemo_advanced.StocksAdapter.RowHolder;
import com.lightstreamer.ls_client.UpdateInfo;

public class StockForList {
    
    private String stockName;
    private String lastPrice;
    private double lastPriceNum;
    private String time;
    
    private int stockNameColor = R.color.background;
    private int lastPriceColor = R.color.background;
    private int timeColor = R.color.background; 
    
    private int pos;
    private TurnOffRunnable turningOff;
    
    private DecimalFormat format = new DecimalFormat("#.00");

    
    public StockForList(String item, int pos) {
        this.pos = pos;
    }
    
    public void update(UpdateInfo newData, Handler handler, final ListView listView) {
        boolean isSnapshot = newData.isSnapshot();
        if (newData.isValueChanged("stock_name")) {
            stockName = newData.getNewValue("stock_name");
            stockNameColor = isSnapshot ? R.color.snapshot_highlight : R.color.higher_highlight;
        }
        if (newData.isValueChanged("time")) {
            time = newData.getNewValue("time");
            timeColor = isSnapshot ? R.color.snapshot_highlight : R.color.higher_highlight;
        }
        if (newData.isValueChanged("last_price")) {
            double newPrice = Double.parseDouble(newData.getNewValue("last_price"));
            lastPrice = format.format(newPrice);
            
            if (isSnapshot) {
                lastPriceColor = R.color.snapshot_highlight;
            } else {
                lastPriceColor = newPrice < lastPriceNum ? R.color.lower_highlight : R.color.higher_highlight;
                lastPriceNum = newPrice;
            }
        }
        
        if (this.turningOff != null) {
            this.turningOff.disable();
        }
        
        
        
        handler.post(new Runnable() {

            @Override
            public void run() {
                RowHolder holder = extractHolder(listView);
                if (holder != null) {
                    fill(holder);
                }
            }
            
        });
        
        this.turningOff = new TurnOffRunnable(listView);
        handler.postDelayed(this.turningOff,600);
    }
    

    public void fill(RowHolder holder) {
        holder.stock_name.setText(stockName);
        holder.last_price.setText(lastPrice);
        holder.time.setText(time);
        
        this.fillColor(holder);
    }
    
    public void fillColor(RowHolder holder) {
        holder.stock_name.setBackgroundResource(stockNameColor);
        holder.last_price.setBackgroundResource(lastPriceColor);
        holder.time.setBackgroundResource(timeColor);
    }

    RowHolder extractHolder(ListView listView) {
        View row = listView.getChildAt(pos - listView.getFirstVisiblePosition());
        if(row == null) {
            return null;
        }
        return (RowHolder) row.getTag();
    }
    
    
    private class TurnOffRunnable implements Runnable {

        private boolean valid = true;
        private ListView listView;
        
        public TurnOffRunnable(ListView listView) {
            this.listView = listView;
        }
        
        public synchronized void disable() {
            valid = false;
        }

        @Override
        public synchronized void run() {
            if (!valid) {
                return;
            }
            stockNameColor = R.color.transparent;
            lastPriceColor = R.color.transparent;
            timeColor = R.color.transparent;
            
            
            RowHolder holder = extractHolder(listView);
            if(holder != null) {
                fillColor(holder);
            }
        }
        
        
        
    }
    
}