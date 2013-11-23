package by.zatta.pilight;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
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
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import by.zatta.pilight.connection.ConnectionService;

public class MainActivity extends Activity implements View.OnClickListener, ServiceConnection {
	private Button btnStart, btnStop, btnBind, btnUnbind, btnUpby1, btnUpby10;
	private TextView textStatus, textIntValue, textStrValue;
	private Messenger mServiceMessenger = null;
	boolean mIsBound;

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
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.base, menu);
		return true;
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
				String str1 = msg.getData().getString("str1");
				textStrValue.setText("Str Message: " + str1);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}	
}