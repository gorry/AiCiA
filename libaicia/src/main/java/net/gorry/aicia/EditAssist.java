/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import android.content.Context;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

/**
 * @author gorry
 *
 */
public class EditAssist {
	private final Context me;

	private KeyListener mOldKeyListener;

	EditAssist(final Context context) {
		me = context;
	}

	/**
	 * editTextにinsTextを挿入する
	 * @param editText EditText
	 * @param insText 挿入文字列
	 */
	public static void insert(final EditText editText, final String insText) {
		final String edit = editText.getText().toString();
		int selStart = editText.getSelectionStart();
		int selEnd = editText.getSelectionEnd();
		if (selStart > selEnd) {
			final int swap = selStart;
			selStart = selEnd;
			selEnd = swap;
		}
		final String edit2 = edit.substring(0, selStart) + insText + edit.substring(selEnd, edit.length());
		editText.setText(edit2);
		editText.setSelection(selStart+insText.length());
	}

	/**
	 * editTextにsetTextを設定する
	 * @param editText EditText
	 * @param setText 挿入文字列
	 */
	public static void set(final EditText editText, final String setText) {
		editText.setText(setText);
		editText.setSelection(setText.length());
	}

	/**
	 * onKeyDownに使うテキストのコピー・カット・ペースト処理
	 * @param context コンテキスト
	 * @param editText EditText
	 * @param keyCode onKeyDown()のkeyCode
	 * @param event onKeyDown()のevent
	 * @return 処理を行ったらtrue
	 */
	public static boolean cutCopyPaste(final Context context, final EditText editText, final int keyCode, final KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_A) && (event.isAltPressed())) {
			// 全選択
			final String edit = editText.getText().toString();
			editText.setSelection(0, edit.length());
			editText.requestFocus();
			return true;
		}
		if ((keyCode == KeyEvent.KEYCODE_X) && (event.isAltPressed())) {
			// カット
			final String edit = editText.getText().toString();
			int selStart = editText.getSelectionStart();
			int selEnd = editText.getSelectionEnd();
			if (selStart > selEnd) {
				final int swap = selStart;
				selStart = selEnd;
				selEnd = swap;
			}
			final ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(edit.substring(selStart, selEnd));
			final String edit2 = edit.substring(0, selStart) + edit.substring(selEnd, edit.length());
			editText.setText(edit2);
			editText.setSelection(selStart);
			editText.requestFocus();
			return true;
		}
		if ((keyCode == KeyEvent.KEYCODE_C) && (event.isAltPressed())) {
			// コピー
			final String edit = editText.getText().toString();
			int selStart = editText.getSelectionStart();
			int selEnd = editText.getSelectionEnd();
			if (selStart > selEnd) {
				final int swap = selStart;
				selStart = selEnd;
				selEnd = swap;
			}
			final ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(edit.substring(selStart, selEnd));
			editText.requestFocus();
			return true;
		}
		if ((keyCode == KeyEvent.KEYCODE_V) && (event.isAltPressed())) {
			// 貼り付け
			final ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			final String cmText = cm.getText().toString();
			EditAssist.insert(editText, cmText);
			editText.requestFocus();
			return true;
		}
		return false;
	}

	/**
	 * 元のKeyListenerの登録
	 * @param keyListener 古いリスナ
	 */
	public void setOldKeyListener(final KeyListener keyListener) {
		mOldKeyListener = keyListener;
	}

	/**
	 * 差し替えとなるKeyListener
	 *
	 */
	public final KeyListener newKeyListener = new KeyListener() {
		public int getInputType() {
			return mOldKeyListener.getInputType();
		}

		public boolean onKeyDown(final View view, final Editable text, final int keyCode, final KeyEvent event) {
			if (EditAssist.cutCopyPaste(me, (EditText)view, keyCode, event)) {
				return true;
			}
			return mOldKeyListener.onKeyDown(view, text, keyCode, event);
		}

		public boolean onKeyUp(final View view, final Editable text, final int keyCode, final KeyEvent event) {
			return mOldKeyListener.onKeyUp(view, text, keyCode, event);
		}

		public boolean onKeyOther(final View view, final Editable text, final KeyEvent event) {
			return mOldKeyListener.onKeyOther(view, text, event);
		}

		public void clearMetaKeyState(final View view, final Editable content, final int states) {
			mOldKeyListener.clearMetaKeyState(view, content, states);
		}

	};


	/**
	 * Enter/Space/Shift/Alt以外のキーを押したかどうかの確認
	 * @param keyCode キーコード
	 * @return 押されていたらtrue
	 */
	public static boolean isAnyKey(final int keyCode) {
		switch (keyCode) {
			case 92:  // IS01の[絵・顔・記]キー
			case 93:  // IS01の[文字]キー
			case KeyEvent.KEYCODE_0:
			case KeyEvent.KEYCODE_1:
			case KeyEvent.KEYCODE_2:
			case KeyEvent.KEYCODE_3:
			case KeyEvent.KEYCODE_4:
			case KeyEvent.KEYCODE_5:
			case KeyEvent.KEYCODE_6:
			case KeyEvent.KEYCODE_7:
			case KeyEvent.KEYCODE_8:
			case KeyEvent.KEYCODE_9:
			case KeyEvent.KEYCODE_STAR:
			case KeyEvent.KEYCODE_POUND:
			case KeyEvent.KEYCODE_A:
			case KeyEvent.KEYCODE_B:
			case KeyEvent.KEYCODE_C:
			case KeyEvent.KEYCODE_D:
			case KeyEvent.KEYCODE_E:
			case KeyEvent.KEYCODE_F:
			case KeyEvent.KEYCODE_G:
			case KeyEvent.KEYCODE_H:
			case KeyEvent.KEYCODE_I:
			case KeyEvent.KEYCODE_J:
			case KeyEvent.KEYCODE_K:
			case KeyEvent.KEYCODE_L:
			case KeyEvent.KEYCODE_M:
			case KeyEvent.KEYCODE_N:
			case KeyEvent.KEYCODE_O:
			case KeyEvent.KEYCODE_P:
			case KeyEvent.KEYCODE_Q:
			case KeyEvent.KEYCODE_R:
			case KeyEvent.KEYCODE_S:
			case KeyEvent.KEYCODE_T:
			case KeyEvent.KEYCODE_U:
			case KeyEvent.KEYCODE_V:
			case KeyEvent.KEYCODE_W:
			case KeyEvent.KEYCODE_X:
			case KeyEvent.KEYCODE_Y:
			case KeyEvent.KEYCODE_Z:
			case KeyEvent.KEYCODE_COMMA:
			case KeyEvent.KEYCODE_PERIOD:
			case KeyEvent.KEYCODE_TAB:
			case KeyEvent.KEYCODE_SPACE:
			case KeyEvent.KEYCODE_DEL:
			case KeyEvent.KEYCODE_GRAVE:
			case KeyEvent.KEYCODE_MINUS:
			case KeyEvent.KEYCODE_EQUALS:
			case KeyEvent.KEYCODE_LEFT_BRACKET:
			case KeyEvent.KEYCODE_RIGHT_BRACKET:
			case KeyEvent.KEYCODE_BACKSLASH:
			case KeyEvent.KEYCODE_SEMICOLON:
			case KeyEvent.KEYCODE_APOSTROPHE:
			case KeyEvent.KEYCODE_SLASH:
			case KeyEvent.KEYCODE_AT:
			case KeyEvent.KEYCODE_PLUS:
				return true;
		}
		return false;
	}

}
