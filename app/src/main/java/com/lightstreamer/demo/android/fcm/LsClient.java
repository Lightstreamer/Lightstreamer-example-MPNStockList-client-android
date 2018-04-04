/*
 * Copyright (c) Lightstreamer Srl
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
package com.lightstreamer.demo.android.fcm;

import static com.lightstreamer.demo.android.fcm.Utils.TAG;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lightstreamer.client.LightstreamerClient;
import com.lightstreamer.client.Subscription;
import com.lightstreamer.client.mpn.MpnSubscription;
import com.lightstreamer.client.mpn.android.MpnDevice;

import android.content.Context;
import android.util.Log;

import javax.annotation.Nonnull;

/**
 * Singleton class embedding a {@link LightstreamerClient}.
 */
public class LsClient {
    
    public static final LsClient instance = new LsClient();
    
    private final AtomicBoolean expectingConnected = new AtomicBoolean(false);
    
    private volatile LightstreamerClient client;

    private volatile StatusChangeListener statusListener;

    private volatile Status status = Status.DISCONNECTED;

    private volatile Context context;

    private volatile String senderId;
    
    private LsClient() {}

    public void initClient(String pushServerUrl, String adapterName, Context context, String senderId) {
        Log.i(TAG, "Init Lightstreamer client");
        client = new com.lightstreamer.client.LightstreamerClient(pushServerUrl, adapterName);
        client.addListener(new Utils.VoidClientListener() {
            @Override
            public void onStatusChange(String status) {
                switch (status) {
                    case "CONNECTING":
                        setStatus(Status.CONNECTING);
                        break;
                    case "CONNECTED:STREAM-SENSING":
                        setStatus(Status.CONNECTING);
                        break;
                    case "DISCONNECTED":
                        setStatus(Status.DISCONNECTED);
                        break;
                    case "DISCONNECTED:WILL-RETRY":
                        setStatus(Status.DISCONNECTED);
                        break;
                    case "CONNECTED:HTTP-STREAMING":
                        setStatus(Status.STREAMING);
                        break;
                    case "CONNECTED:WS-STREAMING":
                        setStatus(Status.STREAMING);
                        break;
                    case "CONNECTED:HTTP-POLLING":
                        setStatus(Status.POLLING);
                        break;
                    case "CONNECTED:WS-POLLING":
                        setStatus(Status.POLLING);
                        break;
                    case "DISCONNECTED:TRYING-RECOVERY":
                        setStatus(Status.DISCONNECTED);
                        break;
                    case "STALLED":
                        setStatus(Status.STALLED);
                        break;
                    default:
                        Log.wtf(TAG, "Received unexpected connection status: " + status);
                }
            }
        });
        this.context = context;
        this.senderId = senderId;
    }
    
    public void subscribe(Subscription sub) {
        client.subscribe(sub);
    }
    
    public void subscribe(MpnSubscription sub) {
        client.subscribe(sub, true);
    }
    
    public void unsubscribe(Subscription sub) {
        client.unsubscribe(sub);
    }
    
    public void unsubscribe(MpnSubscription sub) {
        client.unsubscribe(sub);
    }
    
    public List<MpnSubscription> getMpnSubscriptions() {
        return client.getMpnSubscriptions("ALL");
    }
    
    public void setListener(StatusChangeListener statusListener) {
        this.statusListener = statusListener;
    }
    
    public synchronized void start() {
        Log.d(TAG, "Connecting to server...");
        if (expectingConnected.compareAndSet(false,true)) {
            setStatus(Status.CONNECTING);
            client.connect();
            Log.i(TAG, "Creating MPN device");
            MpnDevice.create(context, senderId, new MpnDevice.MpnDeviceCreationListener() {
                @Override
                public void onSuccess(@Nonnull MpnDevice device) {
                    device.addListener(new Utils.VoidMpnDeviceListener() {
                        @Override
                        public void onRegistrationFailed(int code, String message) {
                            Log.d(TAG, "Can't register MPN ID, push notifications are disabled: " + message);
                        }

                        @Override
                        public void onRegistered() {
                            Log.d(TAG,"MPN ID registered");
                        }
                    });
                    client.registerForMpn(device);
                }

                @Override
                public void onFailure(@Nonnull Exception e) {
                    Log.e(TAG, "Can't obtain a token", e);
                }
            });
        }
    }
    
    public synchronized void stop(boolean applyPause) {
        Log.d(TAG, "Disconnecting from server...");
        if (expectingConnected.compareAndSet(true,false)) {
            setStatus(Status.DISCONNECTED);
            client.disconnect();
        }
    }
    
    public Status getStatus() {
        return status;
    }

    private void setStatus(Status status) {
        this.status = status;
        Log.i(TAG, "Client status: " + this.status); 
        if (this.statusListener != null) {
            this.statusListener.onStatusChange(this.status);
        }
    }
    
    public static enum Status {
        STALLED,
        STREAMING,
        POLLING,
        DISCONNECTED,
        CONNECTING,
        WAITING
    }
    
    public interface StatusChangeListener {
        public void onStatusChange(Status status);
    }
}
