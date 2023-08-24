/**
 *
 */
package net.gorry.aicia;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import net.gorry.libaicia.BuildConfig;
import net.gorry.libaicia.R;

/**
 * 
 * ファイルリストの取得
 * 
 * @author gorry
 *
 */
public class ActivitySelectTtfFile extends ListActivity {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "ActivitySelectTtfFile";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private String mCurrentFolderName = "";
	private File mCurDir;
	private File[] mDirEntry;
	private SelectTtfFileAdapter mAdapter;
	private String mExtFilenameFilter;
	// private boolean mSelected;
	private String mCurrentFileName;
	private String mLastFolderName = "";

	/* アプリの一時退避
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (T) Log.v(TAG, M()+"@in: outState="+outState);

		outState.putString("mCurrentFolderName", mCurrentFolderName);
		outState.putString("mCurrentFileName", mCurrentFileName);
		outState.putString("mLastFolderName", mLastFolderName);

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		mCurrentFolderName = savedInstanceState.getString("mCurrentFolderName");
		mCurrentFileName = savedInstanceState.getString("mCurrentFileName");
		mLastFolderName = savedInstanceState.getString("mLastFolderName");

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * @param ext 拡張子
	 * @return 拡張子がマッチしたらtrue
	 */
	public FilenameFilter extNameFilter(final String ext) {
		if (T) Log.v(TAG, M()+"@in: ext="+ext);

		mExtFilenameFilter = new String(ext.toLowerCase());
		return new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				if (T) Log.v(TAG, M()+"@in: dir="+dir+", name="+name);

				boolean f = false;
				final File file = new File(dir.getPath() + "/" + name);
				if (!file.isDirectory()) {
					final String lname = name.toLowerCase();
					f = lname.endsWith(mExtFilenameFilter);
				}

				if (T) Log.v(TAG, M()+"@out: f="+f);
				return f;
			}
		};
	}

	/**
	 * @return エントリがdirならtrue
	 */
	public static FileFilter dirEntryFilter() {
		if (T) Log.v(TAG, M()+"@in");

		return new FileFilter() {
			public boolean accept(final File file) {
				if (T) Log.v(TAG, M()+"@in: file="+file);

				boolean f = !(file.isFile());

				if (T) Log.v(TAG, M()+"@out: f="+f);
				return f;
			}
		};
	}

	class compareFileName implements Comparator<File> {
		public int compare(final File f1, final File f2) {
			return (f1.getName().compareToIgnoreCase(f2.getName()));
		}

		public boolean equals(final File f1, final File f2) {
			return (f1.getName().equalsIgnoreCase(f2.getName()));
		}
	}

	/**
	 * @param uri uri
	 */
	public void setFileList(final Uri uri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

		// フォルダ一覧＋指定拡張子ファイル一覧
		final String path = uri.getPath();
		final int idx = path.lastIndexOf('/');
		final String folder;
		final String filename;
		if (idx >= 0) {
			folder = path.substring(0, idx+1);
			filename = path.substring(idx+1);
		} else {
			folder = "/";
			filename = "";
		}
		mCurrentFolderName = folder;
		mCurDir = new File(folder);
		mCurrentFileName = filename;
		final Comparator<File> c1 = new compareFileName();
		File[] dirs = mCurDir.listFiles(dirEntryFilter());
		if (dirs == null) {
			dirs = new File[0];
		}
		Arrays.sort(dirs, c1);
		File[] files = mCurDir.listFiles(extNameFilter(".ttf"));
		if (files == null) {
			files = new File[0];
		}
		Arrays.sort(files, c1);
		final File upDir = new File(folder + "..");
		mDirEntry = new File[1 + dirs.length + files.length];
		mDirEntry[0] = upDir;
		System.arraycopy(dirs, 0, mDirEntry, 1, dirs.length);
		System.arraycopy(files, 0, mDirEntry, 1+dirs.length, files.length);

		int curpos = -1;
		if ((mLastFolderName != null) && (mLastFolderName.length() > 0+1)) {
			// ".."を選んだあと、今までいたフォルダを初期位置にする
			final String s1 = mLastFolderName.substring(0, mLastFolderName.length()-1);
			final int i1 = s1.lastIndexOf('/');
			if (i1 >= 0) {
				final String s2 = s1.substring(i1+1);
				if (s2.length() > 0) {
					for (int i=0; i<mDirEntry.length; i++) {
						if (mDirEntry[i].getName().equals(s2)) {
							curpos = i;
							break;
						}
					}
				}
			}
		}
		if (curpos < 0) {
			for (int i=0; i<mDirEntry.length; i++) {
				if (mDirEntry[i].getName().equals(mCurrentFileName)) {
					curpos = i;
					break;
				}
			}
		}
		mAdapter = new SelectTtfFileAdapter(this, mDirEntry, curpos);
		setListAdapter(mAdapter);

		if (curpos >= 0) {
			getListView().setSelection(curpos);
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in");
		super.onCreate(savedInstanceState);
	}

	/*
	 * 再起動
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public void onRestart() {
		if (T) Log.v(TAG, M()+"@in");
		super.onRestart();

	}

	/*
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (T) Log.v(TAG, M()+"@in");
		super.onStart();
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		if (T) Log.v(TAG, M()+"@in");
		super.onResume();

		// mSelected = false;
		Uri uri;
		final Intent intent = getIntent();
		uri = intent.getData();
		if ((mCurrentFolderName != null) && (mCurrentFolderName.length() > 0)) {
			if ((mCurrentFileName != null) && (mCurrentFileName.length() > 0)) {
				uri = Uri.parse("file://" + mCurrentFolderName + mCurrentFileName);
			} else {
				uri = Uri.parse("file://" + mCurrentFolderName);
			}
		}
		if (uri == null) {
			uri = Uri.parse("file:///");
		}
		setFileList(uri);
		mySetTitle(uri);
	}

	/*
	 * 停止
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		if (T) Log.v(TAG, M()+"@in");
		super.onPause();
	}

	/*
	 * 中止
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		if (T) Log.v(TAG, M()+"@in");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		if (T) Log.v(TAG, M()+"@in");
		super.onDestroy();
	}

	@Override
	public void onContentChanged() {
		if (T) Log.v(TAG, M()+"@in");
		super.onContentChanged();
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int pos, final long id) {
		if (T) Log.v(TAG, M()+"@in: l="+l+", v="+v+", pos="+pos+", id="+id);

		final File file = mDirEntry[pos];
		if (T) Log.v(TAG, M()+"Selected [" + file.getPath() + "]");

		if (file.isDirectory() || (file.getName().equals(".."))) {
			Uri uri = Uri.parse("file://" + file.getPath() + "/");
			final String name = file.getName();
			mLastFolderName = "";
			if (name.equals("..")) {
				uri = Uri.parse("file://" + file.getParentFile().getParent() + "/");
				mLastFolderName = mCurrentFolderName;
			}
			setFileList(uri);
			mySetTitle(uri);
		} else {
			mLastFolderName = "";
			// mSelected = true;
			final Intent intent = new Intent();
			intent.putExtra("path", file.getPath());
			setResult(RESULT_OK, intent);
			finish();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * Urlをタイトルに設定
	 * @param uri Uri
	 */
	public void mySetTitle(final Uri uri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

		final String path = uri.getPath();
		final int idx = path.lastIndexOf('/');
		final String folder;
		if (idx >= 0) {
			folder = path.substring(0, idx+1);
		} else {
			folder = "/";
		}
		setTitle(folder + " " + getString(R.string.activityselectttffile_java_title));

		if (T) Log.v(TAG, M()+"@out");
	}
}
