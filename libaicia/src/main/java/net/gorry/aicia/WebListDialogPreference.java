/**
 *
 */
package net.gorry.aicia;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.DialogPreference;
import android.provider.Browser;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import net.gorry.libaicia.R;

/**
 *
 * ウェブサイト一覧から選択
 *
 * @author GORRY
 *
 */
/**
 * @author gorry
 *
 */
@SuppressLint("Instantiatable")
public class WebListDialogPreference extends DialogPreference  {
	private static final String TAG = "WebListDialogPreference";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;
	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;
	private Context me;

	private String mWebSiteName;
	private String mWebSiteUrl;

	private LinearLayout mBaseLayout;
	private LinearLayout mRow1Layout;
	private LinearLayout mRow2Layout;
	private TextView mTextWebSiteName;
	private TextView mTextWebSiteUrl;
	private EditText mEditWebSiteName;
	private EditText mEditWebSiteUrl;
	private Button mButtonChoose;
	private int mId;
	private Drawable mIcon = null;

	/**
	 * @param context context
	 * @param attrs attrs
	 */
	/*
	public WebListDialogPreference(final Context context) {
		super(context);
		me = context;
		onConstruct();
	}
	*/

	/**
	 * @param context context
	 * @param attrs attrs
	 */
	public WebListDialogPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		me = context;
		onConstruct();
	}

	/**
	 * @param context context
	 * @param attrs attrs
	 * @param defStyle defStyle
	 */
	public WebListDialogPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		me = context;
		onConstruct();
	}

	/**
	 * コンストラクタの下請け
	 */
	public void onConstruct() {
		if (I) Log.i(TAG, "onConstruct()");

		setLayoutResource(R.layout.weblistdialogpreference);

		final String key = getKey();
		int resId = R.string.pref_exweblist_web1;
		mId = 0;

		for (int i=0; i<SystemConfig.maxExWebSite; i++) {
			final String resName = "pref_exweblist_web"+(i+1);
			if (key.equals(resName)) {
				mId = i;
				resId = me.getResources().getIdentifier(resName, "string", me.getPackageName());
				break;
			}
		}

		final String title = me.getString(resId);
		setDialogTitle(title);
		mWebSiteName = SystemConfig.exWebSiteName[mId];
		mWebSiteUrl = SystemConfig.exWebSiteUrl[mId];
	}

	@Override
	protected void onBindView(final View view) {
		if (I) Log.i(TAG, "onBindView()");
		super.onBindView(view);

		final ImageView imageView = (ImageView)view.findViewById(R.id.myicon);
		if ((imageView != null) && (mIcon != null)) {
			imageView.setImageDrawable(mIcon);
		}
	}

	/**
	 * アイコン設定
	 * @param icon アイコン
	 */
	public void setIcon(final Drawable icon) {
		if (I) Log.i(TAG, "setIcon()");
		if (((icon == null) && (mIcon != null)) || ((icon != null) && (!icon.equals(mIcon)))) {
			mIcon = icon;
			notifyChanged();
		}
	}

	/**
	 * アイコン取得
	 * @return アイコン
	 */
	public Drawable getIcon() {
		if (I) Log.i(TAG, "getIcon()");
		return mIcon;
	}

	/*
	 * [Clear]ボタンの追加
	 * @see android.preference.DialogPreference#onPrepareDialogBuilder(android.app.AlertDialog.Builder)
	 */
	@Override
	protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
		builder.setNeutralButton(R.string.activitymain_java_exweb_button_clear, onClickButtonClear);
	}

	@Override
	protected View onCreateDialogView() {
		if (I) Log.i(TAG, "onCreateDialogView()");

		mBaseLayout = new LinearLayout(me);
		mBaseLayout.setOrientation(LinearLayout.VERTICAL);

		mButtonChoose = new Button(me);
		mButtonChoose.setText(R.string.activitymain_java_exweb_button_choose);
		mButtonChoose.setEnabled(false);
		// mButtonChoose.setOnClickListener(onClickButtonChoose);
		mBaseLayout.addView(mButtonChoose);

		mRow1Layout = new LinearLayout(me);
		mRow1Layout.setOrientation(LinearLayout.HORIZONTAL);
		mRow2Layout = new LinearLayout(me);
		mRow2Layout.setOrientation(LinearLayout.HORIZONTAL);
		mBaseLayout.addView(mRow1Layout);
		mBaseLayout.addView(mRow2Layout);

		mTextWebSiteUrl = new TextView(me);
		mTextWebSiteUrl.setText("URL:");
		mRow1Layout.addView(mTextWebSiteUrl);

		mEditWebSiteUrl = new EditText(me);
		mEditWebSiteUrl.setText(mWebSiteUrl);
		mEditWebSiteUrl.setSingleLine(true);
		mEditWebSiteUrl.setLayoutParams(new TableLayout.LayoutParams(FP, WC));
		final EditAssist editAssist1 = new EditAssist(me);
		editAssist1.setOldKeyListener(mEditWebSiteUrl.getKeyListener());
		mEditWebSiteUrl.setKeyListener(editAssist1.newKeyListener);
		mEditWebSiteUrl.addTextChangedListener(mEditWebSiteUrlChangedListener);
		mRow1Layout.addView(mEditWebSiteUrl);

		mTextWebSiteName = new TextView(me);
		mTextWebSiteName.setText("Title:");
		mRow2Layout.addView(mTextWebSiteName);

		mEditWebSiteName = new EditText(me);
		mEditWebSiteName.setText(mWebSiteName);
		mEditWebSiteName.setSingleLine(true);
		mEditWebSiteName.setLayoutParams(new TableLayout.LayoutParams(FP, WC));
		final EditAssist editAssist2 = new EditAssist(me);
		editAssist2.setOldKeyListener(mEditWebSiteName.getKeyListener());
		mEditWebSiteName.setKeyListener(editAssist2.newKeyListener);
		mEditWebSiteName.addTextChangedListener(mEditWebSiteNameChangedListener);
		mRow2Layout.addView(mEditWebSiteName);

		return mBaseLayout;
	}

	/**
	 * URL変更リスナ
	 */
	public final TextWatcher mEditWebSiteUrlChangedListener = new TextWatcher() {
		public void afterTextChanged(final Editable s) {
			mWebSiteUrl = mEditWebSiteUrl.getText().toString();
			if ((mWebSiteUrl == null) || (mWebSiteUrl.length() == 0)) {
				mWebSiteUrl = "http://";
				mEditWebSiteUrl.setText(mWebSiteUrl);
			}
			setEnablePositiveButton();
			setEnableEditWebSiteName();
		}

		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
			return;
		}

		public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
			return;
		}
	};

	/**
	 * サイト名変更リスナ
	 */
	public final TextWatcher mEditWebSiteNameChangedListener = new TextWatcher() {
		public void afterTextChanged(final Editable s) {
			mWebSiteName = mEditWebSiteName.getText().toString();
			setEnablePositiveButton();
		}

		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
			return;
		}

		public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
			return;
		}
	};

	/**
	 * [Clear]ボタンリスナ
	 */
	public final DialogInterface.OnClickListener onClickButtonClear = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
			if (I) Log.i(TAG, "onClick() onClickButtonClear");

			if (which == DialogInterface.BUTTON3) {
				mWebSiteUrl = "";
				mWebSiteName = "";
				mEditWebSiteUrl.setText(mWebSiteUrl);
				mEditWebSiteName.setText(mWebSiteName);
				SystemConfig.exWebSiteUrl[mId] = mWebSiteUrl;
				SystemConfig.exWebSiteName[mId] = mWebSiteName;
				callChangeListener(new Integer(mId));
			}
		}
	};

	/**
	 * [Choose]ボタンリスナ
	 */
	private final OnClickListener onClickButtonChoose = new OnClickListener()
	{
		final ArrayList<String> bookmarkName = new ArrayList<String>();
		final ArrayList<String> bookmarkUrl = new ArrayList<String>();

		public void createApplicationMenu()
		{
			if (I) Log.i(TAG, "createApplicationMenu()");
			final AwaitAlertDialogList dlg = new AwaitAlertDialogList(me);
			final ProgressDialog pd = new ProgressDialog(me);
			final Handler h = new Handler();

			// メニュー開く＆選択後
			final Runnable openMenu = new Runnable() {
				public void run() {
					dlg.setTitle(me.getString(R.string.activitymain_java_exweb_title));
					dlg.setLongClickEnable(false);
					dlg.create();

					new Thread(new Runnable() {	public void run() {
						final int result = dlg.show();
						if (result != AwaitAlertDialogBase.CANCEL) {
							final int sel = result - AwaitAlertDialogList.CLICKED;
							if ((0 <= sel) && (sel < bookmarkName.size())) {
								((Activity)me).runOnUiThread(new Runnable(){ public void run() {
									final int id = bookmarkName.size()-sel-1;
									mWebSiteUrl = bookmarkUrl.get(id);
									mWebSiteName = bookmarkName.get(id);
									mEditWebSiteUrl.setText(mWebSiteUrl);
									mEditWebSiteName.setText(mWebSiteName);
									setEnablePositiveButton();
									setEnableEditWebSiteName();
								}});
							}
						}
					} }).start();
				}
			};

			// プログレス消去
			final Runnable dismissProgressDialog = new Runnable() {
				public void run() {
					pd.dismiss();
					h.post(openMenu);
				}
			};

			// メニュー内容作成
			final Runnable createMenu = new Runnable() {
				public void run() {
					((Activity)me).runOnUiThread(new Runnable(){ public void run() {
						final String[] projection = new String[]{
								"bookmark", // Browser.BookmarkColumns.BOOKMARK,
								"created", // Browser.BookmarkColumns.CREATED,
								"date", // Browser.BookmarkColumns.DATE,
								"favicon", // Browser.BookmarkColumns.FAVICON,
								"title", // Browser.BookmarkColumns.TITLE,
								"url", // Browser.BookmarkColumns.URL,
								"visits", // Browser.BookmarkColumns.VISITS
						};
						final Cursor c = me.getContentResolver().query(Uri.parse("content://browser/bookmarks")/* Browser.BOOKMARKS_URI */, projection, "bookmark=1", null, null);
						if (c != null) {
							if (c.moveToFirst()) {
								for (int i=0; i<c.getCount(); i++) {
									int idx;
									String name = "(no Name)";
									String url = "(no URL)";
									idx = c.getColumnIndex("title" /* Browser.BookmarkColumns.TITLE */ );
									if (idx >= 0) {
										name = c.getString(idx);
									}
									idx = c.getColumnIndex("url" /* Browser.BookmarkColumns.URL */ );
									if (idx >= 0) {
										url = c.getString(idx);
									}
									bookmarkUrl.add(url);
									bookmarkName.add(name);
									c.moveToNext();
								}
							}
							c.close();
						}

						// 逆順に表示
						for (int i=bookmarkName.size()-1; i>=0; i--) {
							dlg.addItem(bookmarkName.get(i), 1);
						}

						h.post(dismissProgressDialog);
					}});
				}
			};

			// プログレス表示
			final Runnable showProgressDialog = new Runnable() {
				public void run() {
					pd.setTitle(me.getString(R.string.activitymain_java_progress_createbookmarkmenu));
					pd.setIndeterminate(true);
					pd.setCancelable(false);
					pd.show();
					h.post(createMenu);
				}
			};

			h.post(showProgressDialog);
		}

		// ボタン押下
		public void onClick(final View v)
		{
			if (I) Log.i(TAG, "onClick() onClickButtonChoose");

			((Activity)me).runOnUiThread(new Runnable(){ public void run() {
				createApplicationMenu();
			}});
		}
	};

	/*
	 * ダイアログクローズ時
	 * @see android.preference.DialogPreference#onDialogClosed(boolean)
	 */
	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		if (I) Log.i(TAG, "onDialogClosed()");
		if (positiveResult) {
			mWebSiteName = mEditWebSiteName.getText().toString();
			if ((mWebSiteName == null) || (mWebSiteName.length() == 0) || (mWebSiteUrl == null) || (mWebSiteUrl.length() == 0)) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_exweb_noweb));
				return;
			}
			final Uri uri = Uri.parse(mWebSiteUrl);
			if (!uri.isAbsolute()) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_exweb_noweb));
				return;
			}
			SystemConfig.exWebSiteUrl[mId] = mWebSiteUrl;
			SystemConfig.exWebSiteName[mId] = mWebSiteName;
			callChangeListener(new Integer(mId));
		}
		super.onDialogClosed(positiveResult);
	}

	/**
	 * アイコンをビューに表示
	 * @param v ビュー
	 */
	public void setIcon(final ImageView v) {
		if (I) Log.i(TAG, "setIcon()");
		if (mIcon != null) {
			v.setImageDrawable(mIcon);
		}
	}

	/**
	 * ダイアログ表示処理
	 * @param state ステート
	 */
	@Override
	protected void showDialog(final Bundle state) {
		if (I) Log.i(TAG, "showDialog()");

		super.showDialog(state);
		setEnablePositiveButton();
		setEnableEditWebSiteName();
	}

	/**
	 * Editを使用可能にする
	 */
	public void setEnableEditWebSiteName() {
		final EditText e = mEditWebSiteName;
		if ((mWebSiteUrl == null) || (mWebSiteUrl.length() == 0)) {
			e.setEnabled(false);
		} else {
			e.setEnabled(true);
		}
	}

	/**
	 * Okボタンを使用可能にする
	 */
	public void setEnablePositiveButton() {
		final AlertDialog d = (AlertDialog)(super.getDialog());
		final Button b = d.getButton(DialogInterface.BUTTON_POSITIVE);
		if ((mWebSiteUrl != null) && (mWebSiteUrl.length() > 0) && (mWebSiteName != null) && (mWebSiteName.length() > 0)) {
			final Uri uri = Uri.parse(mWebSiteUrl);
			if (uri.isAbsolute()) {
				b.setClickable(true);
				b.setEnabled(true);
				return;
			}
		}
		b.setClickable(false);
		b.setEnabled(false);
	}

}
