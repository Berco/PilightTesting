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

package by.zatta.pilight;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import by.zatta.pilight.connection.ConnectionService;

public class ConnectionChangeReceiver extends BroadcastReceiver {

	//private final static String TAG = "Zatta::ConnectionChangeReceiver";

	boolean isConnectedToKnownHome(Context ctx) {
		String currentNetwork = null;
		ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (!(info == null))
			if (!(info.getType() == ConnectivityManager.TYPE_WIFI)) return false;

		WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (!(wifiInfo == null)) {
			currentNetwork = wifiInfo.getSSID();
		}
		if (currentNetwork == null) return false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String previous = prefs.getString("networks_known", "");
		// Log.d(TAG, previous);
		currentNetwork = currentNetwork.replace("\"", "");
		if (previous.contains(currentNetwork)) {
			// Log.d(TAG, previous + " did contain " + currentNetwork);
			return true;
		} else {
			return false;
		}
	}

	private void makeNotification(boolean left, Context ctx) {
		NotificationManager mNotMan = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		if (left) {
			Bitmap mBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.eye_black);
			int height = (int) ctx.getResources().getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) ctx.getResources().getDimension(android.R.dimen.notification_large_icon_width);
			mBitmap = Bitmap.createScaledBitmap(mBitmap, width / 3, height / 3, false);

			Notification.Builder builder = new Notification.Builder(ctx).setContentTitle("pilight")
					.setContentText("Left your home network").setSmallIcon(R.drawable.eye_black).setLargeIcon(mBitmap);
			mNotMan.notify(34, builder.build());
		} else {
			mNotMan.cancel(34);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Log.v(TAG, "received connectivity change: " + intent.getAction());
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean useService = prefs.getBoolean("useService", true);
		boolean dontShowNotification = prefs.getBoolean("destroySilent", false);

		if (useService) {
			if (isConnectedToKnownHome(context)) {
				// Log.v(TAG, "we are connected to home!");
				makeNotification(false, context);
				// Added this sleep because often the connection isn't fully up (obtaining ip or so)
				try { Thread.sleep(2000); } catch (InterruptedException e) {}
				context.startService(new Intent(context, ConnectionService.class));
			} else {
				// Log.v(TAG, "not at home anymore :(");
				if (context.stopService(new Intent(context, ConnectionService.class))){
					if (!dontShowNotification)
						makeNotification(true, context);
				}
					
				// context.sendBroadcast(new Intent("pilight-left-network"));
			}
		}
	}
}
