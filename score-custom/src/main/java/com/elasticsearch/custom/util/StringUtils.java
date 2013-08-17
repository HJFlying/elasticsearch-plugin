package com.elasticsearch.custom.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringUtils {
	private static String[] emptyarray = new String[0];
	private static String[] ctrl2str = new String[32];
	
	static {
		Arrays.fill(ctrl2str, "");
		ctrl2str['\b'] = "\\b";
		ctrl2str['\f'] = "\\f";
		ctrl2str['\n'] = "\\n";
		ctrl2str['\r'] = "\\r";
		ctrl2str['\t'] = "\\t";
	}
	
	public static String[] split(String str, char ch) {
		List<String> substrs = new ArrayList<String>();
		int last = 0;
		for (int i = 0; i < str.length(); i++) {
			if (ch == str.charAt(i)) {
				if (i > last) substrs.add(str.substring(last, i));
				last = i + 1;
			}
		}
		if (str.length() > last) substrs.add(str.substring(last));
		return substrs.toArray(emptyarray);
	}

	public static String join(List<String> strs, String sep) {
		StringBuffer buf = new StringBuffer(256); // Java default is 16, probably too small
		if (strs.size() > 0) {
			buf.append(strs.get(0));
			for (String str: strs.subList(1, strs.size())) buf.append(sep).append(str);
		}
		return buf.toString();
	}

	public static String escapeJson(String json) {
		StringBuilder sb = new StringBuilder(256);
		int last = 0;
		for (int i = 0; i < json.length(); i++) {
			char ch = json.charAt(i);
			if (ch < 32) {
				if (i > last) sb.append(json.substring(last, i)).append(ctrl2str[ch]);
				last = i + 1;
			}
		}
		if (json.length() > last) sb.append(json.substring(last));
		return sb.toString();
	}
	
	public static void main(String[] args) {
		char[] buf = new char[]{16,16};
		System.out.println(escapeJson(new String(buf)).length());
	}
}