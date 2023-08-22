/**
 * 
 * AiCiA - Android IRC Client
 * 
 * Copyright (C)2010 GORRY.
 * 
 */

package net.gorry.aicia;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

/**
 * 
 * IRCメッセージ行形成処理
 * 
 * @author GORRY
 *
 */
public class IRCMsg {
	/** SubLogへエコーするときの内部名 */
	public static final String sOtherChannelServerName = " *other";
	/** Otherチャンネルの内部名 */
	public static final String sOtherChannelName = " *other";
	/** システムチャンネルの内部名 */
	public static final String sSystemChannelName = " *system";

	private static final String sOtherChannelNameDisp = "*other*";
	private static final String sSystemChannelNameDisp = "*system*";

	private static final int sNickColors1[] = {
		Color.rgb(0xfe, 0x4c, 0x4c),
		Color.rgb(0xff, 0x76, 0x32),
		Color.rgb(0xff, 0xbf, 0x00),
		Color.rgb(0xe1, 0xff, 0x00),
		Color.rgb(0x7f, 0xff, 0x00),
		Color.rgb(0x1d, 0xff, 0x00),
		Color.rgb(0x00, 0xff, 0x33),
		Color.rgb(0x00, 0xff, 0x99),
		Color.rgb(0x00, 0xff, 0xff),
		Color.rgb(0x32, 0xb4, 0xff),
		Color.rgb(0x32, 0x69, 0xff),
		Color.rgb(0x58, 0x4c, 0xfe),
		Color.rgb(0x84, 0x32, 0xff),
		Color.rgb(0xcb, 0x32, 0xff),
		Color.rgb(0xff, 0x32, 0xcf),
		Color.rgb(0xfe, 0x4c, 0x90),
	};

	private static final int sNickColors2[] = {
		Color.rgb(0xcc, 0x00, 0x00),
		Color.rgb(0xb2, 0x3e, 0x00),
		Color.rgb(0x99, 0x72, 0x00),
		Color.rgb(0x58, 0x66, 0x00),
		Color.rgb(0x3f, 0x7f, 0x00),
		Color.rgb(0x10, 0x8c, 0x00),
		Color.rgb(0x00, 0x99, 0x21),
		Color.rgb(0x00, 0xa5, 0x63),
		Color.rgb(0x00, 0xb2, 0xb2),
		Color.rgb(0x00, 0x81, 0xcc),
		Color.rgb(0x00, 0x33, 0xcc),
		Color.rgb(0x17, 0x00, 0xcc),
		Color.rgb(0x5f, 0x00, 0xcc),
		Color.rgb(0xaa, 0x00, 0xcc),
		Color.rgb(0xcc, 0x00, 0x9c),
		Color.rgb(0xcc, 0x00, 0x4e),
	};

	/**
	 * 現在時刻の日付時刻文字列を形成
	 * @return 時刻文字列
	 */
	public static String getDateMsg() {
		final Date cal = Calendar.getInstance().getTime();
		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		final String msg = sdf.format(cal);
		return msg;
	}

	/**
	 * カラーリングした日付時刻文字列を形成
	 * @param dateMsg 時刻文字列
	 * @param color 色
	 * @return 形成文字列
	 */
	public static SpannableStringBuilder colorDateMsg(final String dateMsg, final int color) {
		SpannableStringBuilder ssb = null;
		switch (SystemConfig.dateMode) {
		default:
		case 0:
			ssb = new SpannableStringBuilder("");
			break;
		case 1:
			final String msg = dateMsg + " ";
			ssb = new SpannableStringBuilder(msg);
			final ForegroundColorSpan c = new ForegroundColorSpan(color);
			ssb.setSpan(c, 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			break;
		}
		return ssb;
	}

	/**
	 * カラーリングしたnickを形成
	 * @param nick nick
	 * @param color メインテキストの色
	 * @return 形成文字列
	 */
	public static SpannableStringBuilder colorNick(final String nick, final int color) {
		if (nick == null) {
			return new SpannableStringBuilder("");
		}
		final String msg = "<" + nick + "> ";
		final SpannableStringBuilder ssb = new SpannableStringBuilder(msg);
		final char cNick[] = nick.toCharArray();
		int col = 0;
		for (int i=0; i<nick.length(); i++) {
			col += cNick[i];
		}
		final boolean darker = ((Color.red(color)*299 + Color.green(color)*587 + Color.blue(color)*114) < 500);
		final int[] sNickColors = (darker ? sNickColors2 : sNickColors1);
		final ForegroundColorSpan c = new ForegroundColorSpan(sNickColors[col%sNickColors.length]);
		ssb.setSpan(c, 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ssb;
	}

	/**
	 * カラーリングしたテキストを形成
	 * @param msg メッセージ
	 * @param color テキストの色
	 * @return 形成文字列
	 */
	public static SpannableStringBuilder colorText(final String msg, final int color) {
		if (msg == null) {
			return new SpannableStringBuilder("");
		}
		final SpannableStringBuilder ssb = new SpannableStringBuilder(msg);
		final ForegroundColorSpan c = new ForegroundColorSpan(color);
		ssb.setSpan(c, 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ssb;
	}

	/**
	 * 「チャンネル・Nick・メッセージ」から構成される文字列を形成
	 * @param channel チャンネル名
	 * @param nick Nick
	 * @param message メッセージ
	 * @return 形成文字列
	 */

	public static String channelNickMessage(final String channel, final String nick, final String message) {
		if (nick == null) {
			return message;
		}
		return "<" + channelName(channel) + "> <" + nick + "> " + message;
	}


	/**
	 * チャンネル名文字列を形成
	 * @param channel チャンネル名
	 * @return 形成文字列
	 */
	public static String channelName(final String channel) {
		if (channel == null) {
			return "(null)";
		}
		if (channel.equals(sSystemChannelName)) {
			return sSystemChannelNameDisp;
		}
		if (channel.equals(sOtherChannelName)) {
			return sOtherChannelNameDisp;
		}
		return channel;
	}

	/**
	 * 「サーバ名・チャンネル名・トピック」から構成されるタイトル文字列を形成
	 * @param serverName サーバ名
	 * @param channel チャンネル名
	 * @param topic トピック
	 * @return 形成文字列
	 */
	public static String serverChannelTopicTitle(final String serverName, final String channel, final String topic) {
		if (topic == null) {
			return channelName(channel) + " [" + serverName + "] - " + (ActivityMain.mDonate ? "AiCiA (DONATED)" : "AiCiA");
		}
		return channelName(channel) + " " + topic + " - " + (ActivityMain.mDonate ? "AiCiA (DONATED)" : "AiCiA");
	}

	/**
	 * 「サーバ名・チャンネル名」から構成されるToast文字列を形成
	 * @param serverName サーバ名
	 * @param channel チャンネル名
	 * @return 形成文字列
	 */
	public static String serverChannelToast(final String serverName, final String channel) {
		return channelName(channel) + " [" + serverName + "]";
	}

	/**
	 * URL使用可能文字列判定
	 * @param c 文字
	 * @return 使用可能ならtrue
	 */
	public static boolean isUrlChar(final char c)
	{
		if (Character.isLetterOrDigit(c)) return true;
		switch (c) {
		case '!':
		case '#':
		case '$':
		case '%':
		case '&':
		case '\'':
		case '=':
		case '-':
		case '~':
		case '\\':
		case '@':
		case ';':
		case '+':
		case ',':
		case '.':
		case '/':
		case '?':
			return true;
		}
		return false;
	}


}
