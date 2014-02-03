package by.zatta.pilight.tasker;

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
		Toast.makeText(context,command,Toast.LENGTH_SHORT).show();
		return;
	}
}
