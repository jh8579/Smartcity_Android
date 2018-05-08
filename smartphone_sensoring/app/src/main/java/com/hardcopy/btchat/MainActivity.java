/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hardcopy.btchat;

import java.util.Timer;
import java.util.TimerTask;

import com.hardcopy.btchat.fragments.ExampleFragment;
import com.hardcopy.btchat.fragments.FragmentAdapter;
import com.hardcopy.btchat.fragments.IFragmentListener;
import com.hardcopy.btchat.service.BTCTemplateService;
import com.hardcopy.btchat.utils.AppSettings;
import com.hardcopy.btchat.utils.Constants;
import com.hardcopy.btchat.utils.Logs;
import com.hardcopy.btchat.utils.RecycleUtils;
import com.hardcopy.btchat.R;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Math.sqrt;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, IFragmentListener,SensorEventListener {

    // Debugging
    private static final String TAG = "RetroWatchActivity";
    
	// Context, System
	private Context mContext;
	private BTCTemplateService mService;
	private ActivityHandler mActivityHandler;
	
	// Global
	
	// UI stuff
	private FragmentManager mFragmentManager;
	private FragmentAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;
	
	private ImageView mImageBT = null;
	private TextView mTextStatus = null;

	// Refresh timer
	private Timer mRefreshTimer = null;

	private SensorManager mSensorManager;
	private Sensor mSensor;
	private SensorManager sSensorManager;
	private Sensor sSensor;
	float gravity[] = new float[3];
	float linear_acceleration[] = new float[3];
	double gravity_sum;

	int gravity_count = 0;
	int sound = 3;
	int time = 0;
	double db = 0;
	double dbCheck = 0;

	SoundMeter s = new SoundMeter();

	/*****************************************************
	 *	 Overrided methods
	 ******************************************************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//----- System, Context
		mContext = this;	//.getApplicationContext();
		mActivityHandler = new ActivityHandler();
		AppSettings.initializeAppSettings(mContext);
		
		setContentView(R.layout.activity_main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		mSensorManager.registerListener(this, mSensor,SensorManager.SENSOR_DELAY_NORMAL);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(255,255,255,255)));
		actionBar.setStackedBackgroundDrawable(getResources().getDrawable(R.drawable.main_tap));
		// Create the adapter that will return a fragment for each of the primary sections of the app.
		mFragmentManager = getSupportFragmentManager();
		mSectionsPagerAdapter = new FragmentAdapter(mFragmentManager, mContext, this, mActivityHandler);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by the adapter.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		// Setup views
		mImageBT = (ImageView) findViewById(R.id.status_title);
		mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
		mTextStatus = (TextView) findViewById(R.id.status_text);
		mTextStatus.setText(getResources().getString(R.string.bt_state_init));
		
		// Do data initialization after service started and binded
		doStartService();
	}

	@Override
	public synchronized void onStart() {
		super.onStart();
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		// Stop the timer
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
			mRefreshTimer = null;
		}
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		finalizeActivity();
	}
	
	@Override
	public void onLowMemory (){
		super.onLowMemory();
		// onDestroy is not always called when applications are finished by Android system.
		finalizeActivity();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_scan:
			// Launch the DeviceListActivity to see devices and do scan
			doScan();
			return true;
		case R.id.action_discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();		// TODO: Disable this line to run below code
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}
	
	/**
	 * Implements TabListener
	 */
	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}
	
	@Override
	public void OnFragmentCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
		switch(msgType) {
		case IFragmentListener.CALLBACK_RUN_IN_BACKGROUND:
			if(mService != null)
				mService.startServiceMonitoring();
			break;
		case IFragmentListener.CALLBACK_SEND_MESSAGE:
			if(mService != null && arg2 != null)
				mService.sendMessageToRemote(arg2);

		default:
			break;
		}
	}
	
	
	/*****************************************************
	 *	Private methods
	 ******************************************************/
	
	/**
	 * Service connection
	 */
	private ServiceConnection mServiceConn = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(TAG, "Activity - Service connected");
			
			mService = ((BTCTemplateService.ServiceBinder) binder).getService();
			
			// Activity couldn't work with mService until connections are made
			// So initialize parameters and settings here. Do not initialize while running onCreate()
			initialize();
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};
	
	/**
	 * Start service if it's not running
	 */
	private void doStartService() {
		Log.d(TAG, "# Activity - doStartService()");
		startService(new Intent(this, BTCTemplateService.class));
		bindService(new Intent(this, BTCTemplateService.class), mServiceConn, Context.BIND_AUTO_CREATE);
	}
	
	/**
	 * Stop the service
	 */
	private void doStopService() {
		Log.d(TAG, "# Activity - doStopService()");
		mService.finalizeService();
		stopService(new Intent(this, BTCTemplateService.class));
	}
	
	/**
	 * Initialization / Finalization
	 */
	private void initialize() {
		Logs.d(TAG, "# Activity - initialize()");
		mService.setupService(mActivityHandler);
		
		// If BT is not on, request that it be enabled.
		// RetroWatchService.setupBT() will then be called during onActivityResult
		if(!mService.isBluetoothEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
		}
		
		// Load activity reports and display
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
		}
		
		// Use below timer if you want scheduled job
		//mRefreshTimer = new Timer();
		//mRefreshTimer.schedule(new RefreshTimerTask(), 5*1000);
	}
	
	private void finalizeActivity() {
		Logs.d(TAG, "# Activity - finalizeActivity()");
		
		if(!AppSettings.getBgService()) {
			doStopService();
		} else {
		}

		// Clean used resources
		RecycleUtils.recursiveRecycle(getWindow().getDecorView());
		System.gc();
	}
	
	/**
	 * Launch the DeviceListActivity to see devices and do scan
	 */
	private void doScan() {
		Intent intent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
	}
	
	/**
	 * Ensure this device is discoverable by others
	 */
	private void ensureDiscoverable() {
		if (mService.getBluetoothScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(intent);
		}
	}
	
	
	/*****************************************************
	 *	Public classes
	 ******************************************************/
	
	/**
	 * Receives result from external activity
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Logs.d(TAG, "onActivityResult " + resultCode);
		
		switch(requestCode) {
		case Constants.REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Attempt to connect to the device
				if(address != null && mService != null)
					mService.connectDevice(address);
			}
			break;
			
		case Constants.REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a BT session
				mService.setupBT();
			} else {
				// User did not enable Bluetooth or an error occured
				Logs.e(TAG, "BT is not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
			}
			break;
		}	// End of switch(requestCode)
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
			time++;
			final float alpha = 0.8f;

			// Isolate the force of gravity with the low-pass filter.
			gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

			// Remove the gravity contribution with the high-pass filter.
			linear_acceleration[0] = event.values[0] - gravity[0];
			linear_acceleration[1] = event.values[1] - gravity[1];
			linear_acceleration[2] = event.values[2] - gravity[2];

			gravity_sum = sqrt(linear_acceleration[0]*linear_acceleration[0]+linear_acceleration[1]*linear_acceleration[1]+linear_acceleration[2]*linear_acceleration[2]);

			if(gravity_sum > 0.08){
				gravity_count++;
				if(gravity_count == 60){
					gravity_count = 0;
				}
			}

			if(time % 15 == 0){
				s.start();
				while(sound > 0) {
					db = Math.log10(s.getAmplitude()) * 10;
					db += 20;
					Log.d("소리값", String.valueOf(db));
					sound--;
				}
				dbCheck += db;
				s.stop();
			}

//            Log.d("sum값",String.valueOf(gravity_sum));
//            Log.d("x값",String.valueOf(linear_acceleration[0]));
//            Log.d("y값",String.valueOf(linear_acceleration[1]));
//            Log.d("z값",String.valueOf(linear_acceleration[2]));
			Log.d("sum값",String.valueOf(gravity_count));
			sound = 3;

			if(mService != null && time == 60){
				double returnType = (double)gravity_count / (double)time;
				if(returnType<0.3 && (dbCheck/4)<=52)
				{
					mService.sendMessageToRemote(String.valueOf(1));
				}
				else if(returnType<0.3 &&(dbCheck/4)>52)

				{
					mService.sendMessageToRemote((String.valueOf(2)));
				}
				else if(returnType<0.7 && (dbCheck/4)<=52)
				{
					mService.sendMessageToRemote((String.valueOf(3)));
				}
				else if(returnType<0.7 && (dbCheck/4)>52)
				{
					mService.sendMessageToRemote((String.valueOf(4)));
				}
				else if(returnType>=0.7 && (dbCheck/4) <=52)
				{
					mService.sendMessageToRemote((String.valueOf(5)));
				}
				else if(returnType>=0.7 && (dbCheck/4)>52)
				{
					mService.sendMessageToRemote((String.valueOf(6)));
				}
				Log.d("전송 여부", "성공" + String.valueOf(returnType));
				gravity_count = 0;
				time = 0;
				dbCheck = 0;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}


	/*****************************************************
	 *	Handler, Callback, Sub-classes
	 ******************************************************/
	
	public class ActivityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what) {
			// Receives BT state messages from service 
			// and updates BT state UI
			case Constants.MESSAGE_BT_STATE_INITIALIZED:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_init));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
				break;
			case Constants.MESSAGE_BT_STATE_LISTENING:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_wait));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
				break;
			case Constants.MESSAGE_BT_STATE_CONNECTING:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_connect));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_away));
				break;
			case Constants.MESSAGE_BT_STATE_CONNECTED:
				if(mService != null) {
					String deviceName = mService.getDeviceName();
					if(deviceName != null) {
						mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
								getResources().getString(R.string.bt_state_connected) + " " + deviceName);
						mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_online));
					}
				}
				break;
			case Constants.MESSAGE_BT_STATE_ERROR:
				mTextStatus.setText(getResources().getString(R.string.bt_state_error));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_busy));
				break;
			
			// BT Command status
			case Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED:
				mTextStatus.setText(getResources().getString(R.string.bt_cmd_sending_error));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_busy));
				break;
				
			///////////////////////////////////////////////
			// When there's incoming packets on bluetooth
			// do the UI works like below
			///////////////////////////////////////////////
			case Constants.MESSAGE_READ_CHAT_DATA:
				if(msg.obj != null) {
					ExampleFragment frg = (ExampleFragment) mSectionsPagerAdapter.getItem(FragmentAdapter.FRAGMENT_POS_EXAMPLE);
					frg.showMessage((String)msg.obj);
				}
				break;
			
			default:
				break;
			}
			
			super.handleMessage(msg);
		}
	}	// End of class ActivityHandler
	
    /**
     * Auto-refresh Timer
     */
	private class RefreshTimerTask extends TimerTask {
		public RefreshTimerTask() {}
		
		public void run() {
			mActivityHandler.post(new Runnable() {
				public void run() {
					// TODO:
					mRefreshTimer = null;
				}
			});
		}
	}

}
