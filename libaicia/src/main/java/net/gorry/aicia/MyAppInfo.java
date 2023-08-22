/**
 * 
 */
package net.gorry.aicia;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * 
 * アプリケーション情報の取得
 * 
 * @author GORRY
 *
 */
public class MyAppInfo {
	private static final String TAG = "MyAppInfo";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;
	private static Context me;
	private static PackageManager pm;

	/**
	 * コンテキストの登録
	 * @param context context
	 */
	public static void setContext(final Context context) {
		if (I) Log.i(TAG, "setContext()");
		me = context;
		pm = me.getPackageManager();
	}

	/**
	 * ResolveInfoの作成
	 * @param packageName パッケージ名
	 * @param activityName アクティビティ名
	 * @return ResolveInfo
	 */
	public static ResolveInfo createResolveInfo(final String packageName, final String activityName) {
		if (I) Log.i(TAG, "createResolveInfo()");
		if ((packageName == null) || (packageName.length() == 0)) {
			return (null);
		}
		if ((activityName == null) || (activityName.length() == 0)) {
			return (null);
		}
		ResolveInfo info = null;
		final Intent intent = new Intent();
		List<ResolveInfo> apps = null;
		try {
			intent.setClassName(packageName, activityName);
			apps = pm.queryIntentActivities(intent, 0);
		} catch (final Exception e) {
			// e.printStackTrace();
		}
		if (apps != null) {
			if (apps.size() > 0) {
				info = apps.get(0);
			}
		}
		return info;
	}

	/**
	 * アプリケーション名の取得
	 * @param info アプリケーション情報
	 * @return アプリケーション名
	 */
	public static String getAppName(final ResolveInfo info) {
		if (I) Log.i(TAG, "getAppName()");
		return info.activityInfo.loadLabel(pm).toString();
	}

	/**
	 * パッケージ名の取得
	 * @param info アプリケーション情報
	 * @return パッケージ名
	 */
	public static String getPackageName(final ResolveInfo info) {
		if (I) Log.i(TAG, "getPackageName()");
		return info.activityInfo.applicationInfo.packageName;
	}

	/**
	 * アクティビティ名の取得
	 * @param info アプリケーション情報
	 * @return パッケージ名
	 */
	public static String getActivityName(final ResolveInfo info) {
		if (I) Log.i(TAG, "getActivityName()");
		return info.activityInfo.name;
	}

	/**
	 * アクティビティからアイコンを取得
	 * @param info アプリケーション情報
	 * @return アイコン
	 */
	public static Drawable getIcon(final ResolveInfo info) {
		if (I) Log.i(TAG, "getIcon()");
		return info.activityInfo.loadIcon(pm);
	}

}
