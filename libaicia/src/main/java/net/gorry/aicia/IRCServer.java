/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;
import org.schwering.irc.lib.JISUtility;
import org.schwering.irc.lib.ssl.SSLDefaultTrustManager;
import org.schwering.irc.lib.ssl.SSLIRCConnection;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;

/**
 *
 * IRCサーバ接続処理
 *
 * @author GORRY
 *
 */
public class IRCServer {
	private static final String TAG = "IRCServer";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;
	private static final boolean D2 = false;

	private IRCServerEventListener[] listeners = new IRCServerEventListener[0];
	private final Context me;

	private IRCConnection mConn;
	private final IRCServerConfig mConfig;
	private final ArrayList<ArrayList<SpannableStringBuilder>> mSpanChannelLog;
	private final ArrayList<String> mChannelName = new ArrayList<String>();
	private final HashMap<String, Integer> mChannelNameMap = new HashMap<String, Integer>();
	private final ArrayList<Boolean> mChannelUpdated = new ArrayList<Boolean>();
	private final ArrayList<Boolean> mChannelAlerted = new ArrayList<Boolean>();
	private final ArrayList<String> mChannelTopic = new ArrayList<String>();
	private final ArrayList<ArrayList<String>> mChannelUserList = new ArrayList<ArrayList<String>>();
	private final ArrayList<IRCChannelConfig> mChannelConfig = new ArrayList<IRCChannelConfig>();
	private String mNowNick = null;
	private String mCurrentChannel;
	private long mNextKeepAliveTime = 0;
	private long mNextReconnectTime = 0;
	private boolean mNeedReconnect = false;
	private final boolean mConnect;
	private String[] mAlertKeywords = null;
	private boolean mUseJisHalfKana = false;
	private boolean mConvertJisHalfKana = false;
	private int mConfigId = 0;
	private boolean mThrowGetUrlCommand = false;
	
	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 * @param connect IRCサーバに接続する実体として使用するときはtrue
	 */
	IRCServer(final Context context, final boolean connect) {
		if (I) Log.i(TAG, "IRCServer()");
		me = context;
		mConnect = connect;
		if (connect) {
			setTimerHandler();
		}
		mConfig = new IRCServerConfig(me);
		createServer();
		mSpanChannelLog = new ArrayList<ArrayList<SpannableStringBuilder>>();
		createChannel(IRCMsg.sSystemChannelName);
	}

	/**
	 * リスナ登録処理
	 * @param l リスナ
	 */
	public synchronized void addEventListener(final IRCServerEventListener l) {
		if (I) Log.i(TAG, "addEventListener()");
		if (l == null) {
			throw new IllegalArgumentException("Listener is null.");
		}
		final int len = listeners.length;
		final IRCServerEventListener[] oldListeners = listeners;
		listeners = new IRCServerEventListener[len + 1];
		System.arraycopy(oldListeners, 0, listeners, 0, len);
		listeners[len] = l;
	}

	/**
	 * リスナ削除処理
	 * @param l リスナ
	 * @return true 正常に削除された
	 */
	public synchronized boolean removeEventListener(final IRCServerEventListener l) {
		if (I) Log.i(TAG, "removeEventListener()");
		if (l == null) {
			return false;
		}
		int index = -1;
		for (int i=0; i<listeners.length; i++) {
			if (listeners[i].equals(l)) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return false;
		}
		listeners[index] = null;
		final int len = listeners.length - 1;
		final IRCServerEventListener[] newListeners = new IRCServerEventListener[len];
		for (int i = 0, j = 0; i < len; j++) {
			if (listeners[j] != null) {
				newListeners[i++] = listeners[j];
			}
		}
		listeners = newListeners;
		return true;
	}

	/**
	 * サーバ名取得
	 * @return サーバ名
	 */
	public String getServerName() {
		if (I) Log.i(TAG, "getServerName()");
		return mConfig.mServerName;
	}

	/**
	 * サーバ設定のフェッチ
	 * @return サーバ設定
	 */
	public IRCServerConfig fetchConfig() {
		if (I) Log.i(TAG, "fetchConfig()");
		return mConfig;
	}

	/**
	 * サーバ設定の読み込み
	 * @param configId サーバ設定ID
	 */
	public void loadConfig(final int configId) {
		if (D) Log.d(TAG, "loadConfig(): configId="+configId);
		disconnect();
		mConfig.loadConfig(configId);
		mNowNick = mConfig.mNick;
		mAlertKeywords = null;
		if ((mConfig.mAlertKeywords != null) && (mConfig.mAlertKeywords.length() > 0) ) {
			mAlertKeywords = mConfig.mAlertKeywords.split("[ ]+");
			if ( mAlertKeywords[0].length() == 0 ) {
				mAlertKeywords = null;
			} else {
				for (int i=0; i<mAlertKeywords.length; i++) {
					mAlertKeywords[i] = mAlertKeywords[i].toUpperCase();
				}
			}
		}
		mUseJisHalfKana = mConfig.mEncoding.equalsIgnoreCase("ISO-2022-JP_with_halfkana");
		mConvertJisHalfKana = mConfig.mEncoding.equalsIgnoreCase("ISO-2022-JP");
		mConfigId = configId;
	}

	/**
	 * サーバ設定の保存
	 * @param configId サーバ設定ID
	 */
	public void saveConfig(final int configId) {
		if (D) Log.d(TAG, "saveConfig()");
		mConfig.saveConfig(configId);
	}

	/**
	 * サーバ設定の削除
	 * @param configId サーバ設定ID
	 */
	public void deleteConfig(final int configId) {
		if (D) Log.d(TAG, "deleteConfig(): configId="+configId);
		disconnect();
		mConfig.deleteConfig(configId);
	}

	/**
	 * サーバ設定のエクスポート
	 * @param configId サーバ設定ID
	 * @return 成功ならtrue
	 */
	public boolean exportIRCServerConfig(final int configId) {
		if (D) Log.d(TAG, "exportIRCServerConfig(): configId="+configId);
		boolean f = false;
		mConfig.loadConfig(configId);
		f = mConfig.exportConfig(configId);
		if (!f) return false;

		for (int i=0; i<mConfig.mChannelNames.size(); i++) {
			final String channel = mConfig.mChannelNames.get(i);
			final IRCChannelConfig config = new IRCChannelConfig(me);
			config.loadConfig(mConfigId, channel);
			f = config.exportConfig(mConfigId, channel);
			if (!f) return false;
		}
		return true;
	}

	/**
	 * サーバ設定のインポート
	 * @param configId サーバ設定ID
	 */
	public void importIRCServerConfig(final int configId) {
		if (D) Log.d(TAG, "importIRCServerConfig(): configId="+configId);
		mConfig.importConfig(configId);
		mConfig.saveConfig(configId);

		for (int i=0; i<mConfig.mChannelNames.size(); i++) {
			final String channel = mConfig.mChannelNames.get(i);
			final IRCChannelConfig config = new IRCChannelConfig(me);
			config.importConfig(mConfigId, channel);
			config.saveConfig(mConfigId, channel);
		}
	}

	/**
	 * サーバへ接続中かどうかのフラグを保存
	 * @param configId サーバ設定ID
	 * @return 成功ならtrue
	 */
	public boolean saveConnectedFlag(final int configId) {
		if (D) Log.d(TAG, "saveConnected(): configId="+configId);
		mConfig.setConnectedFlag(configId, mNeedReconnect);
		return true;
	}

	/**
	 * サーバへ接続中かどうかのフラグを読み出し
	 * @param configId サーバ設定ID
	 * @return サーバへ接続中ならtrue
	 */
	public boolean loadConnectedFlag(final int configId) {
		if (D) Log.d(TAG, "loadConnected(): configId="+configId);
		return mConfig.isConnectedFlag(configId);
	}

	/**
	 * IRClibのクローズ
	 */
	public void close() {
		if (D) Log.d(TAG, "close(): server="+mConfig.mServerName);
		if (!mConnect) return;
		disconnect();
		if (mConn != null) {
			mConn.close();
			mConn = null;
		}
		mNowNick = null;
	}

