
package org.ironrhino.core.freemarker;

import java.util.Map;
import java.util.Set;

import freemarker.core.CollectionAndSequence;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.ext.util.ModelFactory;
import freemarker.template.MapKeyValuePairIterator;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class FriendlyMapModel extends MapModel implements TemplateHashModelEx, TemplateHashModelEx2 {
	static final ModelFactory FACTORY = new ModelFactory() {

		@Override
		public TemplateModel create(Object object, ObjectWrapper wrapper) {
			return new FriendlyMapModel((Map<?, ?>) object, (BeansWrapper) wrapper);
		}
	};

	public FriendlyMapModel(Map<?, ?> map, BeansWrapper wrapper) {
		super(map, wrapper);
	}

	@Override
	public boolean isEmpty() {
		return ((Map<?, ?>) object).isEmpty();
	}

	@Override
	protected Set<?> keySet() {
		return ((Map<?, ?>) object).keySet();
	}

	@Override
	public TemplateCollectionModel values() {
		return new CollectionAndSequence(new SimpleSequence(((Map<?, ?>) object).values(), wrapper));
	}

	@Override
	public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
		return new MapKeyValuePairIterator(((Map<?, ?>) object), wrapper);
	}
}
