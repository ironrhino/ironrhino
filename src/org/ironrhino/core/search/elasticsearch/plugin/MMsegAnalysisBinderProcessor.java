package org.ironrhino.core.search.elasticsearch.plugin;

import org.elasticsearch.index.analysis.AnalysisModule;

/**
 * 
 * @author /medcl
 * @url https://github.com/medcl/elasticsearch-analysis-mmseg
 * 
 */
public class MMsegAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {

	@Override
	public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {

	}

	@Override
	public void processTokenizers(TokenizersBindings tokenizersBindings) {
		tokenizersBindings.processTokenizer("mmseg", MMsegTokenizerFactory.class);
		super.processTokenizers(tokenizersBindings);
	}

	@Override
	public void processAnalyzers(AnalyzersBindings analyzersBindings) {
		analyzersBindings.processAnalyzer("mmseg", MMsegAnalyzerProvider.class);
		super.processAnalyzers(analyzersBindings);
	}

}
