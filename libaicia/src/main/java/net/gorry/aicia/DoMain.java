/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.TextView;

import net.gorry.libaicia.R;

/**
 * @author GORRY
 *
 */
public class DoMain {
	private static final String TAG = "DoMain";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = true;
	private final ActivityMain me;
	private IIRCService iIRCService;

	/**
	 * コンストラクタ
	 * @param a アクティビティインスタンス
	 */
	DoMain(final ActivityMain a) {
		me = a;
	}

	/**
	 * iIRCServiceの登録
	 * @param i iIRCService
	 */
	public void setIIRCService(final IIRCService i) {
		if (I) Log.i(TAG, "setIIRCService()");
		iIRCService = i;
	}

	/**
	 * IRCサーバリスト設定へ遷移
	 */
	public void doServerListConfig() {
		if (I) Log.i(TAG, "doServerListConfig()");
		final Intent intent = new Intent(
				me,
				ActivityIRCServerListConfig.class
		);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_IRCSERVERLISTCONFIG);
	}

	/**
	 * システム設定へ遷移
	 */
	public void doSystemConfig() {
		if (I) Log.i(TAG, "doSystemConfig()");
		final Intent intent = new Intent(
				me,
				ActivitySystemConfig.class
		);
		final boolean isLandscape = SystemConfig.getOrientation();
		intent.putExtra("islandscape", isLandscape);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_SYSTEMCONFIG);
	}

	/**
	 * チャンネル設定へ遷移
	 */
	public void doChannelConfig() {
		if (I) Log.i(TAG, "doChannelConfig()");

		int i = 0;
		String currentServerName;
		String currentChannel;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName == null) return;
			currentChannel = iIRCService.getCurrentChannel(currentServerName);
			if (currentChannel == null) return;
			final String[] svList = iIRCService.getServerList();
			for (i=0; i<svList.length; i++) {
				if (currentServerName.equalsIgnoreCase(svList[i])) break;
			}
			if (i >= svList.length) return;
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		final Intent intent = new Intent(
				me,
				ActivityIRCChannelConfig.class
		);
		intent.putExtra("serverid", i);
		intent.putExtra("channelname", ActivityMain.mCurrentChannel);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_IRCCHANNELCONFIG);
	}

	/**
	 * IRCサーバ自動接続
	 */
	public void doConnectAuto() {
		if (I) Log.i(TAG, "doConnectAuto()");
		try {
			int nConnect = 0;
			nConnect = iIRCService.connectAuto();
			if (nConnect < 0) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_noserver));
			} else if (nConnect == 0) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_noserverautoconnect));
			}
		}
		catch(final RemoteException e) {
			if (iIRCService != null) {
				(me).runOnUiThread(new Runnable(){ public void run() {
					final Runnable r = new Runnable() {
						public void run() {
							doConnectAuto();
						}
					};
					me.resumeIRCService(true, r);
				} });
				return;
			}
			// エラー
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_errorautoconnect));
		}
		return;
	}

	/**
	 * 完全終了
	 */
	public void doShutdown() {
		if (I) Log.i(TAG, "doShutdown()");
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogOkCancel dlg = new AwaitAlertDialogOkCancel(me);
			dlg.setTitle(me.getString(R.string.activitymain_java_confirmfinish_title));
			dlg.setMessage(me.getString(R.string.activitymain_java_confirmfinish));
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// 終了してよいか確認
				final int result = dlg.show();
				switch (result) {
				case AwaitAlertDialogBase.OK:
				{
					// ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_closing));
					(me).runOnUiThread(new Runnable(){ public void run() {
						ActivityMain.mShutdownServiceOnDestroy = true;
						me.shutdownService();
						// me.finish();
					} });
					break;
				}
				case AwaitAlertDialogBase.CANCEL:
				default:
					break;
				}
			} }).start();
		} });
	}

	/**
	 * チャンネル変更
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param showToast トーストを表示するならtrue
	 */
	public synchronized void doChangeChannel(final String serverName, final String channel, final boolean showToast) {
		if (I) Log.i(TAG, "doChangeChannel()");
		ActivityMain.mCurrentServerName = new String(serverName);
		ActivityMain.mCurrentChannel = new String(channel);
		try {
			iIRCService.setCurrentServerName(serverName);
			iIRCService.setCurrentChannel(serverName, channel);
			iIRCService.clearChannelUpdated(serverName, channel);
			iIRCService.clearChannelAlerted(serverName, channel);
			ActivityMain.mTopic = iIRCService.getTopic(serverName, channel);
			if (showToast) {
				ActivityMain.myShortToastShow(IRCMsg.serverChannelToast(serverName, channel));
			}
			final CharSequence text = iIRCService.getSpanChannelLog(serverName, channel);
			if (text != null) {
				final SpannableStringBuilder chLog = new SpannableStringBuilder(text);
				ActivityMain.layout.mMainLogWindow.setMessage(chLog);
			}
			me.updateTitle();
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 次のチャンネルへ移動
	 * @param dir 方向
	 * @param mode モード
	 * @param showToast トーストを表示
	 * @return チャンネル移動が成功したらtrue
	 */
	public synchronized boolean doChangeNextChannel(final int dir, final int mode, final boolean showToast) {
		if (I) Log.i(TAG, "doChangeNextChannel()");
		try {
			final boolean result = iIRCService.changeNextChannel(dir, mode);
			if (result) {
				final String serverName = iIRCService.getCurrentServerName();
				final String channel = iIRCService.getCurrentChannel(serverName);
				ActivityMain.mCurrentServerName = serverName;
				ActivityMain.mCurrentChannel = channel;
				ActivityMain.mTopic = iIRCService.getTopic(serverName, channel);
				iIRCService.clearChannelUpdated(serverName, channel);
				iIRCService.clearChannelAlerted(serverName, channel);
				if (showToast) {
					ActivityMain.myShortToastShow(IRCMsg.serverChannelToast(serverName, channel));
				}
				final SpannableStringBuilder chLog = new SpannableStringBuilder(iIRCService.getSpanChannelLog(serverName, channel));
				ActivityMain.layout.mMainLogWindow.setMessage(chLog);
				me.updateTitle();
				return true;
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * チャンネル変更
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 */
	public void doChangeChannelByLogLine(final String[] logLine) {
		if (I) Log.i(TAG, "doChangeChannelByLogLine()");

		if ((logLine.length < 2) || (logLine[0] == null) || (logLine[1] == null)) {
			return;
		}
		
		boolean ok;

		// サーバIDとチャンネル名の取得
		final String regex = "^[^<]*<(.)[^:]*:([^>]*)>.*$";
		final Pattern p = Pattern.compile(regex);
		final Matcher m = p.matcher(logLine[0]);
		int serverId = 0;
		String channelName = null;
		ok = false;
		m.reset();
		if (m.find()) {
			final String num = m.replaceAll("$1");
			serverId = Integer.parseInt(num)-1;
			channelName = m.replaceAll("$2");
			ok = true;
		}
		if (!ok) {
			return;
		}

		// カレントサーバとチャンネル名の取得
		ok = false;
		String currentServerName = null;
		String currentChannel = null;
		String[] serverList = null;
		try {
			serverList = iIRCService.getServerList();
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok || (serverList == null) || (currentServerName == null) || (currentChannel == null)) {
			return;
		}

		// サーバ名＆チャンネル名が同一でなければ移動
		if (serverId < serverList.length) {
			final String newServerName = serverList[serverId];
			if (!currentServerName.equalsIgnoreCase(newServerName) || !currentChannel.equalsIgnoreCase(channelName)) {
				doChangeChannel(newServerName, channelName, true);
			}
		}
		
	}

	/**
	 * フラット化したサーバ名・チャンネルリストの取得
	 * @param flatServerName フラット化したサーバ名リスト
	 * @param flatChannel フラット化したチャンネル名リスト
	 * @param flatChannelUpdated フラット化した更新一覧
	 * @param flatChannelAlerted フラット化した通知一覧
	 * @return カレントチャンネルのインデックス
	 */
	private int getFlatServerChannelList(final ArrayList<String> flatServerName, final ArrayList<String> flatChannel, final ArrayList<Boolean> flatChannelUpdated, final ArrayList<Boolean> flatChannelAlerted ) {
		if (I) Log.i(TAG, "getFlatServerChannelList()");
		int noFlat = 0;
		int select = -1;

		try {
			final String currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName == null) return -1;
			final String currentChannel = iIRCService.getCurrentChannel(currentServerName);
			if (currentChannel == null) return -1;
			final String[] svList = iIRCService.getServerList();
			for (int i = 0; i < svList.length; i++) {
				final String[] chList = iIRCService.getChannelList(svList[i]);
				final boolean[] chuList = iIRCService.getChannelUpdatedList(svList[i]);
				final boolean[] chaList = iIRCService.getChannelAlertedList(svList[i]);
				for (int j = 0; j < chList.length; j++) {
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
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return select;
	}

	/**
	 * 全チャンネルから移動先を選択
	 */
	public void doSelectChannel() {
		if (I) Log.i(TAG, "doSelectChannel()");
		(me).runOnUiThread(new Runnable(){ public void run() {
			// チャンネル一覧を得る
			final AwaitAlertDialogTabList dlg = new AwaitAlertDialogTabList(me);
			final ArrayList<String> flatServerName = new ArrayList<String>();
			final ArrayList<String> flatChannel = new ArrayList<String>();
			final ArrayList<Boolean> flatChannelUpdated = new ArrayList<Boolean>();
			final ArrayList<Boolean> flatChannelAlerted = new ArrayList<Boolean>();
			final int currentId = getFlatServerChannelList(flatServerName, flatChannel, flatChannelUpdated, flatChannelAlerted);
			final int numFlat = flatServerName.size();
			if (numFlat == 0) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
				return;
			}

			// メニューから選択
			for (int i=0; i<flatServerName.size(); i++) {
				String item;
				if (flatChannel.get(i).equals(IRCMsg.sSystemChannelName))
				{
					item = "[" + flatServerName.get(i) + "]";
					dlg.addItem(item, 1);
				} else {
					if (flatChannelAlerted.get(i)) {
						item = "  [@] " + flatChannel.get(i);
					} else if (flatChannelUpdated.get(i)) {
						item = "  [*] " + flatChannel.get(i);
					} else {
						item = "  " + flatChannel.get(i);
					}
					dlg.addItem(item, 2);
				}
			}
			dlg.setTitle(me.getString(R.string.activitymain_java_channellist));
			dlg.setSelection(currentId);
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// 指定チャンネルへ移動
				final int result = dlg.show()-AwaitAlertDialogList.CLICKED;
				if ((0 <= result) && (result < numFlat)) {
					(me).runOnUiThread(new Runnable(){ public void run() {
						doChangeChannel(flatServerName.get(result), flatChannel.get(result), true);
					} });
				}
			} }).start();
		} });
	}

	/**
	 * 更新されたチャンネルから移動先を選択
	 * 移動先には「更新チャンネル」「更新チャンネルが含まれるサーバのシステムチャンネル」が
	 * 必ず含まれる。
	 */
	public void doSelectUpdatedChannel()
	{
		if (I) Log.i(TAG, "doSelectUpdatedChannel()");
		(me).runOnUiThread(new Runnable(){ public void run() {

			// チャンネル一覧を得る
			final AwaitAlertDialogTabList dlg = new AwaitAlertDialogTabList(me);
			final ArrayList<String> flatServerName = new ArrayList<String>();
			final ArrayList<String> flatChannel = new ArrayList<String>();
			final ArrayList<Boolean> flatChannelUpdated = new ArrayList<Boolean>();
			final ArrayList<Boolean> flatChannelAlerted = new ArrayList<Boolean>();
			int currentId = getFlatServerChannelList(flatServerName, flatChannel, flatChannelUpdated, flatChannelAlerted);
			int numFlat = flatServerName.size();
			int select = -1;
			int nItems = 0;
			boolean updated = false;
			for (int i=0; i<flatServerName.size(); i++) {
				if (flatChannelUpdated.get(i)) {
					updated = true;
					break;
				}
			}
			if (!updated) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notupdated));
				return;
			}

			// 最初に「更新フラグのクリア」を追加
			dlg.addItem("[" + me.getString(R.string.activitymain_java_clearupdatedchannel)  + "]", 1);
			nItems++;

			// 更新済みチャンネル一覧を作る
			for (int i=0; i<flatServerName.size(); i++) {
				String item;
				boolean pass = true;
				final boolean isServerName = (flatChannel.get(i).equals(IRCMsg.sSystemChannelName));
				if (isServerName) {
					try {
						final boolean[] chaList = iIRCService.getChannelAlertedList(flatServerName.get(i));
						updated = false;
						for (int j=0; j<chaList.length; j++) {
							if (chaList[j]) {
								updated = true;
								break;
							}
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					if (updated) {
						item = "[" + flatServerName.get(i) + "]";
						dlg.addItem(item, 1);
						if (i == currentId) {
							select = nItems;
						}
						nItems++;
						pass = true;
					}
				} else if (updated) {
					if (flatChannelAlerted.get(i)) {
						item = "  [@] " + flatChannel.get(i);
					} else {
						item = "  " + flatChannel.get(i);
					}
					dlg.addItem(item, 2);
					if (i == currentId) {
						select = nItems;
					}
					nItems++;
					pass = true;
				}
				if (!pass) {
					// 未更新のチャンネルはリストから外す
					flatServerName.remove(i);
					flatChannel.remove(i);
					flatChannelUpdated.remove(i);
					flatChannelAlerted.remove(i);
					numFlat--;
					if (currentId >= i) currentId--;
					i--;
				}
			}
			final int listSize = nItems;

			// メニューから選択
			dlg.setTitle(me.getString(R.string.activitymain_java_updatedchannellist));
			dlg.setSelection(select);
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show()-AwaitAlertDialogList.CLICKED;
				if (result == 0) {
					// 更新フラグのクリア
					try {
						iIRCService.clearChannelUpdatedAll();
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_clearupdatedchannel_done));
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
				} else if ((1 <= result) && (result < listSize)) {
					// 指定チャンネルへ移動
					(me).runOnUiThread(new Runnable(){ public void run() {
						doChangeChannel(flatServerName.get(result-1), flatChannel.get(result-1), true);
					} });
				}
			} }).start();
		} });
	}


	/**
	 * 通知されたチャンネルから移動先を選択
	 * 移動先には「通知チャンネル」「通知チャンネルが含まれるサーバのシステムチャンネル」が
	 * 必ず含まれる。
	 */
	public void doSelectAlertedChannel()
	{
		if (I) Log.i(TAG, "doSelectAlertedChannel()");
		(me).runOnUiThread(new Runnable(){ public void run() {

			// チャンネル一覧を得る
			final AwaitAlertDialogTabList dlg = new AwaitAlertDialogTabList(me);
			final ArrayList<String> flatServerName = new ArrayList<String>();
			final ArrayList<String> flatChannel = new ArrayList<String>();
			final ArrayList<Boolean> flatChannelUpdated = new ArrayList<Boolean>();
			final ArrayList<Boolean> flatChannelAlerted = new ArrayList<Boolean>();
			int currentId = getFlatServerChannelList(flatServerName, flatChannel, flatChannelUpdated, flatChannelAlerted);
			int numFlat = flatServerName.size();
			int select = -1;
			int nItems = 0;
			boolean alerted = false;
			for (int i=0; i<flatServerName.size(); i++) {
				if (flatChannelAlerted.get(i)) {
					alerted = true;
					break;
				}
			}
			if (!alerted) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notalerted));
				return;
			}

			// 最初に「通知フラグのクリア」を追加
			dlg.addItem("[" + me.getString(R.string.activitymain_java_clearalertedchannel)  + "]", 1);
			nItems++;

			// 通知済みチャンネル一覧を作る
			for (int i=0; i<flatServerName.size(); i++) {
				String item;
				boolean pass = false;
				final boolean isServerName = (flatChannel.get(i).equals(IRCMsg.sSystemChannelName));
				if (isServerName) {
					try {
						final boolean[] chaList = iIRCService.getChannelAlertedList(flatServerName.get(i));
						alerted = false;
						for (int j=0; j<chaList.length; j++) {
							if (chaList[j]) {
								alerted = true;
								break;
							}
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					if (alerted) {
						item = "[" + flatServerName.get(i) + "]";
						dlg.addItem(item, 1);
						if (i == currentId) {
							select = nItems;
						}
						nItems++;
						pass = true;
					}
				} else if (flatChannelAlerted.get(i)) {
					item = "  " + flatChannel.get(i);
					dlg.addItem(item, 2);
					if (i == currentId) {
						select = nItems;
					}
					nItems++;
					pass = true;
				}
				if (!pass) {
					// 未通知のチャンネルはリストから外す
					flatServerName.remove(i);
					flatChannel.remove(i);
					flatChannelUpdated.remove(i);
					flatChannelAlerted.remove(i);
					numFlat--;
					if (currentId >= i) currentId--;
					i--;
				}
			}
			final int listSize = nItems;

			// メニューから選択
			dlg.setTitle(me.getString(R.string.activitymain_java_alertedchannellist));
			dlg.setSelection(select);
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show()-AwaitAlertDialogList.CLICKED;
				if (result == 0) {
					// 通知フラグのクリア
					try {
						iIRCService.clearChannelAlertedAll();
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_clearalertedchannel_done));
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
				} else if ((1 <= result) && (result < listSize)) {
					// 指定チャンネルへ移動
					(me).runOnUiThread(new Runnable(){ public void run() {
						doChangeChannel(flatServerName.get(result-1), flatChannel.get(result-1), true);
					} });
				}
			} }).start();
		} });
	}
	/**
	 * ダイアログ選択でユーザーリストを作成する
	 * @param userList 選択肢となるユーザーリスト
	 * @param titleResource ダイアログタイトルリソース
	 * @param isTIG TIGモードのときtrue
	 * @return 選択されたユーザーのリスト
	 */
	private String[] getUserListFromDialog(final String[] userList, final int titleResource, final boolean isTIG) {
		if (I) Log.i(TAG, "getUserListFromDialog()");
		final Object o = new Object();
		// ユーザー名の取得
		final ArrayList<String> selectedUserList = new ArrayList<String>();
		if (userList.length == 0) {
			return null;
		}

		// ユーザー名リストを作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogListMulti dlg = new AwaitAlertDialogListMulti(me);
			for (int i=0; i<userList.length; i++) {
				if (isTIG) {
					dlg.addItem("@" + userList[i], 2, false);
				} else {
					dlg.addItem(userList[i], 2, false);
				}
			}
			dlg.setTitle(me.getString(titleResource));
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show();
				if (result == AwaitAlertDialogBase.CANCEL) {
					synchronized (o) {
						o.notifyAll();
					}
					return;
				}
				final Boolean[] selection = dlg.getSelection();
				for (int i=0; i<selection.length; i++) {
					if (selection[i]) {
						if (isTIG) {
							selectedUserList.add("@" + userList[i]);
						} else {
							selectedUserList.add(userList[i]);
						}
					}
				}
				synchronized (o) {
					o.notifyAll();
				}
			} }).start();
		}});
		synchronized (o) {
			try {
				o.wait();
			} catch (final InterruptedException e) {
				//
			}
		}
		if (selectedUserList.size() == 0 ) {
			return null;
		}
		final String[] retUserList = selectedUserList.toArray(new String[0]);
		return retUserList;
	}

	/**
	 * ダイアログ選択でユーザーを得る
	 * @param userList 選択肢となるユーザーリスト
	 * @param titleResource ダイアログタイトルリソース
	 * @param isTIG TIGモードのときtrue
	 * @return 選択されたユーザー
	 */
	private String getUserFromDialog(final String[] userList, final int titleResource, final boolean isTIG) {
		if (I) Log.i(TAG, "getUserFromDialog()");
		final Object o = new Object();
		// ユーザー名の取得
		final String[] selectedUser = new String[1];
		if (userList.length == 0) {
			return null;
		}
		selectedUser[0] = null;

		// ユーザー名リストを作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			for (int i=0; i<userList.length; i++) {
				if (isTIG) {
					dlg.addItem("@" + userList[i], 2);
				} else {
					dlg.addItem(userList[i], 2);
				}
			}
			dlg.setTitle(me.getString(titleResource));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show();
				final int id = result - AwaitAlertDialogList.CLICKED;
				if ((result == AwaitAlertDialogBase.CANCEL) || (id < 0) || (id >= userList.length)) {
					synchronized (o) {
						o.notifyAll();
					}
					return;
				}
				if (isTIG) {
					selectedUser[0] = "@" + userList[id];
				} else {
					selectedUser[0] = userList[id];
				}
				synchronized (o) {
					o.notifyAll();
				}
			} }).start();
		}});
		synchronized (o) {
			try {
				o.wait();
			} catch (final InterruptedException e) {
				//
			}
		}
		if (selectedUser[0] == null) {
			return null;
		}
		return selectedUser[0];
	}

	/**
	 * カレントチャンネルユーザーからダイアログ選択でユーザーリストを作成する
	 * @param titleResource ダイアログタイトルリソース
	 * @param isTIG TIGモードのときtrue
	 * @param multiSelect ２人以上選択したいときはtrue
	 * @return 選択されたユーザーのリスト
	 */
	public String[] getUserListFromCurrentChannel(final int titleResource, final boolean isTIG, final boolean multiSelect) {
		if (I) Log.i(TAG, "getUserListFromCurrentChannel()");
		// カレントチャンネルの取得
		boolean ok = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return null;
		}

		// ユーザー名の取得
		final String[] userList;
		try {
			userList = iIRCService.getUserList(currentServerName, currentChannel);
		} catch (final RemoteException e) {
			e.printStackTrace();
			return null;
		}
		if (userList.length == 0) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nouserlist));
			return null;
		}

		// 選択ユーザー名を取得
		if (multiSelect) {
			return getUserListFromDialog(userList, titleResource, isTIG);
		}
		final String user = getUserFromDialog(userList, titleResource, isTIG);
		if (user == null) {
			return null;
		}
		final String retUser[] = new String[1];
		retUser[0] = user;
		return retUser;
	}

	/**
	 * カレントチャンネルTIGログからダイアログ選択でユーザーリストを作成する
	 * @param titleResource ダイアログタイトルリソース
	 * @param pickPrivUser 発言ユーザーを拾うときtrue
	 * @param pickTargetUser 発言内で対象になっているユーザーを拾うときtrue
	 * @param multiSelect ２人以上選択したいときはtrue
	 * @param isOtherChannel Otherチャンネルから作成したいときtrue
	 * @return 選択されたユーザーのリスト
	 */
	public String[] getUserListFromTIGLog(final int titleResource, final boolean pickPrivUser, final boolean pickTargetUser, final boolean multiSelect, final boolean isOtherChannel) {
		if (I) Log.i(TAG, "getUserListFromTIGLog()");
		// カレントチャンネルの取得
		boolean ok = false;
		String currentServerName = IRCMsg.sOtherChannelServerName;
		String currentChannel = IRCMsg.sOtherChannelName;
		if (!isOtherChannel) {
			try {
				currentServerName = iIRCService.getCurrentServerName();
				if (currentServerName != null) {
					currentChannel = iIRCService.getCurrentChannel(currentServerName);
					if (currentChannel != null) {
						ok = true;
					}
				}
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			if (!ok) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
				return null;
			}
		}

		// ログの取得
		final ArrayList<String> aUserList = new ArrayList<String>();
		String log = null;
		try {
			log = iIRCService.getSpanChannelLog(currentServerName, currentChannel).toString();
		} catch (final RemoteException e) {
			e.printStackTrace();
			return null;
		}
		if (log == null) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nouserlist));
			return null;
		}

		// ログを新しいほうから読んでユーザー名の取得
		final String regex = "[@][A-Za-z0-9_]+";
		final Pattern p = Pattern.compile(regex);
		String regex2 = "^[^<]*<([^>]*)>.*$";
		if (isOtherChannel) {
			regex2 = "^[^<]*<[^>]*>[^<]*<([^>]*)>.*$";
		}
		final Pattern p2 = Pattern.compile(regex2);
		final String[] loglines = log.split("\n");
		for (int i=loglines.length-1; i>=0; i--) {
			if (pickPrivUser) {
				final Matcher m2 = p2.matcher(loglines[i]);
				if (m2.find()) {
					final String user = m2.replaceAll("$1");
					if (!aUserList.contains(user)) {
						aUserList.add(user);
					}
				}
			}
			if (pickTargetUser) {
				final Matcher m = p.matcher(loglines[i]);
				m.reset();
				while (m.find()) {
					final String user = m.group().substring(1);
					if (!aUserList.contains(user)) {
						aUserList.add(user);
					}
				}
			}
		}
		final int size = aUserList.size();
		final String[] userList = new String[size];
		for (int i=0; i<size; i++) {
			userList[i] = new String(aUserList.get(i));
		}

		// 選択ユーザー名を取得
		if (multiSelect) {
			return getUserListFromDialog(userList, titleResource, true);
		}
		final String retUser[] = new String[1];
		retUser[0] = getUserFromDialog(userList, titleResource, true);
		return retUser;

	}

	/**
	 * IRC用ユーティリティメニューの表示
	 */
	public void doInsertIRCUser() {
		if (I) Log.i(TAG, "doInsertIRCUser()");
		// 選択ユーザー名を挿入
		new Thread(new Runnable() {	public void run() {
			final String[] userList = getUserListFromCurrentChannel(
					R.string.activitymain_java_insertfromuserlist,
					false, true
			);
			if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
				(me).runOnUiThread(new Runnable(){ public void run() {
					for (int i=0; i<userList.length; i++) {
						EditAssist.insert(ActivityMain.layout.mInputBox, userList[i] + " ");
					}
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			}
		} }).start();
	}

	/**
	 * ダイアログ選択でTypableMapを含むコマンドラインを作成する
	 * @param userList 選択肢となるユーザーリスト
	 * @param titleResource ダイアログタイトルリソース
	 * @param isTIG TIGモードのときtrue
	 * @return 選択されたユーザーのリスト
	 */
	/*
	private String getTIGLogLineFromDialog(final int cmdno, final ArrayList<String> aTypableMapList, final ArrayList<String> aLineList, final int titleResource, final boolean isOtherChannel) {
		if (I) Log.i(TAG, "getTIGCommandFromDialog()");
		final Object o = new Object();
		final String[] retCmdLine = new String[1];
		retCmdLine[0] = null;

		// ユーザー名リストを作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			for (int i=0; i<aTypableMapList.size(); i++) {
				dlg.addItem(aTypableMapList.get(i) + ": " + aLineList.get(i), 2);
			}
			dlg.setTitle(me.getString(titleResource));
			dlg.setLongClickEnable(false);
			dlg.setListResource(R.layout.alertdialoglist_typablemap);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show();
				if (result == AwaitAlertDialogBase.CANCEL) {
					synchronized (o) {
						o.notifyAll();
					}
					return;
				}
				final int id = result-AwaitAlertDialogList.CLICKED;
				switch (cmdno) {
				case 0:  // re
				{
					final ArrayList<String> aUserList = new ArrayList<String>();
					// 自分を足さないようにする
					try {
						final String serverName = iIRCService.getCurrentServerName();
						final String nick = iIRCService.getNick(serverName);
						aUserList.add("@" + nick);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}

					// re先の人を足さないようにする
					String regex2 = "^[^<]*<([^>]*)>.*$";
					if (isOtherChannel) {
						regex2 = "^[^<]*<[^>]*>[^<]*<([^>]*)>.*$";
					}
					final Pattern p2 = Pattern.compile(regex2);
					final Matcher m2 = p2.matcher(aLineList.get(id));
					if (m2.find()) {
						final String user = m2.replaceAll("$1");
						if (!aUserList.contains(user)) {
							aUserList.add("@" + user);
						}
					}

					// tweet内の人を足す
					retCmdLine[0] = "re " + aTypableMapList.get(id) + " ";
					final String regex = "[@][A-Za-z0-9_]+";
					final Pattern p = Pattern.compile(regex);
					String line = aLineList.get(id);
					String regex3 = "^[^<]*<[^>]*>(.*)$";
					if (isOtherChannel) {
						regex3 = "^[^<]*<[^>]*>[^<]*<[^>]*>(.*)$";
					}
					final Pattern p3 = Pattern.compile(regex3);
					final Matcher m3 = p3.matcher(line);
					if (m3.find()) {
						line = m3.replaceAll("$1");
					}
					final Matcher m = p.matcher(line);
					m.reset();
					while (m.find()) {
						final String user = m.group();
						if (!aUserList.contains(user)) {
							aUserList.add(user);
							retCmdLine[0] = retCmdLine[0] + user + " ";
						}
					}
					break;
				}
				case 1:  // rt
					retCmdLine[0] = "rt " + aTypableMapList.get(id);
					break;
				case 2:  // qt
					retCmdLine[0] = "qt " + aTypableMapList.get(id) + " ";
					break;
				case 3:  // ort
					retCmdLine[0] = SystemConfig.TIG_oldRTCmd + " " + aTypableMapList.get(id) + " ";
					break;
				case 9:  // fav
					retCmdLine[0] = "fav " + aTypableMapList.get(id) + " ";
					break;
				}
				synchronized (o) {
					o.notifyAll();
				}
			} }).start();
		}});
		synchronized (o) {
			try {
				o.wait();
			} catch (final InterruptedException e) {
				//
			}
		}
		return retCmdLine[0];
	}
	*/

	/**
	 * TIGログからユーザー名を抽出する
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 * @return ユーザーリスト
	 */
	public ArrayList<String> extractUserListFromLogLine(final String[] logLine, final boolean isOtherChannel) {
		final ArrayList<String> aUserList = new ArrayList<String>();
		final ArrayList<String> aAvoidUserList = new ArrayList<String>();

		if ((logLine.length < 2) || (logLine[0] == null) || (logLine[1] == null)) {
			return aUserList;
		}

		// 自分を足さないようにする
		try {
			final String serverName = iIRCService.getCurrentServerName();
			final String nick = iIRCService.getNick(serverName);
			aAvoidUserList.add("@" + nick);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		// 発言者を足す
		String regex2 = "^[^<]*<([^>]*)>.*$";
		if (isOtherChannel) {
			regex2 = "^[^<]*<[^>]*>[^<]*<([^>]*)>.*$";
		}
		final Pattern p2 = Pattern.compile(regex2);
		final Matcher m2 = p2.matcher(logLine[0]);
		if (m2.find()) {
			final String user = m2.replaceAll("$1");
			if (!aAvoidUserList.contains(user)) {
				aUserList.add("@" + user);
			}
		}

		// tweet内の人を足す
		final String regex = "[@][A-Za-z0-9_]+";
		final Pattern p = Pattern.compile(regex);
		String line = logLine[0];
		String regex3 = "^[^<]*<[^>]*>(.*)$";
		if (isOtherChannel) {
			regex3 = "^[^<]*<[^>]*>[^<]*<[^>]*>(.*)$";
		}
		final Pattern p3 = Pattern.compile(regex3);
		final Matcher m3 = p3.matcher(line);
		if (m3.find()) {
			line = m3.replaceAll("$1");
		}
		final Matcher m = p.matcher(line);
		m.reset();
		while (m.find()) {
			final String user = m.group();
			if (!aAvoidUserList.contains(user)) {
				aAvoidUserList.add(user);
				aUserList.add(user);
			}
		}

		return aUserList;
	}

	/**
	 * IRCログ行を得る
	 * @param titleResource ダイアログタイトルリソース
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 * @param enableLongClick ロングタップを許可するときtrue
	 * @param isTIGOnly TIGログ行のみを対象とするときtrue
	 * @param toastId 同時に表示するToastのID（0で非表示）
	 * @return TIGログ行[0]とTypableMap名[1]、[2]が"true"ならロングタップ
	 */
	public String[] selectIRCLogLine(final int titleResource, final boolean isOtherChannel, final boolean enableLongClick, final boolean isTIGOnly, final int toastId) {
		if (I) Log.i(TAG, "selectIRCLogLine()");
		// カレントチャンネルの取得
		boolean ok = false;
		String currentServerName = IRCMsg.sOtherChannelServerName;
		String currentChannel = IRCMsg.sOtherChannelName;
		if (!isOtherChannel) {
			try {
				currentServerName = iIRCService.getCurrentServerName();
				if (currentServerName != null) {
					currentChannel = iIRCService.getCurrentChannel(currentServerName);
					if (currentChannel != null) {
						ok = true;
					}
				}
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			if (!ok) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
				return null;
			}
		}

		// ログの取得
		final ArrayList<String> aLineList = new ArrayList<String>();
		final ArrayList<String> aTypableMapList = new ArrayList<String>();
		String log = null;
		try {
			log = iIRCService.getSpanChannelLog(currentServerName, currentChannel).toString();
		} catch (final RemoteException e) {
			e.printStackTrace();
			return null;
		}
		if (log == null) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nolog));
			return null;
		}
		// ログを新しいほうから読んでtypableMap行の取得
		if (isTIGOnly) {
			final String regex = "^.* \\(([a-z]*)\\)$";  // typableMapのある行にマッチ
			final Pattern p = Pattern.compile(regex);
			/*
			final String regex2 = " \\([a-z]*\\)$";  // typableMap部の削除
			final Pattern p2 = Pattern.compile(regex2);
			*/
			final String[] loglines = log.split("\n");
			for (int i=loglines.length-1; i>=0; i--) {
				final Matcher m = p.matcher(loglines[i]);
				if (m.find()) {
					final String tmap = m.replaceAll("$1");
					/*
					// typableMapをカットする
					final Matcher m2 = p2.matcher(loglines[i]);
					final String line = m2.replaceAll("");
					aLineList.add(line);
					 */
					aLineList.add(loglines[i]);
					aTypableMapList.add(tmap);
				}
			}
		} else if (isOtherChannel) {
			final String regex = "^.* \\(([a-z]*)\\)$";  // typableMapのある行にマッチ
			final Pattern p = Pattern.compile(regex);
			final String regex2 = "^[^<]*<[^>]*[>] [<][^>]*[>] .*$";  // 発言行にマッチ
			final Pattern p2 = Pattern.compile(regex2);
			final String[] loglines = log.split("\n");
			for (int i=loglines.length-1; i>=0; i--) {
				final Matcher m = p.matcher(loglines[i]);
				if (m.find()) {
					final String tmap = m.replaceAll("$1");
					/*
					// typableMapをカットする
					final Matcher m2 = p2.matcher(loglines[i]);
					final String line = m2.replaceAll("");
					aLineList.add(line);
					 */
					aLineList.add(loglines[i]);
					aTypableMapList.add(tmap);
				} else {
					final Matcher m2 = p2.matcher(loglines[i]);
					if (m2.find()) {
						aLineList.add(loglines[i]);
						aTypableMapList.add("");
					}
				}
			}
		} else {
			final String regex = "^.* \\(([a-z]*)\\)$";  // typableMapのある行にマッチ
			final Pattern p = Pattern.compile(regex);
			final String regex2 = "^[^<]*<[^>]*[>] .*$";  // 発言行にマッチ
			final Pattern p2 = Pattern.compile(regex2);
			final String[] loglines = log.split("\n");
			for (int i=loglines.length-1; i>=0; i--) {
				final Matcher m = p.matcher(loglines[i]);
				if (m.find()) {
					final String tmap = m.replaceAll("$1");
					/*
					// typableMapをカットする
					final Matcher m2 = p2.matcher(loglines[i]);
					final String line = m2.replaceAll("");
					aLineList.add(line);
					 */
					aLineList.add(loglines[i]);
					aTypableMapList.add(tmap);
				} else {
					final Matcher m2 = p2.matcher(loglines[i]);
					if (m2.find()) {
						aLineList.add(loglines[i]);
						aTypableMapList.add("");
					}
				}
			}
		}
		
		final Object o = new Object();
		final String[] retLine = new String[3];
		retLine[0] = null;
		retLine[1] = null;
		retLine[2] = "";

		// ログを選択する
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			for (int i=0; i<aTypableMapList.size(); i++) {
				// dlg.addItem(aTypableMapList.get(i) + ": " + aLineList.get(i), 2);
				dlg.addItem(aLineList.get(i), 2);
			}
			dlg.setTitle(me.getString(titleResource));
			dlg.setLongClickEnable(enableLongClick);
			dlg.setListResource(R.layout.alertdialoglist_typablemap);
			dlg.create();
			if (toastId != 0) {
				ActivityMain.myShortToastShow2(me.getString(toastId));
			}
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result == AwaitAlertDialogBase.CANCEL) {
					synchronized (o) {
						o.notifyAll();
					}
					return;
				}
				if (result >= AwaitAlertDialogList.LONGCLICKED) {
					retLine[2] = "true";
					result -= AwaitAlertDialogList.LONGCLICKED;
				} else if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					synchronized (o) {
						o.notifyAll();
					}
					return;
				}
				final int id = result;
				retLine[0] = aLineList.get(id);
				retLine[1] = aTypableMapList.get(id);
				synchronized (o) {
					o.notifyAll();
				}
			} }).start();
		}});
		synchronized (o) {
			try {
				o.wait();
			} catch (final InterruptedException e) {
				//
			}
		}

		return retLine;
	}

	/**
	 * TIGログ行を得る
	 * @param titleResource ダイアログタイトルリソース
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 * @param enableLongClick ロングタップを許可するときtrue
	 * @param toastId 同時に表示するToastのID（0で非表示）
	 * @return TIGログ行[0]とTypableMap名[1]、[2]が"true"ならロングタップ
	 */
	public String[] selectTIGLogLine(final int titleResource, final boolean isOtherChannel, final boolean enableLongClick, final int toastId) {
		if (I) Log.i(TAG, "selectTIGLogLine()");
		return selectIRCLogLine(titleResource, isOtherChannel, enableLongClick, true, toastId);
	}
	
	/**
	 * TIGコマンドを得る
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param cmd TIGコマンド（"RE", "RT", "QT", "ORT", "FAV"）
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 * @return TIGコマンドライン
	 */
	public String getTIGCommand(final String[] logLine, final String cmd, final boolean isOtherChannel) {
		if (I) Log.i(TAG, "getTIGCommand()");

		String retCmdLine = null;

		// ログ行の取得
		if ((logLine.length < 2) || (logLine[0] == null) || (logLine[1] == null)) {
			return null;
		}
		
		if (cmd.equalsIgnoreCase("RE")) {
			final ArrayList<String> aUserList = new ArrayList<String>();
			// 自分を足さないようにする
			try {
				final String serverName = iIRCService.getCurrentServerName();
				final String nick = iIRCService.getNick(serverName);
				aUserList.add("@" + nick);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}

			// re先の人を足さないようにする
			String regex2 = "^[^<]*<([^>]*)>.*$";
			if (isOtherChannel) {
				regex2 = "^[^<]*<[^>]*>[^<]*<([^>]*)>.*$";
			}
			final Pattern p2 = Pattern.compile(regex2);
			final Matcher m2 = p2.matcher(logLine[0]);
			if (m2.find()) {
				final String user = m2.replaceAll("$1");
				if (!aUserList.contains(user)) {
					aUserList.add("@" + user);
				}
			}

			// tweet内の人を足す
			retCmdLine = "re " + logLine[1] + " ";
			final String regex = "[@][A-Za-z0-9_]+";
			final Pattern p = Pattern.compile(regex);
			String line = logLine[0];
			String regex3 = "^[^<]*<[^>]*>(.*)$";
			if (isOtherChannel) {
				regex3 = "^[^<]*<[^>]*>[^<]*<[^>]*>(.*)$";
			}
			final Pattern p3 = Pattern.compile(regex3);
			final Matcher m3 = p3.matcher(line);
			if (m3.find()) {
				line = m3.replaceAll("$1");
			}
			final Matcher m = p.matcher(line);
			m.reset();
			while (m.find()) {
				final String user = m.group();
				if (!aUserList.contains(user)) {
					aUserList.add(user);
					retCmdLine = retCmdLine + user + " ";
				}
			}
		}
		else if (cmd.equalsIgnoreCase("RT")) {
			retCmdLine = "rt " + logLine[1] + " ";
		}
		else if (cmd.equalsIgnoreCase("QT")) {
			retCmdLine = "qt " + logLine[1] + " ";
		}
		else if (cmd.equalsIgnoreCase("ORT")) {
			retCmdLine = SystemConfig.TIG_oldRTCmd + " " + logLine[1] + " ";
		}
		else if (cmd.equalsIgnoreCase("FAV")) {
			retCmdLine = "fav " + logLine[1] + " ";
		}
		else if (cmd.equalsIgnoreCase("U")) {
			retCmdLine = "u " + logLine[1] + " ";
		}

		return retCmdLine;
	}

	/**
	 * reコマンドを挿入
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 */
	public void doInsertReplyCommand(final String logLine[], final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doInsertReplyCommand()");
		final String cmd = getTIGCommand(
				logLine,
				"RE",
				isOtherChannel
		);
		if (cmd != null) {
			(me).runOnUiThread(new Runnable(){ public void run() {
				if (isOtherChannel) {
					doChangeChannelByLogLine(logLine);
				}
				EditAssist.insert(ActivityMain.layout.mInputBox, cmd);
			}});
		}
	}

	/**
	 * rtコマンドを挿入
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 */
	public void doInsertRetweetCommand(final String logLine[], final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doInsertRetweetCommand()");
		final String cmd = getTIGCommand(
				logLine,
				"RT",
				isOtherChannel
		);
		if (cmd != null) {
			(me).runOnUiThread(new Runnable(){ public void run() {
				if (isOtherChannel) {
					doChangeChannelByLogLine(logLine);
				}
				EditAssist.insert(ActivityMain.layout.mInputBox, cmd);
			}});
		}
	}

	/**
	 * qtコマンドを挿入
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 */
	public void doInsertQuotedTweetCommand(final String logLine[], final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doInsertQuotedTweetCommand()");
		final String cmd = getTIGCommand(
				logLine,
				"QT",
				isOtherChannel
		);
		if (cmd != null) {
			(me).runOnUiThread(new Runnable(){ public void run() {
				if (isOtherChannel) {
					doChangeChannelByLogLine(logLine);
				}
				EditAssist.insert(ActivityMain.layout.mInputBox, cmd);
			}});
		}
	}
	
	/**
	 * ortコマンドを挿入
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 */
	public void doInsertOldRetweetCommand(final String logLine[], final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doInsertOldRetweetCommand()");
		final String cmd = getTIGCommand(
				logLine,
				"ORT",
				isOtherChannel
		);
		if (cmd != null) {
			(me).runOnUiThread(new Runnable(){ public void run() {
				if (isOtherChannel) {
					doChangeChannelByLogLine(logLine);
				}
				EditAssist.insert(ActivityMain.layout.mInputBox, cmd);
			}});
		}
	}

	/**
	 * favコマンドを挿入
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 */
	public void doInsertFavoriteCommand(final String logLine[], final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doInsertFavoriteCommand()");
		final String cmd = getTIGCommand(
				logLine,
				"FAV",
				isOtherChannel
		);
		if (cmd != null) {
			(me).runOnUiThread(new Runnable(){ public void run() {
				if (isOtherChannel) {
					doChangeChannelByLogLine(logLine);
				}
				EditAssist.insert(ActivityMain.layout.mInputBox, cmd);
			}});
		}
	}

	/**
	 * tweetをwebで開くためのコマンドを送信
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルを対象にするときtrue
	 */
	public void doOpenTweetWeb(final String logLine[], final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doOpenTweetWeb()");
		final String cmd = getTIGCommand(
				logLine,
				"U",
				isOtherChannel
		);
		if (cmd != null) {
			(me).runOnUiThread(new Runnable(){ public void run() {
				if (isOtherChannel) {
					doChangeChannelByLogLine(logLine);
				}
				ActivityMain.sendQuietMessageToIRCService(cmd);
			}});
		}
	}

	/**
	 * TIG用ユーティリティメニューの表示
	 */
	public void doUtilityTIG() {
		if (I) Log.i(TAG, "doUtilityTIG()");
		// チャンネル確認
		boolean ok = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return;
		}
		final String fCurrentServerName = currentServerName;
		final String fCurrentChannel = currentChannel;

		// TIG用ユーティリティメニューの作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_reply), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_rt), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_qt), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_ort), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_favorite), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_opentweetweb), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_user), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_privuserfromlog), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_targetuserfromlog), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_openuserwebfromchannel), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_openuserwebfromlog), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_dmfromchannel), 1);
			dlg.addItem("*"+me.getString(R.string.activitymain_java_tigutility_cmd_dmfromlog), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_ircutility), 1);
			dlg.setTitle(me.getString(R.string.activitymain_java_tigutility_title));
			dlg.setLongClickEnable(true);
			dlg.create();
			ActivityMain.myShortToastShow2(me.getString(R.string.activitymain_java_tigutility_toast));
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				final boolean[] isLongClick = new boolean[1];
				isLongClick[0] = false;
				if (result >= AwaitAlertDialogList.LONGCLICKED) {
					isLongClick[0] = true;
					result -= AwaitAlertDialogList.LONGCLICKED;
				} else if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;
				case 0:  // cmd_reply
				{
					// reコマンドを挿入
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_tigutility_cmd_reply,
							isLongClick[0],
							false,
							0
					);
					doInsertReplyCommand(logLine, isLongClick[0]);
					break;
				}
				case 1:  // cmd_rt
				{
					// rtコマンドを挿入
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_tigutility_cmd_rt,
							isLongClick[0],
							false,
							0
					);
					doInsertRetweetCommand(logLine, isLongClick[0]);
					break;
				}
				case 2:  // cmd_qt
				{
					// qtコマンドを挿入
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_tigutility_cmd_qt,
							isLongClick[0],
							false,
							0
					);
					doInsertQuotedTweetCommand(logLine, isLongClick[0]);
					break;
				}
				case 3:  // cmd_ort
				{
					// ortコマンドを挿入
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_tigutility_cmd_ort,
							isLongClick[0],
							false,
							0
					);
					doInsertOldRetweetCommand(logLine, isLongClick[0]);
					break;
				}
				case 4:  // cmd_fav
				{
					// favコマンドを挿入
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_tigutility_cmd_favorite,
							isLongClick[0],
							false,
							0
					);
					doInsertFavoriteCommand(logLine, isLongClick[0]);
					break;
				}
				case 5:  // cmd_opentweetweb
				{
					// webでtweetを開く
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_tigutility_cmd_opentweetweb,
							isLongClick[0],
							false,
							0
					);
					doOpenTweetWeb(logLine, isLongClick[0]);
					break;
				}
				case 6:  // cmd_user
				{
					// 選択ユーザー名を挿入
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_insertfromuserlist_tiguser,
							true, true
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						(me).runOnUiThread(new Runnable(){ public void run() {
							for (int i=0; i<userList.length; i++) {
								EditAssist.insert(ActivityMain.layout.mInputBox, userList[i] + " ");
							}
						}});
					}
					break;
				}
				case 7:  // cmd_privuserfromlog
				{
					// ログ内の発言ユーザー名を挿入
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_insertfromuserlist_tiguserlog,
							isLongClick[0],
							false,
							0
					);
					final ArrayList<String> userList = extractUserListFromLogLine(logLine, false);
					if (userList.size() > 0) {
						(me).runOnUiThread(new Runnable(){ public void run() {
							if (isLongClick[0]) {
								doChangeChannelByLogLine(logLine);
							}
							EditAssist.insert(ActivityMain.layout.mInputBox, userList.get(0) + " ");
						}});
					}
					break;
				}
				case 8:  // cmd_targetuserfromlog
				{
					// ログ内の対象ユーザー名を挿入
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_insertfromuserlist_tiguserlog,
							isLongClick[0],
							false,
							0
					);
					final ArrayList<String> userList = extractUserListFromLogLine(logLine, false);
					if (userList.size() > 0) {
						(me).runOnUiThread(new Runnable(){ public void run() {
							if (isLongClick[0]) {
								doChangeChannelByLogLine(logLine);
							}
							for (int i=0; i<userList.size(); i++) {
								EditAssist.insert(ActivityMain.layout.mInputBox, userList.get(i) + " ");
							}
						}});
					}
					break;
				}
				case 9:  // cmd_openuserweb
				{
					// チャンネル内ユーザーのTwitterサイトを開く
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_openuserweb_tiguser,
							true, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						doOpenTwitterUserWeb(userList[0]);
					}
					break;
				}
				case 10:  // cmd_openuserwebfromlog
				{
					// ログ内ユーザーのTwitterサイトを開く
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_openuserweb_tiguserlog,
							isLongClick[0],
							false,
							0
					);
					final ArrayList<String> userList = extractUserListFromLogLine(logLine, false);
					if (userList.size() > 0) {
						doOpenTwitterUserWeb(userList.get(0));
					}
					break;
				}
				case 11:  // cmd_dmfromchannel
				{
					// チャンネル内ユーザーにDM
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_tigutility_ask_dmfromchannel,
							false, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						final String cmd = "/join " + userList[0];
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 12:  // cmd_dmfromlog
				{
					// ログ内ユーザーにDM
					final String[] logLine = selectTIGLogLine(
							R.string.activitymain_java_tigutility_ask_dmfromlog,
							isLongClick[0],
							false,
							0
					);
					if ((logLine.length >= 2) && (logLine[0] != null) && (logLine[1] != null)) {
						String currentServerName2 = fCurrentServerName;
						String currentChannel2 = fCurrentChannel;
						boolean ok2 = false;
						if (isLongClick[0]) {
							(me).runOnUiThread(new Runnable(){ public void run() {
								doChangeChannelByLogLine(logLine);
							}});
							try {
								currentServerName2 = iIRCService.getCurrentServerName();
								if (currentServerName2 != null) {
									currentChannel2 = iIRCService.getCurrentChannel(currentServerName2);
									if (currentChannel2 != null) {
										ok2 = true;
									}
								}
							} catch (final RemoteException e) {
								e.printStackTrace();
							}
						}
						if (ok2) {
							final ArrayList<String> userList = extractUserListFromLogLine(logLine, false);
							if (userList.size() > 0) {
								final String cmd = "/join " + userList.get(0);
								try {
									iIRCService.sendCommandLine(currentServerName2, currentChannel2, cmd);
								} catch (final RemoteException e) {
									e.printStackTrace();
								}
							}
						}
					}
					break;
				}
				case 13:  // cmd_ircutility
				{
					// IRCユーティリティの表示
					doLogWindowLongTapIRC(false);
					break;
				}

				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}

	/**
	 * TIG選択ログにアクション
	 * @param logLine TIGログ行[0]とTypableMap名[1]
	 * @param isOtherChannel Otherチャンネルから起動したときtrue
	 */
	public void doActionToTIGLog(final String[] logLine, final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doActionToTIGLog()");

		// チャンネル確認
		boolean ok = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return;
		}
		final String fCurrentServerName = currentServerName;
		final String fCurrentChannel = currentChannel;

		// TIG用選択ログアクションメニューの作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_reply), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_rt), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_qt), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_ort), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_favorite), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_opentweetweb), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_targetuser), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_openuserweb), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_dm), 1);
			// dlg.setTitle(me.getString(R.string.menu_main_logwindowlongtap_tiglog_title));
			dlg.setTitle(logLine[0]);
			final LayoutInflater inflate = me.getLayoutInflater();
			final TextView v = (TextView)inflate.inflate(R.layout.alertdialoglist_actiontotiglog, null);
			v.setText(logLine[0]);
			dlg.setCustomTitleView(v);
			dlg.setLongClickEnable(true);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;
				case 0:  // cmd_reply
				{
					// reコマンドを挿入
					doInsertReplyCommand(logLine, isOtherChannel);
					break;
				}
				case 1:  // cmd_rt
				{
					// rtコマンドを挿入
					doInsertRetweetCommand(logLine, isOtherChannel);
					break;
				}
				case 2:  // cmd_qt
				{
					// qtコマンドを挿入
					doInsertQuotedTweetCommand(logLine, isOtherChannel);
					break;
				}
				case 3:  // cmd_ort
				{
					// ortコマンドを挿入
					doInsertOldRetweetCommand(logLine, isOtherChannel);
					break;
				}
				case 4:  // cmd_fav
				{
					// favコマンドを挿入
					doInsertFavoriteCommand(logLine, isOtherChannel);
					break;
				}
				case 5:  // cmd_opentweetweb
				{
					// webでtweetを開く
					doOpenTweetWeb(logLine, isOtherChannel);
					break;
				}
				case 6:  // cmd_targetuser
				{
					// ログ内の対象ユーザー名を挿入
					final ArrayList<String> userList = extractUserListFromLogLine(logLine, isOtherChannel);
					if (userList.size() > 0) {
						(me).runOnUiThread(new Runnable(){ public void run() {
							for (int i=0; i<userList.size(); i++) {
								EditAssist.insert(ActivityMain.layout.mInputBox, userList.get(i) + " ");
							}
						}});
					}
					break;
				}
				case 7:  // cmd_openuserweb
				{
					// ユーザーのTwitterサイトを開く
					final ArrayList<String> userList = extractUserListFromLogLine(logLine, isOtherChannel);
					if (userList.size() > 0) {
						doOpenTwitterUserWeb(userList.get(0));
					}
					break;
				}
				case 8:  // cmd_dm
				{
					// ダイレクトメッセージ
					final ArrayList<String> userList = extractUserListFromLogLine(logLine, isOtherChannel);
					if (userList.size() > 0) {
						final String cmd = "/join " + userList.get(0);
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
		
	}

	/**
	 * Twitterユーザーwebの表示
	 * @param userName "@ユーザー名"
	 */
	public void doOpenTwitterUserWeb(final String userName) {
		if (I) Log.i(TAG, "doOpenTwitterUserWeb()");
		final String name = userName.substring(1);
		final String url = (SystemConfig.twitterSiteIsMobile ? "http://mobile.twitter.com/" : "http://twitter.com/") + name;
		ActivityMain.openWebPage(url);
	}

	/**
	 * AiCiAヘルプの表示
	 */
	public void doShowHelp() {
		if (I) Log.i(TAG, "doShowHelp()");
		final String url = "http://gorry.hauN.org/android/aicia/help/";
		ActivityMain.openWebPage(url);
	}

	/**
	 * ユーティリティの起動
	 * @param mode モード
	 */
	public void doUtility(final int mode) {
		if (I) Log.i(TAG, "doUtility()");
		if (ActivityMain.mCurrentServerName != null) {
			boolean isTIGMode = false;
			try {
				isTIGMode = iIRCService.isTIGMode(ActivityMain.mCurrentServerName);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			if (isTIGMode) {
				if (mode >= 1) {
					doUtilityIRC();
				} else {
					doUtilityTIG();
				}
				return;
			}
			if (mode >= 1) {
				doInsertIRCUser();
			} else {
				doUtilityIRC();
			}
		}
	}

	/**
	 * スクリーンオブジェクトのショートカット処理
	 * @param keyCode キーコード
	 * @param event イベント
	 * @return キー処理を行ったらtrue
	 */
	public boolean doShortcutKey(final int keyCode, final KeyEvent event) {
		if (I) Log.i(TAG, "doShortcutKey()");
		if (event.isAltPressed()) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				if (event.isShiftPressed()) {
					doUtility(1);
				} else {
					doUtility(0);
				}
				return true;

			case KeyEvent.KEYCODE_SEARCH:
				if (event.isShiftPressed()) {
					if (SystemConfig.cmdLongPressChannelToNotified) {
						doSelectAlertedChannel();
					} else {
						doSelectUpdatedChannel();
					}
				} else {
					doSelectChannel();
				}
				return true;

			case KeyEvent.KEYCODE_DPAD_UP:
				if (event.isShiftPressed()) {
					final boolean changed = doChangeNextChannel(-1, 1, false);
					if (!changed) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notupdated));
					}
				} else {
					final boolean changed = doChangeNextChannel(-1, 0, false);
					if (!changed) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notchanged));
					}
				}
				ActivityMain.layout.mInputBox.requestFocus();
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if (event.isShiftPressed()) {
					final boolean changed = doChangeNextChannel(1, 1, false);
					if (!changed) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notupdated));
					}
				} else {
					final boolean changed = doChangeNextChannel(1, 0, false);
					if (!changed) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notchanged));
					}
				}
				ActivityMain.layout.mInputBox.requestFocus();
				return true;
			}
			if (EditAssist.cutCopyPaste(me, ActivityMain.layout.mInputBox, keyCode, event)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 文字列の入力
	 * @param titleResource ダイアログタイトル
	 * @param defaultParam 文字列の初期値
	 * @param comment ダイアログに表示するコメント
	 * @return 入力された文字列
	 */
	public String getDialogString(final int titleResource, final String defaultParam, final String comment) {
		if (I) Log.i(TAG, "getDialogString()");
		final Object o = new Object();
		final String[] resultParam = new String[1];

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogString dlg = new AwaitAlertDialogString(me);
			dlg.setTitle(me.getString(titleResource));
			dlg.setDefaultParam(defaultParam);
			if (comment != null) {
				dlg.setComment(comment);
			}
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// 文字列を入力
				final int result = dlg.show();
				switch (result) {
				case AwaitAlertDialogBase.OK:
				{
					resultParam[0] = dlg.getResultParam();
					break;
				}
				case AwaitAlertDialogBase.CANCEL:
				default:
					break;
				}
				synchronized (o) {
					o.notifyAll();
				}
			} }).start();
		} });
		synchronized (o) {
			try {
				o.wait();
			} catch (final InterruptedException e) {
				//
			}
		}
		if (resultParam[0] == null) {
			return null;
		}
		return resultParam[0];
	}

	/**
	 * IRC用ユーティリティメニューの表示
	 */
	public void doUtilityIRC() {
		if (I) Log.i(TAG, "doUtilityIRC()");
		// チャンネル確認
		boolean ok = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return;
		}
		final String fCurrentServerName = currentServerName;
		final String fCurrentChannel = currentChannel;

		// IRC用ユーティリティメニューの作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_topic), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_nick), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_join), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_invite), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_kick), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_part), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_incop), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_decop), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_oper), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_talk), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_whois), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_raw), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_config_channel), 1);
			dlg.setTitle(me.getString(R.string.activitymain_java_ircutility_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show();
				switch (result-AwaitAlertDialogList.CLICKED) {
				default:
					break;
				case 0:  // cmd_topic
				{
					// /topicコマンドを実行
					final String topic = getDialogString(
							R.string.activitymain_java_ircutility_ask_newtopic,
							"",
							null
					);
					if (topic != null) {
						final String cmd = "/topic " + topic;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 1:  // cmd_nick
				{
					// /nickコマンドを実行
					final String nick = getDialogString(
							R.string.activitymain_java_ircutility_ask_newnick,
							"",
							null
					);
					if (nick != null) {
						final String cmd = "/nick " + nick;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 2:  // cmd_join
				{
					// /joinコマンドを実行
					final String joinch = getDialogString(
							R.string.activitymain_java_ircutility_ask_joinchannel,
							"",
							me.getString(R.string.activitymain_java_ircutility_ask_joinchannel_comment)
					);
					if (joinch != null) {
						joinch.replaceAll("[ ]+", "");
						final String cmd = "/join " + joinch;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 3:  // cmd_invite
				{
					// /inviteコマンドを実行
					final String user = getDialogString(
							R.string.activitymain_java_ircutility_ask_inviteuser,
							"",
							null
					);
					if (user != null) {
						final String cmd = "/invite " + user;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 4:  // cmd_kick
				{
					// /kickコマンドを実行
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_ircutility_ask_kickuser,
							false, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						final String reason = getDialogString(
								R.string.activitymain_java_ircutility_ask_kickreason,
								"",
								null
						);
						if (reason != null) {
							final String cmd = "/kick " + fCurrentChannel + " " + userList[0] + " " + reason;
							try {
								iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
							} catch (final RemoteException e) {
								e.printStackTrace();
							}
						}
					}
					break;
				}
				case 5:  // cmd_part
				{
					// /partコマンドを実行
					final String partmsg = getDialogString(
							R.string.activitymain_java_ircutility_ask_partmsg,
							"",
							null
					);
					if (partmsg != null) {
						final String cmd = "/part " + fCurrentChannel + " " + partmsg;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 6:  // cmd_incop
				{
					// +Oコマンドを実行
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_ircutility_ask_incopuser,
							false, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						final String cmd = "/mode " + " +o " + userList[0];
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 7:  // cmd_decop
				{
					// -Oコマンドを実行
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_ircutility_ask_decopuser,
							false, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						final String cmd = "/mode " + " -o " + userList[0];
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 8:  // cmd_oper
				{
					// OPERコマンドを実行
					final String user = getDialogString(
							R.string.activitymain_java_ircutility_ask_operuser,
							"",
							null
					);
					if ((user != null) && (user.length() >= 1)) {
						final String passwd = getDialogString(
								R.string.activitymain_java_ircutility_ask_operpass,
								"",
								null
						);
						final String cmd = "/oper " + user + " " + passwd;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 9:  // cmd_talk
				{
					// TALKコマンドを実行
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_ircutility_ask_talkuser,
							false, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						final String cmd = "/join " + userList[0];
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 10:  // cmd_whois
				{
					// /whoisコマンドを実行
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_ircutility_ask_whoisuser,
							false, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						final String cmd = "/whois " + userList[0];
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 11:  // cmd_raw
				{
					// /rawコマンドを実行
					final String rawcmd = getDialogString(
							R.string.activitymain_java_ircutility_ask_raw,
							"",
							null
					);
					if (rawcmd != null) {
						final String cmd = "/raw " + rawcmd;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 12:  // cmd_config_channel
				{
					// チャンネル設定
					doChannelConfig();
					break;
				}
				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}

	/**
	 * 外部プログラムの起動
	 */
	public void doExApp() {
		if (I) Log.i(TAG, "doExApp()");

		// 外部プログラムメニューの作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogIconList dlg = new AwaitAlertDialogIconList(me);
			int n;
			for (n=SystemConfig.maxExApp-1; n>=1; n--) {
				final String appName = SystemConfig.exAppName[n];
				final String packageName = SystemConfig.exAppPackageName[n];
				final String activityName = SystemConfig.exAppActivityName[n];
				if ((appName != null) && (appName.length() > 0) && (packageName != null) && (packageName.length() > 0) && (activityName != null) && (activityName.length() > 0)) {
					break;
				}
			}
			for (int i=0; i<=n; i++) {
				final String appName = SystemConfig.exAppName[i];
				final String packageName = SystemConfig.exAppPackageName[i];
				final String activityName = SystemConfig.exAppActivityName[i];
				boolean appAssigned = false;
				boolean appFound = false;
				if ((appName != null) && (appName.length() > 0) && (packageName != null) && (packageName.length() > 0) && (activityName != null) && (activityName.length() > 0)) {
					appAssigned = true;
					final ResolveInfo info = MyAppInfo.createResolveInfo(packageName, activityName);
					if (info != null) {
						final Drawable icon = MyAppInfo.getIcon(info);
						if (icon != null) {
							dlg.addItem(appName, icon, 1, true);
							appFound = true;
						}
					}
				}
				if (!appAssigned) {
					dlg.addItem(me.getString(R.string.activitymain_java_exapp_notassigned), null, 1, false);
				} else if (!appFound) {
					dlg.addItem(appName + " " + me.getString(R.string.activitymain_java_exapp_notfound), null, 1, false);
				}
			}
			dlg.setTitle(me.getString(R.string.activitymain_java_exapp_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show();
				boolean ok = false;
				if (result != AwaitAlertDialogBase.CANCEL) {
					final int id = result - AwaitAlertDialogList.CLICKED;
					if ((0 <= id) && (id < SystemConfig.maxExApp)) {
						final String appName = SystemConfig.exAppName[id];
						final String packageName = SystemConfig.exAppPackageName[id];
						final String activityName = SystemConfig.exAppActivityName[id];
						if ((appName != null) && (appName.length() > 0) && (packageName != null) && (packageName.length() > 0) && (activityName != null) && (activityName.length() > 0)) {
							try {
								final Intent intent = new Intent(Intent.ACTION_MAIN);
								intent.setClassName(packageName, activityName);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								me.startActivity(intent);
								ok = true;
							} catch (final ActivityNotFoundException e) {
								e.printStackTrace();
							}
						}
					}
					if (!ok) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_exapp_noreg));
					}
				}
			} }).start();
		}});
	}

	/**
	 * 外部プログラムの設定
	 */
	public void doExAppConfig() {
		if (I) Log.i(TAG, "doExAppConfig()");

		final Intent intent = new Intent(
				me,
				ActivityExAppList.class
		);
		final boolean isLandscape = SystemConfig.getOrientation();
		intent.putExtra("islandscape", isLandscape);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_EXAPPLIST);
	}


	/**
	 * ブラウザでウェブサイトを開く
	 */
	public void doExWeb() {
		if (I) Log.i(TAG, "doExWeb()");

		// ウェブサイトメニューの作成
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			int n;
			for (n=SystemConfig.maxExWebSite-1; n>=1; n--) {
				final String webSiteName = SystemConfig.exWebSiteName[n];
				final String webSiteUrl = SystemConfig.exWebSiteUrl[n];
				if ((webSiteName != null) && (webSiteName.length() > 0) && (webSiteUrl != null) && (webSiteUrl.length() > 0) ) {
					break;
				}
			}
			for (int i=0; i<=n; i++) {
				final String webSiteName = SystemConfig.exWebSiteName[i];
				final String webSiteUrl = SystemConfig.exWebSiteUrl[i];
				if ((webSiteName != null) && (webSiteName.length() > 0) && (webSiteUrl != null) && (webSiteUrl.length() > 0) ) {
					dlg.addItem(webSiteName, 1, true);
				} else {
					dlg.addItem(me.getString(R.string.activitymain_java_exweb_none), 1, false);
				}
			}
			dlg.setTitle(me.getString(R.string.activitymain_java_exweb_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				final int result = dlg.show();
				boolean ok = false;
				if (result != AwaitAlertDialogBase.CANCEL) {
					final int id = result - AwaitAlertDialogList.CLICKED;
					if ((0 <= id) && (id < SystemConfig.maxExWebSite)) {
						final String webSiteName = SystemConfig.exWebSiteName[id];
						final String webSiteUrl = SystemConfig.exWebSiteUrl[id];
						if ((webSiteName != null) && (webSiteName.length() > 0) && (webSiteUrl != null) && (webSiteUrl.length() > 0)) {
							ActivityMain.openWebPage(webSiteUrl);
							ok = true;
						}
					}
					if (!ok) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_exweb_noreg));
					}
				}
			} }).start();
		}});
	}

	/**
	 * ウェブサイトの設定
	 */
	public void doExWebConfig() {
		if (I) Log.i(TAG, "doExWebSiteConfig()");

		final Intent intent = new Intent(
				me,
				ActivityExWebList.class
		);
		final boolean isLandscape = SystemConfig.getOrientation();
		intent.putExtra("islandscape", isLandscape);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_EXWEBLIST);
	}

	/**
	 * [Ch]ボタンメニュー
	 */
	public void doChButton() {
		if (I) Log.i(TAG, "doChButton()");

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_chbutton_1), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_chbutton_2), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_chbutton_3), 1);
			dlg.setTitle(me.getString(R.string.menu_main_chbutton_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;

				case 0:
					doSelectChannel();
					break;

				case 1:
					doSelectUpdatedChannel();
					break;

				case 2:
					doSelectAlertedChannel();
					break;
				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}

	/**
	 * [U]ボタンメニュー
	 */
	public void doUButton() {
		if (I) Log.i(TAG, "doUButton()");

		if (ActivityMain.mCurrentServerName != null) {
			boolean isTIGMode = false;
			try {
				isTIGMode = iIRCService.isTIGMode(ActivityMain.mCurrentServerName);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			if (!isTIGMode) {
				doUtilityIRC();
				return;
			}
		}

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_ubutton_tig_1), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ubutton_tig_2), 1);
			dlg.setTitle(me.getString(R.string.menu_main_ubutton_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;

				case 0:
					doUtilityTIG();
					break;

				case 1:
					doUtilityIRC();
					break;
				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}

	/**
	 * コピーボタンメニュー
	 */
	public void doCopyButton() {
		if (I) Log.i(TAG, "doCopyButton()");

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_copybutton_1), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_copybutton_2), 1);
			dlg.setTitle(me.getString(R.string.menu_main_copybutton_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;

				case 0:
					(me).runOnUiThread(new Runnable(){ public void run() {
						ActivityMain.layout.copyFromMainLogWindow();
					}});
					break;

				case 1:
					(me).runOnUiThread(new Runnable(){ public void run() {
						ActivityMain.layout.copyFromSubLogWindow();
					}});
					break;
				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}
	
	/**
	 * 選択ユーザーにアクション（IRC）
	 * @param user ユーザー名
	 */
	public void doActionToIRCUser(final String user) {
		if (I) Log.i(TAG, "doActionToIRCUser()");

		// チャンネル確認
		boolean ok = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return;
		}
		final String fCurrentServerName = currentServerName;
		final String fCurrentChannel = currentChannel;

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_talk), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_whois), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_incop), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_decop), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_oper), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_kick), 1);
			final String title1 = me.getString(R.string.menu_main_logwindowlongtap_irc_user_to_title_1);
			final String title2 = me.getString(R.string.menu_main_logwindowlongtap_irc_user_to_title_2);
			dlg.setTitle(title1 + user + title2);
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;
				case 0:  // cmd_talk
				{
					// TALKコマンドを実行
					final String cmd = "/join " + user;
					try {
						iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
					break;
				}
				case 1:  // cmd_whois
				{
					// /whoisコマンドを実行
					final String cmd = "/whois " + user;
					try {
						iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
					break;
				}
				case 2:  // cmd_incop
				{
					// +Oコマンドを実行
					final String cmd = "/mode " + " +o " + user;
					try {
						iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
					break;
				}
				case 3:  // cmd_decop
				{
					// -Oコマンドを実行
					final String cmd = "/mode " + " -o " + user;
					try {
						iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
					break;
				}
				case 4:  // cmd_oper
				{
					// OPERコマンドを実行
					final String passwd = getDialogString(
							R.string.activitymain_java_ircutility_ask_operpass,
							"",
							null
					);
					final String cmd = "/oper " + user + " " + passwd;
					try {
						iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
					break;
				}
				case 5:  // cmd_kick
				{
					// /kickコマンドを実行
					final String reason = getDialogString(
							R.string.activitymain_java_ircutility_ask_kickreason,
							"",
							null
					);
					if (reason != null) {
						final String cmd = "/kick " + fCurrentChannel + " " + user + " " + reason;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}

				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}
	
	/**
	 * ログウィンドウロングタップメニュー（IRC）
	 * @param isOtherChannel Otherチャンネルから起動したときtrue
	 */
	public void doLogWindowLongTapIRC(final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doLogWindowLongTapIRC()");

		// チャンネル確認
		boolean ok = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return;
		}
		final String fCurrentServerName = currentServerName;
		final String fCurrentChannel = currentChannel;

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_select_channel), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_select_user), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_copylog), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_topic), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_nick), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_join), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_invite), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_part), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_raw), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_config_channel), 1);
			dlg.setTitle(me.getString(R.string.menu_main_logwindowlongtap_irc_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				boolean isLongClick = false;
				if (result >= AwaitAlertDialogList.LONGCLICKED) {
					isLongClick = true;
					result -= AwaitAlertDialogList.LONGCLICKED;
				} else if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;

				case 0:  // cmd_select_channel
				{
					// チャンネルを選択
					if (isLongClick) {
						if (SystemConfig.cmdLongPressChannelToNotified) {
							doSelectAlertedChannel();
						} else {
							doSelectUpdatedChannel();
						}
					} else {
						doSelectChannel();
					}
					break;
				}
				case 1:  // select_user
				{
					// ユーザー名を選択
					final String[] userList = getUserListFromCurrentChannel(
							R.string.activitymain_java_ircutility_ask_selectuser,
							false, false
					);
					if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
						doActionToIRCUser(userList[0]);
					}
					break;
				}
				case 2:  // cmd_copylog
				{
					// ログのコピー
					(me).runOnUiThread(new Runnable(){ public void run() {
						if (isOtherChannel) {
							ActivityMain.layout.copyFromSubLogWindow();
						} else {
							ActivityMain.layout.copyFromMainLogWindow();
						}
					}});
					break;
				}
				case 3:  // cmd_topic
				{
					// /topicコマンドを実行
					final String topic = getDialogString(
							R.string.activitymain_java_ircutility_ask_newtopic,
							"",
							null
					);
					if (topic != null) {
						final String cmd = "/topic " + topic;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 4:  // cmd_nick
				{
					// /nickコマンドを実行
					final String nick = getDialogString(
							R.string.activitymain_java_ircutility_ask_newnick,
							"",
							null
					);
					if (nick != null) {
						final String cmd = "/nick " + nick;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 5:  // cmd_join
				{
					// /joinコマンドを実行
					final String joinch = getDialogString(
							R.string.activitymain_java_ircutility_ask_joinchannel,
							"",
							me.getString(R.string.activitymain_java_ircutility_ask_joinchannel_comment)
					);
					if (joinch != null) {
						joinch.replaceAll("[ ]+", "");
						final String cmd = "/join " + joinch;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 6:  // cmd_invite
				{
					// /inviteコマンドを実行
					final String user = getDialogString(
							R.string.activitymain_java_ircutility_ask_inviteuser,
							"",
							null
					);
					if (user != null) {
						final String cmd = "/invite " + user;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 7:  // cmd_part
				{
					// /partコマンドを実行
					final String partmsg = getDialogString(
							R.string.activitymain_java_ircutility_ask_partmsg,
							"",
							null
					);
					if (partmsg != null) {
						final String cmd = "/part " + fCurrentChannel + " " + partmsg;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 8:  // cmd_raw
				{
					// /rawコマンドを実行
					final String rawcmd = getDialogString(
							R.string.activitymain_java_ircutility_ask_raw,
							"",
							null
					);
					if (rawcmd != null) {
						final String cmd = "/raw " + rawcmd;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 9:  // cmd_config_channel
				{
					// チャンネル設定
					doChannelConfig();
					break;
				}

				}

				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}

	/**
	 * 選択ユーザーにアクション（TIG）
	 * @param user ユーザー名("@"なし)
	 */
	public void doActionToTIGUser(final String user) {
		if (I) Log.i(TAG, "doActionToTIGUser()");

		// チャンネル確認
		boolean ok = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return;
		}
		final String fCurrentServerName = currentServerName;
		final String fCurrentChannel = currentChannel;

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_dm), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_targetuser), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_openuserweb), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_ircutility_cmd_kick), 1);
			final String title1 = me.getString(R.string.menu_main_logwindowlongtap_tig_user_to_title_1);
			final String title2 = me.getString(R.string.menu_main_logwindowlongtap_tig_user_to_title_2);
			dlg.setTitle(title1 + user + title2);
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;
				case 0:  // cmd_talk
				{
					// ダイレクトメッセージ
					final String cmd = "/join " + user;
					try {
						iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
					break;
				}
				case 1:  // cmd_targetuser
				{
					// ユーザー名を挿入
					(me).runOnUiThread(new Runnable(){ public void run() {
						EditAssist.insert(ActivityMain.layout.mInputBox, "@" + user + " ");
					}});
					break;
				}
				case 2:  // cmd_openuserweb
				{
					// ユーザーのTwitterサイトを開く
					doOpenTwitterUserWeb("@" + user);
					break;
				}
				case 3:  // cmd_kick
				{
					// /kickコマンドを実行
					final String reason = getDialogString(
							R.string.activitymain_java_ircutility_ask_kickreason,
							"",
							null
					);
					if (reason != null) {
						final String cmd = "/kick " + fCurrentChannel + " " + user + " " + reason;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}

				}
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}
	
	/**
	 * ログウィンドウロングタップメニュー（TIG）
	 * @param isOtherChannel Otherチャンネルから起動したときtrue
	 */
	public void doLogWindowLongTapTIG(final boolean isOtherChannel) {
		if (I) Log.i(TAG, "doLogWindowLongTapTIG()");

		// チャンネル確認
		boolean ok = false;
		final boolean[] isTIG = new boolean[1];
		isTIG[0] = false;
		String currentServerName = null;
		String currentChannel = null;
		try {
			currentServerName = iIRCService.getCurrentServerName();
			if (currentServerName != null) {
				currentChannel = iIRCService.getCurrentChannel(currentServerName);
				if (currentChannel != null) {
					ok = true;
				}
			}
			isTIG[0] = iIRCService.isTIGMode(currentServerName);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		if (!ok) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_nochannel));
			return;
		}
		final String fCurrentServerName = currentServerName;
		final String fCurrentChannel = currentChannel;

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_select_channel), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_select_log), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_select_channeluser), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_copylog), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_invite), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_config_channel), 1);
			dlg.addItem(me.getString(R.string.activitymain_java_tigutility_cmd_ircutility), 1);
			dlg.setTitle(me.getString(R.string.menu_main_logwindowlongtap_tig_title));
			dlg.setLongClickEnable(true);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				boolean isLongClick = false;
				if (result >= AwaitAlertDialogList.LONGCLICKED) {
					isLongClick = true;
					result -= AwaitAlertDialogList.LONGCLICKED;
				} else if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				switch (result) {
				default:
					break;

				case 0:  // cmd_select_channel
				{
					// チャンネルを選択
					if (isLongClick) {
						if (SystemConfig.cmdLongPressChannelToNotified) {
							doSelectAlertedChannel();
						} else {
							doSelectUpdatedChannel();
						}
					} else {
						doSelectChannel();
					}
					break;
				}
				case 1:  // cmd_select_log
				{
					// ログを選択
					final String[] logLine;
					if (isOtherChannel) {
						logLine = selectIRCLogLine(
								R.string.activitymain_java_tigutility_ask_select_log,
								isOtherChannel,
								true,
								false,
								R.string.activitymain_java_tigutility_sublog_toast
						);
					} else {
						logLine = selectTIGLogLine(
								R.string.activitymain_java_tigutility_ask_select_log,
								isOtherChannel,
								true,
								R.string.activitymain_java_tigutility_mainlog_toast
						);
					}
					if ((logLine.length >= 3) && (logLine[0] != null) && (logLine[1] != null)) {
						(me).runOnUiThread(new Runnable(){ public void run() {
							if (isOtherChannel) {
								doChangeChannelByLogLine(logLine);
							}
						}});
						if (!logLine[1].equals("")) {
							if (isOtherChannel) {
								if (!logLine[2].equalsIgnoreCase("true")) {  // !長押し
									doActionToTIGLog(logLine, isOtherChannel);
								}
								break;
							}
							if (!logLine[2].equalsIgnoreCase("true")) {  // !長押し
								// reコマンドを挿入
								doActionToTIGLog(logLine, isOtherChannel);
							} else {
								doInsertReplyCommand(logLine, isOtherChannel);
							}
						}
					}
					break;
				}
				case 2:  // cmd_select_channeluser
				{
					// ユーザー名を選択
					if (isTIG[0]) {
						final String[] userList = getUserListFromCurrentChannel(
								R.string.activitymain_java_tigutility_ask_select_channeluser,
								true, false
						);
						if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
							doActionToTIGUser(userList[0].substring(1));
						}
					} else {
						final String[] userList = getUserListFromCurrentChannel(
								R.string.activitymain_java_ircutility_ask_selectuser,
								false, false
						);
						if ((userList != null) && (userList.length >= 1) && (userList[0] != null) && (userList[0].length() >= 1)) {
							doActionToIRCUser(userList[0]);
						}
					}
					break;
				}
				case 3:  // cmd_copylog
				{
					// ログのコピー
					(me).runOnUiThread(new Runnable(){ public void run() {
						if (isOtherChannel) {
							ActivityMain.layout.copyFromSubLogWindow();
						} else {
							ActivityMain.layout.copyFromMainLogWindow();
						}
					}});
					break;
				}
				case 4:  // cmd_invite
				{
					// /inviteコマンドを実行
					final String user = getDialogString(
							R.string.activitymain_java_ircutility_ask_inviteuser,
							"",
							null
					);
					if (user != null) {
						final String cmd = "/invite " + user;
						try {
							iIRCService.sendCommandLine(fCurrentServerName, fCurrentChannel, cmd);
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				case 5:  // cmd_config_channel
				{
					// チャンネル設定
					doChannelConfig();
					break;
				}
				case 6:  // cmd_ircutility
				{
					// IRCユーティリティの表示
					doLogWindowLongTapIRC(false);
					break;
				}

				}
				
				(me).runOnUiThread(new Runnable(){ public void run() {
					ActivityMain.layout.mInputBox.requestFocus();
				}});
			} }).start();
		}});
	}

	/**
	 * メインログウィンドウロングタップ
	 */
	public void doMainLogWindowLongTap() {
		if (I) Log.i(TAG, "doMainLogWindowLongTap()");

		boolean isTIGMode = false;
		if (ActivityMain.mCurrentServerName != null) {
			try {
				isTIGMode = iIRCService.isTIGMode(ActivityMain.mCurrentServerName);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		}

		if (!isTIGMode) {
			doLogWindowLongTapIRC(false);
			return;
		}
		doLogWindowLongTapTIG(false);
	}

	/**
	 * サブログウィンドウロングタップ
	 */
	public void doSubLogWindowLongTap() {
		if (I) Log.i(TAG, "doSubLogWindowLongTap()");

		boolean isTIGMode = false;
		try {
			isTIGMode = iIRCService.haveTIGModeInConnectedServers();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (!isTIGMode) {
			doLogWindowLongTapIRC(true);
			return;
		}
		doLogWindowLongTapTIG(true);
	}

	/**
	 * 入力履歴メニュー
	 */
	public void doInputHistoryMenu() {
		if (I) Log.i(TAG, "doInputHistoryMenu()");

		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			int pos = 0;
			String his;
			try {
				while ((his = iIRCService.getInputHistory(pos)) != null) {
					dlg.addItem(his, 1);
					pos++;
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			dlg.setTitle(me.getString(R.string.activitymain_java_inputhistory_title));
			dlg.setLongClickEnable(false);
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				int result = dlg.show();
				if (result >= AwaitAlertDialogList.CLICKED) {
					result -= AwaitAlertDialogList.CLICKED;
				} else {
					result = AwaitAlertDialogBase.CANCEL;
				}
				if (result >= 0) {
					final int pos2 = result;
					(me).runOnUiThread(new Runnable(){ public void run() {
						try {
							final String inp = ActivityMain.layout.mInputBox.getText().toString();
							final String his2 = iIRCService.getInputHistory(pos2);
							EditAssist.set(ActivityMain.layout.mInputBox, his2);
							iIRCService.pushInputHistory(inp);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}});
				}
			} }).start();
		}});
	}

	/**
	 * 入力履歴へ挿入
	 */
	public void doPushInputHistory() {
		if (I) Log.i(TAG, "doPushInputHistory()");
		final String inp = ActivityMain.layout.mInputBox.getText().toString();
		try {
			iIRCService.pushInputHistory(inp);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		EditAssist.set(ActivityMain.layout.mInputBox, "");
	}

	/**
	 * 通知のクリア
	 */
	public void doClearNotify() {
		if (I) Log.i(TAG, "doClearNotify()");
		try {
			iIRCService.clearNotify();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
