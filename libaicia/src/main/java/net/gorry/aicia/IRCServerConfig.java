/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.gorry.libaicia.R;

/**
 *
 * IRCサーバ設定の読み書き処理
 *
 * @author GORRY
 *
 */
public class IRCServerConfig {
	private static final String TAG = "IRCServerConfig";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;

	private final Context me;

	/** サーバ設定名 */
	public String mServerName;

	/** 接続ホスト名 */
	public String mHost;

	/** 接続ポート番号 */
	public int mPort;

	/** 接続パスワード */
	public String mPass;

	/** 初期nick */
	public String mNick;

	/** ユーザー名 */
	public String mUsername;

	/** ユーザー実名 */
	public String mRealname;

	/** 文字列エンコーディング */
	public String mEncoding;

	/** SSL接続するならtrue */
	public boolean mUseSsl;

	/** メインメニューの[Connect]ボタンで接続するならtrue */
	public boolean mAutoConnect;

	/** 切断時に自動再接続を行うならtrue */
	public boolean mAutoReconnect;

	/** TweetIRCGateway(TIG)用オプションを使うならtrue */
	public boolean mForTIG;

	/** システムメッセージをサブログにも出力するならtrue */
	public boolean mPutPaleTextOnSublog;

	/** 初期接続チャンネル名 */
	public String mConnectingChannel;

	/** アラートキーワード */
	public String mAlertKeywords;

	/** チャンネル名リスト */
	public ArrayList<String> mChannelNames = new ArrayList<String>();

	/** アラートをOS通知するならtrue */
	public boolean mAlertNotify;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 */
	public IRCServerConfig(final Context context) {
		if (I) Log.i(TAG, "IRCServerConfig()");
		me = context;
	}

	/**
	 * 設定読み込み
	 * @param configId サーバ設定ID
	 * @return 読み込んだ設定のバージョン
	 */
	public String loadConfig(final int configId) {
		if (I) Log.i(TAG, "loadConfig()");
		return loadConfigCore(configId, false);
	}
	
	/**
	 * 設定インポート
	 * @param configId サーバ設定ID
	 * @return 読み込んだ設定のバージョン
	 */
	public String importConfig(final int configId) {
		if (I) Log.i(TAG, "importConfig()");
		return loadConfigCore(configId, true);
	}
	
	/**
	 * 設定読み込み
	 * @param configId サーバ設定ID
	 * @param importing 設定を外部ファイルから入力するときtrue
	 * @return 読み込んだ設定のバージョン
	 */
	private String loadConfigCore(final int configId, final boolean importing) {
		if (I) Log.i(TAG, "loadConfigCore()");
		final String id = String.valueOf(configId);
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
			return SystemConfig.getVersionString();
		}

		String version = pref.getString("fileVersion", SystemConfig.getVersionString());

		mServerName = pref.getString(id + ":servername", "");
		mHost = pref.getString(id + ":host", "");
		mPort = pref.getInt(id + ":port", 6667);
		mPass = pref.getString(id + ":pass", "");
		mNick = pref.getString(id + ":nick", "");
		mUsername = pref.getString(id + ":username", "");
		mRealname = pref.getString(id + ":realname", "");
		mEncoding = pref.getString(id + ":encoding", "ISO-2022-JP");
		mUseSsl = pref.getBoolean(id + ":usessl", false);
		mAutoConnect = pref.getBoolean(id + ":autoconnect", true);
		mAutoReconnect = pref.getBoolean(id + ":autoreconnect", true);
		mForTIG = pref.getBoolean(id + ":fortig", false);
		mPutPaleTextOnSublog = pref.getBoolean(id + ":putpaletextonsublog", true);
		mAlertNotify = pref.getBoolean(id + ":alertnotify", true);
		mConnectingChannel = pref.getString(id + ":channel", "");
		mAlertKeywords = pref.getString(id + ":alertkeywords", "");
		loadChannelNamesCore(configId, mChannelNames, importing);
		
