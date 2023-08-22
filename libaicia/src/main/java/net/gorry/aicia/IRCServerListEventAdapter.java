/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

import android.text.SpannableStringBuilder;

/**
 * IRCサーバリスト イベントアダプタ
 *
 * @author GORRY
 *
 */
public class IRCServerListEventAdapter implements IRCServerListEventListener{
	public void receiveMessageToChannel(final String serverName, final String channel, final String nick, final String dateMsg, final SpannableStringBuilder ssb, final SpannableStringBuilder ssbOther, final boolean toSublog, final boolean alert) {
		// adapter
	}
	public void createServer(final String serverName, final String dateMsg) {
		// adapter
	}
	public void removeServer(final String serverName, final String dateMsg) {
		// adapter
	}
	public void createChannel(final String serverName, final String channel, final String dateMsg) {
		// adapter
	}
	public void removeChannel(final String serverName, final String channel, final String dateMsg) {
		// adapter
	}
	public void changeNick(final String serverName, final String oldNick, final String newNick, final String dateMsg) {
		// adapter
	}
	public void changeTopic(final String serverName, final String channel, final String topic, final String dateMsg) {
		// adapter
	}
	public void receiveConnect(final String serverName) {
		// adapter
	}
	public void receiveDisconnect(final String serverName) {
		// adapter
	}
}
