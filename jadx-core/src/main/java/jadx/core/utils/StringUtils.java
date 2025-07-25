package jadx.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.args.IntegerFormat;
import jadx.core.deobf.NameMapper;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class StringUtils {
	private static final StringUtils DEFAULT_INSTANCE = new StringUtils(new JadxArgs());
	private static final String WHITES = " \t\r\n\f\b";
	private static final String WORD_SEPARATORS = WHITES + "(\")<,>{}=+-*/|[]\\:;'.`~!#^&";

	public static StringUtils getInstance() {
		return DEFAULT_INSTANCE;
	}

	private final boolean escapeUnicode;
	private final IntegerFormat integerFormat;

	public StringUtils(JadxArgs args) {
		this.escapeUnicode = args.isEscapeUnicode();
		this.integerFormat = args.getIntegerFormat();
	}

	public IntegerFormat getIntegerFormat() {
		return integerFormat;
	}

	public static void visitCodePoints(String str, IntConsumer visitor) {
		int len = str.length();
		int offset = 0;
		while (offset < len) {
			int codePoint = str.codePointAt(offset);
			visitor.accept(codePoint);
			offset += Character.charCount(codePoint);
		}
	}

	public String unescapeString(String str) {
		int len = str.length();
		if (len == 0) {
			return "\"\"";
		}
		StringBuilder res = new StringBuilder();
		res.append('"');
		visitCodePoints(str, codePoint -> processCodePoint(codePoint, res));
		res.append('"');
		return res.toString();
	}

	private void processCodePoint(int codePoint, StringBuilder res) {
		String str = getSpecialStringForCodePoint(codePoint);
		if (str != null) {
			res.append(str);
			return;
		}
		if (isEscapeNeededForCodePoint(codePoint)) {
			res.append("\\u").append(String.format("%04x", codePoint));
		} else {
			res.appendCodePoint(codePoint);
		}
	}

	private boolean isEscapeNeededForCodePoint(int codePoint) {
		if (codePoint < 32) {
			return true;
		}
		if (codePoint < 127) {
			return false;
		}
		if (escapeUnicode) {
			return true;
		}
		return !NameMapper.isPrintableCodePoint(codePoint);
	}

	/**
	 * Represent single char the best way possible
	 */
	public String unescapeChar(char c, boolean explicitCast) {
		if (c == '\'') {
			return "'\\''";
		}
		String str = getSpecialStringForCodePoint(c);
		if (str != null) {
			return '\'' + str + '\'';
		}
		if (c >= 127 && escapeUnicode) {
			return String.format("'\\u%04x'", (int) c);
		}
		if (NameMapper.isPrintableChar(c)) {
			return "'" + c + '\'';
		}
		String intStr = Integer.toString(c);
		return explicitCast ? "(char) " + intStr : intStr;
	}

	public String unescapeChar(char ch) {
		return unescapeChar(ch, false);
	}

	@Nullable
	private String getSpecialStringForCodePoint(int c) {
		switch (c) {
			case '\n':
				return "\\n";
			case '\r':
				return "\\r";
			case '\t':
				return "\\t";
			case '\b':
				return "\\b";
			case '\f':
				return "\\f";
			case '\'':
				return "'";
			case '"':
				return "\\\"";
			case '\\':
				return "\\\\";

			default:
				return null;
		}
	}

	public static String escape(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case '.':
				case '/':
				case ';':
				case '$':
				case ' ':
				case ',':
				case '<':
					sb.append('_');
					break;

				case '[':
					sb.append('A');
					break;

				case ']':
				case '>':
				case '?':
				case '*':
					break;

				default:
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	public static String escapeXML(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			String replace = escapeXmlChar(c);
			if (replace != null) {
				sb.append(replace);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String escapeResValue(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			commonEscapeAndAppend(sb, c);
		}
		return sb.toString();
	}

	public static String escapeResStrValue(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\'':
					sb.append("\\'");
					break;
				default:
					commonEscapeAndAppend(sb, c);
					break;
			}
		}
		return sb.toString();
	}

	private static String escapeXmlChar(char c) {
		if (c <= 0x1F) {
			return "\\" + (int) c;
		}
		switch (c) {
			case '&':
				return "&amp;";
			case '<':
				return "&lt;";
			case '>':
				return "&gt;";
			case '"':
				return "&quot;";
			case '\'':
				return "&apos;";
			case '\\':
				return "\\\\";
			default:
				return null;
		}
	}

	private static String escapeWhiteSpaceChar(char c) {
		switch (c) {
			case '\n':
				return "\\n";
			case '\r':
				return "\\r";
			case '\t':
				return "\\t";
			case '\b':
				return "\\b";
			case '\f':
				return "\\f";
			default:
				return null;
		}
	}

	private static void commonEscapeAndAppend(StringBuilder sb, char c) {
		String replace = escapeWhiteSpaceChar(c);
		if (replace == null) {
			replace = escapeXmlChar(c);
		}
		if (replace != null) {
			sb.append(replace);
		} else {
			sb.append(c);
		}
	}

	public static boolean notEmpty(String str) {
		return str != null && !str.isEmpty();
	}

	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	public static boolean notBlank(String str) {
		return notEmpty(str) && !str.trim().isEmpty();
	}

	public static int countMatches(String str, String subStr) {
		if (str == null || str.isEmpty() || subStr == null || subStr.isEmpty()) {
			return 0;
		}
		int subStrLen = subStr.length();
		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf(subStr, idx)) != -1) {
			count++;
			idx += subStrLen;
		}
		return count;
	}

	public static boolean containsChar(String str, char ch) {
		return str.indexOf(ch) != -1;
	}

	public static String removeChar(String str, char ch) {
		int pos = str.indexOf(ch);
		if (pos == -1) {
			return str;
		}
		StringBuilder sb = new StringBuilder(str.length());
		int cur = 0;
		int next = pos;
		while (true) {
			sb.append(str, cur, next);
			cur = next + 1;
			next = str.indexOf(ch, cur);
			if (next == -1) {
				sb.append(str, cur, str.length());
				break;
			}
		}
		return sb.toString();
	}

	/**
	 * returns how many lines does it have between start to pos in content.
	 */
	public static int countLinesByPos(String content, int pos, int start) {
		if (start >= pos) {
			return 0;
		}
		int count = 0;
		int tempPos = start;
		do {
			tempPos = content.indexOf("\n", tempPos);
			if (tempPos == -1) {
				break;
			}
			if (tempPos >= pos) {
				break;
			}
			count += 1;
			tempPos += 1;
		} while (tempPos < content.length());
		return count;
	}

	/**
	 * returns lines that contain pos to end if end is not -1.
	 */
	public static String getLine(String content, int pos, int end) {
		if (pos >= content.length()) {
			return "";
		}
		if (end != -1) {
			if (end > content.length()) {
				end = content.length() - 1;
			}
		} else {
			end = pos + 1;
		}
		// get to line head
		int headPos = content.lastIndexOf("\n", pos);
		if (headPos == -1) {
			headPos = 0;
		}
		// get to line end
		int endPos = content.indexOf("\n", end);
		if (endPos == -1) {
			endPos = content.length();
		}
		return content.substring(headPos, endPos);
	}

	public static boolean isWhite(char chr) {
		return WHITES.indexOf(chr) != -1;
	}

	public static boolean isWordSeparator(char chr) {
		return WORD_SEPARATORS.indexOf(chr) != -1;
	}

	public static String removeSuffix(String str, String suffix) {
		if (str.endsWith(suffix)) {
			return str.substring(0, str.length() - suffix.length());
		}
		return str;
	}

	public static @Nullable String getPrefix(String str, String delim) {
		int idx = str.indexOf(delim);
		if (idx != -1) {
			return str.substring(0, idx);
		}
		return null;
	}

	public static String getDateText() {
		return new SimpleDateFormat("HH:mm:ss").format(new Date());
	}

	private String formatNumber(long number, int bytesLen, boolean cast) {
		String numStr;
		if (integerFormat.isHexadecimal()) {
			String hexStr = Long.toHexString(number);
			if (number < 0) {
				// cut leading 'f' for negative numbers to match number type length
				int len = hexStr.length();
				numStr = "0x" + hexStr.substring(len - bytesLen * 2, len);
				// force cast, because unsigned negative numbers are bigger
				// than signed max value allowed by compiler
				cast = true;
			} else {
				numStr = "0x" + hexStr;
			}
		} else {
			numStr = Long.toString(number);
		}
		if (bytesLen == 8 && (number == Long.MIN_VALUE || Math.abs(number) >= Integer.MAX_VALUE)) {
			// force cast for long values bigger than min/max int
			// to resolve compiler error: "integer number too large"
			cast = true;
		}
		if (cast) {
			if (bytesLen == 8) {
				return numStr + 'L';
			}
			return getCastStr(bytesLen) + numStr;
		}
		return numStr;
	}

	private static String getCastStr(int bytesLen) {
		switch (bytesLen) {
			case 1:
				return "(byte) ";
			case 2:
				return "(short) ";
			case 4:
				return "(int) ";
			case 8:
				return "(long) ";
			default:
				throw new JadxRuntimeException("Unexpected number type length: " + bytesLen);
		}
	}

	public String formatByte(long l, boolean cast) {
		return formatNumber(l, 1, cast);
	}

	public String formatShort(long l, boolean cast) {
		if (integerFormat == IntegerFormat.AUTO) {
			switch ((short) l) {
				case Short.MAX_VALUE:
					return "Short.MAX_VALUE";
				case Short.MIN_VALUE:
					return "Short.MIN_VALUE";
			}
		}
		return formatNumber(l, 2, cast);
	}

	public String formatInteger(long l, boolean cast) {
		if (integerFormat == IntegerFormat.AUTO) {
			switch ((int) l) {
				case Integer.MAX_VALUE:
					return "Integer.MAX_VALUE";
				case Integer.MIN_VALUE:
					return "Integer.MIN_VALUE";
			}
		}
		return formatNumber(l, 4, cast);
	}

	public String formatLong(long l, boolean cast) {
		if (integerFormat == IntegerFormat.AUTO) {
			if (l == Long.MAX_VALUE) {
				return "Long.MAX_VALUE";
			}
			if (l == Long.MIN_VALUE) {
				return "Long.MIN_VALUE";
			}
		}
		return formatNumber(l, 8, cast);
	}

	public static String formatDouble(double d) {
		if (Double.isNaN(d)) {
			return "Double.NaN";
		}
		if (d == Double.NEGATIVE_INFINITY) {
			return "Double.NEGATIVE_INFINITY";
		}
		if (d == Double.POSITIVE_INFINITY) {
			return "Double.POSITIVE_INFINITY";
		}
		if (d == Double.MIN_VALUE) {
			return "Double.MIN_VALUE";
		}
		if (d == Double.MAX_VALUE) {
			return "Double.MAX_VALUE";
		}
		if (d == Double.MIN_NORMAL) {
			return "Double.MIN_NORMAL";
		}
		return Double.toString(d) + 'd';
	}

	public static String formatFloat(float f) {
		if (Float.isNaN(f)) {
			return "Float.NaN";
		}
		if (f == Float.NEGATIVE_INFINITY) {
			return "Float.NEGATIVE_INFINITY";
		}
		if (f == Float.POSITIVE_INFINITY) {
			return "Float.POSITIVE_INFINITY";
		}
		if (f == Float.MIN_VALUE) {
			return "Float.MIN_VALUE";
		}
		if (f == Float.MAX_VALUE) {
			return "Float.MAX_VALUE";
		}
		if (f == Float.MIN_NORMAL) {
			return "Float.MIN_NORMAL";
		}
		return Float.toString(f) + 'f';
	}

	public static String capitalizeFirstChar(String str) {
		if (isEmpty(str)) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}
}
