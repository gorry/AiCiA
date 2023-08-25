/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;

import net.gorry.libaicia.BuildConfig;
import net.gorry.libaicia.R;

/**
 *
 * システム設定
 *
 * @author GORRY
 *
 */
public class SystemConfig {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "SystemConfig";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private static Context me;
	private static boolean isLandscape = false;

	/** */
	public static int verbose;

	/** */
	public static int rotateMode;

	/** */
	public static boolean canRotate180;

	/** */
	public static int dateMode;

	/** */
	public static int channelLogBufferSize;
	/** */
	public static int otherChannelLogBufferSize;
	/** */
	public static int spanChannelLogLines;
	/** */
	public static int spanOtherChannelLogLines;

	/** */
	public static int mainLogBufferSize;
	/** */
	public static int subLogBufferSize;

	/** */
	public static final int maxColorSet = 4;
	/** */
	public static int[] mainLogBackgroundColor = new int[maxColorSet];
	/** */
	public static int[] mainLogDateColor = new int[maxColorSet];
	/** */
	public static int[] mainLogTextColor = new int[maxColorSet];
	/** */
	public static int[] mainLogPaleTextColor = new int[maxColorSet];
	/** */
	public static int[] mainLogEmphasisTextColor = new int[maxColorSet];
	/** */
	public static int[] subLogBackgroundColor = new int[maxColorSet];
	/** */
	public static int[] subLogDateColor = new int[maxColorSet];
	/** */
	public static int[] subLogTextColor = new int[maxColorSet];
	/** */
	public static int[] subLogPaleTextColor = new int[maxColorSet];
	/** */
	public static int[] subLogEmphasisTextColor = new int[maxColorSet];
	/** */
	public static int[] alertKeywordsColor2 = new int[maxColorSet];
	/** */
	public static int[] alertLineColor = new int[maxColorSet];

	/** */
	public static boolean ringOnAlert;
	/** */
	public static boolean vibrateOnAlert;
	/** */
	public static boolean highlightOnAlert;
	/** */
	public static boolean copyToSystemChannelOnAlert;
	/** */
	public static boolean notifyOnAlert;

	/** */
	public static boolean noSleepMode;

	/** */
	public static int ringLevel;

	/** */
	public static int autoReconnectWaitSec;
	/** */
	public static int checkConnectWaitSec;

	/** */
	public static boolean now_horizontalMode;
	/** */
	public static boolean now_swapWindowMode;
	/** */
	public static int now_subWindowMode;
	/** */
	public static int now_mainLogFontSize;
	/** */
	public static int now_mainLogFontFace;
	/** */
	public static int now_mainLogLineSpacing;
	/** */
	public static int now_subLogFontSize;
	/** */
	public static int now_subLogFontFace;
	/** */
	public static int now_subLogLineSpacing;
	/** */
	public static int now_editFontSize;
	/** */
	public static int now_editFontFace;
	/** */
	public static boolean now_buttonChMove;
	/** */
	public static boolean now_buttonCh;
	/** */
	public static boolean now_buttonU;
	/** */
	public static boolean now_buttonAppli;
	/** */
	public static boolean now_buttonWebsite;
	/** */
	public static int now_buttonSize;
	/** */
	public static boolean now_buttonWide;
	/** */
	public static int now_colorSet = 0;
	/** */
	public static boolean now_showStatusBar;
	/** */
	public static boolean now_showTitleBar;

	/** */
	public static boolean portrait_horizontalMode;
	/** */
	public static boolean portrait_swapWindowMode;
	/** */
	public static int portrait_subWindowMode;
	/** */
	public static int portrait_mainLogFontSize;
	/** */
	public static int portrait_mainLogFontFace;
	/** */
	public static int portrait_mainLogLineSpacing;
	/** */
	public static int portrait_subLogFontSize;
	/** */
	public static int portrait_subLogFontFace;
	/** */
	public static int portrait_subLogLineSpacing;
	/** */
	public static int portrait_editFontSize;
	/** */
	public static int portrait_editFontFace;
	/** */
	public static boolean portrait_buttonChMove;
	/** */
	public static boolean portrait_buttonCh;
	/** */
	public static boolean portrait_buttonU;
	/** */
	public static boolean portrait_buttonAppli;
	/** */
	public static boolean portrait_buttonWebsite;
	/** */
	public static int portrait_buttonSize;
	/** */
	public static boolean portrait_buttonWide;
	/** */
	// public static int portrait_colorSet;
	/** */
	public static boolean portrait_showStatusBar;
	/** */
	public static boolean portrait_showTitleBar;

	/** */
	public static boolean landscape_horizontalMode;
	/** */
	public static boolean landscape_swapWindowMode;
	/** */
	public static int landscape_subWindowMode;
	/** */
	public static int landscape_mainLogFontSize;
	/** */
	public static int landscape_mainLogFontFace;
	/** */
	public static int landscape_mainLogLineSpacing;
	/** */
	public static int landscape_subLogFontSize;
	/** */
	public static int landscape_subLogFontFace;
	/** */
	public static int landscape_subLogLineSpacing;
	/** */
	public static int landscape_editFontSize;
	/** */
	public static int landscape_editFontFace;
	/** */
	public static boolean landscape_buttonChMove;
	/** */
	public static boolean landscape_buttonCh;
	/** */
	public static boolean landscape_buttonU;
	/** */
	public static boolean landscape_buttonAppli;
	/** */
	public static boolean landscape_buttonWebsite;
	/** */
	public static int landscape_buttonSize;
	/** */
	public static boolean landscape_buttonWide;
	/** */
	// public static int landscape_colorSet;
	/** */
	public static boolean landscape_showStatusBar;
	/** */
	public static boolean landscape_showTitleBar;

	/** */
	public static String TIG_oldRTCmd;
	/** */
	public static Boolean twitterSiteIsMobile;

	/** */
	public static Boolean allowSendHalfKana;
	/** */
	public static Boolean inputSingleLine = true;

	/** */
	public static final int maxExApp = 8;
	/** */
	public static String[] exAppName = new String[maxExApp];
	/** */
	public static String[] exAppPackageName = new String[maxExApp];
	/** */
	public static String[] exAppActivityName = new String[maxExApp];

	/** */
	public static final int maxExWebSite = 8;
	/** */
	public static String[] exWebSiteName = new String[maxExWebSite];
	/** */
	public static String[] exWebSiteUrl = new String[maxExWebSite];

	/** */
	public static Boolean showLowMemoryIcon;
	/** */
	public static Boolean showServerIcon;
	/** */
	public static Boolean fixSystemIcon;

	/** */
	public static final int limitLongClickMove = 16;

	/** */
	public static Typeface userTypeface;

	/** */
	public static Typeface editTypeface;

	/** */
	public static Typeface mainLogTypeface;

	/** */
	public static Typeface subLogTypeface;

	/** */
	public static final String myFolderName = "AiCiA";

	/** */
	public static final String systemConfigExportFileName = "system.setting";

	/** */
	public static final String serverConfigExportFileName = "ircserver.setting";

	/** */
	public static Boolean cmdLongPressChannelToNotified;

	/** */
	public static String externalFontPath;
	/** */
	public static String externalFontPathDefault;

	/** */
	private static int viewLayout;