	/**
	 * IRCサーバへの切断発行
	 */
	public void disconnect() {
		if (D) Log.d(TAG, "disconnect(): server="+mConfig.mServerName);
		if (!mConnect) return;
		if (mConfig.mHost == null) return;
		mNeedReconnect = false;
		clearNextKeepAliveTime();
		clearNextReconnectTime();
		stopTimer();
		new Thread(new Runnable() { public void run() {
			boolean disconnected = false;
			if (D) Log.d(TAG, "disconnect(): server="+mConfig.mServerName+": start");
			receiveSystemChannel("Disconnecting to " + mConfig.mHost + ":" + mConfig.mPort + "...", true);
			for (int i=0; i<3; i++) {
				if (!isConnected()) {
					disconnected = true;
					break;
				}
				if (D) Log.d(TAG, "disconnect(): server="+mConfig.mServerName+": doQuit("+i+")");
				mConn.doQuit();
				for (int j=0; j<50; j++) {
					if (!isConnected()) {
						disconnected = true;
						break;
					}
					if (D) Log.d(TAG, "disconnect(): server="+mConfig.mServerName+": sleep for retry");
					try {
						Thread.sleep(100);
					} catch (final InterruptedException e) {
						//
					}
				}
			}
			if (!disconnected) {
				if (D) Log.d(TAG, "disconnect(): server="+mConfig.mServerName+": failed disconnect: force disconnect");
			}
			mNowNick = null;
		}}).start();
		try {
			Thread.sleep(500);
		} catch (final InterruptedException e) {
			//
		}
	}

	/**
	 * IRCサーバへの接続発行
	 */
	public void connect() {
		if (D) Log.d(TAG, "connect(): server="+mConfig.mServerName);
		if (!mConnect) return;
		mNeedReconnect = true;
		if (mConn != null) {
			mConn.close();
			mConn = null;
		}
		if (mNowNick == null) {
			mNowNick = new String(mConfig.mNick);
		}
		if (mConfig.mUseSsl) {
			final SSLIRCConnection sslconn = new SSLIRCConnection(
					mConfig.mHost,
					mConfig.mPort,
					mConfig.mPort,
					mConfig.mPass,
					mNowNick,
					mConfig.mUsername,
					mConfig.mRealname
			);
			SSLIRCConnection.protocol = "TLS";
			sslconn.addTrustManager(new TrustManager());
			mConn = sslconn;

		} else {
			mConn = new IRCConnection(
					mConfig.mHost,
					mConfig.mPort,
					mConfig.mPort,
					mConfig.mPass,
					mNowNick,
					mConfig.mUsername,
					mConfig.mRealname
			);
		}
		mConn.addIRCEventListener(new IRClibListener());
		mConn.setEncoding(mConfig.mEncoding);
		mConn.setPong(false);
		//mConn.setDaemon(true);
		mConn.setColors(false);
		mConn.setTimeout(0);
		startTimer();
		receiveSystemChannel("Connecting to " + mConfig.mHost + ":" + mConfig.mPort + "...", true);
		new Thread(new Runnable() { public void run() {
			try {
					mConn.connect();
			} catch (final IOException ex) {
				receiveSystemChannel("Connection error: " + ex.getClass().getName() + ": " + ex.getMessage(), true);
				for (int i=listeners.length-1; i>=0; i--) {
					listeners[i].receiveDisconnect(mConfig.mServerName);
				}
				clearNextKeepAliveTime();
				if (getNeedReconnect()) {
					setNextReconnectTime();
				}
			}
		}}).start();

		try {
			Thread.sleep(500);
		} catch (final InterruptedException e) {
			//
		}
	}

	/**
	 * SSLトラストマネージャ
	 */
	public class TrustManager extends SSLDefaultTrustManager {
		/**
		 * @param chain X509Certificate
		 * @return true
		 */
		public boolean isServerTrusted(final X509Certificate chain[]) {
			return true;
		}
	}

	/**
	 * 自動接続がONならIRCサーバへの接続発行
	 * @return 自動接続がONならtrue
	 */
	public boolean connectAuto() {
		if (D) Log.d(TAG, "connectAuto(): server="+mConfig.mServerName);
		if (mConfig.mAutoConnect) {
			if (!isConnected()) {
				connect();
			}
			return true;
		}
		return false;
	}

	/**
	 * サーバオブジェクトの作成
	 */
	public void createServer() {
		if (D) Log.d(TAG, "createServer(): server="+mConfig.mServerName);
		final String dateMsg = IRCMsg.getDateMsg();
		for (int i=listeners.length-1; i>=0; i--) {
			listeners[i].createServer(mConfig.mServerName, dateMsg);
		}
	}

	/**
	 * サーバオブジェクトの削除
	 */
	public void removeServer() {
		if (D) Log.d(TAG, "removeServer(): server="+mConfig.mServerName);
		final String dateMsg = IRCMsg.getDateMsg();
		for (int i=listeners.length-1; i>=0; i--) {
			listeners[i].removeServer(mConfig.mServerName, dateMsg);
		}
	}

	/**
	 * チャンネル名からチャンネルIDを得る
	 * @param channel チャンネル名
	 * @return チャンネルID
	 */
	public synchronized int getChannelId(final String channel) {
		if (I) Log.i(TAG, "getChannelId()");
		if (channel == null) return -1;
		final Object obj = mChannelNameMap.get(channel.toLowerCase());
		if (obj == null) {
			return -1;
		}
		return ((Integer)obj).intValue();
	}

	/**
	 * チャンネルオブジェクトの作成
	 * @param channel チャンネル名
	 */
	public synchronized void createChannel(final String channel) {
		if (D) Log.d(TAG, "createChannel(): server="+mConfig.mServerName+", channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			final String dateMsg = IRCMsg.getDateMsg();
			final int num = mChannelName.size();
			mSpanChannelLog.add(new ArrayList<SpannableStringBuilder>());
			mChannelName.add(channel);
			mChannelUpdated.add(false);
			mChannelAlerted.add(false);
			mChannelTopic.add("");
			mChannelNameMap.put(channel.toLowerCase(), num);
			mChannelUserList.add(new ArrayList<String>());
			final IRCChannelConfig config = new IRCChannelConfig(me);
			config.loadConfig(mConfigId, channel);
			mChannelConfig.add(config);
			final int channelId = mChannelName.size()-1;
			mChannelUserList.get(channelId).add(mNowNick);

			for (int i=listeners.length-1; i>=0; i--) {
				listeners[i].createChannel(mConfig.mServerName, channel, dateMsg);
			}

			// Talkのときは相手をユーザーリストに入れる
			if (channelNameIsUser(channel)) {
				echoChannel(channel, "Start talking with " + channel, true);
				if (!mChannelUserList.get(channelId).contains(channel)) {
					mChannelUserList.get(channelId).add(channel);
				}
			} else {
				echoChannel(channel, "Channel " + IRCMsg.channelName(channel) + " has created", true);
			}
		}
	}

	/**
	 * チャンネルオブジェクトの削除
	 * @param channel チャンネル名
	 */
	public synchronized void removeChannel(final String channel) {
		if (D) Log.d(TAG, "removeChannel(): server="+mConfig.mServerName+", channel="+channel);
		final String dateMsg = IRCMsg.getDateMsg();
		final int id = getChannelId(channel);
		if (id >= 0) {
			for (int i=listeners.length-1; i>=0; i--) {
				listeners[i].removeChannel(mConfig.mServerName, channel, dateMsg);
			}
			mSpanChannelLog.remove(id);
			mChannelName.remove(id);
			mChannelUpdated.remove(id);
			mChannelAlerted.remove(id);
			mChannelTopic.remove(id);
			mChannelNameMap.remove(channel.toLowerCase());
			mChannelUserList.remove(id);
			mChannelConfig.remove(id);

			// カレントチャンネルの調整
			if ( getChannelId(mCurrentChannel) < 0 ) {
				final int newNum = mChannelName.size();
				final int newId = Math.min(id, newNum-1);
				if (newId < 0) {
					mCurrentChannel = null;
				} else {
					mCurrentChannel = mChannelName.get(newId);
				}
			}
		}
	}

	/**
	 * チャンネルの設定読み込み
	 * @param channel チャンネル名
	 */
	public synchronized void loadChannelConfig(final String channel) {
		if (D) Log.d(TAG, "loadChannelConfig(): server="+mConfig.mServerName+", channel="+channel);
		final int id = getChannelId(channel);
		if (id >= 0) {
			mChannelConfig.get(id).loadConfig(mConfigId, channel);
		}
	}

	/**
	 * チャンネルの設定保存
	 * @param channel チャンネル名
	 */
	/*
	public synchronized void saveChannelConfig(final String channel) {
		if (D) Log.d(TAG, "saveChannelConfig(): server="+mConfig.mServerName+", channel="+channel);
		final int id = getChannelId(channel);
		if (id >= 0) {
			mChannelConfig.get(id).saveConfig(mConfigId, channel);
		}
	}
	*/

	/**
	 * チャンネルの設定インポート
	 * @param channel チャンネル名
	 */
	/*
	public synchronized void importChannelConfig(final String channel) {
		if (D) Log.d(TAG, "importChannelConfig(): server="+mConfig.mServerName+", channel="+channel);
		final int id = getChannelId(channel);
		if (id >= 0) {
			mChannelConfig.get(id).importConfig(mConfigId, channel);
		}
	}
	*/

