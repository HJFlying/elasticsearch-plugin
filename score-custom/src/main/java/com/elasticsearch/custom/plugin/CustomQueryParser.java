package com.elasticsearch.custom.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Query;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.cache.Cache;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.query.QueryParser;
import org.elasticsearch.index.query.QueryParsingException;

import com.elasticsearch.custom.cache.ElasticDocFreqCache;
import com.elasticsearch.custom.query.CustomQuery;
import com.elasticsearch.custom.query.keyword.KeywordInfo;
import com.elasticsearch.custom.query.keyword.KeywordQueryDetail;
import com.elasticsearch.custom.query.topic.TopicInfo;
import com.elasticsearch.custom.query.topic.TopicQueryDetail;

public class CustomQueryParser implements QueryParser {
    public static final String NAME = "lezhi_query";
    
    private Cache<String, Integer> cache = CacheBuilder.newBuilder()
			.maximumSize(100000).concurrencyLevel(5)
			.expireAfterWrite(10, TimeUnit.MINUTES).expireAfterAccess(5, TimeUnit.MINUTES)
			.build();

    @Override
    public String[] names() {
        return new String[]{NAME, Strings.toCamelCase(NAME)};
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
    	XContentParser parser = parseContext.parser();

    	int[] fieldboost = KeywordQueryDetail.DEFAULT_FIELD_BOOST;
        KeywordInfo[] tis = null;
        String signature = null;
        long reftime = System.currentTimeMillis();
        int maxword = 5;
        TopicInfo[] topics = null;
        
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
                token = parser.nextToken();
                if ("fieldboost".equals(currentFieldName)) {
                	String[] values = parseTextArray(parser);
                	fieldboost = new int[values.length];
                	for (int i = 0; i < values.length; i++) fieldboost[i] = Integer.parseInt(values[i]);
                } else if ("keyword".equals(currentFieldName)) {
                	tis = KeywordInfo.parse(parser.text());
                } else if ("signature".equals(currentFieldName)) {
                	signature = parser.text();
                } else if ("reftime".equals(currentFieldName)) {
                	reftime = Long.parseLong(parser.text());
                } else if ("maxword".equals(currentFieldName)) {
                	maxword = Integer.parseInt(parser.text());
                } else if("topic".equals(currentFieldName)){
                	topics = TopicInfo.parse(parser.text());
                } else throw new QueryParsingException(parseContext.index(), "[lezhi_query] query does not support [" + currentFieldName + "]");
            }
        }
        
        KeywordQueryDetail[] termDetails = KeywordQueryDetail.parse(tis, fieldboost, "keyword");
        TopicQueryDetail[] topicDetails = TopicQueryDetail.parse(topics, "topic");
        
        return new CustomQuery(new ElasticDocFreqCache(cache, parseContext.index().name()), termDetails, topicDetails, signature, reftime, maxword);
    }
    
    private String[] parseTextArray(XContentParser parser) throws IOException {
    	 if (parser.currentToken() != XContentParser.Token.START_ARRAY) {
             throw new ElasticSearchParseException("not an array");
         }
    	 
    	 List<String> values = new ArrayList<String>();
    	 XContentParser.Token token = parser.nextToken();
         while (token != XContentParser.Token.END_ARRAY) {
        	 values.add(parser.text());
        	 token = parser.nextToken();
         }
         
         return values.toArray(new String[0]);
    }
}