/**
 * 
 */
package net.gorry.aicia;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.util.Xml;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.gorry.libaicia.BuildConfig;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author gorry
 *
 */
public class MySharedPreferences {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "AlarmManagerReceiver";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private Context me;
	private Activity myActivity;
	private boolean mShared = false;
	private String mFilename;
	private SharedPreferences mPref;
	private SharedPreferences.Editor mEditor;
	private TreeMap<String, Object> mMap;
	private TreeMap<String, Object> mEditMap;

	/**
	 * @param context activity
	 * @param name name
	 */
	@SuppressWarnings("unchecked")
	public MySharedPreferences(Context context, String name) {
		if (T) Log.v(TAG, M()+"@in: context="+context+", name=" + name);

		me = context;
		mFilename = name;

		if (T) Log.v(TAG, M()+"@out");
	}

	static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 101;
	static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 102;

	public int checkReadPermission() {
		if (T) Log.v(TAG, M()+"@in");

		/*
		int ret = ContextCompat.checkSelfPermission(myActivity, Manifest.permission.READ_EXTERNAL_STORAGE);
		if (ret == PackageManager.PERMISSION_GRANTED) {
			if (T) Log.v(TAG, M()+"@out: ret=0");
			return 1;
		}
		if (ActivityCompat.shouldShowRequestPermissionRationale(myActivity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			Log.e(TAG, "checkReadPermission(): need permission READ_EXTERNAL_STORAGE");
			if (T) Log.v(TAG, M()+"@out: ret=-1");
			return -1;
		}
		ActivityCompat.requestPermissions(
				myActivity,
				new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
				MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
		);

		if (T) Log.v(TAG, M()+"@out: ret=0");
		return 0;
		*/

		// getExternalFilesDir()のアクセスだけになったので常にgranted
		if (T) Log.v(TAG, M()+"@out: granted");
		return 1;
	}

	public int checkWritePermission() {
		if (T) Log.v(TAG, M()+"@in");

		/*
		int ret = ContextCompat.checkSelfPermission(myActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (ret == PackageManager.PERMISSION_GRANTED) {
			if (T) Log.v(TAG, M()+"@out: ret=0");
			return 1;
		}
		if (ActivityCompat.shouldShowRequestPermissionRationale(myActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			Log.e(TAG, "MySharedPreferences(): need permission WRITE_EXTERNAL_STORAGE");
			if (T) Log.v(TAG, M()+"@out: ret=-1");
			return -1;
		}
		ActivityCompat.requestPermissions(
				myActivity,
				new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
				MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
		);

		if (T) Log.v(TAG, M()+"@out: ret=0");
		return 0;
		*/

		// getExternalFilesDir()のアクセスだけになったので常にgranted
		if (T) Log.v(TAG, M()+"@out: granted");
		return 1;
	}

