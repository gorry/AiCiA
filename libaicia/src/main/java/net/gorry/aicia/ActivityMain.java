/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import net.gorry.libaicia.BuildConfig;
import net.gorry.libaicia.R;

/**
 *
 * IRCクライアント「AiCiA」
 *
 * @author GORRY
 *
 */

public class ActivityMain extends Activity {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "ActivityMain";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	/** */
	public static final int ACTIVITY_SYSTEMCONFIG = 1;
	/** */
	public static final int ACTIVITY_EXAPPLIST = 2;
	/** */
	public static final int ACTIVITY_EXWEBLIST = 3;
	/** */
	public static final int ACTIVITY_IRCCHANNELCONFIG = 4;
	/** */
	public static final int ACTIVITY_IRCSERVERLISTCONFIG = 5;
	/** */
	public static final int ACTIVITY_IRCSERVERCONFIG = 6;

	private static Activity me;
	/** */
	private static IIRCService iIRCService;

	/** */
	public static String mCurrentServerName = null;
	/** */
	public static String mCurrentChannel = null;
	/** */
	public static String mTopic = null;

	/** */
	public static boolean mShutdownServiceOnDestroy = false;

	private final IRCServiceReceiver mReceiver = new IRCServiceReceiver();

	private String mInitOpenServerName = null;
	private String mInitOpenChannel = null;

	/** */
	public static Layout layout;
	/** */
	public static DoMain doMain;

	/** */
	public static boolean mDonate;

	/** */
	public static boolean mImportConfig = false;

	private static Toast mToast = null;

	// private CountDownLatch mSignaliIRCServiceSetup;


	/* アプリの一時退避
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		addVerboseLog(4, TAG, "onSaveInstanceState()");
		layout.saveInstanceState(outState);
	}

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		addVerboseLog(4, TAG, "onRestoreInstanceState()");
		layout.restoreInstanceState(savedInstanceState);
	}

	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		addVerboseLog(4, TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		iIRCService = null;
		mImportConfig = false;
		mShutdownServiceOnDestroy = false;
		mInitOpenServerName = null;
		mInitOpenChannel = null;

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final boolean b = (extras.getBoolean("donate"));
			if (b) mDonate = true;
			mInitOpenServerName = extras.getString("serverName");
			mInitOpenChannel = extras.getString("channel");
		}

		me = this;
		MyAppInfo.setContext(me);
		MyIcon.setContext(me);
		layout = new Layout(ActivityMain.this);
		doMain = new DoMain(ActivityMain.this);
		SystemConfig.setContext(ActivityMain.this);
		layout.setOrientation(true);
		SystemConfig.loadConfig();
		layout.setRotateMode();

		// getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		layout.baseLayout_Create(true);

		// mSignaliIRCServiceSetup = new CountDownLatch(1);
		// myBindService();
		// myRegisterReceiver();

		if (Build.VERSION.SDK_INT >= 11) {
			//requestWindowFeature(Window.FEATURE_ACTION_BAR);
			//getActionBar().show();
		}

		if (!SystemConfig.now_showTitleBar) {
			if (Build.VERSION.SDK_INT >= 11) {
				// アクションバーがなくなるとメニューボタンがなくなるため、隠さない
				// getActionBar().hide();
			} else {
				requestWindowFeature(Window.FEATURE_NO_TITLE);
			}
		}

		if (Build.VERSION.SDK_INT >= 11) {
			getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
		}
		
		setContentView(layout.mBaseLayout);

		updateTitle();

		setShowStatusBar();
	}

	/*
	 * 再起動
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public void onRestart() {
		addVerboseLog(4, TAG, "onRestart()");
		super.onRestart();

		mImportConfig = false;

	}

	/*
	 * 通知アイコンからの起動でIntentが付いてるとき
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(final Intent intent) {
		addVerboseLog(4, TAG, "onNewIntent()");
		super.onNewIntent(intent);

		mImportConfig = false;

		mInitOpenServerName = null;
		mInitOpenChannel = null;
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			mInitOpenServerName = extras.getString("serverName");
			mInitOpenChannel = extras.getString("channel");
		}
	}

	/*
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		addVerboseLog(4, TAG, "onStart()");
		super.onStart();

		mImportConfig = false;

		setNoSleepMode(true);
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		addVerboseLog(4, TAG, "onResume()");
		super.onResume();

		mImportConfig = false;

		resumeIRCService(false, null);
	}

	public void resumeIRCService(final boolean init, final Runnable r) {
		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable[] setupService = new Runnable[1];
		final Runnable dismissProgressDialog = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onResume(): dismissProgressDialog");
				pd.dismiss();
				if (r != null) {
					r.run();
				}
			}
		};
		final Runnable postSetupService = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onResume(): postSetupService");
				doMain.setIIRCService(iIRCService);
				layout.setIIRCService(iIRCService);
				layout.reviveLogs();
				setChannelOnResume();
				h.post(dismissProgressDialog);
			}
		};
		final Runnable waitSetupService = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onResume(): waitSetupService");
				new Thread(new Runnable() {	public void run() {
					int count;
					for (count=0; count<100; count++) {
						if (iIRCService != null) break;
						try {
							Thread.sleep(200);
							// mSignaliIRCServiceSetup.await();
						} catch (final InterruptedException e) {
							//
						}
					}
					if (count >= 100) {
						h.post(setupService[0]);
					} else {
						h.post(postSetupService);
					}
				}} ).start();
			}
		};
		setupService[0] = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onResume(): setupService");
				myBindService();
				myRegisterReceiver();
				h.post(waitSetupService);
			}
		};
		final Runnable showProgressDialog = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onResume(): showProgressDialog");
				pd.setTitle(getString(R.string.activitymain_java_progress_bindservice));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(setupService[0]);
			}
		};
		if (init) {
			unregisterReceiver(mReceiver);
			unbindService(ircServiceConn);
			iIRCService = null;
		}
		if (iIRCService != null) {
			try {
				// サービス生存確認
				final String version = SystemConfig.getVersionString();
				final String serviceVersion = iIRCService.getVersionString();
				if (version.equals(serviceVersion)) {
					doMain.setIIRCService(iIRCService);
					layout.setIIRCService(iIRCService);
					final boolean status = setChannelOnResume();
					if (status) {
						return;
					}
				}
				iIRCService = null;
			} catch (final Exception e) {
				iIRCService = null;
			}
		}
		h.post(showProgressDialog);
	}
	
	/**
	 * サービスの復元
	 */
	private boolean setChannelOnResume() {
		addVerboseLog(4, TAG, "resumeService()");
		layout.restoreInputBox();

		try {
			iIRCService.clearLowMemoryNotification();

			final String serverName = mInitOpenServerName;
			String ch = mInitOpenChannel;
			if ((ch == null) || (ch.length() == 0)) {
				ch = iIRCService.getCurrentChannel(serverName);
			}
			if (mInitOpenServerName != null) {
				if (ch != null) {
					final String channel = ch;
					(me).runOnUiThread(new Runnable(){ public void run() {
						doMain.doChangeChannel(serverName, channel, true);
					}});
				}
			}
			mInitOpenServerName = null;
			mInitOpenChannel = null;
		} catch (final RemoteException e) {
			return (false);
		}
		return (true);
	}