	/**
	 * チャンネルの設定エクスポート
	 * @param channel チャンネル名
	 * @return 成功ならtrue
	 */
	/*
	public synchronized boolean exportChannelConfig(final String channel) {
		if (D) Log.d(TAG, "exportChannelConfig(): server="+mConfig.mServerName+", channel="+channel);
		boolean f = false;
		final int id = getChannelId(channel);
		if (id >= 0) {
			f = mChannelConfig.get(id).exportConfig(mConfigId, channel);
		}
		return f;
	}
	*/

	/**
	 * nickの変更
	 * @param oldNick 旧nick
	 * @param newNick 新nick
	 */
	public void changeNick(final String oldNick, final String newNick) {
		if (D) Log.d(TAG, "changeNick(): server="+mConfig.mServerName+", newNick="+newNick);
		if (oldNick.equalsIgnoreCase(mNowNick)) {
			mNowNick = newNick;
		}
		for( int id=0; id<mChannelName.size(); id++) {
			final ArrayList<String> userList = mChannelUserList.get(id);
			final int uid = userList.indexOf(oldNick);
			if (uid >= 0) {
				userList.remove(uid);
			}
			if (!userList.contains(newNick)) {
				userList.add(newNick);
			}
		}
		final String dateMsg = IRCMsg.getDateMsg();
		for (int i=listeners.length-1; i>=0; i--) {
			listeners[i].changeNick(mConfig.mServerName, oldNick, newNick, dateMsg);
		}
	}

