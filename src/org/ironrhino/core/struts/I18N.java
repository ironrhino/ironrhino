package org.ironrhino.core.struts;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.LocalizedTextUtil;
import com.opensymphony.xwork2.util.ValueStack;

public class I18N {

	public static String getText(String key) {
		return getText(I18N.class, key);
	}

	public static String getText(Class<?> clazz, String key) {
		ActionContext context = ActionContext.getContext();
		Locale locale = context != null ? context.getLocale() : Locale
				.getDefault();
		ValueStack vs = context != null ? context.getValueStack() : null;
		return LocalizedTextUtil.findText(clazz, key, locale, key, null, vs);
	}

	public static String getTextForEnum(Class<? extends Enum<?>> clazz) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Enum<?> en : clazz.getEnumConstants()) {
			map.put(en.name(), LocalizedTextUtil.findText(clazz, en.name(),
					ActionContext.getContext() != null ? ActionContext
							.getContext().getLocale() : Locale.getDefault(), en
							.name(), null));
		}
		return map.toString();
	}

}
