package com.elasticsearch.custom;

import java.util.ArrayList;
import java.util.List;

import com.elasticsearch.custom.api.Client;
import com.elasticsearch.custom.api.Doc;
import com.elasticsearch.custom.api.HitDoc;
import com.elasticsearch.custom.util.SignatureUtil;

public class TestClient {
	public static Client client = null;
	public static void main(String[] args) {
		client = new Client("192.1681.1.136");
		testQuery();
//		testExists();
//		testbulkAdd();
	}
	
	public static void testQuery(){
		String siteprefix = "http://www.taihainet.com";
		String signature = SignatureUtil.signature("http://www.taihainet.com/news/fujian/yghx/2013-04-09/1049760.html");
		String keyword = "漳浦赤土乡|5,3,0.7497855663120012 市委|2,0,0.44332910313363244 抛弃|7,3,0.15571059411452604 漳州|3,0,0.744529820442697 湖西|2,0,1.0 漳浦|7,3,0.9563777805141502 浦赤土|5,3,0.7525562167154227 民政部门|5,3,0.6278171801824994 女子|2,0,0.5041807691326353 陈文元|2,0,0.5379742875086084 漳州市委办|2,0,0.5652431755610914 深山|9,3,0.6390313756112161 乡政府|2,0,0.6278171801824994 疏忽|2,0,0.20000000000000004 走失|3,0,0.1883674081371067 精神病人|6,3,0.6324046742304701 精神病|7,3,0.6919598325487599 范亭|4,0,1.0 找到|3,0,0.12448292249653688 赤土乡|8,3,0.6605353737103273 途中|2,0,0.15245221301299264";
		String topic = "91|0.13916666666666713";
//		List<HitDoc> hitDocs = client.query(siteprefix, signature, keyword, topic, System.currentTimeMillis(), 10);
//		List<HitDoc> hitDocs = client.query(siteprefix, signature, null, topic, System.currentTimeMillis(), 10);
		List<HitDoc> hitDocs = client.query(siteprefix, signature, keyword, topic, System.currentTimeMillis(), 10);
		System.out.println(hitDocs.size());
		for(HitDoc hitdoc : hitDocs) System.out.println(hitdoc);
	}
	
	public static void testExists(){
		List<String> urls = new ArrayList<String>();
		urls.add("http://www.taihainet.com/news/fujian/yghx/2013-04-09/1049760.html");
		System.out.println(client.exists(urls));
	}
	
	public static void testbulkAdd(){
		List<Doc> docs = new ArrayList<Doc>();
		
		String url = "http://www.test.com";
		for(int i = 0; i < 20; i++){
			String tempUrl = url + "/" + i;
			Doc doc = new Doc(tempUrl, "test title" + i, SignatureUtil.signature(tempUrl), "", "test|2,3", "12|0.321", System.currentTimeMillis());
			docs.add(doc);
		}
		
		client.bulkAdd(docs);
	}
}
