/**
 * 
 * AiCiA - Android IRC Client
 * 
 * Copyright (C)2010 GORRY.
 * 
 */

package net.gorry.aicia;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import net.gorry.libaicia.R;

/**
 * サーバリストの選択処理
 * 
 * @author GORRY
 *
 */
public class ActivityIRCServerListConfig extends Activity {
	private static final String TAG = "ActivityIRCServerListConfig";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = true;
	//	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;

	private static final int ACTIVITY_IRCSERVERCONFIG = 1;

	private IIRCService iIRCService = null;

	private String[] mServerList;
	private int mNumServerList;
	private LinearLayout mLinearLayout;
	private ListView mListView;

	private IRCServerList mIrcServerList;
	private IRCServerConfig mConfig;
	private boolean mAddServer = false;
	private int mServerId = 0;
	private String mServerName;

	private Activity me;

	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle icicle) {
		if (I) Log.i(TAG, "onCreate()");
		super.onCreate(icicle);
		me = this;
		setTitle(R.string.activityircserverlist_java_chooseserver);

		iIRCService = ActivityMain.getIIRCService();

		createServerListView();

		mLinearLayout = new LinearLayout(me);
		mLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP));
		{
			mLinearLayout.addView(mListView);
		}

		setContentView(mLinearLayout);

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

	/* 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public synchronized void onResume() {
		if (I) Log.i(TAG, "onResume()");
		super.onResume();
	}

	/* 中断
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		if (I) Log.i(TAG, "onPause()");
		super.onPause();
	}

	/*
	 * 停止
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		if (I) Log.i(TAG, "onStop()");
		super.onStop();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (I) Log.i(TAG, "onDestroy()");
		super.onDestroy();
		if (mIrcServerList != null) {
			mIrcServerList = null;
		}
	}

	/**
	 * サーバリストビューの作成
	 */
	private void createServerListView() {
		mIrcServerList = null;
		mIrcServerList = new IRCServerList(me, false);
		mIrcServerList.reloadList();
		mServerList = null;
		mServerList = mIrcServerList.getServerList();
		mNumServerList = mServerList.length;
		final ArrayList<String> serverListArray = new ArrayList<String>();
		for (int i=0; i<mNumServerList; i++) {
			serverListArray.add(mServerList[i]);
		}
		serverListArray.add("[" + getString(R.string.activityircserverlist_java_addnewserver) + "]"); // 常に最後

		mListView = new ListView(me);
		{
			mListView.setLayoutParams(new LinearLayout.LayoutParams(FP, FP));
			final ArrayAdapter<String> arrayAdapter =	new ArrayAdapter<String>(
					me,
					R.layout.ircserverlist_rowdata,
					serverListArray.toArray(new String[serverListArray.size()])
			);
			mListView.setAdapter(arrayAdapter);
			mListView.setOnItemClickListener(new MyClickAdapter());
			mListView.setOnItemLongClickListener(new MyLongClickAdapter());
		}
	}

	/*
	 * アクティビティの結果処理
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case ACTIVITY_IRCSERVERCONFIG:
				if (resultCode == RESULT_OK) {
					try {
						if (mAddServer) {
							mConfig = new IRCServerConfig(me);
							mConfig.loadConfig(mServerId);
							mIrcServerList.addServer(mConfig);
							mConfig = null;
							iIRCService.addNewServer();
						} else {
							mIrcServerList.reloadServerConfig(mServerId);
							iIRCService.reloadServerConfig(mServerId);
						}
						mLinearLayout.removeView(mListView);
						mListView = null;
						createServerListView();
						mLinearLayout.addView(mListView);
						final Toast toast = Toast.makeText(getApplicationContext(), R.string.activityircserverlist_java_savechanged, Toast.LENGTH_SHORT);
						toast.show();
					}
					catch(final Exception e) {
						alertUpdateServerList();
					}
				}
		}

	}

	/**
	 * サーバリストの更新エラー表示
	 */
	private void alertUpdateServerList() {
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogOk dlg = new AwaitAlertDialogOk(me);
			dlg.setTitle("ERROR");
			if (mAddServer) {
				dlg.setMessage(getString(R.string.activityircserverlist_java_faildaddnewserver));
			} else {
				dlg.setMessage(getString(R.string.activityircserverlist_java_faildeditnewserver));
			}
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				dlg.show();
			} }).start();
		} });
	}

	/**
	 * サーバリストアイテムのクリック時の処理
	 */
	class MyClickAdapter implements OnItemClickListener {
		public void onItemClick(final AdapterView<?> adapter, final View view, final int position, final long id) {
			final TextView tv = (TextView)view;
			mServerId = position;
			mServerName = mIrcServerList.getServerName(mServerId);
			mAddServer = false;
			if (mServerId >= mNumServerList) {
				mServerId = mNumServerList;  // [Add new server]を選択
				mAddServer = true;
				startIRCServerConfigActivity();
				return;
			}

			(me).runOnUiThread(new Runnable(){ public void run() {
				final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
				dlg.setTitle(tv.getText().toString());
				dlg.addItem(me.getString(R.string.activityircserverlist_java_item_edit), 1);
				dlg.addItem(me.getString(R.string.activityircserverlist_java_item_connect), 1);
				dlg.addItem(me.getString(R.string.activityircserverlist_java_item_disconnect), 1);
				dlg.addItem(me.getString(R.string.activityircserverlist_java_item_delete), 1);
				dlg.create();
				new Thread(new Runnable() {	public void run() {
					// 選択したサーバ設定のコンテキストメニューを表示する
					final int result = dlg.show();
					switch (result-AwaitAlertDialogList.CLICKED) {
						case 0:  // Edit
							mAddServer = false;
							if (mServerId >= mNumServerList) {
								break;
							}
							startIRCServerConfigActivity();
							break;

						case 1:  // Connect
							(me).runOnUiThread(new Runnable(){ public void run() {
								if (I) Log.i(TAG, "connectServer(): serverName="+mServerName);
								final Intent intent = new Intent();
								intent.putExtra("disconnect", 0);
								intent.putExtra("connect", 1);
								intent.putExtra("server", mServerName);
								setResult(RESULT_OK, intent);
								finish();
							} });
							break;

						case 2:  // Disconnect
							doDisconnectServer();
							break;

						case 3:  // Delete
							doDeleteServer();
							break;
					}
				} }).start();
			} });
		}
	}

	/**
	 * IRCServerConfigActivityの起動
	 */
	private void startIRCServerConfigActivity() {
		final Intent intent = new Intent(
				ActivityIRCServerListConfig.this,
				ActivityIRCServerConfig.class
		);
		intent.setPackage(getPackageName());
		intent.putExtra("serverid", mServerId);
		intent.putExtra("addserver", mAddServer);
		startActivityForResult(intent, ACTIVITY_IRCSERVERCONFIG);
	}

	/**
	 * サーバリストアイテムの長押し時の処理
	 */
	class MyLongClickAdapter implements OnItemLongClickListener {
		public boolean onItemLongClick(final AdapterView<?> adapter, final View view, final int position, final long id) {
			mServerId = position;
			mServerName = mIrcServerList.getServerName(mServerId);
			mAddServer = false;
			if (mServerId >= mNumServerList) {
				return true;  // [Add new server]の長押しは無効
			}
			startIRCServerConfigActivity();
			return true;
		}
	}

	/**
	 * サーバから切断（確認付き）
	 */
	private void doDisconnectServer() {
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogOkCancel dlg = new AwaitAlertDialogOkCancel(me);
			dlg.setTitle(getString(R.string.activityircserverlist_java_disconnectserver_title));
			dlg.setMessage(String.format(getString(R.string.activityircserverlist_java_disconnectserver), mServerName) );
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// サーバから切断してよいか確認
				final int result = dlg.show();
				switch (result) {
					case AwaitAlertDialogBase.OK:
					{
						(me).runOnUiThread(new Runnable(){ public void run() {
							final Intent intent = new Intent();
							intent.putExtra("disconnect", 1);
							intent.putExtra("connect", 0);
							intent.putExtra("server", mServerName);
							setResult(RESULT_OK, intent);
							finish();
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
	 * サーバ設定の削除（確認付き）
	 */
	private void doDeleteServer() {
		(me).runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogYesNo dlg = new AwaitAlertDialogYesNo(me);
			dlg.setTitle(getString(R.string.activityircserverlist_java_deleteserver_title));
			dlg.setMessage(String.format(getString(R.string.activityircserverlist_java_deleteserver), mServerName) );
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// サーバ設定を削除してよいか確認
				final int result = dlg.show();
				switch (result) {
					case AwaitAlertDialogBase.YES:
					{
						(me).runOnUiThread(new Runnable(){ public void run() {
							try {
								mIrcServerList.removeServer(mServerId);
								iIRCService.removeServer(mServerId);
								mLinearLayout.removeView(mListView);
								mListView = null;
								createServerListView();
								mLinearLayout.addView(mListView);
							} catch(final RemoteException e) {
								alertUpdateServerList();
							}
						} });
						break;
					}
					case AwaitAlertDialogBase.NO:
					default:
						break;
				}
			} }).start();
		} });
	}

}
