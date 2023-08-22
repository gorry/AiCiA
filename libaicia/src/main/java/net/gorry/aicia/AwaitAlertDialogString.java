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
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * 入力待ち型アラートダイアログ（String型）
 * 
 * @author GORRY
 *
 */
public class AwaitAlertDialogString extends AwaitAlertDialogBase {
	private static final String TAG = "AwaitAlertDialogString";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;
	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	// private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;

	private String mDefaultParam;
	private String mResultParam;
	private String mCommentText = null;
	private int mResult = CANCEL;
	private LinearLayout mLayout;
	private EditText mEdit = null;
	private TextView mComment = null;
	private boolean mMultiLine = false;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 */
	AwaitAlertDialogString(final Context context) {
		super(context);
	}

	/*
	 * ダイアログにビューを追加
	 * @see net.gorry.aicia.AwaitAlertDialogBase#addView(android.app.AlertDialog.Builder)
	 */
	@Override
	public void addView(final AlertDialog.Builder bldr) {
		if (I) Log.i(TAG, "addView()");

		mLayout = new LinearLayout(me);
		mLayout.setOrientation(LinearLayout.VERTICAL);
		mLayout.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));

		mEdit = new EditText(me);
		mEdit.setText(mDefaultParam);
		mEdit.setSingleLine(!mMultiLine);
		mLayout.addView(mEdit);

		if (mCommentText != null) {
			mComment = new TextView(me);
			mComment.setText(mCommentText);
			mLayout.addView(mComment);
		}

		bldr.setView(mLayout);
		bldr.setPositiveButton("Ok", MyClickListener);
		bldr.setNegativeButton("Cancel", MyClickListener);
		bldr.setCancelable(true);
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
	 * ダイアログの返り値を作成
	 * @return 入力文字列
	 */
	public String getResultParam() {
		if (I) Log.i(TAG, "getResult()");
		return mResultParam;
	}

	/**
	 * クリック時の応答
	 */
	DialogInterface.OnClickListener MyClickListener = new DialogInterface.OnClickListener(){
		public void onClick(final DialogInterface dialog, final int which) {
			if (I) Log.i(TAG, "onClick()");
			switch (which) {
				case DialogInterface.BUTTON1:
					mResult = OK;
					mResultParam = mEdit.getText().toString();
					break;
				case DialogInterface.BUTTON2:
					mResult = CANCEL;
					break;
			}
		}
	};

	/**
	 * 初期値を登録
	 * @param def 初期値
	 */
	public void setDefaultParam(final String def) {
		if (I) Log.i(TAG, "setMessage()");
		mDefaultParam = def;
	}

	/**
	 * 初期値を登録
	 * @param def 初期値
	 */
	public void setUseMultiLine(final boolean def) {
		if (I) Log.i(TAG, "setUseMultiLine()");
		mMultiLine = def;
	}

	/**
	 * コメントを登録
	 * @param comment コメント
	 */
	public void setComment(final String comment) {
		if (I) Log.i(TAG, "setComment()");
		mCommentText = comment;
	}

	/**
	 * ダイアログ表示時に呼び出される
	 */
	@Override
	void onShowMyDialog() {
		//
	}
}



