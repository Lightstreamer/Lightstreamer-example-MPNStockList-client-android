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
package com.lightstreamer.demo.android;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    
    private Map<Double,FixedYSeries> fixedLines = new HashMap<Double,FixedYSeries>();
    private Map<Double,FixedYSeries> tempFixedLines = new HashMap<Double,FixedYSeries>();
    
    LineAndPointFormatter fixedRedLineFormatter;
    LineAndPointFormatter fixedGreenLineFormatter;
    
    private Series series;
    private XYPlot dynamicPlot;
    
    DecimalFormat df = new DecimalFormat("00");
    
    double maxY = 0; 
    double minY = 0;
    
    private final static int MAX_SERIES_SIZE = 40;
    
    public Chart() {
        this.series = new Series();
    }
    
    private void adjustToFixedLine(Map<Double,FixedYSeries> fixedLines) {
        synchronized(this.fixedLines) {
            for(Map.Entry<Double,FixedYSeries> entry : fixedLines.entrySet()) {
                FixedYSeries fixedLine = entry.getValue();
                if (fixedLine.size() > 0) {
                    double fixed = fixedLine.getFixed(); 
                    if (fixed > maxY) {
                        maxY = fixed+0.1;
                    } else if (fixedLine.getFixed() < minY) {
                        minY = fixed-0.1;
                    }
                }
            }
        }
    }
    
    private void adjustYBoundaries() {
        //default positioning puts the origin on the bottom, we want it on the center
        double min = minY;
        double max = maxY;
        
        adjustToFixedLine(fixedLines);
        adjustToFixedLine(tempFixedLines);
        
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
    
    private FixedYSeries moving;
    
    public void setMovingTriggerLine(double trigger) {
        if (moving == null) {
            moving = new FixedYSeries();
            this.dynamicPlot.addSeries(moving, fixedGreenLineFormatter);
        }
        moving.fix(trigger);
        this.redraw();
    }
    
    public void endMovingTriggerLine(double trigger) {
        synchronized(this.fixedLines) {
            this.dynamicPlot.removeSeries(moving);
            moving = null;
            FixedYSeries fixedLine = fixedLines.get(trigger);
            if (fixedLine != null) {
                return;
            }
            addFixedLine(tempFixedLines,trigger,fixedGreenLineFormatter);
        }
        this.redraw();
    }
    
    public void setTriggerLine(double trigger) {
        synchronized(this.fixedLines) {
            removeFixedLine(tempFixedLines,trigger);
            addFixedLine(fixedLines,trigger,fixedRedLineFormatter);
        }
        this.redraw();
    }
    
    public void removeTriggerLine(double trigger) {
        synchronized(this.fixedLines) {
            removeFixedLine(tempFixedLines,trigger);
            removeFixedLine(fixedLines,trigger);
        }
        this.redraw();
    }
    
    private void addFixedLine(Map<Double, FixedYSeries> fixedLines,
            double trigger, LineAndPointFormatter formatter) {
        
        FixedYSeries fixedLine = fixedLines.get(trigger);
        if (fixedLine == null) {
            fixedLine = new FixedYSeries();
            fixedLine.fix(trigger);
            fixedLines.put(trigger, fixedLine);
            this.dynamicPlot.addSeries(fixedLine, formatter);
        } 
    }

    private void removeFixedLine(Map<Double, FixedYSeries> fixedLines,
            double trigger) {
        FixedYSeries fixedLine = fixedLines.remove(trigger);
        if (fixedLine != null) {
            this.dynamicPlot.removeSeries(fixedLine);
        }
    }

    
    
    public void onResume(Context context) {
        PixelUtils.init(context);
        
        int line = context.getResources().getColor(R.color.chart_line);
        LineAndPointFormatter formatter = new LineAndPointFormatter(line, line, null, null);
        this.dynamicPlot.addSeries(series, formatter);
        
        int red = context.getResources().getColor(R.color.lower_highlight);
        fixedRedLineFormatter = new LineAndPointFormatter(red, null, null, null);
        
        
        int green = context.getResources().getColor(R.color.higher_highlight);
        fixedGreenLineFormatter = new LineAndPointFormatter(green, null, null, null);
        
       
        this.clean();
        
    }
    
    public void onPause() {
        this.dynamicPlot.removeSeries(series);
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
        
        synchronized(this.fixedLines) {
            Iterator<Map.Entry<Double,FixedYSeries>> cleanIterator = fixedLines.entrySet().iterator();
            while(cleanIterator.hasNext()) {
                this.dynamicPlot.removeSeries(cleanIterator.next().getValue());
                cleanIterator.remove();
            }
            cleanIterator = tempFixedLines.entrySet().iterator();
            while(cleanIterator.hasNext()) {
                this.dynamicPlot.removeSeries(cleanIterator.next().getValue());
                cleanIterator.remove();
            }
        }
        
        
        maxY = 0;
        minY = 0;
        this.redraw();
    }
    
    private void redraw() {
        if (this.dynamicPlot != null) {
            this.adjustYBoundaries();
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
    }
    
    private void onFirstPoint(double newPrice) {
        minY = newPrice-1;
        if (minY < 0) {
            minY = 0;
        }
        maxY = newPrice+1;
    }
    
    private class FixedYSeries implements XYSeries {
        
        private double fixedY = 0;
        
        public void fix(double fixed) {
            this.fixedY = fixed;
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
