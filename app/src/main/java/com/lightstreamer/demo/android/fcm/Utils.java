package com.lightstreamer.demo.android.fcm;

import com.lightstreamer.client.mpn.MpnDeviceListener;

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
        public void onListenStart() {}
        @Override
        public void onListenEnd() {}
    }
    
    public static class VoidClientListener implements com.lightstreamer.client.ClientListener {
        @Override
        public void onListenEnd() {}
        @Override
        public void onListenStart() {}
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
        public void onListenEnd() {}
        @Override
        public void onListenStart() {}
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
        public void onListenEnd() {}
        @Override
        public void onListenStart() {}
        @Override
        public void onPropertyChanged(String propertyName) {}
        @Override
        public void onModificationError(int code, String message, String propertyName) {}
        @Override
        public void onStatusChanged(String status, long timestamp) {}
        @Override
        public void onSubscription() {}
        @Override
        public void onSubscriptionError(int code, String message) {}
        @Override
        public void onUnsubscriptionError(int i, String s) {}
        @Override
        public void onTriggered() {}
        @Override
        public void onUnsubscription() {}
    }
}
