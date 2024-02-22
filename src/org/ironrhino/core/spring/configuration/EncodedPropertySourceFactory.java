package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.ClassUtils;

public class EncodedPropertySourceFactory extends DefaultPropertySourceFactory {

	public static final String KEY_DECODER = "DECODER";

	@SuppressWarnings("unchecked")
	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
		if (name == null) {
			Resource underlying = resource.getResource();
			name = underlying.getClass().getSimpleName() + "@" + System.identityHashCode(underlying);
		}
		return new EncodedPropertySource(name,
				(EnumerablePropertySource<Map<String, Object>>) super.createPropertySource(name, resource));
	}

	static class EncodedPropertySource extends EnumerablePropertySource<EnumerablePropertySource<Map<String, Object>>> {

		private final EnumerablePropertySource<Map<String, Object>> rawPropertySource;

		private final Function<String, String> decoder;

		@SuppressWarnings("unchecked")
		EncodedPropertySource(String name, EnumerablePropertySource<Map<String, Object>> source) {
			super(name, source);
			this.rawPropertySource = source;
			if (this.rawPropertySource.containsProperty(KEY_DECODER)) {
				String classname = source.getProperty(KEY_DECODER).toString();
				if (!ClassUtils.isPresent(classname, null)) {
					// IllegalArgumentException is ignored if ignoreResourceNotFound=true
					throw new RuntimeException("Provided decoder '" + classname + "' not found");
				}
				try {
					Class<?> clazz = ClassUtils.forName(classname, null);
					ResolvableType required = ResolvableType.forClassWithGenerics(Function.class, String.class,
							String.class);
					if (!required.isAssignableFrom(clazz)) {
						throw new RuntimeException("Provided decoder '" + classname
								+ "' must implements java.util.function.Function<String,String>");
					}
					decoder = BeanUtils.instantiateClass(clazz, Function.class);
				} catch (ClassNotFoundException ex) {
					throw new RuntimeException(ex);
				}
			} else {
				decoder = (s) -> new String(Base64.getDecoder().decode(s));
			}
		}

		@Override
		public Object getProperty(String name) {
			Object value = rawPropertySource.getProperty(name);
			if (KEY_DECODER.equals(name)) {
				return value;
			}
			if (value instanceof String) {
				try {
					value = decoder.apply((String) value);
				} catch (Exception ex) {
					throw new RuntimeException("Failed to resolve property: " + name, ex);
				}
			}
			return value;
		}

		@Override
		public boolean containsProperty(String name) {
			return rawPropertySource.containsProperty(name);
		}

		@Override
		public String[] getPropertyNames() {
			return rawPropertySource.getPropertyNames();
		}

	}

}
