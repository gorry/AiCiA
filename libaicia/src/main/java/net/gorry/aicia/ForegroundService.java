/**
 *
 */
package net.gorry.aicia;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author GORRY
 *
 */

public abstract class ForegroundService extends Service {
	private static final String TAG = "ForegroundService";
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = false;

	private static final Class<?>[] mSetForegroundSignature = new Class[] {
	    boolean.class};
	private static final Class<?>[] mStartForegroundSignature =
		new Class[] { int.class, Notification.class };
	private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

	private final int mNotificationId;

	private NotificationManager mNotificationManager;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	/**
	 * @param id ID
	 */
	public ForegroundService(int id) {
		if (I) Log.i(TAG, "ForegroundService()");
		mNotificationId = id;
	}

	protected NotificationManager getNotificationManager() {
		if (I) Log.i(TAG, "getNotificationManager()");
		return mNotificationManager;
	}

	void invokeMethod(Method method, Object[] args) {
	    try {
	        method.invoke(this, args);
	    } catch (InvocationTargetException e) {
	        // Should not happen.
	        Log.w(TAG, "Unable to invoke method", e);
	    } catch (IllegalAccessException e) {
	        // Should not happen.
	        Log.w(TAG, "Unable to invoke method", e);
	    }
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older APIs if it is not
	 * available.
	 */
	protected void startForegroundCompat(Notification notification) {
		if (I) Log.i(TAG, "startForegroundCompat()");
		if (mStartForeground != null) {
			if (I) Log.i(TAG, "startForegroundCompat(): mStartForeground != null");
			mStartForegroundArgs[0] = Integer.valueOf(mNotificationId);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (Exception e) {
				// Log.e(e);
			}
			return;
		}

		if (I) Log.i(TAG, "startForegroundCompat(): mStartForeground == null");
	    mSetForegroundArgs[0] = Boolean.TRUE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
		mNotificationManager.notify(mNotificationId, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older APIs if it is not
	 * available.
	 */
	protected void stopForegroundCompat() {
		if (I) Log.i(TAG, "stopForegroundCompat()");
		if (mStopForeground != null) {
			if (I) Log.i(TAG, "stopForegroundCompat(): mStopForeground != null");
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (Exception e) {
				// Log.e(e);
			}
			return;
		}

		if (I) Log.i(TAG, "stopForegroundCompat(): mStopForeground == null");
		mNotificationManager.cancel(mNotificationId);
	    mSetForegroundArgs[0] = Boolean.FALSE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
	}

	@Override
	public void onCreate() {
		if (I) Log.i(TAG, "onCreate()");
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
		if (mStartForeground != null) {
			if (I) Log.i(TAG, "mStartForeground() != null");
		} else {
			if (I) Log.i(TAG, "mStartForeground() == null");
		}
	    try {
	        mSetForeground = getClass().getMethod("setForeground", mSetForegroundSignature);
	    } catch (NoSuchMethodException e) {
	        throw new IllegalStateException("OS doesn't have Service.startForeground OR Service.setForeground!");
	    }
	}

	@Override
	public void onDestroy() {
		if (I) Log.i(TAG, "onDestroy()");
		stopForegroundCompat();
	}
}
