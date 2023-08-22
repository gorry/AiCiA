/**
 *
 * AiCiA - Android IRC Client
 *
 * Copyright (C)2010 GORRY.
 *
 */

package net.gorry.aicia;

/**
 *
 * IRCサービス インターフェース
 *
 * @author GORRY
 *
 */
interface IIRCService {
	/**
	 * IRCサービスのシャットダウン
	 */
	void shutdown();

	/**
	 * システム設定のリロード
	 */
	void reloadSystemConfig();

	/**
	 * Spanチャンネルログの取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return Spanチャンネルログ
	 */
	CharSequence getSpanChannelLog(String serverName, String channel);

	/**
	 * チャンネルログのクリア
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	void clearChannelLog(String serverName, String channel);

	/**
	 * 全チャンネルログのクリア
	 */
	void clearAllChannelLog();

	/**
	 * チャンネル設定のリロード
	 */
	void reloadChannelConfig(String serverName, String channel);

	/**
	 * nickの取得
	 * @param serverName サーバ設定名
	 * @return nick
	 */
	String getNick(String serverName);

	/**
	 * トピックの取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return トピック
	 */
	String getTopic(String serverName, String channel);

	/**
	 * コマンドラインの送信
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message コマンドライン
	 */
	void sendCommandLine(String serverName, String channel, String message);

	/**
	 * コマンドラインの送信（エコーしない）
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message コマンドライン
	 */
	void sendQuietCommandLine(String serverName, String channel, String message);

	/**
	 * メッセージの送信
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	void sendMessageToChannel(String serverName, String channel, String message);

	/**
	 * メッセージの送信（エコーしない）
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	void sendQuietMessageToChannel(String serverName, String channel, String message);

	/**
	 * Noticeメッセージの送信
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	void sendNoticeToChannel(String serverName, String channel, String message);

	/**
	 * メッセージのエコー
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @param message メッセージ
	 */
	void echoMessageToChannel(String serverName, String channel, String message);

	/**
	 * 指定サーバへ接続を発行
	 * @param serverName サーバ設定名
	 */
	void connectServer(String serverName);

	/**
	 * 指定サーバへ切断を発行
	 * @param serverName サーバ設定名
	 */
	void disconnectServer(String serverName);

	/**
	 * 指定サーバへクローズを発行
	 * @param serverName サーバ設定名
	 */
	void closeServer(String serverName);

	/**
	 * 自動接続設定されているサーバへ接続を発行
	 */
	int connectAuto();

	/**
	 * 全サーバへ切断を発行
	 */
	void disconnectAll();

	/**
	 * 全サーバへクローズを発行
	 */
	void closeAll();

	/**
	 * サーバリストの再読み込み
	 */
	void reloadList();

	/**
	 * 新しいサーバ設定を追加
	 */
	void addNewServer();

	/**
	 * 指定サーバの設定を再読み込み
	 * @param serverId サーバID
	 */
	void reloadServerConfig(int serverId);

	/**
	 * 指定サーバを削除
	 * @param serverId サーバID
	 */
	void removeServer(int serverId);

	/**
	 * カレントサーバを設定
	 * @param serverName サーバ設定名
	 */
	void setCurrentServerName(String serverName);

	/**
	 * カレントサーバを取得
	 * @return サーバ設定名
	 */
	String getCurrentServerName();

	/**
	 * カレントチャンネルを設定
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	void setCurrentChannel(String serverName, String channel);

	/**
	 * カレントチャンネルを取得
	 * @param serverName サーバ設定名
	 * @return チャンネル名
	 */
	String getCurrentChannel(String serverName);

	/**
	 * サーバ名リストを取得
	 * @return サーバ名リスト
	 */
	String[] getServerList();

	/**
	 * チャンネル名リストを取得
	 * @param serverName サーバ設定名
	 * @return チャンネル名リスト
	 */
	String[] getChannelList(String serverName);

	/**
	 * チャンネルログ更新リストを取得
	 * @param serverName サーバ設定名
	 * @return チャンネルログ更新リスト
	 */
	boolean[] getChannelUpdatedList(String serverName);

	/**
	 * チャンネルログ更新フラグを取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return チャンネルログが更新されていたらtrue
	 */
	boolean getChannelUpdated(String serverName, String channel);

	/**
	 * チャンネルログ更新フラグをクリア
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	void clearChannelUpdated(String serverName, String channel);

	/**
	 * 全チャンネルログ更新フラグをクリア
	 */
	void clearChannelUpdatedAll();

	/**
	 * チャンネルログ更新リストを取得
	 * @param serverName サーバ設定名
	 * @return チャンネルログ更新リスト
	 */
	boolean[] getChannelAlertedList(String serverName);

	/**
	 * チャンネルログ通知フラグを取得
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 * @return チャンネルログが更新されていたらtrue
	 */
	boolean getChannelAlerted(String serverName, String channel);

	/**
	 * チャンネルログ通知フラグをクリア
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	void clearChannelAlerted(String serverName, String channel);

	/**
	 * 全チャンネルログ通知フラグをクリア
	 */
	void clearChannelAlertedAll();

	/**
	 * ユーザーリストを得る
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	String[] getUserList(String serverName, String channel);

	/**
	 * 次のチャンネルに移動する
	 * @param dir 方向（1=next, -1=prev）
	 * @param mode 「未更新チャンネルをスキップする」とき1
	 */
	boolean changeNextChannel(int dir, int mode);

	/**
	 * チャンネルに入る
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	void joinChannel(String serverName, String channel);

	/**
	 * 複数チャンネルに順次入る
	 * @param serverName サーバ設定名
	 * @param channels チャンネル名（スペース区切り）
	 */
	void joinChannels(String serverName, String channels);

	/**
	 * チャンネルから出る
	 * @param serverName サーバ設定名
	 * @param channel チャンネル名
	 */
	void partChannel(String serverName, String channel);

	/**
	 * 入力テキストの保存
	 * @param input 入力内容
	 */
	void saveInputBox(String input, int selStart, int selEnd);

	/**
	 * 入力テキストの読み出し
	 * @return 入力内容
	 */
	String loadInputBox();

	/**
	 * 入力テキスト選択開始位置の読み出し
	 * @return 選択開始位置
	 */
	int loadInputBoxSelStart();

	/**
	 * 入力テキスト選択終了位置の読み出し
	 * @return 選択終了位置
	 */
	int loadInputBoxSelEnd();

	/**
	 * Pingを受けたときに返すPong
	 */
	void receivePong();

	/**
	 * サーバ設定がTIG向けならtrue
	 */
	boolean isTIGMode(String serverName);

	/**
	 * "Low Memory"通知の消去
	 */
	void clearLowMemoryNotification();

	/**
	 * バージョンの取得
	 */
	String getVersionString();

	/**
	 * サブログに出力するかどうかのフラグを取得
	 */
	boolean isPutOnSublog(String serverName, String channel);

	/**
	 * サブログに全て出力するかどうかのフラグを取得
	 */
	boolean isPutOnSublogAll(String serverName, String channel);

	/**
	 * 入力履歴を取得
	 */
	String getInputHistory(int pos);

	/**
	 * 入力履歴に登録
	 */
	void pushInputHistory(String message);

	/**
	 * TIGモードで接続を発行しているサーバがあればtrue
	 */
	boolean haveTIGModeInConnectedServers();

	/**
	 * 通知のクリア
	 */
	void clearNotify();

}