	/**
	 * メッセージの受信センター
	 * @param channel チャンネル名
	 * @param nick Nick
	 * @param message メッセージ
	 * @param isConn エコーメッセージのときtrue
	 * @param update ログを更新するときtrue
	 * @param pale ログを淡色化するときtrue
	 * @param emphasis ログを強調するときtrue
	 * @param isCopy ログがコピーメッセージのときtrue
	 */
	public void receiveMessageToChannel(final String channel, final String nick, final String message, final boolean isConn, final boolean update, final boolean pale, final boolean emphasis, final boolean isCopy ) {
		String channel2 = channel;
		if (isCopy) {
			channel2 = IRCMsg.sSystemChannelName;
		}
		if (I) Log.i(TAG, "receiveMessageToChannel()");
		if (I) Log.i(TAG, "channel=[" + channel2 + "]");
		if (I) Log.i(TAG, "message=[" + message + "]");
		int id = getChannelId(channel2);
		if (id < 0) {
			createChannel(channel2);
			id = getChannelId(channel2);
			if (id < 0) {
				if (D2) throw new IllegalArgumentException("channel [" + channel2 + "] is not exist");
				return;
			}
		}

		// TIGモードで自分がtwitter URLのみを投じたらそのURLをwebで開くことにする
		if ((nick != null) && nick.equalsIgnoreCase(mNowNick)) {
			if (mThrowGetUrlCommand) {
				if (mConfig.mForTIG) {
					String line = message;
					if (line.startsWith("http://twitter.com")) {
						String url = line.replaceFirst("[ \t].*$", "");
						if (url.equals(line)) {
							openWebPage(url);
							return;
						}
					}
				}
				if (!message.startsWith("u ") && !message.startsWith("h ")) {
					mThrowGetUrlCommand = false;
				}
			}
		}

		final String dateMsg = IRCMsg.getDateMsg();

		// キーワード確認
		boolean alert = false;
		int alertIndex = 0;
		int alertIndexEnd = 0;
		if (!pale) {
			if (mChannelConfig.get(id).mUseAlert) {
				if (mAlertKeywords != null) {
					final String capMsg = message.toUpperCase();
					for (int i=0; i<mAlertKeywords.length; i++) {
						alertIndex = capMsg.indexOf(mAlertKeywords[i]);
						if (alertIndex >= 0) {
							alert = true;
							alertIndexEnd = alertIndex + mAlertKeywords[i].length();
							break;
						}
					}
				}
				if (mChannelConfig.get(id).mUseAlertAll) {
					alert = true;
					if (alertIndex < 0) {
						alertIndexEnd = alertIndex = 0;
					}
				}
			}
		}

		if (alert && !isCopy && !channel2.equalsIgnoreCase(IRCMsg.sSystemChannelName)) {
			receiveMessageToChannel(channel, nick, message, isConn, update, pale, emphasis, true);
		}

		// Spanログを作成する
		final SpannableStringBuilder ssb = new SpannableStringBuilder(message);
		if (pale) {
			final ForegroundColorSpan c = new ForegroundColorSpan(SystemConfig.mainLogPaleTextColor[SystemConfig.now_colorSet]);
			ssb.setSpan(c, 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (emphasis) {
			final ForegroundColorSpan c = new ForegroundColorSpan(SystemConfig.mainLogEmphasisTextColor[SystemConfig.now_colorSet]);
			ssb.setSpan(c, 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else {
			final ForegroundColorSpan c = new ForegroundColorSpan(SystemConfig.mainLogTextColor[SystemConfig.now_colorSet]);
			ssb.setSpan(c, 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		// リンクの作成
		Linkify.addLinks(ssb, Linkify.WEB_URLS);
		if (mConfig.mForTIG) {
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
			final Pattern pattern = Pattern.compile("#([A-Za-z0-9_]+)");
			final String scheme = (SystemConfig.twitterSiteIsMobile ?  "http://mobile.twitter.com/searches?q=%23" : "http://twitter.com/search?q=%23");
			Linkify.addLinks(ssb, pattern, scheme, hashMatchFilter, hashTransformFilter);
		}

		if (alert && SystemConfig.highlightOnAlert) {
			// final ForegroundColorSpan c = new ForegroundColorSpan(SystemConfig.alertKeywordsColor[SystemConfig.now_colorSet]);
			// ssb.setSpan(c, alertIndex, alertIndexEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			final BackgroundColorSpan c = new BackgroundColorSpan(SystemConfig.alertLineColor[SystemConfig.now_colorSet]);
			ssb.setSpan(c, 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			if (alertIndex != alertIndexEnd) {
				final ForegroundColorSpan c2 = new ForegroundColorSpan(SystemConfig.alertKeywordsColor2[SystemConfig.now_colorSet]);
				ssb.setSpan(c2, alertIndex, alertIndexEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		// spannableChannelLogへ収集
		ArrayList<SpannableStringBuilder> ssba = mSpanChannelLog.get(id);
		final int size = ssba.size();
		if ( size >= SystemConfig.spanChannelLogLines*2 ) {
			final ArrayList<SpannableStringBuilder> ssba2 = new ArrayList<SpannableStringBuilder>(
					ssba.subList(size-SystemConfig.spanChannelLogLines, size));
			ssba = ssba2;
			mSpanChannelLog.set(id, ssba);
		}

		final SpannableStringBuilder ssbLine = IRCMsg.colorDateMsg(dateMsg, SystemConfig.mainLogDateColor[SystemConfig.now_colorSet]);
		if (isCopy && SystemConfig.copyToSystemChannelOnAlert) {
			String chid = "";
			final int channelId = getChannelId(channel);
			if ((channelId >= 1) && (channelId <= 26)) {
				final String idChannel = "abcdefghijklmnopqrstuvwxyz";
				chid = idChannel.substring(channelId-1, channelId) + ":";
			}
			final String msg = "<" + chid + channel + "> ";
			final SpannableStringBuilder ssbChannel = new SpannableStringBuilder(msg);
			final ForegroundColorSpan c = new ForegroundColorSpan(SystemConfig.mainLogTextColor[SystemConfig.now_colorSet]);
			ssbChannel.setSpan(c, 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssbLine.append(ssbChannel);
		}
		if ((nick != null) && (nick.length() > 0)) {
			final SpannableStringBuilder ssbNick = IRCMsg.colorNick(nick, SystemConfig.mainLogTextColor[SystemConfig.now_colorSet]);
			ssbLine.append(ssbNick);
		}
		ssbLine.append(ssb);
		ssbLine.append("\n");
		ssba.add(ssbLine);

		// トークのときは常にalert
		if (!pale) {
			if (channelNameIsUser(channel2)) {
				alert = true;
			}
		}

		if (!isCopy) {
			if (update) {
				mChannelUpdated.set(id, true);
			}
			if (alert) {
				mChannelAlerted.set(id, true);
			}

			boolean forceSublog = emphasis;
			boolean forbidSublog = false;
			if (pale) {
				forbidSublog = !((mConfig.mPutPaleTextOnSublog) && (mChannelConfig.get(id).mPutPaleTextOnSublog));
			}
			for (int i=listeners.length-1; i>=0; i--) {
				listeners[i].receiveMessageToChannel(mConfig.mServerName, channel2, nick, dateMsg, ssb, forceSublog, forbidSublog, alert);
			}
		}
	}

	/**
	 * チャンネルへのメッセージ受信
	 * @param channel チャンネル名
	 * @param nick ニックネーム
	 * @param message メッセージ
	 * @param update ログを更新するときtrue
	 * @param pale ログを淡色化するときtrue
	 */
	public void receiveChannel(final String channel, final String nick, final String message, final boolean update, final boolean pale) {
		if (I) Log.i(TAG, "receiveChannel()");
		receiveMessageToChannel(
				channel,
				nick,
				message,
				false,
				update,
				pale,
				false,
				false
		);
	}

	/**
	 * システムチャンネルへのメッセージ受信
	 * @param message メッセージ
	 * @param pale ログを淡色化するときtrue
	 */
	public void receiveSystemChannel(final String message, final boolean pale) {
		if (I) Log.i(TAG, "receiveSystemChannel()");
		receiveMessageToChannel(
				IRCMsg.sSystemChannelName,
				null,
				message,
				false,
				false,
				pale,
				false,
				false
		);
	}

	/**
	 * Connect/Disconnectメッセージ受信
	 * @param message メッセージ
	 */
	public void receiveConnect(final String message) {
		if (I) Log.i(TAG, "receiveConnect()");
		receiveMessageToChannel(
				IRCMsg.sSystemChannelName,
				null,
				message,
				false,
				false,
				false,
				true,
				false
		);
	}

	/**
	 * チャンネルへのメッセージエコー
	 * @param channel チャンネル名
	 * @param message メッセージ
	 * @param pale ログを淡色化するときtrue
	 */
	public void echoChannel(final String channel, final String message, final boolean pale) {
		if (I) Log.i(TAG, "echoChannel()");
		receiveMessageToChannel(
				channel,
				mNowNick,
				message,
				true,
				false,
				pale,
				false,
				false
		);
	}

	/**
	 * システムチャンネルへのメッセージエコー
	 * @param message メッセージ
	 */
	public void echoSystemChannel(final String message) {
		if (I) Log.i(TAG, "echoSystemChannel()");
		receiveMessageToChannel(
				IRCMsg.sSystemChannelName,
				null,
				message,
				true,
				false,
				true,
				false,
				false
		);
	}

	/**
	 * 指定チャンネルへのコマンドライン処理
	 * @param channel チャンネル名
	 * @param message コマンドライン
	 * @param quiet エコーしないならtrue
	 */
	public void sendCommandLineCore(final String channel, final String message, final boolean quiet) {
		if (I) Log.i(TAG, "sendCommandLine()");
		if (I) Log.i(TAG, "channel=[" + channel + "]");
		if (I) Log.i(TAG, "message=[" + message + "]");
		if (!quiet) echoChannel(channel, message, true);
		final String words[] = message.split("[ ]+");
		final String cmd = words[0].toUpperCase();
		final String arg = new String(message).replaceAll("^[^ ]+[ ]+(.*)[ ]*$", "$1");
		if (cmd.equals("/DEBUG")) {
			doDebug(channel, arg);
			return;
		}
		if (cmd.equals("/TOPIC")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doTopic(channel, arg);
				} else {
					mConn.doTopic(channel);
				}
			}}).start();
			return;
		}
		if (cmd.equals("/NICK")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doNick(words[1]);
				}
			}}).start();
			return;
		}
		if (cmd.equals("/JOIN")) {
			if (words.length >= 2) {
				joinChannel(words[1]);
			}
			return;
		}
		if (cmd.equals("/INVITE")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doInvite(words[1], channel);
				}
			}}).start();
			return;
		}
		if (cmd.equals("/PART")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doPart(channel, arg);
				} else {
					mConn.doPart(channel);
				}
			}}).start();
			return;
		}
		if (cmd.equals("/MODE")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doMode(channel, arg);
				} else {
					mConn.doMode(channel);
				}
			}}).start();
			return;
		}
		if (cmd.equals("/KICK")) {
			final String arg2 = new String(message).replaceAll("^[^ ]+[ ]+[^ ]+[ ]+[^ ]+(.*)[ ]*$", "$1");
			new Thread(new Runnable() { public void run() {
				if (words.length >= 4) {
					mConn.doKick(channel, words[2], arg2);
				} else if (words.length >= 3) {
					mConn.doKick(channel, words[2], "kicked");
				}
			}}).start();
			return;
		}
		if (cmd.equals("/WHOIS")) {
			if (words.length >= 2) {
				mConn.doWhois(words[1]);
			}
			return;
		}
		if (cmd.equals("/WHOWAS")) {
			if (words.length >= 2) {
				mConn.doWhowas(words[1]);
			}
			return;
		}
		if (cmd.equals("/USERHOST")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doWhowas(words[1]);
				}
			}}).start();
			return;
		}
		if (cmd.equals("/OPER")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 3) {
					mConn.doOper(words[1], words[2]);
				}
			}}).start();
			return;
		}
		if (cmd.equals("/LIST")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doList(words[1]);
				} else {
					mConn.doList();
				}
			}}).start();
			return;
		}
		if (cmd.equals("/NAMES")) {
			new Thread(new Runnable() { public void run() {
				if (words.length >= 2) {
					mConn.doList(words[1]);
				} else {
					mConn.doList();
				}
			}}).start();
			return;
		}
		if (cmd.equals("/RAW")) {
			if (words.length >= 2) {
				sendSystemChannel(arg);
			}
			return;
		}
		final String rawMsg = new String(message).replaceFirst("^[/]", "");
		sendSystemChannel(rawMsg);
	}

	/**
	 * 指定チャンネルへのコマンドライン処理
	 * @param channel チャンネル名
	 * @param message コマンドライン
	 */
	public void sendCommandLine(final String channel, final String message) {
		sendCommandLineCore(channel, message, false);
	}
	
	/**
	 * 指定チャンネルへのコマンドライン処理
	 * @param channel チャンネル名
	 * @param message コマンドライン
	 */
	public void sendQuietCommandLine(final String channel, final String message) {
		sendCommandLineCore(channel, message, true);
	}
	
	/**
	 * 指定チャンネルへのメッセージ送信
	 * @param channel チャンネル名
	 * @param message メッセージ
	 * @param quiet エコーしないならtrue
	 */
	public void sendChannelCore(final String channel, final String message, final boolean quiet) {
		if (I) Log.i(TAG, "sendChannel()");
		if (I) Log.i(TAG, "channel=[" + channel + "]");
		if (I) Log.i(TAG, "message=[" + message + "]");
		new Thread(new Runnable() { public void run() {
			mThrowGetUrlCommand = false;
			if (message.startsWith("u ") || message.startsWith("h ")) {
				mThrowGetUrlCommand = true;
			}
			if (mConvertJisHalfKana || (mUseJisHalfKana && !SystemConfig.allowSendHalfKana)) {
				final String convMsg = JISUtility.hankakuKatakanaToZenkakuKatakana(message);
				mConn.doPrivmsg(channel, convMsg);
				if (!quiet) echoChannel(channel, convMsg, false);
			} else {
				mConn.doPrivmsg(channel, message);
				if (!quiet) echoChannel(channel, message, false);
			}
		}}).start();
	}

	/**
	 * 指定チャンネルへのメッセージ送信
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void sendChannel(final String channel, final String message) {
		sendChannelCore(channel, message, false);
	}

	/**
	 * 指定チャンネルへのメッセージ送信（エコーしない）
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void sendQuietChannel(final String channel, final String message) {
		sendChannelCore(channel, message, true);
	}

	/**
	 * 指定チャンネルへのNoriceメッセージ送信
	 * @param channel チャンネル名
	 * @param message メッセージ
	 * @param quiet エコーしないならtrue
	 */
	public void sendChannelNoticeCore(final String channel, final String message, final boolean quiet) {
		if (I) Log.i(TAG, "sendChannelNotice()");
		if (I) Log.i(TAG, "channel=[" + channel + "]");
		if (I) Log.i(TAG, "message=[" + message + "]");
		new Thread(new Runnable() { public void run() {
			if (mConvertJisHalfKana || (mUseJisHalfKana && !SystemConfig.allowSendHalfKana)) {
				final String convMsg = JISUtility.hankakuKatakanaToZenkakuKatakana(message);
				mConn.doNotice(channel, convMsg);
				if (!quiet) echoChannel(channel, convMsg, false);
			} else {
				mConn.doNotice(channel, message);
				if (!quiet) echoChannel(channel, message, false);
			}
		}}).start();
	}

	/**
	 * 指定チャンネルへのNoticeメッセージ送信
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void sendChannelNotice(final String channel, final String message) {
		sendChannelNoticeCore(channel, message, false);
	}
	
	/**
	 * 指定チャンネルへのNoriceメッセージ送信（エコーしない）
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	public void sendQuietChannelNotice(final String channel, final String message) {
		sendChannelNoticeCore(channel, message, true);
	}
	
	/**
	 * システムチャンネルへのメッセージ送信
	 * @param message メッセージ
	 */
	public void sendSystemChannel(final String message) {
		if (I) Log.i(TAG, "sendSystemChannel()");
		if (I) Log.i(TAG, "message=[" + message + "]");
		new Thread(new Runnable() { public void run() {
				mConn.send(message);
		}}).start();
		// echoChannel(IRCMsg.sSystemChannelName, message);
	}

	/**
	 * チャンネル数を得る（システムチャンネル含む）
	 * @return チャンネル数
	 */
	public int getNumChannels() {
		return mChannelName.size();
	}

	/**
	 * チャンネルIDからチャンネル名を得る
	 * @param id チャンネルID
	 * @return チャンネル名
	 */
	public String getChannelName(final int id) {
		if (I) Log.i(TAG, "getChannelName()");
		final int num=mChannelName.size();
		if (id < num) {
			return mChannelName.get(id);
		}
		return null;
	}

	/**
	 * チャンネル名リストを得る
	 * @return チャンネルリスト
	 */
	public String[] getChannelNameList() {
		if (I) Log.i(TAG, "getChannelList()");
		return mChannelName.toArray(new String[0]);
	}

	/**
	 * チャンネルログ更新リストを得る
	 * 配列順はgetChannelList()で得られるチャンネル名リストと同一
	 * @return チャンネルログ更新リスト
	 */
	public Boolean[] getChannelUpdatedList() {
		if (I) Log.i(TAG, "getChannelUpdated()");
		return mChannelUpdated.toArray(new Boolean[0]);
	}

	/**
	 * チャンネルログ通知リストを得る
	 * 配列順はgetChannelList()で得られるチャンネル名リストと同一
	 * @return チャンネルログ通知リスト
	 */
	public Boolean[] getChannelAlertedList() {
		if (I) Log.i(TAG, "getChannelAlerted()");
		return mChannelAlerted.toArray(new Boolean[0]);
	}

	/**
	 * IDでSpannableチャンネルログを得る
	 * @param id チャンネルID
	 * @return Spannableチャンネルログ
	 */
	public SpannableStringBuilder getSpanChannelLog(final int id) {
		if (I) Log.i(TAG, "getChannelLog(" + id + ")");
		final int num=mChannelName.size();
		if (id < num) {
			final SpannableStringBuilder msg = new SpannableStringBuilder();
			final ArrayList<SpannableStringBuilder> ssba = mSpanChannelLog.get(id);
			for (int i=0; i<ssba.size(); i++) {
				msg.append(ssba.get(i));
			}
			return msg;

		}
		return null;
	}

	/**
	 * チャンネル名でSpannableチャンネルログを得る
	 * @param channel チャンネル名
	 * @return Spannableチャンネルログ
	 */
	public SpannableStringBuilder getSpanChannelLog(final String channel) {
		if (D) Log.d(TAG, "getChannelLog(): channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return null;
		}
		return getSpanChannelLog(id);
	}

	/**
	 * チャンネルIDでチャンネルログのクリア
	 * @param id チャンネルID
	 */
	public void clearChannelLog(final int id) {
		if (I) Log.i(TAG, "clearChannelLog(): id="+id);
		final int num=mChannelName.size();
		if (id < num) {
			// mChannelLog.get(id).setLength(0);
			mSpanChannelLog.get(id).clear();
		}
	}

	/**
	 * 全チャンネルログのクリア
	 */
	public void clearAllChannelLog() {
		if (D) Log.d(TAG, "clearAllChannelLog()");
		final int num=mChannelName.size();
		for (int i=0; i<num; i++) {
			// mChannelLog.get(i).setLength(0);
			mSpanChannelLog.get(i).clear();
		}
	}

	/**
	 * チャンネル名でチャンネルログのクリア
	 * @param channel チャンネル名
	 */
	public void clearChannelLog(final String channel) {
		if (D) Log.d(TAG, "clearChannelLog(): channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return;
		}
		// mChannelLog.get(id).setLength(0);
		mSpanChannelLog.get(id).clear();
	}

	/**
	 * チャンネルIDでチャンネルログ更新フラグの取得
	 * @param id チャンネルID
	 * @return チャンネルログ更新フラグ
	 */
	public boolean getChannelUpdated(final int id) {
		if (I) Log.i(TAG, "clearChannelUpdated(): id="+id);
		final int num=mChannelUpdated.size();
		if (id < num) {
			return mChannelUpdated.get(id);
		}
		return false;
	}

	/**
	 * チャンネル名でチャンネルログ更新フラグの取得
	 * @param channel チャンネル名
	 * @return チャンネルログ更新フラグ
	 */
	public boolean getChannelUpdated(final String channel) {
		if (I) Log.i(TAG, "clearChannelUpdated(): channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return false;
		}
		return mChannelUpdated.get(id);
	}

	/**
	 * チャンネルIDでチャンネルログ更新フラグのクリア
	 * @param id チャンネルID
	 */
	public void clearChannelUpdated(final int id) {
		if (I) Log.i(TAG, "clearChannelUpdated(): id="+id);
		final int num=mChannelUpdated.size();
		if (id < num) {
			mChannelUpdated.set(id, false);
		}
	}

	/**
	 * チャンネル名でチャンネルログ更新フラグのクリア
	 * @param channel チャンネル名
	 */
	public void clearChannelUpdated(final String channel) {
		if (I) Log.i(TAG, "clearChannelUpdated(): channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return;
		}
		mChannelUpdated.set(id, false);
	}

	/**
	 * チャンネルIDでチャンネルログ通知フラグの取得
	 * @param id チャンネルID
	 * @return チャンネルログ通知フラグ
	 */
	public boolean getChannelAlerted(final int id) {
		if (I) Log.i(TAG, "clearChannelAlerted(): id="+id);
		final int num=mChannelAlerted.size();
		if (id < num) {
			return mChannelAlerted.get(id);
		}
		return false;
	}

	/**
	 * チャンネル名でチャンネルログ通知フラグの取得
	 * @param channel チャンネル名
	 * @return チャンネルログ通知フラグ
	 */
	public boolean getChannelAlerted(final String channel) {
		if (I) Log.i(TAG, "clearChannelAlerted(): channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return false;
		}
		return mChannelAlerted.get(id);
	}

	/**
	 * チャンネルIDでチャンネルログ通知フラグのクリア
	 * @param id チャンネルID
	 */
	public void clearChannelAlerted(final int id) {
		if (I) Log.i(TAG, "clearChannelAlerted(): id="+id);
		final int num=mChannelAlerted.size();
		if (id < num) {
			mChannelAlerted.set(id, false);
		}
	}

	/**
	 * チャンネル名でチャンネルログ通知フラグのクリア
	 * @param channel チャンネル名
	 */
	public void clearChannelAlerted(final String channel) {
		if (I) Log.i(TAG, "clearChannelAlerted(): channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return;
		}
		mChannelAlerted.set(id, false);
	}

	/**
	 * カレントチャンネルを得る
	 * @return チャンネル名
	 */
	public String getCurrentChannel() {
		if (I) Log.i(TAG, "getCurrentChannel()");
		return mCurrentChannel;
	}

	/**
	 * カレントチャンネルの設定
	 * @param channel チャンネル名
	 */
	public void setCurrentChannel(final String channel) {
		if (D) Log.d(TAG, "setCurrentChannel(): channel="+channel);
		mCurrentChannel = new String(channel);
	}

	/**
	 * チャンネルに入る
	 * @param channel チャンネル名
	 */
	public void joinChannel(final String channel) {
		if (D) Log.d(TAG, "joinChannel(): channel="+channel);
		if (channel == null) return;
		if (channel.length() == 0) return;
		new Thread(new Runnable() { public void run() {
			if (channel.contains(",")) {
				final String ch = new String(channel).replaceAll("^([^,]+).*$", "$1");
				final String key = new String(channel).replaceAll("^[^,]+,(.*)$", "$1");
				mConn.doJoin(ch, key);
			} else {
				if (channelNameIsUser(channel)) {
					createChannel(channel);
				} else {
					mConn.doJoin(channel);
				}
			}
		}}).start();
	}

	/**
	 * 複数チャンネルに順次入る
	 * @param channels チャンネル名（スペース区切り）
	 */
	public void joinChannels(final String channels) {
		if (D) Log.d(TAG, "joinChannels(): channels="+channels);

//		new Thread(new Runnable() { public void run() {
			final String[] channelList = channels.split("[ \t\n]+");
			for ( int i=0; i<channelList.length; i++ ) {
				joinChannel(channelList[i]);
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					//
				}
			}
