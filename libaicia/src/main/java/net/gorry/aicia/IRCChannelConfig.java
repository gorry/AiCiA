/**
 *
 */
package net.gorry.aicia;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.gorry.libaicia.R;

/**
 *
 * IRCﾁｬﾝﾈﾙ設定の読み書き処理
 *
 * @author GORRY
 *
 */
public class IRCChannelConfig {
	private static final String TAG = "IRCServerConfig";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;

	private final Context me;

	/** サブログに出すならtrue */
	public boolean mPutOnSublog;

	/** 常にサブログに出すならtrue */
	public boolean mPutOnSublogAll;

	/** 薄字メッセージをサブログに出すならtrue */
	public boolean mPutPaleTextOnSublog;

	/** アラートを出すならtrue */
	public boolean mUseAlert;

	/** 常にアラート扱いにするならtrue */
	public boolean mUseAlertAll;

	/** アラートをOS通知するならtrue */
	public boolean mAlertNotify;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 */
	public IRCChannelConfig(final Context context) {
		if (I) Log.i(TAG, "IRCChannelConfig()");
		me = context;
	}

	/**
	 * 設定読み込み
	 * @param serverId サーバ設定ID
	 * @param channelName チャンネル名
	 */
	public void loadConfig(final int serverId, final String channelName) {
		if (I) Log.i(TAG, "loadConfig()");
		loadConfigCore(serverId, channelName, false);
	}
	
	/**
	 * 設定インポート
	 * @param serverId サーバ設定ID
	 * @param channelName チャンネル名
	 */
	public void importConfig(final int serverId, final String channelName) {
		if (I) Log.i(TAG, "importConfig()");
		loadConfigCore(serverId, channelName, true);
	}
	
	/**
	 * 設定読み込み
	 * @param serverId サーバ設定ID
	 * @param channelName チャンネル名
	 * @param importing 設定を外部ファイルから入力するときtrue
	 */
	private void loadConfigCore(final int serverId, final String channelName, final boolean importing) {
		if (I) Log.i(TAG, "loadConfigCore()");
		final String id = serverId + ":" + channelName;
		String filename;
		if (importing) {
			filename = SystemConfig.getExternalPath() + SystemConfig.serverConfigExportFileName;
		} else {
			filename = "ircserver";
		}
		final MySharedPreferences pref = new MySharedPreferences(me, filename);
		if (importing) {
			pref.setActivity(ActivitySystemConfig.getActivity());
		}
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			return;
		}

		mPutOnSublog = pref.getBoolean(id + ":putonsublog", true);
		mPutOnSublogAll = pref.getBoolean(id + ":putonsublogall", false);
		mPutPaleTextOnSublog = pref.getBoolean(id + ":putpaletextonsublog", true);
		mUseAlert = pref.getBoolean(id + ":usealert", true);
		mUseAlertAll = pref.getBoolean(id + ":usealertall", false);
		mAlertNotify = pref.getBoolean(id + ":alertnotify", true);
	}

	/**
	 * 設定をコピー
	 * @param from コピー元
	 */
	public void copy(final IRCChannelConfig from) {
		if (I) Log.i(TAG, "copy()");
		mPutOnSublog = from.mPutOnSublog;
		mPutOnSublogAll = from.mPutOnSublogAll;
		mPutPaleTextOnSublog = from.mPutPaleTextOnSublog;
		mUseAlert = from.mUseAlert;
		mUseAlertAll = from.mUseAlertAll;
		mAlertNotify = from.mAlertNotify;
	}

	/**
	 * 設定を比較
	 * @param from 比較元
	 * @return 同じならtrue
	 */
	public boolean compare(final IRCChannelConfig from) {
		if (I) Log.i(TAG, "compare()");
		if (mPutOnSublog != from.mPutOnSublog) return false;
		if (mPutOnSublogAll != from.mPutOnSublogAll) return false;
		if (mPutPaleTextOnSublog != from.mPutPaleTextOnSublog) return false;
		if (mUseAlert != from.mUseAlert) return false;
		if (mUseAlertAll != from.mUseAlertAll) return false;
		if (mAlertNotify != from.mAlertNotify) return false;
		return true;
	}

	/**
	 * 設定保存
	 * @param serverId サーバ設定ID
	 * @param channelName チャンネル名
	 * @return 成功ならtrue
	 */
	public boolean saveConfig(final int serverId, final String channelName) {
		if (I) Log.i(TAG, "saveConfig()");
		return saveConfigCore(serverId, channelName, false);
	}
	
	/**
	 * 設定エクスポート
	 * @param serverId サーバ設定ID
	 * @param channelName チャンネル名
	 * @return 成功ならtrue
	 */
	public boolean exportConfig(final int serverId, final String channelName) {
		if (I) Log.i(TAG, "exportConfig()");
		return saveConfigCore(serverId, channelName, true);
	}
	
	/**
	 * 設定保存
	 * @param serverId サーバ設定ID
	 * @param channelName チャンネル名
	 * @param exporting 設定を外部ファイルに出力するときtrue
	 * @return 成功ならtrue
	 */
	public boolean saveConfigCore(final int serverId, final String channelName, final boolean exporting) {
		if (I) Log.i(TAG, "saveConfigCore()");
		String filename;
		if (exporting) {
			filename = SystemConfig.getExternalPath() + SystemConfig.serverConfigExportFileName;
		} else {
			filename = "ircserver";
		}
		final MySharedPreferences pref = new MySharedPreferences(me, filename);
		if (exporting) {
			pref.setActivity(ActivitySystemConfig.getActivity());
		}
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_error_saveconfig));
			return false;
		}
		final MySharedPreferences.Editor editor = pref.edit();
		final String id = serverId + ":" + channelName;
		editor.putBoolean(id + ":putonsublog", mPutOnSublog);
		editor.putBoolean(id + ":putonsublogall", mPutOnSublogAll);
		editor.putBoolean(id + ":putpaletextonsublog", mPutPaleTextOnSublog);
		editor.putBoolean(id + ":usealert", mUseAlert);
		editor.putBoolean(id + ":usealertall", mUseAlertAll);
		editor.putBoolean(id + ":alertnotify", mAlertNotify);
		return editor.commit() >= 1;
	}

	/**
	 * 設定消去
	 * @param serverId サーバ設定ID
	 * @param channelName チャンネル設定ID
	 */
	public void deleteConfig(final int serverId, final String channelName) {
		if (I) Log.i(TAG, "deleteConfig()");
		final SharedPreferences pref = me.getSharedPreferences("ircserver", 0);
		final String id = serverId + ":" + channelName;
		final SharedPreferences.Editor editor = pref.edit();
		editor.remove(id + ":putonsublog");
		editor.remove(id + ":putonsublogall");
		editor.remove(id + ":putpaletextonsublog");
		editor.remove(id + ":usealert");
		editor.remove(id + ":usealertall");
		editor.remove(id + ":alertnotify");
		editor.commit();
	}


}
