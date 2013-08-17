package com.elasticsearch.custom.query;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.valuesource.LongFieldSource;
import org.apache.lucene.search.Query;

public class RecencyBoostQuery extends CustomScoreQuery {
	private LongFieldSource lastModifiedSrc;
	private long refTime;

	public RecencyBoostQuery(Query query, long refTime, LongFieldSource lastModifiedSrc) {
		super(query);
		this.lastModifiedSrc = lastModifiedSrc;
		this.refTime = refTime;
	}
	
	private class RecencyBooster extends CustomScoreProvider {
		private long now = System.currentTimeMillis();
		private FunctionValues vals;
		
		public RecencyBooster(AtomicReaderContext context) throws IOException {
			super(context);
			vals = lastModifiedSrc.getValues(null, context);
		}

		@Override
		public float customScore(int doc, float queryScore, float valSrcScore) throws IOException {
			long lastModified = vals.longVal(doc);
			double timediff = (now - lastModified) / 3600000d;
            if (timediff < 0) timediff = 0;
			if (lastModified > refTime) timediff = Math.min((lastModified - refTime) / 3600000d, timediff);
			double timescore = 1 / Math.log(Math.E + timediff);
			return queryScore * (1 + (float)timescore);
		}
		
	}

	@Override
	protected CustomScoreProvider getCustomScoreProvider(AtomicReaderContext context) throws IOException {
		return new RecencyBooster(context);
	}
	
}