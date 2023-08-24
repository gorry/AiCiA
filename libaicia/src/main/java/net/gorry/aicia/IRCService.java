/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.SpannableStringBuilder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import net.gorry.libaicia.BuildConfig;
import net.gorry.libaicia.R;

/**
 *
 * IRCサービス
 *
 * @author GORRY
 *
 */
public class IRCService extends ForegroundService {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "IRCService";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"()";
	}

	/** IRCServiceのアクション名 */
	public static final String ACTION = "IRCService";

	/** IRCServiceの機能番号 */
	public static final int RECEIVE_MESSAGE = 1;
	/** */
	public static final int CREATE_SERVER = 2;
	/** */
	public static final int REMOVE_SERVER = 3;
	/** */
	public static final int CREATE_CHANNEL = 4;
	/** */
	public static final int REMOVE_CHANNEL = 5;
	/** */
	public static final int CHANGE_NICK = 6;
	/** */
	public static final int CHANGE_TOPIC = 7;
	/** */
	public static final int PING_PONG = 8;

	private static final int NOTIFY_SYSTEM = 101;
	private static final int NOTIFY_LOWMEMORY = 1;
	private static final int NOTIFY_SERVER_MESSAGE = 10000;
	private static final int NOTIFY_ALERT = 20000;

	private static final String CHANNEL_AICIA="aicia_ch_1";

	private IRCServerList ircServerList = null;
	private Context me = null;

	private NotificationManager mNotificationManager;
	private String mInputBox = "";
	private int mInputBoxSelStart = 0;
	private int mInputBoxSelEnd = 0;
	private boolean mDonate = false;
	private boolean mPrintDonate = false;
	private final Handler mHandler = new Handler();
	private boolean mSetAlarm = false;
	private boolean mSetReceiverHome = false;
	private HomeReceiver mHomeReceiver = new HomeReceiver();

	private SoundPool mSoundPool = null;
	private int mSound = 0;
	private int mSoundId = 0;
	private float mSoundVolume = 0.0F;

	private static final int mInputHistorySize = 20;
	private ArrayList<String> mInputHistory = new ArrayList<String>();

	/**
	 * コンストラクタ
	 */
	public IRCService() {
		super(NOTIFY_SYSTEM);
		me = this;
	}

	/*
	 * 作成
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		if (T) Log.v(TAG, M()+": start");
		mNotificationManager = super.getNotificationManager();

		SystemConfig.setContext(me);
		SystemConfig.loadConfig();

		/*
		if (ircServerList == null) {
			ircServerList = new IRCServerList(me, true);
			ircServerList.addEventListener(new IRCServerListListener());
			ircServerList.reloadList();
			// new Thread(new Runnable() {	public void run() {
				ircServerList.restoreServerIsConnectedFlag();
			// }}).start();
		}
		 */

		if (!mSetReceiverHome) {
			IntentFilter iFilter = new IntentFilter();
			iFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
			registerReceiver(mHomeReceiver, iFilter);
			mSetReceiverHome = true;
		}

		loadRingSound();

		if (T) Log.v(TAG, M()+": end");
	}

	/*
	 * 開始
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (T) Log.v(TAG, M()+": start: intent=["+intent+"], flags="+flags+", startId="+startId);
		if (intent != null) {
			final Bundle extras = intent.getExtras();
			if (extras != null) {
				mDonate = (extras.getBoolean("donate"));
			}
		}

		setupNotification();

		SystemConfig.setContext(me);
		SystemConfig.loadConfig();

		if (ircServerList == null) {
			ircServerList = new IRCServerList(me, true);
			ircServerList.addEventListener(new IRCServerListListener());
			ircServerList.reloadList();
			ircServerList.restoreServerIsConnectedFlag();
		}
		
		return START_REDELIVER_INTENT;
	}

	private void sendPingToMain() {
		if (T) Log.v(TAG, M()+": start");
		
		// ActivityMainにKeepalive Pingを送る
		final Intent intent2 = new Intent(ACTION);
		intent2.putExtra("msg", IRCService.PING_PONG);
		sendBroadcast(intent2);
		if (T) Log.v(TAG, M()+": end");
	}

	/**
	 * HOMEボタン押下を受け取るレシーバ
	 */
	public class HomeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (T) Log.v(TAG, M()+": start: receive HomeReceiver");
			sendPingToMain();
			if (T) Log.v(TAG, M()+": end");
		}
	}



	private void setupNotification() {
		if (T) Log.v(TAG, M()+"@in");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

			NotificationChannel channel = new NotificationChannel(
				CHANNEL_AICIA,
				"AICIA",
				NotificationManager.IMPORTANCE_DEFAULT
			);
			channel.enableLights(true);
			channel.setLightColor(Color.WHITE);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			manager.createNotificationChannel(channel);
		}

		SharedPreferences pref = getApplicationContext().getSharedPreferences("ircservice", 0);
		final boolean killedByLowMemory = pref.getBoolean("killedbylowmemory", false);
		if (killedByLowMemory) {
			try {
				// ここで例外発生により落ちる事例がいくつかあるので防止。原因不明
				startForegroundCompat(
						showNotification(
								"Restart",
								(mDonate ? "AiCiA (DONATED)" : "AiCiA"),
								getString(R.string.ircservice_java_restartircservicebylowmemory),
								R.drawable.icon_normal,
								(SystemConfig.fixSystemIcon ? Notification.FLAG_ONGOING_EVENT : 0),
								NOTIFY_SYSTEM,
								null,
								null
						)
				);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			final SharedPreferences.Editor editor = pref.edit();
			editor.putBoolean("killedbylowmemory", false);
			editor.commit();
		} else {
			try {
				// ここで例外発生により落ちる事例がいくつかあるので防止。原因不明
				startForegroundCompat(
						showNotification(
								"Start", 
								(mDonate ? "AiCiA (DONATED)" : "AiCiA"),
								getString(R.string.ircservice_java_startircservice),
								R.drawable.icon_normal,
								(SystemConfig.fixSystemIcon ? Notification.FLAG_ONGOING_EVENT : 0),
								NOTIFY_SYSTEM,
								null,
								null
						)
				);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		clearNotification(NOTIFY_LOWMEMORY);
		pref = null;

		// startTimer();
		if (!mSetAlarm) {
			MyAlarmManager.setAlarmManager(this,new Runnable() {
				@Override
				public void run() {
					if (D) Log.d(TAG, "sendPing()");
					// ActivityMainにKeepalive Pingを送る
					final Intent intent2 = new Intent(ACTION);
					intent2.putExtra("msg", IRCService.PING_PONG);
					sendBroadcast(intent2);
				}
			});
			mSetAlarm = true;
		}

		if (T) Log.v(TAG, M()+"@out");
	}





	/*
	 * 破棄
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

		if (T) Log.v(TAG, M()+": start");
		showNotification(
				"End", 
				(mDonate ? "AiCiA (DONATED)" : "AiCiA"),
				getString(R.string.ircservice_java_shutdownircservice),
				R.drawable.icon_normal,
				(SystemConfig.fixSystemIcon ? Notification.FLAG_ONGOING_EVENT : 0),
				NOTIFY_SYSTEM,
				null,
				null
		);
		if (ircServerList != null) {
			ircServerList.closeAll();
		}
		SharedPreferences pref = getApplicationContext().getSharedPreferences("ircservice", 0);
		final SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("killedbylowmemory", false);
		editor.commit();
		pref = null;
		clearNotification(NOTIFY_SYSTEM);
		clearNotification(NOTIFY_LOWMEMORY);
		for (int i=0; i<ircServerList.getNumServers(); i++) {
			clearNotification(NOTIFY_SERVER_MESSAGE+i);
		}
		// stopTimer();
		unregisterReceiver(mHomeReceiver);
		mSetReceiverHome = false;
		MyAlarmManager.resetAlarmManager(this);
		mSetAlarm = false;
		ircServerList.dispose();
		ircServerList = null;
		if (T) Log.v(TAG, M()+": end");
		System.exit(0);
	}

	// バインド
	/*
	 * バインド
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		if (T) Log.v(TAG, M()+": start: intent=["+intent+"]");
//		me = this;
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			mDonate = (extras.getBoolean("donate"));
		}

		/*
		SystemConfig.setContext(me);
		SystemConfig.loadConfig();
		*/

		if (ircServerList == null) {
			ircServerList = new IRCServerList(me, true);
			ircServerList.addEventListener(new IRCServerListListener());
			ircServerList.reloadList();
			ircServerList.restoreServerIsConnectedFlag();
		}

		if (!mDonate) {
			if (!mPrintDonate) {
				mPrintDonate = true;
				ircServerList.echoMessageToOther("You can select [AiCiA Donate Version]: https://play.google.com/store/apps/details?id=net.gorry.aicia_donate");
			}
		}

		if (!IIRCService.class.getName().equals(intent.getAction())) {
			return null;
		}

		if (T) Log.v(TAG, M()+": end");
		return ircServiceIf;
	}

	/*
	 * 再バインド
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(final Intent intent) {
		if (T) Log.v(TAG, M()+": start");
		if (T) Log.v(TAG, M()+": end");
	}

	/*
	 * バインド解除
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(final Intent intent) {
		if (T) Log.v(TAG, M()+": start");
		if (T) Log.v(TAG, M()+": end");
		return true;
	}

	@Override
	public void onLowMemory() {
		if (T) Log.v(TAG, M()+": start");
		showNotification(
				"Low Memory", 
				(mDonate ? "AiCiA (DONATED)" : "AiCiA"),
				getString(R.string.ircservice_java_onlowmemory),
				R.drawable.icon_warn,
				0,
				NOTIFY_LOWMEMORY,
				null,
				null
		);
		SharedPreferences pref = getApplicationContext().getSharedPreferences("ircservice", 0);
		final SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("killedbylowmemory", true);
		editor.commit();
		pref = null;
		if (T) Log.v(TAG, M()+": end");
	}

	@Override
	public void onTrimMemory(int level) {
		if (T) Log.v(TAG, M()+": start: level="+level);
		if (T) Log.v(TAG, M()+": end");
	}

	//
	/**
	 * サーバリストイベントリスナ
	 */
	private class IRCServerListListener implements IRCServerListEventListener {
		public void receiveMessageToChannel(final String serverName, final String channel, final String nick, final String dateMsg, final SpannableStringBuilder ssb, final SpannableStringBuilder ssbOther, final boolean toSublog, final boolean alert) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], nick=["+nick+"], dateMsg=["+dateMsg+"], ssb=["+ssb+"]");
			if (alert) {
				doAlert(serverName, channel, dateMsg, nick, ssb);
			}
			int serverId = ircServerList.getServerId(serverName);
			final IRCServer ircServer = ircServerList.getServer(serverName);
			int channelId = 0;
			if (ircServer != null) {
				channelId = ircServer.getChannelId(channel);
			}
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", IRCService.RECEIVE_MESSAGE);
			intent.putExtra("server", serverName);
			intent.putExtra("channel", channel);
			intent.putExtra("nick", nick);
			intent.putExtra("date", dateMsg);
			intent.putExtra("ssb", ssb);
			intent.putExtra("ssbOther", ssbOther);
			intent.putExtra("toSublog", toSublog);
			intent.putExtra("alert", alert);
			intent.putExtra("serverid", serverId);
			intent.putExtra("channelid", channelId);
			sendBroadcast(intent);
			if (T) Log.v(TAG, M()+": end");
		}

		public void createServer(final String serverName, final String dateMsg) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], dateMsg=["+dateMsg+"]");
			if (I) Log.i(TAG, M()+": "+dateMsg+" Create Server ["+serverName+"]");
			ircServerList.setCurrentServerName(serverName);
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", IRCService.CREATE_SERVER);
			intent.putExtra("server", serverName);
			intent.putExtra("date", dateMsg);
			sendBroadcast(intent);
			if (T) Log.v(TAG, M()+": end");
		}

		public void removeServer(final String serverName, final String dateMsg) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], dateMsg=["+dateMsg+"]");
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", IRCService.REMOVE_SERVER);
			intent.putExtra("server", serverName);
			intent.putExtra("date", dateMsg);
			sendBroadcast(intent);
			if (T) Log.v(TAG, M()+": end");
		}

		public void createChannel(final String serverName, final String channel, final String dateMsg) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], dateMsg=["+dateMsg+"]");
			if (I) Log.i(TAG, M()+": "+dateMsg+" Create Channel ["+channel+"] on Server ["+serverName+"]");
			ircServerList.setCurrentServerName(serverName);
			ircServerList.setCurrentChannel(serverName, channel);
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", IRCService.CREATE_CHANNEL);
			intent.putExtra("server", serverName);
			intent.putExtra("channel", channel);
			intent.putExtra("date", dateMsg);
			sendBroadcast(intent);
			if (T) Log.v(TAG, M()+": end");
		}

		public void removeChannel(final String serverName, final String channel, final String dateMsg) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], dateMsg=["+dateMsg+"]");
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", IRCService.REMOVE_CHANNEL);
			intent.putExtra("server", serverName);
			intent.putExtra("channel", channel);
			intent.putExtra("date", dateMsg);
			sendBroadcast(intent);
			if (T) Log.v(TAG, M()+": end");
		}

		public void changeNick(final String serverName, final String oldNick, final String newNick, final String dateMsg) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], oldNick=["+oldNick+"], newNick=["+newNick+"], dateMsg=["+dateMsg+"]");
			if (I) Log.i(TAG, "changeNick()");
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", IRCService.CHANGE_NICK);
			intent.putExtra("server", serverName);
			intent.putExtra("oldnick", oldNick);
			intent.putExtra("newnick", newNick);
			intent.putExtra("date", dateMsg);
			sendBroadcast(intent);
			if (T) Log.v(TAG, M()+": end");
		}

		public void changeTopic(final String serverName, final String channel, final String topic, final String dateMsg) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], topic=["+topic+"], dateMsg=["+dateMsg+"]");
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", IRCService.CHANGE_TOPIC);
			intent.putExtra("server", serverName);
			intent.putExtra("channel", channel);
			intent.putExtra("topic", topic);
			intent.putExtra("date", dateMsg);
			sendBroadcast(intent);
			if (T) Log.v(TAG, M()+": end");
		}

		public void receiveConnect(final String serverName) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			if (D) ircServerList.echoMessageToChannel(serverName, IRCMsg.sSystemChannelName, "receiveConnect()");
			if (SystemConfig.showServerIcon) {
				showNotification("Connect " + serverName, 
						serverName + " / " + (mDonate ? "AiCiA (DONATED)" : "AiCiA"),
						String.format(me.getString(R.string.ircservice_java_connectserver),serverName),
						R.drawable.icon_server,
						0,
						NOTIFY_SERVER_MESSAGE + ircServerList.getServerId(serverName),
						serverName,
						null
				);
			}
			if (T) Log.v(TAG, M()+": end");
		}

		public void receiveDisconnect(final String serverName) {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			if (D) ircServerList.echoMessageToChannel(serverName, IRCMsg.sSystemChannelName, "receiveDisconnect()");
			if (SystemConfig.showServerIcon) {
				showNotification("Disconnect " + serverName,
						serverName + " / " + (mDonate ? "AiCiA (DONATED)" : "AiCiA"),
						String.format(me.getString(R.string.ircservice_java_disconnectserver),serverName),
						R.drawable.icon_disconnect,
						0,
						NOTIFY_SERVER_MESSAGE + ircServerList.getServerId(serverName),
						serverName,
						null
				);
			}
			if (ircServerList.getNeedReconnect(serverName)) {
				ircServerList.echoMessageToChannel(serverName, IRCMsg.sSystemChannelName, "Wait " + SystemConfig.autoReconnectWaitSec + " seconds for retry.");
			}
			if (T) Log.v(TAG, M()+": end");
		}
	}

	/**
	 * 通知表示
	 * @param ticker ティッカー
	 * @param title タイトル
	 * @param message メッセージ
	 * @param id ID
	 */
	private Notification showNotification(final String ticker, final String title, final String message, final int icon, final int flag, final int id, final String serverName, final String channel) {
		if (T) Log.v(TAG, M()+": start: id="+id+", ticker="+ticker+", title="+title+", message="+message);
		final Intent intent = new Intent(this, net.gorry.aicia.ActivityMain.class);
		if (id == NOTIFY_SYSTEM) {
			//
		} else if (id == NOTIFY_LOWMEMORY) {
			//
		} else if (id >= NOTIFY_SERVER_MESSAGE) {
/*
			final int serverId = id - NOTIFY_SERVER_MESSAGE;
			if (serverId < ircServerList.getNumServers()) {
				intent.putExtra("serverName", ircServerList.getServerName(serverId));
				intent.setData((Uri.parse("foobar://"+SystemClock.elapsedRealtime())));
			}
*/
			boolean f = false;
			if ((serverName != null) && (serverName.length() > 0)) {
				f = true;
				intent.putExtra("serverName", serverName);
			}
			if ((channel != null) && (channel.length() > 0)) {
				f = true;
				intent.putExtra("channel", channel);
			}
			if (f) {
				intent.setData((Uri.parse("foobar://"+SystemClock.elapsedRealtime())));
			}
		}

		/*
		final Notification notification = new Notification(icon, ticker, System.currentTimeMillis());
		final PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, title, message, pi);
		notification.flags = flag;
		mNotificationManager.cancel(id);
		mNotificationManager.notify(id, notification);
		return notification;
		*/

		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(me);
		builder.setContentIntent(pi);
		builder.setTicker(ticker);
		builder.setContentTitle(title);
		builder.setContentText(message);
		builder.setSmallIcon(icon);
		builder.setSilent(true);
		// builder.setLargeIcon(icon);
		builder.setWhen(System.currentTimeMillis());
		// builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
		builder.setAutoCancel(true);

		if ((flag & Notification.FLAG_ONGOING_EVENT) != 0) {
			builder.setOngoing(true);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder.setChannelId(CHANNEL_AICIA);
		}

		mNotificationManager.cancel(id);
		final Notification notification = builder.build();
		mNotificationManager.notify(id, notification);

		if (T) Log.v(TAG, M()+": end");
		return notification;
	}

	/**
	 * 通知表示の消去
	 * @param id ID
	 */
	private void clearNotification(final int id) {
		if (T) Log.v(TAG, M()+": start: id="+id);
		mNotificationManager.cancel(id);
		if (T) Log.v(TAG, M()+": end");
	}

	/**
	 * 通知表示の全消去
	 */
	private void clearNotificationAll() {
		if (T) Log.v(TAG, M()+": start");
		mNotificationManager.cancelAll();
		if (T) Log.v(TAG, M()+": end");
	}

	/**
	 * フラット化したサーバ名・チャンネルリストの取得
	 * @param flatServerName
	 * @param flatChannel
	 * @param flatChannelUpdated
	 * @param flatChannelAlerted
	 * @return
	 */
	private int getFlatServerChannelList(final ArrayList<String> flatServerName, final ArrayList<String> flatChannel, final ArrayList<Boolean> flatChannelUpdated, final ArrayList<Boolean> flatChannelAlerted ) {
		if (T) Log.v(TAG, M()+": start");
		int noFlat = 0;
		int select = -1;
		final String currentServerName = ircServerList.getCurrentServerName();
		if (currentServerName == null) return -1;
		final String currentChannel = ircServerList.getCurrentChannel(currentServerName);
		if (currentChannel == null) return -1;
		final String[] svList = ircServerList.getServerList();
		for (int i=0; i<svList.length; i++) {
			final String[] chList = ircServerList.getChannelList(svList[i]);
			final Boolean[] chuList = ircServerList.getChannelUpdatedList(svList[i]);
			final Boolean[] chaList = ircServerList.getChannelAlertedList(svList[i]);
			for (int j=0; j<chList.length; j++) {
				flatServerName.add(svList[i]);
				flatChannel.add(chList[j]);
				flatChannelUpdated.add(chuList[j]);
				flatChannelAlerted.add(chaList[j]);
				if (svList[i].equalsIgnoreCase(currentServerName)
						&& chList[j].equalsIgnoreCase(currentChannel)) {
					select = noFlat;
				}
				noFlat++;
			}
		}
		if (T) Log.v(TAG, M()+": end");
		return select;
	}

	/**
	 * IRCサービスAPI
	 */
	private final IIRCService.Stub ircServiceIf = new IIRCService.Stub() {
		public void shutdown() {
			if (T) Log.v(TAG, M()+": start");
			if (I) Log.i(TAG, M()+" API SHUTDOWN");
			mSetReceiverHome = false;
			MyAlarmManager.resetAlarmManager(me);
			mSetAlarm = false;
			ircServerList.closeAll();
			stopSelf();
			if (T) Log.v(TAG, M()+": end");
		}

		public void reloadSystemConfig() {
			if (T) Log.v(TAG, M()+": start");
			SystemConfig.loadConfig();

			loadRingSound();
			if (T) Log.v(TAG, M()+": end");
		}

		public CharSequence getSpanChannelLog(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			CharSequence s = ircServerList.getSpanChannelLog(serverName, channel);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public void clearChannelLog(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			ircServerList.clearChannelLog(serverName, channel);
			if (T) Log.v(TAG, M()+": end");
		}

		public void clearAllChannelLog() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			final int num = ircServerList.getNumServers();
			for (int i=0; i<num; i++) {
				final String serverName = ircServerList.getServerName(i);
				ircServerList.clearAllChannelLog(serverName);
			}
			ircServerList.clearAllChannelLog(IRCMsg.sOtherChannelName);
			ircServerList.echoMessageToOther("Sub-log was cleared.");
			if (T) Log.v(TAG, M()+": end");
		}

		public void reloadChannelConfig(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			ircServerList.getServer(serverName).loadChannelConfig(channel);
			if (T) Log.v(TAG, M()+": end");
		}

		public String getNick(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			String s = ircServerList.getNick(serverName);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public String getTopic(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			String s = ircServerList.getTopic(serverName, channel);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public void sendCommandLine(final String serverName, final String channel, final String message) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], message=["+message+"]");
			ircServerList.sendCommandLine(serverName, channel, message);
			if (T) Log.v(TAG, M()+": end");
		}

		public void sendMessageToChannel(final String serverName, final String channel, final String message) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], message=["+message+"]");
			ircServerList.sendMessageToChannel(serverName, channel, message);
			pushInputHistory(message);
			if (T) Log.v(TAG, M()+": end");
		}

		public void sendQuietCommandLine(final String serverName, final String channel, final String message) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], message=["+message+"]");
			ircServerList.sendQuietCommandLine(serverName, channel, message);
			if (T) Log.v(TAG, M()+": end");
		}

		public void sendQuietMessageToChannel(final String serverName, final String channel, final String message) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], message=["+message+"]");
			ircServerList.sendQuietMessageToChannel(serverName, channel, message);
			pushInputHistory(message);
			if (T) Log.v(TAG, M()+": end");
		}

		public void sendNoticeToChannel(final String serverName, final String channel, final String message) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], message=["+message+"]");
			ircServerList.sendNoticeToChannel(serverName, channel, message);
			pushInputHistory(message);
			if (T) Log.v(TAG, M()+": end");
		}

		public void echoMessageToChannel(final String serverName, final String channel, final String message) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], message=["+message+"]");
			ircServerList.echoMessageToChannel(serverName, channel, message);
			if (T) Log.v(TAG, M()+": end");
		}

		public void connectServer(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			if (I) Log.i(TAG, "connectServer(): serverName="+serverName);
			ircServerList.connectServer(serverName);
			ircServerList.saveServerIsConnectedFlag();
			if (T) Log.v(TAG, M()+": end");
		}

		public void disconnectServer(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			ircServerList.disconnectServer(serverName);
			ircServerList.saveServerIsConnectedFlag();
			if (T) Log.v(TAG, M()+": end");
		}

		public void closeServer(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			ircServerList.closeServer(serverName);
			ircServerList.saveServerIsConnectedFlag();
			if (T) Log.v(TAG, M()+": end");
		}

		public int connectAuto() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			int ret = ircServerList.connectAuto();
			ircServerList.saveServerIsConnectedFlag();
			if (T) Log.v(TAG, M()+": end: ret="+ret);
			return ret;
		}

		public void disconnectAll() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			ircServerList.disconnectAll();
			ircServerList.saveServerIsConnectedFlag();
			if (T) Log.v(TAG, M()+": end");
		}

		public void closeAll() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			ircServerList.closeAll();
			ircServerList.saveServerIsConnectedFlag();
			if (T) Log.v(TAG, M()+": end");
		}

		public void reloadList() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			ircServerList.reloadList();
			if (T) Log.v(TAG, M()+": end");
		}

		public synchronized void addNewServer() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			final int num = ircServerList.getNumServers();
			final IRCServerConfig config = new IRCServerConfig(me);
			config.loadConfig(num);
			final CountDownLatch signal = new CountDownLatch(1);
			final Runnable adder = new Runnable(){ public void run() {
				ircServerList.addServer(config);
				signal.countDown();
			} };
			mHandler.post(adder);
			try {
				signal.await();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			if (T) Log.v(TAG, M()+": end");
		}

		public void reloadServerConfig(final int serverId) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverId="+serverId);
			ircServerList.reloadServerConfig(serverId);
			final String serverName = ircServerList.getServerName(serverId);
			if (SystemConfig.showServerIcon) {
				showNotification("Update " + serverName, 
						serverName + " / " + (mDonate ? "AiCiA (DONATED)" : "AiCiA"),
						String.format(me.getString(R.string.ircservice_java_updateserver),serverName),
						R.drawable.icon_disconnect,
						0,
						NOTIFY_SERVER_MESSAGE + ircServerList.getServerId(serverName),
						serverName,
						null
				);
			}
			if (T) Log.v(TAG, M()+": end");
		}

		public synchronized void removeServer(final int serverId) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverId="+serverId);
			final CountDownLatch signal = new CountDownLatch(1);
			final Runnable remover = new Runnable(){ public void run() {
				ircServerList.removeServer(serverId);
				signal.countDown();
			} };
			mHandler.post(remover);
			try {
				signal.await();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			if (T) Log.v(TAG, M()+": end");
		}

		public void setCurrentServerName(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			ircServerList.setCurrentServerName(serverName);
			if (T) Log.v(TAG, M()+": end");
		}

		public String getCurrentServerName() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			String s = ircServerList.getCurrentServerName();
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public void setCurrentChannel(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			ircServerList.setCurrentChannel(serverName, channel);
			if (T) Log.v(TAG, M()+": end");
		}

		public String getCurrentChannel(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			String s = ircServerList.getCurrentChannel(serverName);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public String[] getServerList() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			String[] s = ircServerList.getServerList();
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public String[] getChannelList(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			String[] s = ircServerList.getChannelList(serverName);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public boolean[] getChannelUpdatedList(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			final Boolean[] b1 = ircServerList.getChannelUpdatedList(serverName);
			final int num = b1.length;
			final boolean[] b2 = new boolean[num];
			for (int i=0; i<num; i++) {
				b2[i] = b1[i];
			}
			if (T) Log.v(TAG, M()+": end: b2=["+b2+"]");
			return b2;
		}

		public boolean getChannelUpdated(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			boolean b = ircServerList.getChannelUpdated(serverName, channel);
			if (T) Log.v(TAG, M()+": end: b="+b);
			return b;
		}

		public void clearChannelUpdated(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			ircServerList.clearChannelUpdated(serverName, channel);
			if (T) Log.v(TAG, M()+": end");
		}

		public void clearChannelUpdatedAll() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			for (int i=0; i<ircServerList.getNumServers(); i++) {
				final IRCServer ircServer = ircServerList.getServer(i);
				for (int j=0; j<ircServer.getNumChannels(); j++) {
					ircServer.clearChannelUpdated(j);
				}
			}
			if (T) Log.v(TAG, M()+": end");
		}

		public boolean[] getChannelAlertedList(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			final Boolean[] b1 = ircServerList.getChannelAlertedList(serverName);
			final int num = b1.length;
			final boolean[] b2 = new boolean[num];
			for (int i=0; i<num; i++) {
				b2[i] = b1[i];
			}
			if (T) Log.v(TAG, M()+": end: b2=["+b2+"]");
			return b2;
		}

		public boolean getChannelAlerted(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			boolean b =ircServerList.getChannelAlerted(serverName, channel);
			if (T) Log.v(TAG, M()+": end: b="+b);
			return b;
		}

		public void clearChannelAlerted(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			ircServerList.clearChannelAlerted(serverName, channel);
			if (T) Log.v(TAG, M()+": end");
		}

		public void clearChannelAlertedAll() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			for (int i=0; i<ircServerList.getNumServers(); i++) {
				final IRCServer ircServer = ircServerList.getServer(i);
				for (int j=0; j<ircServer.getNumChannels(); j++) {
					ircServer.clearChannelAlerted(j);
				}
			}
			if (T) Log.v(TAG, M()+": end");
		}

		public String[] getUserList(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			String[] s = ircServerList.getUserList(serverName, channel);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public boolean changeNextChannel(final int dir, final int mode) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: dir="+dir+", mode="+mode);
			final ArrayList<String> flatServerName = new ArrayList<String>();
			final ArrayList<String> flatChannel = new ArrayList<String>();
			final ArrayList<Boolean> flatChannelUpdated = new ArrayList<Boolean>();
			final ArrayList<Boolean> flatChannelAlerted = new ArrayList<Boolean>();
			final int currentId = getFlatServerChannelList(flatServerName, flatChannel, flatChannelUpdated, flatChannelAlerted);
			final int numFlat = flatServerName.size();
			if (numFlat < 1) return false;
			int newId = currentId;
			while (true) {
				if (dir > 0) {
					newId++;
					if (newId >= numFlat) newId = 0;
				} else {
					newId--;
					if (newId < 0) newId = numFlat-1;
				}
				if (newId == currentId) break;
				if (flatChannel.get(newId).equals(IRCMsg.sSystemChannelName)) continue;
				if (mode == 0) break;
				if (flatChannelUpdated.get(newId)) break;
				if (flatChannelAlerted.get(newId)) break;
			}
			if (newId == currentId) return false;
			ircServerList.setCurrentServerName(flatServerName.get(newId));
			ircServerList.setCurrentChannel(flatServerName.get(newId), flatChannel.get(newId));
			if (T) Log.v(TAG, M()+": end");
			return true;
		}

		public void joinChannel(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			ircServerList.joinChannel(serverName, channel);
			if (T) Log.v(TAG, M()+": end");
		}

		public void joinChannels(final String serverName, final String channels) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channels=["+channels+"]");
			ircServerList.joinChannels(serverName, channels);
			if (T) Log.v(TAG, M()+": end");
		}

		public void partChannel(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			ircServerList.joinChannel(serverName, channel);
			if (T) Log.v(TAG, M()+": end");
		}

		public void saveInputBox(final String input, final int selStart, final int selEnd) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: input=["+input+"], selStart="+selStart+", selEnd="+selEnd);
			mInputBox = new String(input);
			mInputBoxSelStart = selStart;
			mInputBoxSelEnd = selEnd;
			if (T) Log.v(TAG, M()+": end");
		}

		public String loadInputBox() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			String s = new String(mInputBox);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public int loadInputBoxSelStart() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			int ret = mInputBoxSelStart;
			if (T) Log.v(TAG, M()+": end: ret="+ret);
			return ret;
		}

		public int loadInputBoxSelEnd() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			int ret = mInputBoxSelEnd;
			if (T) Log.v(TAG, M()+": end: ret="+ret);
			return ret;
		}

		public void receivePong() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			if (T) Log.v(TAG, M()+": end");
		}

		public boolean isTIGMode(final String serverName) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"]");
			boolean b = ircServerList.isTIGMode(serverName);
			if (T) Log.v(TAG, M()+": end: b="+b);
			return b;
		}

		public void clearLowMemoryNotification() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			clearNotification(NOTIFY_LOWMEMORY);
			if (T) Log.v(TAG, M()+": end");
		}

		public String getVersionString() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			String s = SystemConfig.getVersionString();
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public boolean isPutOnSublog(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			boolean b = ircServerList.isPutOnSublog(serverName, channel);
			if (T) Log.v(TAG, M()+": end: b="+b);
			return b;
		}

		public boolean isPutOnSublogAll(final String serverName, final String channel) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"]");
			boolean b = ircServerList.isPutOnSublogAll(serverName, channel);
			if (T) Log.v(TAG, M()+": end: b="+b);
			return b;
		}

		public String getInputHistory(final int pos) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: pos="+pos);
			String s = getInputHistoryMessage(pos);
			if (T) Log.v(TAG, M()+": end: s=["+s+"]");
			return s;
		}

		public void pushInputHistory(final String message) throws RemoteException {
			if (T) Log.v(TAG, M()+": start: message=["+message+"]");
			pushInputHistoryMessage(message);
			if (T) Log.v(TAG, M()+": end");
		}

		public boolean haveTIGModeInConnectedServers() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			for (int i=0; i<ircServerList.getNumServers(); i++) {
				final IRCServer ircServer = ircServerList.getServer(i);
				if (ircServer.isTIGMode()) {
					if (ircServer.isConnected()) {
						if (T) Log.v(TAG, M()+": end: ret=true");
						return true;
					}
					if (ircServer.isTimerEnabled()) {
						if (T) Log.v(TAG, M()+": end: ret=true");
						return true;
					}
				}
			}
			if (T) Log.v(TAG, M()+": end: ret=false");
			return false;
		}

		public void clearNotify() throws RemoteException {
			if (T) Log.v(TAG, M()+": start");
			clearNotificationAll();
			if (T) Log.v(TAG, M()+": end");
		}

	};

	/**
	 * アラート処理
	 */
	private void doAlert(final String serverName, final String channel, final String date, final String nick, final SpannableStringBuilder ssb) {
		if (T) Log.v(TAG, M()+": start: serverName=["+serverName+"], channel=["+channel+"], date=["+date+"], nick=["+nick+"], ssb=["+ssb+"]");

		if (SystemConfig.notifyOnAlert && ircServerList.isAlertNotify(serverName, channel)) {
			SharedPreferences pref = getApplicationContext().getSharedPreferences("ircservice", 0);
			int notifyAlertCount = pref.getInt("notifyalertcount", 0);
			String str = String.format(me.getString(R.string.ircservice_java_alert_title), nick, channel, serverName);

			showNotification(
					str,
					"<"+channel+"> "+serverName + " / " + (mDonate ? "AiCiA (DONATED)" : "AiCiA"),
					String.format(me.getString(R.string.ircservice_java_alert), date, nick, ssb),
					R.drawable.iconexc,
					0,
					NOTIFY_ALERT + notifyAlertCount,
					serverName,
					channel
			);
			notifyAlertCount = (notifyAlertCount+1)%10000;
			final SharedPreferences.Editor editor = pref.edit();
			editor.putInt("notifyalertcount", notifyAlertCount);
			editor.commit();
		}

		if (SystemConfig.vibrateOnAlert) {
			new Thread(new Runnable() {	public void run() {
				final Vibrator vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
				final long[] pat = {0,200,200,200,200,200,200,200};
				vib.vibrate(pat, -1);
			} }).start();
		}
		if (SystemConfig.ringOnAlert) {
			/*
			new Thread(new Runnable() {	public void run() {
				final ToneGenerator tg = new ToneGenerator(
						AudioManager.STREAM_SYSTEM,
						ToneGenerator.MAX_VOLUME);
				tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {
					//
				}
				tg.stopTone();
			} }).start();
			*/
			try {
				switch (SystemConfig.ringLevel) {
				default:
				case 0:
					mSoundVolume = 0.25F;
					break;
				case 1:
					mSoundVolume = 0.5F;
					break;
				case 2:
					mSoundVolume = 1.0F;
					break;
				}
				if (mSoundId != 0) {
					mSoundPool.stop(mSoundId);
				}
				mSoundId = mSoundPool.play(
						mSound, mSoundVolume, mSoundVolume,
						0, 0, 1
				);
			} catch (final Exception ex) {
				//
			}
		}
		if (T) Log.v(TAG, M()+": end");
	}

	/**
	 * 通知音の読み込み
	 */
	private void loadRingSound() {
		if (T) Log.v(TAG, M()+": start");
		try {
			if (mSoundPool != null) {
				mSoundPool.release();
				mSoundPool = null;
				mSound = 0;
			}
			mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
			try {
				String path = SystemConfig.getExternalPath();
				path += "ring.ogg";
				mSound = mSoundPool.load(path, 1);
			} catch (final Exception e) {
				//
			}
			if (mSound == 0) {
				mSound = mSoundPool.load(me, R.raw.ring, 1);
			}
		} catch (final Exception ex) {
			//
		}
		if (T) Log.v(TAG, M()+": end");
	}

	/**
	 * 入力メッセージを履歴登録
	 * @param message 入力メッセージ
	 */
	private void pushInputHistoryMessage(String message) {
		if (T) Log.v(TAG, M()+": start: message=["+message+"]");
		if ((message == null) || message.equals("")) {
			if (T) Log.v(TAG, M()+": end: empty");
			return;
		}
		for (int i=0; i<mInputHistory.size(); i++) {
			if (mInputHistory.get(i).equals(message)) {
				mInputHistory.remove(i);
				break;
			}
		}
		mInputHistory.add(message);
		if (mInputHistory.size() > mInputHistorySize) {
			mInputHistory.remove(0);
		}
		if (T) Log.v(TAG, M()+": end");
	}

	/**
	 * 入力メッセージ履歴を参照
	 * @param pos 位置
	 * @return 履歴内容
	 */
	private String getInputHistoryMessage(int pos) {
		if (T) Log.v(TAG, M()+": start: pos="+pos);
		if (pos >= mInputHistory.size()) {
			if (T) Log.v(TAG, M()+": end: null");
			return null;
		}
		String s = mInputHistory.get(mInputHistory.size()-pos-1);
		if (T) Log.v(TAG, M()+": end: s=["+s+"]");
		return s;
	}

	/**
	 * タイマー処理
	 */
	/*
	private boolean mTimerStop = false;
	private static final long mTimerStep = 60000;
	private long mTimerCount = 0;
	private long mTimerNext = 0;
	private static final int TIMER = 1;
	private final Handler mTimerHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (msg.what == TIMER) {
				while (SystemClock.uptimeMillis() > mTimerNext) {
					mTimerNext += mTimerStep;
					mTimerCount++;
					onTimer(mTimerCount);
				}
				if (mTimerStop == false) {
					final Message msg2 = mTimerHandler.obtainMessage(TIMER);
					sendMessageAtTime(msg2, mTimerNext);
				}
			}
		}
	};
	private void startTimer() {
		if (D) Log.d(TAG, "startTimer()");
		mTimerStop = false;
		mTimerNext = SystemClock.uptimeMillis();
		final Message msg = mTimerHandler.obtainMessage(TIMER);
		mTimerHandler.sendMessageAtTime(msg, mTimerNext+mTimerStep);
	}
	private void stopTimer() {
		if (D) Log.d(TAG, "stopTimer()");
		mTimerStop = true;
	}
	private long getTimerCount() {
		return mTimerCount;
	}
	private void onTimer(final long timerCount) {
		if (D) Log.d(TAG, "onTimer()");
		// ActivityMainにKeepalive Pingを送る
		final Intent intent = new Intent(ACTION);
		intent.putExtra("msg", IRCService.PING_PONG);
		intent.putExtra("timer", getTimerCount());
		sendBroadcast(intent);
	}
	 */

	/**
	 * アラーム処理
	 */
	/*
	private void setAlarmManager() {
		if (D) Log.d(TAG, "setAlarmManager()");
		if (mAlarmManager == null) {
			Intent intent = new Intent(IRCService.this, AlarmManagerReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
			long firstTime = SystemClock.elapsedRealtime();
			firstTime += 60 * 1000;
			mAlarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
			mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 60 * 1000, sender);
			if (D) Log.d(TAG, "setAlarmManager(): set alarm");
		}
	}

	private void resetAlarmManager() {
		if (D) Log.d(TAG, "resetAlarmManager()");
		if (mAlarmManager != null) {
			Intent intent = new Intent(IRCService.this, AlarmManagerReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			am.cancel(sender);
			mAlarmManager = null;
			if (D) Log.d(TAG, "resetAlarmManager(): reset alarm");
		}

	}
	*/

}
