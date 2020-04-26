package org.ironrhino.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.core.ResolvableType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GenericTypeResolver {

	public static Type resolveType(Type type, Class<?> contextClass) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			ResolvableType[] generics = new ResolvableType[pt.getActualTypeArguments().length];
			int i = 0;
			for (Type arg : pt.getActualTypeArguments()) {
				generics[i++] = ResolvableType.forType(resolveType(arg, contextClass));
			}
			return ResolvableType.forClassWithGenerics((Class<?>) pt.getRawType(), generics).getType();
		}
		return org.springframework.core.GenericTypeResolver.resolveType(type, contextClass);
	}

}
