package org.ironrhino.core.struts;

import java.util.LinkedHashMap;
import java.util.Map;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

public class I18N {

	public static String getText(String key) {
		return LocalizedTextUtil.findText(I18N.class, key, ActionContext
				.getContext().getLocale(), key, null);
	}

	public static String getTextForEnum(Class<? extends Enum<?>> clazz) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Enum<?> en : clazz.getEnumConstants()) {
			map.put(en.name(), LocalizedTextUtil.findText(clazz, en.name(),
					ActionContext.getContext().getLocale(), en.name(), null));
		}
		return map.toString();
	}

}
