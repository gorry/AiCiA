/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import net.gorry.libaicia.R;

/**
 *
 * サーバ設定の編集処理
 *
 * @author GORRY
 *
 */
public class ActivityIRCServerConfig extends Activity {
	private static final String TAG = "ActivityIRCServerConfig";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = true;
	private Activity me;

	private Button mSaveButton;
	private Button mCancelButton;

	private EditText mServerNameEdit;
	private EditText mHostEdit;
	private EditText mPortEdit;
	private EditText mPassEdit;
	private EditText mNickEdit;
	private EditText mUsernameEdit;
	private EditText mRealnameEdit;
	private EditText mEncodingEdit;
	private Button mEncodingButton;
	private CheckBox mUseSslCheckBox;
	private CheckBox mAutoconnectCheckBox;
	private CheckBox mAutoreconnectCheckBox;
	private EditText mChannelEdit;
	private EditText mAlertKeywordsEdit;
	private CheckBox mForTIGCheckBox;
	private CheckBox mPutPaleTextOnSublogCheckBox;
	private CheckBox mAlertNotifyCheckBox;
	private int mServerId = 0;
	private boolean mAddServer = false;

	private IRCServerConfig mConfig;
	private IRCServerConfig mConfigBack;

	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle icicle) {
		if (I) Log.i(TAG, "onCreate()");
		super.onCreate(icicle);
		me = this;
		setTitle(R.string.activityircserverconfig_java_serversetting);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mServerId = extras.getInt("serverid");
			mAddServer = extras.getBoolean("addserver");
		}

		setContentView(R.layout.activity_ircserverconfig);
		mServerNameEdit = (EditText)findViewById(R.id.servername_edittext);
		mHostEdit = (EditText)findViewById(R.id.host_edittext);
		mPortEdit = (EditText)findViewById(R.id.port_edittext);
		mPassEdit = (EditText)findViewById(R.id.pass_edittext);
		mNickEdit = (EditText)findViewById(R.id.nick_edittext);
		mUsernameEdit = (EditText)findViewById(R.id.username_edittext);
		mRealnameEdit = (EditText)findViewById(R.id.realname_edittext);
		mEncodingEdit = (EditText)findViewById(R.id.encoding_edittext);
		mEncodingButton = (Button)findViewById(R.id.encoding_button);
		mEncodingButton.setOnClickListener(onEncodingClick);
		mUseSslCheckBox = (CheckBox)findViewById(R.id.usessl_checkbox);
		mAutoconnectCheckBox = (CheckBox)findViewById(R.id.autoconnect_checkbox);
		mAutoreconnectCheckBox = (CheckBox)findViewById(R.id.autoreconnect_checkbox);
		mForTIGCheckBox = (CheckBox)findViewById(R.id.fortig_checkbox);
		mPutPaleTextOnSublogCheckBox = (CheckBox)findViewById(R.id.putpaletextonsublog_checkbox);
		mAlertNotifyCheckBox = (CheckBox)findViewById(R.id.alertnotify_checkbox);
		mChannelEdit = (EditText)findViewById(R.id.channel_edittext);
		mAlertKeywordsEdit = (EditText)findViewById(R.id.alertkeywords_edittext);

		mConfig = new IRCServerConfig(this);
		if (mAddServer) {
			mConfig.deleteConfig(mServerId);
		}
		mConfig.loadConfig(mServerId);
		mConfigBack = new IRCServerConfig(this);
		mConfigBack.copy(mConfig);
		mServerNameEdit.setText(mConfig.mServerName);
		mHostEdit.setText(mConfig.mHost);
		mPortEdit.setText(Integer.toString(mConfig.mPort));
		mPassEdit.setText(mConfig.mPass);
		mNickEdit.setText(mConfig.mNick);
		mUsernameEdit.setText(mConfig.mUsername);
		mRealnameEdit.setText(mConfig.mRealname);
		mEncodingEdit.setText(mConfig.mEncoding);
		mUseSslCheckBox.setChecked(mConfig.mUseSsl);
		mAutoconnectCheckBox.setChecked(mConfig.mAutoConnect);
		mAutoreconnectCheckBox.setChecked(mConfig.mAutoReconnect);
		mForTIGCheckBox.setChecked(mConfig.mForTIG);
		mPutPaleTextOnSublogCheckBox.setChecked(mConfig.mPutPaleTextOnSublog);
		mAlertNotifyCheckBox.setChecked(mConfig.mAlertNotify);
		mChannelEdit.setText(mConfig.mConnectingChannel);
		mAlertKeywordsEdit.setText(mConfig.mAlertKeywords);

		mSaveButton = (Button)findViewById(R.id.save_button);
		mSaveButton.setOnClickListener(onSaveClick);
		mCancelButton = (Button)findViewById(R.id.cancel_button);
		mCancelButton.setOnClickListener(onCancelClick);

		if (!SystemConfig.notifyOnAlert) {
			mAlertNotifyCheckBox.setEnabled(false);
		}
	}

	/*
	 * キー入力
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (I) Log.i(TAG, "onKeyDown()");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// アクティビティ終了として使う
			final int getDataResult = getData();
			if (mConfig.compare(mConfigBack)) {
				setResult(RESULT_CANCELED);
				(me).runOnUiThread(new Runnable(){ public void run() {
					finish();
				} });
				return true;
			}

			(me).runOnUiThread(new Runnable(){ public void run() {
				final AwaitAlertDialogYesNoCancel dlg = new AwaitAlertDialogYesNoCancel(me);
				dlg.setTitle(getString(R.string.activityircserverconfig_java_save_title));
				dlg.setMessage(String.format(getString(R.string.activityircserverconfig_java_save, mConfig.mServerName)));
				dlg.create();
				new Thread(new Runnable() {	public void run() {
					// サーバ設定を保存してよいか確認
					final int result = dlg.show();
					switch (result) {
					case AwaitAlertDialogBase.YES:
					{
						if (getDataResult < 0) {
							alertServerName(getDataResult);
							return;
						}
						mConfig.saveConfig(mServerId);
						setResult(RESULT_OK);
						finish();
						return;
					}
					case AwaitAlertDialogBase.NO:
					{
						setResult(RESULT_CANCELED);
						finish();
						return;
					}
					case AwaitAlertDialogBase.CANCEL:
					default:
						break;
					}
				} }).start();
			} });
			return true;
		}
		super.onKeyDown(keyCode, event);
		return false;
	}

	/**
	 * ダイアログからのデータ取得
	 * @return reject要因があれば負
	 */
	private final int getData()
	{
		if (I) Log.i(TAG, "getData()");
		mConfig.mServerName = mServerNameEdit.getText().toString();
		mConfig.mHost = mHostEdit.getText().toString();
		mConfig.mPort = Integer.parseInt(mPortEdit.getText().toString());
		mConfig.mPass = mPassEdit.getText().toString();
		mConfig.mNick = mNickEdit.getText().toString();
		mConfig.mUsername = mUsernameEdit.getText().toString();
		mConfig.mRealname = mRealnameEdit.getText().toString();
		mConfig.mEncoding = mEncodingEdit.getText().toString();
		mConfig.mUseSsl = mUseSslCheckBox.isChecked();
		mConfig.mAutoConnect = mAutoconnectCheckBox.isChecked();
		mConfig.mAutoReconnect = mAutoreconnectCheckBox.isChecked();
		mConfig.mForTIG = mForTIGCheckBox.isChecked();
		mConfig.mPutPaleTextOnSublog = mPutPaleTextOnSublogCheckBox.isChecked();
		mConfig.mAlertNotify = mAlertNotifyCheckBox.isChecked();
		mConfig.mConnectingChannel = mChannelEdit.getText().toString();
		mConfig.mAlertKeywords = mAlertKeywordsEdit.getText().toString();

		// サーバ名が空ならreject
		if (mConfig.mServerName.equals("")) {
			return -1;
		}

		// 同名の設定がすでにあったらreject
		final IRCServerList ircServerList = new IRCServerList(me, false);
		ircServerList.reloadList();
		final int id = ircServerList.getServerId(mConfig.mServerName);
		if ((id >= 0) && (id != mServerId)) {
			return -2;
		}

		// ホスト名が空ならreject
		if (mConfig.mHost.equals("")) {
			return -3;
		}

		// Nickが空ならreject
		if (mConfig.mNick.equals("")) {
			return -4;
		}

		// Usernameが空ならreject
		if (mConfig.mUsername.equals("")) {
			return -5;
		}

		// Realnameが空ならreject
		if (mConfig.mRealname.equals("")) {
			return -6;
		}

		return 0;
	}

	/*
	 * エラー表示
	 * @param mode エラー内容
	 */
	private void alertServerName(final int mode) {
		me.runOnUiThread(new Runnable(){ public void run() {
			final AwaitAlertDialogOk dlg = new AwaitAlertDialogOk(me);
			dlg.setTitle("ERROR");
			switch (mode) {
			default:
				dlg.setMessage("Unknown error.");
				break;

			case -1:
				dlg.setMessage(getString(R.string.activityircserverconfig_java_error_servernamerequired));
				mServerNameEdit.requestFocus();
				break;

			case -2:
				dlg.setMessage(getString(R.string.activityircserverconfig_java_error_sameservername));
				mServerNameEdit.requestFocus();
				break;

			case -3:
				dlg.setMessage(getString(R.string.activityircserverconfig_java_error_emptyhost));
				mHostEdit.requestFocus();
				break;

			case -4:
				dlg.setMessage(getString(R.string.activityircserverconfig_java_error_emptynick));
				mNickEdit.requestFocus();
				break;

			case -5:
				dlg.setMessage(getString(R.string.activityircserverconfig_java_error_emptyusername));
				mUsernameEdit.requestFocus();
				break;

			case -6:
				dlg.setMessage(getString(R.string.activityircserverconfig_java_error_emptyrealname));
				mRealnameEdit.requestFocus();
				break;
			}
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// セーブ不可確認
				dlg.show();
			} }).start();
		} });
	}

	/*
	 * 文字コード[選択]ボタン処理
	 */
	private final OnClickListener onEncodingClick = new OnClickListener()
	{
		public void onClick(final View v)
		{
			if (I) Log.i(TAG, "onClick() onEncodingClick");
			(me).runOnUiThread(new Runnable(){
				public void run() {
				final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
				int current = -1;
				final String encoding = mEncodingEdit.getText().toString();
				final SortedMap<String, Charset> charset = Charset.availableCharsets();
				final Set<?> cs = charset.entrySet();
				int n = 0;
				final ArrayList<String> encodings = new ArrayList<String>();
				for (final Iterator<?> i = cs.iterator(); i.hasNext();) {
					@SuppressWarnings("rawtypes")
					final Map.Entry m = (Map.Entry)i.next();
					String s = (String)m.getKey();
					dlg.addItem(s, 0);
					encodings.add(s);
					if (s.equalsIgnoreCase(encoding)) {
						current = n;
					}
					n++;
					if (s.equalsIgnoreCase("ISO-2022-JP")) {
						s = "ISO-2022-JP_with_halfkana";
						dlg.addItem(s, 0);
						encodings.add(s);
						if (s.equalsIgnoreCase(encoding)) {
							current = n;
						}
						n++;
					}
				}
				final int num = n;
				dlg.setTitle(getString(R.string.activityircserverconfig_encoding));
				dlg.setSelection(current);
				dlg.create();
				new Thread(new Runnable() {	public void run() {
					final int result = dlg.show()-AwaitAlertDialogList.CLICKED;
					if ((0 <= result) && (result < num)) {
						(me).runOnUiThread(new Runnable(){ public void run() {
							mEncodingEdit.setText(encodings.get(result));
							mEncodingEdit.requestFocus();
						} });
					}
				} }).start();
			} });
		}
	};

	private final OnClickListener onSaveClick = new OnClickListener()
	{
		public void onClick(final View v)
		{
			if (I) Log.i(TAG, "onClick() onSaveClick");
			final int getDataResult = getData();
			if (getDataResult < 0) {
				alertServerName(getDataResult);
				return;
			}
			if (!mConfig.compare(mConfigBack)) {
				mConfig.saveConfig(mServerId);
				setResult(RESULT_OK);
			}
			finish();
		}
	};

	private final OnClickListener onCancelClick = new OnClickListener()
	{
		public void onClick(final View v)
		{
			if (I) Log.i(TAG, "onClick() onCancelClick");

			getData();
			if (mConfig.compare(mConfigBack)) {
				setResult(RESULT_CANCELED);
				finish();
				return;
			}

			final AwaitAlertDialogYesNo dlg = new AwaitAlertDialogYesNo(me);
			dlg.setTitle(getString(R.string.activityircserverconfig_java_cancel_title));
			dlg.setMessage(String.format(getString(R.string.activityircserverconfig_java_cancel, mConfig.mServerName)));
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// サーバ設定をキャンセルしてよいか確認
				final int result = dlg.show();
				switch (result) {
				case AwaitAlertDialogBase.YES:
				{
					setResult(RESULT_CANCELED);
					finish();
					return;
				}
				case AwaitAlertDialogBase.NO:
				default:
					break;
				}
			} }).start();
		}
	};

}
