package com.elasticsearch.custom.plugin;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.PrefixFilter;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.cache.filter.support.CacheKeyFilter;
import org.elasticsearch.index.query.FilterParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.query.QueryParsingException;

import com.elasticsearch.custom.filter.FuzzyDuplicateFilter;

public class CustomPrefixParser implements FilterParser {

    public static final String NAME = "lezhi_prefix";

    public CustomPrefixParser() {
    }

    @Override
    public String[] names() {
        return new String[]{NAME};
    }

    @Override
    public Filter parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        boolean cache = true;
        String siteprefix = null;
        
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
            	if ("_cache".equals(currentFieldName)) {
                    cache = parser.booleanValue();
                } else if ("siteprefix".equals(currentFieldName)) {
                    siteprefix = parser.text();
                } else throw new QueryParsingException(parseContext.index(), "[lezhi_prefix] filter does not support [" + currentFieldName + "]");
            }
        }

        Filter filter = null;
        String cacheKey = null;
        if (siteprefix == null) {
        	filter = new FuzzyDuplicateFilter("signature");
        	cacheKey = parseContext.index().name();
        } else {
        	BooleanFilter filters = new BooleanFilter();
        	filters.add(new PrefixFilter(new Term("url", siteprefix)), Occur.MUST);
        	filters.add(new FuzzyDuplicateFilter("signature"), Occur.MUST);
        	filter = filters;
        	cacheKey = siteprefix;
        }

        if (cache) {
            filter = parseContext.cacheFilter(filter, new CacheKeyFilter.Key(cacheKey));
        }

        return filter;
    }
}