/**
 * 
 */
package net.gorry.aicia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
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
 * アプリ一覧から選択
 * 
 * @author GORRY
 *
 */
/**
 * @author gorry
 *
 */
@SuppressLint("Instantiatable")
public class AppListDialogPreference extends DialogPreference  {
	private static final String TAG = "AppListDialogPreference";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = true;
	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;
	private Context me;

	private String mAppName;
	private String mPackageName;
	private String mActivityName;

	private LinearLayout mBaseLayout;
	private LinearLayout mRow1Layout;
	private LinearLayout mRow2Layout;
	private TextView mTextActivityName;
	private ImageView mImageIcon;
	private EditText mEditAppName;
	private Button mButtonChoose;
	private int mId;
	private ResolveInfo mResolveInfo = null;
	private Drawable mIcon = null;

	/**
	 * @param context context
	 */
	/*
	public AppListDialogPreference(final Context context) {
		super(context);
		me = context;
		onConstruct();
	}
	*/

	/**
	 * @param context context
	 * @param attrs attrs
	 */
	public AppListDialogPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		me = context;
		onConstruct();
	}

	/**
	 * @param context context
	 * @param attrs attrs
	 * @param defStyle defStyle
	 */
	public AppListDialogPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		me = context;
		onConstruct();
	}

	/**
	 * コンストラクタの下請け
	 */
	public void onConstruct() {
		if (I) Log.i(TAG, "onConstruct()");

		setLayoutResource(R.layout.applistdialogpreference);

		final String key = getKey();
		int resId = R.string.pref_exapplist_app1;
		mId = 0;

		for (int i=0; i<SystemConfig.maxExApp; i++) {
			final String resName = "pref_exapplist_app"+(i+1);
			if (key.equals(resName)) {
				mId = i;
				resId = me.getResources().getIdentifier(resName, "string", me.getPackageName());
				break;
			}
		}

		final String title = me.getString(resId);
		setDialogTitle(title);
		mAppName = SystemConfig.exAppName[mId];
		mPackageName = SystemConfig.exAppPackageName[mId];
		mActivityName = SystemConfig.exAppActivityName[mId];
		mResolveInfo = MyAppInfo.createResolveInfo(mPackageName, mActivityName);
		if (mResolveInfo != null) {
			mIcon = MyIcon.resizeIcon(MyAppInfo.getIcon(mResolveInfo));
		} else {
			mIcon = null;
		}
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
		builder.setNeutralButton(R.string.activitymain_java_exapp_button_clear, onClickButtonClear);
	}

	@Override
	protected View onCreateDialogView() {
		if (I) Log.i(TAG, "onCreateDialogView()");

		mBaseLayout = new LinearLayout(me);
		mBaseLayout.setOrientation(LinearLayout.VERTICAL);

		mButtonChoose = new Button(me);
		mButtonChoose.setText(R.string.activitymain_java_exapp_button_choose);
		mButtonChoose.setOnClickListener(onClickButtonChoose);
		mBaseLayout.addView(mButtonChoose);

		mRow1Layout = new LinearLayout(me);
		mRow1Layout.setOrientation(LinearLayout.HORIZONTAL);
		mRow2Layout = new LinearLayout(me);
		mRow2Layout.setOrientation(LinearLayout.HORIZONTAL);
		mBaseLayout.addView(mRow1Layout);
		mBaseLayout.addView(mRow2Layout);

		final DisplayMetrics metrics = new DisplayMetrics();
		((Activity)me).getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mImageIcon = new ImageView(me);
		mImageIcon.setLayoutParams(new LinearLayout.LayoutParams((int)(48*metrics.scaledDensity), (int)(48*metrics.scaledDensity)));
		setIcon(mImageIcon);
		mRow1Layout.addView(mImageIcon);

		mEditAppName = new EditText(me);
		mEditAppName.setText(mAppName);
		mEditAppName.setSingleLine(true);
		mEditAppName.setLayoutParams(new TableLayout.LayoutParams(FP, WC));
		final EditAssist editAssist1 = new EditAssist(me);
		editAssist1.setOldKeyListener(mEditAppName.getKeyListener());
		mEditAppName.setKeyListener(editAssist1.newKeyListener);
		mEditAppName.addTextChangedListener(mEditAppNameChangedListener);
		mRow1Layout.addView(mEditAppName);

		mTextActivityName = new TextView(me);
		mTextActivityName.setText(mActivityName);
		mTextActivityName.setGravity(Gravity.CENTER_VERTICAL);
		mRow2Layout.addView(mTextActivityName);

		return mBaseLayout;
	}

	/**
	 * アプリ名変更リスナ
	 */
	public final TextWatcher mEditAppNameChangedListener = new TextWatcher() {
		public void afterTextChanged(final Editable s) {
			mAppName = mEditAppName.getText().toString();
			setEnablePositiveButton();
			setEnableEditAppName();
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
				mAppName = "";
				mPackageName = "";
				mActivityName = "";
				mIcon = null;
				SystemConfig.exAppName[mId] = mAppName;
				SystemConfig.exAppPackageName[mId] = mPackageName;
				SystemConfig.exAppActivityName[mId] = mActivityName;
				setIcon(mImageIcon);
				callChangeListener(new Integer(mId));
			}
		}
	};

	/**
	 * [Choose]ボタンリスナ
	 */
	public final OnClickListener onClickButtonChoose = new OnClickListener()
	{
		final ArrayList<ResolveInfo> appList = new ArrayList<ResolveInfo>();

		public void createApplicationMenu()
		{
			if (I) Log.i(TAG, "createApplicationMenu()");
			final AwaitAlertDialogIconList dlg = new AwaitAlertDialogIconList(me);
			final ProgressDialog pd = new ProgressDialog(me);
			final Handler h = new Handler();

			// メニュー開く＆選択後
			final Runnable openMenu = new Runnable() {
				public void run() {
					dlg.setTitle(me.getString(R.string.activitymain_java_exapp_title));
					dlg.setLongClickEnable(false);
					dlg.create();

					new Thread(new Runnable() {	public void run() {
						final int result = dlg.show();
						if (result != AwaitAlertDialogBase.CANCEL) {
							final int id = result - AwaitAlertDialogList.CLICKED;
							if ((0 <= id) && (id < appList.size())) {
								((Activity)me).runOnUiThread(new Runnable(){ public void run() {
									final ResolveInfo info = appList.get(id);
									if (info != null) {
										mResolveInfo = info;
										mAppName = MyAppInfo.getAppName(info);
										mEditAppName.setText(mAppName);
										mPackageName = MyAppInfo.getPackageName(info);
										mActivityName = MyAppInfo.getActivityName(info);
										mTextActivityName.setText(mActivityName);
										mIcon = MyIcon.resizeIcon(MyAppInfo.getIcon(info));
										setIcon(mImageIcon);
										setEnablePositiveButton();
										setEnableEditAppName();
									}
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
					new Thread(new Runnable() {	public void run() {
						final PackageManager pm = me.getPackageManager();
						final Intent intent = new Intent(Intent.ACTION_MAIN, null);
						intent.addCategory(Intent.CATEGORY_LAUNCHER);
						final List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
						Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));

						for (int i=0; i<apps.size(); i++) {
							final ResolveInfo info = apps.get(i);
							appList.add(info);
							final String label = info.activityInfo.loadLabel(pm).toString();
							final Drawable icon = info.activityInfo.loadIcon(pm);
							dlg.addItem(label, icon, 1);
						}

						h.post(dismissProgressDialog);
					} }).start();
				}
			};

			// プログレス表示
			final Runnable showProgressDialog = new Runnable() {
				public void run() {
					pd.setTitle(me.getString(R.string.activitymain_java_progress_createapplicationmenu));
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
			mAppName = mEditAppName.getText().toString();
			if ((mAppName == null) || (mAppName.length() == 0) || (mActivityName == null) || (mActivityName.length() == 0)) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_exapp_noapp));
				return;
			}
			SystemConfig.exAppName[mId] = mAppName;
			SystemConfig.exAppPackageName[mId] = mPackageName;
			SystemConfig.exAppActivityName[mId] = mActivityName;
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
		} else {
			v.setImageDrawable(null);
		}
	}

	/**
	 * @param dialog ダイアログ
	 * @param which ボタン種別
	 */
	/*
	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		if (I) Log.i(TAG, "onClick()");

		if (which == DialogInterface.BUTTON_POSITIVE) {
			mAppName = mEditAppName.getText().toString();
			if ((mAppName == null) || (mAppName.length() == 0) || (mActivityName == null) || (mActivityName.length() == 0)) {
				ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_exapp_noapp));
				return;
			}
		}

		super.onClick(dialog, which);
	}
	 */

	/**
	 * ダイアログ表示処理
	 * @param state ステート
	 */
	@Override
	protected void showDialog(final Bundle state) {
		if (I) Log.i(TAG, "showDialog()");

		super.showDialog(state);
		setEnablePositiveButton();
		setEnableEditAppName();
	}

	/**
	 * Editを使用可能にする
	 */
	public void setEnableEditAppName() {
		final EditText e = mEditAppName;
		if ((mActivityName == null) || (mActivityName.length() == 0)) {
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
		if ((mAppName != null) && (mAppName.length() > 0) && (mActivityName != null) && (mActivityName.length() > 0)) {
			b.setClickable(true);
			b.setEnabled(true);
			return;
		}
		b.setClickable(false);
		b.setEnabled(false);
	}

}
