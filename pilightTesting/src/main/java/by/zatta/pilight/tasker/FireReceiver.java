package by.zatta.pilight.tasker;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import by.zatta.pilight.connection.ConnectionService;

public final class FireReceiver extends BroadcastReceiver {
	private static final String TAG = "FireReceiver";
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		Bundle extraBundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
		String[] what = extraBundle.getStringArray("Extra");
		Log.v(TAG, Integer.toString(what.length));
		Log.v(TAG, what[0]);
		Log.v(TAG, what[1]);
		String command = what[2];
		
		if (isConnectionServiceActive(context)){
			//context.sendBroadcast(new Intent("pilight-switch-device").putExtra("command", command));
			context.startService(new Intent(context, ConnectionService.class).putExtra("command", command));
		}else{
			Log.v(TAG, "ConnectionService found NOT active");
			if (isConnectedToKnownHome(context)){
				Log.v(TAG, "Looks like we are home anyway");
				context.startService(new Intent(context, ConnectionService.class).putExtra("command", command));
			} else {
				Log.v(TAG, "But not at home, not even trying..");
				context.startService(new Intent(context, ConnectionService.class).putExtra("command", command));
				Toast.makeText(context, "Trying over the internet",Toast.LENGTH_SHORT).show();
			}
		}
		return;
	}
	
	boolean isConnectionServiceActive(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (ConnectionService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

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
}
