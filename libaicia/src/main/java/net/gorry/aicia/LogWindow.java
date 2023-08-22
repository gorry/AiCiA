/**
 * 
 * AiCiA - Android IRC Client
 * 
 * Copyright (C)2010 GORRY.
 * 
 */

package net.gorry.aicia;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 
 * ログウィンドウ
 * 
 * @author GORRY
 *
 */
public class LogWindow {
	private static final String TAG = "LogWindow";
	private static final boolean V = false;
	private static final boolean D = false;//true;
	private static final boolean I = false;//true;
	@SuppressWarnings("unused")
	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	@SuppressWarnings("unused")
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;

	private LogScrollView mScrollView;
	private LogTextView mTextView;
	private final Handler mHandler = new Handler();
	private String mWindowName;
	private int mTextColorResourceId;
	private int mDateColor;
	private int mTextSize = 0;
	private int mLineSpacing = 0;
	private Typeface mTypeface = null;
	private int mLogBufferSize;
	@SuppressWarnings("unused")
	private int mParentLayoutWidth;
	private int mParentLayoutHeight;
	private Layout.LogLinearLayout mParentLayout;
	private final Context me;
	private int mWeight;
	private boolean mStopScroll = false;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 * @param name ウィンドウ名
	 * @param weight weight
	 */
	public LogWindow(final Context context, final String name, final int weight) {
		if (I) Log.i(TAG, "LogWindow()");
		me = context;
		mWindowName = name;
		mWeight = weight;
	}

	/**
	 * ログバッファサイズの設定
	 * ログサイズが設定値の２倍になると、設定値までトリミングされる。
	 * @param size ログバッファサイズ
	 */
	public void setLogBufferSize(final int size) {
		if (I) Log.i(TAG, "setLogBufferSize()");
		mLogBufferSize = size;
	}

	/**
	 * TextViewの取得
	 * @return TextView
	 */
	public TextView getTextView() {
		if (D) Log.d(TAG, "getTextView()");
		return mTextView;
	}

	/**
	 * ScrollViewの取得
	 * @return ScrollView
	 */
	public ScrollView getScrollView() {
		if (D) Log.d(TAG, "getScrollView()");
		return mScrollView;
	}

	/**
	 * ステートセーブ
	 * @param outState ステート情報格納先
	 */
	public void saveInstanceState(final Bundle outState) {
		if (I) Log.i(TAG, "saveInstanceState()");
		//		outState.putCharSequence(mWindowName, mTextView.getText());
	}

	/**
	 * ステートロード
	 * @param inState ステート情報格納先
	 */
	public void restoreInstanceState(final Bundle inState) {
		if (I) Log.i(TAG, "restoreInstanceState()");
		//		mTextView.setText(inState.getCharSequence(mWindowName));
	}

	/**
	 * ウィンドウ名の設定
	 * @param name ウィンドウ名
	 */
	public void setWindowName(final String name) {
		if (I) Log.i(TAG, "setWindowName()");
		mWindowName = name;
	}

	/**
	 * ウィンドウ名の取得
	 * @return ウィンドウ名
	 */
	public String getWindowName() {
		if (D) Log.d(TAG, "getWindowName()");
		return mWindowName;
	}

	/**
	 * テキストフォントサイズの設定
	 * @param size フォントサイズ
	 */
	public void setTextSize(final int size) {
		if (I) Log.i(TAG, "setTextSize()");
		mTextSize = size;
		if (mTextView != null) {
			if (mTextSize > 0) {
				mTextView.setTextSize(mTextSize);
			}
		}
	}

	/**
	 * テキストフォントフェイスの設定
	 * @param face フォントフェイス
	 */
	public void setTypeface(final Typeface face) {
		if (I) Log.i(TAG, "setTypeface()");
		mTypeface = face;
		if (mTextView != null) {
			if (mTypeface != null) {
				mTextView.setTypeface(mTypeface);
			}
		}
	}

	/**
	 * テキスト行間の設定
	 * @param size 行間
	 */
	public void setLineSpacing(final int size) {
		if (I) Log.i(TAG, "setLineSpacing()");
		mLineSpacing = size;
		if (mTextView != null) {
			mTextView.setLineSpacing(mLineSpacing, (float)1.0);
		}
	}

	/**
	 * テキストカラーリソースの設定
	 * @param id テキストカラーリソースID
	 */
	public void setTextColorResource(final int id) {
		if (I) Log.i(TAG, "setTextColorResource()");
		mTextColorResourceId = id;
	}

	/**
	 * 日付時刻カラーの設定
	 * @param color Color.rgb()などによるカラー値
	 */
	public void setDateColor(final int color) {
		if (I) Log.i(TAG, "setDateColor()");
		mDateColor = color;
	}

