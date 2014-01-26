package by.zatta.pilight.tasker;

import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import by.zatta.pilight.R;
import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.dialogs.StatusDialog;
import by.zatta.pilight.dialogs.StatusDialog.OnChangedStatusListener;
import by.zatta.pilight.model.DeviceEntry;

public class ActionActivity extends Activity implements ServiceConnection, OnChangedStatusListener{
	
	String mServer, mPort, mLocation, mDevice, mState, mValue;
	EditText mServerText, mPortText, mLocationText, mDeviceText, mValueText;
	RadioGroup g;
	String[] mExtra;
	
	private static final String TAG = "Zatta::ActionActivity";
	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	
	private ServiceConnection mConnection = this;
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
	private Messenger mServiceMessenger = null;
	boolean mIsBound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
		
		setContentView(R.layout.tasker_edit_activity_layout);
		mServerText = (EditText) findViewById(R.id.editTServerAddress);
		mPortText = (EditText) findViewById(R.id.editTPortNum);
		mLocationText = (EditText) findViewById(R.id.editTLocation);
		mDeviceText = (EditText) findViewById(R.id.editTDevice);
		g = (RadioGroup) findViewById(R.id.radioGState);
		mValueText = (EditText) findViewById(R.id.editTValue);
		
		if (savedInstanceState == null) {
			if (localeBundle != null) {
				mExtra = localeBundle.getStringArray("Extra");
				mServerText.setText(mExtra[0]);
				mPortText.setText(mExtra[1]);
				mLocationText.setText(mExtra[2]);
				mDeviceText.setText(mExtra[3]);
				if (mExtra[4] == "off"){
					g.check(R.id.radioOff);
				}
				mValueText.setText(mExtra[5]);
			}
		}
		setTitle("Settings");
	}
	
	@Override
	protected void onDestroy() {
		//Log.d(TAG, "onDestroy");
		super.onDestroy();
		try {
			doUnbindService();
		} catch (Throwable t) {
			Log.w(TAG, "Failed to unbind from the service", t);
		}
		
	}

	@Override
	protected void onPause() {
		//Log.d(TAG, "onPause");
		if (mIsBound) {
			//Log.v(TAG, "onPause::unbinding");
			doUnbindService();
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean useService = prefs.getBoolean("useService", true);
		if (!useService){
			stopService(new Intent(ActionActivity.this, ConnectionService.class));
		}
		finish();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		//Log.v(TAG, "onResume");
		automaticBind();
		super.onResume();
	}
	
	public void finishActivity(View view){
		mServer = mServerText.getText().toString();
		mPort = mPortText.getText().toString();
		mLocation = mLocationText.getText().toString();
		mDevice = mDeviceText.getText().toString();
		int selected = g.getCheckedRadioButtonId();
		if (selected == R.id.radioOn){
			mState = "on";
		}
		else {
			mState = "off";
		}
		mValue = mValueText.getText().toString();
		mExtra = new String[6];
		mExtra[0] = mServer;
		mExtra[1] = mPort;
		mExtra[2] = mLocation;
		mExtra[3] = mDevice;
		mExtra[4] = mState;
		mExtra[5] = mValue;
		Bundle extraBundle = new Bundle();
		extraBundle.putStringArray("Extra", mExtra);
		Intent i = new Intent();
		i.putExtra("Server", mServer).putExtra("Port", mPort).putExtra("Location", mLocation).putExtra("Device", mDevice).putExtra("State", mState).putExtra("Value", mValue);
		i.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, extraBundle);
		String blurb = mServer + ":" + mPort + " => " + mLocation + " " + mDevice + " " + mState + " " + mValue;
		i.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);
		setResult(-1,i);
		finish();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mServiceMessenger = new Messenger(service);
		try {
			Message msg = Message.obtain(null, ConnectionService.MSG_REGISTER_CLIENT);
			msg.replyTo = mMessenger;
			mServiceMessenger.send(msg);
		} catch (RemoteException e) {
			// In this case the service has crashed before we could even do
			// anything with it
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// This is called when the connection with the service has been
		// unexpectedly disconnected - process crashed.
		mServiceMessenger = null;
		// textStatus.setText("Disconnected.");
	}
	
	/**
	 * Check if the service is running. If the service is running when the activity starts, we want to automatically bind to it.
	 */
	private void automaticBind() {
		startService(new Intent(ActionActivity.this, ConnectionService.class));
		if (isConnectionServiceActive()) {
			doBindService();
		}
	}
	
	boolean isConnectionServiceActive() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (ConnectionService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Bind this Activity to MyService
	 */
	private void doBindService() {
		bindService(new Intent(this, ConnectionService.class), mConnection, Context.BIND_ADJUST_WITH_ACTIVITY);
		mIsBound = true;
	}

	/**
	 * Un-bind this Activity to MyService
	 */
	private void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mServiceMessenger != null) {
				try {
					Message msg = Message.obtain(null, ConnectionService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mServiceMessenger.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	private void openDialogFragment(DialogFragment dialogStandardFragment) {
		if (dialogStandardFragment != null) {
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			DialogFragment prev = (DialogFragment) fm.findFragmentByTag("dialog");
			if (prev != null) {
				prev.dismiss();
			}
			try {
				dialogStandardFragment.show(ft, "dialog");
			} catch (IllegalStateException e) {
				Log.w(TAG, "activity wasn't yet made");
			}
		}
	}

	private void closeDialogFragments() {
		FragmentManager fm = getFragmentManager();
		DialogFragment prev = (DialogFragment) fm.findFragmentByTag("dialog");
		if (prev != null) {
			prev.dismiss();
		}
	}
	
	/**
	 * Handle incoming messages from ConnectionService
	 */
	@SuppressLint("HandlerLeak")
	private class IncomingMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.v(TAG, "receiving a message");
			Bundle bundle = msg.getData();
			bundle.setClassLoader(getApplicationContext().getClassLoader());

			switch (msg.what) {
			case ConnectionService.MSG_SET_STATUS:
				String status = bundle.getString("status", "no status received yet");
				Log.v(TAG, "status received in activity: " + status);
				if (status.equals("UPDATE")) break;				
				
				FragmentManager fm = getFragmentManager();
				Fragment prev = fm.findFragmentByTag("dialog");
				
				if (prev == null){
					Log.v(TAG, "there was not a fragment with tag dialog");
					openDialogFragment(StatusDialog.newInstance(status));
					break;
				}else if (!(prev.getClass().equals(StatusDialog.class))){
					Log.v(TAG, "there was fragment with tag dialog not being StatusDialog");
					openDialogFragment(StatusDialog.newInstance(status));
					break;
				}else{
					Log.v(TAG, "there was a statusdialog running");
					StatusDialog.setChangedStatus(status);
					break;
				}
				
			case ConnectionService.MSG_SET_BUNDLE:
				mDevices = bundle.getParcelableArrayList("config");

				//if (allLocations.isEmpty())
				//	initMenu();

				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	@Override
	public void onChangedStatusListener(int what) {
		
		Log.d(TAG, "onChangedStatusListener called");
		switch (what) {
		case StatusDialog.DISMISS:
			closeDialogFragments();
			break;
		case StatusDialog.FINISH:
			closeDialogFragments();
			doUnbindService();
			stopService(new Intent(ActionActivity.this, ConnectionService.class));
			finish();
			break;
		case StatusDialog.RECONNECT:
			this.sendBroadcast(new Intent("pilight-reconnect"));
			//TODO after a reconnection it takes a while before a new mDevices is received..
			break;
		}
	}

}

