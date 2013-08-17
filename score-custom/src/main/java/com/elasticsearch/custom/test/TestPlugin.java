package com.elasticsearch.custom.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.SimpleFSDirectory;
import org.elasticsearch.index.query.IndexQueryParserService;

import com.alibaba.fastjson.JSON;
import com.elasticsearch.custom.api.Query;
import com.elasticsearch.custom.filter.FuzzyDuplicateFilter;
import com.elasticsearch.custom.plugin.CustomQueryParser;
import com.elasticsearch.custom.query.CustomQuery;
import com.elasticsearch.custom.util.LargestTitle;
import com.elasticsearch.custom.util.SignatureUtil;

public class TestPlugin {
	public static void main(String[] args) throws IOException {
		DirectoryReader reader = DirectoryReader.open(new SimpleFSDirectory(new File("E:/contentv2_taihainet.com")));
		IndexSearcher searcher = new IndexSearcher(reader);
		
		String siteprefix = "http://www.taihainet.com";
		BooleanFilter filters = new BooleanFilter();
//		filters.add(NumericRangeFilter.newLongRange("lastModified", 1355760000000L, System.currentTimeMillis(), true, true), Occur.MUST);
		filters.add(new PrefixFilter(new Term("url", siteprefix)), Occur.MUST);
		filters.add(new FuzzyDuplicateFilter("signature"), Occur.MUST);
		Filter filter = new CachingWrapperFilter(filters);
		
		String signature = SignatureUtil.signature(LargestTitle.parseLargest("test title"));
		String keyword = "漳浦赤土乡|5,3,0.7497855663120012 市委|2,0,0.44332910313363244 抛弃|7,3,0.15571059411452604 漳州|3,0,0.744529820442697 湖西|2,0,1.0 漳浦|7,3,0.9563777805141502 浦赤土|5,3,0.7525562167154227 民政部门|5,3,0.6278171801824994 女子|2,0,0.5041807691326353 陈文元|2,0,0.5379742875086084 漳州市委办|2,0,0.5652431755610914 深山|9,3,0.6390313756112161 乡政府|2,0,0.6278171801824994 疏忽|2,0,0.20000000000000004 走失|3,0,0.1883674081371067 精神病人|6,3,0.6324046742304701 精神病|7,3,0.6919598325487599 范亭|4,0,1.0 找到|3,0,0.12448292249653688 赤土乡|8,3,0.6605353737103273 途中|2,0,0.15245221301299264";
//		String keyword = "袭警|2,2,0.832121240 诏安|2,1,0.32125412312";
		String topic = "11|0.22658603377453193";
		
    	Query q = new Query(signature, keyword, topic, System.currentTimeMillis());
        Map<String,  Query> querymap = new HashMap<String,  Query>();
  		querymap.put(CustomQueryParser.NAME, q);
  		IndexQueryParserService service = TestQueryParser.getIndexQueryParserService();
        CustomQuery query =(CustomQuery)service.parse(JSON.toJSONString(querymap, false)).query();
//    	Query q = query.rewrite(reader);
//    	
//    	for (int i = 0; i < Integer.MAX_VALUE; i++) {
//    		long start = System.currentTimeMillis();
//    		searcher.search(query, filter, 20);
//    		System.out.println("cost(ms): " + (System.currentTimeMillis() - start));
//    	}
    	
//    	TopDocs result = searcher.search(query, filter, 20);
        TopDocs result = searcher.search(query, 20);
    	System.out.println("total: " + result.totalHits);
    	for (ScoreDoc doc: result.scoreDocs) {
    		Document d = searcher.doc(doc.doc);
    		System.out.println(doc.score + " => " + d.get("title") + " / " + d.get("url") + " => " + d.get("signature"));
    		System.out.println(searcher.explain(query, doc.doc));
    	}
    	reader.close();
	}
}