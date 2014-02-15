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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.dialogs.AboutDialog;
import by.zatta.pilight.dialogs.StatusDialog;
import by.zatta.pilight.dialogs.StatusDialog.OnChangedStatusListener;
import by.zatta.pilight.fragments.BaseFragment;
import by.zatta.pilight.fragments.DeviceListFragment;
import by.zatta.pilight.fragments.DeviceListFragment.DeviceListListener;
import by.zatta.pilight.fragments.PrefFragment;
import by.zatta.pilight.fragments.PrefFragment.OnLanguageListener;
import by.zatta.pilight.fragments.PrefFragment.OnViewChangeListener;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;

public class MainActivity extends Activity implements ServiceConnection, DeviceListListener,
		OnChangedStatusListener, OnViewChangeListener,OnLanguageListener {

	private static final String TAG = "Zatta::MainActivity";
	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	private Map<String, String> allLocations = new LinkedHashMap<String, String>();
	private BaseFragment mBaseFragment;
	private ServiceConnection mConnection = this;
	private String mCurrentTitle;
	private DrawerLayout mDrawer;
	private ListView mDrawerList;
	private CustomActionBarDrawerToggle mDrawerToggle;
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
	private Messenger mServiceMessenger = null;
	boolean mIsBound;

	@Override
	public void deviceListListener(int what, String action) {
		// Log.d(TAG, "deviceListListener called");
		switch (what)
		{
		case ConnectionService.MSG_SWITCH_DEVICE:
			sendMessageToService(action);
			break;
		case 2004:
			break;
		}
	}

	@Override
	public void onChangedStatusListener(int what) {
		switch (what)
		{
		case StatusDialog.DISMISS:
			closeDialogFragments();
			break;
		case StatusDialog.FINISH:
			closeDialogFragments();
			doUnbindService();
			stopService(new Intent(MainActivity.this, ConnectionService.class));
			finish();
			break;
		case StatusDialog.RECONNECT:
			this.sendBroadcast(new Intent("pilight-reconnect"));
			// TODO after a reconnection it takes a while before a new mDevices is received..
			break;
		}
	}
	
	@Override
	public void onViewChangeListener(Boolean forceList) {
		Log.e(TAG, "received forceList: " + Boolean.toString(forceList));
		FragmentManager fm = getFragmentManager();
		BaseFragment prev = (BaseFragment) fm.findFragmentByTag("DeviceList");
		if (prev != null) prev.onCreate(null);
	}
	
	@Override
	public void onLanguageListener(String language) {
		Toast.makeText(this, "Language: " + language, Toast.LENGTH_SHORT).show();
		Log.w(TAG, language + " Listenener");
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate starts");
		setContentView(R.layout.mainactivity_layout);
		mCurrentTitle = getString(R.string.app_name);

		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerToggle = new CustomActionBarDrawerToggle(this, mDrawer);
		mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		
		if (savedInstanceState != null){
			Log.v(TAG, "savedInstanceState was not null");
			mDevices = savedInstanceState.getParcelableArrayList("config");
			mCurrentTitle = savedInstanceState.getString("currentTitle", "oops");
			getActionBar().setTitle(mCurrentTitle);
			initMenu();
		} else {
			mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			openDialogFragment(StatusDialog.newInstance("CONNECTING"));
		}
		automaticBind();
		Log.v(TAG, "onCreate done");
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
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
		switch (item.getItemId())
		{
		case R.id.menu_about:
			openDialogFragment(AboutDialog.newInstance());
			return true;
		case R.id.menu_settings:
			Fragment pref = fm.findFragmentByTag("prefs");
			if (pref == null) {
				ft.replace(R.id.fragment_main, new PrefFragment(), "prefs");
				fm.popBackStack();
				ft.addToBackStack(null);
				ft.commit();
			} else {
				ft.remove(fm.findFragmentByTag("prefs"));
				ft.commit();
				fm.popBackStack();
			}
			return true;
		default:
			break;
		}
		// Handle your other action bar items...
		return super.onOptionsItemSelected(item);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
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
	
	public void makeLocale(String language){
    	Log.w(TAG, language + " makeLocale");
        Locale locale = new Locale(language); 
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, 
        getBaseContext().getResources().getDisplayMetrics());
    }
    
    public String myAppVersion(){
		PackageInfo pinfo;
		try {
			pinfo = this.getPackageManager().getPackageInfo((this.getPackageName()), 0);
			return pinfo.versionName;
		} catch (NameNotFoundException e) {
			return " ";
		}
		
	}

	private void initMenu() {
		Log.v(TAG, "calling initMenu");
		mDrawerList = (ListView) findViewById(R.id.drawer);
		if (mDrawerList != null) {
			mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, makeLocationList()));
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		}

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawer.setDrawerListener(mDrawerToggle);
		mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

		closeDialogFragments();
	}

	private String[] makeLocationList() {
		allLocations.clear();
		allLocations.put(getString(R.string.title_all), null);
		if (mDevices != null) {
			for (DeviceEntry dentry : mDevices) {
				if (!allLocations.containsValue(dentry.getLocationID())) {
					for (SettingEntry sentry : dentry.getSettings()) {
						if (sentry.getKey().equals("locationName")) allLocations.put(sentry.getValue(), dentry.getLocationID());
					}
				}
			}
		}
		String[] locationNames = new String[allLocations.size()];
		locationNames = allLocations.keySet().toArray(locationNames);
		return locationNames;
	}
	
	private void startInitialFragment(){
		String location = allLocations.values().toArray(new String[allLocations.size()])[1];
		mCurrentTitle = allLocations.keySet().toArray(new String[allLocations.size()])[1];
		getActionBar().setTitle(mCurrentTitle);
		mBaseFragment = DeviceListFragment.newInstance(mDevices, location);
		openFragment(mBaseFragment);
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
		Log.v(TAG, "openFragment");
		if (mBaseFragment2 != null) {
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			fm.popBackStack();
			ft.replace(R.id.fragment_main, mBaseFragment2, mBaseFragment2.getName());
			ft.commit();
		}
	}

	/**
	 * Send data to the service
	 * 
	 * @param intvaluetosend
	 *            The data to send
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean useService = prefs.getBoolean("useService", true);
		if (!useService) {
			stopService(new Intent(MainActivity.this, ConnectionService.class));
		}
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO implement this properly
		Log.d(TAG, "onSaveInstanceState");
		outState.putParcelableArrayList("config", (ArrayList<? extends Parcelable>) mDevices);
		outState.putString("currentTitle", mCurrentTitle);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Log.v(TAG, "onPostCreate");
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerToggle != null) {
			// Log.v(TAG, "mDrawerToggle sync state");
			mDrawerToggle.syncState();
		}
	}

	@Override
	protected void onResume() {
		// Log.v(TAG, "onResume");
		automaticBind();
		super.onResume();
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

	private class CustomActionBarDrawerToggle extends ActionBarDrawerToggle {

		public CustomActionBarDrawerToggle(Activity mActivity, DrawerLayout mDrawerLayout) {
			super(mActivity, mDrawerLayout, R.drawable.ic_navigation_drawer, R.string.app_name, R.string.app_name);
		}

		@Override
		public void onDrawerClosed(View view) {
			getActionBar().setTitle(mCurrentTitle);
			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
		}

		@Override
		public void onDrawerOpened(View drawerView) {
			getActionBar().setTitle(getString(R.string.app_name));
			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
		}
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Highlight the selected item, update the title, and close the
			// drawer
			// update selected item and title, then close the drawer
			mDrawerList.setItemChecked(position, true);
			String location = allLocations.values().toArray(new String[allLocations.size()])[position];
			mBaseFragment = DeviceListFragment.newInstance(mDevices, location);
			openFragment(mBaseFragment);
			mCurrentTitle = allLocations.keySet().toArray(new String[allLocations.size()])[position];
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

			switch (msg.what)
			{
			case ConnectionService.MSG_SET_STATUS:
				String status = bundle.getString("status", "no status received yet");
				// Log.v(TAG, "status received in activity: " + status);
				if (status.equals("UPDATE")) break;

				FragmentManager fm = getFragmentManager();
				Fragment prev = fm.findFragmentByTag("dialog");
				// Log.e(TAG, "prev was: " +Boolean.toString(prev==null));

				// Log.e(TAG, "pref : " +Boolean.toString(!(prev.getClass().equals(StatusDialog.class))));

				if (prev == null) {
					// Log.v(TAG, "there was not a fragment with tag dialog");
					openDialogFragment(StatusDialog.newInstance(status));
					break;
				} else if (!(prev.getClass().equals(StatusDialog.class))) {
					// Log.v(TAG, "there was fragment with tag dialog not being StatusDialog");
					openDialogFragment(StatusDialog.newInstance(status));
					break;
				} else {
					// Log.v(TAG, "there was a statusdialog running");
					StatusDialog.setChangedStatus(status);
					break;
				}

			case ConnectionService.MSG_SET_BUNDLE:
				mDevices = bundle.getParcelableArrayList("config");
				if (allLocations.isEmpty()) {
					initMenu();
					startInitialFragment();
				}
				try {
					DeviceListFragment.updateUI(mDevices);
				} catch (Exception e) {
					// Log.v(TAG, "ListBaseFragment isn't made yet");
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
}