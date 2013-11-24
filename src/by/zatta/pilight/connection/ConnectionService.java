package by.zatta.pilight.connection;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import by.zatta.pilight.MainActivity;
import by.zatta.pilight.R;
import by.zatta.pilight.model.Config;

public class ConnectionService extends Service {

	private static final String TAG = "Zatta::ConnectionService";
	
	@SuppressWarnings("unused")
	private boolean boundToActivity;
	private static Bitmap bmp;
	private static Context ctx;
	private static HeartBeat t = null;
	private static NotificationManager mNotMan;
	private static Notification.Builder builder;
	private boolean isConnectionUp = false;
		 	
	private static List<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SET_INT_VALUE = 3;
	public static final int MSG_SET_STRING_VALUE = 4;
	public static final int MSG_SET_DEVICE = 5;
	public static final int MSG_SET_CONFIG = 6;
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler()); // Target we publish for clients to send messages to IncomingHandler.
	
	private static NotificationType mCurrentNotif;
	private enum NotificationType {
		DESTROYED,
		CONNECTING,
		FAILED,
		CONNECTED,
		LOST_CONNECTION,
		LEFT_NETWORK,
		UPDATE,
	}
	
	
	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		super.onCreate();
		
		ctx = this;
		
		this.registerReceiver(mMessageReceiver, new IntentFilter("my-event"));
				
		bmp = BitmapFactory.decodeResource(getResources(),R.drawable.eye);
				int height = (int) getResources().getDimension(android.R.dimen.notification_large_icon_height);
				int width = (int) getResources().getDimension(android.R.dimen.notification_large_icon_width);
		bmp = Bitmap.createScaledBitmap(bmp, width, height, false); 
		
		mNotMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		isConnectionUp = makeConnection();
			
	}
	
	private boolean makeConnection(){
		Log.d(TAG, "makeConnection called");
		if (ConnectionProvider.INSTANCE.doConnect()) {
			Log.d(TAG, "recieved connection!");
			makeNotification(NotificationType.CONNECTED, null);
			startForeground(35, builder.build());
			getConfig();
			if (t == null || !t.isAlive()){
				t = new HeartBeat();
			 	   t.start();
			}
			return true;
		}else{
			Log.d(TAG, "received connection failure");
			makeNotification(NotificationType.FAILED, null);
			ConnectionProvider.INSTANCE.finishTheWork();
			return false;
		}
	}
	
	private void getConfig(){
		String jsonString = ConnectionProvider.INSTANCE.getConfig();
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("config")) {
				Log.e(TAG, "has config");
				try {
					Config.parse(json.getJSONObject("config"));
					Config.print();
				} catch(JSONException e) {}
			
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private boolean dropConnection(){
		Log.d(TAG, "dropConnection called");
		ConnectionProvider.INSTANCE.finishTheWork();
		try {t.finalize();}	catch (Throwable e) { Log.w(TAG, "couldnt finalize the heart-beat"); }
		isConnectionUp = false;
		return false;
	}
	
	public static void postUpdate(String update){
		sendMessageToUI(java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + update);
		if (update.contains("Lost Connection"))
			makeNotification(NotificationType.LOST_CONNECTION, null);
		else if (update.contains("Reconnection failed"))
			makeNotification(NotificationType.LOST_CONNECTION, null);
		else if (update.contains("Reconnection attempt"))
			makeNotification(NotificationType.CONNECTING, update);
		else
			makeNotification(NotificationType.UPDATE, update);
	}
	
	private static void makeNotification(NotificationType type, String message){
		Intent main;
		PendingIntent sentBroadcast;
		PendingIntent startMainActivity;
		
		String myDate = java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime());
				
		if (type != mCurrentNotif){
			switch (type){
				case DESTROYED:
					builder = new Notification.Builder(ctx);
					builder
						.setSmallIcon(R.drawable.eye)
						.setLargeIcon(bmp)
						.setContentTitle("Pilight")
						.setContentText("service destroyed");
				mCurrentNotif = NotificationType.DESTROYED;
					break;
				case CONNECTING:
					builder = new Notification.Builder(ctx)
		        		.setContentTitle("pilight")
		        		.setContentText("Connecting " + myDate)
		        		.setSmallIcon(R.drawable.eye)
		        		.setLargeIcon(bmp);
					mCurrentNotif = NotificationType.CONNECTING;
					break;
				case CONNECTED:
					main = new Intent(ctx, MainActivity.class);
					main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startMainActivity = PendingIntent.getActivity(ctx, 0, main,	PendingIntent.FLAG_UPDATE_CURRENT);
					
					builder = new Notification.Builder(ctx)
		        		.setContentTitle("pilight")
		        		.setContentText("Connected " + myDate)
		        		.setContentIntent(startMainActivity)
		        		.setSmallIcon(R.drawable.eye)
		        		.setLargeIcon(bmp);
					mCurrentNotif = NotificationType.CONNECTED;
					break;
				case FAILED:
					main = new Intent("my-event");
					sentBroadcast = PendingIntent.getBroadcast(ctx, 0, main,	PendingIntent.FLAG_UPDATE_CURRENT);
					
					builder = new Notification.Builder(ctx)
		        		.setContentTitle("pilight")
		        		.setContentText("Failed " + myDate)
		        		.addAction(R.drawable.refresh, "Try to connect again", sentBroadcast)
		        		.setSmallIcon(R.drawable.eye)
		        		.setLargeIcon(bmp);	
					mCurrentNotif = NotificationType.FAILED;
					break;	
				case LOST_CONNECTION:
					main = new Intent("my-event");
					sentBroadcast = PendingIntent.getBroadcast(ctx, 0, main,	PendingIntent.FLAG_UPDATE_CURRENT);
					
					builder = new Notification.Builder(ctx)
		        		.setContentTitle("pilight")
		        		.setContentText("Lost Connection")
		        		.addAction(R.drawable.refresh, "Try to reconnect", sentBroadcast)
		        		.setSmallIcon(R.drawable.eye)
		        		.setLargeIcon(bmp);
					mCurrentNotif = NotificationType.LOST_CONNECTION;
					break;
				case UPDATE:
					main = new Intent(ctx, MainActivity.class);
					main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startMainActivity = PendingIntent.getActivity(ctx, 0, main,	PendingIntent.FLAG_UPDATE_CURRENT);
					
					builder = new Notification.Builder(ctx)
		        		.setContentTitle(myDate)
		        		.setContentText(message)
		        		.setStyle(new Notification.BigTextStyle().bigText(message))
		        		.setContentIntent(startMainActivity)
		        		.setSmallIcon(R.drawable.eye)
		        		.setLargeIcon(bmp);
					mCurrentNotif = NotificationType.UPDATE;
					break;
				default:
					break;
			}
		} else {
			builder
				.setContentTitle(myDate)
				.setContentText(message)
				.setStyle(new Notification.BigTextStyle().bigText(message));
		}
		mNotMan.notify(35, builder.build());
		
	}
	
	boolean isNetworkConnectionAvailable() { 
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        Log.d(TAG, "network: " + info.getTypeName()+  " + " + info.getExtraInfo());
        return (info.isConnectedOrConnecting());
    }

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		this.unregisterReceiver(mMessageReceiver);
		dropConnection();
		makeNotification(NotificationType.DESTROYED, null);
		stopForeground(false);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e(TAG, "onStartCommand");		
		return START_STICKY;
	}
	
	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind");
		boundToActivity = true;
		return mMessenger.getBinder();
	}

	@Override
	public void onRebind(Intent intent) {
		Log.v(TAG, "onRebind");
		boundToActivity=true;
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		boundToActivity = false;
		super.onUnbind(intent);
		return true;
	}
		
	public class HeartBeat extends Thread {
		boolean lostConnection = false;
		boolean fromProvider;
		long runs = 0;
		
		@Override
		protected void finalize() throws Throwable {
			Log.d(TAG, "Finalize the HEARTBEAT");
			this.interrupt();
			super.finalize();
		}
		
		@Override
		public void interrupt(){
			Log.d(TAG, "interrupted HEARTBEAT");
			super.interrupt();
		}
		
		@Override
		public void run() {
			try {
				while(!lostConnection) {
					fromProvider= ConnectionProvider.INSTANCE.stillConnected();
					Log.d(TAG, "fromProvider: stillConnected= " + fromProvider);
					if(!fromProvider){
						Log.w(TAG, "Lost Connection!!");
						postUpdate("Lost Connection");
						ConnectionProvider.INSTANCE.finishTheWork();
						isConnectionUp = false;
						Log.d(TAG, "dropped connection");
						int attempts = 0;
						while(attempts < 3 && !isConnectionUp){
							Log.d(TAG, "should reconnect");
							postUpdate("Reconnection attempt " + Integer.toString(attempts+1));
							isConnectionUp = makeConnection();
							attempts++;
						}
						Log.d(TAG, "hopefully reconnected");
						attempts = 0;
						if (isConnectionUp){
							postUpdate("Reconnection succesfull");
							lostConnection = false;
						}else{
							postUpdate("Reconnection failed");
							lostConnection = true;
						}
						lostConnection = !isConnectionUp;		
					}
					//runs++; //for testing purposes
					if (runs > 3){
						runs = 0;
						ConnectionProvider.INSTANCE.disturbConnetion();
					}else{
					Thread.sleep(15000);
					}
				} Log.d(TAG, "ended timer thread for heart-beat");
			} catch(Exception e) {
				Log.d(TAG, "something wrong in the heart-beat \n" + e);
			}
			Log.w(TAG, "ended heart-beat");
		}
	}
	
	// handler for received Intents for the "my-event" event 
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	  @Override
	  public void onReceive(Context context, Intent intent) {
	    // Extract data included in the Intent
	    String message = intent.getStringExtra("message");
	    Log.d("receiver", "Got message: " + message);
	    
	    makeNotification(NotificationType.CONNECTING, null);
	    
	    if (isConnectionUp) dropConnection();
	    else isConnectionUp = makeConnection();	    
	  }
	};
	
	/**
	 * Handle incoming messages from MainActivity
	 */
	@SuppressLint("HandlerLeak")
	private class IncomingMessageHandler extends Handler { // Handler of incoming messages from clients.
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG,"handleMessage: " + msg.what);
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SET_INT_VALUE:
				//incrementBy = msg.arg1;
				Log.d(TAG, "Ontvangen van activity: "+ Integer.toString(msg.arg1));
				switch (msg.arg1){
				case 1:
					try {
						Log.d(TAG, "heart-beat is null: " + (t == null));
					} catch (Exception e) { Log.d(TAG, "writer nullness could not be determined");	}
					try {
						Log.d(TAG, "heart-beat is alive: " + (t.isAlive()));
					} catch (Exception e) { Log.d(TAG, "writer aliveliness could not be determined");	}
					
					
					break;
				case 10:
					if (t == null || !t.isAlive()){
						t = new HeartBeat();
							Log.d(TAG, "starting haert-beat");
					 	   t.start();
					} else {
						Log.d(TAG, "hart-beat not null & alive");
					}
					break;
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/**
	 * Send the data to all clients.
	 * @param message The message to send.
	 */
	private static void sendMessageToUI(String message) {
		//Log.d(TAG, "sent message called, clients attached: " + Integer.toString(mClients.size()));
		Iterator<Messenger> messengerIterator = mClients.iterator();		
		while(messengerIterator.hasNext()) {
			Messenger messenger = messengerIterator.next();
			try {
				// Send data as an Integer				
				messenger.send(Message.obtain(null, MSG_SET_INT_VALUE, 1337, 0));

				// Send data as a String
				Bundle bundle = new Bundle();
				bundle.putString("str1", message);
				Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
				msg.setData(bundle);
				messenger.send(msg);

			} catch (RemoteException e) {
				//Log.d(TAG, "mClients.remove called");
				mClients.remove(messenger);
			}
		}
	}
	
}