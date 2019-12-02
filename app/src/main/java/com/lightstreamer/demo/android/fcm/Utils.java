package com.lightstreamer.demo.android.fcm;

import com.lightstreamer.client.mpn.MpnDeviceInterface;
import com.lightstreamer.client.mpn.MpnDeviceListener;
import com.lightstreamer.client.mpn.MpnSubscription;

import javax.annotation.Nullable;

public class Utils {
    
    public static final String TAG = "LsDemo";
    
    public static class VoidMpnDeviceListener implements MpnDeviceListener {
        @Override
        public void onRegistrationFailed(int code, String message) {}
        @Override
        public void onRegistered() {}
        @Override
        public void onSuspended() {}
        @Override
        public void onSubscriptionsUpdated() {}
        @Override
        public void onStatusChanged(String status, long timestamp) {}
        @Override
        public void onResumed() {}
        @Override
        public void onListenStart(MpnDeviceInterface device) {}
        @Override
        public void onListenEnd(MpnDeviceInterface device) {}
    }
    
    public static class VoidClientListener implements com.lightstreamer.client.ClientListener {
        @Override
        public void onListenEnd(com.lightstreamer.client.LightstreamerClient client) {}
        @Override
        public void onListenStart(com.lightstreamer.client.LightstreamerClient client) {}
        @Override
        public void onServerError(int errorCode, String errorMessage) {}
        @Override
        public void onStatusChange(String status) {}
        @Override
        public void onPropertyChange(String property) {}
    }
    
    public static class VoidSubscriptionListener implements com.lightstreamer.client.SubscriptionListener {
        @Override
        public void onClearSnapshot(String itemName, int itemPos) {}
        @Override
        public void onCommandSecondLevelItemLostUpdates(int lostUpdates, String key) {}
        @Override
        public void onCommandSecondLevelSubscriptionError(int code, String message, String key) {}
        @Override
        public void onEndOfSnapshot(String itemName, int itemPos) {}
        @Override
        public void onItemLostUpdates(String itemName, int itemPos, int lostUpdates) {}
        @Override
        public void onItemUpdate(com.lightstreamer.client.ItemUpdate itemUpdate) {}
        @Override
        public void onListenEnd(com.lightstreamer.client.Subscription subscription) {}
        @Override
        public void onListenStart(com.lightstreamer.client.Subscription subscription) {}
        @Override
        public void onSubscription() {}
        @Override
        public void onSubscriptionError(int code, String message) {}
        @Override
        public void onUnsubscription() {}
        @Override
        public void onRealMaxFrequency(String frequency) {}
    }
    
    public static class VoidMpnSubscriptionListener implements com.lightstreamer.client.mpn.MpnSubscriptionListener {
        @Override
        public void onListenEnd(MpnSubscription client) {}
        @Override
        public void onListenStart(MpnSubscription client) {}
        @Override
        public void onPropertyChanged(String propertyName) {}
        @Override
        public void onStatusChanged(String status, long timestamp) {}
        @Override
        public void onSubscription() {}
        @Override
        public void onSubscriptionError(int code, String message) {}
        @Override
        public void onUnsubscriptionError(int i, @Nullable String s) {}
        @Override
        public void onTriggered() {}
        @Override
        public void onUnsubscription() {}
    }
}
