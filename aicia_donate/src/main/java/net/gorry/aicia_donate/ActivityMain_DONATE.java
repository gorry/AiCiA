/**
 * 
 * AiCiA - Android IRC Client
 * 
 * Copyright (C)2010 GORRY.
 * 
 */
package net.gorry.aicia_donate;

import net.gorry.aicia.ActivityMain;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * 有料版
 * 
 * @author GORRY
 *
 */
public class ActivityMain_DONATE extends Activity {
	private static final String TAG = "ActivityMain_DONATE";
	private static final boolean V = false;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (V) Log.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		final Intent i1 = getIntent();
		i1.setPackage(this.getPackageName());
		String initOpenServerName = null;
		if (i1 != null) {
			final Bundle extras = i1.getExtras();
			if (extras != null) {
				initOpenServerName = extras.getString("serverName");
			}
		}

		final Intent intent = new Intent(
				this,
				ActivityMain.class
		);
		intent.setPackage(this.getPackageName());
		intent.putExtra("donate", true);
		intent.putExtra("serverName", initOpenServerName);
		startActivity(intent);
		if (V) Log.v(TAG, "finish");
		finish();
	}

}