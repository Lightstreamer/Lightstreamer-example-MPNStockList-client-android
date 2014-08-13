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

import android.os.Handler;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.lightstreamer.ls_client.UpdateInfo;

public class Stock {

    //var fieldsList = ["last_price", "time", "pct_change", "bid_quantity", "bid", "ask", "ask_quantity", "min", "max", "ref_price", "open_price", "stock_name", 
    
    private HashMap<String,TextView> holder = null;
    private HashMap<String,UpdateRunnable> turnOffRunnables = new HashMap<String,UpdateRunnable>();

    private String[] numericFields;
    private String[] otherFields;
    private Chart chart;
    
    private String trigger;
    private double lastPrice; //might improve by saving all the field values
    
    
    public static final String TRIGGER_HEAD = "Double.parseDouble(${last_price})";
    public static final String TRIGGER_LT = "<=";
    public static final String TRIGGER_GT = ">=";
    
    private ToggleButton mpnStatus;
    

    
    public Stock(String item, String[] numericFields, String[] otherFields) {
        this.numericFields = numericFields;
        this.otherFields = otherFields;
    }
    
    public void setHolder(HashMap<String,TextView> holder) { //UI thread
        this.holder = holder;
        
        this.resetHolder(holder, numericFields);
        this.resetHolder(holder, otherFields);

    }
    
    private void resetHolder(HashMap<String,TextView> holder, String[] fields) {  //UI thread
        for (int i=0; i<fields.length; i++) {
            
            TextView field = holder.get(fields[i]);
            if (field != null) {
                field.setText("N/A");
            }
            
        }
    }
    
    public void setChart(Chart chart) { //UI thread
        this.chart = chart;
        this.chart.clean();
    }
    
    public void setToggle(ToggleButton mpnStatus) { //UI thread
        this.mpnStatus = mpnStatus;
        changeToggleStatus(false);
    }
    
    private void changeToggleStatus(boolean active) { //UI thread
        if (this.mpnStatus != null) {
            mpnStatus.setChecked(active);
        }
    }
    
    public void updateMpnStatus(final boolean active, double trigger, Handler handler) {
        handler.post(new Runnable() {
            public void run() {
                changeToggleStatus(active);
            }
        });
        
        setTrigger(trigger);
    }
    
    public void setTrigger(Double yVal) {
        if (yVal == this.lastPrice) {
            //invalid!
            yVal = -1.0;
        }
        chart.setTriggerLine(yVal);
        
        if (yVal < 0) {
            this.trigger = null;
        } else {
            this.trigger = TRIGGER_HEAD;
            if (yVal < this.lastPrice) {
                this.trigger += TRIGGER_LT; 
            } else {
                this.trigger += TRIGGER_GT;
            }
            
            this.trigger += Double.toString(yVal);
        }
    }
    
    public String getTrigger() {
        return this.trigger;
    }
    
    public void update(UpdateInfo newData, Handler handler) {
        this.updateView(newData, handler, numericFields, true);
        this.updateView(newData, handler, otherFields, false);
        
        //save lastPrice
        String value = newData.getNewValue("last_price");
        try {
            this.lastPrice = Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            //unexpected o_O
        }
        
        
        chart.addPoint(newData);
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
                                double valueNum = Double.parseDouble(value);
                                double oldValueNum = Double.parseDouble(oldValue);
                                upDown = valueNum - oldValueNum;
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
