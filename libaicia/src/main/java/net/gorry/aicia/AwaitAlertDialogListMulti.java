/**
 * 
 * AiCiA - Android IRC Client
 * 
 * Copyright (C)2010 GORRY.
 * 
 */

package net.gorry.aicia;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import net.gorry.libaicia.R;


/**
 * 
 * 入力待ち型アラートダイアログ（複数選択リスト型）
 * 
 * 返り値は以下のように拡張される。
 * Integer[] 選択した項目のリスト（一番上の項目がn=0）
 * 
 * @author GORRY
 *
 */
public class AwaitAlertDialogListMulti extends AwaitAlertDialogBase {
	private static final String TAG = "AwaitAlertDialogListMulti";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;

	@SuppressWarnings("unused")
	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;

	private final ArrayList<String> mItemList = new ArrayList<String>();
	private final ArrayList<Boolean> mItemCheckedList = new ArrayList<Boolean>();
	private final ArrayList<Integer> mItemShortcutLv = new ArrayList<Integer>();
	private final ArrayList<Integer> mItemShortcutLv1Id = new ArrayList<Integer>();
	private final ArrayList<Integer> mItemShortcutLv2Id = new ArrayList<Integer>();
	private int mLayout = R.layout.alertdialoglistmulti;
	private int mSelection = -1;
	private ListView mListView;
	// private final ArrayList<Integer> mSelectedIdList = new ArrayList<Integer>();
	int mResult = CANCEL;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 */
	AwaitAlertDialogListMulti(final Context context) {
		super(context);
		if (I) Log.i(TAG, "AwaitAlertDialogListMulti()");
	}