	/**
	 * バージョン文字列の読み取り
	 * @return バージョン文字列
	 */
	public static String getVersionString() {
		if (T) Log.v(TAG, M()+"@in");

		String ret = "";
		try {
			final String packageName = me.getPackageName();
			PackageInfo packageInfo = me.getPackageManager().getPackageInfo(
					packageName, PackageManager.GET_META_DATA
			);
			ret = packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}


	/**
	 * 外部フォルダパスの読み取り
	 * @return パス名（末尾の"/"を含む）
	 */
	public static String getExternalPath() {
		String path = me.getExternalFilesDir(null).toString();
		path += "/" + SystemConfig.myFolderName + "/";
		return path;
	}


	/**
	 * カラーセットの初期化
	 * @param index index
	 */
	public static void initializeColorSet(final int index) {
		switch (index) {
		case 0:
		case 2:
			mainLogBackgroundColor[index] = Color.BLACK;
			mainLogDateColor[index]       = Color.GREEN;
			mainLogTextColor[index]       = Color.WHITE;
			mainLogPaleTextColor[index]   = Color.GRAY;
			mainLogEmphasisTextColor[index]   = Color.RED;
			subLogBackgroundColor[index]  = Color.rgb(32,32,32);
			subLogDateColor[index]        = Color.GREEN;
			subLogTextColor[index]        = Color.WHITE;
			subLogPaleTextColor[index]    = Color.GRAY;
			subLogEmphasisTextColor[index]    = Color.RED;
			alertKeywordsColor2[index]    = Color.BLACK;
			alertLineColor[index]         = Color.rgb(255,128,128);
			break;
		case 1:
		case 3:
			mainLogBackgroundColor[index] = Color.WHITE;
			mainLogDateColor[index]       = Color.rgb(0,128,0);
			mainLogTextColor[index]       = Color.BLACK;
			mainLogPaleTextColor[index]   = Color.GRAY;
			mainLogEmphasisTextColor[index]   = Color.RED;
			subLogBackgroundColor[index]  = Color.rgb(224,224,224);
			subLogDateColor[index]        = Color.rgb(0,128,0);
			subLogTextColor[index]        = Color.BLACK;
			subLogPaleTextColor[index]    = Color.GRAY;
			subLogEmphasisTextColor[index]    = Color.RED;
			alertKeywordsColor2[index]    = Color.WHITE;
			alertLineColor[index]         = Color.rgb(255,128,128);
		}
	}

	/**
	 * 設定消去
	 */
	public static void deleteConfig() {
		if (T) Log.v(TAG, M()+"@in");

		final SharedPreferences pref = me.getSharedPreferences("system", 0);
		final SharedPreferences.Editor editor = pref.edit();

		editor.remove("fileVersion");

		editor.remove("verbose");

		editor.remove("rotatemode");

		editor.remove("datemode");

		editor.remove("channellogbuffersize");
		editor.remove("otherchannellogbuffersize");
		editor.remove("spanchannelloglines");
		editor.remove("spanotherchannelloglines");

		editor.remove("mainlogbuffersize");
		editor.remove("sublogbuffersize");
		editor.remove("cmdlinetextsize");
		

		editor.remove("nowcolorset");
		editor.remove("mainlogbackgroundcolor");
		editor.remove("mainlogdatecolor");
		editor.remove("mainlogtextcolor");
		editor.remove("sublogbackgroundcolor");
		editor.remove("sublogdatecolor");
		editor.remove("sublogtextcolor");
		editor.remove("alertkeywordscolor");
		editor.remove("logcolor");
		editor.remove("palelogcolor");
		for (int i=0; i<maxColorSet; i++) {
			editor.remove("nowcolorset"+i);
			editor.remove("mainlogbackgroundcolor"+i);
			editor.remove("mainlogdatecolor"+i);
			editor.remove("mainlogtextcolor"+i);
			editor.remove("sublogbackgroundcolor"+i);
			editor.remove("sublogdatecolor"+i);
			editor.remove("sublogtextcolor"+i);
			editor.remove("alertkeywordscolor"+i);
			editor.remove("logcolor"+i);
			editor.remove("palelogcolor"+i);
		}

		editor.remove("ringonalert");
		editor.remove("vibrateonalert");
		editor.remove("highlightonalert");
		editor.remove("copytosystemchannelonalert");
		editor.remove("putpaletexttosublog");
		editor.remove("nosleepmode");

		editor.remove("ringlevel");

		editor.remove("autoreconnectwaitsec");
		editor.remove("checkconnectwaitsec");

		editor.remove("portrait_horizontalmode");
		editor.remove("portrait_swapwindowmode");
		editor.remove("portrait_subwindowmode");
		editor.remove("portrait_mainlogfontsize");
		editor.remove("portrait_mainlogfontface");
		editor.remove("portrait_sublogfontsize");
		editor.remove("portrait_sublogfontface");
		editor.remove("portrait_editfontsize");
		editor.remove("portrait_editfontface");
		editor.remove("portrait_button_chmove");
		editor.remove("portrait_button_ch");
		editor.remove("portrait_button_u");
		editor.remove("portrait_button_appli");
		editor.remove("portrait_button_website");
		editor.remove("portrait_colorset");
		editor.remove("portrait_showstatusbar");
		editor.remove("portrait_showtitlebar");

		editor.remove("landscape_horizontalmode");
		editor.remove("landscape_swapwindowmode");
		editor.remove("landscape_subwindowmode");
		editor.remove("landscape_mainlogfontsize");
		editor.remove("landscape_mainlogfontface");
		editor.remove("landscape_sublogfontsize");
		editor.remove("landscape_sublogfontface");
		editor.remove("landscape_editfontsize");
		editor.remove("landscape_editfontface");
		editor.remove("landscape_button_chmove");
		editor.remove("landscape_button_ch");
		editor.remove("landscape_button_u");
		editor.remove("landscape_button_appli");
		editor.remove("landscape_button_website");
		editor.remove("landscape_colorset");
		editor.remove("landscape_showstatusbar");
		editor.remove("landscape_showtitlebar");

		editor.remove("tig_oldrtcmd");
		editor.remove("twittersiteismobile");

		editor.remove("allowsendhalfkana");
		editor.remove("inputsingleline");

		for (int i=0; i<maxExApp; i++) {
			editor.remove("exAppName"+i);
			editor.remove("exAppPackageName"+i);
			editor.remove("exAppActivityName"+i);
		}

		for (int i=0; i<maxExWebSite; i++) {
			editor.remove("ExWebSiteName"+i);
			editor.remove("ExWebSiteUrl"+i);
		}

		editor.remove("showlowmemoryicon");
		editor.remove("autofadeservericon");
		editor.remove("fixsystemicon");

		editor.remove("cmdlongpresschanneltonotified");

		editor.remove("externalfontpath");

		editor.commit();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 設定読み込み
	 * @return 読み込んだ設定のバージョン
	 */
	public static String loadConfig() {
		if (T) Log.v(TAG, M()+"@in");

		String ret = loadConfigCore(false);

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 設定インポート
	 * @return 読み込んだ設定のバージョン
	 */
	public static String importConfig() {
		if (T) Log.v(TAG, M()+"@in");

		String ret = loadConfigCore(true);

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 設定読み込みコア
	 * @param importing 設定を外部ファイルから入力するときtrue
	 * @return 読み込んだ設定のバージョン
	 */
	private static String loadConfigCore(final boolean importing) {
		if (T) Log.v(TAG, M()+"@in: importing="+importing);

		String version = getVersionString();

		String filename;
		if (importing) {
			filename = SystemConfig.getExternalPath() + SystemConfig.systemConfigExportFileName;
		} else {
			filename = "system";
		}
		final MySharedPreferences pref = new MySharedPreferences(me, filename);
		if (importing) {
			pref.setActivity(ActivitySystemConfig.getActivity());
		}
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			if (T) Log.v(TAG, M()+"@out: version="+version);
			return version;
		}

		String rawVersion = pref.getString("fileVersion", "");
		version = pref.getString("fileVersion", version);

		verbose = pref.getInt("verbose", 0);

		rotateMode = pref.getInt("rotatemode", 0);
		canRotate180 = pref.getBoolean("canrotate180", true);

		dateMode = pref.getInt("datemode", 1);

		channelLogBufferSize = pref.getInt("channellogbuffersize", 1024*8);
		otherChannelLogBufferSize = pref.getInt("otherchannellogbuffersize", 1024*8);
		spanChannelLogLines = pref.getInt("spanchannelloglines", 200);
		spanOtherChannelLogLines = pref.getInt("spanotherchannelloglines", 200);

		mainLogBufferSize = pref.getInt("mainlogbuffersize", 1024*16);
		subLogBufferSize = pref.getInt("sublogbuffersize", 1024*16);

		for (int i=0; i<maxColorSet; i++) {
			initializeColorSet(i);
			mainLogBackgroundColor[i] = pref.getInt("mainlogbackgroundcolor"+i, mainLogBackgroundColor[i]);
			mainLogDateColor[i] = pref.getInt("mainlogdatecolor"+i, mainLogDateColor[i]);
			mainLogTextColor[i] = pref.getInt("mainlogtextcolor"+i, mainLogTextColor[i]);
			mainLogPaleTextColor[i] = pref.getInt("mainlogpaletextcolor"+i, mainLogPaleTextColor[i]);
			mainLogEmphasisTextColor[i] = pref.getInt("mainlogemphasistextcolor"+i, mainLogEmphasisTextColor[i]);
			subLogBackgroundColor[i] = pref.getInt("sublogbackgroundcolor"+i, subLogBackgroundColor[i]);
			subLogDateColor[i] = pref.getInt("sublogdatecolor"+i, subLogDateColor[i]);
			subLogTextColor[i] = pref.getInt("sublogtextcolor"+i, subLogTextColor[i]);
			subLogPaleTextColor[i] = pref.getInt("sublogpaletextcolor"+i, subLogPaleTextColor[i]);
			subLogEmphasisTextColor[i] = pref.getInt("sublogpaletextcolor"+i, subLogEmphasisTextColor[i]);
			alertKeywordsColor2[i] = pref.getInt("alertkeywordscolor2"+i, alertKeywordsColor2[i]);
			alertLineColor[i] = pref.getInt("alertlinecolor"+i, alertLineColor[i]);

			// 現状まだこの設定は使用しないので初期化
			initializeColorSet(i);
		}

		ringOnAlert = pref.getBoolean("ringonalert", true);
		vibrateOnAlert = pref.getBoolean("vibrateonalert", true);
		highlightOnAlert = pref.getBoolean("highlightonalert", true);
		copyToSystemChannelOnAlert = pref.getBoolean("copytosystemchannelonalert", true);
		notifyOnAlert = pref.getBoolean("notifyonalert", true);
		noSleepMode = pref.getBoolean("nosleepmode", true);

		ringLevel = pref.getInt("ringlevel", 2);

		autoReconnectWaitSec = pref.getInt("autoreconnectwaitsec", 30);
		checkConnectWaitSec = pref.getInt("checkconnectwaitsec", 90);

		portrait_horizontalMode = pref.getBoolean("portrait_horizontalmode", false);
		boolean f = false;
		if (rawVersion.length() > 0) {
			f = (version.compareTo("2011.0620.1") >= 0);
		}
		portrait_swapWindowMode = pref.getBoolean("portrait_swapwindowmode", f);
		portrait_subWindowMode = pref.getInt("portrait_subwindowmode", 2);
		portrait_mainLogFontSize = pref.getInt("portrait_mainlogfontsize", 10);
		portrait_mainLogFontFace = pref.getInt("portrait_mainlogfontface", 0);
		portrait_mainLogLineSpacing = pref.getInt("portrait_mainloglinespacing", 0);
		portrait_subLogFontSize = pref.getInt("portrait_sublogfontsize", 10);
		portrait_subLogFontFace = pref.getInt("portrait_sublogfontface", 0);
		portrait_subLogLineSpacing = pref.getInt("portrait_subloglinespacing", 0);
		portrait_editFontSize = pref.getInt("portrait_editfontsize", 12);
		portrait_editFontFace = pref.getInt("portrait_editfontface", 0);
		portrait_buttonChMove = pref.getBoolean("portrait_button_chmove", true);
		portrait_buttonCh = pref.getBoolean("portrait_button_ch", true);
		portrait_buttonU = pref.getBoolean("portrait_button_u", true);
		portrait_buttonAppli = pref.getBoolean("portrait_button_appli", false);
		portrait_buttonWebsite = pref.getBoolean("portrait_button_website", false);
		portrait_buttonSize = pref.getInt("portrait_button_size", 18);
		portrait_buttonWide = pref.getBoolean("portrait_button_wide", false);
		// portrait_colorSet = pref.getInt("portrait_colorset", 0);
		portrait_showStatusBar = pref.getBoolean("portrait_showstatusbar", true);
		portrait_showTitleBar = pref.getBoolean("portrait_showtitlebar", true);

		landscape_horizontalMode = pref.getBoolean("landscape_horizontalmode", true);
		landscape_swapWindowMode = pref.getBoolean("landscape_swapwindowmode", false);
		landscape_subWindowMode = pref.getInt("landscape_subwindowmode", 2);
		landscape_mainLogFontSize = pref.getInt("landscape_mainlogfontsize", 10);
		landscape_mainLogFontFace = pref.getInt("landscape_mainlogfontface", 0);
		landscape_mainLogLineSpacing = pref.getInt("landscape_mainloglinespacing", 0);
		landscape_subLogFontSize = pref.getInt("landscape_sublogfontsize", 10);
		landscape_subLogFontFace = pref.getInt("landscape_sublogfontface", 0);
		landscape_subLogLineSpacing = pref.getInt("landscape_subloglinespacing", 0);
		landscape_editFontSize = pref.getInt("landscape_editfontsize", 12);
		landscape_editFontFace = pref.getInt("landscape_editfontface", 0);
		landscape_buttonChMove = pref.getBoolean("landscape_button_chmove", true);
		landscape_buttonCh = pref.getBoolean("landscape_button_ch", true);
		landscape_buttonU = pref.getBoolean("landscape_button_u", true);
		landscape_buttonAppli = pref.getBoolean("landscape_button_appli", false);
		landscape_buttonWebsite = pref.getBoolean("landscape_button_website", false);
		landscape_buttonSize = pref.getInt("landscape_button_size", 18);
		landscape_buttonWide = pref.getBoolean("landscape_button_wide", false);
		// landscape_colorSet = pref.getInt("landscape_colorset", 0);
		landscape_showStatusBar = pref.getBoolean("landscape_showstatusbar", true);
		landscape_showTitleBar = pref.getBoolean("landscape_showtitlebar", true);

		// カラーセットは縦横共通で扱う
		now_colorSet = pref.getInt("nowcolorset", 0);

		TIG_oldRTCmd = pref.getString("tig_oldrtcmd", "mrt");
		twitterSiteIsMobile = pref.getBoolean("twittersiteismobile", true);

		allowSendHalfKana = pref.getBoolean("allowsendhalfkana", false);
		inputSingleLine = pref.getBoolean("inputsingleline", true);

		for (int i=0; i<maxExApp; i++) {
			exAppName[i] = pref.getString("exapp_name_"+i, "");
			exAppPackageName[i] = pref.getString("exapp_packagename_"+i, "");
			exAppActivityName[i] = pref.getString("exapp_activityname_"+i, "");
		}

		for (int i=0; i<maxExWebSite; i++) {
			exWebSiteName[i] = pref.getString("exwebsite_name_"+i, "");
			exWebSiteUrl[i] = pref.getString("exwebsite_url_"+i, "");
		}

		showLowMemoryIcon = pref.getBoolean("showlowmemoryicon", true);
		showServerIcon = pref.getBoolean("showservericon", true);
		fixSystemIcon = pref.getBoolean("fixsystemicon", false);

		cmdLongPressChannelToNotified = pref.getBoolean("cmdlongpresschanneltonotified", false);

		String path = SystemConfig.getExternalPath() + "font.ttf";
		externalFontPathDefault = path;
		externalFontPath = pref.getString("externalfontpath", path);
		if (externalFontPath.length() == 0) {
			externalFontPath = externalFontPathDefault;
		}
		if (!externalFontPath.startsWith(SystemConfig.getExternalPath())) {
			String oldPath = Environment.getExternalStorageDirectory().toString() + "/" + SystemConfig.myFolderName + "/";
			if (externalFontPath.startsWith(oldPath)) {
				externalFontPath = SystemConfig.getExternalPath() + externalFontPath.substring(oldPath.length());
			}
		}
		
		final File userTypefaceFile = new File(externalFontPath);
		if (userTypefaceFile.exists()) {
			userTypeface = Typeface.createFromFile(externalFontPath);
		}

		setOrientation(isLandscape);

		if (T) Log.v(TAG, M()+"@out: version="+version);
		return version;
	}


	/**
	 * 設定保存
	 * @return 成功ならtrue
	 */
	public static boolean saveConfig() {
		if (T) Log.v(TAG, M()+"@in");

		boolean ret = saveConfigCore(false);

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 設定保存
	 * @return 成功ならtrue
	 */
	public static boolean exportConfig() {
		if (T) Log.v(TAG, M()+"@in");

		boolean ret = saveConfigCore(true);

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 設定保存
	 * @param exporting 設定を外部ファイルに出力するときtrue
	 * @return 成功ならtrue
	 */
	private static boolean saveConfigCore(final boolean exporting) {
		if (T) Log.v(TAG, M()+"@in: exporting="+exporting);

		String filename;
		boolean result = false;

		if (exporting) {
			filename = SystemConfig.getExternalPath() + SystemConfig.systemConfigExportFileName;
		} else {
			filename = "system";
		}
		final MySharedPreferences pref = new MySharedPreferences(me, filename);
		if (exporting) {
			pref.setActivity(ActivitySystemConfig.getActivity());
		}
		int ret = pref.readSharedPreferences();
		if (ret < 1) {
			ActivityMain.myShortToastShow(me.getString(R.string.activitymain_java_error_saveconfig));
			if (T) Log.v(TAG, M()+"@out: result="+result);
			return result;
		}
		final MySharedPreferences.Editor editor = pref.edit();

		editor.clear();
		
		final String version = getVersionString();
		editor.putString("fileVersion", version);

		editor.putInt("verbose", verbose);

		editor.putInt("rotatemode", rotateMode);
		editor.putBoolean("canrotate180", canRotate180);

		editor.putInt("datemode", dateMode);

		editor.putInt("channellogbuffersize", channelLogBufferSize);
		editor.putInt("otherchannellogbuffersize", otherChannelLogBufferSize);
		editor.putInt("spanchannelloglines", spanChannelLogLines);
		editor.putInt("spanotherchannelloglines", spanOtherChannelLogLines);

		editor.putInt("mainlogbuffersize", mainLogBufferSize);
		editor.putInt("sublogbuffersize", subLogBufferSize);

		for (int i=0; i<maxColorSet; i++) {
			editor.putInt("mainlogbackgroundcolor"+i, mainLogBackgroundColor[i]);
			editor.putInt("mainlogdatecolor"+i, mainLogDateColor[i]);
			editor.putInt("mainlogtextcolor"+i, mainLogTextColor[i]);
			editor.putInt("mainlogpaletextcolor"+i, mainLogPaleTextColor[i]);
			editor.putInt("mainlogemphasistextcolor"+i, mainLogEmphasisTextColor[i]);
			editor.putInt("sublogbackgroundcolor"+i, subLogBackgroundColor[i]);
			editor.putInt("sublogdatecolor"+i, subLogDateColor[i]);
			editor.putInt("sublogtextcolor"+i, subLogTextColor[i]);
			editor.putInt("sublogpaletextcolor"+i, subLogPaleTextColor[i]);
			editor.putInt("sublogemphasistextcolor"+i, subLogEmphasisTextColor[i]);
			editor.putInt("alertkeywordscolor2"+i, alertKeywordsColor2[i]);
			editor.putInt("alertlinecolor"+i, alertLineColor[i]);
		}

		editor.putBoolean("ringonalert", ringOnAlert);
		editor.putBoolean("vibrateonalert", vibrateOnAlert);
		editor.putBoolean("highlightonalert", highlightOnAlert);
		editor.putBoolean("copytosystemchannelonalert", copyToSystemChannelOnAlert);
		editor.putBoolean("notifyonalert", notifyOnAlert);
		editor.putBoolean("nosleepmode", noSleepMode);

		editor.putInt("ringlevel", ringLevel);

		editor.putInt("autoreconnectwaitsec", autoReconnectWaitSec);
		editor.putInt("checkconnectwaitsec", checkConnectWaitSec);

		editor.putBoolean("portrait_horizontalmode", portrait_horizontalMode);
		editor.putBoolean("portrait_swapwindowmode", portrait_swapWindowMode);
		editor.putInt("portrait_subwindowmode", portrait_subWindowMode);
		editor.putInt("portrait_mainlogfontsize", portrait_mainLogFontSize);
		editor.putInt("portrait_mainlogfontface", portrait_mainLogFontFace);
		editor.putInt("portrait_mainloglinespacing", portrait_mainLogLineSpacing);
		editor.putInt("portrait_sublogfontsize", portrait_subLogFontSize);
		editor.putInt("portrait_sublogfontface", portrait_subLogFontFace);
		editor.putInt("portrait_subloglinespacing", portrait_subLogLineSpacing);
		editor.putInt("portrait_editfontsize", portrait_editFontSize);
		editor.putInt("portrait_editfontface", portrait_editFontFace);
		editor.putBoolean("portrait_button_chmove", portrait_buttonChMove);
		editor.putBoolean("portrait_button_ch", portrait_buttonCh);
		editor.putBoolean("portrait_button_u", portrait_buttonU);
		editor.putBoolean("portrait_button_appli", portrait_buttonAppli);
		editor.putBoolean("portrait_button_website", portrait_buttonWebsite);
		editor.putInt("portrait_button_size", portrait_buttonSize);
		editor.putBoolean("portrait_button_wide", portrait_buttonWide);
		// editor.putInt("portrait_colorset", portrait_colorSet);
		editor.putBoolean("portrait_showstatusbar", portrait_showStatusBar);
		editor.putBoolean("portrait_showtitlebar", portrait_showTitleBar);

		editor.putBoolean("landscape_horizontalmode", landscape_horizontalMode);
		editor.putBoolean("landscape_swapwindowmode", landscape_swapWindowMode);
		editor.putInt("landscape_subwindowmode", landscape_subWindowMode);
		editor.putInt("landscape_mainlogfontsize", landscape_mainLogFontSize);
		editor.putInt("landscape_mainlogfontface", landscape_mainLogFontFace);
		editor.putInt("landscape_mainloglinespacing", landscape_mainLogLineSpacing);
		editor.putInt("landscape_sublogfontsize", landscape_subLogFontSize);
		editor.putInt("landscape_sublogfontface", landscape_subLogFontFace);
		editor.putInt("landscape_subloglinespacing", landscape_subLogLineSpacing);
		editor.putInt("landscape_editfontsize", landscape_editFontSize);
		editor.putInt("landscape_editfontface", landscape_editFontFace);
		editor.putBoolean("landscape_button_chmove", landscape_buttonChMove);
		editor.putBoolean("landscape_button_ch", landscape_buttonCh);
		editor.putBoolean("landscape_button_u", landscape_buttonU);
		editor.putBoolean("landscape_button_appli", landscape_buttonAppli);
		editor.putBoolean("landscape_button_website", landscape_buttonWebsite);
		editor.putInt("landscape_button_size", landscape_buttonSize);
		editor.putBoolean("landscape_button_wide", landscape_buttonWide);
		// editor.putInt("landscape_colorset", landscape_colorSet);
		editor.putBoolean("landscape_showstatusbar", landscape_showStatusBar);
		editor.putBoolean("landscape_showtitlebar", landscape_showTitleBar);

		// カラーセットは縦横共通で扱う
		editor.putInt("nowcolorset", now_colorSet);

		editor.putString("tig_oldrtcmd", TIG_oldRTCmd);
		editor.putBoolean("twittersiteismobile", twitterSiteIsMobile);

		editor.putBoolean("allowsendhalfkana", allowSendHalfKana);
		editor.putBoolean("inputsingleline", inputSingleLine);

		for (int i=0; i<maxExApp; i++) {
			editor.putString("exapp_name_"+i, exAppName[i]);
			editor.putString("exapp_packagename_"+i, exAppPackageName[i]);
			editor.putString("exapp_activityname_"+i, exAppActivityName[i]);
		}

		for (int i=0; i<maxExWebSite; i++) {
			editor.putString("exwebsite_name_"+i, exWebSiteName[i]);
			editor.putString("exwebsite_url_"+i, exWebSiteUrl[i]);
		}

		editor.putBoolean("showlowmemoryicon", showLowMemoryIcon);
		editor.putBoolean("showservericon", showServerIcon);
		editor.putBoolean("fixsystemicon", fixSystemIcon);

		editor.putBoolean("cmdlongpresschanneltonotified", cmdLongPressChannelToNotified);

		editor.putString("externalfontpath", externalFontPath);

		result = (editor.commit() >= 1);

		if (T) Log.v(TAG, M()+"@out: result="+result);
		return result;
	}

	/**
	 * コンテキスト設定
	 * @param context コンテキスト
	 */
	public static void setContext(final Context context) {
		if (me == null) {
			me = context;
		}
	}

	/**
	 * 回転方向設定
	 * @param orientation 横長ならtrue
	 */
	public static void setOrientation(final boolean orientation) {
		isLandscape = orientation;
		if (isLandscape) {
			now_horizontalMode = landscape_horizontalMode;
			now_swapWindowMode = landscape_swapWindowMode;
			now_subWindowMode = landscape_subWindowMode;
			now_mainLogFontSize = landscape_mainLogFontSize;
			now_mainLogFontFace = landscape_mainLogFontFace;
			now_mainLogLineSpacing = landscape_mainLogLineSpacing;
			now_subLogFontSize = landscape_subLogFontSize;
			now_subLogFontFace = landscape_subLogFontFace;
			now_subLogLineSpacing = landscape_subLogLineSpacing;
			now_editFontSize = landscape_editFontSize;
			now_editFontFace = landscape_editFontFace;
			now_buttonChMove = landscape_buttonChMove;
			now_buttonCh = landscape_buttonCh;
			now_buttonU = landscape_buttonU;
			now_buttonAppli = landscape_buttonAppli;
			now_buttonWebsite = landscape_buttonWebsite;
			now_buttonSize = landscape_buttonSize;
			now_buttonWide = landscape_buttonWide;
			// now_colorSet = landscape_colorSet;
			now_showStatusBar = landscape_showStatusBar;
			now_showTitleBar = landscape_showTitleBar;
		} else {
			now_horizontalMode = portrait_horizontalMode;
			now_swapWindowMode = portrait_swapWindowMode;
			now_subWindowMode = portrait_subWindowMode;
			now_mainLogFontSize = portrait_mainLogFontSize;
			now_mainLogFontFace = portrait_mainLogFontFace;
			now_mainLogLineSpacing = portrait_mainLogLineSpacing;
			now_subLogFontSize = portrait_subLogFontSize;
			now_subLogFontFace = portrait_subLogFontFace;
			now_subLogLineSpacing = portrait_subLogLineSpacing;
			now_editFontSize = portrait_editFontSize;
			now_editFontFace = portrait_editFontFace;
			now_buttonChMove = portrait_buttonChMove;
			now_buttonCh = portrait_buttonCh;
			now_buttonU = portrait_buttonU;
			now_buttonAppli = portrait_buttonAppli;
			now_buttonWebsite = portrait_buttonWebsite;
			now_buttonSize = portrait_buttonSize;
			now_buttonWide = portrait_buttonWide;
			// now_colorSet = portrait_colorSet;
			now_showStatusBar = portrait_showStatusBar;
			now_showTitleBar = portrait_showTitleBar;
		}
		setTypeface();
	}

	/**
	 * @param n フォントフェイス種別
	 * @return フォントフェイス
	 */
	private static Typeface getTypeface(int n) {
		Typeface t = null;
		switch (n) {
		case 1:
			t = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
			break;
		case 2:
			t = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
			break;
		case 3:
			t = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
			break;
		case 4:
			if (userTypeface != null) {
				t = userTypeface;
			} else {
				t = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
			}
			break;
		}
		if (t == null){
			t = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
		}
		return t;
	}

	/**
	 * フォントフェイスの設定処理
	 */
	public static void setTypeface() {
		mainLogTypeface = getTypeface(now_mainLogFontFace);
		subLogTypeface = getTypeface(now_subLogFontFace);
		editTypeface = getTypeface(now_editFontFace);
	}

	/**
	 * 現在の回転方向取得
	 * @return 回転方向 横長ならtrue
	 */
	public static boolean getOrientation() {
		return isLandscape;
	}

	/**
	 * PreferenceActivityから設定を転送
	 * @param sp 情報入出力先
	 * @return リブートレベル（bit0=要画面再構成、bit1=要サービス再起動、bit2=ログクリア）
	 */
	public static int getFromPreferenceActivity(final SharedPreferences sp) {
		if (T) Log.v(TAG, M()+"@in: sp="+sp);

		int rebootLevel = 0;

		final int back_verbose = verbose;
		final int back_rotateMode = rotateMode;
		final boolean back_canRotate180 = canRotate180;
		final int back_now_editFontSize = now_editFontSize;
		final int back_now_editFontFace = now_editFontFace;
		final int back_now_mainLogFontSize = now_mainLogFontSize;
		final int back_now_mainLogFontFace = now_mainLogFontFace;
		final int back_now_mainLogLineSpacing = now_mainLogLineSpacing;
		final int back_now_subLogFontSize = now_subLogFontSize;
		final int back_now_subLogFontFace = now_subLogFontFace;
		final int back_now_subLogLineSpacing = now_subLogLineSpacing;
		final boolean back_now_horizontalMode = now_horizontalMode;
		final boolean back_now_swapWindowMode = now_swapWindowMode;
		final int back_now_subWindowMode = now_subWindowMode;
		final int back_spanChannelLogLines = spanChannelLogLines;
		final int back_spanOtherChannelLogLines = spanOtherChannelLogLines;
		final int back_mainLogBufferSize = mainLogBufferSize;
		final int back_subLogBufferSize = subLogBufferSize;
		final boolean back_ringOnAlert = ringOnAlert;
		final boolean back_vibrateOnAlert = vibrateOnAlert;
		final boolean back_highlightOnAlert = highlightOnAlert;
		final boolean back_copyToSystemChannelOnAlert = copyToSystemChannelOnAlert;
		final boolean back_notifyOnAlert = notifyOnAlert;
		final boolean back_cmdLongPressChannelToNotified = cmdLongPressChannelToNotified;
		final int back_ringLevel = ringLevel;
		final boolean back_noSleepMode = noSleepMode;
		final boolean back_allowSendHalfKana = allowSendHalfKana;
		final boolean back_inputSingleLine = inputSingleLine;
		final boolean back_showLowMemoryIcon = showLowMemoryIcon;
		final boolean back_showServerIcon = showServerIcon;
		final boolean back_fixSystemIcon = fixSystemIcon;
		final boolean back_now_buttonChMove = now_buttonChMove;
		final boolean back_now_buttonCh = now_buttonCh;
		final boolean back_now_buttonU = now_buttonU;
		final boolean back_now_buttonAppli = now_buttonAppli;
		final boolean back_now_buttonWebsite = now_buttonWebsite;
		final int back_now_buttonSize = now_buttonSize;
		final boolean back_now_buttonWide = now_buttonWide;
		final int back_now_colorSet = now_colorSet;
		final boolean back_now_showStatusBar = now_showStatusBar;
		final boolean back_now_showTitleBar = now_showTitleBar;
		final String back_externalFontPath = externalFontPath;

		verbose = sp_getInt(sp, "pref_sys_advanced_verbose", verbose);

		rotateMode = sp_getInt(sp, "pref_sys_view_rotatemode", rotateMode);
		canRotate180 = sp_getBoolean(sp, "pref_sys_view_canrotate180", canRotate180);

		now_editFontSize = sp_getInt(sp, "pref_sys_view_edit_fontsize", now_editFontSize);
		now_editFontFace = sp_getInt(sp, "pref_sys_view_edit_fontface", now_editFontFace);
		now_mainLogFontSize = sp_getInt(sp, "pref_sys_view_mainlog_fontsize", now_mainLogFontSize);
		now_mainLogFontFace = sp_getInt(sp, "pref_sys_view_mainlog_fontface", now_mainLogFontFace);
		now_mainLogLineSpacing = sp_getInt(sp, "pref_sys_view_mainlog_linespacing", now_mainLogLineSpacing);
		now_subLogFontSize = sp_getInt(sp, "pref_sys_view_sublog_fontsize", now_subLogFontSize);
		now_subLogFontFace = sp_getInt(sp, "pref_sys_view_sublog_fontface", now_subLogFontFace);
		now_subLogLineSpacing = sp_getInt(sp, "pref_sys_view_sublog_linespacing", now_subLogLineSpacing);
		viewLayout = sp_getInt(sp, "pref_sys_view_layout", viewLayout);
		now_horizontalMode = ((viewLayout & 2) != 0);
		now_swapWindowMode = ((viewLayout & 1) != 0);
		now_subWindowMode = sp_getInt(sp, "pref_sys_view_windowsize", now_subWindowMode);

		spanChannelLogLines = sp_getInt(sp, "pref_sys_advanced_log_lines_mainlog", spanChannelLogLines);
		spanOtherChannelLogLines = sp_getInt(sp, "pref_sys_advanced_log_lines_sublog", spanOtherChannelLogLines);
		mainLogBufferSize = sp_getInt(sp, "pref_sys_advanced_log_buffer_mainlog", mainLogBufferSize);
		subLogBufferSize = sp_getInt(sp, "pref_sys_advanced_log_buffer_sublog", subLogBufferSize);

		autoReconnectWaitSec = sp_getInt(sp, "pref_sys_advanced_reconnect_interval", autoReconnectWaitSec);
		checkConnectWaitSec = sp_getInt(sp, "pref_sys_advanced_keepalive_interval", checkConnectWaitSec);

		TIG_oldRTCmd = sp_getString(sp, "pref_sys_advanced_tigmode_oldrtcmd", TIG_oldRTCmd);
		twitterSiteIsMobile = sp_getBoolean(sp, "pref_sys_advanced_tigmode_twittersiteismobile", twitterSiteIsMobile);

		ringOnAlert = sp_getBoolean(sp, "pref_sys_action_alert_ring", ringOnAlert);
		vibrateOnAlert = sp_getBoolean(sp, "pref_sys_action_alert_vibrate", vibrateOnAlert);
		highlightOnAlert = sp_getBoolean(sp, "pref_sys_action_alert_highlight", highlightOnAlert);
		copyToSystemChannelOnAlert = sp_getBoolean(sp, "pref_sys_action_alert_copytosystemchannel", copyToSystemChannelOnAlert);
		notifyOnAlert = sp_getBoolean(sp, "pref_sys_action_alert_notify", notifyOnAlert);
		noSleepMode = sp_getBoolean(sp, "pref_sys_action_nosleep", noSleepMode);
		
		cmdLongPressChannelToNotified = sp_getBoolean(sp, "pref_sys_action_alert_longpresschannel", cmdLongPressChannelToNotified);

		ringLevel = sp_getInt(sp, "pref_sys_action_alert_ring_level", ringLevel);

		allowSendHalfKana = sp_getBoolean(sp, "pref_sys_input_send_halfkana", allowSendHalfKana);
		inputSingleLine = sp_getBoolean(sp, "pref_sys_input_singleline", inputSingleLine);

		showLowMemoryIcon = sp_getBoolean(sp, "pref_sys_icon_lowmemory", showLowMemoryIcon);
		showServerIcon = sp_getBoolean(sp, "pref_sys_icon_showserver", showServerIcon);
		fixSystemIcon = sp_getBoolean(sp, "pref_sys_icon_fixsystem", fixSystemIcon);

		now_buttonChMove = sp_getBoolean(sp, "pref_sys_view_button_chmove", now_buttonChMove);
		now_buttonCh = sp_getBoolean(sp, "pref_sys_view_button_ch", now_buttonCh);
		now_buttonU = sp_getBoolean(sp, "pref_sys_view_button_u", now_buttonU);
		now_buttonAppli = sp_getBoolean(sp, "pref_sys_view_button_appli", now_buttonAppli);
		now_buttonWebsite = sp_getBoolean(sp, "pref_sys_view_button_website", now_buttonWebsite);
		now_buttonSize = sp_getInt(sp, "pref_sys_view_button_size", now_buttonSize);
		now_buttonWide = sp_getBoolean(sp, "pref_sys_view_button_wide", now_buttonWide);

		now_colorSet = sp_getInt(sp, "pref_sys_view_colorset", now_colorSet);

		now_showStatusBar = sp_getBoolean(sp, "pref_sys_view_show_statusbar", now_showStatusBar);
		now_showTitleBar = sp_getBoolean(sp, "pref_sys_view_show_titlebar", now_showTitleBar);

		externalFontPath = sp_getString(sp, "pref_sys_advanced_externalfontpath", externalFontPath);
		if (externalFontPath.length() == 0) {
			externalFontPath = externalFontPathDefault;
		}
		
		if (isLandscape) {
			landscape_editFontSize = now_editFontSize;
			landscape_editFontFace = now_editFontFace;
			landscape_mainLogFontSize = now_mainLogFontSize;
			landscape_mainLogFontFace = now_mainLogFontFace;
			landscape_mainLogLineSpacing = now_mainLogLineSpacing;
			landscape_subLogFontSize = now_subLogFontSize;
			landscape_subLogFontFace = now_subLogFontFace;
			landscape_subLogLineSpacing = now_subLogLineSpacing;
			landscape_horizontalMode = now_horizontalMode;
			landscape_swapWindowMode = now_swapWindowMode;
			landscape_subWindowMode = now_subWindowMode;
			landscape_buttonChMove = now_buttonChMove;
			landscape_buttonCh = now_buttonCh;
			landscape_buttonU = now_buttonU;
			landscape_buttonAppli = now_buttonAppli;
			landscape_buttonWebsite = now_buttonWebsite;
			landscape_buttonSize = now_buttonSize;
			landscape_buttonWide = now_buttonWide;
			// landscape_colorSet = now_colorSet;
			landscape_showStatusBar = now_showStatusBar;
			landscape_showTitleBar = now_showTitleBar;
		} else {
			portrait_editFontSize = now_editFontSize;
			portrait_editFontFace = now_editFontFace;
			portrait_mainLogFontSize = now_mainLogFontSize;
			portrait_mainLogFontFace = now_mainLogFontFace;
			portrait_mainLogLineSpacing = now_mainLogLineSpacing;
			portrait_subLogFontSize = now_subLogFontSize;
			portrait_subLogFontFace = now_subLogFontFace;
			portrait_subLogLineSpacing = now_subLogLineSpacing;
			portrait_horizontalMode = now_horizontalMode;
			portrait_swapWindowMode = now_swapWindowMode;
			portrait_subWindowMode = now_subWindowMode;
			portrait_buttonChMove = now_buttonChMove;
			portrait_buttonCh = now_buttonCh;
			portrait_buttonU = now_buttonU;
			portrait_buttonAppli = now_buttonAppli;
			portrait_buttonWebsite = now_buttonWebsite;
			portrait_buttonSize = now_buttonSize;
			portrait_buttonWide = now_buttonWide;
			// portrait_colorSet = now_colorSet;
			portrait_showStatusBar = now_showStatusBar;
			portrait_showTitleBar = now_showTitleBar;
		}

		if (back_verbose != verbose) rebootLevel |= 2;
		if (back_rotateMode != rotateMode) rebootLevel |= 1;
		if (back_canRotate180 != canRotate180) rebootLevel |= 1;
		if (back_now_editFontSize != now_editFontSize) rebootLevel |= 1;
		if (back_now_editFontFace != now_editFontFace) rebootLevel |= 1;
		if (back_now_mainLogFontSize != now_mainLogFontSize) rebootLevel |= 1;
		if (back_now_mainLogFontFace != now_mainLogFontFace) rebootLevel |= 1;
		if (back_now_mainLogLineSpacing != now_mainLogLineSpacing) rebootLevel |= 1;
		if (back_now_subLogFontSize != now_subLogFontSize) rebootLevel |= 1;
		if (back_now_subLogFontFace != now_subLogFontFace) rebootLevel |= 1;
		if (back_now_subLogLineSpacing != now_subLogLineSpacing) rebootLevel |= 1;
		if (back_now_horizontalMode != now_horizontalMode) rebootLevel |= 1;
		if (back_now_swapWindowMode != now_swapWindowMode) rebootLevel |= 1;
		if (back_now_subWindowMode != now_subWindowMode) rebootLevel |= 1;
		if (back_spanChannelLogLines != spanChannelLogLines) rebootLevel |= 2;
		if (back_spanOtherChannelLogLines != spanOtherChannelLogLines) rebootLevel |= 2;
		if (back_mainLogBufferSize != mainLogBufferSize) rebootLevel |= 2;
		if (back_subLogBufferSize != subLogBufferSize) rebootLevel |= 2;
		if (back_ringOnAlert != ringOnAlert) rebootLevel |= 2;
		if (back_vibrateOnAlert != vibrateOnAlert) rebootLevel |= 2;
		if (back_highlightOnAlert != highlightOnAlert) rebootLevel |= 2;
		if (back_copyToSystemChannelOnAlert != copyToSystemChannelOnAlert) rebootLevel |= 2;
		if (back_notifyOnAlert != notifyOnAlert) rebootLevel |= 2;
		if (back_noSleepMode != noSleepMode) rebootLevel |= 8;
		if (back_cmdLongPressChannelToNotified != cmdLongPressChannelToNotified) rebootLevel |= 2;
		if (back_ringLevel != ringLevel) rebootLevel |= 2;
		if (back_allowSendHalfKana != allowSendHalfKana) rebootLevel |= 2;
		if (back_inputSingleLine != inputSingleLine) rebootLevel |= 2;
		if (back_showLowMemoryIcon != showLowMemoryIcon) rebootLevel |= 2;
		if (back_showServerIcon != showServerIcon) rebootLevel |= 2;
		if (back_fixSystemIcon != fixSystemIcon) rebootLevel |= 2;
		if (back_now_buttonChMove != now_buttonChMove) rebootLevel |= 1;
		if (back_now_buttonCh != now_buttonCh) rebootLevel |= 1;
		if (back_now_buttonU != now_buttonU) rebootLevel |= 1;
		if (back_now_buttonAppli != now_buttonAppli) rebootLevel |= 1;
		if (back_now_buttonWebsite != now_buttonWebsite) rebootLevel |= 1;
		if (back_now_buttonSize != now_buttonSize) rebootLevel |= 1;
		if (back_now_buttonWide != now_buttonWide) rebootLevel |= 1;
		if (back_now_colorSet != now_colorSet) rebootLevel |= 1|2|4;
		if (back_now_showStatusBar != now_showStatusBar) rebootLevel |= 8;
		if (back_now_showTitleBar != now_showTitleBar) rebootLevel |= 8;
		if (back_externalFontPath != externalFontPath) {
			rebootLevel |= 1;
			userTypeface = null;
			final File userTypefaceFile = new File(externalFontPath);
			if (userTypefaceFile.exists()) {
				userTypeface = Typeface.createFromFile(externalFontPath);
			}
		}

		if (T) Log.v(TAG, M()+"@out: rebootLevel="+rebootLevel);
		return rebootLevel;
	}
	private static int sp_getInt(final SharedPreferences sp, final String reg, final int defparam) {
		int ret = defparam;
		try {
			ret = Integer.valueOf(sp.getString(reg, Integer.toString(defparam)));
		} catch (final Exception e) {
			//
		}
		return ret;
	}
	private static String sp_getString(final SharedPreferences sp, final String reg, final String defparam) {
		return sp.getString(reg, defparam);
	}
	private static boolean sp_getBoolean(final SharedPreferences sp, final String reg, final boolean defparam) {
		return sp.getBoolean(reg, defparam);
	}

	/**
	 * PreferenceActivityへ設定を転送
	 * @param sp 情報入出力先
	 */
	public static void setForPreferenceActivity(final SharedPreferences sp) {
		if (T) Log.v(TAG, M()+"@in: sp="+sp);


		final SharedPreferences.Editor spe = sp.edit();

		spe_putInt(spe, "pref_sys_advanced_verbose", verbose);

		spe_putInt(spe, "pref_sys_view_rotatemode", rotateMode);
		spe_putBoolean(spe, "pref_sys_view_canrotate180", canRotate180);

		spe_putInt(spe, "pref_sys_view_edit_fontsize", now_editFontSize);
		spe_putInt(spe, "pref_sys_view_edit_fontface", now_editFontFace);
		spe_putInt(spe, "pref_sys_view_mainlog_fontsize", now_mainLogFontSize);
		spe_putInt(spe, "pref_sys_view_mainlog_fontface", now_mainLogFontFace);
		spe_putInt(spe, "pref_sys_view_mainlog_linespacing", now_mainLogLineSpacing);
		spe_putInt(spe, "pref_sys_view_sublog_fontsize", now_subLogFontSize);
		spe_putInt(spe, "pref_sys_view_sublog_fontface", now_subLogFontFace);
		spe_putInt(spe, "pref_sys_view_sublog_linespacing", now_subLogLineSpacing);
		viewLayout = (now_horizontalMode ? 2 : 0) + (now_swapWindowMode ? 1 : 0);
		spe_putInt(spe, "pref_sys_view_layout", viewLayout);
		spe_putInt(spe, "pref_sys_view_windowsize", now_subWindowMode);

		spe_putInt(spe, "pref_sys_advanced_log_lines_mainlog", spanChannelLogLines);
		spe_putInt(spe, "pref_sys_advanced_log_lines_sublog", spanOtherChannelLogLines);
		spe_putInt(spe, "pref_sys_advanced_log_buffer_mainlog", mainLogBufferSize);
		spe_putInt(spe, "pref_sys_advanced_log_buffer_sublog", subLogBufferSize);

		spe_putInt(spe, "pref_sys_advanced_reconnect_interval", autoReconnectWaitSec);
		spe_putInt(spe, "pref_sys_advanced_keepalive_interval", checkConnectWaitSec);

		spe_putString(spe, "pref_sys_advanced_tigmode_oldrtcmd", TIG_oldRTCmd);
		spe_putBoolean(spe, "pref_sys_advanced_tigmode_twittersiteismobile", twitterSiteIsMobile);

		spe_putBoolean(spe, "pref_sys_action_alert_ring", ringOnAlert);
		spe_putBoolean(spe, "pref_sys_action_alert_vibrate", vibrateOnAlert);
		spe_putBoolean(spe, "pref_sys_action_alert_highlight", highlightOnAlert);
		spe_putBoolean(spe, "pref_sys_action_alert_copytosystemchannel", copyToSystemChannelOnAlert);
		spe_putBoolean(spe, "pref_sys_action_alert_notify", notifyOnAlert);
		spe_putBoolean(spe, "pref_sys_action_alert_longpresschannel", cmdLongPressChannelToNotified);

		spe_putInt(spe, "pref_sys_action_alert_ring_level", ringLevel);

		spe_putBoolean(spe, "pref_sys_action_nosleep", noSleepMode);

		spe_putBoolean(spe, "pref_sys_input_send_halfkana", allowSendHalfKana);
		spe_putBoolean(spe, "pref_sys_input_singleline", inputSingleLine);

		spe_putBoolean(spe, "pref_sys_icon_lowmemory", showLowMemoryIcon);
		spe_putBoolean(spe, "pref_sys_icon_showserver", showServerIcon);
		spe_putBoolean(spe, "pref_sys_icon_fixsystem", fixSystemIcon);

		spe_putBoolean(spe, "pref_sys_view_button_chmove", now_buttonChMove);
		spe_putBoolean(spe, "pref_sys_view_button_ch", now_buttonCh);
		spe_putBoolean(spe, "pref_sys_view_button_u", now_buttonU);
		spe_putBoolean(spe, "pref_sys_view_button_appli", now_buttonAppli);
		spe_putBoolean(spe, "pref_sys_view_button_website", now_buttonWebsite);
		spe_putInt(spe, "pref_sys_view_button_size", now_buttonSize);
		spe_putBoolean(spe, "pref_sys_view_button_wide", now_buttonWide);

		spe_putInt(spe, "pref_sys_view_colorset", now_colorSet);

		spe_putBoolean(spe, "pref_sys_view_show_statusbar", now_showStatusBar);
		spe_putBoolean(spe, "pref_sys_view_show_titlebar", now_showTitleBar);

		spe_putString(spe, "pref_sys_advanced_externalfontpath", externalFontPath);

		spe.commit();

		if (T) Log.v(TAG, M()+"@out");
	}
	private static void spe_putInt(final Editor spe, final String reg, final int param) {
		spe.putString(reg, Integer.toString(param));
	}
	private static void spe_putString(final Editor spe, final String reg, final String param) {
		spe.putString(reg, param);
	}
	private static void spe_putBoolean(final Editor spe, final String reg, final Boolean param) {
		spe.putBoolean(reg, param);
	}

	/**
	 * PreferenceActivityへ設定を転送
	 * @param sp 情報入出力先
	 */
	public static void clearForPreferenceActivity(final SharedPreferences sp) {
		if (T) Log.v(TAG, M()+"@in: sp="+sp);

		final SharedPreferences.Editor spe = sp.edit();

		spe.remove("pref_sys_advanced_verbose");

		spe.remove("pref_sys_view_rotatemode");

		spe.remove("pref_sys_view_edit_fontsize");
		spe.remove("pref_sys_view_edit_fontface");
		spe.remove("pref_sys_view_mainlog_fontsize");
		spe.remove("pref_sys_view_mainlog_fontface");
		spe.remove("pref_sys_view_mainlog_linespacing");
		spe.remove("pref_sys_view_sublog_fontsize");
		spe.remove("pref_sys_view_sublog_fontface");
		spe.remove("pref_sys_view_sublog_linespacing");
		spe.remove("pref_sys_view_layout");
		spe.remove("pref_sys_view_windowsize");

		spe.remove("pref_sys_advanced_log_lines_mainlog");
		spe.remove("pref_sys_advanced_log_lines_sublog");
		spe.remove("pref_sys_advanced_log_buffer_mainlog");
		spe.remove("pref_sys_advanced_log_buffer_sublog");

		spe.remove("pref_sys_advanced_reconnect_interval");
		spe.remove("pref_sys_advanced_keepalive_interval");

		spe.remove("pref_sys_advanced_tigmode_oldrtcmd");
		spe.remove("pref_sys_advanced_tigmode_twittersiteismobile");

		spe.remove("pref_sys_action_alert_ring");
		spe.remove("pref_sys_action_alert_vibrate");
		spe.remove("pref_sys_action_alert_highlight");
		spe.remove("pref_sys_action_alert_copytosystemchannel");
		spe.remove("pref_sys_action_alert_longpresschannel");

		spe.remove("pref_sys_action_alert_ring_level");

		spe.remove("pref_sys_action_nosleep");

		spe.remove("pref_sys_input_send_halfkana");
		spe.remove("pref_sys_input_singleline");

		spe.remove("pref_sys_icon_lowmemory");
		spe.remove("pref_sys_icon_showserver");
		spe.remove("pref_sys_icon_fixsystem");

		spe.remove("pref_sys_view_button_chmove");
		spe.remove("pref_sys_view_button_ch");
		spe.remove("pref_sys_view_button_u");
		spe.remove("pref_sys_view_button_appli");
		spe.remove("pref_sys_view_button_website");
		spe.remove("pref_sys_view_button_size");
		spe.remove("pref_sys_view_button_wide");

		spe.remove("pref_sys_view_colorset");

		spe.remove("pref_sys_view_show_statusbar");
		spe.remove("pref_sys_view_show_titlebar");

		spe.remove("pref_sys_advanced_externalfontpath");

		spe.commit();

		if (T) Log.v(TAG, M()+"@out");
	}
	
	/**
	 * @param sp SharedPreferences
	 * @param path path
	 */
	public static void setForPreferenceActivity_ExternalFontPath(final SharedPreferences sp, final String path) {
		final SharedPreferences.Editor spe = sp.edit();

		spe_putString(spe, "pref_sys_advanced_externalfontpath", path);
		
		spe.commit();
	}
}
