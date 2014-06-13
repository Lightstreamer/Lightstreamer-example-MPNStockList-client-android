package com.lightstreamer.demo.stocklistdemo_advanced;

import com.lightstreamer.ls_client.ExtendedTableInfo;
import com.lightstreamer.ls_client.HandyTableListener;
import com.lightstreamer.ls_client.SubscribedTableKey;

public interface Subscription {

    public HandyTableListener getTableListener();

    public SubscribedTableKey getTableKey();

    public ExtendedTableInfo getTableInfo();

    public void setTableKey(SubscribedTableKey key);

}
