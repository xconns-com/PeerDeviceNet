package com.xconns.peerdevicenet.example.connsettings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.xconns.peerdevicenet.DeviceInfo;
import com.xconns.peerdevicenet.NetInfo;
import com.xconns.peerdevicenet.RouterConnectionClient;

public class PrefsFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	Activity mCtx = null;

	// --- GUI ---

	public static final String PREF_KEY_DEVICE_NAME = "device_name";
	public static final String PREF_KEY_MAX_SESSIONS = "max_sessions";
	public static final String PREF_KEY_CONN_PIN = "conn_pin";
	public static final String PREF_KEY_AUTO_SCAN = "auto_scan";
	public static final String PREF_KEY_AUTO_CONN = "auto_conn";
	public static final String PREF_KEY_AUTO_ACCEPT = "auto_accept";
	public static final String PREF_KEY_SEARCH_TIMEOUT = "search_timeout";
	public static final String PREF_KEY_CONN_TIMEOUT = "conn_timeout";
	public static final String PREF_KEY_PEER_LIVE_TIMEOUT = "live_timeout";
	public static final String PREF_KEY_USE_SSL = "use_ssl";
	public static final String PREF_KEY_SHUT_DOWN = "shutdown";

	PreferenceScreen prefScreen = null;

	EditTextPreference mDeviceNamePref = null;

	ListPreference mSearchTimeoutPref = null;
	String mSearchTimeout = null;

	ListPreference mLiveTimeoutPref = null;
	String mLiveTimeout = null;

	ListPreference mConnTimeoutPref = null;
	String mConnTimeout = null;

	ListPreference mMaxSessionsPref = null;
	String mMaxSessions = null;

	CheckBoxPreference mConnPINPref = null;
	boolean mConnPIN = false;

	CheckBoxPreference mAutoScanPref = null;
	boolean mAutoScan = false;

	CheckBoxPreference mAutoConnPref = null;
	boolean mAutoConn = false;

	CheckBoxPreference mAutoAcceptPref = null;
	boolean mAutoAccept = false;

	CheckBoxPreference mUseSSLPref = null;
	boolean useSSL = true;
	
	// --- preference values ---

	String mDeviceName = null;//my device name
	int maxSessions = 10; //default to max 10 sessions
	int liveTimeout = 4000; //default to 4 seconds
	int searchTimeout = 30000; // default to 30 seconds
	int connTimeout = 5000; // default to 5 seconds

	// --- interface to router core service ---
	RouterConnectionClient connClient = null;

	// Debugging
	static final String TAG = "PrefsFragment";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.conn_setting_prefs);

	}

	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		mCtx = activity;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);

		// bind variables to preferences after activity and its view hierarchy created
		prefScreen = getPreferenceScreen();

		mDeviceNamePref = (EditTextPreference) prefScreen
				.findPreference(PREF_KEY_DEVICE_NAME);
		mDeviceName = mDeviceNamePref.getText();
		if (mDeviceName == null || mDeviceName.length() == 0 || mDeviceName.equalsIgnoreCase("unknown")) {
			String buildName = android.os.Build.MODEL;
			if (buildName != null && buildName.length() > 0) {
				mDeviceName = buildName;
			}
		}
		mDeviceNamePref.setSummary(mDeviceName);
		
		mSearchTimeoutPref = (ListPreference) prefScreen
				.findPreference(PREF_KEY_SEARCH_TIMEOUT);
		mSearchTimeout = mSearchTimeoutPref.getValue();
		searchTimeout = Integer.parseInt(mSearchTimeout) * 1000;
		mSearchTimeoutPref.setSummary(mSearchTimeout + " seconds");

		mMaxSessionsPref = (ListPreference) prefScreen
				.findPreference(PREF_KEY_MAX_SESSIONS);
		mMaxSessions = mMaxSessionsPref.getValue();
		maxSessions = Integer.parseInt(mMaxSessions);
		mMaxSessionsPref.setSummary(mMaxSessions + " sessions");

		mLiveTimeoutPref = (ListPreference) prefScreen
				.findPreference(PREF_KEY_PEER_LIVE_TIMEOUT);
		mLiveTimeout = mLiveTimeoutPref.getValue();
		mLiveTimeoutPref.setSummary(mLiveTimeout + " seconds");
		liveTimeout = Integer.parseInt(mLiveTimeout) * 1000;

		mConnTimeoutPref = (ListPreference) prefScreen
				.findPreference(PREF_KEY_CONN_TIMEOUT);
		mConnTimeout = mConnTimeoutPref.getValue();
		connTimeout = Integer.parseInt(mConnTimeout) * 1000;
		mConnTimeoutPref.setSummary(mConnTimeout + " seconds");

		mConnPINPref = (CheckBoxPreference) prefScreen
				.findPreference(PREF_KEY_CONN_PIN);
		mConnPIN = mConnPINPref.isChecked();

		mAutoScanPref = (CheckBoxPreference) prefScreen
				.findPreference(PREF_KEY_AUTO_SCAN);
		mAutoScan = mAutoScanPref.isChecked();

		mAutoConnPref = (CheckBoxPreference) prefScreen
				.findPreference(PREF_KEY_AUTO_CONN);
		mAutoConn = mAutoConnPref.isChecked();

		mAutoAcceptPref = (CheckBoxPreference) prefScreen
				.findPreference(PREF_KEY_AUTO_ACCEPT);
		mAutoAccept = mAutoAcceptPref.isChecked();

		mUseSSLPref = (CheckBoxPreference) prefScreen
				.findPreference(PREF_KEY_USE_SSL);
		useSSL = mUseSSLPref.isChecked();
		
		//init client to Router Connection Service

		// bind to router connection service
		// other Connectors or ConnectionManager should be responsible 
		// for keeping Router service alive by startService()
		connClient = new RouterConnectionClient(mCtx, connHandler);
		connClient.bindService();
		
		//update Router ConnectionService about current settings
		connClient.setConnectionInfo(
				mDeviceName, 
				useSSL, 
				liveTimeout,
				connTimeout, 
				searchTimeout
		);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "PrefFragment destroyed, unbind connHnadler");
		super.onDestroy();
		connClient.unbindService();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		Log.d(TAG, "pref changed: " + key);

		// get latest preference values
		if (key.equals(PREF_KEY_DEVICE_NAME)) {
			mDeviceName = mDeviceNamePref.getText();
			mDeviceNamePref.setSummary(mDeviceName);
		} else if (key.equals(PREF_KEY_CONN_PIN)) {
			mConnPIN = mConnPINPref.isChecked();
		} else if (key.equals(PREF_KEY_AUTO_SCAN)) {
			mAutoScan = mAutoScanPref.isChecked();
		} else if (key.equals(PREF_KEY_AUTO_CONN)) {
			mAutoConn = mAutoConnPref.isChecked();
		} else if (key.equals(PREF_KEY_AUTO_ACCEPT)) {
			mAutoAccept = mAutoAcceptPref.isChecked();
		} else if (key.equals(PREF_KEY_USE_SSL)) {
			useSSL = mUseSSLPref.isChecked();
		} else if (key.equals(PREF_KEY_SEARCH_TIMEOUT)) {
			mSearchTimeout = mSearchTimeoutPref.getValue();
			searchTimeout = Integer.parseInt(mSearchTimeout) * 1000;
			mSearchTimeoutPref.setSummary(mSearchTimeout + " seconds");
		} else if (key.equals(PREF_KEY_PEER_LIVE_TIMEOUT)) {
			mLiveTimeout = mLiveTimeoutPref.getValue();
			mLiveTimeoutPref.setSummary(mLiveTimeout + " seconds");
			liveTimeout = Integer.parseInt(mLiveTimeout) * 1000;
		} else if (key.equals(PREF_KEY_CONN_TIMEOUT)) {
			mConnTimeout = mConnTimeoutPref.getValue();
			connTimeout = Integer.parseInt(mConnTimeout) * 1000;
			mConnTimeoutPref.setSummary(mConnTimeout + " seconds");
		} else if (key.equals(PREF_KEY_MAX_SESSIONS)) {
			mMaxSessions = mMaxSessionsPref.getValue();
			maxSessions = Integer.parseInt(mMaxSessions);
			mMaxSessionsPref.setSummary(mMaxSessions + " sessions");
		}

		//update settings in Router ConnectionService
		connClient.setConnectionInfo(mDeviceName, useSSL, liveTimeout, connTimeout, searchTimeout);
	}

	//placeholder for now
	RouterConnectionClient.ConnectionHandler connHandler = new RouterConnectionClient.ConnectionHandler() {

		@Override
		public void onGetConnectionInfo(String devName, boolean uSSL,
				int liveTime, int connTime, int searchTime) {
		}

		@Override
		public void onSetConnectionInfo() {
		}

		public void onError(String errInfo) {
			Log.e(TAG, errInfo);
		}

		public void onConnected(DeviceInfo dev) {
		}

		public void onDisconnected(DeviceInfo dev) {
		}

		public void onGetDeviceInfo(DeviceInfo device) {
		}

		public void onGetPeerDevices(DeviceInfo[] devices) {
		}

		public void onConnecting(DeviceInfo device, byte[] token) {
			//
		}

		public void onConnectionFailed(DeviceInfo device, int rejectCode) {
			//
		}

		@Override
		public void onSearchStart(DeviceInfo groupLeader) {
		}

		public void onSearchFoundDevice(DeviceInfo dev, boolean uSSL) {
		}

		public void onSearchComplete() {
		}

		@Override
		public void onGetNetworks(NetInfo[] nets) {
		}

		@Override
		public void onGetActiveNetwork(NetInfo net) {
		}

		@Override
		public void onNetworkConnected(NetInfo net) {
		}

		@Override
		public void onNetworkDisconnected(NetInfo net) {
		}

		@Override
		public void onNetworkActivated(NetInfo net) {
		}

		@Override
		public void onNetworkConnecting(NetInfo net) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onNetworkConnectionFailed(NetInfo net) {
			// TODO Auto-generated method stub
			
		}
	};
}
