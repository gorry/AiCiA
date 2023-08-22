/**
 *
 */
package net.gorry.aicia;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import net.gorry.libaicia.R;

/**
 *
 * チャンネル設定の編集処理
 *
 * @author GORRY
 *
 */
public class ActivityIRCChannelConfig extends Activity {
	private static final String TAG = "ActivityIRCChannelConfig";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = true;
	private Activity me;

	private Button mSaveButton;
	private Button mCancelButton;

	private TextView mChannelNameEdit;
	private CheckBox mPutOnSublogCheckBox;
	private CheckBox mPutOnSublogAllCheckBox;
	private CheckBox mUseAlertCheckBox;
	private CheckBox mUseAlertAllCheckBox;
	private CheckBox mPutPaleTextOnSublogCheckBox;
	private CheckBox mAlertNotifyCheckBox;
	private int mServerId = 0;
	private String mChannelName = "";

	private IRCChannelConfig mChannelConfig;
	private IRCChannelConfig mChannelConfigBack;
	private IRCServerConfig mServerConfig;

	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle icicle) {
		if (I) Log.i(TAG, "onCreate()");
		super.onCreate(icicle);
		me = this;
		setTitle(R.string.activityircchannelconfig_java_channelsetting);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mServerId = extras.getInt("serverid");
			mChannelName = extras.getString("channelname");
		}

		setContentView(R.layout.activity_ircchannelconfig);
		mChannelNameEdit = (EditText)findViewById(R.id.channelname_edittext);
		mPutOnSublogCheckBox = (CheckBox)findViewById(R.id.putonsublog_checkbox);
		mPutOnSublogAllCheckBox = (CheckBox)findViewById(R.id.putonsublogall_checkbox);
		mPutPaleTextOnSublogCheckBox = (CheckBox)findViewById(R.id.putpaletextonsublog_checkbox);
		mAlertNotifyCheckBox = (CheckBox)findViewById(R.id.alertnotify_checkbox);
		mUseAlertCheckBox = (CheckBox)findViewById(R.id.usealert_checkbox);
		mUseAlertAllCheckBox = (CheckBox)findViewById(R.id.usealertall_checkbox);

		mServerConfig = new IRCServerConfig(this);
		mServerConfig.loadConfig(mServerId);
		mChannelConfig = new IRCChannelConfig(this);
		mChannelConfig.loadConfig(mServerId, mChannelName);
		mChannelConfigBack = new IRCChannelConfig(this);
		mChannelConfigBack.copy(mChannelConfig);
		mChannelNameEdit.setText(mChannelName);
		mPutOnSublogCheckBox.setChecked(mChannelConfig.mPutOnSublog);
		mPutOnSublogAllCheckBox.setChecked(mChannelConfig.mPutOnSublogAll);
		mPutPaleTextOnSublogCheckBox.setChecked(mChannelConfig.mPutPaleTextOnSublog);
		mAlertNotifyCheckBox.setChecked(mChannelConfig.mAlertNotify);
		mUseAlertCheckBox.setChecked(mChannelConfig.mUseAlert);
		mUseAlertAllCheckBox.setChecked(mChannelConfig.mUseAlertAll);

		mSaveButton = (Button)findViewById(R.id.save_button);
		mSaveButton.setOnClickListener(onSaveClick);
		mCancelButton = (Button)findViewById(R.id.cancel_button);
		mCancelButton.setOnClickListener(onCancelClick);
		
		if (!mServerConfig.mPutPaleTextOnSublog) {
			mPutPaleTextOnSublogCheckBox.setEnabled(false);
		}
		if (!mServerConfig.mAlertNotify || !SystemConfig.notifyOnAlert) {
			mAlertNotifyCheckBox.setEnabled(false);
		}
	}

	/*
	 * キー入力
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (I) Log.i(TAG, "onConfigurationChanged()");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// アクティビティ終了として使う
			getData();
			if (mChannelConfig.compare(mChannelConfigBack)) {
				setResult(RESULT_CANCELED);
				(me).runOnUiThread(new Runnable(){ public void run() {
					finish();
				} });
				return true;
			}

			(me).runOnUiThread(new Runnable(){ public void run() {
				final AwaitAlertDialogYesNoCancel dlg = new AwaitAlertDialogYesNoCancel(me);
				dlg.setTitle(getString(R.string.activityircchannelconfig_java_save_title));
				dlg.setMessage(String.format(getString(R.string.activityircchannelconfig_java_save, mChannelName)));
				dlg.create();
				new Thread(new Runnable() {	public void run() {
					// サーバ設定を保存してよいか確認
					final int result = dlg.show();
					switch (result) {
					case AwaitAlertDialogBase.YES:
					{
						mChannelConfig.saveConfig(mServerId, mChannelName);
						mServerConfig.registerChannelName(mServerId, mChannelName);
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
		mChannelConfig.mPutOnSublog = mPutOnSublogCheckBox.isChecked();
		mChannelConfig.mPutOnSublogAll = mPutOnSublogAllCheckBox.isChecked();
		mChannelConfig.mPutPaleTextOnSublog = mPutPaleTextOnSublogCheckBox.isChecked();
		mChannelConfig.mAlertNotify = mAlertNotifyCheckBox.isChecked();
		mChannelConfig.mUseAlert = mUseAlertCheckBox.isChecked();
		mChannelConfig.mUseAlertAll = mUseAlertAllCheckBox.isChecked();

		return 0;
	}

	private final OnClickListener onSaveClick = new OnClickListener()
	{
		public void onClick(final View v)
		{
			if (I) Log.i(TAG, "onClick() onSaveClick");
			getData();
			if (!mChannelConfig.compare(mChannelConfigBack)) {
				mChannelConfig.saveConfig(mServerId, mChannelName);
				mServerConfig.registerChannelName(mServerId, mChannelName);
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
			if (mChannelConfig.compare(mChannelConfigBack)) {
				setResult(RESULT_CANCELED);
				finish();
				return;
			}

			final AwaitAlertDialogYesNo dlg = new AwaitAlertDialogYesNo(me);
			dlg.setTitle(getString(R.string.activityircchannelconfig_java_cancel_title));
			dlg.setMessage(String.format(getString(R.string.activityircchannelconfig_java_cancel, mChannelName)));
			dlg.create();
			new Thread(new Runnable() {	public void run() {
				// チャンネル設定をキャンセルしてよいか確認
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
