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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.lightstreamer.client.LightstreamerClient;
import com.lightstreamer.client.Subscription;
import com.lightstreamer.client.mpn.MpnSubscription;
import com.lightstreamer.client.mpn.MpnDevice;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.lightstreamer.demo.android.fcm.Utils.TAG;

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

    private volatile Activity activity;

    private volatile Deferred<MpnDevice, Object, Object> deferredDevice;
    
    private LsClient() {}

    public void initClient(String pushServerUrl, String adapterName, Context context, Activity activity) {
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
        this.activity = activity;
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

    public Promise<MpnDevice, Object, Object> getMpnDevice() {
        return deferredDevice.promise();
    }
    
    public synchronized void start() {
        deferredDevice = new DeferredObject<>();
        Log.d(TAG, "Connecting to server...");
        if (expectingConnected.compareAndSet(false,true)) {
            setStatus(Status.CONNECTING);
            client.connect();
            Log.i(TAG, "Creating MPN device");

            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                @Override
                public void onComplete(final Task<InstanceIdResult> task) {
                    if (task.isSuccessful()) {
                        MpnDevice device = new MpnDevice(context, task.getResult().getToken());
                        device.addListener(new Utils.VoidMpnDeviceListener() {
                            @Override
                            public void onRegistrationFailed(final int code, final String message) {
                                Log.d(TAG, "Can't register MPN, push notifications are disabled: " + message);
                                showAlert("Can't register push notifications: " + code + " - " + message);
                            }

                            @Override
                            public void onRegistered() {
                                Log.d(TAG,"MPN registered");
                            }
                        });
                        client.registerForMpn(device);
                        deferredDevice.resolve(device);
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Can't obtain a token", e);
                        showAlert("Can't obtain a token: " + e);
                        deferredDevice.reject(e);
                    }
                }
            });
        }
    }

    void showAlert(final String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage(msg);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
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
