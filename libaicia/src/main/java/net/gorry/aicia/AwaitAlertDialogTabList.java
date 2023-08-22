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

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ScrollView;

import net.gorry.libaicia.R;

/**
 * 
 * 入力待ち型アラートダイアログ（タブ付リスト型）
 * 
 * 返り値は以下のように拡張される。
 * CLICKED+n n番めの項目をクリック（一番上の項目がn=0）
 * LONGCLICKED+n n番めの項目をロングクリック（一番上の項目がn=0）
 * CANCEL 選択されなかった
 * 
 * @author GORRY
 *
 */
public class AwaitAlertDialogTabList extends AwaitAlertDialogBase {
	private static final String TAG = "AwaitAlertDialogTabList";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;
	/** アイテムクリックの選択値 */
	public static final int CLICKED = 10000;
	/** アイテムロングクリックの選択値 */
	public static final int LONGCLICKED = 20000;

	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;

	private final ArrayList<String> mItemList = new ArrayList<String>();
	private final ArrayList<Integer> mItemShortcutLv = new ArrayList<Integer>();
	private final ArrayList<Integer> mItemShortcutLv1Id = new ArrayList<Integer>();
	private final ArrayList<Integer> mItemShortcutLv2Id = new ArrayList<Integer>();
	private int mLayout = R.layout.alertdialoglist;
	private int mClickResult = -1;
	private int mLongClickResult = -1;
	private boolean mEnableLongClick = true;
	private int mSelection = -1;
	private ListView mListView;
	private ScrollView mScrollView;
	private HorizontalScrollView mHorizontalScrollView;
	private LinearLayout mBaseLayout;
	private LinearLayout mTabLayout;
	private LinearLayout mListLayout;
	private LinearLayout mTabView;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 */
	AwaitAlertDialogTabList(final Context context) {
		super(context);
		if (I) Log.i(TAG, "AwaitAlertDialogTabList()");
	}

