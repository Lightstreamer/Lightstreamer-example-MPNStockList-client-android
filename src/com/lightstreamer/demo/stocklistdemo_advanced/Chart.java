package com.lightstreamer.demo.stocklistdemo_advanced;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    
    private Series series;
    private XYPlot dynamicPlot;
    private LineAndPointFormatter formatter;
    
    DecimalFormat df = new DecimalFormat("00");
    
    float maxY = 0; 
    float minY = 0;
   
    public Chart() {
        this.series = new Series();
    }
    
    private void adjustYBoundaries() {
        //default positioning puts the origin on the bottom, we want it on the center
        dynamicPlot.setRangeBoundaries(minY, maxY, BoundaryMode.FIXED);
    }
    
    public void setPlot(XYPlot dynamicPlot) {
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
            
            dynamicPlot.setDomainValueFormat(new Format() {

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
                    // TODO Auto-generated method stub
                    return null;
                }
                
            });
        }
    }
    
    public void onResume(Context context) {
        PixelUtils.init(context);
        
        int line = context.getResources().getColor(R.color.chart_line);
        this.formatter = new LineAndPointFormatter(line, null, null, null);
        this.dynamicPlot.addSeries(series, formatter);
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
    
    private void onYOverflow(float last) {
        //TODO currently never shrinks
        float shift = 1;
        if (last > maxY) {
          float newMax = maxY + shift;
          if (last > newMax) {
            newMax = last;
          }

          this.maxY = newMax;

        } else if (last < minY) {
          float newMin = minY - shift;
          if (last < newMin) {
            newMin = last;
          }
          
          this.minY = newMin;
        }
        this.adjustYBoundaries();
    }
    
    private void onFirstPoint(float newPrice) {
        minY = newPrice-1;
        if (minY < 0) {
            minY = 0;
        }
        maxY = newPrice+1;
        this.adjustYBoundaries();
    }
    
    
    private class Series implements XYSeries {

        
        ArrayList<Number> prices = new ArrayList<Number>();
        ArrayList<Number> times = new ArrayList<Number>();
     
        @Override
        public String getTitle() {
            return "";
        }

        public void add(String time, String lastPrice) {
            if (prices.size() >= 40) {
                prices.remove(0);
                times.remove(0);
            }
            
            float newPrice = Float.parseFloat(lastPrice);
            
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
    
}
