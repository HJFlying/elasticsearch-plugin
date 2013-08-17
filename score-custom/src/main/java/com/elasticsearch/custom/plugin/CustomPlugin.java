package com.elasticsearch.custom.plugin;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.query.IndexQueryParserModule;
import org.elasticsearch.plugins.AbstractPlugin;

import com.elasticsearch.custom.analysis.TermFreqFieldAnalyzerProvider;
import com.elasticsearch.custom.analysis.TopicAnalyzerProvider;

public class CustomPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "lezhi-plugin";
    }

    @Override
    public String description() {
        return "Lezhi recommendation algorithm plugin";
    }

    public void onModule(AnalysisModule module) {
    	module.addAnalyzer("lezhi_keyword", TermFreqFieldAnalyzerProvider.class);
        module.addAnalyzer("lezhi_topic", TopicAnalyzerProvider.class);
    }
    
    public void onModule(IndexQueryParserModule module) {
        module.addFilterParser(CustomPrefixParser.NAME, CustomPrefixParser.class);
    	module.addQueryParser(CustomQueryParser.NAME, CustomQueryParser.class);
    }
}