//		}}).start();
	}

	/**
	 * チャンネルから出る
	 * @param channel チャンネル名
	 */
	public void partChannel(final String channel) {
		if (D) Log.d(TAG, "joinChannel(): channel="+channel);
		new Thread(new Runnable() { public void run() {
			mConn.doPart(channel);
		}}).start();
	}

	/**
	 * トピックを得る
	 * @param channel チャンネル名
	 * @return トピック
	 */
	public String getTopic(final String channel) {
		if (I) Log.i(TAG, "getTopic(): channel="+channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return null;
		}
		return mChannelTopic.get(id).toString();
	}

	/**
	 * トピックを変更する
	 * @param channel チャンネル名
	 * @param topic トピック
	 */
	public void changeTopic(final String channel, final String topic) {
		if (D) Log.d(TAG, "setTopic(): channel="+channel+", topic="+topic);
		createChannel(channel);
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return;
		}
		mChannelTopic.set(id, topic);
		final String dateMsg = IRCMsg.getDateMsg();
		for (int i=listeners.length-1; i>=0; i--) {
			listeners[i].changeTopic(mConfig.mServerName, channel, topic, dateMsg);
		}
	}

	/**
	 * nickを得る
	 * @return nick
	 */
	public String getNick() {
		if (I) Log.i(TAG, "getNick()");
		if (mConn == null) return "";
		return mConn.getNick();
	}

	/**
	 * nickの設定
	 * @param nick nick
	 */
	public void setNick(final String nick) {
		if (D) Log.d(TAG, "setNick(): nick="+nick);
		if (mConn == null) return;
		new Thread(new Runnable() { public void run() {
			mConn.doNick(nick);
		}}).start();
	}

	/**
	 * 接続中かどうかを返す
	 * @return 接続中ならtrue
	 */
	public boolean isConnected() {
		if (I) Log.i(TAG, "isConnected()");
		return (mConn != null) && mConn.isConnected();
	}

	/**
	 * 再接続時刻読み出し
	 * @return 再接続時刻
	 */
	public long getNextReconnectTime() {
		if (I) Log.i(TAG, "getNextReconnectTime()");
		return mNextReconnectTime;
	}

	/**
	 * 再接続時刻登録
	 */
	public void setNextReconnectTime() {
		if (I) Log.i(TAG, "setNextReconnectTime()");
		final int t1 = SystemConfig.autoReconnectWaitSec;
		if (t1 > 0) {
			final long count = getTimerCount() + t1;
			mNextReconnectTime = count;
		}
	}

	/**
	 * 再接続時刻クリア
	 */
	public void clearNextReconnectTime() {
		if (I) Log.i(TAG, "clearNextReconnectTime()");
		mNextReconnectTime = 0;
	}

	/**
	 * KeepAlive送出時刻読み出し
	 * @return 送出時刻
	 */
	public long getNextKeepAliveTime() {
		if (I) Log.i(TAG, "getNextKeepAliveTime()");
		return mNextKeepAliveTime;
	}

	/**
	 * KeepAlive送出時刻登録
	 */
	public void setNextKeepAliveTime() {
		if (I) Log.i(TAG, "setNextKeepAliveTime()");
		if (isConnected()) {
			final int t1 = SystemConfig.checkConnectWaitSec;
			if (t1 > 0) {
				final long count = getTimerCount() + t1;
				mNextKeepAliveTime = count;
			}
		}
	}

	/**
	 * KeepAlive送出停止
	 */
	public void clearNextKeepAliveTime() {
		if (I) Log.i(TAG, "clearNextKeepAliveTime()");
		mNextKeepAliveTime = 0;
	}

	/**
	 * KeepAliveデータを送出する
	 */
	public void sendKeepAliveMessage() {
		if (I) Log.i(TAG, "sendKeepAliveMessage()");
		if (mNowNick != null) {
			if (isConnected()) {
				if (I) Log.i(TAG, "send keepalive message");
				if (SystemConfig.verbose >= 2) echoSystemChannel("V2: send keepalive message");
				new Thread(new Runnable() { public void run() {
					mConn.doPong(mNowNick);
				}}).start();
			}
		}
	}

	/**
	 * 自動再接続フラグ読み出し
	 * @return 自動再接続フラグがonならtrue
	 */
	public boolean getNeedReconnect() {
		if (I) Log.i(TAG, "getNeedReconnect()");
		if (!mNeedReconnect) {
			if (I) Log.i(TAG, "not need reconnect");
			if (SystemConfig.verbose >= 2) echoSystemChannel("V2: not need reconnect");
			return false;
		}
		return mConfig.mAutoReconnect;
	}

	/**
	 * 接続が切れているかどうか調べる
	 * @return 接続が切れているならtrue
	 */
	public boolean isDisconnected() {
		if (I) Log.i(TAG, "isDisconnected()");
		if (!mNeedReconnect) {
			if (I) Log.i(TAG, "not need reconnect");
			if (SystemConfig.verbose >= 2) echoSystemChannel("V2: not need reconnect");
			return false;
		}
		return !isConnected();
	}

	/**
	 * ユーザーリストを得る
	 * @param channel チャンネル名
	 * @return ユーザーリスト
	 */
	public String[] getUserList(final String channel) {
		if (I) Log.i(TAG, "getUserList()");
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return null;
		}
		final ArrayList<String> userList = mChannelUserList.get(id);
		final String[] users = userList.toArray(new String[0]);
		java.util.Arrays.sort(users);
		return users;
	}

	/**
	 * サーバ設定がTIGモードかどうか調べる
	 * @return サーバ設定がTIGモードならtrue
	 */
	public boolean isTIGMode() {
		if (I) Log.i(TAG, "isTIGMode()");
		return mConfig.mForTIG;
	}

	/**
	 * 指定チャンネルをサブログに出すかどうか調べる
	 * @param channel チャンネル名
	 * @return サブログに出すならtrue
	 */
	public boolean isPutOnSublog(final String channel) {
		if (I) Log.i(TAG, "isPutOnSublog()");
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return false;
		}
		return mChannelConfig.get(id).mPutOnSublog;
	}

	/**
	 * 指定チャンネルの全発言をサブログに出すかどうか調べる
	 * @param channel チャンネル名
	 * @return サブログに出すならtrue
	 */
	public boolean isPutOnSublogAll(final String channel) {
		if (I) Log.i(TAG, "isPutOnSublogAll()");
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return false;
		}
		return mChannelConfig.get(id).mPutOnSublogAll;
	}

	/**
	 * 指定チャンネルのアラートをOS通知するかどうか調べる
	 * @param channel チャンネル名
	 * @return OS通知するならtrue
	 */
	public boolean isAlertNotify(final String channel) {
		if (I) Log.i(TAG, "isAlertNotify()");
		final int id = getChannelId(channel);
		if (id < 0) {
			if (D2) throw new IllegalArgumentException("channel [" + channel + "] is not exist");
			return false;
		}
		return mConfig.mAlertNotify && mChannelConfig.get(id).mAlertNotify;
	}

	/**
	 * IRClibリスナ
	 */
	private class IRClibListener implements IRCEventListener{

		public void onRegistered() {
			receiveConnect("Connected " + mConfig.mServerName);
			for (int i=listeners.length-1; i>=0; i--) {
				listeners[i].receiveConnect(mConfig.mServerName);
			}
			joinChannels(mConfig.mConnectingChannel);
			setNextKeepAliveTime();
			clearNextReconnectTime();
		}

		public void onDisconnected() {
			receiveConnect("Disconnected " + mConfig.mServerName);
			for (int i=listeners.length-1; i>=0; i--) {
				listeners[i].receiveDisconnect(mConfig.mServerName);
			}
			clearNextKeepAliveTime();
			if (getNeedReconnect()) {
				setNextReconnectTime();
			}
		}

		public void onError(final String message) {
			receiveSystemChannel("IRC Error: " + message, true);
			setNextKeepAliveTime();
		}

		public void onError(final int num, final String message) {
			if (num > 0) {
				doNumericReply(num, "", message);
			} else {
				receiveSystemChannel("IRC Error: " + num + " " + message, true);
			}
			setNextKeepAliveTime();
		}

		public void onInvite(final String channel, final IRCUser user, final String invitedNick) {
			joinChannels(channel);
			receiveChannel(channel, null, user.getNick() + " has invited " + invitedNick + " to " + channel, false, true);
			setNextKeepAliveTime();
		}

		public void onJoin(final String channel, final IRCUser user) {
			final int channelId = getChannelId(channel);
			if (channelId >= 0) {
				if (!mChannelUserList.get(channelId).contains(user.getNick())) {
					mChannelUserList.get(channelId).add(user.getNick());
				}
			}
			receiveChannel(channel, null, user.getNick() + " has joined " + channel, false, true);
			setNextKeepAliveTime();
		}

		public void onKick(final String channel, final IRCUser user, final String kickedNick, final String message) {
			receiveChannel(channel, null, user.getNick() + " has kicked " + kickedNick, false, true);
			final int channelId = getChannelId(channel);
			if (channelId >= 0) {
				if (mChannelUserList.get(channelId).contains(user.getNick())) {
					mChannelUserList.get(channelId).remove(user.getNick());
				}
			}
			setNextKeepAliveTime();
		}

		public void onMode(final String channel, final IRCUser user, final IRCModeParser modeParser) {
			receiveChannel(channel, null, user.getNick() + " has set mode " + modeParser.getLine(), false, true);
			setNextKeepAliveTime();
		}

		public void onMode(final IRCUser user, final String targetNick, final String mode) {
			receiveSystemChannel(user.getNick() + " has changed the mode of " + targetNick + " to " + mode, true);
			setNextKeepAliveTime();
		}

		public void onNick(final IRCUser user, final String newNick) {
			receiveSystemChannel(user.getNick() + " is now known as " + newNick, true);
			changeNick(user.getNick(), newNick);
			setNextKeepAliveTime();
		}

		public void onNotice(final String target, final IRCUser user, final String message) {
			if (user.getNick() == null) {
				receiveSystemChannel(IRCMsg.channelNickMessage(target, user.getNick(), message), true);
				// } else if (user.getHost() == null) {
				// 	receiveSystemChannel(IRCMsg.channelNickMessage(target, user.getNick(), message), true);
			} else {
				if (channelNameIsUser(target)) {
					receiveChannel(user.getNick(), user.getNick(), message, true, true);
				} else {
					receiveChannel(target, user.getNick(), message, true, true);
				}
			}
			setNextKeepAliveTime();
		}

		public void onPart(final String channel, final IRCUser user, final String message) {
			receiveChannel(channel, null, user.getNick() + " has left channel (" + message + ")", true, true);
			final int channelId = getChannelId(channel);
			if (channelId >= 0) {
				if (mChannelUserList.get(channelId).contains(user.getNick())) {
					mChannelUserList.get(channelId).remove(user.getNick());
				}
			}
			setNextKeepAliveTime();
		}

		public void onPrivmsg(final String channel, final IRCUser user, final String message, final String line) {
			if (user.getNick() == null) {
				receiveSystemChannel(IRCMsg.channelNickMessage(channel, user.getNick(), message), true);
				// } else if (user.getHost() == null) {
				// 	receiveSystemChannel(IRCMsg.channelNickMessage(channel, user.getNick(), message), true);
			} else {
				if (line.contains("\u0001")) {
					receiveSystemChannel("CTCP-query("+message+") from "+user.getNick(), true);
				} else if (channelNameIsUser(channel)) {
					receiveChannel(user.getNick(), user.getNick(), message, true, false);
				} else {
					receiveChannel(channel, user.getNick(), message, true, false);
				}
			}
			setNextKeepAliveTime();
		}

		public void onQuit(final IRCUser user, final String message) {
			receiveSystemChannel(user.getNick() + " has left IRC (" + message + ")", true);
			setNextKeepAliveTime();
		}

		public void onReply(final int num, final String value, final String message) {
			if (num > 0) {
				doNumericReply(num, value, message);
			} else {
				receiveSystemChannel("[" + Integer.toString(num) + "] [" + value + "] ["+ message + "]", true);
			}
			setNextKeepAliveTime();
		}

		public void onTopic(final String channel, final IRCUser user, final String topic) {
			changeTopic(channel, topic);
			receiveChannel(channel, "", user.getNick() + " has changed the topic to " + topic, false, true);
			setNextKeepAliveTime();
		}

		public void onPing(final String ping) {
			if (SystemConfig.verbose >= 2) {
				receiveSystemChannel("V2: Ping(): doPong " + ping, true );
			}
			new Thread(new Runnable() { public void run() {
				mConn.doPong(ping);
			}}).start();
			setNextKeepAliveTime();
		}

		public void unknown(final String prefix, final String command, final String middle, final String trailing) {
			receiveSystemChannel(command + " " + middle + " :" + trailing, true);
			// receiveSystemChannel("unknown(): prefix=[" + prefix + "] command=[" + command + "] middle=[" + middle + "] trailing=[" + trailing + "]");
			setNextKeepAliveTime();
		}

	}

	/**
	 * ニューメリックリプライの処理
	 * @param num ニューメリックリプライ番号
	 * @param value ?
	 * @param message メッセージ
	 */
	public void doNumericReply(final int num, final String value, final String message) {
		// boolean nodisp = false;
		if (SystemConfig.verbose >= 1) {
			// if ((num < 400) || (num > 499)) {
				receiveSystemChannel("V1:[" + Integer.toString(num) + "] [" + value + "] ["+ message + "]", true);
			// }
		}

		switch (num) {
		case 332:  // RPL_TOPIC
		{
			final String[] words = value.split("[ ]+");
			if (words.length >= 2) {
				changeTopic(words[1], message);
				receiveChannel(words[1], "", "Topic: " + message, false, true);
			} else {
				receiveSystemChannel("[" + Integer.toString(num) + "] [" + value + "] ["+ message + "]", true);
			}
			break;
		}

		case 333:  // RPL_TOPICWHOTIME
		{
			final String[] words = value.split("[ ]");
			if (words.length >= 2) {
				final long d = Long.valueOf(message);
				final Date date = new Date(d*1000);
				final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
				final String s = sdf.format(date);
				String nick = "(somebody)";
				if (words.length >= 3) {
					nick = words[2];
				}
				receiveChannel(words[1], "", nick + " set this topic at " + s, false, true);
			} else {
				receiveSystemChannel("[" + Integer.toString(num) + "] [" + value + "] ["+ message + "]", true);
			}
			break;
		}

		case 353:  // RPL_NAMREPLY
		{
			final String[] words = value.split("[ ]+");
			final String channel;
			if (words.length < 3) break;
			channel = words[2];
			final int channelId = getChannelId(channel);
			if (channelId < 0) break;
			final String[] users = message.split("[ ]+");
			for (int i=0; i<users.length; i++) {
				final String user = users[i].replaceAll("^[@+]", "");
				if (!mChannelUserList.get(channelId).contains(user)) {
					mChannelUserList.get(channelId).add(user);
				}
			}
			// receiveSystemChannel("[" + Integer.toString(num) + "] [" + value + "] ["+ message + "]");
			break;
		}
		case 366:  // RPL_ENDOFNAMES
			break;

		case 372:  // RPL_MOTD
		case 375:  // RPL_MOTDSTART
			// nodisp = true;
			receiveSystemChannel(message, true);
			break;
		case 376:  // RPL_ENDOFMOTD
		case 422:  // ERR_NOMOTD
			break;

		case   1:  //
		case   2:  //
		case   3:  //
		case   4:  //
		case   5:  //
			receiveSystemChannel(message, true);
			break;

		case 433:  // RPL_NICKNAMEISUSE
		{
			// receiveSystemChannel("[" + Integer.toString(num) + "] [" + value + "] ["+ message + "]");
			if (mNowNick == null) break;
			// NICK衝突回避
			final int len = mNowNick.length();
			final char[] nickChr = mNowNick.toCharArray();
			int i;
			for (i=len; i>0; --i) {
				if ((nickChr[i-1] < '0') || (nickChr[i-1] > '9')) break;
			}
			int suffix = 0;
			for (int j=i; j<len; j++) {
				suffix = suffix*10 + (nickChr[j]-'0');
			}
			final String newSuffix = Integer.toString(suffix+1);
			int nickRestLen = len-newSuffix.length();
			if (nickRestLen < 0) nickRestLen = 0;
			final String newNick = mNowNick.substring(0, nickRestLen) + newSuffix;
			echoSystemChannel("newNick is [" + newNick + "]");
			changeNick(mConn.getNick(), newNick);
			break;
		}

		case 311:  // RPL_WHOISUSER
		case 314:  // RPL_WHOWASUSER
		{
			final String[] words = value.split("[ ]+");
			if (words.length >= 2) {
				echoSystemChannel("Nick: [" + words[1] + "]");
			}
			if (words.length >= 3) {
				echoSystemChannel("User Name: [" + words[2] + "]");
			}
			if (words.length >= 4) {
				echoSystemChannel("Host: [" + words[3] + "]");
			}
			echoSystemChannel("Real Name: [" + message + "]");
			break;
		}

		case 319:  // RPL_WHOISCHANNELS
		{
			final String[] words = message.split("[ ]+");
			for (int i=0; i<words.length; i++) {
				echoSystemChannel("Channel: [" + words[i] + "]");
			}
			break;
		}

		case 312:  // RPL_WHOISSERVER
		{
			final String[] words = value.split("[ ]");
			if (words.length >= 3) {
				echoSystemChannel("Server: [" + words[2] + "]");
			}
			echoSystemChannel("Server Information: [" + message + "]");
			break;
		}

		case 322:  // RPL_LIST
		{
			final String[] words = value.split("[ ]");
			if (words.length >= 3) {
				echoSystemChannel(words[1] + " (" + words[2] + ") : [" + message + "]");
			} else if (words.length >= 2) {
				echoSystemChannel(words[1] + ": [" + message + "]");
			} else {
				echoSystemChannel("[" + value + "] [" + message + "]");
			}
			break;
		}

		case 324:  // RPL_CHANNELMODEIS
		{
			final String[] words = value.split("[ ]");
			if (words.length >= 2) {
				receiveChannel(words[1], "", "mode " + message, false, true);
			} else {
				echoSystemChannel("[" + value + "] [" + message + "]");
			}
			break;
		}

		case 302:  // RPL_USERHOST
		case 265:  // RPL_LOCALUSERS
		case 266:  // RPL_GLOBALUSERS
		{
			echoSystemChannel("[" + message + "]");
			break;
		}

		case 251:  // RPL_LUSERCLIENT
		case 313:  // RPL_WHOISOPERATOR
		case 317:  // RPL_WHOISIDLE
		case 318:  // RPL_ENDOFWHOIS
		case 252:  //
		case 253:  //
		case 254:  // RPL_LUSERCHANNELS
		case 255:  // RPL_LUSERME
		case 352:  // WHOREPLY
		case 315:  // ENDOFWHO
		case 321:  // RPL_LISTSTART
		case 323:  // RPL_LISTEND
		case 369:  // RPL_ENDOFWHOWAS
		case 349:  // RPL_ENDOFEXCEPTLIST
			break;

		default:
			if ((num >= 300) && (num <= 499)) {
				receiveSystemChannel("[" + Integer.toString(num) + "] [" + value + "] ["+ message + "]", true);
			}
			break;
		}
	}

	/**
	 * ランダム文字列を生成する。
	 * @param length 文字列長
	 * @return 文字列
	 */
	public String randomMessage(final int length) {
		final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		final StringBuffer str = new StringBuffer(length+1);
		for (int i=0; i<length; i++) {
			final int idx = (int)(Math.random()*chars.length());
			str.append(chars.substring(idx, idx+1));
		}
		return str.toString();
	}

	/**
	 * デバッグコマンド処理
	 * @param channel チャンネル
	 * @param line デバッグコマンドライン
	 */
	public void doDebug(final String channel, final String line) {
		if (I) Log.i(TAG, "doDebug()");
		final String words[] = line.split(" ");
		final String cmd = words[0].toUpperCase();
		final String arg = new String(line).replaceAll("^[^ ]+[ ]+(.*)[ ]*", "$1");
		if (cmd.equals("MSG")) {
			receiveChannel(channel, mNowNick, arg, true, true);
			return;
		}
		if (cmd.equals("REPLY")) {
			final int numericReply = Integer.valueOf(words[1]);
			doNumericReply(numericReply, mNowNick, arg);
			return;
		}
		if (cmd.equals("RNDMSG")) {
			int count = 100;
			if (words.length >= 2) {
				count = Integer.valueOf(words[1]);
			}
			for (int i=0; i<count; i++) {
				final String message = randomMessage(64);
				receiveChannel(channel, mNowNick, message, true, true);
			}
			return;
		}
		if (cmd.equals("RNDCHMSG")) {
			int count = 100;
			if (words.length >= 2) {
				count = Integer.valueOf(words[1]);
			}
			for (int i=0; i<count; i++) {
				final String message = randomMessage(64);
				receiveChannel(mChannelName.get((int)(Math.random()*mChannelName.size())), mNowNick, message, true, true);
			}
			return;
		}
	}

	/**
	 * タイマー処理
	 */
	private Object mTimerSync = new Object();
	private boolean mTimerEnable = false;
	private static final long mTimerStep = 10000;
	private long mTimerCount = 0;
	private long mTimerNext = 0;
	private static final int TIMER = 1;
	private Timer mTimer;
	private MyTimerTask mTimerTask;

	private void setTimerHandler() {
		if (I) Log.i(TAG, "setTimerHandler()");
		if (mTimer == null) {
			mTimer = new Timer(true);
			mTimerTask = new MyTimerTask();
		}
	}

	class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			if (I) Log.i(TAG, "TimerTask()");
			while (SystemClock.uptimeMillis() > mTimerNext) {
				mTimerNext += mTimerStep;
				mTimerCount+= mTimerStep/1000;
				onTimer(mTimerCount);
			}
			synchronized(mTimerSync) {
				if (mTimerEnable) {
					mTimer.cancel();
					mTimer.purge();
					mTimer = null;
				}
				if (mTimer == null) {
					mTimer = new Timer(true);
					mTimerTask = new MyTimerTask();
				}
				if (I) Log.i(TAG, "TimerTask(): next="+mTimerStep);
				mTimer.schedule(mTimerTask, mTimerStep);
				mTimerEnable = true;
			}
		}
	};
	
	/**
	 * @return タイマー稼働中ならtrue
	 */
	public boolean isTimerEnabled() {
		synchronized(mTimerSync) {
			return mTimerEnable; 
		}
	}
	private void startTimer() {
		synchronized(mTimerSync) {
			if (mTimerEnable) {
				mTimer.cancel();
				mTimer.purge();
				mTimer = null;
			}
			if (mTimer == null) {
				mTimer = new Timer(true);
				mTimerTask = new MyTimerTask();
			}
			mTimerNext = SystemClock.uptimeMillis();
			if (I) Log.i(TAG, "startTimer(): next="+mTimerStep);
			mTimer.schedule(mTimerTask, mTimerStep);
			mTimerEnable = true;
		}
	}
	private void stopTimer() {
		if (I) Log.i(TAG, "stopTimer()");
		synchronized(mTimerSync) {
			if (mTimerEnable) {
				mTimer.cancel();
				mTimer.purge();
				mTimer = null;
			}
			mTimerEnable = false;
		}
	}
	private long getTimerCount() {
		return mTimerCount;
	}
	private void onTimer(final long timerCount) {
		// KeepAlive送出＆切断検出
		final long t1 = getNextKeepAliveTime();
		if ((t1 > 0) && (t1 < timerCount)) {
			if (SystemConfig.checkConnectWaitSec > 0) {
				if (I) Log.i(TAG, "ontimer(): try keepalive");
				if (SystemConfig.verbose >= 2) echoChannel(IRCMsg.sSystemChannelName, "V2: Post KeepAlive packet", true);
				setNextKeepAliveTime();
				sendKeepAliveMessage();
				if (isDisconnected()) {
					clearNextKeepAliveTime();
					if (I) Log.i(TAG, "ontimer(): lost connection");
					echoChannel(IRCMsg.sSystemChannelName, "Lost connection", true);
					disconnect();
					connect();
				}
			} else {
				clearNextKeepAliveTime();
			}
		}

		// 自動再接続
		final long t2 = getNextReconnectTime();
		if ((t2 > 0) && (t2 < timerCount)) {
			if (I) Log.i(TAG, "ontimer(): try reconnect");
			echoChannel(IRCMsg.sSystemChannelName, "Try reconnect", true);
			clearNextReconnectTime();
			connect();
		}
	}

	private boolean channelNameIsUser(final String channel) {
		if (channel == null) return false;
		if (channel.length() == 0) return false;
		final char c = channel.toCharArray()[0];
		if ((c != '*') && (c != '%') && (c != '&') && (c != '#') && (c != '!') && (c != '+') && (c != ' ')) {
			return true;
		}
		return false;
	}

	/**
	 * webページを開く
	 * @param url URL
	 */
	private void openWebPage(final String url) {
		if (I) Log.i(TAG, "openWebPage(): url="+url);
		String url2 = url;
		if (SystemConfig.twitterSiteIsMobile) {
			url2 = url.replaceFirst("[:][/][/]twitter[.]com", "://mobile.twitter.com");
		}
		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url2));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		me.startActivity(intent);
	}

}

