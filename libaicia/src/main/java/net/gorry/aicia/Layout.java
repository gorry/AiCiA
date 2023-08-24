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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import net.gorry.libaicia.R;

/**
 * @author GORRY
 *
 */
public class Layout {
	private static final String TAG = "Layout";
	private static final boolean V = false;
	private static final boolean D = false;//true;
	private static final boolean I = false;//true;
	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;
	private final ActivityMain me;
	private IIRCService iIRCService;

	private LogLinearLayout mMainLogLayout;
	private LogLinearLayout mSubLogLayout;
	private LinearLayout mLogLayout;
	private LinearLayout mCmdLayout;
	private Button mChannelButton;
	private Button mChPrevButton;
	private Button mChNextButton;
	private Button mUserButton;
	private Button mAppliButton;
	private Button mWebsiteButton;

	private int mBaseLayoutWidth;
	@SuppressWarnings("unused")
	private int mBaseLayoutHeight;
	private int mLogLayoutWidth;
	private int mLogLayoutHeight;

	private Boolean mNotRestore = true;

	private KeyListener mInputBoxDefaultListener;

	/** */
	public LinearLayout mBaseLayout;
	/** */
	public LogWindow mMainLogWindow;
	/** */
	public LogWindow mSubLogWindow;
	/** */
	public EditText mInputBox;

	private MotionEvent mLastMotionEvent;
	private boolean mCancelLongClickView = false;

	/**
	 * コンストラクタ
	 * @param a アクティビティインスタンス
	 */
	Layout(final ActivityMain a) {
		me = a;
	}

	/**
	 * iIRCServiceの登録
	 * @param i iIRCService
	 */
	public void setIIRCService(final IIRCService i) {
		iIRCService = i;
	}

	/**
	 *  アプリの一時退避
	 * @param outState 退避先
	 */
	public void saveInstanceState(final Bundle outState) {
		if (I) Log.i(TAG, "saveInstanceState()");
		mMainLogWindow.saveInstanceState(outState);
		mSubLogWindow.saveInstanceState(outState);
		outState.putCharSequence("mInputBox", mInputBox.getText());
		outState.putInt("mInputBox_selStart", mInputBox.getSelectionStart());
		outState.putInt("mInputBox_selEnd", mInputBox.getSelectionEnd());
	}

	/**
	 * アプリの一時退避復元
	 * @param savedInstanceState 復元元
	 */
	public void restoreInstanceState(final Bundle savedInstanceState) {
		if (I) Log.i(TAG, "restoreInstanceState()");
		mMainLogWindow.restoreInstanceState(savedInstanceState);
		mSubLogWindow.restoreInstanceState(savedInstanceState);
		mInputBox.setText(savedInstanceState.getCharSequence("mInputBox"));
		mInputBox.setSelection(savedInstanceState.getInt("mInputBox_selStart"), savedInstanceState.getInt("mInputBox_selEnd"));
		mInputBox.requestFocus();
		mNotRestore = false;
	}

