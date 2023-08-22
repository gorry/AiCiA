package net.gorry.aicia;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author GORRY
 *
 */
public class MyAlarmManager extends BroadcastReceiver {
	private static final String TAG = "AlarmManagerReceiver";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;//true;

	private static AlarmManager mAlarmManager = null;
	private static Runnable mRunnable;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (I) Log.i(TAG, "onReceive()");
		if (mRunnable != null) {
			try {
				mRunnable.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * アラーム設定
	 * @param context context
	 * @param run run
	 */
	public static void setAlarmManager(Context context, Runnable run) {
		if (I) Log.i(TAG, "setAlarmManager()");
		if (mAlarmManager == null) {
			mRunnable = run;
			Intent intent = new Intent(context, MyAlarmManager.class);
			PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
			long firstTime = SystemClock.elapsedRealtime();
			firstTime += 600 * 1000;
			mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 600 * 1000, sender);
			if (I) Log.i(TAG, "setAlarmManager(): set alarm");
		}
	}

	/**
	 * アラーム解除
	 * @param context context
	 */
	public static void resetAlarmManager(Context context) {
		if (I) Log.i(TAG, "resetAlarmManager()");
		if (mAlarmManager != null) {
			Intent intent = new Intent(context, MyAlarmManager.class);
			PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.cancel(sender);
			mAlarmManager = null;
			if (I) Log.i(TAG, "resetAlarmManager(): reset alarm");
		}

	}

}
