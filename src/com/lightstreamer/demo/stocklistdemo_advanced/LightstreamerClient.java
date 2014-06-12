package com.lightstreamer.demo.stocklistdemo_advanced;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;

import com.lightstreamer.ls_client.ConnectionInfo;
import com.lightstreamer.ls_client.ConnectionListener;
import com.lightstreamer.ls_client.LSClient;
import com.lightstreamer.ls_client.PushConnException;
import com.lightstreamer.ls_client.PushServerException;
import com.lightstreamer.ls_client.PushUserException;


public class LightstreamerClient {
	
	public interface StatusChangeListener {
		public void onStatusChange(int status);
	}
	
	private static final String TAG = "LSConnection";
	
	public static final int STALLED = 4;
	public static final int STREAMING = 2;
	public static final int POLLING = 3;
	public static final int DISCONNECTED = 0;
	public static final int CONNECTING = 1;
	public static final int WAITING = 5;
	
	private static String statusToString(int status){
		switch(status) {
		
			case STALLED: {
				return "STALLED";
			}
			case STREAMING: {
				return "STREAMING";
			}
			case POLLING: {
				return "POLLING";
			}
			case DISCONNECTED: {
				return "DISCONNECTED";
			}
			case CONNECTING: {
				return "CONNECTING";
			}
			case WAITING: {
				return "WAITING";
			}
			default: {
				return "Unexpected";
			}
		
		}
	}
	
	

    private AtomicBoolean expectingConnected = new AtomicBoolean(false);
    private boolean connected = false; // do not get/set this outside the eventsThread
    
    final private ExecutorService eventsThread = Executors.newSingleThreadExecutor();
    
    final private ConnectionInfo cInfo = new ConnectionInfo();
    final private LSClient client = new LSClient();
    private ClientListener currentListener = null;
    
	private StatusChangeListener statusListener;
    
    public LightstreamerClient(String pushServerUrl, StatusChangeListener statusListener) {
    	 this.cInfo.pushServerUrl = pushServerUrl;
    	 this.cInfo.adapter = "DEMO";
    	 
    	 this.statusListener = statusListener;
    }
    
    public void start() {
    	Log.d(TAG,"Connection enabled");
    	if (expectingConnected.compareAndSet(false,true)) {
    		this.startConnectionThread();
    	}
    }
    
    public void stop() {
    	Log.d(TAG,"Connection disabled");
    	if (expectingConnected.compareAndSet(true,false)) {
    		this.startConnectionThread();
    	}
    }    
    
    
    private void startConnectionThread() {
    	eventsThread.execute(new ConnectionThread());
    }
    
    //ClientListener calls it through eventsThread
    private void changeStatus(ClientListener caller, boolean connected, int status) {
    	if (caller != this.currentListener) {
    		return;
    	}
    	
    	Log.i(TAG,statusToString(status)); 
    	this.statusListener.onStatusChange(status);
    	
    	this.connected = connected;
    	
    	
    	if (connected != expectingConnected.get()) {
    		this.startConnectionThread();
    	}
    }
    
    
    private class ConnectionThread implements Runnable {
        public void run() {
            //expectingConnected can be changed by outside events
            
            while(connected != expectingConnected.get()) { 
                if (!connected) {
                	Log.i(TAG,statusToString(CONNECTING));
                	statusListener.onStatusChange(CONNECTING);
                	try {
                		currentListener = new ClientListener();
                		client.openConnection(cInfo, currentListener);
                		Log.d(TAG,"Connecting success");
                		connected = true;
                	} catch (PushConnException e) {
                		Log.v(TAG, e.getMessage());
                    } catch (PushServerException e) {
                    	Log.v(TAG, e.getMessage());
                    } catch (PushUserException e) {
                    	Log.v(TAG, e.getMessage());
                    }
                	
                	if (!connected) {
                		try {
                			Log.i(TAG,statusToString(WAITING));
                			statusListener.onStatusChange(WAITING);
                			Log.d(TAG,"Connecting failure: will retry");
							Thread.sleep(5000);
						} catch (InterruptedException e) {
						}
                	}
                	
                } else {
                	Log.v(TAG,"Disconnecting");
                	client.closeConnection();
                	statusListener.onStatusChange(DISCONNECTED);
                	Log.i(TAG,statusToString(DISCONNECTED)); 
                	currentListener = null;
                	connected = false;
                }
            	
            }     
        }
    }
    
    private class ConnectionEvent implements Runnable {

    	private final ClientListener caller;
		private final boolean connected;
		private final int status;

		public ConnectionEvent(ClientListener caller, boolean connected, int status) {
    		this.caller = caller;
    		this.connected = connected;
    		this.status = status;
    	}
    	
		@Override
		public void run() {
			changeStatus(this.caller, this.connected, this.status);
		}
    	
    }
    
    
    private class ClientListener implements ConnectionListener {

    	private final long randomId;
		private int lastConnectionStatus;

		public ClientListener() {
			randomId = Math.round(Math.random()*1000);
		}
		
		@Override
		public void onActivityWarning(boolean warn) {
			Log.d(TAG,randomId + " onActivityWarning " + warn);
			if (warn) {
				eventsThread.execute(new ConnectionEvent(this,true,STALLED));
			} else {
				eventsThread.execute(new ConnectionEvent(this,true,this.lastConnectionStatus));
			}
			
		}

		@Override
		public void onClose() {
			Log.d(TAG,randomId + " onClose");
			eventsThread.execute(new ConnectionEvent(this,false,DISCONNECTED));
		}

		@Override
		public void onConnectionEstablished() {
			Log.d(TAG,randomId + " onConnectionEstablished");
		}

		@Override
		public void onDataError(PushServerException pse) {
			Log.d(TAG,randomId + " onDataError: " + pse.getErrorCode() + " -> " + pse.getMessage());
		}

		@Override
		public void onEnd(int cause) {
			Log.d(TAG,randomId + " onEnd " + cause);
		}

		@Override
		public void onFailure(PushServerException pse) {
			Log.d(TAG,randomId + " onFailure: " + pse.getErrorCode() + " -> " + pse.getMessage());
		}

		@Override
		public void onFailure(PushConnException pce) {
			Log.d(TAG,randomId + " onFailure: " + pce.getMessage());
		}

		@Override
		public void onNewBytes(long num) {
			Log.v(TAG,randomId + " onNewBytes " + num);
		}

		@Override
		public void onSessionStarted(boolean isPolling) {
			Log.d(TAG,randomId + " onSessionStarted; isPolling: " + isPolling);
			if (isPolling) {
				this.lastConnectionStatus = POLLING;
			} else {
				this.lastConnectionStatus = STREAMING;
			}
			eventsThread.execute(new ConnectionEvent(this,true,this.lastConnectionStatus));
			
		}
    	
    }
}
