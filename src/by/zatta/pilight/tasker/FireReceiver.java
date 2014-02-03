package by.zatta.pilight.tasker;

import by.zatta.pilight.connection.ConnectionService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public final class FireReceiver extends BroadcastReceiver {
	private static final String TAG = "FireReveiver";
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.v(TAG, "Fired");
		Bundle extraBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
		String[] what = extraBundle.getStringArray("Extra");
		String command = what[2];
		
		if (isConnectionServiceActive(context)){
			context.sendBroadcast(new Intent("pilight-switch-device").putExtra("command", command));
		}else{
			Toast.makeText(context,"NOT Active" + command,Toast.LENGTH_SHORT).show();
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
}
