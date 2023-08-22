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
 *
 * IRCサーバイベント アダプタ
 *
 * @author GORRY
 *
 */
public class IRCServerEventAdapter implements IRCServerEventListener {
	public void receiveMessageToChannel(final String serverName, final String channel, final String nick, final String dateMsg, final SpannableStringBuilder ssb, final boolean forceSublog, final boolean forbidSublog, final boolean alert) {
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
