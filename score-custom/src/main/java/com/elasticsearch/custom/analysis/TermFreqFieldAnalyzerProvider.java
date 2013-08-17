package com.elasticsearch.custom.analysis;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.elasticsearch.index.settings.IndexSettings;

public class TermFreqFieldAnalyzerProvider extends AbstractIndexAnalyzerProvider<TermFreqFieldAnalyzer> {

    private final TermFreqFieldAnalyzer analyzer;

    @Inject
    public TermFreqFieldAnalyzerProvider(Index index, @IndexSettings Settings indexSettings, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings, name, settings);
        analyzer = new TermFreqFieldAnalyzer();
    }

    @Override
    public TermFreqFieldAnalyzer get() {
        return this.analyzer;
    }
}