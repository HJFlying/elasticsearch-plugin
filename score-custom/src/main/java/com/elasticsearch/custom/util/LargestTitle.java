package com.elasticsearch.custom.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LargestTitle {
	private static Set<Character> defaulttitlechars = new HashSet<Character>(Arrays.asList('"', '“', '”', '<', '>', '《', '》', '.'));
	
	public static String parseLargest(String title) {
		return parseLargest(title, defaulttitlechars);
	}
	
	public static String parseLargest(String title, Set<Character> titlechars) {
		List<String> parts = new ArrayList<String>();
		int last = 0;
		for (int i = 0; i < title.length(); i++) {
			char ch = title.charAt(i);
			if (Character.isLetterOrDigit(ch) || Character.isWhitespace(ch) || titlechars.contains(ch)) continue;
			if (i > last) {
				if (i > last + 1) parts.add(title.substring(last, i));
			}
			last = i + 1;
		}
		if (title.length() > last) {
			if (title.length() > last + 1) parts.add(title.substring(last));
		}
		
		int maxLen = 0;
		String largest = "";
		for (String part: parts) {
			if (maxLen < part.length()) {
				maxLen = part.length();
				largest = part;
			}
		}
		return largest;
	}
	
	public static void main(String[] args) {
		System.out.println(parseLargest("孙俪无缘续集 《甄嬛传》续集甄嬛角色大猜想"));
	}
}