	/*
	 * ダイアログにビューを追加
	 * @see net.gorry.aicia.AwaitAlertDialogBase#addView(android.app.AlertDialog.Builder)
	 */
	@Override
	public void addView(final AlertDialog.Builder bldr) {
		if (I) Log.i(TAG, "addView()");

		mBaseLayout = new LinearLayout(me);
		mBaseLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP));

		mListLayout = new LinearLayout(me);
		mListLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP, 1));

		mTabLayout = new LinearLayout(me);
		mTabView = new LinearLayout(me);

		final boolean isLandscape = (me.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
		if (isLandscape) {
			mTabLayout.setLayoutParams(new LinearLayout.LayoutParams(WC, FP));
			mTabView.setLayoutParams(new LinearLayout.LayoutParams(WC, FP));

			mBaseLayout.setOrientation(LinearLayout.HORIZONTAL);
			mTabLayout.setOrientation(LinearLayout.VERTICAL);
			mTabView.setOrientation(LinearLayout.VERTICAL);

			mScrollView = new ScrollView(me);
			mScrollView.setLayoutParams(new LinearLayout.LayoutParams(WC, FP));
			mTabLayout.addView(mScrollView);
			mScrollView.addView(mTabView);
		} else {
			mTabLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
			mTabView.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));

			mBaseLayout.setOrientation(LinearLayout.VERTICAL);
			mTabLayout.setOrientation(LinearLayout.HORIZONTAL);
			mTabView.setOrientation(LinearLayout.HORIZONTAL);
		
			mHorizontalScrollView = new HorizontalScrollView(me);
			mHorizontalScrollView.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
			mTabLayout.addView(mHorizontalScrollView);
			mHorizontalScrollView.addView(mTabView);
		}

		mBaseLayout.addView(mListLayout);
		mBaseLayout.addView(mTabLayout);

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
			mListView.setLayoutParams(new LinearLayout.LayoutParams(FP, FP));
			mListView.setFocusable(true);
			mListView.setFocusableInTouchMode(true);
			final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					me,
					mLayout,
					mItemList
			);
			mListView.setAdapter(arrayAdapter);
			mListView.setOnItemClickListener(new MyListClickAdapter());
			if (mEnableLongClick) {
				mListView.setOnItemLongClickListener(new MyListLongClickAdapter());
			}
			mListView.requestFocus();
			mListView.setSelection(mSelection);
			mListLayout.addView(mListView);
		}

		final WindowManager windowManager = (WindowManager)me.getSystemService(Context.WINDOW_SERVICE);
		final Display display = windowManager.getDefaultDisplay();
		final int width = display.getWidth();
		final int height = display.getHeight();
		final int shorter = (width < height) ? width : height;

		for (int i=0; i<lv1Id; i++) {
			Button b = new Button(me);
			b.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
			b.setText(Integer.toString(i+1));
			b.setWidth(shorter/5);
			b.setMinimumWidth(shorter/5);
			b.setLongClickable(true);
			b.setOnClickListener(onTabButtonClick);
			b.setOnLongClickListener(onTabButtonLongClick);
			mTabView.addView(b);
		}
		
		bldr.setView(mBaseLayout);
		bldr.setCancelable(true);
	}

	/**
	 * タブボタン押下
	 */
	private final OnClickListener onTabButtonClick = new OnClickListener()
	{
		@Override
		public void onClick(final View v)
		{
			if (I) Log.i(TAG, "onClick() onTabButtonClick");
			final Button b = (Button)v;
			final String s = b.getText().toString();
			final int id = Integer.valueOf(s);
			doMoveSelectionLv1(id);
		}
	};
	private final OnLongClickListener onTabButtonLongClick = new OnLongClickListener()
	{
		@Override
		public boolean onLongClick(final View v)
		{
			if (I) Log.i(TAG, "onLongClick() onTabButtonClick");
			final Button b = (Button)v;
			final String s = b.getText().toString();
			final int id = Integer.valueOf(s);
			doMoveSelectionLv1(id);
			mClickResult = mSelection;
			mDialog.dismiss();
			return true;
		}
	};


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
		if (mClickResult >= 0) return (CLICKED + mClickResult);
		if (mLongClickResult >= 0) return (LONGCLICKED + mLongClickResult);
		return CANCEL;
	}

	/**
	 * クリック時の応答
	 */
	class MyListClickAdapter implements OnItemClickListener {
		public void onItemClick(final AdapterView<?> adapter, final View view, final int position, final long id) {
			if (I) Log.i(TAG, "onItemClick()");
			mClickResult = position;
			mDialog.dismiss();
		}
	}

	/**
	 * ロングクリック時の応答
	 */
	class MyListLongClickAdapter implements OnItemLongClickListener {
		public boolean onItemLongClick(final AdapterView<?> adapter, final View view, final int position, final long id) {
			if (I) Log.i(TAG, "onItemLongClick()");
			mLongClickResult = position;
			mDialog.dismiss();
			return true;
		}
	}

	/**
	 * リストアイテムを登録
	 * @param pos 登録位置
	 * @param item 登録アイテム
	 * @param lv ショートカットレベル
	 */
	public void setItem(final int pos, final String item, final int lv) {
		if (I) Log.i(TAG, "setListItem()");
		mItemList.set(pos, item);
		mItemShortcutLv.set(pos, lv);
	}

	/**
	 * リストアイテムを末尾に登録
	 * @param item 登録アイテム
	 * @param lv ショートカットレベル
	 */
	public void addItem(final String item, final int lv) {
		if (I) Log.i(TAG, "addListItem()");
		mItemList.add(item);
		mItemShortcutLv.add(lv);
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
	 * リストのレイアウトを設定
	 * @param resourceId リソースID
	 */
	public void setListResource(final int resourceId) {
		if (I) Log.i(TAG, "setListResource()");
		mLayout = resourceId;
	}

	/**
	 * ロングクリックの有効/無効を設定
	 * @param enable 有効にするならtrue
	 */
	public void setLongClickEnable(final boolean enable) {
		if (I) Log.i(TAG, "setLongClickEnable()");
		mEnableLongClick = enable;
	}
}
