package com.lightstreamer.demo.stocklistdemo_advanced;

import java.util.HashMap;

import android.os.Handler;
import android.widget.TextView;

import com.lightstreamer.ls_client.UpdateInfo;

public class Stock {

    //var fieldsList = ["last_price", "time", "pct_change", "bid_quantity", "bid", "ask", "ask_quantity", "min", "max", "ref_price", "open_price", "stock_name", 
    
    HashMap<String,TextView> holder = null;
    private HashMap<String,UpdateRunnable> turnOffRunnables = new HashMap<String,UpdateRunnable>();

    private String[] numericFields;
    private String[] otherFields;

    
    public Stock(String item, String[] numericFields, String[] otherFields) {
        this.numericFields = numericFields;
        this.otherFields = otherFields;
    }
    
    public void setHolder(HashMap<String,TextView> holder) {
        this.holder = holder;
        
        this.resetHolder(holder, numericFields);
        this.resetHolder(holder, otherFields);

    }
    
    private void resetHolder(HashMap<String,TextView> holder, String[] fields) {
        for (int i=0; i<fields.length; i++) {
            
            TextView field = holder.get(fields[i]);
            if (field != null) {
                field.setText("N/A");
            }
            
        }
    }
    
    
    public void update(UpdateInfo newData, Handler handler) {
        this.updateView(newData, handler, numericFields, true);
        this.updateView(newData, handler, otherFields, false);
    }
    
    private void updateView(UpdateInfo newData, Handler handler, String[] fields, boolean numeric) {
        boolean snapshot = newData.isSnapshot();
        for (int i=0; i<fields.length; i++) {
            
            if (newData.isValueChanged(fields[i])) {
                String value = newData.getNewValue(fields[i]);
                TextView field = holder.get(fields[i]);
                
                if (field != null) {
                    
                    double upDown = 0.0;
                    int color;
                    if (!snapshot ) {
                        // update cell color 
                        if (numeric) {
                            String oldValue = newData.getOldValue(fields[i]);
                            try {
                                double valueInt = Double.parseDouble(value);
                                double oldValueInt = Double.parseDouble(oldValue);
                                upDown = valueInt - oldValueInt;
                            } catch (NumberFormatException nfe) {
                                //unexpected o_O
                            }
                        }
                        
                        if (upDown < 0) {
                            color = R.color.lower_highlight; 
                        } else {
                            color = R.color.higher_highlight; 
                        }
                       
                    } else {
                        color = R.color.snapshot_highlight;
                    }
                    
                   
                    
                    UpdateRunnable turnOff = turnOffRunnables.get(fields[i]);
                    if (turnOff != null) {
                        turnOff.invalidate();
                    }
                    turnOff = new UpdateRunnable(field,null,R.color.transparent);
                    this.turnOffRunnables.put(fields[i], turnOff);
                    
                    handler.post(new UpdateRunnable(field,value,color));
                    handler.postDelayed(turnOff, 600);
                }
                
            }
        }
    }
    
    
    
    private class UpdateRunnable implements Runnable {
        private int background;
        private TextView view;
        private String text;
        private boolean valid = true;

        UpdateRunnable(TextView view, String text, int background) {
            this.view = view;
            this.text = text;
            this.background = background;
        }

        public synchronized void run() {
            if (this.valid) {
                if (this.text != null) {
                    view.setText(text);
                }
                view.setBackgroundResource(background);
                view.invalidate();
            }
        }

        public synchronized void invalidate() {
            this.valid = false;
        }
    }

    
    
    
}