	/*
	 * 停止
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		addVerboseLog(4, TAG, "onPause()");
		super.onPause();
		layout.saveInputBox();
	}

	/*
	 * 中止
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		addVerboseLog(4, TAG, "onStop()");
		super.onStop();

		setNoSleepMode(false);
	}

	//
	/*
	 * 破棄
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		addVerboseLog(4, TAG, "onDestroy()");
		super.onDestroy();

		if (mShutdownServiceOnDestroy) {
			// mShutdownServiceOnDestroy = false;
			try {
				if (iIRCService != null) {
					iIRCService.shutdown();
				}
			}
			catch(final RemoteException e) {
				// エラー
			}
		}
		unregisterReceiver(mReceiver);
		unbindService(ircServiceConn);
		
		if (mImportConfig || mShutdownServiceOnDestroy) {
			System.exit(0);
		}
	}

	/*
	 * コンフィギュレーション変更
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public synchronized void onConfigurationChanged(final Configuration newConfig) {
		addVerboseLog(4, TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);

		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable dismissProgressDialog = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onConfigurationChanged(): dismissProgressDialog");
				pd.dismiss();
			}
		};

		final Runnable doChangeConfiguration = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onConfigurationChanged(): doChangeConfiguration");
				layout.changeConfiguration(newConfig);
				h.post(dismissProgressDialog);
			}
		};

		final Runnable showProgressDialog = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "onConfigurationChanged(): showProgressDialog");
				pd.setTitle(getString(R.string.activitymain_java_progress_changeconfiguration));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(doChangeConfiguration);
			}
		};

		h.post(showProgressDialog);

	}

	/*
	 * メインウィンドウにフォーカスが移るときの処理
	 * 子レイアウトを調整するためのエントリポイントとして使う
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public synchronized void onWindowFocusChanged(final boolean b) {
		addVerboseLog(4, TAG, "onWindowFocusChanged()");
		layout.updateBaseLayout();
	}

	/**
	 * IRCサービスへのバインド
	 */
	private void myBindService() {
		addVerboseLog(4, TAG, "myBindService()");
		final Intent i1 = new Intent(this, IRCService.class);
		i1.setPackage(me.getPackageName());
		i1.putExtra("donate", mDonate);
		startService(i1);
		final Intent i2 = new Intent(IIRCService.class.getName());
		i2.setPackage(me.getPackageName());
		i2.putExtra("donate", mDonate);
		bindService(i2, ircServiceConn, BIND_AUTO_CREATE);
	}

