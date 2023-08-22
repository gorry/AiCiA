/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;

import net.gorry.libaicia.R;

/**
 *
 * IRCサーバリスト処理
 *
 * @author GORRY
 *
 */
public class IRCServerList {
	private static final String TAG = "IRCServerList";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;
	private static final boolean D2 = false;

	private ArrayList<IRCServer> mIrcServerList = new ArrayList<IRCServer>();
	private final HashMap<String, Integer> mIrcServerNameMap = new HashMap<String, Integer>();
	private String mCurrentServerName;
	private IRCServerListEventListener[] mListeners = new IRCServerListEventListener[0];
	private final Context me;
	private ArrayList<SpannableStringBuilder> mSpanOtherChannelLog;
	private final boolean mConnect;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 * @param connect IRCサーバに接続する実体として使用するときはtrue
	 */
	public IRCServerList(final Context context, final boolean connect) {
		if (D) Log.d(TAG, "IRCServerList()");
		me = context;
		mConnect = connect;
		mSpanOtherChannelLog = new ArrayList<SpannableStringBuilder>();
		echoMessageToOther("Sub-log");
	}

	/**
	 * 破棄
	 */
	public void dispose() {
		for (int id=0; id<mIrcServerList.size(); id++) {
			final IRCServer ircServer = mIrcServerList.get(id);
			ircServer.close();
		}
		mIrcServerList = null;
	}

	/**
	 * リスナへのイベント転送処理
	 */
	private class IRCServerListener implements IRCServerEventListener {
		public synchronized void receiveMessageToChannel(final String serverName, final String channel, final String nick, final String dateMsg, final SpannableStringBuilder ssb, final boolean forceSubLog, final boolean forbidSubLog, final boolean alert) {
			doReceiveMessageToChannel(serverName, channel, nick, dateMsg, ssb, forceSubLog, forbidSubLog, alert);
		}