		return version;
	}

	/**
	 * 設定をコピー
	 * @param from コピー元
	 */
	public void copy(final IRCServerConfig from) {
		if (I) Log.i(TAG, "copy()");
		mServerName = new String(from.mServerName);
		mHost = new String(from.mHost);
		mPort = from.mPort;
		mPass = new String(from.mPass);
		mNick = new String(from.mNick);
		mUsername = new String(from.mUsername);
		mRealname = new String(from.mRealname);
		mEncoding = new String(from.mEncoding);
		mUseSsl = from.mUseSsl;
		mAutoConnect = from.mAutoConnect;
		mAutoReconnect = from.mAutoReconnect;
		mForTIG = from.mForTIG;
		mPutPaleTextOnSublog = from.mPutPaleTextOnSublog;
		mAlertNotify = from.mAlertNotify;
		mConnectingChannel = new String(from.mConnectingChannel);
		mAlertKeywords = new String(from.mAlertKeywords);
		mChannelNames.clear();
		mChannelNames.addAll(from.mChannelNames);
	}

	/**
	 * 設定を比較
	 * @param from 比較元
	 * @return 同じならtrue
	 */
	public boolean compare(final IRCServerConfig from) {
		if (I) Log.i(TAG, "compare()");
		if (!mServerName.equals(from.mServerName)) return false;
		if (!mHost.equals(from.mHost)) return false;
		if (mPort != from.mPort) return false;
		if (!mPass.equals(from.mPass)) return false;
		if (!mNick.equals(from.mNick)) return false;
		if (!mUsername.equals(from.mUsername)) return false;
		if (!mRealname.equals(from.mRealname)) return false;
		if (!mEncoding.equals(from.mEncoding)) return false;
		if (mUseSsl != from.mUseSsl) return false;
		if (mAutoConnect != from.mAutoConnect) return false;
		if (mAutoReconnect != from.mAutoReconnect) return false;
		if (mForTIG != from.mForTIG) return false;
		if (mPutPaleTextOnSublog != from.mPutPaleTextOnSublog) return false;
		if (mAlertNotify != from.mAlertNotify) return false;
		if (!mConnectingChannel.equals(from.mConnectingChannel)) return false;
		if (!mAlertKeywords.equals(from.mAlertKeywords)) return false;
		// mChannelNamesは比較不要
		// mIsConnectedは比較不要
		return true;
	}

	/**
	 * 設定保存
	 * @param configId サーバ設定ID
	 * @return 成功ならtrue
	 */
	public boolean saveConfig(final int configId) {
		if (I) Log.i(TAG, "saveConfig()");
		return saveConfigCore(configId, false);
	}

	/**
	 * 設定エクスポート
	 * @param configId サーバ設定ID
	 * @return 成功ならtrue
	 */
	public boolean exportConfig(final int configId) {
		if (I) Log.i(TAG, "exportConfig()");
		return saveConfigCore(configId, true);
	}

	/**
	 * 設定保存
	 * @param configId サーバ設定ID
	 * @param exporting 設定を外部ファイルに出力するときtrue
	 * @return 成功ならtrue
	 */
	private boolean saveConfigCore(final int configId, final boolean exporting) {
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

		final String version = SystemConfig.getVersionString();
		editor.putString("fileVersion", version);

		final String id = String.valueOf(configId);
		editor.putString(id + ":servername", mServerName);
		editor.putString(id + ":host", mHost);
		editor.putInt(id + ":port", mPort);
		editor.putString(id + ":pass", mPass);
		editor.putString(id + ":nick", mNick);
		editor.putString(id + ":username", mUsername);
		editor.putString(id + ":realname", mRealname);
		editor.putString(id + ":encoding", mEncoding);
		editor.putBoolean(id + ":usessl", mUseSsl);
		editor.putBoolean(id + ":autoconnect", mAutoConnect);
		editor.putBoolean(id + ":autoreconnect", mAutoReconnect);
		editor.putBoolean(id + ":fortig", mForTIG);
		editor.putBoolean(id + ":putpaletextonsublog", mPutPaleTextOnSublog);
		editor.putBoolean(id + ":alertnotify", mAlertNotify);
		editor.putString(id + ":channel", mConnectingChannel);
		editor.putString(id + ":alertkeywords", mAlertKeywords);
		ret = editor.commit();
		if (ret < 1) return false;
		boolean f = saveChannelNamesCore(configId, mChannelNames, exporting);
		if (!f) return false;
		return true;
	}

	/**
	 * 設定消去
	 * @param configId サーバ設定ID
	 */
	public void deleteConfig(final int configId) {
		if (I) Log.i(TAG, "deleteConfig()");
		final SharedPreferences pref = me.getSharedPreferences("ircserver", 0);
		final SharedPreferences.Editor editor = pref.edit();
		final String id = String.valueOf(configId);
		editor.remove(id + ":servername");
		editor.remove(id + ":host");
		editor.remove(id + ":port");
		editor.remove(id + ":pass");
		editor.remove(id + ":nick");
		editor.remove(id + ":username");
		editor.remove(id + ":realname");
		editor.remove(id + ":encoding");
		editor.remove(id + ":usessl");
		editor.remove(id + ":autoconnect");
		editor.remove(id + ":autoreconnect");
		editor.remove(id + ":fortig");
		editor.remove(id + ":putpaletextonsublog");
		editor.remove(id + ":alertnotify");
		editor.remove(id + ":channel");
		editor.remove(id + ":alertkeywords");
		final String id2 = configId + ":channels";
		final int no = pref.getInt(id2, 0);
		for (int i=0; i<no; i++) {
			final String id3 = configId + ":channelname:" + i;
			final String ch = pref.getString(id3, "");
			final IRCChannelConfig chconfig = new IRCChannelConfig(me);
			chconfig.deleteConfig(configId, ch);
			editor.remove(id3);
		}
		editor.remove(id2);

		editor.remove(id + ":isconnected");
	
		editor.commit();
	}

	/**
	 * チャンネル名の登録
	 * @param configId サーバ設定ID
	 * @param channelName チャンネル名
	 * @return チャンネルID
	 */
	public int registerChannelName(final int configId, final String channelName) {
		if (I) Log.i(TAG, "registerChannelName()");
		
		ArrayList<String> channelNames = new ArrayList<String>();
		loadChannelNames(configId, channelNames);
		final String chNameLower = channelName.toLowerCase();
		for (int i=0; i<channelNames.size(); i++) {
			String ch = channelNames.get(i);
			if (ch != null) {
				if (ch.equalsIgnoreCase(chNameLower)) {
					return i;
				}
			}
		}
		
		channelNames.add(chNameLower);
		saveChannelNames(configId, channelNames);
		return channelNames.size();
		
		/*
		final SharedPreferences pref = me.getSharedPreferences("ircserver", 0);
		int no = 0;
		while (true) {
			final String id = configId + ":channelname:" + no;
			final String ch = pref.getString(id, "");
			if (ch == null) break;
			if (ch.equals("")) break;
			if (ch.equalsIgnoreCase(channelName)) {
				return no;
			}
			no++;
		}
		final SharedPreferences.Editor editor = pref.edit();
		final String id = configId + ":channelname:" + no;
		editor.putString(id, channelName);
		final String id2 = configId + ":channels";
		editor.putInt(id2, no+1);
		editor.commit();
		return (no);
		*/
	}

	/**
	 * チャンネル名リストの読み込み
	 * @param configId サーバ設定ID
	 * @param channelNames チャンネル名リストを返す
	 */
	public void loadChannelNames(final int configId, ArrayList<String> channelNames) {
		if (I) Log.i(TAG, "loadChannelNames()");
		loadChannelNamesCore(configId, channelNames, false);
	}
	
	/**
	 * チャンネル名リストのインポート
	 * @param configId サーバ設定ID
	 * @param channelNames チャンネル名リストを返す
	 */
	public void importChannelNames(final int configId, ArrayList<String> channelNames) {
		if (I) Log.i(TAG, "importChannelNames()");
		loadChannelNamesCore(configId, channelNames, true);
	}
	
	/**
	 * チャンネル名リストの読み込み
	 * @param configId サーバ設定ID
	 * @param channelNames チャンネル名リストを返す
	 * @param importing 設定を外部ファイルから入力するときtrue
	 */
	public void loadChannelNamesCore(final int configId, ArrayList<String> channelNames, final boolean importing) {
		if (I) Log.i(TAG, "loadChannelNamesCore()");
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
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_error_loadconfig));
			return;
		}
		channelNames.clear();
		final String id2 = configId + ":channels";
		final int no = pref.getInt(id2, 0);
		for (int i=0; i<no; i++) {
			final String id = configId + ":channelname:" + i;
			final String ch = pref.getString(id, "");
			if (ch == null) break;
			if (ch.equals("")) break;
			channelNames.add(ch);
		}
	}
	
	/**
	 * チャンネル名リストの保存
	 * @param configId サーバ設定ID
	 * @param channelNames チャンネル名リストを返す
	 * @return 成功ならtrue
	 */
	public boolean saveChannelNames(final int configId, ArrayList<String> channelNames) {
		if (I) Log.i(TAG, "saveChannelNames()");
		return saveChannelNamesCore(configId, channelNames, false);
	}
	
	/**
	 * チャンネル名リストのインポート
	 * @param configId サーバ設定ID
	 * @param channelNames チャンネル名リストを返す
	 * @return 成功ならtrue
	 */
	public boolean exportChannelNames(final int configId, ArrayList<String> channelNames) {
		if (I) Log.i(TAG, "exportChannelNames()");
		return saveChannelNamesCore(configId, channelNames, true);
	}
	
	/**
	 * チャンネル名リストの保存
	 * @param configId サーバ設定ID
	 * @param channelNames チャンネル名リスト
	 * @param exporting 設定を外部ファイルに出力するときtrue
	 * @return 成功ならtrue
	 */
	public boolean saveChannelNamesCore(final int configId, ArrayList<String> channelNames, final boolean exporting) {
		if (I) Log.i(TAG, "saveChannelNamesCore()");
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
		final String id2 = configId + ":channels";
		editor.putInt(id2, channelNames.size());
		for (int i=0; i<channelNames.size(); i++) {
			final String id = configId + ":channelname:" + i;
			String ch = channelNames.get(i);
			editor.putString(id, ch);
		}
		return (editor.commit() >= 1);
	}

	/**
	 * 接続中かどうかのフラグを返す
	 * @param configId サーバ設定ID
	 * @return 接続中ならtrue
	 */
	public boolean isConnectedFlag(final int configId) {
		if (I) Log.i(TAG, "isConnected()");
		final String id = String.valueOf(configId);
		final MySharedPreferences pref = new MySharedPreferences(me, "ircserver");
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			return false;
		}
		return pref.getBoolean(id + ":isconnected", false);
	}

	/**
	 * 接続中かどうかのフラグを保存
	 * @param configId サーバ設定ID
	 * @param sw 接続中ならtrue
	 * @return 保存成功ならtrue
	 */
	public boolean setConnectedFlag(final int configId, final boolean sw) {
		if (I) Log.i(TAG, "setConnected()");
		final MySharedPreferences pref = new MySharedPreferences(me, "ircserver");
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_error_saveconfig));
			return false;
		}
		final MySharedPreferences.Editor editor = pref.edit();
		final String id = String.valueOf(configId);
		editor.putBoolean(id + ":isconnected", sw);
		ret = editor.commit();
		return  (ret >= 1);
	}




}