	public int readSharedPreferences() {
		if (T) Log.v(TAG, M()+"@in");

		if (!mFilename.startsWith("/")) {
			mShared = true;
			int mode = ((Build.VERSION.SDK_INT >= 11) ? Context.MODE_MULTI_PROCESS : Context.MODE_PRIVATE);
			mPref = me.getSharedPreferences(mFilename, mode);
			mEditor = mPref.edit();

			if (T) Log.v(TAG, M()+"@out: ret=1");
			return 1;
		}

		int ret = checkReadPermission();
		if (ret < 1) {

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		mShared = false;
		mMap = new TreeMap<String, Object>();
		try {
			FileInputStream istream = new FileInputStream(mFilename);
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(istream, "UTF-8");
			int event = parser.getEventType();
			while (event != XmlPullParser.END_DOCUMENT) {
				switch (event) {
				case XmlPullParser.START_TAG:
					TreeMap<String, Object> map = new TreeMap<String, Object>();
					readValueXml(map, parser);
					if (map.containsKey("")) {
						Object map2 = map.get("");
						if (map2 != null) {
							mMap.putAll((TreeMap<String, Object>)map2);
						}
					}
					break;
				}
				event = parser.next();
			}
			istream.close();
		} catch (org.xmlpull.v1.XmlPullParserException e) {
			e.printStackTrace();
        } catch (FileNotFoundException e) {
			e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
        }

		if (T) Log.v(TAG, M()+"@out: ret=1");
        return 1;
	}

	/**
	 * 値読み込み用XMLパーザ
	 * @param map 値格納用map
	 * @param parser XmlPullParser
	 * @return 処理成功ならtrue
	 */
	private final boolean readValueXml(Map<String, Object> map, XmlPullParser parser) throws XmlPullParserException, IOException {
		if (T) Log.v(TAG, M()+"@in: map="+map+", parser="+parser);

		Object obj = null;
		final String valueName = parser.getAttributeValue(null, "name");
		final String tagName = parser.getName();
		int event;
		boolean result = false;
		
		if (T) Log.v(TAG, M()+"tagName=[" + tagName + "], valueName=[" + valueName + "]");

		if (tagName.equals("null")) {
			result = true;
		} else if (tagName.equals("map")) {
			parser.next();
			result = readMapXml(map, parser, valueName);
			if (T) Log.v(TAG, M()+"@out: result="+result);
			return (result);
		} else if (tagName.equals("int")) {
			String value = parser.getAttributeValue(null, "value");
			if (T) Log.v(TAG, M()+"value=[" + value + "]");
			obj = Integer.parseInt(value);
			map.put(valueName, obj);
			result = true;
		} else if (tagName.equals("boolean")) {
			String value = parser.getAttributeValue(null, "value");
			if (T) Log.v(TAG, M()+"value=[" + value + "]");
			obj = Boolean.valueOf(value);
			map.put(valueName, obj);
			result = true;
		} else if (tagName.equals("string")) {
			String value = "";
			if (T) Log.v(TAG, M()+"<string> start");
			while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (event == XmlPullParser.START_TAG) {
					String tag = parser.getName();
					throw new XmlPullParserException("<string> cannot contains any other tag: <" + tag + ">");
				} else if (event == XmlPullParser.END_TAG) {
					String tag = parser.getName();
					if (tag.equals("string")) {
						if (T) Log.v(TAG, M()+"<string> close: value=[" + value + "]");
						obj = value;
						map.put(valueName, obj);
						result = true;
						if (T) Log.v(TAG, M()+"@out: result="+result);
						return (result);
					}
					throw new XmlPullParserException("<string> must be closed by </string>: </" + tag + ">");
				} else if (event == XmlPullParser.TEXT) {
					String content = parser.getText();
					if (T) Log.v(TAG, M()+"content=[" + content + "]");
					value += content;
				} else {
					throw new XmlPullParserException("cannot parse in <string>: event " + event);
				}
			}
		} else {
			throw new XmlPullParserException("unknown tag <" + tagName + "> start");
		}

		while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				throw new XmlPullParserException("<" + tagName + "> cannot contains any other tag: <" + tag + ">");
			} else if (event == XmlPullParser.END_TAG) {
				String tag = parser.getName();
				if (tag.equals(tagName)) {
					if (T) Log.v(TAG, M()+"<" + tagName + "> close.");
					if (T) Log.v(TAG, M()+"@out: result="+result);
					return result;
				}
				throw new XmlPullParserException("<" + tagName + "> is closed by another tag:  </" + tag + ">");
			} else if (event == XmlPullParser.TEXT) {
				String content = parser.getText();
				if (T) Log.v(TAG, M()+"contains text [" + content + "]");
				// throw new XmlPullParserException("cannot contains text [" + content + "] in <" + tagName + ">");
			} else {
				throw new XmlPullParserException("cannot parse in <" + tagName + ">: event " + event);
			}
		}

		throw new XmlPullParserException("end of document in <" + tagName + ">");
	}

	/**
	 * readMapXml
	 * @param map 値格納用map
	 * @param parser XmlPullParser
	 * @param tagName mapキー名
	 * @return 処理成功ならtrue
	 */
	private final boolean readMapXml(Map<String, Object> map, XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
		if (T) Log.v(TAG, M()+"@in: map="+map+", parser="+parser+", tagName="+tagName);

		TreeMap<String, Object> map2 = new TreeMap<String, Object>();
		boolean result = false;
		String tagName2 = (tagName == null) ? "" : tagName;

		int event = parser.getEventType();
		while (event != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.START_TAG) {
				readValueXml(map2, parser);
			} else if (event == XmlPullParser.END_TAG) {
				String tag = parser.getName();
				if (tag.equals("map")) {
					if (T) Log.v(TAG, M()+"<map> close");
					Object obj = map2;
					map.put(tagName2, obj);
					result = true;
					break;
				}
				throw new XmlPullParserException("<map> must be closed by </map>: </" + tag + ">");
			} else if (event == XmlPullParser.TEXT) {
				String content = parser.getText();
				if (T) Log.v(TAG, M()+"contains text [" + content + "]");
				// throw new XmlPullParserException("cannot contains text [" + content + "] in <" + tagName + ">");
			} else {
				throw new XmlPullParserException("cannot parse in <map>: event " + event);
			}
			event = parser.next();
		}

		if (T) Log.v(TAG, M()+"@out: result="+result);
		return result;
	}

	/**
	 * getInt
	 * @param name name
	 * @param def def
	 * @return getInt
	 */
	public int getInt(String name, int def) {
		if (T) Log.v(TAG, M()+"@in name="+name+", def="+def);
		int value;
		if (mShared) {
			value = mPref.getInt(name, def);
			if (T) Log.v(TAG, M()+"@out: shared: value=[" + value + "]");
			return value;
		}
		if (mMap.containsKey(name)) {
			value = (Integer)mMap.get(name);
		} else {
			if (T) Log.v(TAG, M()+"not have key");
			value = def;
		}
		if (T) Log.v(TAG, M()+"@out: value=[" + value + "]");
		return value;
	}

	/**
	 * getString
	 * @param name name
	 * @param def def
	 * @return getInt
	 */
	public String getString(String name, String def) {
		if (T) Log.v(TAG, M()+"@in name="+name+", def="+def);
		String value;
		if (mShared) {
			value = mPref.getString(name, def);
			if (T) Log.v(TAG, M()+"@out: shared: value=[" + value + "]");
			return value;
		}
		if (mMap.containsKey(name)) {
			value = (String)mMap.get(name);
		} else {
			if (T) Log.v(TAG, M()+"not have key");
			value = def;
		}
		if (T) Log.v(TAG, M()+"@out: value=[" + value + "]");
		return value;
	}

	/**
	 * getBoolean
	 * @param name name
	 * @param def def
	 * @return getInt
	 */
	public boolean getBoolean(String name, boolean def) {
		if (T) Log.v(TAG, M()+"getBoolean(): " + mFilename + ": name=[" + name + "], def=[" + def + "]");
		boolean value;
		if (mShared) {
			value = mPref.getBoolean(name, def);
			if (T) Log.v(TAG, M()+"@out: shared: value=[" + value + "]");
			return value;
		}
		if (mMap.containsKey(name)) {
			value = (Boolean)mMap.get(name);
		} else {
			if (T) Log.v(TAG, M()+"not have key");
			value = def;
		}
		if (T) Log.v(TAG, M()+"@out: value=[" + value + "]");
		return value;
	}

	/**
	 * @author gorry
	 *
	 */
	public final class Editor {

		/**
		 * clear
		 * @return this
		 */
		public MySharedPreferences.Editor clear() {
			if (T) Log.v(TAG, M()+"@in");

			if (mShared) {
				if (T) Log.v(TAG, M()+"clear shared");
				mEditor.clear();
				return this;
			}

			mEditMap.clear();

			if (T) Log.v(TAG, M()+"@out");
			return this;
		}

		/**
		 * commit
		 * @return 成功ならtrue
		 */
		public int commit() {
			if (T) Log.v(TAG, M()+"@in");

			int ret = 0;
			if (mShared) {
				ret = (mEditor.commit() ? 1 : 0);
				if (T) Log.v(TAG, M()+"@out: shared: ret="+ret);
				return ret;
			}

			ret = checkWritePermission();
			if (ret < 1) {
				if (T) Log.v(TAG, M()+"@out: ret="+ret);
				return ret;
			}

			boolean result = false;
			try {
				// 親フォルダを作る
				File file = new File(mFilename);
				String parentDirPath = file.getParent();
				file = new File(parentDirPath);
				if (file.isDirectory()) {
					if (T) Log.v(TAG, M()+"parent Directory ["+parentDirPath+"] exists");
				} else {
					file.mkdirs();
					if (!file.isDirectory()) {
						Log.e(TAG, "cannot create Directory ["+parentDirPath+"]");
						if (T) Log.v(TAG, M()+"@out: ret=0");
						return 0;
					}
					if (T) Log.v(TAG, M()+"parent Directory ["+parentDirPath+"] created");
				}

				FileOutputStream ostream = new FileOutputStream(mFilename);
				XmlSerializer writer = Xml.newSerializer();
				writer.setOutput(ostream, "UTF-8");
				writer.startDocument("UTF-8", true);
				writer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
				writeMapXml(null, mEditMap, writer);
				writer.flush();
				ostream.close();
				result = true;
			} catch (org.xmlpull.v1.XmlPullParserException e) {
				e.printStackTrace();
	        } catch (FileNotFoundException e) {
				// e.printStackTrace();
	        } catch (IOException e) {
				e.printStackTrace();
	        }
			
			if (T) Log.v(TAG, M()+"@out: ret=1");
	        return 1;
		}

		/**
		 * remove
		 * @param name name
		 * @return this
		 */
		public MySharedPreferences.Editor remove(String name) {
			if (T) Log.v(TAG, M()+"@in: name="+name);
			if (mShared) {
				mEditor.remove(name);
				if (T) Log.v(TAG, M()+"@out: shared");
				return this;
			}
			mEditMap.remove(name);

			if (T) Log.v(TAG, M()+"@out");
			return this;
		}

		/**
		 * putInt
		 * @param name name
		 * @param data data
		 * @return this
		 */
		public MySharedPreferences.Editor putInt(String name, int data) {
			if (T) Log.v(TAG, M()+"@in: name=" + name + ", data=" + data);
			if (mShared) {
				mEditor.putInt(name, data);
				if (T) Log.v(TAG, M()+"@out: shared");
				return this;
			}
			mEditMap.put(name, data);
			if (T) Log.v(TAG, M()+"@out");
			return this;
		}

		/**
		 * putString
		 * @param name name
		 * @param data data
		 * @return this
		 */
		public MySharedPreferences.Editor putString(String name, String data) {
			if (T) Log.v(TAG, M()+"@in: name=" + name + ", data=" + data);
			if (mShared) {
				mEditor.putString(name, data);
				if (T) Log.v(TAG, M()+"@out: shared");
				return this;
			}
			mEditMap.put(name, data);
			if (T) Log.v(TAG, M()+"@out");
			return this;
		}

		/**
		 * putBoolean
		 * @param name name
		 * @param data data
		 * @return this
		 */
		public MySharedPreferences.Editor putBoolean(String name, boolean data) {
			if (T) Log.v(TAG, M()+"@in: name=" + name + ", data=" + data);
			if (mShared) {
				if (T) Log.v(TAG, M()+"@out: shared");
				mEditor.putBoolean(name, data);
				return this;
			}
			mEditMap.put(name, data);
			if (T) Log.v(TAG, M()+"@out");
			return this;
		}
	}

	/**
	 * edit
	 * @return edit
	 */
	public Editor edit() {
		if (T) Log.v(TAG, M()+"@in");
		if (mShared) {
			if (T) Log.v(TAG, M()+"@out: shared");
			return new Editor();
		}
		mEditMap = new TreeMap<String, Object>();
		mEditMap.putAll(mMap);
		if (T) Log.v(TAG, M()+"@out");
		return new Editor();
	}

	/**
	 * 値書き込み用XMLライタ
	 * @param name Name
	 * @param data Data
	 * @return 書き込み成功時true
	 */
	private final boolean writeValueXml(String name, Object data, XmlSerializer writer) throws XmlPullParserException, IOException {
		if (T) Log.v(TAG, M()+"@in: name=" + name + ", data=" + data);
		String typeStr = null;
		if (data == null) {
			if (T) Log.v(TAG, M()+"write <null>");
			writer.startTag(null, "null");
			if (name != null) {
				writer.attribute(null, "name", name);
			}
			writer.endTag(null, "null");
			if (T) Log.v(TAG, M()+"@out: true");
			return true;
		} else if (data instanceof String) {
			if (T) Log.v(TAG, M()+"write <string>");
			writer.startTag(null, "string");
			if (name != null) {
				writer.attribute(null, "name", name);
			}
			writer.text(data.toString());
			writer.endTag(null, "string");
			if (T) Log.v(TAG, M()+"@out: true");
			return true;
		} else if (data instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>)data;
			writeMapXml(name, map, writer);
			if (T) Log.v(TAG, M()+"@out: true");
			return true;
		} else if (data instanceof Integer) {
			typeStr = "int";
		} else if (data instanceof Boolean) {
			typeStr = "boolean";
		} else {
			if (T) Log.v(TAG, M()+"writeValueXml(): unknown type");
			throw new RuntimeException("writeValueXml(): unknown type: name=[" + name + "], data=[" + data + "]");
		}

		if (T) Log.v(TAG, M()+"write <" + typeStr + ">");
		writer.startTag(null, typeStr);
		if (name != null) {
			writer.attribute(null, "name", name);
		}
		writer.attribute(null, "value", data.toString());
		writer.endTag(null, typeStr);
		if (T) Log.v(TAG, M()+"@out: true");
		return true;
	}


	/**
	 * Map書き込み用XMLライタ
	 * @param name Name
	 * @param map Map
	 * @return 書き込み成功時true
	 */
	private final boolean writeMapXml(String name, Map<String, Object> map, XmlSerializer writer) throws XmlPullParserException, IOException {
		if (T) Log.v(TAG, M()+"@in: name=" + name);

		if (map == null) {
			writer.startTag(null, "null");
			writer.endTag(null, "null");
			if (T) Log.v(TAG, M()+"@out: true");
			return true;
		}

		writer.startTag(null, "map");
		if (name != null) {
			writer.attribute(null, "name", name);
		}

		Set<?> s = map.entrySet();
		Iterator<?> i = s.iterator();
		while (i.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry entry = (Map.Entry)i.next();
			final String entryname = (String)entry.getKey();
			final Object entrydata = entry.getValue();
			if (T) Log.v(TAG, M()+"map-key=[" + entryname + "]");
			writeValueXml(entryname, entrydata, writer);
		}
		
		writer.endTag(null, "map");
		if (T) Log.v(TAG, M()+"@out: true");
		return true;
	}

	public void setActivity(Activity a) {
		if (T) Log.v(TAG, M()+"@in: activity="+a);

		myActivity = a;

		if (T) Log.v(TAG, M()+"@out");
	}
}
