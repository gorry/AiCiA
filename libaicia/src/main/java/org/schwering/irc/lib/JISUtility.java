/**
 * 
 */
package org.schwering.irc.lib;

import java.io.UnsupportedEncodingException;

/**
 * JIS入出力用ユーティリティ
 * 
 * @author GORRY
 *
 */
public class JISUtility {

	/**
	 * JIS文字列をUNICODE文字列に変換する
	 * @param jisLine 入力JIS文字列
	 * @return 出力UNICODE文字列
	 */
	public static String convJis2Unicode(final String jisLine) {
		// JIS->SJIS convert
		final StringBuffer sb = new StringBuffer();
		final char[] l = jisLine.toCharArray();
		int mode = 0;
		for (int i=0; i<l.length; i++) {
			final char c = l[i];
			if (c == 0x1b) {
				i++;
				if (i >= l.length) break;
				switch (l[i]) {
				case '(':
					i++;
					if (i >= l.length) break;
					switch (l[i]) {
					case 'B':  // ESC ( B : ASCII
						mode = 0;
						break;
					case 'I':  // ESC ( I : JIS X 0201-1976 Kana
						mode = 1;
						break;
					case 'J':  // ESC ( J : JIS X 0201-1976 Latin
						mode = 2;
						break;
					}
					break;
				case '$':
					i++;
					if (i >= l.length) break;
					switch (l[i]) {
					case '@':  // ESC $ @ : JIS X 0208-1978
						mode = 4;
						break;
					case 'B':  // ESC $ B : JIS X 0208-1983
						mode = 5;
						break;
					}
					break;
				}
				continue;
			}
			switch (mode) {
			case 0:  // ASCII
				sb.append(c);
				break;
			case 1:  // JIS X 0201-1976 Kana
				sb.append(c);
				break;
			case 2:  // JIS X 0201-1976 Latin
				switch (c) {
				case 0x0e:  // SO
					mode = 3;
					break;
				case 0x0f:  // SI
					mode = 2;
					break;
				default:
					sb.append(c);
					break;
				}
				break;
			case 3:  // JIS X 0201-1976 Latin SO
				switch (c) {
				case 0x0e:  // SO
					mode = 3;
					break;
				case 0x0f:  // SI
					mode = 2;
					break;
				default:
					sb.append(c|0x80);
					break;
				}
				break;
			case 4:  // JIS X 0201-1978
			case 5:  // JIS X 0201-1983
				if (c >= 0x80) {
					if ((0xa1 <= c) && (c<=0xdf)) {
						sb.append(c);
					}
				} else {
					i++;
					if (i >= l.length) break;
					int c1 = c;
					int c2 = l[i];
					c2 += ((c1 & 0x01) != 0) ? 0x1F : 0x7D;
					if (c2 >= 0x7f) c2++;
					c1 = ((c1 - 0x21) >> 1) + 0x81;
					if (c1 > 0x9f) c1 += 0x40;
					sb.append((char)c1);
					sb.append((char)c2);
				}
				break;
			}
		}

		// SJIS->UNICODE convert
		String line = null;
		try {
			line = new String(sb.toString().getBytes("ISO-8859-1"), "Shift_JIS");
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return line;
	}

	/**
	 * UNICODE文字列をJIS文字列に変換する
	 * @param unicodeLine 入力UNICODE文字列
	 * @return 出力JIS文字列
	 */
	public static String convUnicode2Jis(final String unicodeLine) {
		// UNICODE->SJIS convert
		byte [] l = null;
		try {
			l = unicodeLine.getBytes("Shift_JIS");
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// SJIS->JIS convert
		final StringBuffer sb = new StringBuffer();
		int mode = 0;
		for (int i=0; i<l.length; i++) {
			int c1 = (l[i] & 0xff);
			if (c1 >= 0x80) {
				if ((0xa1 <= c1) && (c1 <= 0xdf)) {
					if (mode != 2) {
						mode = 2;
						sb.append((char)0x1b);
						sb.append('(');
						sb.append('J');
					}
					sb.append((char)c1);
				} else {
					if (mode != 1) {
						mode = 1;
						sb.append((char)0x1b);
						sb.append('$');
						sb.append('B');
					}
					i++;
					if (i >= l.length) break;
					int c2 = (l[i] & 0xff);
					c1 -= (c1 <= 0x9f) ? 0x71 : 0xb1;
					c1 = (c1 << 1) + 1;
					if (c2 > 0x7f) c2--;
					if (c2 >= 0x9e) {
						c2 -= 0x7d;
						c1++;
					} else {
						c2 -= 0x1f;
					}
					sb.append((char)c1);
					sb.append((char)c2);
				}
				continue;
			}
			if (mode != 0) {
				mode = 0;
				sb.append((char)0x1b);
				sb.append('(');
				sb.append('B');
			}
			sb.append((char)c1);
		}
		if (mode != 0) {
			sb.append((char)0x1b);
			sb.append('(');
			sb.append('B');
		}

		String line = null;
		try {
			line = new String(sb.toString().getBytes("ISO-8859-1"), "ISO-8859-1");
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return line;
	}

	// ------------------------------

	private static final char[] HANKAKU_KATAKANA = { '｡', '｢', '｣', '､', '･',
		'ｦ', 'ｧ', 'ｨ', 'ｩ', 'ｪ', 'ｫ', 'ｬ', 'ｭ', 'ｮ', 'ｯ', 'ｰ', 'ｱ', 'ｲ',
		'ｳ', 'ｴ', 'ｵ', 'ｶ', 'ｷ', 'ｸ', 'ｹ', 'ｺ', 'ｻ', 'ｼ', 'ｽ', 'ｾ', 'ｿ',
		'ﾀ', 'ﾁ', 'ﾂ', 'ﾃ', 'ﾄ', 'ﾅ', 'ﾆ', 'ﾇ', 'ﾈ', 'ﾉ', 'ﾊ', 'ﾋ', 'ﾌ',
		'ﾍ', 'ﾎ', 'ﾏ', 'ﾐ', 'ﾑ', 'ﾒ', 'ﾓ', 'ﾔ', 'ﾕ', 'ﾖ', 'ﾗ', 'ﾘ', 'ﾙ',
		'ﾚ', 'ﾛ', 'ﾜ', 'ﾝ', 'ﾞ', 'ﾟ' };

	private static final char[] ZENKAKU_KATAKANA = { '。', '「', '」', '、', '・',
		'ヲ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ッ', 'ー', 'ア', 'イ',
		'ウ', 'エ', 'オ', 'カ', 'キ', 'ク', 'ケ', 'コ', 'サ', 'シ', 'ス', 'セ', 'ソ',
		'タ', 'チ', 'ツ', 'テ', 'ト', 'ナ', 'ニ', 'ヌ', 'ネ', 'ノ', 'ハ', 'ヒ', 'フ',
		'ヘ', 'ホ', 'マ', 'ミ', 'ム', 'メ', 'モ', 'ヤ', 'ユ', 'ヨ', 'ラ', 'リ', 'ル',
		'レ', 'ロ', 'ワ', 'ン', '゛', '゜' };

	private static final char HANKAKU_KATAKANA_FIRST_CHAR = HANKAKU_KATAKANA[0];

	private static final char HANKAKU_KATAKANA_LAST_CHAR = HANKAKU_KATAKANA[HANKAKU_KATAKANA.length - 1];

	/**
	 * 半角カタカナから全角カタカナへ変換します。
	 * @param c 変換前の文字
	 * @return 変換後の文字
	 */
	public static char hankakuKatakanaToZenkakuKatakana(final char c) {
		if ((c >= HANKAKU_KATAKANA_FIRST_CHAR) && (c <= HANKAKU_KATAKANA_LAST_CHAR)) {
			return ZENKAKU_KATAKANA[c - HANKAKU_KATAKANA_FIRST_CHAR];
		}
		return c;
	}
	/**
	 * 2文字目が濁点・半濁点で、1文字目に加えることができる場合は、合成した文字を返します。
	 * 合成ができないときは、c1を返します。
	 * @param c1 変換前の1文字目
	 * @param c2 変換前の2文字目
	 * @return 変換後の文字
	 */
	public static char mergeChar(final char c1, final char c2) {
		if (c2 == 'ﾞ') {
			if ("ｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾊﾋﾌﾍﾎ".indexOf(c1) >= 0) {
				switch (c1) {
				case 'ｶ': return 'ガ';
				case 'ｷ': return 'ギ';
				case 'ｸ': return 'グ';
				case 'ｹ': return 'ゲ';
				case 'ｺ': return 'ゴ';
				case 'ｻ': return 'ザ';
				case 'ｼ': return 'ジ';
				case 'ｽ': return 'ズ';
				case 'ｾ': return 'ゼ';
				case 'ｿ': return 'ゾ';
				case 'ﾀ': return 'ダ';
				case 'ﾁ': return 'ヂ';
				case 'ﾂ': return 'ヅ';
				case 'ﾃ': return 'デ';
				case 'ﾄ': return 'ド';
				case 'ﾊ': return 'バ';
				case 'ﾋ': return 'ビ';
				case 'ﾌ': return 'ブ';
				case 'ﾍ': return 'ベ';
				case 'ﾎ': return 'ボ';
				}
			}
		} else if (c2 == 'ﾟ') {
			if ("ﾊﾋﾌﾍﾎ".indexOf(c1) >= 0) {
				switch (c1) {
				case 'ﾊ': return 'パ';
				case 'ﾋ': return 'ピ';
				case 'ﾌ': return 'プ';
				case 'ﾍ': return 'ペ';
				case 'ﾎ': return 'ポ';
				}
			}
		}
		return c1;
	}

	/**
	 * 文字列中の半角カタカナを全角カタカナに変換します。
	 * @param s 変換前文字列
	 * @return 変換後文字列
	 */
	public static String hankakuKatakanaToZenkakuKatakana(final String s) {
		if (s.length() == 0) {
			return s;
		} else if (s.length() == 1) {
			return hankakuKatakanaToZenkakuKatakana(s.charAt(0)) + "";
		} else {
			final StringBuffer sb = new StringBuffer(s);
			int i = 0;
			for (i = 0; i < sb.length() - 1; i++) {
				final char originalChar1 = sb.charAt(i);
				final char originalChar2 = sb.charAt(i + 1);
				final char margedChar = mergeChar(originalChar1, originalChar2);
				if (margedChar != originalChar1) {
					sb.setCharAt(i, margedChar);
					sb.deleteCharAt(i + 1);
				} else {
					final char convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1);
					if (convertedChar != originalChar1) {
						sb.setCharAt(i, convertedChar);
					}
				}
			}
			if (i < sb.length()) {
				final char originalChar1 = sb.charAt(i);
				final char convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1);
				if (convertedChar != originalChar1) {
					sb.setCharAt(i, convertedChar);
				}
			}
			return sb.toString();
		}

	}


}