	/**
	 * ウィンドウ横幅の設定
	 * @param w 横幅
	 */
	public void setWidth(final int w) {
		mTextView.setWidth(w);
	}

	/**
	 * ウィンドウ縦幅の設定
	 * @param h 縦幅
	 */
	public void setHeight(final int h) {
		// 実際は設定禁止
		// mTextView.setHeight(h);
	}

	/**
	 * 親レイアウトの縦横幅を登録
	 * @param w 横幅
	 * @param h 縦幅
	 */
	public void registerParentLayout(final int w, final int h) {
		mParentLayoutWidth = w;
		mParentLayoutHeight = h;
		mParentLayout.updateViewLayout(
				mScrollView,
				new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT,
						LayoutParams.FILL_PARENT,
						mWeight
				)
		);
	}

	/**
	 * フリックでチャンネル移動可能なテキストビュー
	 */
	private class LogTextView extends TextView {
		/**
		 * コンストラクタ
		 * @param context コンテキスト
		 */
		public LogTextView(final Context context) {
			super(context);
		}

		/*
		 * テキストビューのタッチ処理
		 * @see android.widget.TextView#onTouchEvent(android.view.MotionEvent)
		 */
		@Override
		public boolean onTouchEvent(final MotionEvent event) {
			if (D) Log.d(TAG, "LogTextView: onTouchEvent()");
			final boolean result = super.onTouchEvent(event);
			mScrollView.checkStopScrollFlag();
			setStopScroll();
			return mParentLayout.subOnTouchEvent(event, result);
		}
	}

	/**
	 * フリックでチャンネル移動可能なスクロールビュー
	 */
	private class LogScrollView extends ScrollView {
		boolean mCheckStopScroll;
		int mScrollBack1;
		int mScrollBack2;
		int mScrollBack3;
		
		public LogScrollView(Context context) {
			super(context);
		}

		public void checkStopScrollFlag() {
			mCheckStopScroll = true;
		}

		@Override
	    public void computeScroll() {
	    	super.computeScroll();
			if (D) Log.d(TAG, "LogScrollView: computeScroll()");

			// スクロールが端まで来たらsetStopScroll()の判定をする
	    	mScrollBack3 = mScrollBack2;
	    	mScrollBack2 = mScrollBack1;
	    	mScrollBack1 = getScrollY();
			if (D) Log.d(TAG, "LogScrollView: computeScroll(): "+String.format("mScrollBack1=%d, mScrollBack2=%d, mScrollBack3=%d", mScrollBack1, mScrollBack2, mScrollBack3));
	    	if ((mScrollBack3 != mScrollBack2) && (mScrollBack2 == mScrollBack1)) {
	    		if (mCheckStopScroll) {
	    			mCheckStopScroll = false;
	    			setStopScroll();
	    		}
	        }
	    }

		/*
		 * スクロールビューのタッチ処理
		 * @see android.widget.ScrollView#onTouchEvent(android.view.MotionEvent)
		 */
		@Override
		public boolean onTouchEvent(final MotionEvent event) {
			if (D) Log.d(TAG, "LogScrollView: onTouchEvent()");
			final boolean result = super.onTouchEvent(event);
			mScrollView.checkStopScrollFlag();
			setStopScroll();
			return mParentLayout.subOnTouchEvent(event, result);
		}
	}
	
	/**
	 * ログウィンドウを作成
	 * @param context コンテキスト
	 * @param parentLayout 親レイアウト
	 */
	public void createWindow(final Context context, final Layout.LogLinearLayout parentLayout) {
		if (I) Log.i(TAG, "createWindow()");
		mParentLayout = parentLayout;
		mScrollView = new LogScrollView(context);
		mScrollView.setLayoutParams(
				new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT,
						LayoutParams.FILL_PARENT,
						mWeight
				)
		);
		parentLayout.addView(mScrollView);

		{
			mTextView = new LogTextView(context);
			mTextView.setLayoutParams(
					new LinearLayout.LayoutParams(
							LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT,
							mWeight
					)
			);
			if (mTextSize > 0) {
				mTextView.setTextSize(mTextSize);
			}
			mTextView.setLineSpacing(mLineSpacing, (float)1.0);
			if (mTypeface != null) {
				mTextView.setTypeface(mTypeface);
			}
			// textView.setAutoLinkMask(Linkify.ALL);
			final MovementMethod movementmethod = LinkMovementMethod.getInstance();
			mTextView.setMovementMethod(movementmethod);
			mTextView.setFocusableInTouchMode(true);

			final Resources resources = me.getResources();
			final XmlResourceParser parser = resources.getXml(mTextColorResourceId);
			ColorStateList colors = null;
			try {
				colors = ColorStateList.createFromXml(resources, parser);
			} catch (final XmlPullParserException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			mTextView.setTextColor(colors);

			mScrollView.addView(mTextView);

			mTextView.setKeyListener(onTextViewKey);
		}
	}
	
	/**
	 * ビューのオートスクロール停止フラグ処理
	 */
	private void setStopScroll() {
		if (D) Log.d(TAG, "setStopScroll()");
		final int svScrollY = mScrollView.getScrollY();
		final int svHeight = mScrollView.getHeight();
		final int tvHeight = mTextView.getHeight();
		mStopScroll = (svScrollY+svHeight != tvHeight);
		if (mStopScroll) {
			if (D) Log.d(TAG, String.format("mStopScroll=%d", (mStopScroll ? 1 : 0)));
		}
		if (D) Log.d(TAG, "onScrollViewTouch: onTouch(): "+String.format("svScrollY=%d, svHeight=%d, tvHeight=%d", svScrollY, svHeight, tvHeight));
	}

	/**
	 * スクロールビューを再上段まで移動
	 */
	public void setScrollToHead() {
		if (I) Log.i(TAG, "setScrollToHead()");
		final Runnable scroller = new Runnable() {
			public void run() {
				mScrollView.smoothScrollTo(0, 0);
			}
		};
		scroller.run();
		mStopScroll = false;
	}

	/**
	 * スクロールビューを再下段まで移動
	 */
	public void setScrollToTail() {
		if (I) Log.i(TAG, "setScrollToTail()");
		final Runnable scroller = new Runnable() {
			public void run() {
				mScrollView.smoothScrollTo(0, mTextView.getHeight() - mScrollView.getHeight());
			}
		};
		scroller.run();
		mStopScroll = false;
	}

	/**
	 * ログウィンドウの破棄
	 */
	public void destroyWindow() {
		if (I) Log.i(TAG, "destroyWindow()");
		if (mParentLayout != null) {
			mParentLayout.removeAllViews();
			mTextView = null;
			mScrollView = null;
		}
	}


	/**
	 * ログウィンドウのキーリスナ処理
	 */
	private final KeyListener onTextViewKey = new KeyListener() {
		public int getInputType() {
			return 0;
		}

		public boolean onKeyDown(final View view, final Editable text, final int keyCode, final KeyEvent event) {
			/*
			if (keyCode == KeyEvent.KEYCODE_ENTER) {
				ActivityMain.layout.mInputBox.requestFocus();
				return true;
			}
			 */
			if (EditAssist.isAnyKey(keyCode)) {
				ActivityMain.layout.mInputBox.requestFocus();
				return true;
			}
			final boolean k = ActivityMain.doMain.doShortcutKey(keyCode, event);
			if (k == true) return true;
			return false;
		}

		public boolean onKeyUp(final View view, final Editable text, final int keyCode, final KeyEvent event) {
			setStopScroll();
			return false;
		}

		public boolean onKeyOther(final View view, final Editable text, final KeyEvent event) {
			return false;
		}

		public void clearMetaKeyState(final View view, final Editable content, final int states) {
			return;
		}

	};

	/**
	 * ログウィンドウにSpanログを登録
	 * @param ssb Spanログ
	 */
	public synchronized void setMessage(final SpannableStringBuilder ssb) {
		if (D) Log.d(TAG, "setMessage()");
		final Runnable scroller = new Runnable() {
			public void run() {
				if (D) Log.d(TAG, "scroller start");
				mScrollView.scrollTo(0, mTextView.getHeight() - mScrollView.getHeight());
				if (D) Log.d(TAG, "scroller end");
			}
		};
		final Runnable setter = new Runnable() {
			public void run() {
				if (D) Log.d(TAG, "setter start");
				mTextView.setText(ssb);
				if (D) Log.d(TAG, "setter end");
				mHandler.post(scroller);
			}
		};
		mHandler.post(setter);
	}

	/**
	 * ログウィンドウにSpanログを追加
	 * @param ssb Spanログ
	 */
	public synchronized void addMessage(final SpannableStringBuilder ssb) {
		if (D) Log.d(TAG, "addMessage()");
		final int tvHeight = mTextView.getHeight();
		final int svHeight = mScrollView.getHeight();
		final int svScrollY = mScrollView.getScrollY();
		@SuppressWarnings("unused")
		final int scrollViewOldY = tvHeight - svHeight;
		if (D) Log.d(TAG, String.format("tvHeight=%d, svScrollY=%d, svHeight=%d, mParentLayoutHeight=%d", tvHeight, svScrollY, svHeight, mParentLayoutHeight));
		final boolean viewOldLog = (tvHeight-svScrollY-svHeight > mParentLayoutHeight);  // １画面以上戻っていたらスクロールしない
		final Runnable scroller = new Runnable() {
			public void run() {
				// 行が増えて値が変わるので取り直し
				if (D) Log.d(TAG, "scroller start");
				mScrollView.smoothScrollTo(0, mTextView.getHeight() - mScrollView.getHeight());
				if (D) Log.d(TAG, "scroller end");
			}
		};
		final Runnable adder = new Runnable() {
			public void run() {
				if (mTextView.length() > mLogBufferSize) {
					// バッファトリミング
					if (D) Log.d(TAG, "get start");
					final CharSequence buf = mTextView.getText();
					if (D) Log.d(TAG, "get end");
					if (D) Log.d(TAG, "trim start");
					int start = buf.length()/2;
					final int end = buf.length();
					for (;start<end-1; start++) {
						if (buf.charAt(start) == '\n') {
							start++;
							break;
						}
					}
					if (D) Log.d(TAG, "trim end");
					if (D) Log.d(TAG, "trim set start");
					mTextView.setText(buf.subSequence(start, end));
					if (D) Log.d(TAG, "trim set end");
				}
				if (D) Log.d(TAG, "adder start");
				mTextView.append(ssb);
				if (D) Log.d(TAG, "adder end");
//				if (!viewOldLog) {
				if (!mStopScroll) {
					mHandler.post(scroller);
				}
			}
		};
		if (ssb == null) {
			if (!viewOldLog) {
				mHandler.post(scroller);
			}
		} else {
			mHandler.post(adder);
		}
	}

	/**
	 * ログウィンドウにテキストメッセージを追加
	 * @param message テキストメッセージ
	 * @param color テキストメッセージの色
	 * @param dateMsg 日付時刻
	 */
	public synchronized void addMessage(final String message, final int color, final String dateMsg) {
		if (D) Log.d(TAG, "addMessage()");
		final int tvHeight = mTextView.getHeight();
		final int svHeight = mScrollView.getHeight();
		final int svScrollY = mScrollView.getScrollY();
		@SuppressWarnings("unused")
		final int scrollViewOldY = tvHeight - svHeight;
		if (D) Log.d(TAG, String.format("tvHeight=%d, svScrollY=%d, svHeight=%d, mParentLayoutHeight=%d", tvHeight, svScrollY, svHeight, mParentLayoutHeight));
		final boolean viewOldLog = (tvHeight-svScrollY-svHeight > mParentLayoutHeight);  // １画面以上戻っていたらスクロールしない
		final Runnable scroller = new Runnable() {
			public void run() {
				// 行が増えて値が変わるので取り直し
				mScrollView.smoothScrollTo(0, mTextView.getHeight() - mScrollView.getHeight());
			}
		};
		final Runnable adder = new Runnable() {
			public void run() {
				SpannableStringBuilder span = null;
				String msg = message + "\n";
				switch (SystemConfig.dateMode) {
				default:
				case 0:
				{
					span = new SpannableStringBuilder(msg);
					final ForegroundColorSpan c = new ForegroundColorSpan(color);
					span.setSpan(c, 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					break;
				}
				case 1:
				{
					msg = dateMsg + " " + message + "\n";
					span = new SpannableStringBuilder(msg);
					final ForegroundColorSpan c2 = new ForegroundColorSpan(color);
					span.setSpan(c2, 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					final ForegroundColorSpan c = new ForegroundColorSpan(mDateColor);
					span.setSpan(c, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					break;
				}
				}
				Linkify.addLinks(span, Linkify.WEB_URLS);
				if (mTextView.length() > mLogBufferSize) {
					// バッファトリミング
					if (D) Log.d(TAG, "get start");
					final CharSequence buf = mTextView.getText();
					if (D) Log.d(TAG, "get end");
					if (D) Log.d(TAG, "trim start");
					int start = buf.length()/2;
					final int end = buf.length();
					for (;start<end-1; start++) {
						if (buf.charAt(start) == '\n') {
							start++;
							break;
						}
					}
					if (D) Log.d(TAG, "trim end");
					if (D) Log.d(TAG, "trim set start");
					mTextView.setText(buf.subSequence(start, end));
					if (D) Log.d(TAG, "trim set end");
				}
				if (D) Log.d(TAG, "adder start");
				mTextView.append(span);
				if (D) Log.d(TAG, "adder end");
//				if (!viewOldLog) {
				if (!mStopScroll) {
					mHandler.post(scroller);
				}
			}
		};
		mHandler.post(adder);
	}


	/**
	 * ログのクリア
	 */
	public synchronized void clearLog() {
		final Runnable clearer = new Runnable() {
			public void run() {
				mTextView.setText("");
			}
		};
		mHandler.post(clearer);
	}


}
