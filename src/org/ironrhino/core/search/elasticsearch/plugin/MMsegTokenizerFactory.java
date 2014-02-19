package org.ironrhino.core.search.elasticsearch.plugin;

import java.io.File;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenizerFactory;
import org.elasticsearch.index.settings.IndexSettings;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.SimpleSeg;
import com.chenlb.mmseg4j.analysis.MMSegTokenizer;

/**
 * 
 * @author /medcl
 * @url https://github.com/medcl/elasticsearch-analysis-mmseg
 * 
 */
public class MMsegTokenizerFactory extends AbstractTokenizerFactory {

	Seg seg_method;
	private String seg_type;

	@Inject
	public MMsegTokenizerFactory(Index index,
			@IndexSettings Settings indexSettings, Environment env,
			@Assisted String name, @Assisted Settings settings) {
		super(index, indexSettings, name, settings);

		String path = new File(env.configFile(), "mmseg").getPath();
		logger.info(path);
		Dictionary dic = Dictionary.getInstance(path);
		// seg_type = settings.get("seg_type", "max_word");
		seg_type = settings.get("seg_type", "complex");
		if (seg_type.equals("max_word")) {
			seg_method = new MaxWordSeg(dic);
		} else if (seg_type.equals("complex")) {
			seg_method = new ComplexSeg(dic);
		} else if (seg_type.equals("simple")) {
			seg_method = new SimpleSeg(dic);
		}

	}

	@Override
	public Tokenizer create(Reader reader) {
		logger.info(seg_type);
		logger.info(seg_method.toString());
		return new MMSegTokenizer(seg_method, reader);
	}
}
