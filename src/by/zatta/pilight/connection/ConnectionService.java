/******************************************************************************************
 * 
 * Copyright (C) 2013 Zatta
 * 
 * This file is part of pilight for android.
 * 
 * pilight for android is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by the 
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * pilight for android is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with pilightfor android.
 * If not, see <http://www.gnu.org/licenses/>
 * 
 * Copyright (c) 2013 pilight project
 ********************************************************************************************/

package by.zatta.pilight.connection;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import by.zatta.pilight.MainActivity;
import by.zatta.pilight.R;
import by.zatta.pilight.model.Config;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.Origin;
import by.zatta.pilight.model.OriginEntry;

public class ConnectionService extends Service {

	public static NotificationType mCurrentNotif = NotificationType.DESTROYED;

	public static final int MSG_REGISTER_CLIENT = 1912376432;
	public static final int MSG_SET_BUNDLE = 1927364723;
	public static final int MSG_SET_STATUS = 1098378957;
	public static final int MSG_SWITCH_DEVICE = 1755547587;
	public static final int MSG_UNREGISTER_CLIENT = 1873487903;
	private static Context aCtx;
	private static Context ctx;

	public static List<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients (activities,
																			// widget:))
	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	private static NotificationManager mNotMan;
	private static Notification.Builder builder;
	private static HeartBeat t = null;
	private static final String TAG = "Zatta::ConnectionService";
	private boolean isConnectionUp = false;

	public enum NotificationType {
		CONNECTED, CONNECTING, DESTROYED, FAILED, LOST_CONNECTION, UPDATE,
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
			String action = intent.getAction();
			// Log.v("receiver", "Got inent action: " + action);

			if (action.equals("pilight-kill-service")) {
				// Log.v(TAG, "kill recieved");
				stopSelf();
			} else if (action.equals("pilight-reconnect")) {
				// Log.v(TAG, "kill recieved");
				makeNotification(NotificationType.CONNECTING, "reconnecting...");
				if (isConnectionUp)
					dropConnection();
				else isConnectionUp = makeConnection();
			}
		}
	};
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler()); // Target we publish for clients to send messages to
																						// IncomingHandler.

	/**
	 * When binding to the service, we return an interface to our messenger for sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// Log.v(TAG, "onBind");
		return mMessenger.getBinder();
	}

	@Override
	public void onCreate() {
		// Log.v(TAG, "onCreate");
		super.onCreate();
		ctx = this;
		aCtx = getApplicationContext();

		IntentFilter filter = new IntentFilter();
		filter.addAction("pilight-reconnect");
		filter.addAction("pilight-kill-service");
		this.registerReceiver(mMessageReceiver, filter);

		mNotMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		isConnectionUp = makeConnection();

	}

	@Override
	public void onDestroy() {
		// Log.e(TAG, "onDestroy");
		this.unregisterReceiver(mMessageReceiver);
		sendMessageToUI(MSG_SET_STATUS, NotificationType.DESTROYED.name());
		dropConnection();
		mNotMan.cancel(35);
		Toast.makeText(aCtx, "service destroyed", Toast.LENGTH_LONG).show();
		super.onDestroy();
	}

	@Override
	public void onRebind(Intent intent) {
		// Log.v(TAG, "onRebind");
		super.onRebind(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Log.v(TAG, "onStartCommand");
		if ((mCurrentNotif == NotificationType.FAILED || mCurrentNotif == NotificationType.LOST_CONNECTION)
				&& !(mCurrentNotif == NotificationType.CONNECTING)) {
			isConnectionUp = makeConnection();
		}
		return START_STICKY;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// Log.v(TAG, "onUnbind");
		super.onUnbind(intent);
		return true;
	}

	private boolean dropConnection() {
		// Log.v(TAG, "dropConnection called");
		ConnectionProvider.INSTANCE.finishTheWork();
		try {
			t.finalize();
		} catch (Throwable e) {
			// Log.w(TAG, "couldnt finalize the heart-beat");
		}
		isConnectionUp = false;
		return false;
	}

	private void getConfig() {
		String jsonString = ConnectionProvider.INSTANCE.getConfig();
		try {
			JSONObject json = new JSONObject(jsonString);
			if (json.has("config")) {
				// Log.e(TAG, "has config");
				try {
					mDevices = Config.getDevices(json.getJSONObject("config"));
					Collections.sort(mDevices);
				} catch (JSONException e) {
				}
			}
		} catch (JSONException e) {
			Log.w(TAG, "problems in JSONning");
		}
	}

	private boolean addedToPreferences() {
		String currentNetwork = null;
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (!(info == null)) {
			// Log.d(TAG, "networkInfo: " + info.getExtraInfo());
			currentNetwork = info.getExtraInfo();
		}

		WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (!(wifiInfo == null)) {
			// Log.d(TAG, "wifiInfo:" + wifiInfo.getSSID());
			currentNetwork = wifiInfo.getSSID();
		}
		if (currentNetwork == null) return false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(aCtx);
		String previous = prefs.getString("networks_known", "");
		// Log.d(TAG, previous);
		currentNetwork = currentNetwork.replace("\"", "");
		if (previous.contains(currentNetwork)) {
			// Log.d(TAG, previous + " did contain " + currentNetwork);
			return false;
		} else {
			previous = previous + "|&|" + currentNetwork;
			// Log.d(TAG, previous);
			Editor edit = prefs.edit();
			edit.putString("networks_known", previous);
			edit.commit();
			return true;
		}
	}

	private boolean makeConnection() {
		// makeNotification(NotificationType.CONNECTING, null);
		// Log.v(TAG, "makeConnection called");
		if (ConnectionProvider.INSTANCE.doConnect()) {
			// Log.v(TAG, "recieved connection!");
			addedToPreferences();
			makeNotification(NotificationType.CONNECTED, "Connected");
			startForeground(35, builder.build());
			getConfig();
			if (t == null || !t.isAlive()) {
				t = new HeartBeat();
				t.start();
			}
			return true;
		} else {
			// Log.w(TAG, "received connection failure");
			makeNotification(NotificationType.FAILED, "Connecting Failed");
			ConnectionProvider.INSTANCE.finishTheWork();
			return false;
		}
	}

	public static void postUpdate(String update) {
		if (update.contains("origin\":\"config")) {
			try {
				OriginEntry originEntry = Origin.getOriginEntry(new JSONObject(update));
				mDevices = Config.update(originEntry);
				update = Config.getLastUpdateString();
				Log.v(TAG, update.replace("\n", " "));
			} catch (JSONException e) {
				Log.w(TAG, "iets mis met het jsonObject?");
			}
		}
		makeNotification(NotificationType.UPDATE, makeNiceUpdate(update));
		sendMessageToUI(MSG_SET_BUNDLE, java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + update);
	}

	private static String makeNiceUpdate(String update) {
		if (update.contains("temperature") || update.contains("humidity")) {

		}
		return update;
	}

	private static Bitmap bigPic(int png) {
		Bitmap mBitmap = BitmapFactory.decodeResource(aCtx.getResources(), png);
		int height = (int) aCtx.getResources().getDimension(android.R.dimen.notification_large_icon_height);
		int width = (int) aCtx.getResources().getDimension(android.R.dimen.notification_large_icon_width);
		mBitmap = Bitmap.createScaledBitmap(mBitmap, width / 3, height / 3, false);
		return mBitmap;
	}

	private static void makeNotification(NotificationType type, String message) {
		Intent main;
		Intent kill;
		PendingIntent sentBroadcast;
		PendingIntent startMainActivity;
		PendingIntent killService;

		String myDate = java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime());

		// Log.v(TAG, "setting notification: " + type.name());
		if (type != mCurrentNotif) {
			// Log.v(TAG, "setting NEW notification: " + type.name());
			sendMessageToUI(MSG_SET_STATUS, type.name());
			switch (type)
			{
			case DESTROYED:
				builder = new Notification.Builder(ctx);
				builder.setSmallIcon(R.drawable.eye_black).setLargeIcon(bigPic(R.drawable.eye_black)).setContentTitle("pilight")
						.setContentText(message);
				mCurrentNotif = NotificationType.DESTROYED;
				break;
			case CONNECTING:
				kill = new Intent("pilight-kill-service");
				killService = PendingIntent.getBroadcast(ctx, 0, kill, PendingIntent.FLAG_UPDATE_CURRENT);

				builder = new Notification.Builder(ctx).setContentTitle("pilight").setContentText(message)
						.setDeleteIntent(killService).setSmallIcon(R.drawable.eye_trans)
						.setLargeIcon(bigPic(R.drawable.eye_trans));
				mCurrentNotif = NotificationType.CONNECTING;
				break;
			case CONNECTED:
				main = new Intent(ctx, MainActivity.class);
				main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startMainActivity = PendingIntent.getActivity(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);

				builder = new Notification.Builder(ctx).setContentTitle("pilight").setContentText(message + "\n" + myDate)
						.setContentIntent(startMainActivity).setSmallIcon(R.drawable.eye_white)
						.setLargeIcon(bigPic(R.drawable.eye_white));
				mCurrentNotif = NotificationType.CONNECTED;
				break;
			case FAILED:
				main = new Intent("pilight-reconnect");
				sentBroadcast = PendingIntent.getBroadcast(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);
				kill = new Intent("pilight-kill-service");
				killService = PendingIntent.getBroadcast(ctx, 0, kill, PendingIntent.FLAG_UPDATE_CURRENT);

				builder = new Notification.Builder(ctx).setContentTitle("pilight").setContentText("Failed " + "\n" + myDate)
						.setDeleteIntent(killService).addAction(R.drawable.action_refresh, "Try to connect again", sentBroadcast)
						.setSmallIcon(R.drawable.eye_trans).setLargeIcon(bigPic(R.drawable.eye_trans));
				mCurrentNotif = NotificationType.FAILED;
				break;
			case LOST_CONNECTION:
				main = new Intent("pilight-reconnect");
				sentBroadcast = PendingIntent.getBroadcast(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);
				kill = new Intent("pilight-kill-service");
				killService = PendingIntent.getBroadcast(ctx, 0, kill, PendingIntent.FLAG_UPDATE_CURRENT);

				builder = new Notification.Builder(ctx).setContentTitle("pilight").setContentText("Lost Connection")
						.setDeleteIntent(killService).addAction(R.drawable.action_refresh, "Try to reconnect", sentBroadcast)
						.setSmallIcon(R.drawable.eye_trans).setLargeIcon(bigPic(R.drawable.eye_trans));
				mCurrentNotif = NotificationType.LOST_CONNECTION;
				break;
			case UPDATE:
				main = new Intent(ctx, MainActivity.class);
				main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startMainActivity = PendingIntent.getActivity(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);

				builder = new Notification.Builder(ctx).setContentTitle(myDate).setContentText(message)
						.setStyle(new Notification.BigTextStyle().bigText(message)).setContentIntent(startMainActivity)
						.setSmallIcon(R.drawable.eye_white).setLargeIcon(bigPic(R.drawable.eye_white));
				mCurrentNotif = NotificationType.UPDATE;
				break;
			default:
				break;
			}
		} else {
			if (message != null) {
				builder.setContentTitle(myDate).setStyle(new Notification.BigTextStyle().bigText(message));
				builder.setContentText(message);
			}
		}
		mNotMan.notify(35, builder.build());
	}

	/**
	 * Send the data to all clients.
	 * 
	 * @param message
	 *            The message to send.
	 */
	private static void sendMessageToUI(int what, String message) {
		// Log.v(TAG, "sent message called, clients attached: " + Integer.toString(mClients.size()));
		Iterator<Messenger> messengerIterator = mClients.iterator();
		while (messengerIterator.hasNext()) {
			Messenger messenger = messengerIterator.next();
			try {
				Bundle bundle = new Bundle();
				bundle.setClassLoader(aCtx.getClassLoader());
				switch (what)
				{
				case MSG_SET_STATUS:
					// Log.v(TAG, "setting status: " + message);
					bundle.putString("status", message);
					Message msg_string = Message.obtain(null, MSG_SET_STATUS);
					msg_string.setData(bundle);
					messenger.send(msg_string);
					break;
				case MSG_SET_BUNDLE:
					if (!mDevices.isEmpty()) {
						// Log.v(TAG, "putting mDevices");
						bundle.putParcelableArrayList("config", (ArrayList<? extends Parcelable>) mDevices);
						Message msg = Message.obtain(null, MSG_SET_BUNDLE);
						msg.setData(bundle);
						messenger.send(msg);
					}
					break;
				}
			} catch (RemoteException e) {
				Log.w(TAG, "mClients.remove called");
				mClients.remove(messenger);
			}
		}
	}

	/**
	 * Handle incoming messages from MainActivity
	 */
	@SuppressLint("HandlerLeak")
	private class IncomingMessageHandler extends Handler { // Handler of incoming messages from clients.
		@Override
		public void handleMessage(Message msg) {
			// Log.v(TAG, "handleMessage: " + msg.what);
			switch (msg.what)
			{
			case MSG_REGISTER_CLIENT:
				mClients.clear(); // Bit hackery, I admit
				mClients.add(msg.replyTo);
				sendMessageToUI(MSG_SET_STATUS, mCurrentNotif.name());
				if (!mDevices.isEmpty()) sendMessageToUI(MSG_SET_BUNDLE, null);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SWITCH_DEVICE:
				ConnectionProvider.INSTANCE.sendCommand(msg.getData().getString("command"));
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public class HeartBeat extends Thread {
		boolean fromProvider;
		boolean lostConnection = false;
		long runs = 0;

		@Override
		public void interrupt() {
			// Log.v(TAG, "interrupted HEARTBEAT");
			super.interrupt();
		}

		@Override
		public void run() {
			try {
				while (!lostConnection) {
					fromProvider = ConnectionProvider.INSTANCE.stillConnected();
					// Log.v(TAG, "fromProvider: stillConnected= " + fromProvider);
					if (!fromProvider) {
						Log.d(TAG, "HEART-BEAT Lost Connection!!");
						ConnectionProvider.INSTANCE.finishTheWork();
						isConnectionUp = false;
						// Log.d(TAG, "dropped connection");
						int attempts = 0;
						while (attempts < 3 && !isConnectionUp) {
							// Log.v(TAG, "should reconnect");
							makeNotification(NotificationType.CONNECTING, "Reconnection attempt " + Integer.toString(attempts));
							isConnectionUp = makeConnection();
							attempts++;
						}
						// Log.d(TAG, "hopefully reconnected");
						attempts = 0;
						if (isConnectionUp) {
							makeNotification(NotificationType.CONNECTED, "connected");
							lostConnection = false;
						} else {
							stopForeground(true);
							makeNotification(NotificationType.FAILED, "Reconnection failed");
							isConnectionUp = false;
							lostConnection = true;
						}
						lostConnection = !isConnectionUp;
					}
					// runs++; //for testing purposes
					if (runs > 5) {
						runs = 0;
						ConnectionProvider.INSTANCE.disturbConnetion();
					} else {
						Thread.sleep(15000);
					}
				}
				// Log.v(TAG, "ended timer thread for heart-beat");
			} catch (Exception e) {
				Log.w(TAG, "something wrong in the heart-beat \n" + e);
			}
			// Log.w(TAG, "ended heart-beat");
			mNotMan.cancel(35);
		}

		@Override
		protected void finalize() throws Throwable {
			// Log.v(TAG, "Finalize the HEARTBEAT");
			this.interrupt();
			super.finalize();
		}
	}
}