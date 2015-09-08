package org.ironrhino.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ironrhino.core.model.Displayable;

public class EnumUtils {

	public static <T extends Enum<T>> Map<String, String> enumToMap(Class<T> clazz) {
		T[] enums = clazz.getEnumConstants();
		Map<String, String> map = new LinkedHashMap<>();
		if (Displayable.class.isAssignableFrom(clazz)) {
			for (T en : enums) {
				Displayable den = (Displayable) en;
				map.put(den.getName(), den.getDisplayName());
			}
		} else {
			for (T en : enums)
				map.put(en.name(), en.name());
		}
		return map;
	}

	public static <T extends Enum<T>> List<String> enumToList(Class<T> clazz) {
		T[] enums = clazz.getEnumConstants();
		List<String> list = new ArrayList<>();
		for (T en : enums)
			list.add(en.name());
		return list;
	}

}
