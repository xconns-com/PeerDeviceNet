package com.xconns.peerdevicenet.example.connsettings;

import android.app.Activity;
import android.os.Bundle;

public class ConnSettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PrefsFragment()).commit();
		}
	}

}
