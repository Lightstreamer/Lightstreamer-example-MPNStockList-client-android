package com.lightstreamer.demo.stocklistdemo_advanced;

import com.lightstreamer.demo.stocklistdemo_advanced.StocksAdapter.StockHolder;

public class Stock {

	//var fieldsList = ["last_price", "time", "pct_change", "bid_quantity", "bid", "ask", "ask_quantity", "min", "max", "ref_price", "open_price", "stock_name", 
	
	StockHolder holder = null;
	
	private String stockName = "N/A";
	private String lastPrice = "N/A";
	private String time = "N/A";
	
	public Stock(String item) {
		this.stockName = item;
	}

	public void setHolder(StockHolder holder) {
		holder.stock_name.setText(this.stockName);
	    holder.last_price.setText(this.lastPrice);
	    holder.time.setText(this.time);
	}
	
	public void setStockName(String stockName) {
		this.stockName = stockName;
		if (holder != null) {
		    holder.stock_name.setText(this.stockName);
		}
	}

	public void setLastPrice(String lastPrice) {
		this.lastPrice = lastPrice;
		if (holder != null) {
		    holder.last_price.setText(this.lastPrice);
		}
	}

	public void setTime(String time) {
		this.time = time;
		if (holder != null) {
		    holder.time.setText(this.time);
		}
	}

	
	
	
}