	/*
	 * ダイアログにビューを追加
	 * @see net.gorry.aicia.AwaitAlertDialogBase#addView(android.app.AlertDialog.Builder)
	 */
	@Override
	public void addView(final AlertDialog.Builder bldr) {
		if (I) Log.i(TAG, "addView()");

		class BooleanListAdapter extends ArrayAdapter<String> {
			protected LayoutInflater mInflater;
			private final List<String> mItems;
			private final List<Boolean> mItemChecks;
			private final int mRowLayoutResourceId;

			public BooleanListAdapter(final Context context, final int rowLayoutResourceId, final ArrayList<String> items, final ArrayList<Boolean> itemChecks) {
				super(context, rowLayoutResourceId, items);
				if (I) Log.i(TAG, "BooleanListAdapter()");
				mItems = items;
				mItemChecks = itemChecks;
				mInflater = (LayoutInflater)(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
				mRowLayoutResourceId = rowLayoutResourceId;
			}

			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent) {
				if (I) Log.i(TAG, "getView()");
				final View baseView;
				if (convertView == null) {
					baseView = mInflater.inflate(mRowLayoutResourceId, null);
				} else {
					baseView = convertView;
				}

				final CheckBox checkBox = (CheckBox)(baseView.findViewById(R.id.alertdialoglistmulti_checkbox));
				checkBox.setText(mItems.get(position));
				checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(final CompoundButton v, final boolean isChecked) {
						if (I) Log.i(TAG, "onCheckedChanged()");
						mItemChecks.set(position, isChecked);
					}
				});
				checkBox.setChecked(mItemChecks.get(position));
				return baseView;
			}
		}

		int lv1Id = 0;
		int lv2Id = 0;
		for (int i=0; i<mItemShortcutLv.size(); i++) {
			switch (mItemShortcutLv.get(i)) {
				default:
				case 0:
					mItemShortcutLv1Id.add(0);
					mItemShortcutLv2Id.add(0);
					break;
				case 1:
					lv1Id++;
					lv2Id = 0;
					mItemShortcutLv1Id.add(lv1Id);
					mItemShortcutLv2Id.add(lv2Id);
					if (lv1Id <= 10) {
						final String ids = "1234567890";
						mItemList.set(i, ids.substring(lv1Id-1, lv1Id) + ": " + mItemList.get(i));
					}
					break;
				case 2:
					lv2Id++;
					mItemShortcutLv1Id.add(lv1Id);
					mItemShortcutLv2Id.add(lv2Id);
					if (lv2Id <= 26) {
						final String msg = mItemList.get(i);
						final String regex = "^([ ]*)(.*)";
						final Pattern p = Pattern.compile(regex);
						final Matcher m = p.matcher(msg);
						final String ids = "abcdefghijklmnopqrstuvwxyz";
						mItemList.set(i, m.replaceAll("$1" + ids.substring(lv2Id-1, lv2Id) + ": $2"));
					}
					break;
			}
		}

		mListView = new ListView(me);
		{
			mListView.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
					if (I) Log.i(TAG, "onCheckedChanged()");
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_ENTER:
								mResult = OK;
								mDialog.dismiss();
								break;
							case KeyEvent.KEYCODE_DPAD_CENTER:
							case KeyEvent.KEYCODE_SPACE:
							{
								final int pos = mListView.getSelectedItemPosition();
								final View l = mListView.getSelectedView();
								final CheckBox checkBox = (CheckBox)(l.findViewById(R.id.alertdialoglistmulti_checkbox));
								checkBox.setChecked(!mItemCheckedList.get(pos));
								return true;
							}
							case KeyEvent.KEYCODE_1:
								doMoveSelectionLv1(1);
								break;
							case KeyEvent.KEYCODE_2:
								doMoveSelectionLv1(2);
								break;
							case KeyEvent.KEYCODE_3:
								doMoveSelectionLv1(3);
								break;
							case KeyEvent.KEYCODE_4:
								doMoveSelectionLv1(4);
								break;
							case KeyEvent.KEYCODE_5:
								doMoveSelectionLv1(5);
								break;
							case KeyEvent.KEYCODE_6:
								doMoveSelectionLv1(6);
								break;
							case KeyEvent.KEYCODE_7:
								doMoveSelectionLv1(7);
								break;
							case KeyEvent.KEYCODE_8:
								doMoveSelectionLv1(8);
								break;
							case KeyEvent.KEYCODE_9:
								doMoveSelectionLv1(9);
								break;
							case KeyEvent.KEYCODE_0:
								doMoveSelectionLv1(10);
								break;

							case KeyEvent.KEYCODE_A:
								doMoveSelectionLv2(1);
								break;
							case KeyEvent.KEYCODE_B:
								doMoveSelectionLv2(2);
								break;
							case KeyEvent.KEYCODE_C:
								doMoveSelectionLv2(3);
								break;
							case KeyEvent.KEYCODE_D:
								doMoveSelectionLv2(4);
								break;
							case KeyEvent.KEYCODE_E:
								doMoveSelectionLv2(5);
								break;
							case KeyEvent.KEYCODE_F:
								doMoveSelectionLv2(6);
								break;
							case KeyEvent.KEYCODE_G:
								doMoveSelectionLv2(7);
								break;
							case KeyEvent.KEYCODE_H:
								doMoveSelectionLv2(8);
								break;
							case KeyEvent.KEYCODE_I:
								doMoveSelectionLv2(9);
								break;
							case KeyEvent.KEYCODE_J:
								doMoveSelectionLv2(10);
								break;
							case KeyEvent.KEYCODE_K:
								doMoveSelectionLv2(11);
								break;
							case KeyEvent.KEYCODE_L:
								doMoveSelectionLv2(12);
								break;
							case KeyEvent.KEYCODE_M:
								doMoveSelectionLv2(13);
								break;
							case KeyEvent.KEYCODE_N:
								doMoveSelectionLv2(14);
								break;
							case KeyEvent.KEYCODE_O:
								doMoveSelectionLv2(15);
								break;
							case KeyEvent.KEYCODE_P:
								doMoveSelectionLv2(16);
								break;
							case KeyEvent.KEYCODE_Q:
								doMoveSelectionLv2(17);
								break;
							case KeyEvent.KEYCODE_R:
								doMoveSelectionLv2(18);
								break;
							case KeyEvent.KEYCODE_S:
								doMoveSelectionLv2(19);
								break;
							case KeyEvent.KEYCODE_T:
								doMoveSelectionLv2(20);
								break;
							case KeyEvent.KEYCODE_U:
								doMoveSelectionLv2(21);
								break;
							case KeyEvent.KEYCODE_V:
								doMoveSelectionLv2(22);
								break;
							case KeyEvent.KEYCODE_W:
								doMoveSelectionLv2(23);
								break;
							case KeyEvent.KEYCODE_X:
								doMoveSelectionLv2(24);
								break;
							case KeyEvent.KEYCODE_Y:
								doMoveSelectionLv2(25);
								break;
							case KeyEvent.KEYCODE_Z:
								doMoveSelectionLv2(26);
								break;
						}
					}
					return false;
				}
			});
			mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			mListView.setLayoutParams(new LinearLayout.LayoutParams(FP, FP));
			mListView.setFocusable(true);
			mListView.setFocusableInTouchMode(true);
			final BooleanListAdapter arrayAdapter = new BooleanListAdapter(
					me,
					mLayout,
					mItemList,
					mItemCheckedList
			);
			mListView.setAdapter(arrayAdapter);
			mListView.setSelection(mSelection);
		}

		bldr.setView(mListView);
		bldr.setPositiveButton("Ok", MyClickListener);
		bldr.setCancelable(true);
	}

	/**
	 * 指定Lv1ショートカットへ移動
	 * @param lv1Id Lv1Id
	 */
	private void doMoveSelectionLv1(final int lv1Id)
	{
		for (int i=0; i<mItemShortcutLv1Id.size(); i++) {
			if (mItemShortcutLv1Id.get(i) == lv1Id) {
				mListView.setSelection(i);
				mSelection = i;
				return;
			}
		}
	}

	/**
	 * 指定Lv2ショートカットへ移動
	 * @param lv2Id Lv2Id
	 */
	private void doMoveSelectionLv2(final int lv2Id)
	{
		int pos = mListView.getSelectedItemPosition();
		if (pos < 0) {
			pos = mSelection;
			if (pos < 0) pos = 0;
		}
		if (pos >= mItemShortcutLv1Id.size()) return;
		final int lv1Id = mItemShortcutLv1Id.get(pos);
		for (int i=0; i<mItemShortcutLv1Id.size(); i++) {
			if (mItemShortcutLv1Id.get(i) == lv1Id) {
				for (int j=i; j<mItemShortcutLv2Id.size(); j++) {
					if (mItemShortcutLv1Id.get(j) != lv1Id) {
						return;
					}
					if (mItemShortcutLv2Id.get(j) == lv2Id) {
						mListView.setSelection(j);
						mSelection = j;
						return;
					}
				}
			}
		}
	}

	/**
	 * ダイアログ表示時に呼び出される
	 */
	@Override
	public void onShowMyDialog() {
		if (I) Log.i(TAG, "onShowMyDialog()");
	}

	/**
	 * ダイアログの返り値を作成
	 * @return show()が返す値
	 */
	@Override
	public int getResult() {
		if (I) Log.i(TAG, "getResult()");

		int nChecked = 0;
		for (int i=0; i<mItemCheckedList.size(); i++) {
			if (mItemCheckedList.get(i)) {
				nChecked++;
			}
		}

		if (nChecked == 0) return CANCEL;
		return OK;
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
					break;
			}
		}
	};

	/**
	 * リストアイテムを登録
	 * @param pos 登録位置
	 * @param item 登録アイテム
	 * @param lv ショートカットレベル
	 * @param checked チェックされているときtrue
	 */
	public void setItem(final int pos, final String item, final int lv, final boolean checked) {
		if (I) Log.i(TAG, "setListItem()");
		mItemList.set(pos, item);
		mItemShortcutLv.set(pos, lv);
		mItemCheckedList.set(pos, checked);
	}

	/**
	 * リストアイテムを末尾に登録
	 * @param item 登録アイテム
	 * @param lv ショートカットレベル
	 * @param checked チェックされているときtrue
	 */
	public void addItem(final String item, final int lv, final boolean checked) {
		if (I) Log.i(TAG, "addListItem()");
		mItemList.add(item);
		mItemShortcutLv.add(lv);
		mItemCheckedList.add(checked);
	}

	/**
	 * リストの初期選択位置を設定
	 * @param pos 初期選択位置
	 */
	public void setSelection(final int pos) {
		if (I) Log.i(TAG, "setSelection()");
		mSelection = pos;
	}

	/**
	 * リストの選択状態を返す
	 * @return 選択したIDのリスト
	 */
	public Boolean[] getSelection() {
		if (I) Log.i(TAG, "getSelection()");
		return mItemCheckedList.toArray(new Boolean[0]);
	}

	/**
	 * リストのレイアウトを設定
	 * @param resourceId リソースID
	 */
	public void setListResource(final int resourceId) {
		if (I) Log.i(TAG, "setListResource()");
		mLayout = resourceId;
	}
}
