/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import java.util.EventListener;

import android.text.SpannableStringBuilder;

/**
 * IRCサーバリスト リスナインターフェース
 *
 * @author GORRY
 *
 */
public interface IRCServerListEventListener extends EventListener {
	/**
	 * チャンネルへのメッセージ受信イベントリスナ
	 * @param serverName サーバ設定名
	 * @param channel チャンネル
	 * @param nick Nick
	 * @param dateMsg 日付時刻
	 * @param ssb Spanメッセージ
	 * @param ssbOther サブログ用Spanメッセージ
	 * @param toSublog サブログ用Spanメッセージがあるときtrue
	 * @param alert アラートする必要があるときtrue
	 */
	public void receiveMessageToChannel(String serverName, String channel, String nick, String dateMsg, SpannableStringBuilder ssb, SpannableStringBuilder ssbOther, boolean toSublog, boolean alert);

	/**
	 * サーバ作成イベントリスナ
	 * @param serverName サーバ設定名
	 * @param dateMsg 日付時刻
	 */
	public void createServer(String serverName, String dateMsg);

	/**
	 * サーバ削除イベントリスナ
	 * @param serverName サーバ設定名
	 * @param dateMsg 日付時刻
	 */
	public void removeServer(String serverName, String dateMsg);

	/**
	 * チャンネル作成イベントリスナ
	 * @param serverName サーバ設定名
	 * @param channel チャンネル
	 * @param dateMsg 日付時刻
	 */
	public void createChannel(String serverName, String channel, String dateMsg);

	/**
	 * チャンネル削除イベントリスナ
	 * @param serverName サーバ設定名
	 * @param channel チャンネル
	 * @param dateMsg 日付時刻
	 */
	public void removeChannel(String serverName, String channel, String dateMsg);

	/**
	 * nick変更イベントリスナ
	 * @param serverName サーバ設定名
	 * @param oldNick 旧nick
	 * @param newNick 新nick
	 * @param dateMsg 日付時刻
	 */
	public void changeNick(String serverName, String oldNick, String newNick, String dateMsg);

	/**
	 * トピック変更イベントリスナ
	 * @param serverName サーバ設定名
	 * @param channel チャンネル
	 * @param topic トピック
	 * @param dateMsg 日付時刻
	 */
	public void changeTopic(String serverName, String channel, String topic, String dateMsg);

	/**
	 * サーバ接続完了イベントリスナ
	 * @param serverName サーバ設定名
	 */
	public void receiveConnect(String serverName);

	/**
	 * サーバ切断完了イベントリスナ
	 * @param serverName サーバ設定名
	 */
	public void receiveDisconnect(String serverName);
}
