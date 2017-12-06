package org.ironrhino.core.struts;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.CompoundRoot;
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
		Locale locale = context != null ? context.getLocale() : LocaleContextHolder.getLocale();
		ValueStack vs = context != null ? context.getValueStack() : DummyValueStack.INSTANCE;
		try {
			return LocalizedTextUtil.findText(clazz, key, locale, key, args, vs);
		} catch (Exception e) {
			return key;
		}
	}

	@SuppressWarnings("rawtypes")
	private static class DummyValueStack implements ValueStack {

		static final DummyValueStack INSTANCE = new DummyValueStack();

		@Override
		public Map<String, Object> getContext() {
			return Collections.emptyMap();
		}

		@Override
		public void setDefaultType(Class clazz) {
		}

		@Override
		public void setExprOverrides(Map<Object, Object> paramMap) {
		}

		@Override
		public Map<Object, Object> getExprOverrides() {
			return Collections.emptyMap();
		}

		@Override
		public CompoundRoot getRoot() {
			return null;
		}

		@Override
		public void setValue(String key, Object value) {
		}

		@Override
		public void setParameter(String key, Object value) {
		}

		@Override
		public void setValue(String key, Object value, boolean paramBoolean) {
		}

		@Override
		public String findString(String key) {
			return null;
		}

		@Override
		public String findString(String key, boolean paramBoolean) {
			return null;
		}

		@Override
		public Object findValue(String paramString) {
			return null;
		}

		@Override
		public Object findValue(String paramString, boolean paramBoolean) {
			return null;
		}

		@Override
		public Object findValue(String paramString, Class paramClass) {
			return null;
		}

		@Override
		public Object findValue(String paramString, Class paramClass, boolean paramBoolean) {
			return null;
		}

		@Override
		public Object peek() {
			return null;
		}

		@Override
		public Object pop() {
			return null;
		}

		@Override
		public void push(Object paramObject) {
		}

		@Override
		public void set(String paramString, Object paramObject) {
		}

		@Override
		public int size() {
			return 0;
		}

	}

}
