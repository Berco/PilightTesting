package by.zatta.pilight;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;

public class MainActivity extends Activity implements View.OnClickListener, ServiceConnection {
	private Button btnStart, btnStop, btnBind, btnUnbind, btnUpby1, btnUpby10;
	private TextView textStatus, textIntValue, textStrValue;
	
	private ListView mDrawerList;
    private DrawerLayout mDrawer;
    private CustomActionBarDrawerToggle mDrawerToggle;
    private int mCurrentTitle=R.string.app_name;
	
	private Messenger mServiceMessenger = null;
	boolean mIsBound;
	static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();

	private static final String TAG = "Zatta::MainActivity";
	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());

	private ServiceConnection mConnection = this;
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("textStatus", textStatus.getText().toString());
		outState.putString("textIntValue", textIntValue.getText().toString());
		outState.putString("textStrValue", textStrValue.getText().toString());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			textStatus.setText(savedInstanceState.getString("textStatus"));
			textIntValue.setText(savedInstanceState.getString("textIntValue"));
			textStrValue.setText(savedInstanceState.getString("textStrValue"));
		}
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		btnStart = (Button)findViewById(R.id.btnStart);
		btnStop = (Button)findViewById(R.id.btnStop);
		btnBind = (Button)findViewById(R.id.btnBind);
		btnUnbind = (Button)findViewById(R.id.btnUnbind);
		textStatus = (TextView)findViewById(R.id.textStatus);
		textIntValue = (TextView)findViewById(R.id.textIntValue);
		textStrValue = (TextView)findViewById(R.id.textStrValue);
		btnUpby1 = (Button)findViewById(R.id.btnUpby1);
		btnUpby10 = (Button)findViewById(R.id.btnUpby10);

		btnStart.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnBind.setOnClickListener(this);
		btnUnbind.setOnClickListener(this);
		btnUpby1.setOnClickListener(this);
		btnUpby10.setOnClickListener(this);

		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        _initMenu();
        mDrawerToggle = new CustomActionBarDrawerToggle(this, mDrawer);
        mDrawer.setDrawerListener(mDrawerToggle);
		
		automaticBind();
		
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		if (mIsBound){
			Log.d(TAG, "onPause::unbinding");
			doUnbindService();
		}
		
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		automaticBind();
		super.onResume();
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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
        // If the nav drawer is open, hide action items related to the content view
        //boolean drawerOpen = mDrawer.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
		 * The action bar home/up should open or close the drawer.
		 * ActionBarDrawerToggle will take care of this.
		 */
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {

            case R.id.menu_about:
                return true;
            case R.id.menu_settings:
                return true;
            default:
                break;
        }


        // Handle your other action bar items...
        return super.onOptionsItemSelected(item);
    }

    private class CustomActionBarDrawerToggle extends ActionBarDrawerToggle {

        public CustomActionBarDrawerToggle(Activity mActivity, DrawerLayout mDrawerLayout) {
            super(
                    mActivity,
                    mDrawerLayout,
                    R.drawable.ic_navigation_drawer,
                    R.string.app_name,
                    mCurrentTitle);
        }

        @Override
        public void onDrawerClosed(View view) {
            getActionBar().setTitle(getString(mCurrentTitle));
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
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {

            // Highlight the selected item, update the title, and close the drawer
            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(position, true);
//            mBaseFragment = selectFragment(position);
//            mSelectedFragment = position;
//
//            if (mBaseFragment != null)
//                openFragment(mBaseFragment);
            mDrawer.closeDrawer(mDrawerList);
        }
    }
    
    public static final String[] options = {
        "All",
        "Living",
        "BathRoom",
        "Kitchen",
        "Alleyway",
        "Toilet",
        "Stairway",
        "Master bedroom",
        "Bedroom son",
        "Bedroom daughter",
        "Basement" ,
        "Storage",
        "Front garden",
        "Drive way",
        "Back garden",
        "Garage",
        "Lawn"
};


private void _initMenu() {
    mDrawerList = (ListView) findViewById(R.id.drawer);

    if (mDrawerList != null) {
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, options));

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

}

	/**
	 * Check if the service is running. If the service is running 
	 * when the activity starts, we want to automatically bind to it.
	 */
	private void automaticBind() {
		startService(new Intent(MainActivity.this, ConnectionService.class));
		if (isConnectionServiceActive()) {
			doBindService();
		}
	}
	
	boolean isConnectionServiceActive(){
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ConnectionService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
	}

	/**
	 * Send data to the service
	 * @param intvaluetosend The data to send
	 */
	private void sendMessageToService(int intvaluetosend) {
		if (mIsBound) {
			if (mServiceMessenger != null) {
				try {
					Message msg = Message.obtain(null, ConnectionService.MSG_SET_INT_VALUE, intvaluetosend, 0);
					msg.replyTo = mMessenger;
					mServiceMessenger.send(msg);
				} catch (RemoteException e) {
				}
			}
		}
	}

	/**
	 * Bind this Activity to MyService
	 */
	private void doBindService() {
		bindService(new Intent(this, ConnectionService.class), mConnection, Context.BIND_NOT_FOREGROUND);
		mIsBound = true;
		textStatus.setText("Binding.");
	}

	/**
	 * Un-bind this Activity to MyService
	 */	
	private void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it, then now is the time to unregister.
			if (mServiceMessenger != null) {
				try {
					Message msg = Message.obtain(null, ConnectionService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mServiceMessenger.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			textStatus.setText("Unbinding.");
		}
	}

	/**
	 * Handle button clicks
	 */
	@Override
	public void onClick(View v) {
		if(v.equals(btnStart)) {
			automaticBind();
		}
		else if(v.equals(btnStop)) {
			doUnbindService();
			stopService(new Intent(MainActivity.this, ConnectionService.class));
		}
		else if(v.equals(btnBind)) {
			doBindService();
		}
		else if(v.equals(btnUnbind)) {
			doUnbindService();
		}
		else if(v.equals(btnUpby1)) {
			sendMessageToService(1);
		}
		else if(v.equals(btnUpby10)) {
			sendMessageToService(10);
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mServiceMessenger = new Messenger(service);
		textStatus.setText("Attached.");
		try {
			Message msg = Message.obtain(null, ConnectionService.MSG_REGISTER_CLIENT);
			msg.replyTo = mMessenger;
			mServiceMessenger.send(msg);
		} 
		catch (RemoteException e) {
			// In this case the service has crashed before we could even do anything with it
		} 
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// This is called when the connection with the service has been unexpectedly disconnected - process crashed.
		mServiceMessenger = null;
		textStatus.setText("Disconnected.");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			doUnbindService();
		} catch (Throwable t) {
			Log.e(TAG, "Failed to unbind from the service", t);
		}
	}

	/**
	 * Handle incoming messages from ConnectionService
	 */
	@SuppressLint("HandlerLeak")
	private class IncomingMessageHandler extends Handler {		
		@Override
		public void handleMessage(Message msg) {
			// Log.d(LOGTAG,"IncomingHandler:handleMessage");
			switch (msg.what) {
			case ConnectionService.MSG_SET_INT_VALUE:
				textIntValue.setText("Int Message: " + msg.arg1);
				break;
			case ConnectionService.MSG_SET_STRING_VALUE:
				Bundle bundle = msg.getData();
				bundle.setClassLoader(getApplicationContext().getClassLoader());
				
				String str1 = bundle.getString("str1");
				textStrValue.setText("Str Message: " + str1);
								
				mDevices = bundle.getParcelableArrayList("config");
				print();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	public static void print() {
		System.out.println("________________");
		for (DeviceEntry device : mDevices){
			 System.out.println("-"+device.getNameID());
			 System.out.println("-"+device.getLocationID());
			 System.out.println("-"+device.getType());
				for(SettingEntry sentry : device.getSettings()) {
					System.out.println("*"+sentry.getKey()+" = " + sentry.getValue());
				}
			System.out.println("________________");
		 }
	}
}