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

	public static String getText(String key, Object[] args) {
		return getText(I18N.class, key, args);
	}

	public static String getText(Class<?> clazz, String key) {
		return getText(clazz, key, null);
	}

	public static String getText(Class<?> clazz, String key, Object[] args) {
		ActionContext context = ActionContext.getContext();
		Locale locale = context != null ? context.getLocale() : Locale.getDefault();
		ValueStack vs = context != null ? context.getValueStack() : null;
		try {
			return LocalizedTextUtil.findText(clazz, key, locale, key, args, vs);
		} catch (Exception e) {
			return key;
		}
	}

	public static String getTextForEnum(Class<? extends Enum<?>> clazz) {
		ActionContext context = ActionContext.getContext();
		Locale locale = context != null ? context.getLocale() : Locale.getDefault();
		ValueStack vs = context != null ? context.getValueStack() : null;
		Map<String, String> map = new LinkedHashMap<>();
		for (Enum<?> en : clazz.getEnumConstants()) {
			try {
				map.put(en.name(), LocalizedTextUtil.findText(clazz, en.name(), locale, en.name(), null, vs));
			} catch (Exception e) {
				e.printStackTrace();
				map.put(en.name(), en.name());
			}
		}
		return map.toString();
	}

}
