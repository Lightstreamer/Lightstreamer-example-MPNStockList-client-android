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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Color;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import com.lightstreamer.ls_client.UpdateInfo;

public class Chart {
    
    private FixedYSeries fixedLine;
    private Series series;
    private XYPlot dynamicPlot;
    
    DecimalFormat df = new DecimalFormat("00");
    
    double maxY = 0; 
    double minY = 0;
    
    private final static int MAX_SERIES_SIZE = 40;
    
    public Chart() {
        this.series = new Series();
        this.fixedLine = new FixedYSeries();
    }
    
    private void adjustYBoundaries() {
        //default positioning puts the origin on the bottom, we want it on the center
        double min = minY;
        double max = maxY;
        
        if (fixedLine.size() > 0) {
            double fixed = fixedLine.getFixed(); 
            if (fixed > maxY) {
                maxY = fixed+0.1;
            } else if (fixedLine.getFixed() < minY) {
                minY = fixed-0.1;
            }
        }
        
        dynamicPlot.setRangeBoundaries(min, max, BoundaryMode.FIXED);
    }
    
    public void setPlot(final XYPlot dynamicPlot) {
        if (this.dynamicPlot != dynamicPlot) {
            this.dynamicPlot = dynamicPlot;
            dynamicPlot.setDomainStep(XYStepMode.SUBDIVIDE, 4);
            dynamicPlot.setRangeStep(XYStepMode.SUBDIVIDE, 5);
            dynamicPlot.getLegendWidget().setVisible(false);
            
            dynamicPlot.getBackgroundPaint().setColor(Color.BLACK);
            dynamicPlot.getGraphWidget().getBackgroundPaint().setColor(Color.BLACK);
            dynamicPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
            
            dynamicPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.WHITE);
            dynamicPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.WHITE);

            this.adjustYBoundaries();
            
            dynamicPlot.setDomainValueFormat(new FormatDateLabel());
        }
    }
    
    public void setTriggerLine(double trigger) {
        if (trigger < 0) {
            fixedLine.deactivate();
        } else {
            fixedLine.fix(trigger);
        }
        this.redraw();
    }
    
    public void onResume(Context context) {
        PixelUtils.init(context);
        
        int line = context.getResources().getColor(R.color.chart_line);
        LineAndPointFormatter formatter = new LineAndPointFormatter(line, null, null, null);
        this.dynamicPlot.addSeries(series, formatter);
        
        LineAndPointFormatter fixedLineFormatter = new LineAndPointFormatter(Color.RED, null, null, null);
        this.dynamicPlot.addSeries(fixedLine, fixedLineFormatter);
        
    }
    
    public void addPoint(String time,String lastPrice) {
        series.add(time,lastPrice);
        this.redraw();
    }
    
    public void addPoint(UpdateInfo newData) {
        String lastPrice = newData.getNewValue("last_price");
        String time = newData.getNewValue("time");
        this.addPoint(time,lastPrice);
    }
    
    public void clean() {
        this.series.reset();
        maxY = 0;
        minY = 0;
        this.adjustYBoundaries();
        this.redraw();
    }
    
    private void redraw() {
        if (this.dynamicPlot != null) {
            this.dynamicPlot.redraw();
        }
    }
    
    private void onYOverflow(double last) {
        //TODO currently never shrinks
        int shift = 1;
        if (last > maxY) {
            double newMax = maxY + shift;
          if (last > newMax) {
            newMax = last;
          }

          this.maxY = newMax;

        } else if (last < minY) {
            double newMin = minY - shift;
          if (last < newMin) {
            newMin = last;
          }
          
          this.minY = newMin;
        }
        this.adjustYBoundaries();
    }
    
    private void onFirstPoint(double newPrice) {
        minY = newPrice-1;
        if (minY < 0) {
            minY = 0;
        }
        maxY = newPrice+1;
        this.adjustYBoundaries();
    }
    
    private class FixedYSeries implements XYSeries {
        
        private boolean alive = false;
        
        private double fixedY = 0;
        
        public void fix(double fixed) {
            this.fixedY = fixed;
            this.alive = true;
        }
        
        public void deactivate() {
            this.alive = false;
        }
        
        @Override
        public String getTitle() {
            return "";
        }

        @Override
        public Number getX(int index) {
            Number min = dynamicPlot.getCalculatedMinX();
            if (index == 0) {
                return min;
            } else {
                Number res = dynamicPlot.getCalculatedMaxX();
                if (res == min) {
                    res = res.intValue()+1;
                }
                return res;
            }
            
        }

        @Override
        public Number getY(int index) {
            return this.getFixed();
        }
        
        public double getFixed() {
            return this.fixedY;
        }

        @Override
        public int size() {
            if (!alive) {
                return 0;
            }
            
            return 2;
        }
        
        
    }
    
    private class Series implements XYSeries {

        
        ArrayList<Number> prices = new ArrayList<Number>();
        ArrayList<Number> times = new ArrayList<Number>();
     
        @Override
        public String getTitle() {
            return "";
        }

        public void add(String time, String lastPrice) {
            if (prices.size() >= MAX_SERIES_SIZE) {
                prices.remove(0);
                times.remove(0);
            }
            
            double newPrice = Double.parseDouble(lastPrice);
            
            if (prices.size() == 0) {
                onFirstPoint(newPrice);
            }
            
            if (newPrice < minY || newPrice > maxY) {
                onYOverflow(newPrice);
            }
            
            prices.add(newPrice);
            
            String[] pieces = time.split(":");
            int intTime = Integer.parseInt(pieces[0])*60*60 + Integer.parseInt(pieces[1])*60 + Integer.parseInt(pieces[2]);
            times.add(intTime);
            
        }

        public void reset() {
            prices.clear();
            times.clear();
        }

        @Override
        public Number getX(int index) {
            return times.get(index);
        }

        @Override
        public Number getY(int index) {
            return prices.get(index);
        }

        @Override
        public int size() {
            return prices.size();
        }
        
    }

    @SuppressWarnings("serial")
    private class FormatDateLabel extends Format {
        @Override
        public StringBuffer format(Object object, StringBuffer buffer,
                FieldPosition field) {
            Number num = (Number) object;
            
            int val = num.intValue();
            
            buffer.append(df.format((long) TimeUnit.SECONDS.toHours(val)));
            buffer.append(':');
            buffer.append(df.format((long) TimeUnit.SECONDS.toMinutes(val) % 60));
            buffer.append(':');
            buffer.append(df.format(val%60));
            
            
            return buffer;
        }

        @Override
        public Object parseObject(String string, ParsePosition position) {
            return null;
        }
    }
    
    
}
