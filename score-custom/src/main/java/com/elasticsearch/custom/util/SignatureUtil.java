package com.elasticsearch.custom.util;


public class SignatureUtil {
	private SignatureUtil() {}
	
	public static String signature(String text) {
		return Long.toHexString(MurmurHash3.hash64(text));
	}
	
	public static void main(String[] args) {
		System.out.println(signature("http://test.buzzinate.com/wordpress/?p=5019"));
	}
}