package org.ironrhino.rest.doc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.SampleObjectCreator;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Fields;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiDocHelper {

	private static SampleObjectCreator creator = new SampleObjectCreator((type, name) -> {
		if (type == RestStatus.class)
			return RestStatus.OK;
		return null;
	});

	public static Object generateSample(Class<?> apiDocClazz, Method apiDocMethod, Fields fields) throws Exception {
		if (fields != null) {
			if (StringUtils.isNotBlank(fields.sample()))
				return fields.sample();
			String sampleFileName = fields.sampleFileName();
			if (StringUtils.isNotBlank(sampleFileName)) {
				try (InputStream is = apiDocClazz.getResourceAsStream(sampleFileName)) {
					if (is == null) {
						throw new ErrorMessage(sampleFileName + " with " + apiDocClazz.getName() + " is not found!");
					}
					try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
						return br.lines().collect(Collectors.joining("\n"));
					}
				}
			}
			String sampleMethodName = fields.sampleMethodName();
			if (StringUtils.isNotBlank(sampleMethodName)) {
				Method m = apiDocClazz.getDeclaredMethod(sampleMethodName, new Class[0]);
				m.setAccessible(true);
				return m.invoke(apiDocClazz.getConstructor().newInstance(), new Object[0]);
			}
		}
		if (apiDocMethod != null) {
			Type[] argTypes = apiDocMethod.getGenericParameterTypes();
			Object[] args = new Object[argTypes.length];
			for (int i = 0; i < argTypes.length; i++) {
				Type type = argTypes[i];
				if (type instanceof Class) {
					Class<?> cls = (Class<?>) type;
					if (cls.isPrimitive()) {
						if (Number.class.isAssignableFrom(cls))
							args[i] = 0;
						else if (type == Boolean.TYPE)
							args[i] = false;
						else if (type == Byte.TYPE)
							args[i] = (byte) 0;
					}
				}
				if (args[i] == null) {
					args[i] = createSample(type);
				}
			}
			Object obj;
			try {
				obj = apiDocMethod.invoke(apiDocClazz.getConstructor().newInstance(), args);
			} catch (InvocationTargetException | NoSuchMethodException | IllegalArgumentException
					| NullPointerException e) {
				obj = null;
			}
			if (obj == null) {
				obj = createSample(apiDocMethod.getGenericReturnType());
			}
			return obj;
		}
		return null;

	}

	public static Object createSample(Type returnType) {
		return creator.createSample(returnType);
	}

}
