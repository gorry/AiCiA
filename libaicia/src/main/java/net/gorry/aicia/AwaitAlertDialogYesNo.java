/**
 * 
 * AiCiA - Android IRC Client
 * 
 * Copyright (C)2010 GORRY.
 * 
 */

package net.gorry.aicia;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

/**
 * 
 * 入力待ち型アラートダイアログ（Yes/No型）
 * 
 * @author GORRY
 *
 */
public class AwaitAlertDialogYesNo extends AwaitAlertDialogBase {
	private static final String TAG = "AwaitAlertDialogYesNo";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;

	private String mMessage;
	private int mResult = CANCEL;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 */
	AwaitAlertDialogYesNo(final Context context) {
		super(context);
	}

	/*
	 * ダイアログにビューを追加
	 * @see net.gorry.aicia.AwaitAlertDialogBase#addView(android.app.AlertDialog.Builder)
	 */
	@Override
	public void addView(final AlertDialog.Builder bldr) {
		if (I) Log.i(TAG, "addView()");

		bldr.setMessage(mMessage);
		bldr.setPositiveButton("Yes", MyClickListener);
		bldr.setNegativeButton("No", MyClickListener);
		bldr.setCancelable(false);

	}

	/**
	 * ダイアログの返り値を作成
	 * @return show()が返す値
	 */
	@Override
	public int getResult() {
		if (I) Log.i(TAG, "getResult()");
		return mResult;
	}

	/**
	 * クリック時の応答
	 */
	DialogInterface.OnClickListener MyClickListener = new DialogInterface.OnClickListener(){
		public void onClick(final DialogInterface dialog, final int which) {
			if (I) Log.i(TAG, "onClick()");
			switch (which) {
				case DialogInterface.BUTTON1:
					mResult = YES;
					break;
				case DialogInterface.BUTTON2:
					mResult = NO;
					break;
			}
		}
	};

	/**
	 * メッセージを登録
	 * @param msg メッセージ
	 */
	public void setMessage(final String msg) {
		if (I) Log.i(TAG, "setMessage()");
		mMessage = msg;
	}

	/**
	 * ダイアログ表示時に呼び出される
	 */
	@Override
	void onShowMyDialog() {
		//
	}
}



