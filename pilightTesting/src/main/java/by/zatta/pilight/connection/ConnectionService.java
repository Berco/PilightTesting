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
import android.content.res.Configuration;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import by.zatta.pilight.MainActivity;
import by.zatta.pilight.R;
import by.zatta.pilight.model.Config;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.Origin;
import by.zatta.pilight.model.OriginEntry;

public class ConnectionService extends Service {

	public static final int MSG_REGISTER_CLIENT = 1912376432;
	public static final int MSG_SET_BUNDLE = 1927364723;
	public static final int MSG_SET_STATUS = 1098378957;
	public static final int MSG_SWITCH_DEVICE = 1755547587;
	public static final int MSG_UNREGISTER_CLIENT = 1873487903;
	private static final String TAG = "Zatta::ConnectionService";
	public static NotificationType mCurrentNotif = NotificationType.DESTROYED;
	public static List<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients (activities,
	private static Context aCtx;
	private static Context ctx;
	// widget:))
	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	private static NotificationManager mNotMan;
	private static Notification.Builder builder;
	private static HeartBeat mHB = null;
	private static long timeBeat = new Date().getTime();
	private static WriteMonitor mWM = null;
	private static boolean isConnectionUp = false;
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

				stopForeground(false);
				makeNotification(NotificationType.CONNECTING, aCtx.getString(R.string.noti_reconnect));
				if (isConnectionUp)
					dropConnection();
				else isConnectionUp = makeConnection();
			} else if (action.equals("pilight-switch-device")) {
				if (isConnectionUp) Log.v(TAG, "broadcastReceiver: " + intent.getStringExtra("command"));
				Server.CONNECTION.sentCommand(intent.getStringExtra("command"));
			}
		}
	};
	private static boolean isDestroying;
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler()); // Target we publish for clients to send messages to

	private static boolean dropConnection() {
		Log.v(TAG, "dropConnection called");
		try {
			mHB.interrupt();
		} catch (Throwable e) {
			Log.w(TAG, "couldnt interrupt the heart-beat");
		}
		try {
			mWM.interrupt();
		} catch (Throwable e) {
			Log.w(TAG, "couldnt interrupt the WriteMonitor");
		}
		mHB = null;
		mWM = null;
		Server.CONNECTION.disconnect();
		isConnectionUp = false;
		return false;
	}
	// IncomingHandler.

	public static void postUpdate(String update) {
		timeBeat = new Date().getTime();
		Log.w(TAG, update);
		if (update.contains("origin\":\"update")) {
			try {
				OriginEntry originEntry = Origin.getOriginEntry(new JSONObject(update));
				mDevices = Config.update(originEntry);
				update = Config.getLastUpdateString();
				Log.i(TAG, update.replace("\n", " "));
			} catch (JSONException e) {
				Log.w(TAG, "iets mis met het jsonObject?");
			}
			makeNotification(NotificationType.UPDATE, update);
			sendMessageToUI(MSG_SET_BUNDLE, java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime())
					+ update);
		} else if (update.contains("LOST_CONNECTION") && !isDestroying) {
			makeNotification(NotificationType.LOST_CONNECTION, aCtx.getString(R.string.noti_lost));
			dropConnection();
			ctx.sendBroadcast(new Intent("pilight-reconnect"));
		}
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

		//Log.v(TAG, "setting notification: " + type.name() + " while current = " + mCurrentNotif.name());
		if (type != mCurrentNotif) {
			//Log.v(TAG, "setting NEW notification: " + type.name());
			sendMessageToUI(MSG_SET_STATUS, type.name());
			switch (type) {
				case DESTROYED:
					builder = new Notification.Builder(ctx);
					builder.setSmallIcon(R.drawable.eye_black).setLargeIcon(bigPic(R.drawable.eye_black)).setContentTitle(aCtx.getString(R.string.app_name))
							.setContentText(message);
					mCurrentNotif = NotificationType.DESTROYED;
					break;
				case CONNECTING:
					kill = new Intent("pilight-kill-service");
					killService = PendingIntent.getBroadcast(ctx, 0, kill, PendingIntent.FLAG_UPDATE_CURRENT);

					builder = new Notification.Builder(ctx).setContentTitle(aCtx.getString(R.string.app_name)).setContentText(message)
							.setDeleteIntent(killService).setSmallIcon(R.drawable.eye_trans)
							.setLargeIcon(bigPic(R.drawable.eye_trans));
					mCurrentNotif = NotificationType.CONNECTING;
					break;
				case CONNECTED:
					main = new Intent(ctx, MainActivity.class);
					main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startMainActivity = PendingIntent.getActivity(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);

					builder = new Notification.Builder(ctx).setContentTitle(aCtx.getString(R.string.app_name)).setContentText(message + "\n" + myDate)
							.setContentIntent(startMainActivity).setSmallIcon(R.drawable.eye_white)
							.setLargeIcon(bigPic(R.drawable.eye_white));
					mCurrentNotif = NotificationType.CONNECTED;
					break;
				case FAILED:
					main = new Intent("pilight-reconnect");
					sentBroadcast = PendingIntent.getBroadcast(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);
					kill = new Intent("pilight-kill-service");
					killService = PendingIntent.getBroadcast(ctx, 0, kill, PendingIntent.FLAG_UPDATE_CURRENT);

					builder = new Notification.Builder(ctx).setContentTitle("pilight").setContentText(aCtx.getString(R.string.noti_failed) + message)
							.setDeleteIntent(killService).addAction(R.drawable.action_refresh, aCtx.getString(R.string.noti_retry), sentBroadcast)
							.setSmallIcon(R.drawable.eye_trans).setLargeIcon(bigPic(R.drawable.eye_trans));
					mCurrentNotif = NotificationType.FAILED;
					break;
				case NO_SERVER:
					main = new Intent("pilight-reconnect");
					sentBroadcast = PendingIntent.getBroadcast(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);
					kill = new Intent("pilight-kill-service");
					killService = PendingIntent.getBroadcast(ctx, 0, kill, PendingIntent.FLAG_UPDATE_CURRENT);

					builder = new Notification.Builder(ctx).setContentTitle("pilight").setContentText(aCtx.getString(R.string.noti_failed) + message)
							.setDeleteIntent(killService).addAction(R.drawable.action_refresh, aCtx.getString(R.string.noti_retry), sentBroadcast)
							.setSmallIcon(R.drawable.eye_trans).setLargeIcon(bigPic(R.drawable.eye_trans));
					mCurrentNotif = NotificationType.NO_SERVER;
					break;
				case LOST_CONNECTION:
					main = new Intent("pilight-reconnect");
					sentBroadcast = PendingIntent.getBroadcast(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);
					kill = new Intent("pilight-kill-service");
					killService = PendingIntent.getBroadcast(ctx, 0, kill, PendingIntent.FLAG_UPDATE_CURRENT);

					builder = new Notification.Builder(ctx).setContentTitle(aCtx.getString(R.string.app_name)).setContentText(message + "\n" + myDate)
							.setDeleteIntent(killService).addAction(R.drawable.action_refresh, aCtx.getString(R.string.noti_retry), sentBroadcast)
							.setSmallIcon(R.drawable.eye_trans).setLargeIcon(bigPic(R.drawable.eye_trans));
					mCurrentNotif = NotificationType.LOST_CONNECTION;
					break;
				case UPDATE:
					main = new Intent(ctx, MainActivity.class);
					main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startMainActivity = PendingIntent.getActivity(ctx, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);
					String[] title = message.split("\n");
					message = message.replace(title[0] + "\n", "");
					builder = new Notification.Builder(ctx).setContentTitle(title[0]).setContentText(message)
							.setStyle(new Notification.BigTextStyle().bigText(message)).setContentIntent(startMainActivity)
							.setSmallIcon(R.drawable.eye_white).setLargeIcon(bigPic(R.drawable.eye_white));
					mCurrentNotif = NotificationType.UPDATE;
					break;
				default:
					break;
			}
		} else {
			if (type == NotificationType.UPDATE) {
				String[] title = message.split("\n");
				message = message.replace(title[0] + "\n", "");
				builder.setContentTitle(title[0]).setStyle(new Notification.BigTextStyle().bigText(message));
				builder.setContentText(message);
				builder.setContentInfo("Stamp: " + myDate);
			} else if (message != null) {
				if (message.contains("Stamp")) {
					String[] title = message.split("\n");
					message = message.replace(title[0] + "\n", "");
					builder.setContentTitle(title[0]).setStyle(new Notification.BigTextStyle().bigText(message));
				} else {
					builder.setContentTitle(myDate).setStyle(new Notification.BigTextStyle().bigText(message));
					builder.setContentText(message);
				}
			}
		}
		mNotMan.notify(35, builder.build());
	}

	/**
	 * Send the data to all clients.
	 *
	 * @param message The message to send.
	 */
	private static void sendMessageToUI(int what, String message) {
		// Log.v(TAG, "sent message called, clients attached: " + Integer.toString(mClients.size()));
		Iterator<Messenger> messengerIterator = mClients.iterator();
		while (messengerIterator.hasNext()) {
			Messenger messenger = messengerIterator.next();
			try {
				Bundle bundle = new Bundle();
				bundle.setClassLoader(aCtx.getClassLoader());
				switch (what) {
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
	 * When binding to the service, we return an interface to our messenger for sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind");
		return mMessenger.getBinder();
	}

	@Override
	public void onRebind(Intent intent) {
		// Log.v(TAG, "onRebind");
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// Log.v(TAG, "onUnbind");
		super.onUnbind(intent);
		return true;
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		super.onCreate();
		ctx = this;
		aCtx = getApplicationContext();

		SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String language = getPrefs.getString("languagePref", "unknown");
		if (!language.equals("unknown")) makeLocale(language);

		IntentFilter filter = new IntentFilter();
		filter.addAction("pilight-reconnect");
		filter.addAction("pilight-kill-service");
		filter.addAction("pilight-switch-device");
		this.registerReceiver(mMessageReceiver, filter);

		mNotMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		isConnectionUp = makeConnection();
		isDestroying = false;
		Log.v(TAG, "onCreate done");

	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		this.unregisterReceiver(mMessageReceiver);
		sendMessageToUI(MSG_SET_STATUS, NotificationType.DESTROYED.name());
		isDestroying = true;
		dropConnection();
		mNotMan.cancel(35);
		super.onDestroy();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//??

		sendMessageToUI(MSG_SET_STATUS, mCurrentNotif.name());
		if (intent.hasExtra("command") && isConnectionUp) {
			Server.CONNECTION.sentCommand(intent.getStringExtra("command"));
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(aCtx);
			boolean useService = prefs.getBoolean("useService", true);
			if (!useService) {
				if (mWM == null || !mWM.isAlive()) {
					mWM = new WriteMonitor();
					mWM.setName("WriteMonitor");
					mWM.start();
				}
			}
		}
		return START_STICKY;
	}

	public void makeLocale(String language) {
		Log.w(TAG, language + " makeLocale");
		Locale locale = new Locale(language);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config,
				getBaseContext().getResources().getDisplayMetrics());
	}

	private boolean makeConnection() {
		if (mCurrentNotif == NotificationType.DESTROYED)
			makeNotification(NotificationType.CONNECTING, aCtx.getString(R.string.noti_connecting));
		String serverString = Server.CONNECTION.setup();
		String goodConfig = "{\"gui\":{";
		if (serverString.contains(goodConfig)) {
			try {
				mDevices = Config.getDevices(new JSONObject(serverString));
				//mDevices = Config.getDevices(json.getJSONObject("config"));
				Collections.sort(mDevices);
				addedToPreferences();
				makeNotification(NotificationType.CONNECTED, aCtx.getString(R.string.noti_connected));
				sendMessageToUI(MSG_SET_BUNDLE, null);
				startForeground(35, builder.build());
				if (mHB == null || !mHB.isAlive()) {
					mHB = new HeartBeat();
					mHB.setName("Heart-Beat");
					mHB.start();
				}
				return true;

			} catch (JSONException e) {
				Log.w(TAG, "problems in JSONning");
				return false;
			}
		} else if (serverString.contains("ADRESS")) {
			makeNotification(NotificationType.NO_SERVER, serverString);
			return false;
		} else {
			makeNotification(NotificationType.FAILED, serverString);
			return false;
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

	public enum NotificationType {
		CONNECTED, CONNECTING, DESTROYED, NO_SERVER, FAILED, LOST_CONNECTION, UPDATE,
	}

	public class WriteMonitor extends Thread {
		@Override
		public void run() {
			Log.v(TAG, "WriteMonitor started");
			try {
				while (!Thread.interrupted() && Server.CONNECTION.isWriting()) {
					Thread.sleep(3000);
				}
				Log.v(TAG, "WriteMonitor finished, stopping");
				if (mClients.isEmpty())
					stopSelf();
			} catch (InterruptedException e) {
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
			switch (msg.what) {
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
					JSONObject actionJSON = new JSONObject();
					Server.CONNECTION.sentCommand(msg.getData().getString("command"));
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	public class HeartBeat extends Thread {
		long timeHeart;
		long runs = 0;

		@Override
		public void interrupt() {
			// Log.v(TAG, "interrupted HEARTBEAT");
			super.interrupt();
		}

		@Override
		public void run() {
			try {
				while (isConnectionUp) {
					timeHeart = new Date().getTime();
					if (timeHeart - timeBeat > 5000) {
						Server.CONNECTION.sentCommand("HEART");
						Thread.sleep(1500);
					}
					if (timeHeart - timeBeat > 7000) {
						postUpdate("LOST_CONNECTION");
						isConnectionUp = false;
					}
					Thread.sleep(10000);
				}
				Log.v(TAG, "heart-beat thread comes to an end");
			} catch (Exception e) {
				Log.w(TAG, "something wrong in the heart-beat");
			}
		}
	}
}