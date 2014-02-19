package org.ironrhino.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class StringUtils {

	private abstract static class WordTokenizer {
		protected static final char UNDERSCORE = '_';

		/**
		 * Parse sentence。
		 */
		public String parse(String str) {
			if (org.apache.commons.lang3.StringUtils.isEmpty(str)) {
				return str;
			}

			int length = str.length();
			StringBuffer buffer = new StringBuffer(length);

			for (int index = 0; index < length; index++) {
				char ch = str.charAt(index);

				// 忽略空白。
				if (Character.isWhitespace(ch)) {
					continue;
				}

				// 大写字母开始：UpperCaseWord或是TitleCaseWord。
				if (Character.isUpperCase(ch)) {
					int wordIndex = index + 1;

					while (wordIndex < length) {
						char wordChar = str.charAt(wordIndex);

						if (Character.isUpperCase(wordChar)) {
							wordIndex++;
						} else if (Character.isLowerCase(wordChar)) {
							wordIndex--;
							break;
						} else {
							break;
						}
					}

					// 1. wordIndex == length，说明最后一个字母为大写，以upperCaseWord处理之。
					// 2. wordIndex == index，说明index处为一个titleCaseWord。
					// 3. wordIndex > index，说明index到wordIndex -
					// 1处全部是大写，以upperCaseWord处理。
					if ((wordIndex == length) || (wordIndex > index)) {
						index = parseUpperCaseWord(buffer, str, index,
								wordIndex);
					} else {
						index = parseTitleCaseWord(buffer, str, index);
					}

					continue;
				}

				// 小写字母开始：LowerCaseWord。
				if (Character.isLowerCase(ch)) {
					index = parseLowerCaseWord(buffer, str, index);
					continue;
				}

				// 数字开始：DigitWord。
				if (Character.isDigit(ch)) {
					index = parseDigitWord(buffer, str, index);
					continue;
				}

				// 非字母数字开始：Delimiter。
				inDelimiter(buffer, ch);
			}

			return buffer.toString();
		}

		private int parseUpperCaseWord(StringBuffer buffer, String str,
				int index, int length) {
			char ch = str.charAt(index++);

			// 首字母，必然存在且为大写。
			if (buffer.length() == 0) {
				startSentence(buffer, ch);
			} else {
				startWord(buffer, ch);
			}

			// 后续字母，必为小写。
			for (; index < length; index++) {
				ch = str.charAt(index);
				inWord(buffer, ch);
			}

			return index - 1;
		}

		private int parseLowerCaseWord(StringBuffer buffer, String str,
				int index) {
			char ch = str.charAt(index++);

			// 首字母，必然存在且为小写。
			if (buffer.length() == 0) {
				startSentence(buffer, ch);
			} else {
				startWord(buffer, ch);
			}

			// 后续字母，必为小写。
			int length = str.length();

			for (; index < length; index++) {
				ch = str.charAt(index);

				if (Character.isLowerCase(ch)) {
					inWord(buffer, ch);
				} else {
					break;
				}
			}

			return index - 1;
		}

		private int parseTitleCaseWord(StringBuffer buffer, String str,
				int index) {
			char ch = str.charAt(index++);

			// 首字母，必然存在且为大写。
			if (buffer.length() == 0) {
				startSentence(buffer, ch);
			} else {
				startWord(buffer, ch);
			}

			// 后续字母，必为小写。
			int length = str.length();

			for (; index < length; index++) {
				ch = str.charAt(index);

				if (Character.isLowerCase(ch)) {
					inWord(buffer, ch);
				} else {
					break;
				}
			}

			return index - 1;
		}

		private int parseDigitWord(StringBuffer buffer, String str, int index) {
			char ch = str.charAt(index++);

			// 首字符，必然存在且为数字。
			if (buffer.length() == 0) {
				startDigitSentence(buffer, ch);
			} else {
				startDigitWord(buffer, ch);
			}

			// 后续字符，必为数字。
			int length = str.length();

			for (; index < length; index++) {
				ch = str.charAt(index);

				if (Character.isDigit(ch)) {
					inDigitWord(buffer, ch);
				} else {
					break;
				}
			}

			return index - 1;
		}

		protected boolean isDelimiter(char ch) {
			return !Character.isUpperCase(ch) && !Character.isLowerCase(ch)
					&& !Character.isDigit(ch);
		}

		protected abstract void startSentence(StringBuffer buffer, char ch);

		protected abstract void startWord(StringBuffer buffer, char ch);

		protected abstract void inWord(StringBuffer buffer, char ch);

		protected abstract void startDigitSentence(StringBuffer buffer, char ch);

		protected abstract void startDigitWord(StringBuffer buffer, char ch);

		protected abstract void inDigitWord(StringBuffer buffer, char ch);

		protected abstract void inDelimiter(StringBuffer buffer, char ch);
	}

	private static final WordTokenizer CAMEL_CASE_TOKENIZER = new WordTokenizer() {

		@Override
		protected void startSentence(StringBuffer buffer, char ch) {
			buffer.append(Character.toLowerCase(ch));
		}

		@Override
		protected void startWord(StringBuffer buffer, char ch) {
			if (!isDelimiter(buffer.charAt(buffer.length() - 1))) {
				buffer.append(Character.toUpperCase(ch));
			} else {
				buffer.append(Character.toLowerCase(ch));
			}
		}

		@Override
		protected void inWord(StringBuffer buffer, char ch) {
			buffer.append(Character.toLowerCase(ch));
		}

		@Override
		protected void startDigitSentence(StringBuffer buffer, char ch) {
			buffer.append(ch);
		}

		@Override
		protected void startDigitWord(StringBuffer buffer, char ch) {
			buffer.append(ch);
		}

		@Override
		protected void inDigitWord(StringBuffer buffer, char ch) {
			buffer.append(ch);
		}

		@Override
		protected void inDelimiter(StringBuffer buffer, char ch) {
			if (ch != UNDERSCORE) {
				buffer.append(ch);
			}
		}
	};

	private static final WordTokenizer UPPER_CASE_WITH_UNDERSCORES_TOKENIZER = new WordTokenizer() {

		@Override
		protected void startSentence(StringBuffer buffer, char ch) {
			buffer.append(Character.toUpperCase(ch));
		}

		@Override
		protected void startWord(StringBuffer buffer, char ch) {
			if (!isDelimiter(buffer.charAt(buffer.length() - 1))) {
				buffer.append(UNDERSCORE);
			}

			buffer.append(Character.toUpperCase(ch));
		}

		@Override
		protected void inWord(StringBuffer buffer, char ch) {
			buffer.append(Character.toUpperCase(ch));
		}

		@Override
		protected void startDigitSentence(StringBuffer buffer, char ch) {
			buffer.append(ch);
		}

		@Override
		protected void startDigitWord(StringBuffer buffer, char ch) {
			if (!isDelimiter(buffer.charAt(buffer.length() - 1))) {
				buffer.append(UNDERSCORE);
			}

			buffer.append(ch);
		}

		@Override
		protected void inDigitWord(StringBuffer buffer, char ch) {
			buffer.append(ch);
		}

		@Override
		protected void inDelimiter(StringBuffer buffer, char ch) {
			buffer.append(ch);
		}
	};

	public static String toCamelCase(String str) {
		if (str == null)
			return null;
		return CAMEL_CASE_TOKENIZER.parse(str);
	}

	public static String toUpperCaseWithUnderscores(String str) {
		if (str == null)
			return null;
		return UPPER_CASE_WITH_UNDERSCORES_TOKENIZER.parse(str);
	}

	public static String toLowerCaseWithUnderscores(String str) {
		if (str == null)
			return null;
		return toUpperCaseWithUnderscores(str).toLowerCase();
	}

	public static boolean matchesWildcard(String text, String pattern) {
		text += '\0';
		pattern += '\0';

		int N = pattern.length();

		boolean[] states = new boolean[N + 1];
		boolean[] old = new boolean[N + 1];
		old[0] = true;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			states = new boolean[N + 1];
			for (int j = 0; j < N; j++) {
				char p = pattern.charAt(j);

				if (old[j] && (p == '*'))
					old[j + 1] = true;

				if (old[j] && (p == c))
					states[j + 1] = true;
				if (old[j] && (p == '?'))
					states[j + 1] = true;
				if (old[j] && (p == '*'))
					states[j] = true;
				if (old[j] && (p == '*'))
					states[j + 1] = true;
			}
			old = states;
		}
		return states[N];
	}

	public static String trimTail(String input, String tail) {
		if (input == null || tail == null || !input.endsWith(tail))
			return input;
		return input.substring(0, input.length() - tail.length());
	}

	public static String trimTailSlash(String input) {
		return trimTail(input, "/");
	}

	public static String compressRepeat(String input, String repeat) {
		if (input == null || repeat == null)
			return input;
		String s = repeat + repeat;
		while (input.contains(s))
			input = input.replace(s, repeat);
		return input;
	}

	public static String compressRepeatSlash(String input) {
		return compressRepeat(input, "/");
	}

	public static boolean matchesAutocomplete(String text, String pattern) {
		text = text.toLowerCase();
		pattern = pattern.toLowerCase();
		if (text.startsWith(pattern))
			return true;
		char[] chars = pattern.toCharArray();
		if (chars.length <= text.length()) {
			boolean b1 = true;
			for (int i = 0; i < chars.length; i++) {
				boolean b2 = false;
				for (String s : _pinyin(text.charAt(i)))
					if (s.indexOf(chars[i]) == 0) {
						b2 = true;
						break;
					}
				if (!b2) {
					b1 = false;
					break;
				}
			}
			if (b1)
				return true;
		}
		// matches string array split by space
		String[] array = pattern.split("\\s");
		if (array.length > 1) {
			boolean b1 = true;
			for (int i = 0; i < array.length; i++) {
				boolean b2 = false;
				for (String s : _pinyin(text.charAt(i)))
					if (s.indexOf(array[i]) == 0) {
						b2 = true;
						break;
					}
				if (!b2) {
					b1 = false;
					break;
				}
			}
			if (b1)
				return true;
		}
		// matches string increasingly,may be not restricted
		int matchedChars = 0;
		int matchedPosition = 0;
		while (pattern.length() - 1 > matchedPosition) {
			boolean b = false;
			for (String s : _pinyin(text.charAt(matchedChars)))
				if (pattern.substring(matchedPosition).indexOf(s) == 0) {
					b = true;
					matchedPosition += s.length();
					break;
				}
			if (!b)
				return false;
			matchedChars++;
		}
		return true;
	}

	private static String[] _pinyin(char c) {
		HanyuPinyinOutputFormat f = new HanyuPinyinOutputFormat();
		f.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		f.setVCharType(HanyuPinyinVCharType.WITH_V);
		f.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		String[] array = null;
		try {
			array = PinyinHelper.toHanyuPinyinStringArray(c, f);
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			e.printStackTrace();
		}
		if (array == null)// not chinese
			array = new String[] { String.valueOf(c) };
		return array;
	}

	public static String pinyin(String hanyu) {
		StringBuilder sb = new StringBuilder();
		char[] chars = hanyu.toCharArray();
		for (char c : chars)
			sb.append(_pinyin(c)[0]);
		return sb.toString();
	}

	public static String pinyinAbbr(String hanyu) {
		StringBuilder sb = new StringBuilder();
		char[] chars = hanyu.toCharArray();
		for (char c : chars)
			sb.append(_pinyin(c)[0].charAt(0));
		return sb.toString();
	}

	public static String decodeUrl(String url) {
		try {
			if (isUtf8Url(url)) {
				return URLDecoder.decode(url, "UTF-8");
			} else {
				return URLDecoder.decode(url, "GBK");
			}
		} catch (UnsupportedEncodingException e) {
			return url;
		}
	}

	private static boolean utf8codeCheck(String urlCode) {
		StringBuilder sign = new StringBuilder();
		if (urlCode.startsWith("%e"))
			for (int p = 0; p != -1;) {
				p = urlCode.indexOf("%", p);
				if (p != -1)
					p++;
				sign.append(p);
			}
		return sign.toString().equals("147-1");
	}

	private static boolean isUtf8Url(String urlCode) {
		urlCode = urlCode.toLowerCase();
		int p = urlCode.indexOf("%");
		if (p != -1 && urlCode.length() - p > 9) {
			urlCode = urlCode.substring(p, p + 9);
		}
		return utf8codeCheck(urlCode);
	}

	public static String trimLocale(String s) {
		if (s == null)
			return null;
		if (s.indexOf('_') < 0)
			return s;
		for (Locale locale : Locale.getAvailableLocales()) {
			String suffix = "_" + locale.getLanguage();
			if (s.endsWith(suffix))
				return s.substring(0, s.length() - suffix.length());
			suffix += "_" + locale.getCountry();
			if (s.endsWith(suffix))
				return s.substring(0, s.length() - suffix.length());
			suffix += "_" + locale.getVariant();
			if (s.endsWith(suffix))
				return s.substring(0, s.length() - suffix.length());
		}
		return s;
	}

	public static String compressRepeatSpaces(String input) {
		if (input == null)
			return null;
		input = input.trim();
		return input.replaceAll("\\s+", " ");
	}

}
