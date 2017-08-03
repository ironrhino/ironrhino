
package org.ironrhino.core.freemarker;

import java.util.Map;
import java.util.Set;

import freemarker.core.CollectionAndSequence;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;

public class FriendlyMapModel extends MapModel {
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
}
