package com.elasticsearch.custom.query.keyword;

import com.elasticsearch.custom.util.StringUtils;

public class KeywordInfo {
	private String term;
	private int freq;
	private byte field;
	private double boost;
	
	public KeywordInfo(String term, int freq, byte field, double boost) {
		this.term = term;
		this.freq = freq;
		this.field = field;
		this.boost = boost;
	}

	public byte getField() {
		return field;
	}

	public String getTerm() {
		return term;
	}

	public int getFreq() {
		return freq;
	}

	public double getBoost() {
		return boost;
	}
	
	public static KeywordInfo[] parse(String text) {
		String[] parts = StringUtils.split(text, ' ');
		KeywordInfo[] tis = new KeywordInfo[parts.length];
		for (int i = 0; i < tis.length; i++) {
			String[] ti = StringUtils.split(parts[i], '|');
			String term = ti[0];
			String[] infos = StringUtils.split(ti[1], ',');
			tis[i] = new KeywordInfo(term, Integer.parseInt(infos[0]), Byte.parseByte(infos[1]), Double.parseDouble(infos[2]));
		}
		return tis;
	}

	@Override
	public String toString() {
		return term + "(freq=" + freq + ", field=" + field + ", boost=" + boost + ")";
	}
	
	public static void main(String[] args) {
		KeywordInfo[] tis = KeywordInfo.parse("扬言|3,0,0.1673431859774193 男主角|1,0,0.7540591208787603 排除|2,0,0.14081962115009938 朋友|2,0,0.459728868960273 莫文蔚|1,0,1.0 合作|7,0,0.08903485883505796 介意|4,0,0.1883674081371067 娱乐圈|1,0,0.7214044476996578 坦言|2,0,0.16993018624822684 冯德伦|1,1,1.0 明星|9,3,0.5459706407080699 旧爱|7,3,0.5827899076547037 周丽淇|3,0,1.0 郑嘉颖|3,0,1.0 分手|7,3,0.1883674081371067 郑伊健|1,0,1.0 荧幕|2,0,0.8000000000000002 范冰冰|4,2,1.0 机会|2,0,0.4668485557937154 王学兵|3,3,1.0 拍电影|1,0,0.6142634976141934");
		for (KeywordInfo ti: tis) System.out.println(ti);
	}
}