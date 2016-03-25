package org.ironrhino.core.struts;

import org.apache.commons.lang3.StringUtils;

import com.opensymphony.xwork2.interceptor.ParametersInterceptor;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;

public class ParamsInterceptor extends ParametersInterceptor {

	private static final long serialVersionUID = -9206985757047973716L;

	private static final Logger LOG = LoggerFactory.getLogger(ParametersInterceptor.class);

	protected int autoGrowCollectionLimit = 255;

	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	@Override
	protected boolean acceptableName(String name) {
		boolean b = super.acceptableName(name);
		if (b) {
			int start = name.indexOf('[');
			while (start > 0) {
				int end = name.indexOf(']', start);
				if (end < 0)
					break;
				String s = name.substring(start + 1, end);
				if (StringUtils.isNumeric(s)) {
					int index = Integer.valueOf(s);
					if (index > autoGrowCollectionLimit) {
						LOG.warn("Parameter \"#0\" exceed max index: [#1]", name, autoGrowCollectionLimit);
						return false;
					}
				}
				start = name.indexOf('[', end);
			}
		}
		return b;
	}

}