	/**
	 * IRCサービスのAPI取得処理
	 */
	private final ServiceConnection ircServiceConn = new ServiceConnection() {
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			addVerboseLog(4, TAG, "ircServiceConn: onServiceConnected()");
			final IIRCService i = IIRCService.Stub.asInterface(service);
			addVerboseLog(4, TAG, "ircServiceConn: iIRCService loaded");
			iIRCService = i;
			// mSignaliIRCServiceSetup.countDown();
		}
		public void onServiceDisconnected(final ComponentName name) {
			addVerboseLog(4, TAG, "ircServiceConn: onServiceDisconnected()");
			iIRCService = null;
			// layout.setIIRCService(iIRCService);
		}
	};

	/**
	 * IRCサービスのブロードキャストレシーバー登録
	 */
	private void myRegisterReceiver() {
		addVerboseLog(4, TAG, "myRegisterReceiver()");
		final IntentFilter filter = new IntentFilter(IRCService.ACTION);
		registerReceiver(mReceiver, filter);
	}

	/**
	 * IRCサービスからのブロードキャスト受信処理
	 */
	public class IRCServiceReceiver extends BroadcastReceiver {
		@Override
		public synchronized void onReceive(final Context context, final Intent intent) {
			if (iIRCService == null) return;
			if (mShutdownServiceOnDestroy) return;
			final Bundle extras = intent.getExtras();
			final int msg = extras.getInt("msg");
			switch (msg) {
			case IRCService.RECEIVE_MESSAGE:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: RECEIVE_MESSAGE");
				final String serverName = extras.getString("server");
				final String channel = extras.getString("channel");
				final String nick = extras.getString("nick");
				final String dateMsg = extras.getString("date");
				final SpannableStringBuilder ssb = new SpannableStringBuilder(extras.getCharSequence("ssb"));
				final SpannableStringBuilder ssbOther = new SpannableStringBuilder(extras.getCharSequence("ssbOther"));
				final Boolean alert = extras.getBoolean("alert");
				// final int serverId = extras.getInt("serverid");
				final int channelId = extras.getInt("channelid");
				boolean toSublog = extras.getBoolean("toSublog");
				// String myNick = null;

				if (alert) {
					doAlert();
				}
				
				// メッセージをMainlog/Sublogに表示するかどうかを決定
				boolean toMainlog = true;
				boolean isCopy = false;
				if ((mCurrentServerName != null) && (mCurrentChannel != null) &&
						serverName.equalsIgnoreCase(mCurrentServerName) &&
						channel.equalsIgnoreCase(mCurrentChannel)) {
					toSublog = false;
				} else {
					toMainlog = false;
					if (alert) {
						if ((mCurrentServerName != null) && (mCurrentChannel != null) &&
								serverName.equalsIgnoreCase(mCurrentServerName) &&
								mCurrentChannel.equalsIgnoreCase(IRCMsg.sSystemChannelName)) {
							toMainlog = true;
							isCopy = true;
						}
					}
				}
				try {
					// myNick = iIRCService.getNick(serverName);
					if (iIRCService.isPutOnSublog(serverName, channel)) {
						if (iIRCService.isPutOnSublogAll(serverName, channel)) {
							toSublog = true;
						}
					} else {
						toSublog = false;
					}
				} catch (final RemoteException e) {
					e.printStackTrace();
				}

				/*
				// 自分がURLのみを投じたらそのURLをwebで開くことにする
				if ((nick != null) && nick.equalsIgnoreCase(myNick)) {
					String line = ssb.toString();
					if (line.startsWith("http")) {
						String url = line.replaceFirst("[ \t].*$", "");
						if (url.equals(line)) {
							openWebPage(url);
						}
					}
				}
				*/
				
				if (toMainlog) {
					final SpannableStringBuilder ssbLine = IRCMsg.colorDateMsg(dateMsg, SystemConfig.mainLogDateColor[SystemConfig.now_colorSet]);
					if (isCopy && SystemConfig.copyToSystemChannelOnAlert) {
						String chid = "";
						if ((channelId >= 1) && (channelId <= 26)) {
							final String idChannel = "abcdefghijklmnopqrstuvwxyz";
							chid = idChannel.substring(channelId-1, channelId) + ":";
						}
						final String chmsg = "<" + chid + channel + "> ";
						final SpannableStringBuilder ssbChannel = new SpannableStringBuilder(chmsg);
						final ForegroundColorSpan c = new ForegroundColorSpan(SystemConfig.mainLogTextColor[SystemConfig.now_colorSet]);
						ssbChannel.setSpan(c, 0, chmsg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						ssbLine.append(ssbChannel);
					}
					if ((nick != null) && (nick.length() > 0)) {
						final SpannableStringBuilder ssbNick = IRCMsg.colorNick(nick, SystemConfig.mainLogTextColor[SystemConfig.now_colorSet]);
						ssbLine.append(ssbNick);
					}
					ssbLine.append(ssb);
					ssbLine.append("\n");
					layout.mMainLogWindow.addMessage(ssbLine);
					try {
						iIRCService.clearChannelUpdated(mCurrentServerName, mCurrentChannel);
						// iIRCService.clearChannelAlerted(mCurrentServerName, mCurrentChannel);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
				}
				if (toSublog) {
					if (SystemConfig.now_subWindowMode > 0) {
						layout.mSubLogWindow.addMessage(ssbOther);
					}
				}
				break;
			}

			case IRCService.CREATE_SERVER:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: CREATE_SERVER");
				final String serverName = extras.getString("server");
				@SuppressWarnings("unused")
				final String dateMsg = extras.getString("date");
				mCurrentServerName = serverName;
				mCurrentServerName = IRCMsg.sSystemChannelName;
				break;
			}

			case IRCService.REMOVE_SERVER:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: REMOVE_SERVER");
				break;
			}

			case IRCService.CREATE_CHANNEL:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: CREATE_CHANNEL");
				final String serverName = extras.getString("server");
				final String channel = extras.getString("channel");
				@SuppressWarnings("unused")
				final String dateMsg = extras.getString("date");
				mCurrentServerName = serverName;
				mCurrentChannel = channel;
				try {
					mTopic = iIRCService.getTopic(serverName, channel);
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
				updateTitle();
				layout.mMainLogWindow.clearLog();
				break;
			}

			case IRCService.REMOVE_CHANNEL:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: REMOVE_CHANNEL");
				break;
			}

			case IRCService.CHANGE_NICK:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: CHANGE_NICK");
				break;
			}

			case IRCService.CHANGE_TOPIC:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: CHANGE_TOPIC");
				final String serverName = extras.getString("server");
				final String channel = extras.getString("channel");
				final String topic = extras.getString("topic");
				@SuppressWarnings("unused")
				final String dateMsg = extras.getString("date");
				if ((mCurrentServerName != null) && (mCurrentChannel != null) &&
						serverName.equalsIgnoreCase(mCurrentServerName) &&
						channel.equalsIgnoreCase(mCurrentChannel)) {
					mTopic = topic;
					updateTitle();
				}
				break;
			}

			case IRCService.PING_PONG:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: PING_PONG");
				try {
					iIRCService.receivePong();
				} catch (final RemoteException e) {
					/*
					 * 不要っぽい
					if (iIRCService != null) {
						(me).runOnUiThread(new Runnable(){ public void run() {
							final Runnable r = new Runnable() {
								public void run() {
									// doConnectAuto();
								}
							};
							resumeIRCService(true, r);
						} });
						return;
					}
					*/
					e.printStackTrace();
				}
				break;
			}

			default:
			{
				addVerboseLog(3, TAG, "IRCServiceReceiver: unknown message: " + msg);
				break;
			}
			}
		}
	}

	/*
	 * オプションメニュー作成
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		addVerboseLog(4, TAG, "onCreateOptionMenu()");
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	/*
	 * オプションメニュー選択処理
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		addVerboseLog(4, TAG, "onOptionsItemSelected()");
		int i = item.getItemId();
		if (i == R.id.serverlist) {
			doMain.doServerListConfig();
			return true;
		} else if (i == R.id.help) {
			doMain.doShowHelp();
			return true;
		} else if (i == R.id.version) {
			doShowVersion();
			return true;
		} else if (i == R.id.connect) {
			new Thread(new Runnable() {
				public void run() {
					doMain.doConnectAuto();
				}
			}).start();
			return true;
		} else if (i == R.id.shutdown) {
			doMain.doShutdown();
			return true;
		} else if (i == R.id.setting) {
			doMain.doSystemConfig();
			return true;
		} else if (i == R.id.exapp) {
			doMain.doExApp();
			return true;
		} else if (i == R.id.exapp_edit) {
			doMain.doExAppConfig();
			return true;
		} else if (i == R.id.exweb) {
			doMain.doExWeb();
			return true;
		} else if (i == R.id.exweb_edit) {
			doMain.doExWebConfig();
			return true;
		} else if (i == R.id.chbutton) {
			doMain.doChButton();
			return true;
		} else if (i == R.id.ubutton) {
			doMain.doUButton();
			return true;
		} else if (i == R.id.copybutton) {
			doMain.doCopyButton();
			return true;
		} else if (i == R.id.inputhistory) {
			doMain.doInputHistoryMenu();
			return true;
		} else if (i == R.id.channel_edit) {
			doMain.doChannelConfig();
			return true;
		} else if (i == R.id.clearnotify) {
			doMain.doClearNotify();
			return true;
		}
		return false;
	}

	/*
	 * アクティビティの結果処理
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		addVerboseLog(4, TAG, "onActivityResult()");
		switch (requestCode) {
		case ACTIVITY_SYSTEMCONFIG:
			addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_SYSTEMCONFIG");
			SystemConfig.loadConfig();
			int rebootLevel = 0;
			int importConfig = 0;
			int exportConfig = 0;
			if (data != null) {
				final Bundle extras = data.getExtras();
				if (extras != null) {
					rebootLevel = extras.getInt("rebootlevel");
					importConfig = extras.getInt("importconfig");
					exportConfig = extras.getInt("exportconfig");
				}
			}
			if ((rebootLevel & 2) != 0) {
				try {
					if (iIRCService != null) {
						iIRCService.reloadSystemConfig();
					}
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
			}
			if ((rebootLevel & 4) != 0) {
				try {
					if (iIRCService != null) {
						iIRCService.clearAllChannelLog();
					}
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
			}
			if ((rebootLevel & 8) != 0) {
				setNoSleepMode(true);
				setShowStatusBar();
			}
			if ((rebootLevel & 1) != 0) {
				final int fRebootLevel = rebootLevel;
				final ProgressDialog pd = new ProgressDialog(me);
				final Handler h = new Handler();
				final Runnable dismissProgressDialog = new Runnable() {
					public void run() {
						addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_SYSTEMCONFIG: dismissProgressDialog");
						pd.dismiss();
					}
				};

				final Runnable doRebootLayout = new Runnable() {
					public void run() {
						addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_SYSTEMCONFIG: doRebootLayout");
						layout.rebootLayout();
						if ((fRebootLevel & 4) != 0) {
							layout.mMainLogWindow.addMessage("Main-log was cleared.", SystemConfig.mainLogTextColor[SystemConfig.now_colorSet], IRCMsg.getDateMsg());
						}
						h.post(dismissProgressDialog);
					}
				};

				final Runnable showProgressDialog = new Runnable() {
					public void run() {
						addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_SYSTEMCONFIG: showProgressDialog");
						pd.setTitle(getString(R.string.activitymain_java_progress_rebootlayout));
						pd.setIndeterminate(true);
						pd.setCancelable(false);
						pd.show();
						// 少し止めないと変更の反映が速すぎることがある
						/*
							try {
								Thread.sleep(500);
							} catch (final InterruptedException e) {
								//
							}
						 */
						h.post(doRebootLayout);
					}
				};
				h.post(showProgressDialog);
			}
			if (importConfig > 0) {
				final Runnable runAskImportConfig = new Runnable() {
					public void run() {
						askImportConfig();
					}
				};
				checkReadPermission(runAskImportConfig);
			}
			if (exportConfig > 0) {
				final Runnable runAskExportConfig = new Runnable() {
					public void run() {
						askExportConfig();
					}
				};
				int ret = checkWritePermission(runAskExportConfig);
			}

			break;

		case ACTIVITY_EXAPPLIST:
			addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_EXAPPLIST");
			break;

		case ACTIVITY_EXWEBLIST:
			addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_EXWEBLIST");
			break;

		case ACTIVITY_IRCCHANNELCONFIG:
			addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_IRCCHANNELCONFIG");
			try {
				if (iIRCService != null) {
					String currentServerName = iIRCService.getCurrentServerName();
					if (currentServerName == null) break;
					String currentChannel = iIRCService.getCurrentChannel(currentServerName);
					if (currentChannel == null) break;
					iIRCService.reloadChannelConfig(currentServerName, currentChannel);
				}
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			break;

		case ACTIVITY_IRCSERVERLISTCONFIG:
		{
			addVerboseLog(4, TAG, "onActivityResult(): ACTIVITY_IRCSERVERLISTCONFIG");
			int disconnect = 0;
			int reload = 0;
			int connect = 0;
			String server = null;
			if (data != null) {
				final Bundle extras = data.getExtras();
				if (extras != null) {
					disconnect = extras.getInt("disconnect");
					connect = extras.getInt("connect");
					if ((disconnect != 0) || (connect != 0)) {
						server = extras.getString("server");
					}
				}
			}
			if (disconnect != 0) {
				try {
					if (iIRCService != null) {
						iIRCService.disconnectServer(server);
					}
				} catch(final RemoteException e) {
					e.printStackTrace();
				}
			}
			if (connect != 0) {
				try {
					if (iIRCService != null) {
						iIRCService.connectServer(server);
					}
				} catch(final Exception e) {
					e.printStackTrace();
				}
			}

			break;
		}

		}

	}

	/**
	 * ノンスリープモードの設定
	 * @param sw trueでセット、falseでクリア
	 */
	public static void setNoSleepMode(final boolean sw) {
		if (SystemConfig.noSleepMode && sw) {
			me.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
		} else {
			me.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	/**
	 * ステータスバーの表示設定
	 */
	public static void setShowStatusBar() {
		if (SystemConfig.now_showStatusBar) {
			me.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			me.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	/**
	 * Short Toastを表示
	 * @param message メッセージ
	 */
	public static void myShortToastShow(final String message) {
		(me).runOnUiThread(new Runnable(){ public void run() {
			if (mToast != null) {
				mToast.cancel();
			}
			mToast = Toast.makeText(me, message, Toast.LENGTH_SHORT);
			mToast.show();
		} });
	}

	/**
	 * Short Toastを表示
	 * @param message メッセージ
	 */
	public static void myShortToastShow2(final String message) {
		(me).runOnUiThread(new Runnable(){ public void run() {
			if (mToast != null) {
				mToast.cancel();
			}
			mToast = Toast.makeText(me, message, Toast.LENGTH_SHORT);
			mToast.setGravity(Gravity.BOTTOM, 0, 0);
			mToast.show();
		} });
	}

	/**
	 * タイトルバーの更新
	 */
	public void updateTitle() {
		if ((mCurrentChannel != null) && (mCurrentServerName != null)) {
			setTitle(IRCMsg.serverChannelTopicTitle(mCurrentServerName, mCurrentChannel, mTopic));
		} else {
			setTitle((mDonate ? "AiCiA (DONATED)" : "AiCiA"));
		}
	}

	/**
	 * アラート処理
	 */
	private void doAlert() {
		// final Vibrator vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		// final long[] pat = {200,200,200,200,200,200,200,200};
		// vib.vibrate(pat, -1);
	}

	/**
	 * バージョン表示
	 */
	public void doShowVersion() {
		addVerboseLog(4, TAG, "doShowVersion()");
		String versionName = null;
		final PackageManager pm = getPackageManager();
		try {
			PackageInfo info = null;
			if (ActivityMain.mDonate) {
				info = pm.getPackageInfo("net.gorry.aicia_donate", 0);
			} else {
				info = pm.getPackageInfo("net.gorry.aicia", 0);
			}
			versionName = info.versionName;
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}
		final AlertDialog.Builder bldr = new AlertDialog.Builder(me);
		if (ActivityMain.mDonate) {
			bldr.setTitle("AiCiA - Android IRC Client (DONATED)");
		} else {
			bldr.setTitle("AiCiA - Android IRC Client");
		}
		bldr.setMessage("Version " + versionName + "\nCopyright (C)2010-2015 GORRY.")
		.setIcon(R.drawable.icon);
		bldr.create().show();
	}

	/**
	 * ログ出力
	 * @param level level
	 * @param tag tag
	 * @param message ログ
	 */
	public static void addVerboseLog(final int level, final String tag, final String message) {
		if (SystemConfig.verbose >= level) {
			if (layout != null) {
				if (layout.mSubLogWindow != null) {
					layout.mSubLogWindow.addMessage("V" + level + ": " + tag + ": " + message, SystemConfig.subLogPaleTextColor[SystemConfig.now_colorSet], IRCMsg.getDateMsg());
				}
			}
			if (D) Log.d(tag, message);
		}
	}

	/**
	 * シャットダウン
	 */
	public void shutdownService() {
		addVerboseLog(4, TAG, "shutdownService()");
		ActivityMain.mShutdownServiceOnDestroy = true;

		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable dismissProgressDialog = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "shutdownService(): dismissProgressDialog");
				pd.dismiss();

				finish();
			}
		};

		final Runnable doCloseAll = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "shutdownService(): doCloseAll");
				new Thread(new Runnable() {	public void run() {
					if (iIRCService != null) {
						try {
							iIRCService.closeAll();
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}

					if (mImportConfig) {
						// mImportConfig = false;
						(me).runOnUiThread(new Runnable(){ public void run() {
							pd.setMessage(getString(R.string.activitymain_java_progress_importconfig));
						}});
						importConfig();
					}

					h.post(dismissProgressDialog);
				} }).start();
			}
		};

		final Runnable showProgressDialog = new Runnable() {
			public void run() {
				addVerboseLog(4, TAG, "shutdownService(): showProgressDialog");
				pd.setTitle(getString(R.string.activitymain_java_progress_closeall));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(doCloseAll);
			}
		};

		h.post(showProgressDialog);
	}

	/**
	 * 設定のエクスポート
	 * @return 成功ならtrue
	 */
	public boolean exportConfig() {
		addVerboseLog(4, TAG, "exportConfig()");
		boolean f = false;
		f = SystemConfig.exportConfig();
		if (!f) return false;
		IRCServerList ircServerList = new IRCServerList(me, false);
		ircServerList.reloadList();
		f = ircServerList.exportIRCServerListConfig();
		if (!f) return false;
		ActivityMain.myShortToastShow(me.getString(R.string.activitysystemconfig_exportconfig_complete));
		return true;
	}

	/**
	 * 設定のエクスポート確認
	 */
	public void askExportConfig() {
		addVerboseLog(4, TAG, "askExportConfig()");
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogOkCancel dlg = new AwaitAlertDialogOkCancel(me);
			dlg.setTitle(me.getString(R.string.activitysystemconfig_exportconfig_dialogtitle));
			final String path = SystemConfig.getExternalPath();
			dlg.setMessage(me.getString(R.string.activitysystemconfig_exportconfig_summary) + "\n" + path + "\n\n" + me.getString(R.string.activitysystemconfig_exportconfig_summary2));
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// エクスポートしてよいか確認
				final int result = dlg.show();
				switch (result) {
				case AwaitAlertDialogBase.OK:
				{
					(me).runOnUiThread(new Runnable(){ public void run() {
						if (!exportConfig()) {
							// エクスポート失敗
							final AwaitAlertDialogOkCancel dlg2 = new AwaitAlertDialogOkCancel(me);
							dlg2.setTitle(me.getString(R.string.activitysystemconfig_error_title));
							dlg2.setMessage(me.getString(R.string.activitysystemconfig_exportconfig_failed_summary) + "\n" + path);
							dlg2.create();
							new Thread(new Runnable() {	public void run() {
								dlg2.show();
							} }).start();
						}
					} });
					return;
				}
				case AwaitAlertDialogBase.CANCEL:
				default:
					break;
				}
			} }).start();
		} });
	}

	/**
	 * 設定のインポート
	 */
	public void importConfig() {
		addVerboseLog(4, TAG, "importConfig()");
		SystemConfig.importConfig();
		SystemConfig.saveConfig();
		IRCServerList ircServerList = new IRCServerList(me, false);
		ircServerList.importIRCServerListConfig(); // インポート後セーブされる
	}

	/**
	 * 設定のインポート確認
	 */
	public void askImportConfig() {
		addVerboseLog(4, TAG, "askImportConfig()");
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogOkCancel dlg = new AwaitAlertDialogOkCancel(me);
			dlg.setTitle(me.getString(R.string.activitysystemconfig_importconfig_dialogtitle));
			final String path = SystemConfig.getExternalPath();
			dlg.setMessage(me.getString(R.string.activitysystemconfig_importconfig_summary) + "\n" + path);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// インポートしてよいか確認
				final int result = dlg.show();
				switch (result) {
				case AwaitAlertDialogBase.OK:
				{
					// インポート指示を返してアクティビティ終了
					(me).runOnUiThread(new Runnable(){ public void run() {
						mImportConfig = true;
						shutdownService();
						// finish();
					} });
					return;
				}
				case AwaitAlertDialogBase.CANCEL:
				default:
					break;
				}
			} }).start();
		} });
	}

	/**
	 * IRCServiceへのメッセージ送信
	 * @param message 送信メッセージ
	 */
	public static void sendMessageToIRCService(String message) {
		addVerboseLog(4, TAG, "sendMessageToIRCService()");
		try {
			final String serverName = ActivityMain.mCurrentServerName;
			final String channel = ActivityMain.mCurrentChannel;
			if ((serverName != null) && (channel != null)) {
				if (message.length() > 0) {
					if (message.startsWith("/")) {
						iIRCService.sendCommandLine(serverName, channel, message);
					} else {
						iIRCService.sendMessageToChannel(serverName, channel, message);
					}
				}
			}
		}
		catch(final RemoteException e) {
			final String dateMsg = IRCMsg.getDateMsg();
			layout.mMainLogWindow.addMessage("** exception on sendMessageByInputBox()", SystemConfig.mainLogTextColor[SystemConfig.now_colorSet], dateMsg);
		}
	}

	/**
	 * IRCServiceへのメッセージ送信（エコーしない）
	 * @param message 送信メッセージ
	 */
	public static void sendQuietMessageToIRCService(String message) {
		addVerboseLog(4, TAG, "sendMessageToIRCService()");
		try {
			final String serverName = ActivityMain.mCurrentServerName;
			final String channel = ActivityMain.mCurrentChannel;
			if ((serverName != null) && (channel != null)) {
				if (message.length() > 0) {
					if (message.startsWith("/")) {
						iIRCService.sendQuietCommandLine(serverName, channel, message);
					} else {
						iIRCService.sendQuietMessageToChannel(serverName, channel, message);
					}
				}
			}
		}
		catch(final RemoteException e) {
			final String dateMsg = IRCMsg.getDateMsg();
			layout.mMainLogWindow.addMessage("** exception on sendMessageByInputBox()", SystemConfig.mainLogTextColor[SystemConfig.now_colorSet], dateMsg);
		}
	}

	/**
	 * IRCServiceへのNoticeメッセージ送信
	 * @param message 送信メッセージ
	 */
	public static void sendNoticeToIRCService(String message) {
		addVerboseLog(4, TAG, "sendNoticeToIRCService()");
		try {
			final String serverName = ActivityMain.mCurrentServerName;
			final String channel = ActivityMain.mCurrentChannel;
			if ((serverName != null) && (channel != null)) {
				if (message.length() > 0) {
					if (message.startsWith("/")) {
						iIRCService.sendCommandLine(serverName, channel, message);
					} else {
						iIRCService.sendNoticeToChannel(serverName, channel, message);
					}
				}
			}
		}
		catch(final RemoteException e) {
			final String dateMsg = IRCMsg.getDateMsg();
			layout.mMainLogWindow.addMessage("** exception on sendMessageByInputBox()", SystemConfig.mainLogTextColor[SystemConfig.now_colorSet], dateMsg);
		}
	}

	/**
	 * webページを開く
	 * @param url URL
	 */
	public static void openWebPage(final String url) {
		addVerboseLog(4, TAG, "openWebPage(): url="+url);
		String url2 = url;
		if (SystemConfig.twitterSiteIsMobile) {
			url2 = url.replaceFirst("[:][/][/]twitter[.]com", "://mobile.twitter.com");
		}
		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url2));
		me.startActivity(intent);
	}

	/**
	 * iIRCServiceを返す
	 * @return iIRCService
	 */
	public static IIRCService getIIRCService() {
		return iIRCService;
	}


	static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 101;
	static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 102;
	static Runnable mRunnableOnRequestPermissionGranted;

	/**
	 * 読み込みパーミッション処理
	 */
	public int checkReadPermission(Runnable r) {
		if (T) Log.v(TAG, M()+"@in: r="+r);

		/*
		int ret = ContextCompat.checkSelfPermission(me, Manifest.permission.READ_EXTERNAL_STORAGE);
		if (ret == PackageManager.PERMISSION_GRANTED) {
			final Handler h = new Handler();
			h.post(r);
			if (T) Log.v(TAG, M()+"@out: granted");
			return 1;
		}
		mRunnableOnRequestPermissionGranted = r;
		ActivityCompat.requestPermissions(
				me,
				new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
				MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
		);
		if (T) Log.v(TAG, M()+"@out: requested");
		return 0;
		*/

		// getExternalFilesDir()のアクセスだけになったので常にgranted
		final Handler h = new Handler();
		h.post(r);
		if (T) Log.v(TAG, M()+"@out: granted");
		return 1;
	}

	/**
	 * 書き込みパーミッション処理
	 */
	public int checkWritePermission(Runnable r) {
		if (T) Log.v(TAG, M()+"@in: r="+r);

		/*
		int ret = ContextCompat.checkSelfPermission(me, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (ret == PackageManager.PERMISSION_GRANTED) {
			final Handler h = new Handler();
			h.post(r);
			if (T) Log.v(TAG, M()+"@out: granted");
			return 1;
		}
		mRunnableOnRequestPermissionGranted = r;
		ActivityCompat.requestPermissions(
				me,
				new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
				MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
		);
		if (T) Log.v(TAG, M()+"@out: requested");
		return 0;
		*/

		// getExternalFilesDir()のアクセスだけになったので常にgranted
		final Handler h = new Handler();
		h.post(r);
		if (T) Log.v(TAG, M()+"@out: granted");
		return 1;
	}

@Override
public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
	if (T) Log.v(TAG, M()+"@in: requestCode="+requestCode+", permissions[]="+permissions+", grantResults="+grantResults);

	switch (requestCode) {
		case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
			if (grantResults.length > 0) {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					final Handler h = new Handler();
					h.post(mRunnableOnRequestPermissionGranted);

					if (T) Log.v(TAG, M()+"@out: grandted");
					return;
				}
			}
			ActivityMain.myShortToastShow(me.getString(R.string.activitysystemconfig_not_granted_readexternalstorage));

			if (T) Log.v(TAG, M()+"@out: not granted");
			return;
		}

		case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
			if (grantResults.length > 0) {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					final Handler h = new Handler();
					h.post(mRunnableOnRequestPermissionGranted);

					if (T) Log.v(TAG, M()+"@out: grandted");
					return;
				}
			}
			ActivityMain.myShortToastShow(me.getString(R.string.activitysystemconfig_not_granted_writeexternalstorage));

			if (T) Log.v(TAG, M()+"@out: not granted");
			return;
		}

	}
}	
}


