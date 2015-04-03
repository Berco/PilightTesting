package by.zatta.pilight.tasker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import by.zatta.pilight.R;
import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.fragments.BaseFragment;
import by.zatta.pilight.fragments.SetupConnectionFragment;
import by.zatta.pilight.fragments.SetupConnectionFragment.OnChangedStatusListener;
import by.zatta.pilight.fragments.TaskerActionFragment;
import by.zatta.pilight.fragments.TaskerActionFragment.ActionReadyListener;
import by.zatta.pilight.model.ConnectionEntry;
import by.zatta.pilight.model.DeviceEntry;

public class ActionActivity extends Activity implements ServiceConnection, OnChangedStatusListener, ActionReadyListener {

	private static final String TAG = "Zatta::ActionActivity";
	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
	boolean mIsBound;
	Bundle localeBundle;
	private ServiceConnection mConnection = this;
	private Messenger mServiceMessenger = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
		setContentView(R.layout.actionactivity_layout);
	}

	@Override
	protected void onDestroy() {
		// Log.d(TAG, "onDestroy");
		super.onDestroy();
		try {
			doUnbindService();
		} catch (Throwable t) {
			Log.w(TAG, "Failed to unbind from the service", t);
		}

	}

	@Override
	protected void onPause() {
		// Log.d(TAG, "onPause");
		if (mIsBound) {
			// Log.v(TAG, "onPause::unbinding");
			doUnbindService();
		}
		finish();
		super.onPause();
	}

	@Override
	protected void onResume() {
		// Log.v(TAG, "onResume");
		automaticBind();
		super.onResume();
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
		openSetupConnectionFragment(SetupConnectionFragment.newInstance("CONNECTING"));
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

	private void openFragment(BaseFragment mBaseFragment2) {
		if (mBaseFragment2 != null) {
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			fm.popBackStack();
			ft.replace(R.id.fragment_main, mBaseFragment2, mBaseFragment2.getName());
			ft.commit();
		}
	}

	private void openSetupConnectionFragment(BaseFragment mBaseFragment) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		Fragment pref = fm.findFragmentByTag("SetupConnectionFragment");
		if (pref == null) {
			fm.popBackStack();
			ft.replace(R.id.fragment_main, mBaseFragment, "SetupConnectionFragment");
			ft.addToBackStack(null);
			ft.commit();
		}
	}

	@Override
	public void onChangedStatusListener(int what, ConnectionEntry connectionEntry) {
		switch (what) {
			case SetupConnectionFragment.DISMISS:
				closeDialogFragments();
				FragmentManager fm = getFragmentManager();
				BaseFragment prev = (BaseFragment) fm.findFragmentByTag("SetupConnectionFragment");
				if (!(prev == null)) {
					FragmentTransaction ft = fm.beginTransaction();
					ft.remove(prev);
					ft.commit();
					fm.popBackStack();
				}
				break;
			case SetupConnectionFragment.FINISH:
				closeDialogFragments();
				doUnbindService();
				stopService(new Intent(ActionActivity.this, ConnectionService.class));
				finish();
				break;
			case SetupConnectionFragment.RECONNECT:
				Intent intent = new Intent("pilight-reconnect");
				Bundle bundle = new Bundle();
				bundle.putParcelable("connectionEntry", connectionEntry);
				intent.putExtras(bundle);
				this.sendBroadcast(intent);
				break;
		}
	}

	@Override
	public void actionReadyListener(Intent localeIntent) {
		setResult(-1, localeIntent);
		finish();
	}

	/**
	 * Handle incoming messages from ConnectionService
	 */
	@SuppressLint("HandlerLeak")
	private class IncomingMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			//Log.v(TAG, "receiving a message");
			Bundle bundle = msg.getData();
			bundle.setClassLoader(getApplicationContext().getClassLoader());

			switch (msg.what) {
				case ConnectionService.MSG_SET_STATUS:
					String status = bundle.getString("status", "no status received yet");
					//Log.v(TAG, "status received in activity: " + status);
					if (status.equals("UPDATE")) break;

					FragmentManager fm = getFragmentManager();
					Fragment prev = fm.findFragmentByTag("SetupConnectionFragment");

					try {
						if (prev == null) {
							// Log.v(TAG, "there was not a fragment with tag dialog");
							openSetupConnectionFragment(SetupConnectionFragment.newInstance(status));
							break;
						} else if (!(prev.getClass().equals(SetupConnectionFragment.class))) {
							// Log.v(TAG, "there was fragment with tag SetupConnectionFragment not being StatusDialog");
							openSetupConnectionFragment(SetupConnectionFragment.newInstance(status));
							break;
						} else {
							// Log.v(TAG, "there was a SetupConnectionFragment running");
							SetupConnectionFragment.setChangedStatus(status);
							break;
						}
					} catch (IllegalStateException e) {
						Log.w(TAG, "most likely the activity wasn't made yet");
					}

				case ConnectionService.MSG_SET_BUNDLE:
					mDevices = bundle.getParcelableArrayList("config");

					FragmentManager fm2 = getFragmentManager();
					Fragment prev2 = fm2.findFragmentByTag("piTasker");

					if ((prev2 == null)) {
						openFragment(TaskerActionFragment.newInstance(localeBundle, mDevices));
					}

					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

}
