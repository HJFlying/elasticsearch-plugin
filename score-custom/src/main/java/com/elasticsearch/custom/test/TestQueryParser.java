package com.elasticsearch.custom.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.cache.IndexCacheModule;
import org.elasticsearch.index.codec.CodecModule;
import org.elasticsearch.index.engine.IndexEngineModule;
import org.elasticsearch.index.query.IndexQueryParserModule;
import org.elasticsearch.index.query.IndexQueryParserService;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.index.similarity.SimilarityModule;
import org.elasticsearch.indices.query.IndicesQueriesModule;
import org.elasticsearch.script.ScriptModule;

import com.alibaba.fastjson.JSON;
import com.elasticsearch.custom.api.Query;
import com.elasticsearch.custom.plugin.CustomPrefixParser;
import com.elasticsearch.custom.plugin.CustomQueryParser;
import com.elasticsearch.custom.query.CustomQuery;
import com.elasticsearch.custom.util.LargestTitle;
import com.elasticsearch.custom.util.SignatureUtil;

public class TestQueryParser {
	public static void main(String[] args) throws IOException{
		IndexQueryParserService service = getIndexQueryParserService();
		
		String topic = "1|0.122 3|0.55 7|0.22";
        Query q = new Query(SignatureUtil.signature(LargestTitle.parseLargest("test title")), "希望|2,3,0.231 中国|4,2,0.11 强大|1,2,0.1212", topic, System.currentTimeMillis());
        Map<String,  Query> querymap = new HashMap<String,  Query>();
		querymap.put(CustomQueryParser.NAME, q);
        
        CustomQuery lezhiQuery =(CustomQuery)service.parse(JSON.toJSONString(querymap, false)).query();
        System.out.println(lezhiQuery.toString());
	}
	
	public static IndexQueryParserService getIndexQueryParserService(){
		Settings settings = ImmutableSettings.Builder.EMPTY_SETTINGS;

		//add custom queryParser and filterParser
		IndexQueryParserModule queryParserModule = new IndexQueryParserModule(settings);
		queryParserModule.addFilterParser(CustomPrefixParser.NAME, CustomPrefixParser.class);
		queryParserModule.addQueryParser(CustomQueryParser.NAME, CustomQueryParser.class);

        Index index = new Index("test");
        Injector injector = new ModulesBuilder().add(
                new SettingsModule(settings),
//                new ThreadPoolModule(settings),
                new IndicesQueriesModule(),
                new ScriptModule(settings),
                new IndexSettingsModule(index, settings),
                new IndexCacheModule(settings),
                new AnalysisModule(settings),
                new IndexEngineModule(settings),
                new SimilarityModule(settings),
                queryParserModule,
                new IndexNameModule(index),
                new CodecModule(settings),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ClusterService.class).toProvider(Providers.of((ClusterService) null));
                    }
                }
        ).createInjector();
        
        IndexQueryParserService service = injector.getInstance(IndexQueryParserService.class);
//        injector.getInstance(ThreadPool.class).shutdownNow();
        return service;
	}
}
