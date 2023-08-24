/**
 * 
 * AiCiA - Android IRC Client
 * 
 * Copyright (C)2010 GORRY.
 * 
 */

package net.gorry.aicia;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.KeyEvent;

import net.gorry.libaicia.BuildConfig;
import net.gorry.libaicia.R;

/**
 * 
 * システム設定の編集処理
 * 
 * @author GORRY
 *
 */
public class ActivitySystemConfig extends PreferenceActivity {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "ActivitySystemConfig";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private static Activity me;
	private boolean noFinishIt = false;

	private static final int ACTIVITY_SELECT_TTF = 1;
	

	/* アプリの一時退避
	 * onRestoreInstanceState()は来ないので注意
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (T) Log.v(TAG, M()+"@in");
		noFinishIt = true;
	}

	/**
	 * 作成
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in");
		super.onCreate(savedInstanceState);
		me = this;
		setTitle(R.string.pref_sys_title);

		final Bundle extras = getIntent().getExtras();
		boolean isLandscape = false;
		if (extras != null) {
			isLandscape = extras.getBoolean("islandscape");
		}
		final int pref;
		if (isLandscape) {
			if (Build.VERSION.SDK_INT >= 9) {
				// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
				setRequestedOrientation(6);
			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			pref = R.xml.pref_systemconfig_landscape;
		} else {
			if (Build.VERSION.SDK_INT >= 9) {
				// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
				setRequestedOrientation(7);
			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			pref = R.xml.pref_systemconfig_portrait;
		}

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
		SystemConfig.loadConfig();
		SystemConfig.clearForPreferenceActivity(sp);
		SystemConfig.setForPreferenceActivity(sp);
		addPreferencesFromResource(pref);

		// [外部フォントファイル]項目の処理
		{
			final String str = "pref_sys_advanced_externalfontpath";
			PreferenceScreen ps = (PreferenceScreen)findPreference(str);
			ps.setSummary(SystemConfig.externalFontPath);
			ps.setOnPreferenceClickListener(new OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference pc) {
					String key = pc.getKey();
					if (key.equals(str)) {
						return onPreferenceClick_ExternalFontPath(pc);
					}
					return false;
				}
			});
		}

		// [設定の書き出し]の処理
		{
			final String str = "pref_sys_advanced_export_config";
			PreferenceScreen ps = (PreferenceScreen)findPreference(str);
			ps.setOnPreferenceClickListener(new OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference pc) {
					String key = pc.getKey();
					if (key.equals(str)) {
						return onPreferenceClick_ExportConfig(pc);
					}
					return false;
				}
			});
		}

		// [設定の読み込み]の処理
		{
			final String str = "pref_sys_advanced_import_config";
			PreferenceScreen ps = (PreferenceScreen)findPreference(str);
			ps.setOnPreferenceClickListener(new OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference pc) {
					String key = pc.getKey();
					if (key.equals(str)) {
						return onPreferenceClick_ImportConfig(pc);
					}
					return false;
				}
			});
		}

		// [音声で通知]の処理
		{
			final String str = "pref_sys_action_alert_ring";
			CheckBoxPreference ps = (CheckBoxPreference)findPreference(str);
			String s2 = ps.getSummary().toString();
			if (s2.contains("%s")) {
				String path = SystemConfig.getExternalPath() + "ring.ogg";
				s2 = String.format(s2, path);
				ps.setSummary(s2);
			}
		}
	}

	/*
	 * 再起動
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public void onRestart() {
		if (T) Log.v(TAG, M()+"@in");
		super.onRestart();
	}

	/*
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (T) Log.v(TAG, M()+"@in");
		super.onStart();
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public synchronized void onResume() {
		if (T) Log.v(TAG, M()+"@in");
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
		if (T) Log.v(TAG, M()+"@in");
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
		if (T) Log.v(TAG, M()+"@in");
		super.onStop();
	}

	//
	/*
	 * 破棄
	 * @see android.preference.PreferenceActivity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (T) Log.v(TAG, M()+"@in");
		super.onDestroy();
	}

	/*
	 * コンフィギュレーション変更
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	/*
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		if (T) Log.v(TAG, M()+"onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
	}
	 */

	/*
	 * キー入力
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (T) Log.v(TAG, M()+"@in: keyCode="+keyCode+", event="+event);

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

			if (T) Log.v(TAG, M()+"@out: true");
			return true;
		}
		super.onKeyDown(keyCode, event);
		if (T) Log.v(TAG, M()+"@out: false");
		return false;
	}

	/**
	 * フォントパスの変更
	 * @param pref pref
	 * @return true
	 */
	public boolean onPreferenceClick_ExternalFontPath(Preference pref) {
		if (T) Log.v(TAG, M()+"@in: pref="+pref);

		final Intent intent = new Intent(
				me,
				ActivitySelectTtfFile.class
		);
		String lastPath = SystemConfig.externalFontPath;
		if ((lastPath == null) || (lastPath.length() == 0)) {
			lastPath = SystemConfig.externalFontPathDefault;
		}

		final Uri uri = Uri.parse("file://" + lastPath);
		intent.setData(uri);
		me.startActivityForResult(intent, ACTIVITY_SELECT_TTF);

		if (T) Log.v(TAG, M()+"@out: true");
		return true;
	}
	
	/**
	 * 環境のエクスポート
	 * @param pref pref
	 * @return true
	 */
	public boolean onPreferenceClick_ExportConfig(Preference pref) {
		if (T) Log.v(TAG, M()+"in: pref="+pref);

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
		final int rebootLevel = SystemConfig.getFromPreferenceActivity(sp);
		SystemConfig.saveConfig();
		SystemConfig.clearForPreferenceActivity(sp);
		final Intent intent = new Intent();
		intent.putExtra("rebootlevel", rebootLevel);
		intent.putExtra("exportconfig", 1);
		setResult(RESULT_OK, intent);
		finish();

		if (T) Log.v(TAG, M()+"@out: true");
		return true;
	}
	
	/**
	 * 環境のインポート
	 * @param pref pref
	 * @return true
	 */
	public boolean onPreferenceClick_ImportConfig(Preference pref) {
		if (T) Log.v(TAG, M()+"@in: pref="+pref);

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
		final int rebootLevel = SystemConfig.getFromPreferenceActivity(sp);
		SystemConfig.saveConfig();
		SystemConfig.clearForPreferenceActivity(sp);
		final Intent intent = new Intent();
		intent.putExtra("rebootlevel", rebootLevel);
		intent.putExtra("importconfig", 1);
		setResult(RESULT_OK, intent);
		finish();

		if (T) Log.v(TAG, M()+"@out: true");
		return true;
	}
	
	/*
	 * アクティビティの結果処理
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (T) Log.v(TAG, M()+"@in: requestCode="+requestCode+", resuleCode="+resultCode+", data="+data);

		Bundle extras = null;
		// int intentResult = 0;
		if (data != null) {
			extras = data.getExtras();
			if (extras != null) {
				// intentResult = extras.getInt("result");
			}
		}

		switch (requestCode) {
			case ACTIVITY_SELECT_TTF:
				if (T) Log.v(TAG, M()+"onActivityResult(): ACTIVITY_SELECT_TTF");
				if (extras != null) {
					final String path = extras.getString("path");
					final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
					SystemConfig.setForPreferenceActivity_ExternalFontPath(sp, path);
					PreferenceScreen ps = (PreferenceScreen)findPreference("pref_sys_advanced_externalfontpath");
					ps.setSummary(path);
				}
				break;
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	public static Activity getActivity() {
		return me;
	}

}
