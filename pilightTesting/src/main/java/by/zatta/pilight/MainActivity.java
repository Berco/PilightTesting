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
 * with pilight for android.
 * If not, see <http://www.gnu.org/licenses/>
 *
 * Copyright (c) 2013 pilight project
 ********************************************************************************************/

package by.zatta.pilight;

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
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.dialogs.AboutDialog;
import by.zatta.pilight.fragments.BaseFragment;
import by.zatta.pilight.fragments.DeviceListFragment;
import by.zatta.pilight.fragments.DeviceListFragment.DeviceListListener;
import by.zatta.pilight.fragments.PrefFragment;
import by.zatta.pilight.fragments.PrefFragment.OnLanguageListener;
import by.zatta.pilight.fragments.PrefFragment.OnViewChangeListener;
import by.zatta.pilight.fragments.PrefFragment.OnConnectionChangeListener;
import by.zatta.pilight.fragments.SetupConnectionFragment;
import by.zatta.pilight.fragments.SetupConnectionFragment.OnChangedStatusListener;
import by.zatta.pilight.model.ConnectionEntry;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;

public class MainActivity extends Activity implements ServiceConnection, DeviceListListener,
		OnChangedStatusListener, OnViewChangeListener, OnLanguageListener, OnConnectionChangeListener {

	private static final String TAG = "Zatta::MainActivity";
	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	private static String[] groups;
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
	boolean mIsBound;
	private BaseFragment mBaseFragment;
	private ServiceConnection mConnection = this;
	private String mCurrentTitle;
	private DrawerLayout mDrawer;
	private ListView mDrawerList;
	private CustomActionBarDrawerToggle mDrawerToggle;
	private Messenger mServiceMessenger = null;

	@Override
	public void deviceListListener(int what, String action) {
		// Log.d(TAG, "deviceListListener called");
		switch (what) {
			case ConnectionService.MSG_SWITCH_DEVICE:
				sendMessageToService(action);
				break;
			case 2004:
				break;
		}
	}

	@Override
	public void onConnectionChangeListener() {
		this.sendBroadcast(new Intent("pilight-disconnect"));
	}

	/**
	 * Send data to the service
	 */
	private void sendMessageToService(String switchCommand) {
		if (mIsBound) {
			if (mServiceMessenger != null) {
				try {
					Bundle bundle = new Bundle();
					bundle.setClassLoader(this.getClassLoader());
					bundle.putString("command", switchCommand);
					Message msg_string = Message.obtain(null, ConnectionService.MSG_SWITCH_DEVICE);
					msg_string.setData(bundle);
					msg_string.replyTo = mMessenger;
					mServiceMessenger.send(msg_string);
				} catch (RemoteException e) {
				}
			}
		}
	}

	@Override
	public void onChangedStatusListener(int what, List<ConnectionEntry> connectionEntryList) {
		hideKeyboardAndDoItNow();
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
				stopService(new Intent(MainActivity.this, ConnectionService.class));
				finish();
				break;
			case SetupConnectionFragment.RECONNECT:
				Intent intent = new Intent("pilight-reconnect");
				Bundle bundle = new Bundle();
				bundle.putParcelableArrayList("connectionsList", (ArrayList<? extends Parcelable>) connectionEntryList);
				intent.putExtras(bundle);
				this.sendBroadcast(intent);
				break;
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

	@Override
	public void onViewChangeListener(Boolean forceList) {
		FragmentManager fm = getFragmentManager();
		BaseFragment prev = (BaseFragment) fm.findFragmentByTag("DeviceList");
		if (prev != null) prev.onCreate(null);

	}

	@Override
	public void onLanguageListener(String language) {
		makeLocale(language);
		invalidateOptionsMenu();
		initMenu();
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		fm.popBackStack();
		ft.replace(R.id.fragment_main, new PrefFragment(), "prefs");
		ft.addToBackStack(null);
		ft.commit();
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

	private void initMenu() {
		Log.v(TAG, "calling initMenu");
		mDrawerList = (ListView) findViewById(R.id.drawer);
		if (mDrawerList != null) {
			mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, makeLocationArray()));
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		}

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawer.setDrawerListener(mDrawerToggle);
		mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

		closeDialogFragments();
	}

	private String[] makeLocationArray() {
		Map<String, String> allLocations = new LinkedHashMap<String, String>();
		if (mDevices != null) {
			for (DeviceEntry dentry : mDevices) {
				for (SettingEntry sentry : dentry.getSettings()) {
					if ((sentry.getKey().equals("group")) && (!allLocations.containsValue(sentry.getValue())))
						allLocations.put(sentry.getValue(), sentry.getValue());
				}
			}
		}
		groups = new String[allLocations.size()];
		groups = allLocations.keySet().toArray(groups);
		Arrays.sort(groups);
		return groups;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate starts");
		setContentView(R.layout.mainactivity_layout);
		mCurrentTitle = getString(R.string.app_name);

		SharedPreferences getPrefs = getSharedPreferences("ZattaPrefs", Context.MODE_MULTI_PROCESS);
		String language = getPrefs.getString("languagePref", "unknown");
		if (!language.equals("unknown")) makeLocale(language);

		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerToggle = new CustomActionBarDrawerToggle(this, mDrawer);
		mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		if (savedInstanceState != null) {
			Log.v(TAG, "savedInstanceState was not null");
			mDevices = savedInstanceState.getParcelableArrayList("config");
			mCurrentTitle = savedInstanceState.getString("currentTitle", "oops");
			getActionBar().setTitle(mCurrentTitle);
			initMenu();
		} else {
			mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			openSetupConnectionFragment(SetupConnectionFragment.newInstance("CONNECTING"));
		}
		automaticBind();
		Log.v(TAG, "onCreate done");
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}

	@Override
	protected void onResume() {
		automaticBind();
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArrayList("config", (ArrayList<? extends Parcelable>) mDevices);
		outState.putString("currentTitle", mCurrentTitle);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		// Log.d(TAG, "onPause");
		if (mIsBound) {
			// Log.v(TAG, "onPause::unbinding");
			doUnbindService();
		}
		super.onPause();
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
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		FragmentManager fm = getFragmentManager();
		Fragment setupFrag = fm.findFragmentByTag("SetupConnectionFragment");
		if (setupFrag != null) {
			if (setupFrag.isVisible()) {
				doUnbindService();
				stopService(new Intent(MainActivity.this, ConnectionService.class));
				finish();
			}
		}
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * The action bar home/up should open or close the drawer. ActionBarDrawerToggle will take care of this.
		 */
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		switch (item.getItemId()) {
			case R.id.menu_about:
				openDialogFragment(AboutDialog.newInstance());
				return true;
			case R.id.menu_settings:
				Fragment pref = fm.findFragmentByTag("prefs");
				if (pref == null) {
					fm.popBackStack();
					ft.replace(R.id.fragment_main, new PrefFragment(), "prefs");
					ft.addToBackStack(null);
					ft.commit();
				} else {
					ft.remove(fm.findFragmentByTag("prefs"));
					ft.commit();
					fm.popBackStack();
				}
				return true;
			case R.id.menu_disconnect:
				this.sendBroadcast(new Intent("pilight-disconnect"));
				return true;
			default:
				break;
		}
		// Handle your other action bar items...
		return super.onOptionsItemSelected(item);
	}

	private void openDialogFragment(DialogFragment dialogStandardFragment) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		if (dialogStandardFragment != null) {
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

	private void openSetupConnectionFragment(BaseFragment mBaseFragment) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		Fragment pref = fm.findFragmentByTag("SetupConnectionFragment");
		if (pref == null) {
			fm.popBackStack();
			ft.replace(R.id.fragment_main, mBaseFragment, "SetupConnectionFragment");
			//ft.addToBackStack(null);
			ft.commit();
		}
	}

	/**
	 * Check if the service is running. If the service is running when the activity starts, we want to automatically bind to it.
	 */
	private void automaticBind() {
		startService(new Intent(MainActivity.this, ConnectionService.class));
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
	}

	public String myAppVersion() {
		PackageInfo pinfo;
		try {
			pinfo = this.getPackageManager().getPackageInfo((this.getPackageName()), 0);
			return pinfo.versionName;
		} catch (NameNotFoundException e) {
			return " ";
		}
	}

	public void hideKeyboardAndDoItNow() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		View view = getCurrentFocus();
		if (view == null) view = new View(this);
		imm.hideSoftInputFromWindow(view.getWindowToken(),0);
	}

	private void startInitialFragment() {
		mCurrentTitle = groups[0];
		getActionBar().setTitle(groups[0]);
		mBaseFragment = DeviceListFragment.newInstance(mDevices, groups[0]);
		openFragment(mBaseFragment);
	}

	private void openFragment(BaseFragment mBaseFragment2) {
		Log.v(TAG, "openFragment");
		if (mBaseFragment2 != null) {
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			fm.popBackStack();
			ft.replace(R.id.fragment_main, mBaseFragment2, mBaseFragment2.getName());
			ft.commit();
		}
	}

	private class CustomActionBarDrawerToggle extends ActionBarDrawerToggle {

		public CustomActionBarDrawerToggle(Activity mActivity, DrawerLayout mDrawerLayout) {
			super(mActivity, mDrawerLayout, R.drawable.ic_navigation_drawer, R.string.app_name, R.string.app_name);
		}

		@Override
		public void onDrawerOpened(View drawerView) {
			getActionBar().setTitle(getString(R.string.app_name));
			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
		}

		@Override
		public void onDrawerClosed(View view) {
			getActionBar().setTitle(mCurrentTitle);
			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
		}
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Highlight the selected item, update the title, and close the
			// drawer update selected item and title, then close the drawer
			mDrawerList.setItemChecked(position, true);
			mBaseFragment = DeviceListFragment.newInstance(mDevices, groups[position]);
			openFragment(mBaseFragment);
			mCurrentTitle = groups[position];
			mDrawer.closeDrawer(mDrawerList);
		}
	}

	/**
	 * Handle incoming messages from ConnectionService
	 */
	@SuppressLint("HandlerLeak")
	private class IncomingMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// Log.v(TAG, "receiving a message");
			Bundle bundle = msg.getData();
			bundle.setClassLoader(getApplicationContext().getClassLoader());
			FragmentManager fm = getFragmentManager();

			switch (msg.what) {
				case ConnectionService.MSG_SET_STATUS:
					String status = bundle.getString("status", "no status received yet");
					// Log.v(TAG, "status received in activity: " + status);
					if (status.equals("UPDATE")) break;

					Fragment prevCon = fm.findFragmentByTag("SetupConnectionFragment");

					try {
						if (prevCon == null) {
							// Log.v(TAG, "there was not a fragment with tag dialog");
							openSetupConnectionFragment(SetupConnectionFragment.newInstance(status));
							break;
						} else if (!(prevCon.getClass().equals(SetupConnectionFragment.class))) {
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
					try {
						if (groups == null) {
							initMenu();
						}
						fm = getFragmentManager();
						Fragment prevList = fm.findFragmentByTag("DeviceList");

							if (prevList == null) {
								// Log.v(TAG, "there was not a fragment with tag dialog");
								startInitialFragment();
								break;
							} else {
								// Log.v(TAG, "there was a SetupConnectionFragment running");
								DeviceListFragment.updateUI(mDevices);
								break;
							}


					} catch (Exception e) {
						//Log.w(TAG, "ListBaseFragment isn't made yet or something wrong inside the fragment");
					}
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}
}