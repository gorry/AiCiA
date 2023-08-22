/**
 * 
 */
package net.gorry.aicia;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.view.KeyEvent;

import net.gorry.libaicia.R;

/**
 * 
 * ウェブサイト一覧の編集
 * 
 * @author GORRY
 *
 */
public class ActivityExWebList extends PreferenceActivity  {
	private static final String TAG = "ActivityExWebList";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = true;
	private Activity me;
	private boolean noFinishIt = false;


	/* アプリの一時退避
	 * onRestoreInstanceState()は来ないので注意
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (I) Log.i(TAG, "onSaveInstanceState()");
		noFinishIt = true;
	}

	/**
	 * 作成
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (I) Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		me = this;
		setTitle(R.string.activitymain_java_exweb_edit_title);

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
		SystemConfig.loadConfig();
		SystemConfig.clearForPreferenceActivity(sp);
		SystemConfig.setForPreferenceActivity(sp);

		final int pref = R.xml.pref_exweblist;
		addPreferencesFromResource(pref);

		for (int i=0; i<SystemConfig.maxExWebSite; i++) {
			final String resName = "pref_exweblist_web"+(i+1);
			final Preference p = findPreference(resName);
			if (p != null) {
				p.setOnPreferenceChangeListener(new preferenceChangeListener());
				final String exWebSiteName = SystemConfig.exWebSiteName[i];
				final String exWebSiteUrl = SystemConfig.exWebSiteUrl[i];
				if ((exWebSiteName == null) || (exWebSiteName.length() == 0) || (exWebSiteUrl == null) || (exWebSiteUrl.length() == 0)) {
					//
				} else {
					p.setTitle(exWebSiteName);
					p.setSummary(exWebSiteUrl);
				}
			}
		}
	}

	/**
	 * DialogPreferenceの更新
	 */
	private class preferenceChangeListener implements OnPreferenceChangeListener {
		public boolean onPreferenceChange(final Preference pref, final Object newValue) {
			final int id = (Integer)newValue;
			if (id >= 0) {
				final String s = SystemConfig.exWebSiteName[id];
				if ((s != null) && (s.length() > 0)) {
					pref.setTitle(SystemConfig.exWebSiteName[id]);
					pref.setSummary(SystemConfig.exWebSiteUrl[id]);
				} else {
					pref.setTitle(R.string.activitymain_java_exweb_none);
					pref.setSummary("");
				}
			}
			return true;
		}
	}

	/*
	 * 再起動
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public void onRestart() {
		if (I) Log.i(TAG, "onRestart()");
		super.onRestart();
	}

	/*
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (I) Log.i(TAG, "onStart()");
		super.onStart();
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public synchronized void onResume() {
		if (I) Log.i(TAG, "onResume()");
		super.onResume();
		if (!noFinishIt) {
			//
		}
		noFinishIt = false;
	}

	/*
	 * 停止
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		if (I) Log.i(TAG, "onPause()");
		super.onPause();
		if (!noFinishIt) {
			//
		}
	}

	/*
	 * 中止
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		if (I) Log.i(TAG, "onStop()");
		super.onStop();
	}

	//
	/*
	 * 破棄
	 * @see android.preference.PreferenceActivity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (I) Log.i(TAG, "onDestroy()");
		super.onDestroy();
	}

	/*
	 * コンフィギュレーション変更
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	/*
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		if (I) Log.i(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
	}
	 */

	/*
	 *  (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onContentChanged()
	 */
	@Override
	public void onContentChanged() {
		if (I) Log.i(TAG, "onContentChanged()");
		super.onContentChanged();
	}

	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onPreferenceTreeClick(android.preference.PreferenceScreen, android.preference.Preference)
	 */
	@Override
	public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
		if (I) Log.i(TAG, "onPreferenceTreeClick()");
		return false;
	}

	/*
	 * キー入力
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (I) Log.i(TAG, "onConfigurationChanged()");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// アクティビティ終了として使う
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
			final int rebootLevel = SystemConfig.getFromPreferenceActivity(sp);
			SystemConfig.saveConfig();
			SystemConfig.clearForPreferenceActivity(sp);
			final Intent intent = new Intent();
			intent.putExtra("rebootlevel", rebootLevel);
			setResult(RESULT_OK, intent);
			finish();
			return true;
		}
		super.onKeyDown(keyCode, event);
		return false;
	}



}