		public void createServer(final String serverName, final String dateMsg) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].createServer(serverName, dateMsg);
			}
		}

		public void removeServer(final String serverName, final String dateMsg) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].removeServer(serverName, dateMsg);
			}
		}

		public void createChannel(final String serverName, final String channel, final String dateMsg) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].createChannel(serverName, channel, dateMsg);
			}
		}

		public void removeChannel(final String serverName, final String channel, final String dateMsg) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].removeChannel(serverName, channel, dateMsg);
			}
		}

		public void changeNick(final String serverName, final String oldNick, final String newNick, final String dateMsg) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].changeNick(serverName, oldNick, newNick, dateMsg);
			}
		}

		public void changeTopic(final String serverName, final String channel, final String topic, final String dateMsg) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].changeTopic(serverName, channel, topic, dateMsg);
			}
		}

		public void receiveConnect(final String serverName) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].receiveConnect(serverName);
			}
		}

		public void receiveDisconnect(final String serverName) {
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].receiveDisconnect(serverName);
			}
		}
	}

	/**
	 * リスナ登録処理
	 * @param l リスナ
	 */
	public synchronized void addEventListener(final IRCServerListEventListener l) {
		if (I) Log.i(TAG, "addEventListener()");
		if (l == null) {
			throw new IllegalArgumentException("Listener is null.");
		}
		final int len = mListeners.length;
		final IRCServerListEventListener[] oldListeners = mListeners;
		mListeners = new IRCServerListEventListener[len + 1];
		System.arraycopy(oldListeners, 0, mListeners, 0, len);
		mListeners[len] = l;
	}

	/**
	 * リスナ削除処理
	 * @param l リスナ
	 * @return true 正常に削除された
	 */
	public synchronized boolean removeEventListener(final IRCServerListEventListener l) {
		if (I) Log.i(TAG, "removeEventListener()");
		if (l == null) {
			return false;
		}
		int index = -1;
		for (int i=0; i<mListeners.length; i++) {
			if (mListeners[i].equals(l)) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return false;
		}
		mListeners[index] = null;
		final int len = mListeners.length - 1;
		final IRCServerListEventListener[] newListeners = new IRCServerListEventListener[len];
		for (int i=0, j=0; i<len; j++) {
			if (mListeners[j] != null) {
				newListeners[i++] = mListeners[j];
			}
		}
		mListeners = newListeners;
		return true;
	}

	/**
	 * IRCメッセージ処理
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param nick Nick
	 * @param dateMsg 日付時刻
	 * @param ssb Spanメッセージ
	 * @param forceSublog サブログへ出力するときtrue
	 * @param forbidSublog サブログへ出力しないときtrue
	 * @param alert アラートする必要があるときtrue
	 */
	public synchronized void doReceiveMessageToChannel(final String serverName, final String channel, final String nick, final String dateMsg, final SpannableStringBuilder ssb, final boolean forceSublog, final boolean forbidSublog, final boolean alert) {
		int serverId = -1;
		int channelId = -1;
		if (mIrcServerList == null) return;
		serverId = getServerId(serverName);
		if (serverId >= 0) {
			channelId = mIrcServerList.get(serverId).getChannelId(channel);
		}

		// otherChannel用のログを生成
		final SpannableStringBuilder ssbOther = IRCMsg.colorDateMsg(dateMsg, SystemConfig.subLogDateColor[SystemConfig.now_colorSet]);
		if ((channel != null) && (channel.length() > 0) && (!channel.equals(IRCMsg.sOtherChannelName))) {
			String ids = "";
			if ((0 <= serverId) && (serverId < 10)) {
				final String idServer = "1234567890";
				ids = idServer.substring(serverId, serverId+1);
				if ((1 <= channelId) && (channelId <= 26)) {  // 0はシステムチャンネルなので割り当てない
					final String idChannel = "abcdefghijklmnopqrstuvwxyz";
					ids = ids + idChannel.substring(channelId-1, channelId);
				}
				ids = ids + ":";
			}
			final SpannableStringBuilder ssbChannel = new SpannableStringBuilder("<" + ids + IRCMsg.channelName(channel) + "> ");
			final ForegroundColorSpan c = new ForegroundColorSpan(SystemConfig.subLogTextColor[SystemConfig.now_colorSet]);
			ssbChannel.setSpan(c, 0, ssbChannel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssbOther.append(ssbChannel);
		}
		if ((nick != null) && (nick.length() > 0)) {
			ssbOther.append(IRCMsg.colorNick(nick, SystemConfig.subLogTextColor[SystemConfig.now_colorSet]));
		}
		ssbOther.append(ssb);
		ssbOther.append("\n");

		boolean toSublog = true;
		if (forbidSublog) {
			toSublog = false;
		} else if (!forceSublog) {
			if ((serverName != null) && (channel != null) && (mCurrentServerName != null)) {
				final String currentChannel = getCurrentChannel(mCurrentServerName);
				if (currentChannel != null) {
					if (serverName.equalsIgnoreCase(mCurrentServerName) &&
							channel.equalsIgnoreCase(currentChannel)) {
						toSublog = false;
					}
					if (serverId >= 0) {
						final IRCServer ircServer = mIrcServerList.get(serverId);
						if (ircServer.isPutOnSublog(channel)) {
							if (ircServer.isPutOnSublogAll(channel)) {
								toSublog = true;
							}
						} else {
							toSublog = false;
						}
					}
				}
			}
		}
		if (toSublog) {
			// spanOtherChannelLogへ収集
			final int size = mSpanOtherChannelLog.size();
			if (size >= SystemConfig.spanOtherChannelLogLines*2) {
				final ArrayList<SpannableStringBuilder> ssba = new ArrayList<SpannableStringBuilder>(
						mSpanOtherChannelLog.subList(size-SystemConfig.spanOtherChannelLogLines, size));
				mSpanOtherChannelLog = ssba;
			}
			mSpanOtherChannelLog.add(ssbOther);
		}

		for (int i=mListeners.length-1; i>=0; i--) {
			mListeners[i].receiveMessageToChannel(serverName, channel, nick, dateMsg, ssb, ssbOther, toSublog, alert);
		}
	}

	/**
	 * サーバ名リストを得る
	 * @return サーバ名リスト
	 */
	public String[] getServerList() {
		final int numServers = mIrcServerList.size();
		final String[] serverList = new String[numServers];
		for (int i=0; i<numServers; i++) {
			serverList[i] = mIrcServerList.get(i).getServerName();
		}
		return serverList;
	}

	/**
	 * サーバ設定名からサーバIDを得る
	 * @param serverName サーバ設定名
	 * @return サーバID
	 */
	public int getServerId(final String serverName) {
		if (D) Log.d(TAG, "getServerId()");
		if (serverName == null) return -1;
		final Object obj = mIrcServerNameMap.get(serverName.toLowerCase());
		if (obj == null) {
			return -1;
		}
		return ((Integer)obj).intValue();
	}

	/**
	 * サーバIDからサーバ設定名を得る
	 * @param serverId サーバID
	 * @return サーバ設定名
	 */
	public String getServerName(final int serverId) {
		if (D) Log.d(TAG, "getServerName()");
		final int num = mIrcServerList.size();
		if ((0 <= serverId) && (serverId < num)) {
			return new String(mIrcServerList.get(serverId).getServerName());
		}
		return null;
	}

	/**
	 * サーバIDからサーバインスタンスを得る
	 * @param serverId サーバID
	 * @return サーバインスタンス
	 */
	public IRCServer getServer(final int serverId) {
		if (D) Log.d(TAG, "getServer()");
		final int num = mIrcServerList.size();
		if ((0 <= serverId) && (serverId < num)) {
			return mIrcServerList.get(serverId);
		}
		return null;
	}

	/**
	 * サーバ設定名からサーバインスタンスを得る
	 * @param serverName サーバ設定名
	 * @return サーバインスタンス
	 */
	public IRCServer getServer(final String serverName) {
		if (D) Log.d(TAG, "getServer()");
		final int serverId = getServerId(serverName);
		if (0 <= serverId) {
			return mIrcServerList.get(serverId);
		}
		return null;
	}

	/**
	 * 全サーバ設定の再読み込み
	 */
	public void reloadList() {
		if (I) Log.i(TAG, "reloadList()");
		mIrcServerList.clear();
		mIrcServerNameMap.clear();
		final SharedPreferences pref = me.getSharedPreferences("ircserverlist", 0);
		final int num = pref.getInt("numServers", 0);
		for (int i=0; i<num; i++) {
			final IRCServer ircServer = new IRCServer(me, mConnect);
			ircServer.addEventListener(new IRCServerListener());
			ircServer.loadConfig(i);
			mIrcServerList.add(ircServer);
			mIrcServerNameMap.put(ircServer.getServerName().toLowerCase(), i);
		}
	}

	/**
	 * 全サーバ設定の保存
	 */
	/*
	public void saveList() {
		if (I) Log.i(TAG, "saveList()");
		final int num = mIrcServerList.size();
		final SharedPreferences pref = me.getSharedPreferences("ircserverlist", 0);
		final SharedPreferences.Editor editor = pref.edit();
		editor.clear();
		editor.putInt("numServers", num);
		editor.commit();

		for (int serverId=0; serverId<num; serverId++) {
			final IRCServer ircServer = mIrcServerList.get(serverId);
			ircServer.saveConfig(serverId);
		}
	}
	*/

	/**
	 * 全サーバ設定のエクスポート
	 * @return 成功ならtrue
	 */
	public boolean exportIRCServerListConfig() {
		if (I) Log.i(TAG, "exportIRCServerListConfig()");
		final int num = mIrcServerList.size();
		final String filename = SystemConfig.getExternalPath() + SystemConfig.serverConfigExportFileName;
		final MySharedPreferences pref = new MySharedPreferences(me, filename);
		pref.setActivity(ActivitySystemConfig.getActivity());
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_error_saveconfig));
			return false;
		}
		final MySharedPreferences.Editor editor = pref.edit();
		editor.clear();
		editor.putInt("numServers", num);
		ret = editor.commit();
		if (ret < 1) return false;

		for (int serverId=0; serverId<num; serverId++) {
			final IRCServer ircServer = mIrcServerList.get(serverId);
			boolean f = ircServer.exportIRCServerConfig(serverId);
			if (!f) return false;
		}
		return true;
	}

	/**
	 * 全サーバ設定のインポート
	 */
	public void importIRCServerListConfig() {
		if (I) Log.i(TAG, "importIRCServerListConfig()");
		while (mIrcServerList.size() > 0) {
			removeServer(0);
		}
		final String filename = SystemConfig.getExternalPath() + SystemConfig.serverConfigExportFileName;
		final MySharedPreferences pref = new MySharedPreferences(me, filename);
		pref.setActivity(ActivitySystemConfig.getActivity());
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_error_loadconfig));
			return;
		}
		final int num = pref.getInt("numServers", 0);
		for (int serverId=0; serverId<num; serverId++) {
			final IRCServer ircServer = new IRCServer(me, false);
			ircServer.importIRCServerConfig(serverId);
			IRCServerConfig config = new IRCServerConfig(me);
			config.loadConfig(serverId);
			addServer(config);
		}
	}

	/**
	 * ID指定でサーバ設定の再読み込み
	 * @param serverId サーバID
	 */
	public void reloadServerConfig(final int serverId) {
		if (I) Log.i(TAG, "reloadServerConfig(): serverId="+serverId);
		final int num = mIrcServerList.size();
		if ((0 <= serverId) && (serverId < num)) {
			mIrcServerList.get(serverId).close();
			mIrcServerNameMap.remove(mIrcServerList.get(serverId).getServerName().toLowerCase());
			mIrcServerList.get(serverId).loadConfig(serverId);
			mIrcServerNameMap.put(mIrcServerList.get(serverId).getServerName().toLowerCase(), serverId);
			return;
		}
		if (D2) throw new IllegalArgumentException("serverId [" + serverId + "] is not found");
	}

	/**
	 * ID指定でサーバの削除
	 * @param serverId サーバID
	 */
	public void removeServer(final int serverId) {
		if (I) Log.i(TAG, "removeServer(): serverId="+serverId);
		final int num = mIrcServerList.size();
		if ((0 <= serverId) && (serverId < num)) {
			final String serverName = mIrcServerList.get(serverId).getServerName();
			// リストを詰める
			int j = 0;
			for (int i=0; i<num; i++) {
				if (i != serverId) {
					mIrcServerList.get(i).saveConfig(j);
					j++;
				}
			}
			final SharedPreferences pref = me.getSharedPreferences("ircserverlist", 0);
			final SharedPreferences.Editor editor = pref.edit();
			editor.putInt("numServers", num-1);
			editor.commit();

			// 末尾の削除
			IRCServerConfig config = new IRCServerConfig(me);
			config.deleteConfig(num-1);
			config = null;

			mIrcServerList.get(serverId).disconnect();
			mIrcServerList.remove(serverId);
			mIrcServerNameMap.remove(serverName.toLowerCase());

			// カレントサーバの調整
			if ( getServerId(mCurrentServerName) < 0 ) {
				final int newNum = mIrcServerList.size();
				final int newId = Math.min(serverId, newNum-1);
				if (newId < 0) {
					mCurrentServerName = null;
				} else {
					mCurrentServerName = mIrcServerList.get(newId).getServerName();
				}
			}
			return;
		}
		if (D2) throw new IllegalArgumentException("serverId [" + serverId + "] not found");
	}

	/**
	 * サーバの追加
	 * @param config サーバ設定
	 */
	public void addServer(final IRCServerConfig config) {
		if (I) Log.i(TAG, "addServer(): serverName="+config.mServerName);
		if (config.mServerName == null) return;
		if (getServerId(config.mServerName) >= 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + config.mServerName + "] is already exist");
			return;
		}
		int num = mIrcServerList.size();
		config.saveConfig(num);
		final IRCServer ircServer = new IRCServer(me, mConnect);
		ircServer.addEventListener(new IRCServerListener());
		ircServer.loadConfig(num);
		mIrcServerList.add(ircServer);
		mIrcServerNameMap.put(ircServer.getServerName().toLowerCase(), num);
		num++;
		final SharedPreferences pref = me.getSharedPreferences("ircserverlist", 0);
		final SharedPreferences.Editor editor = pref.edit();
		editor.putInt("numServers", num);
		editor.commit();
	}

	/**
	 * サーバへ接続中かどうかのフラグを保存
	 */
	public synchronized void saveServerIsConnectedFlag() {
		if (I) Log.i(TAG, "saveServerIsConnectedFlag()");
		final int num = mIrcServerList.size();

		for (int serverId=0; serverId<num; serverId++) {
			final IRCServer ircServer = mIrcServerList.get(serverId);
			ircServer.saveConnectedFlag(serverId);
		}
	}

	/**
	 * 「サーバへ接続中かどうかのフラグ」に基づいて接続を発行
	 */
	public synchronized void restoreServerIsConnectedFlag() {
		if (I) Log.i(TAG, "restoreServerIsConnectedFlag()");
		final int num = mIrcServerList.size();

		for (int serverId=0; serverId<num; serverId++) {
			final IRCServer ircServer = mIrcServerList.get(serverId);
			if (ircServer.loadConnectedFlag(serverId)) {
				ircServer.connect();
			}
		}
	}

	/**
	 * IRCサーバへの接続発行
	 * @param serverName サーバ設定名
	 */
	public void connectServer(final String serverName) {
		if (I) Log.i(TAG, "connectServer(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.connect();
	}

	/**
	 * IRCサーバへの切断発行
	 * @param serverName サーバ設定名
	 */
	public void disconnectServer(final String serverName) {
		if (I) Log.i(TAG, "disconnectServer(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.disconnect();
	}

	/**
	 * IRCサーバへのクローズ発行
	 * @param serverName サーバ設定名
	 */
	public void closeServer(final String serverName) {
		if (I) Log.i(TAG, "closeServer(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.close();
	}

	/**
	 * 自動接続登録されているIRCサーバへの接続発行
	 * @return 自動接続設定されているサーバ数。-1のときはサーバ設定なし
	 */
	public int connectAuto() {
		if (I) Log.i(TAG, "connectAuto()");
		int nConnect = 0;
		final int num = mIrcServerList.size();
		if (num <= 0) {
			return -1;
		}
		for (int i=0; i<num; i++) {
			if (mIrcServerList.get(i).connectAuto()) {
				nConnect++;
			}
		}
		return nConnect;
	}

	/**
	 * 全IRCサーバへの切断発行
	 */
	public void disconnectAll() {
		if (I) Log.i(TAG, "disconnectAll()");
		final int num = mIrcServerList.size();
		for (int i=0; i<num; i++) {
			mIrcServerList.get(i).disconnect();
		}
	}

	/**
	 * 全IRCサーバへのクローズ発行
	 */
	public void closeAll() {
		if (I) Log.i(TAG, "closeAll()");
		final int num = mIrcServerList.size();
		for (int i=0; i<num; i++) {
			mIrcServerList.get(i).close();
		}
	}

	/**
	 * 接続が切れているかどうか確認
	 * @param serverName サーバ設定名
	 * @return 接続が切れている（＆再接続が必要）ならtrue
	 */
	public boolean isDisconnected(final String serverName) {
		if (D) Log.d(TAG, "isDisconnected(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.isDisconnected();
	}

	/**
	 * 現在登録サーバ数の取得
	 * @return サーバ数
	 */
	public int getNumServers() {
		if (D) Log.d(TAG, "getNumServers()");
		return mIrcServerList.size();
	}

	/**
	 * カレントサーバを得る
	 * @return サーバ設定名
	 */
	public String getCurrentServerName() {
		if (D) Log.d(TAG, "getCurrentServerName()");
		return mCurrentServerName;
	}

	/**
	 * カレントサーバの設定
	 * @param serverName サーバ設定名
	 */
	public void setCurrentServerName(final String serverName) {
		if (D) Log.d(TAG, "setCurrentServerNo(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		mCurrentServerName = new String(serverName);
	}

	/**
	 * チャンネルへコマンドライン送信
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message コマンドライン
	 */
	public void sendCommandLine(final String serverName, final String channel, final String message) {
		if (I) Log.i(TAG, "sendCommandLine(): serverName="+serverName+", channel="+channel+", message="+message);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.sendCommandLine(channel, message);
	}

	/**
	 * チャンネルへメッセージ送信
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void sendMessageToChannel(final String serverName, final String channel, final String message) {
		if (I) Log.i(TAG, "sendMessageToChannel(): serverName="+serverName+", channel="+channel+", message="+message);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.sendChannel(channel, message);
	}

	/**
	 * チャンネルへコマンドライン送信（エコーしない）
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message コマンドライン
	 */
	public void sendQuietCommandLine(final String serverName, final String channel, final String message) {
		if (I) Log.i(TAG, "sendQuietCommandLine(): serverName="+serverName+", channel="+channel+", message="+message);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.sendQuietCommandLine(channel, message);
	}

	/**
	 * チャンネルへメッセージ送信（エコーしない）
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void sendQuietMessageToChannel(final String serverName, final String channel, final String message) {
		if (I) Log.i(TAG, "sendQuietMessageToChannel(): serverName="+serverName+", channel="+channel+", message="+message);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.sendQuietChannel(channel, message);
	}

	/**
	 * チャンネルへNoticeメッセージ送信
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void sendNoticeToChannel(final String serverName, final String channel, final String message) {
		if (I) Log.i(TAG, "sendNoticeToChannel(): serverName="+serverName+", channel="+channel+", message="+message);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.sendChannelNotice(channel, message);
	}

	/**
	 * チャンネルへメッセージエコー
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void echoMessageToChannel(final String serverName, final String channel, final String message) {
		if (I) Log.i(TAG, "echoMessageToChannel(): serverName="+serverName+", channel="+channel+", message="+message);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (serverName.equalsIgnoreCase(IRCMsg.sOtherChannelServerName)) {
				final SpannableStringBuilder ssb = new SpannableStringBuilder(message + "\n");
				final ForegroundColorSpan c2 = new ForegroundColorSpan(SystemConfig.subLogTextColor[SystemConfig.now_colorSet]);
				ssb.setSpan(c2, 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				Linkify.addLinks(ssb, Linkify.WEB_URLS);
				if (isTIGMode(serverName)) {
					final Linkify.MatchFilter hashMatchFilter = new Linkify.MatchFilter() {
						public boolean acceptMatch(final CharSequence s, final int start, final int end) {
							if (start == 0) return true;
							final char c = s.charAt(start-1);
							if (IRCMsg.isUrlChar(c)) return false;
							return true;
						}
					};
					final Linkify.TransformFilter hashTransformFilter = new Linkify.TransformFilter() {
						public final String transformUrl(final Matcher match, final String url) {
							return match.group(1);
						}
					};
					final Pattern pattern = Pattern.compile("#([^ <]+)");
					final String scheme = (SystemConfig.twitterSiteIsMobile ?  "http://m.twitter.com/searches?q=%23" : "http://twitter.com/search?q=%23");
					Linkify.addLinks(ssb, pattern, scheme, hashMatchFilter, hashTransformFilter);
				}
				final String dateMsg = IRCMsg.getDateMsg();
				doReceiveMessageToChannel(
						IRCMsg.sOtherChannelServerName,
						IRCMsg.sOtherChannelName,
						null,
						dateMsg,
						ssb,
						true,
						false,
						false
				);
			} else {
				if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			}
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.echoChannel(channel, message, true);
	}

	/**
	 * otherチャンネルへメッセージエコー
	 * @param message メッセージ
	 */
	public void echoMessageToOther(final String message) {
		echoMessageToChannel(IRCMsg.sOtherChannelServerName, IRCMsg.sOtherChannelName, message);
	}

	/**
	 * 指定サーバ・チャンネルのSpanログ取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return Spanログ
	 */
	public SpannableStringBuilder getSpanChannelLog(final String serverName, final String channel) {
		if (I) Log.i(TAG, "getChannelLog(): serverName="+serverName+", channel="+channel);
		final SpannableStringBuilder msg = new SpannableStringBuilder();
		if (serverName.equals(IRCMsg.sOtherChannelName)){
			for (int i=0; i<mSpanOtherChannelLog.size(); i++) {
				msg.append(mSpanOtherChannelLog.get(i));
			}
			return msg;
		}
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getSpanChannelLog(channel);
	}

	/**
	 * 指定サーバ・チャンネルのログクリア
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	public void clearChannelLog(final String serverName, final String channel) {
		if (I) Log.i(TAG, "clearChannelLog(): serverName="+serverName+", channel="+channel);
		if (serverName.equals(IRCMsg.sOtherChannelName)){
			mSpanOtherChannelLog.clear();
			return;
		}
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.clearChannelLog(channel);
	}

	/**
	 * 指定サーバ・チャンネルのログクリア
	 * @param serverName サーバ設定名
	 */
	public void clearAllChannelLog(final String serverName) {
		if (I) Log.i(TAG, "clearChannelLog(): serverName="+serverName);
		if (serverName.equals(IRCMsg.sOtherChannelName)){
			mSpanOtherChannelLog.clear();
			return;
		}
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.clearAllChannelLog();
	}

	/**
	 * 指定サーバのトピック取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル
	 * @return トピック
	 */
	public String getTopic(final String serverName, final String channel) {
		if (D) Log.d(TAG, "getNick(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getTopic(channel);
	}

	/**
	 * 指定サーバのnick取得
	 * @param serverName サーバ設定名
	 * @return nick
	 */
	public String getNick(final String serverName) {
		if (D) Log.d(TAG, "getNick(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getNick();
	}

	/**
	 * 指定サーバのカレントチャンネル設定
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	public void setCurrentChannel(final String serverName, final String channel) {
		if (I) Log.i(TAG, "setCurrentChannel(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.setCurrentChannel(channel);
	}

	/**
	 * 指定サーバのカレントチャンネルを得る
	 * @param serverName サーバ設定名
	 * @return チャンネル名
	 */
	public String getCurrentChannel(final String serverName) {
		if (D) Log.d(TAG, "getCurrentChannel(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getCurrentChannel();
	}

	/**
	 * 指定サーバのチャンネル名リストを得る
	 * @param serverName サーバ設定名
	 * @return チャンネル名リスト
	 */
	public String[] getChannelList(final String serverName) {
		if (D) Log.d(TAG, "getChannelList(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getChannelNameList();
	}

	/**
	 * 指定サーバのチャンネル更新リストを得る
	 * @param serverName サーバ設定名
	 * @return チャンネル更新リスト
	 */
	public Boolean[] getChannelUpdatedList(final String serverName) {
		if (D) Log.d(TAG, "getChannelUpdatedList(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getChannelUpdatedList();
	}

	/**
	 * 指定サーバのチャンネル更新フラグの取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return チャンネル更新フラグがonならtrue
	 */
	public boolean getChannelUpdated(final String serverName, final String channel) {
		if (D) Log.d(TAG, "getChannelUpdated(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getChannelUpdated(channel);
	}

	/**
	 * チャンネル更新フラグのクリア
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	public void clearChannelUpdated(final String serverName, final String channel) {
		if (I) Log.i(TAG, "clearChannelUpdated(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.clearChannelUpdated(channel);
	}

	/**
	 * 指定サーバのチャンネル更新リストを得る
	 * @param serverName サーバ設定名
	 * @return チャンネル更新リスト
	 */
	public Boolean[] getChannelAlertedList(final String serverName) {
		if (D) Log.d(TAG, "getChannelAlertedList(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getChannelAlertedList();
	}

	/**
	 * 指定サーバのチャンネル通知フラグの取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return チャンネル通知フラグがonならtrue
	 */
	public boolean getChannelAlerted(final String serverName, final String channel) {
		if (D) Log.d(TAG, "getChannelAlerted(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getChannelAlerted(channel);
	}

	/**
	 * チャンネル通知フラグのクリア
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	public void clearChannelAlerted(final String serverName, final String channel) {
		if (I) Log.i(TAG, "clearChannelAlerted(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.clearChannelAlerted(channel);
	}

	/**
	 * 再接続時刻登録
	 * @param serverName サーバ設定名
	 */
	public void setNextReconnectTime(final String serverName) {
		if (D) Log.d(TAG, "setNextReconnectTime(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.setNextReconnectTime();
	}

	//
	/**
	 * 再接続時刻の取得
	 * @param serverName サーバ設定名
	 * @return 再接続時刻
	 */
	public long getNextReconnectTime(final String serverName) {
		if (D) Log.d(TAG, "getNextReconnectTime(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return 0;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getNextReconnectTime();
	}

	/**
	 * KeepAlive送出時刻登録
	 * @param serverName サーバ設定名
	 */
	public void setNextKeepAliveTime(final String serverName) {
		if (D) Log.d(TAG, "setNextKeepAliveTime(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.setNextKeepAliveTime();
	}

	//
	/**
	 * KeepAlive送出時刻の取得
	 * @param serverName サーバ設定名
	 * @return 送出時刻
	 */
	public long getNextKeepAliveTime(final String serverName) {
		if (D) Log.d(TAG, "getNextKeepAliveTime(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return 0;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getNextKeepAliveTime();
	}

	//
	/**
	 * 自動再接続フラグの取得
	 * @param serverName サーバ設定名
	 * @return 自動再接続フラグがonならtrue
	 */
	public boolean getNeedReconnect(final String serverName) {
		if (D) Log.d(TAG, "getNeedReconnect(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getNeedReconnect();
	}

	/**
	 * 指定サーバのチャンネルに入る
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	public void joinChannel(final String serverName, final String channel) {
		if (I) Log.i(TAG, "joinChannel(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.joinChannel(channel);
	}

	/**
	 * 指定サーバの複数チャンネルに順次入る
	 * @param serverName サーバ設定名
	 * @param channels チャンネル名（スペース区切り）
	 */
	public void joinChannels(final String serverName, final String channels) {
		if (I) Log.i(TAG, "joinChannels(): serverName="+serverName+", channels="+channels);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.joinChannels(channels);
	}

	/**
	 * 指定サーバのチャンネルから出る
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	public void partChannel(final String serverName, final String channel) {
		if (I) Log.i(TAG, "partChannel(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		ircServer.partChannel(channel);
	}

	/**
	 * ユーザーリストを得る
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return ユーザーリスト
	 */
	public String[] getUserList(final String serverName, final String channel) {
		if (D) Log.d(TAG, "getUserList(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return null;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.getUserList(channel);
	}

	/**
	 * 指定サーバがTIGモードかどうかを得る
	 * @param serverName サーバ設定名
	 * @return TIGモードならtrue
	 */
	public boolean isTIGMode(final String serverName) {
		if (D) Log.d(TAG, "isTIGMode(): serverName="+serverName);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return ircServer.isTIGMode();
	}

	/**
	 * サブログに出力するかどうかのフラグを取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return フラグがONならtrue
	 */
	public boolean isPutOnSublog(final String serverName, final String channel) {
		if (D) Log.d(TAG, "isPutOnSublog(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return (ircServer.isPutOnSublog(channel));
	}

	/**
	 * サブログに全て出力するかどうかのフラグを取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return フラグがONならtrue
	 */
	public boolean isPutOnSublogAll(final String serverName, final String channel) {
		if (D) Log.d(TAG, "isPutOnSublogAll(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return (ircServer.isPutOnSublogAll(channel));
	}

	/**
	 * アラートをOS通知するかどうかのフラグを取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return フラグがONならtrue
	 */
	public boolean isAlertNotify(final String serverName, final String channel) {
		if (D) Log.d(TAG, "isAlertNotify(): serverName="+serverName+", channel="+channel);
		final int serverId = getServerId(serverName);
		if (serverId < 0) {
			if (D2) throw new IllegalArgumentException("serverName [" + serverName + "] is not exist");
			return false;
		}
		final IRCServer ircServer = mIrcServerList.get(serverId);
		return (ircServer.isAlertNotify(channel));
	}


}