	/**
	 * 回転方向の設定
	 * @param isFirst 初期化のときtrue
	 * @return 回転方向が変わったらtrue
	 */
	public boolean setOrientation(final boolean isFirst) {
		if (I) Log.i(TAG, "setOrientation()");
		final boolean lastOrientation = SystemConfig.getOrientation();
		final boolean isLandscape = (me.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
		switch (SystemConfig.rotateMode) {
		case 0:  // 常に画面の向きに合わせる
			SystemConfig.setOrientation(isLandscape);
			break;
		case 1:  // スタート時の画面の向きで固定
			if (isFirst) {
				SystemConfig.setOrientation(isLandscape);
			}
			break;
		case 2:  // 縦向き表示で固定
			SystemConfig.setOrientation(false);
			break;
		case 3:  // 横向き表示で固定
			SystemConfig.setOrientation(true);
			break;
		}
		final boolean nowOrientation = SystemConfig.getOrientation();
		return (lastOrientation != nowOrientation);
	}

	/**
	 * 回転モードの設定
	 */
	public void setRotateMode() {
		if (I) Log.i(TAG, "setRotateMode()");
		final boolean isLandscape = SystemConfig.getOrientation();
		int mode = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		switch (SystemConfig.rotateMode) {
		default:
		case 0:
			break;
		case 1:
			if (isLandscape) {
				mode = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			} else {
				mode = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			}
			break;
		case 2:
			mode = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			break;
		case 3:
			mode = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			break;
		}

		if (SystemConfig.canRotate180) {
			if (Build.VERSION.SDK_INT >= 9) {
				switch (mode) {
				case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
					mode = 6;  // ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
					break;
				case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
					mode = 7;  // ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
					break;
				}
			}
		}
		me.setRequestedOrientation(mode);
	}

	/**
	 * ベースレイアウトのアップデート
	 */
	public void updateBaseLayout() {
		if (I) Log.i(TAG, "updateBaseLayout()");
		mBaseLayout.updateViewLayout(mLogLayout, new LinearLayout.LayoutParams(FP, FP, 1));
	}

	/**
	 * ベースレイアウト作成
	 * @param isFirst 最初ならtrue
	 */
	public synchronized void baseLayout_Create(final boolean isFirst) {
		if (I) Log.i(TAG, "baseLayout_Create()");
		Editable input = null;
		int selStart = 0;
		int selEnd = 0;
		if (mBaseLayout != null) {
			if (mInputBox != null) {
				input = mInputBox.getText();
				selStart = mInputBox.getSelectionStart();
				selEnd = mInputBox.getSelectionEnd();
			}
			mBaseLayout.removeAllViews();
			logLayout_Create(mBaseLayout);
			cmdLayout_Create(mBaseLayout);
		} else {
			mBaseLayout = new LinearLayout(me) {
				@Override
				protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
					if (I) Log.i(TAG, "onSizeChanged() baseLayout");
					mBaseLayoutWidth = w;
					mBaseLayoutHeight = h;
				}
			};
			mBaseLayout.setOrientation(LinearLayout.VERTICAL);
			mBaseLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP));
			logLayout_Create(mBaseLayout);
			cmdLayout_Create(mBaseLayout);
		}
		if (input != null) {
			mInputBox.setText(input);
			mInputBox.setSelection(selStart, selEnd);
			mInputBox.requestFocus();
		}
	}

	/**
	 * フリックでチャンネル移動可能なレイアウト
	 */
	public class LogLinearLayout extends LinearLayout {
		private int mXOnTouch = 0;
		private int mYOnTouch = 0;
		private boolean mShift = false;
		private boolean mFlickVertical = false;
		private boolean mShortFlickHorizontal = false;
		private boolean mLongFlickHorizontal = false;
		private boolean mFlickRight = false;
		private boolean mFlickLeft = false;
		private long mLastClickTime = 0;
		private Runnable mActionOnDoubleTap = null;
		
		// ロングクリック処理
		private Runnable mActionOnLongClick = null;
		private final int mTimeOutLongClick = 500;
		private final Handler mHandlerLongClick = new Handler();
		private final Runnable mActionLongClick = new Runnable() {
			public void run() {
				synchronized (this) {
					if (I) Log.i(TAG, "LogLinearLayout: mActionLongClick()");
					if (mActionOnLongClick != null) {
						final Vibrator vib = (Vibrator)me.getSystemService(Context.VIBRATOR_SERVICE);
						new Thread(new Runnable() {	public void run() {
							vib.vibrate(50);
						} }).start();
						mActionOnLongClick.run();
					}
				}
			}
		};

		/**
		 * コンストラクタ
		 * @param context コンテキスト
		 */
		public LogLinearLayout(final Context context) {
			super(context);
		}

		/*
		 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
		 */
		@Override
		public boolean onTouchEvent(final MotionEvent event) {
			// ここには届いてないっぽい。ScrollViewのほうに届く
			if (D) Log.d(TAG, "LogLinearLayout: onTouchEvent()");
			final boolean result = super.onTouchEvent(event);
			return subOnTouchEvent(event, result);
		}

		/**
		 * ロングクリック時のコールバック登録
		 * @param r runnable
		 */
		public void setActionOnLongClick(Runnable r) {
			mActionOnLongClick = r;
		}
		
		/**
		 * ダブルタップ時のコールバック登録
		 * @param r runnable
		 */
		public void setActionOnDoubleTap(Runnable r) {
			mActionOnDoubleTap = r;
		}
		
		/**
		 * レイアウトのタッチイベント処理
		 * @param event タッチイベント
		 * @param result super()での返り値
		 * @return 返すべき値
		 */
		public boolean subOnTouchEvent(final MotionEvent event, final boolean result) {
			mLastMotionEvent = event;
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			{
				mXOnTouch = (int)event.getRawX();
				mYOnTouch = (int)event.getRawY();
				mShift = false;
				mFlickVertical = false;
				mShortFlickHorizontal = false;
				mLongFlickHorizontal = false;
				mCancelLongClickView = false;
				mFlickLeft = false;
				mFlickRight = false;
				ActivityMain.addVerboseLog(5, TAG, "ACTION_DOWN: X=" + mXOnTouch + ",Y="  + mYOnTouch);
				mHandlerLongClick.postDelayed(mActionLongClick, mTimeOutLongClick);
				return true;
			}
			case MotionEvent.ACTION_OUTSIDE:
			{
				mHandlerLongClick.removeCallbacks(mActionLongClick);
				mShift = true;
				ActivityMain.addVerboseLog(5, TAG, "ACTION_OUTSIDE: X=" + mXOnTouch + ",Y="  + mYOnTouch);
				return true;
			}
			case MotionEvent.ACTION_UP:
			{
				mHandlerLongClick.removeCallbacks(mActionLongClick);
				final int xOnRelease = (int)event.getRawX();
				final int yOnRelease = (int)event.getRawY();
				final int xMove = xOnRelease - mXOnTouch;
				final int yMove = yOnRelease - mYOnTouch;
				@SuppressWarnings("unused")
				final int smallWidth = (mLogLayoutWidth > mLogLayoutHeight) ? mLogLayoutWidth : mLogLayoutHeight;
				final int edge = event.getEdgeFlags();
				ActivityMain.addVerboseLog(5, TAG, "ACTION_UP: X=" + xOnRelease + ",Y=" + yOnRelease + ",W=" + xMove + ",H=" + yMove + ",EDGE=" + edge);
				if (mFlickVertical) {
					return true;
				}
				if (mLongFlickHorizontal) {
					boolean changed = false;
					if (mFlickRight) {
						ActivityMain.addVerboseLog(5, TAG, "longFlickHorizontal: flickRight");
						changed = ActivityMain.doMain.doChangeNextChannel(-1, 1, false);
					} else if (mFlickLeft) {
						ActivityMain.addVerboseLog(5, TAG, "longFlickHorizontal: flickLeft");
						changed = ActivityMain.doMain.doChangeNextChannel(1, 1, false);
					}
					if (!changed) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notupdated));
					}
					return true;
				}
				if (mShortFlickHorizontal) {
					boolean changed = false;
					if (mFlickRight) {
						final int f = (mShift ? 1 : 0);
						ActivityMain.addVerboseLog(5, TAG, "shortFlickHorizontal: flickRight");
						changed = ActivityMain.doMain.doChangeNextChannel(-1, f, false);
					} else if (mFlickLeft) {
						final int f = (mShift ? 1 : 0);
						ActivityMain.addVerboseLog(5, TAG, "shortFlickHorizontal: flickLeft");
						changed = ActivityMain.doMain.doChangeNextChannel(1, f, false);
					}
					if (!changed) {
						ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notchanged));
					}
					return true;
				}
				int xLimit = SystemConfig.limitLongClickMove;
				int yLimit = SystemConfig.limitLongClickMove;
				if ((Math.abs(xMove) < xLimit) && (Math.abs(yMove) < yLimit)) {
					long t = event.getEventTime();
					if (t-mLastClickTime < 500) {
						if (mActionOnDoubleTap != null) {
							mActionOnDoubleTap.run();
							return true;
						}
					}
					mLastClickTime = t;
				}
				mXOnTouch = xOnRelease;
				mYOnTouch = yOnRelease;
				mShift = false;
				mFlickVertical = false;
				mShortFlickHorizontal = false;
				mLongFlickHorizontal = false;
				mCancelLongClickView = false;
				mFlickLeft = false;
				mFlickRight = false;
				return result;
			}
			case MotionEvent.ACTION_MOVE:
			{
				final int xOnMove = (int)event.getRawX();
				final int yOnMove = (int)event.getRawY();
				final int xMove = xOnMove - mXOnTouch;
				final int yMove = yOnMove - mYOnTouch;
				final int smallWidth = (mLogLayoutWidth > mLogLayoutHeight) ? mLogLayoutWidth : mLogLayoutHeight;
				final int edge = event.getEdgeFlags();
				ActivityMain.addVerboseLog(5, TAG, "ACTION_MOVE: X=" + xOnMove + ",Y=" + yOnMove + ",W=" + xMove + ",H=" + yMove + ",EDGE=" + edge);
				if (!mCancelLongClickView) {
					int xLimit = SystemConfig.limitLongClickMove;
					int yLimit = SystemConfig.limitLongClickMove;
					if ((Math.abs(xMove) > xLimit) || (Math.abs(yMove) > yLimit)) {
						ActivityMain.addVerboseLog(5, TAG, "cancel LongClickView");
						mCancelLongClickView = true;
						mHandlerLongClick.removeCallbacks(mActionLongClick);
					}
				}
				if (Math.abs(xMove) > Math.abs(yMove)) {
					if (!mLongFlickHorizontal && !mFlickVertical && (Math.abs(xMove) >= smallWidth/2)) {
						mLongFlickHorizontal = true;
						ActivityMain.addVerboseLog(5, TAG, "sense longFlickHorizontal");
						if (xMove > 0) {
							mFlickRight = true;
						} else {
							mFlickLeft = true;
						}
						final Vibrator vib = (Vibrator)me.getSystemService(Context.VIBRATOR_SERVICE);
						if (mShortFlickHorizontal) {
							mShortFlickHorizontal = false;
							new Thread(new Runnable() {	public void run() {
								vib.vibrate(50);
							} }).start();
						} else {
							new Thread(new Runnable() {	public void run() {
								final long[] pat = {0,50,250,50};
								vib.vibrate(pat, -1);
							} }).start();
						}
					}
					if (!mShortFlickHorizontal && !mLongFlickHorizontal && !mFlickVertical && (Math.abs(xMove) >= smallWidth/8)) {
						mShortFlickHorizontal = true;
						ActivityMain.addVerboseLog(5, TAG, "sense shortFlickHorizontal");
						if (xMove > 0) {
							mFlickRight = true;
						} else {
							mFlickLeft = true;
						}
						new Thread(new Runnable() {	public void run() {
							final Vibrator vib = (Vibrator)me.getSystemService(Context.VIBRATOR_SERVICE);
							vib.vibrate(50);
						} }).start();
					}
				} else {
					if (!mShortFlickHorizontal && !mLongFlickHorizontal && !mFlickVertical && (Math.abs(yMove) >= smallWidth/8)) {
						mFlickVertical = true;
						ActivityMain.addVerboseLog(5, TAG, "sense FlickVertical");
					}
				}
				return result;
			}
			}
			return result;
		}

	}

	/**
	 * ログウィンドウレイアウト作成
	 * @param parentLayout 親レイアウト
	 */
	public synchronized void logLayout_Create(final LinearLayout parentLayout) {
		if (I) Log.i(TAG, "logLayout_Create()");

		// logLayout
		mLogLayout = new LinearLayout(me) {
			@Override
			protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
				if (I) Log.i(TAG, "logLayout_Create(): onSizeChanged(): "+String.format("w=%d, h=%d, oldw=%d, oldh=%d", w, h, oldw, oldh));
				mLogLayoutWidth = w;
				mLogLayoutHeight = h;

				// if (SystemConfig.isLandscape) {
				if (SystemConfig.now_horizontalMode) {
/*
					// FPの再計算のせいか遅いので使わない
					float we1;
					float we2;
					switch (SystemConfig.now_subWindowMode) {
					case 0:
						we1 = 1;
						we2 = 0;
						break;
					case 1:
						we1 = 2;
						we2 = 1;
						break;
					default:
					case 2:
						we1 = 1;
						we2 = 1;
						break;
					case 3:
						we1 = 1;
						we2 = 2;
						break;
					}
					mSubLogLayout.updateViewLayout(mSubLogWindow.getScrollView(), new LinearLayout.LayoutParams(FP, FP));
					mLogLayout.updateViewLayout(mSubLogLayout, new LinearLayout.LayoutParams(FP, FP, we1));

					mMainLogLayout.updateViewLayout(mMainLogWindow.getScrollView(), new LinearLayout.LayoutParams(FP, FP));
					mLogLayout.updateViewLayout(mMainLogLayout, new LinearLayout.LayoutParams(FP, FP, we2));
*/
					int w1;
					switch (SystemConfig.now_subWindowMode) {
					case 0:
						w1 = 0;
						break;
					case 1:
						w1 = mLogLayoutWidth/3;
						break;
					default:
					case 2:
						w1 = mLogLayoutWidth/2;
						break;
					case 3:
						w1 = mLogLayoutWidth/3*2;
						break;
					}
					mSubLogWindow.setWidth(w1);
					mSubLogLayout.updateViewLayout(mSubLogWindow.getScrollView(), new LinearLayout.LayoutParams(w1, FP, 5));
					mLogLayout.updateViewLayout(mSubLogLayout, new LinearLayout.LayoutParams(w1, FP, 4));

					final int w2 = mLogLayoutWidth-w1;
					mMainLogWindow.setWidth(w2);
					mMainLogLayout.updateViewLayout(mMainLogWindow.getScrollView(), new LinearLayout.LayoutParams(w2, FP, 3));
					mLogLayout.updateViewLayout(mMainLogLayout, new LinearLayout.LayoutParams(w2, FP, 2));
				} else {
					float we1;
					float we2;
					switch (SystemConfig.now_subWindowMode) {
					case 0:
						we1 = 1;
						we2 = 0;
						break;
					case 1:
						we1 = 2;
						we2 = 1;
						break;
					default:
					case 2:
						we1 = 1;
						we2 = 1;
						break;
					case 3:
						we1 = 1;
						we2 = 2;
						break;
					}
					mSubLogLayout.updateViewLayout(mSubLogWindow.getScrollView(), new LinearLayout.LayoutParams(FP, FP));
					mLogLayout.updateViewLayout(mSubLogLayout, new LinearLayout.LayoutParams(FP, FP, we1));

					mMainLogLayout.updateViewLayout(mMainLogWindow.getScrollView(), new LinearLayout.LayoutParams(FP, FP));
					mLogLayout.updateViewLayout(mMainLogLayout, new LinearLayout.LayoutParams(FP, FP, we2));
				}
				mBaseLayout.updateViewLayout(mLogLayout, new LinearLayout.LayoutParams(mBaseLayoutWidth, FP, 1));
				mMainLogWindow.addMessage(null);
				mSubLogWindow.addMessage(null);
			}
		};

		// if (SystemConfig.isLandscape) {
		if (SystemConfig.now_horizontalMode) {
			mLogLayout.setOrientation(LinearLayout.HORIZONTAL);
		} else {
			mLogLayout.setOrientation(LinearLayout.VERTICAL);
		}
		mLogLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP, 1));
		parentLayout.addView(mLogLayout);

		{
			// mainLogLayout
		}
		mMainLogLayout = new LogLinearLayout(me) {
			@Override
			protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
				if (I) Log.i(TAG, "onSizeChanged() mainLogLayout");
				mMainLogWindow.registerParentLayout(w, h);
			}
		};
		mMainLogLayout.setOrientation(LinearLayout.HORIZONTAL);
		mMainLogLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP, 3));
		mMainLogLayout.setBackgroundColor(SystemConfig.mainLogBackgroundColor[SystemConfig.now_colorSet]);
		mMainLogLayout.setActionOnLongClick(mOnMainLogTextViewLongClick);
		mMainLogLayout.setActionOnDoubleTap(mOnMainLogTextViewDoubleTap);
		// mLogLayout.addView(mMainLogLayout);
		mMainLogWindow = new LogWindow(me, "MainLog", 2);
		mMainLogWindow.setTextSize(SystemConfig.now_mainLogFontSize);
		mMainLogWindow.setTypeface(SystemConfig.mainLogTypeface);
		mMainLogWindow.setLineSpacing(SystemConfig.now_mainLogLineSpacing);
		mMainLogWindow.setDateColor(SystemConfig.mainLogDateColor[SystemConfig.now_colorSet]);
		mMainLogWindow.setTextColorResource(R.color.textcolor_mainlog);
		mMainLogWindow.setLogBufferSize(SystemConfig.mainLogBufferSize);
		mMainLogWindow.createWindow(me, mMainLogLayout);

		// subLogLayout
		mSubLogLayout = new LogLinearLayout(me) {
			@Override
			protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
				if (I) Log.i(TAG, "onSizeChanged() subLogLayout");
				mSubLogWindow.registerParentLayout(w, h);
			}
		};
		mSubLogLayout.setOrientation(LinearLayout.HORIZONTAL);
		mSubLogLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP, 5));
		mSubLogLayout.setBackgroundColor(SystemConfig.subLogBackgroundColor[SystemConfig.now_colorSet]);
		mSubLogLayout.setActionOnLongClick(mOnSubLogTextViewLongClick);
		mSubLogLayout.setActionOnDoubleTap(mOnSubLogTextViewDoubleTap);
		// mLogLayout.addView(mSubLogLayout);
		mSubLogWindow = new LogWindow(me, "SubLog", 4);
		mSubLogWindow.setTextSize(SystemConfig.now_subLogFontSize);
		mSubLogWindow.setTypeface(SystemConfig.subLogTypeface);
		mSubLogWindow.setLineSpacing(SystemConfig.now_subLogLineSpacing);
		mSubLogWindow.setDateColor(SystemConfig.subLogDateColor[SystemConfig.now_colorSet]);
		mSubLogWindow.setTextColorResource(R.color.textcolor_sublog);
		mSubLogWindow.setLogBufferSize(SystemConfig.subLogBufferSize);
		mSubLogWindow.createWindow(me, mSubLogLayout);

		if (SystemConfig.now_swapWindowMode) {
			mLogLayout.addView(mSubLogLayout);
			mLogLayout.addView(mMainLogLayout);
		} else {
			mLogLayout.addView(mMainLogLayout);
			mLogLayout.addView(mSubLogLayout);
		}

		if (SystemConfig.now_horizontalMode) {
			mSubLogWindow.getTextView().setPadding(SystemConfig.now_subLogFontSize/4, 0, 0, 0);
		}

		if ( mNotRestore ) {
			mMainLogWindow.addMessage("Main-log", SystemConfig.mainLogTextColor[SystemConfig.now_colorSet], IRCMsg.getDateMsg());
			mMainLogWindow.addMessage("Push [MENU] to start IRC.", SystemConfig.mainLogTextColor[SystemConfig.now_colorSet], IRCMsg.getDateMsg());
		}
	}

	private void pushHistoryInputBoxMessage() {
		if (I) Log.i(TAG, "pushHistoryInputBoxMessage()");
		ActivityMain.doMain.doPushInputHistory();
	}

	private void getHistoryInputBoxMessage() {
		if (I) Log.i(TAG, "getHistoryInputBoxMessage()");
		ActivityMain.doMain.doInputHistoryMenu();
	}


	final GestureDetector mInputBoxGestureDetector = new GestureDetector(
			new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
					int disX = (int)(e2.getX()-e1.getX());
					int disY = (int)(e2.getY()-e1.getY());
					if (I) Log.i(TAG, "mInputBoxGestureDetector: onFling(): disX=" + disX + " disY=" + disY + " X=" + velocityX + " Y=" + velocityY);
					int height = mInputBox.getHeight();
					if (Math.abs(disY) >= height) {
						if (disY > 0) {
							pushHistoryInputBoxMessage();
						} else {
							getHistoryInputBoxMessage();
						}
					}
					return true;
				}

				@Override
				public boolean onSingleTapConfirmed(MotionEvent e1) {
					if (I) Log.i(TAG, "mInputBoxGestureDetector: onSingleTapConfirmed()");
					return true;
				}

				@Override
				public boolean onDoubleTap(MotionEvent e1) {
					if (I) Log.i(TAG, "mInputBoxGestureDetector: onDoubleTap()");
					getHistoryInputBoxMessage();
					return true;
				}

			}
	);

	/**
	 *
	 */
	public final OnTouchListener onTouchInputBox = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if (I) Log.i(TAG, "onTouchInputBox()");
			return mInputBoxGestureDetector.onTouchEvent(event);
		}
	};

	/**
	 * コマンドラインレイアウト作成
	 * @param parentLayout 親レイアウト
	 */
	public synchronized void cmdLayout_Create(final LinearLayout parentLayout) {
		if (I) Log.i(TAG, "cmdLayout_Create()");

		int w = (int)((SystemConfig.now_buttonWide ? 1.5 : 1.0) *me.getResources().getDimension(R.dimen.button_widthunit)*SystemConfig.now_buttonSize);
		// cmdLayout;
		mCmdLayout = new LinearLayout(me);
		mCmdLayout.setOrientation(LinearLayout.HORIZONTAL);
		mCmdLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		parentLayout.addView(mCmdLayout);

		{
			// inputBox;
			mInputBox = new EditText(me);
			mInputBox.setLayoutParams(new LinearLayout.LayoutParams(WC, WC, 1));
			if (SystemConfig.now_editFontSize > 0) {
				mInputBox.setTextSize(SystemConfig.now_editFontSize);
				mInputBox.setTypeface(SystemConfig.editTypeface);
				// minputBox.setHeight(SystemConfig.now_editFontSize);
			}
			mInputBoxDefaultListener = mInputBox.getKeyListener();
			mInputBox.setKeyListener(onInputBoxKey);
			mInputBox.setFreezesText(true);
			mInputBox.setOnTouchListener(onTouchInputBox);
			mCmdLayout.addView(mInputBox);

			// ChPrevButton
			mChPrevButton = new Button(me);
			mChPrevButton.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
			mChPrevButton.setText("↑");
			if (SystemConfig.now_editFontSize > 0) {
				mChPrevButton.setTextSize(SystemConfig.now_buttonSize);
				mChPrevButton.setMinimumWidth(w);
				mChPrevButton.setMinWidth(24);
			}
			mChPrevButton.setOnClickListener(onChPrevButtonClick);
			mChPrevButton.setOnLongClickListener(onChPrevButtonLongClick);
			if (SystemConfig.now_buttonChMove) {
				mCmdLayout.addView(mChPrevButton);
			}
			mChPrevButton.setKeyListener(onButtonKey);


			// channelButton
			mChannelButton = new Button(me);
			mChannelButton.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
			mChannelButton.setText("Ch");
			if (SystemConfig.now_editFontSize > 0) {
				mChannelButton.setTextSize(SystemConfig.now_buttonSize);
				mChannelButton.setMinimumWidth(w);
				mChannelButton.setMinWidth(24);
			}
			mChannelButton.setOnClickListener(onChannelButtonClick);
			mChannelButton.setOnLongClickListener(onChannelButtonLongClick);
			if (SystemConfig.now_buttonCh) {
				mCmdLayout.addView(mChannelButton);
			}
			mChannelButton.setKeyListener(onButtonKey);

			// chNextButton
			mChNextButton = new Button(me);
			mChNextButton.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
			mChNextButton.setText("↓");
			if (SystemConfig.now_editFontSize > 0) {
				mChNextButton.setTextSize(SystemConfig.now_buttonSize);
				mChNextButton.setMinimumWidth(w);
				mChNextButton.setMinWidth(24);
			}
			mChNextButton.setOnClickListener(onChNextButtonClick);
			mChNextButton.setOnLongClickListener(onChNextButtonLongClick);
			if (SystemConfig.now_buttonChMove) {
				mCmdLayout.addView(mChNextButton);
			}
			mChNextButton.setKeyListener(onButtonKey);

			// userButton
			mUserButton = new Button(me);
			mUserButton.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
			mUserButton.setText("U");
			if (SystemConfig.now_editFontSize > 0) {
				mUserButton.setTextSize(SystemConfig.now_buttonSize);
				mUserButton.setMinimumWidth(w);
				mUserButton.setMinWidth(24);
			}
			mUserButton.setOnClickListener(onUserButtonClick);
			mUserButton.setOnLongClickListener(onUserButtonLongClick);
			if (SystemConfig.now_buttonU) {
				mCmdLayout.addView(mUserButton);
			}
			mUserButton.setKeyListener(onButtonKey);

			// appliButton
			mAppliButton = new Button(me);
			mAppliButton.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
			mAppliButton.setText("A");
			if (SystemConfig.now_editFontSize > 0) {
				mAppliButton.setTextSize(SystemConfig.now_buttonSize);
				mAppliButton.setMinimumWidth(w);
				mAppliButton.setMinWidth(24);
			}
			mAppliButton.setOnClickListener(onAppliButtonClick);
			mAppliButton.setOnLongClickListener(onAppliButtonLongClick);
			if (SystemConfig.now_buttonAppli) {
				mCmdLayout.addView(mAppliButton);
			}
			mAppliButton.setKeyListener(onButtonKey);

			// websiteButton
			mWebsiteButton = new Button(me);
			mWebsiteButton.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
			mWebsiteButton.setText("W");
			if (SystemConfig.now_editFontSize > 0) {
				mWebsiteButton.setTextSize(SystemConfig.now_buttonSize);
				mWebsiteButton.setMinimumWidth(w);
				mWebsiteButton.setMinWidth(24);
			}
			mWebsiteButton.setOnClickListener(onWebsiteButtonClick);
			mWebsiteButton.setOnLongClickListener(onWebsiteButtonLongClick);
			if (SystemConfig.now_buttonWebsite) {
				mCmdLayout.addView(mWebsiteButton);
			}
			mWebsiteButton.setKeyListener(onButtonKey);
		}
	}

	/**
	 * 入力ボックスの保存
	 */
	public void saveInputBox() {
		if (I) Log.i(TAG, "saveInputBox()");
		if ((mInputBox != null) && (iIRCService != null)) {
			try {
				iIRCService.saveInputBox(
						mInputBox.getText().toString(),
						mInputBox.getSelectionStart(),
						mInputBox.getSelectionEnd()
				);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 入力ボックスの復元
	 */
	public void restoreInputBox() {
		if (I) Log.i(TAG, "restoreInputBox()");
		if (mInputBox != null) {
			if (mInputBox != null) {
				me.runOnUiThread(new Runnable(){ public void run() {
					try {
						mInputBox.setText(iIRCService.loadInputBox());
						final int selStart = iIRCService.loadInputBoxSelStart();
						final int selEnd = iIRCService.loadInputBoxSelEnd();
						mInputBox.setSelection(selStart, selEnd);
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
					mInputBox.requestFocus();
				} });
			}
		}
	}

	/**
	 * 入力メッセージの送信
	 */
	private void sendMessageByInputBox() {
		if (I) Log.i(TAG, "sendMessageByInputBox()");
		final String message = mInputBox.getText().toString();
		ActivityMain.sendMessageToIRCService(message);
		mInputBox.setText("");
	}

	/**
	 * 入力ボックスのリスナ処理
	 */
	private final KeyListener onInputBoxKey = new KeyListener() {
		public int getInputType() {
			return mInputBoxDefaultListener.getInputType();
		}

		public boolean onKeyDown(final View view, final Editable text, final int keyCode, final KeyEvent event) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_NUMPAD_ENTER:
				if (event.isShiftPressed()) {
					pushHistoryInputBoxMessage();
				} else {
					sendMessageByInputBox();
				}
				return true;

			case KeyEvent.KEYCODE_DEL:
				if (event.isAltPressed()) {
					ActivityMain.doMain.doInputHistoryMenu();
					return true;
				}
				break;
			}
			final boolean k = ActivityMain.doMain.doShortcutKey(keyCode, event);
			if (k == true) return true;
			return mInputBoxDefaultListener.onKeyDown(view, text, keyCode, event);
		}

		public boolean onKeyUp(final View view, final Editable text, final int keyCode, final KeyEvent event) {
			return mInputBoxDefaultListener.onKeyUp(view, text, keyCode, event);
		}

		public boolean onKeyOther(final View view, final Editable text, final KeyEvent event) {
			return mInputBoxDefaultListener.onKeyOther(view, text, event);
		}

		public void clearMetaKeyState(final View view, final Editable content, final int states) {
			mInputBoxDefaultListener.clearMetaKeyState(view, content, states);
		}

	};

	/**
	 * サービスから復活データを貰う
	 */
	public synchronized void reviveLogs() {
		if (I) Log.i(TAG, "reviveLogs()");
		try {
			final SpannableStringBuilder subLog = new SpannableStringBuilder(iIRCService.getSpanChannelLog(IRCMsg.sOtherChannelName, null));
			if (SystemConfig.now_subWindowMode > 0) {
				mSubLogWindow.setMessage(subLog);
			}
			final String serverName = iIRCService.getCurrentServerName();
			if (serverName != null) {
				final String channel = iIRCService.getCurrentChannel(serverName);
				if (channel != null) {
					ActivityMain.doMain.doChangeChannel(serverName, channel, false);
					// final SpannableStringBuilder chLog = new SpannableStringBuilder(iIRCService.getSpanChannelLog(serverName, channel));
					// mMainLogWindow.setMessage(chLog);
				}
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * レイアウトのコンフィギュレーション変更
	 * @param newConfig 新内容
	 */
	public synchronized void changeConfiguration(final Configuration newConfig) {
		if (I) Log.i(TAG, "onConfigurationChanged()");
		final boolean isChanged = setOrientation(false);
		if (isChanged) {
			setRotateMode();
			baseLayout_Create(false);
			if (iIRCService != null) reviveLogs();
		}
		mBaseLayout.requestLayout();
	}

	/**
	 * レイアウトの再作成処理
	 */
	public synchronized void rebootLayout() {
		if (I) Log.i(TAG, "rebootLayout()");
		// final boolean isChanged = setOrientation(true);
		// if (isChanged || (SystemConfig.rotateMode == 0)) {
			setRotateMode();
			baseLayout_Create(false);
			if (iIRCService != null) reviveLogs();
		// }
	}

	/**
	 * MainLogTextViewのロングクリック処理
	 */
	private final Runnable mOnMainLogTextViewLongClick = new Runnable() {
		public void run() {
			if (I) Log.i(TAG, "onMainLogTextViewLongClick()");
			if (mCancelLongClickView) {
				mCancelLongClickView = false;
				return;
			}

			ActivityMain.doMain.doMainLogWindowLongTap();
		}
	};
	
	/**
	 * MainLogTextViewのダブルタップ処理
	 */
	private final Runnable mOnMainLogTextViewDoubleTap = new Runnable() {
		public void run() {
			if (I) Log.i(TAG, "onMainLogTextViewDoubleTap()");
			final int[] i = new int[2];
			mMainLogLayout.getLocationOnScreen(i);
			float x = mLastMotionEvent.getRawX();
			float y = mLastMotionEvent.getRawY();
			// int x1 = mMainLogLayout.getLeft();
			// int y1 = mMainLogLayout.getTop();
			int w = mMainLogLayout.getWidth();
			int h = mMainLogLayout.getHeight();
			float x2 = (((x-i[0])*2)-w)/w;
			float y2 = (((y-i[1])*2)-h)/h;
			if ((x2/2)+y2 >= 0) {
				// ウィンドウ右下をクリック
				mMainLogWindow.setScrollToTail();
			} else {
				// ウィンドウ左上をクリック
				mMainLogWindow.setScrollToHead();
			}
		}
	};
	
	/**
	 * SubLogTextViewのロングクリック処理
	 */
	private final Runnable mOnSubLogTextViewLongClick = new Runnable() {
		public void run() {
			if (I) Log.i(TAG, "onSubLogTextViewLongClick()");
			if (mCancelLongClickView) {
				mCancelLongClickView = false;
				return;
			}

			ActivityMain.doMain.doSubLogWindowLongTap();
		}
	};

	/**
	 * SubLogTextViewのダブルタップ処理
	 */
	private final Runnable mOnSubLogTextViewDoubleTap = new Runnable() {
		public void run() {
			if (I) Log.i(TAG, "onSubLogTextViewDoubleTap()");
			final int[] i = new int[2];
			mSubLogLayout.getLocationOnScreen(i);
			float x = mLastMotionEvent.getRawX();
			float y = mLastMotionEvent.getRawY();
			// int x1 = mSubLogLayout.getLeft();
			// int y1 = mSubLogLayout.getTop();
			int w = mSubLogLayout.getWidth();
			int h = mSubLogLayout.getHeight();
			float x2 = (((x-i[0])*2)-w)/w;
			float y2 = (((y-i[1])*2)-h)/h;
			if ((x2/2)+y2 >= 0) {
				// ウィンドウ右下をクリック
				mSubLogWindow.setScrollToTail();
			} else {
				// ウィンドウ左上をクリック
				mSubLogWindow.setScrollToHead();
			}
		}
	};
	
	/**
	 * ログからコピー
	 * @param text コピーするログ
	 * @param title ダイアログタイトル
	 */
	public void copyFromLogWindow(final String text, final String title) {
		final DialogInterface.OnClickListener MyClickListener = new DialogInterface.OnClickListener(){
			public void onClick(final DialogInterface dialog, final int which) {
				// 何もしない
			}
		};
		final AlertDialog.Builder bldr = new AlertDialog.Builder(me)
		.setTitle(title);
		bldr.setNegativeButton("Close", MyClickListener);
		final ScrollView scroll = new ScrollView(me);
		final EditText edit = new EditText(me);
		edit.setText(text);
		edit.setSelection(edit.getText().length());
		final EditAssist editAssist = new EditAssist(me);
		editAssist.setOldKeyListener(edit.getKeyListener());
		edit.setKeyListener(editAssist.newKeyListener);
		edit.setTextSize(SystemConfig.now_editFontSize);
		scroll.addView(edit);
		bldr.setView(scroll);
		bldr.create().show();
	}

	/**
	 * メインログからコピー
	 */
	public void copyFromMainLogWindow() {
		final String text = mMainLogWindow.getTextView().getText().toString();
		copyFromLogWindow(text, me.getString(R.string.activitymain_java_copyfrommainlog));
	}

	/**
	 * サブログからコピー
	 */
	public void copyFromSubLogWindow() {
		final String text = mSubLogWindow.getTextView().getText().toString();
		copyFromLogWindow(text, me.getString(R.string.activitymain_java_copyfromsublog));
	}

	/**
	 * ボタンのキーリスナ処理
	 */
	private final KeyListener onButtonKey = new KeyListener() {
		public int getInputType() {
			return 0;
		}

		public boolean onKeyDown(final View view, final Editable text, final int keyCode, final KeyEvent event) {
			final boolean k = ActivityMain.doMain.doShortcutKey(keyCode, event);
			if (k == true) return true;
			if (EditAssist.isAnyKey(keyCode)) {
				ActivityMain.layout.mInputBox.requestFocus();
				return true;
			}
			return false;
		}

		public boolean onKeyUp(final View view, final Editable text, final int keyCode, final KeyEvent event) {
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
	 * [Ch]ボタンのクリック処理
	 */
	private final Button.OnClickListener onChannelButtonClick = new Button.OnClickListener() {
		public void onClick(final View v) {
			if (I) Log.i(TAG, "onChannelButtonClick: onClick()");
			ActivityMain.doMain.doSelectChannel();
		}
	};

	/**
	 * [Ch]ボタンのロングクリック処理
	 */
	private final Button.OnLongClickListener onChannelButtonLongClick = new Button.OnLongClickListener() {
		public boolean onLongClick(final View v) {
			if (I) Log.i(TAG, "onChannelButtonLongClick: onLongClick()");
			if (SystemConfig.cmdLongPressChannelToNotified) {
				ActivityMain.doMain.doSelectAlertedChannel();
			} else {
				ActivityMain.doMain.doSelectUpdatedChannel();
			}
			return true;
		}
	};

	/**
	 * [↑]ボタンのクリック処理
	 */
	private final Button.OnClickListener onChPrevButtonClick = new Button.OnClickListener() {
		public void onClick(final View v) {
			if (I) Log.i(TAG, "onChPrevButtonClick: onClick()");
			final boolean changed = ActivityMain.doMain.doChangeNextChannel(-1, 0, false);
			if (!changed) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notchanged));
			}
		}
	};

	/**
	 * [↑]ボタンのロングクリック処理
	 */
	private final Button.OnLongClickListener onChPrevButtonLongClick = new Button.OnLongClickListener() {
		public boolean onLongClick(final View v) {
			if (I) Log.i(TAG, "onChPrevButtonLongClick: onLongClick()");
			final boolean updated = ActivityMain.doMain.doChangeNextChannel(-1, 1, false);
			if (!updated) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notupdated));
			}
			return true;
		}
	};

	/**
	 * [↓]ボタンのクリック処理
	 */
	private final Button.OnClickListener onChNextButtonClick = new Button.OnClickListener() {
		public void onClick(final View v) {
			if (I) Log.i(TAG, "onChNextButtonClick: onClick()");
			final boolean changed = ActivityMain.doMain.doChangeNextChannel(1, 0, false);
			if (!changed) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notchanged));
			}
		}
	};

	/**
	 * [↓]ボタンのロングクリック処理
	 */
	private final Button.OnLongClickListener onChNextButtonLongClick = new Button.OnLongClickListener() {
		public boolean onLongClick(final View v) {
			if (I) Log.i(TAG, "onChNextButtonLongClick: onLongClick()");
			final boolean updated = ActivityMain.doMain.doChangeNextChannel(1, 1, false);
			if (!updated) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_notupdated));
			}
			return true;
		}
	};

	/**
	 * [U]ボタンのクリック処理
	 */
	private final Button.OnClickListener onUserButtonClick = new Button.OnClickListener() {
		public void onClick(final View v) {
			if (I) Log.i(TAG, "onUserButtonClick: onClick()");
			ActivityMain.doMain.doUtility(0);
		}
	};

	/**
	 * [U]ボタンのロングクリック処理
	 */
	private final Button.OnLongClickListener onUserButtonLongClick = new Button.OnLongClickListener() {
		public boolean onLongClick(final View v) {
			if (I) Log.i(TAG, "onUserButtonLongClick: onLongClick()");
			if (ActivityMain.mCurrentServerName != null) {
				ActivityMain.doMain.doUtility(1);
			}
			return true;
		}
	};

	/**
	 * [A]ボタンのクリック処理
	 */
	private final Button.OnClickListener onAppliButtonClick = new Button.OnClickListener() {
		public void onClick(final View v) {
			if (I) Log.i(TAG, "onAppliButtonClick: onClick()");
			ActivityMain.doMain.doExApp();
		}
	};

	/**
	 * [A]ボタンのロングクリック処理
	 */
	private final Button.OnLongClickListener onAppliButtonLongClick = new Button.OnLongClickListener() {
		public boolean onLongClick(final View v) {
			if (I) Log.i(TAG, "onAppliButtonLongClick: onLongClick()");
			ActivityMain.doMain.doExApp();
			return true;
		}
	};

	/**
	 * [W]ボタンのクリック処理
	 */
	private final Button.OnClickListener onWebsiteButtonClick = new Button.OnClickListener() {
		public void onClick(final View v) {
			if (I) Log.i(TAG, "onWebsiteButtonClick: onClick()");
			ActivityMain.doMain.doExWeb();
		}
	};

	/**
	 * [W]ボタンのロングクリック処理
	 */
	private final Button.OnLongClickListener onWebsiteButtonLongClick = new Button.OnLongClickListener() {
		public boolean onLongClick(final View v) {
			if (I) Log.i(TAG, "onWebsiteButtonLongClick: onLongClick()");
			ActivityMain.doMain.doExWeb();
			return true;
		}
	};


}
