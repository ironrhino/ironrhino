
package org.ironrhino.core.freemarker;

import java.util.Map;
import java.util.Set;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;

@SuppressWarnings("rawtypes")
public class MyMapModel extends MapModel {
	static final ModelFactory FACTORY = new ModelFactory() {

		public TemplateModel create(Object object, ObjectWrapper wrapper) {
			return new MyMapModel((Map) object, (BeansWrapper) wrapper);
		}
	};

	public MyMapModel(Map map, BeansWrapper wrapper) {
		super(map, wrapper);
	}

	@Override
	public boolean isEmpty() {
		return ((Map) object).isEmpty();
	}

	@Override
	protected Set keySet() {
		return ((Map) object).keySet();
	}
}
