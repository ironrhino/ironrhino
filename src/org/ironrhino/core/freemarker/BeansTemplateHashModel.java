package org.ironrhino.core.freemarker;

import org.ironrhino.core.struts.MyFreemarkerManager;
import org.ironrhino.core.util.ApplicationContextUtils;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class BeansTemplateHashModel implements TemplateHashModel {

	@Override
	public TemplateModel get(String name) throws TemplateModelException {
		Object bean = ApplicationContextUtils.getBean(name);
		if (bean == null)
			return null;
		return MyFreemarkerManager.DEFAULT_BEANS_WRAPPER.wrap(bean);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException {
		return false;
	}

}
