package com.lightstreamer.demo.stocklistdemo_advanced;

import com.lightstreamer.demo.stocklistdemo_advanced.LightstreamerClient.MpnStatusListener;
import com.lightstreamer.ls_client.ExtendedTableInfo;
import com.lightstreamer.ls_client.HandyTableListener;
import com.lightstreamer.ls_client.SubscribedTableKey;
import com.lightstreamer.ls_client.mpn.MpnInfo;
import com.lightstreamer.ls_client.mpn.MpnKey;

public interface Subscription {

    public HandyTableListener getTableListener();

    public SubscribedTableKey getTableKey();

    public ExtendedTableInfo getTableInfo();

    public void setTableKey(SubscribedTableKey key);
    
    public MpnInfo getMpnInfo();
    
    public void setMpnInfoKey(MpnKey key);
    
    public MpnStatusListener getMpnStatusListener();

}
