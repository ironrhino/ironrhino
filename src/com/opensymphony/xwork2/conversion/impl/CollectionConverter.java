package com.opensymphony.xwork2.conversion.impl;

import com.opensymphony.xwork2.conversion.ObjectTypeDeterminer;
import com.opensymphony.xwork2.conversion.TypeConverter;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.XWorkList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class CollectionConverter extends DefaultTypeConverter {

	private ObjectTypeDeterminer objectTypeDeterminer;

	@Inject
	public void setObjectTypeDeterminer(ObjectTypeDeterminer determiner) {
		this.objectTypeDeterminer = determiner;
	}

	public Object convertValue(Map<String, Object> context, Object target, Member member, String propertyName,
			Object value, Class toType) {
		Collection result;
		Class memberType = String.class;

		if (target != null) {
			memberType = objectTypeDeterminer.getElementClass(target.getClass(), propertyName, null);

			if (memberType == null) {
				memberType = String.class;
			}
		}

		if (toType.isAssignableFrom(value.getClass())) {
			// no need to do anything
			result = (Collection) value;
		} else if (value instanceof Object[]) {
			Object[] objArray = (Object[]) value;
			TypeConverter converter = getTypeConverter(context);
			result = createCollection(toType, memberType, objArray.length);

			for (Object anObjArray : objArray) {
				Object convertedValue = converter.convertValue(context, target, member, propertyName, anObjArray,
						memberType);
				if (!TypeConverter.NO_CONVERSION_POSSIBLE.equals(convertedValue)) {
					result.add(convertedValue);
				}
			}
		} else if (Collection.class.isAssignableFrom(value.getClass())) {
			Collection col = (Collection) value;
			TypeConverter converter = getTypeConverter(context);
			result = createCollection(toType, memberType, col.size());

			for (Object aCol : col) {
				Object convertedValue = converter.convertValue(context, target, member, propertyName, aCol, memberType);
				if (!TypeConverter.NO_CONVERSION_POSSIBLE.equals(convertedValue)) {
					result.add(convertedValue);
				}
			}
		} else {
			result = createCollection(toType, memberType, -1);
			result.add(value);
		}

		return result;

	}

	private Collection createCollection(Class toType, Class memberType, int size) {
		Collection result;

		if (toType == Set.class) {
			if (size > 0) {
				result = new LinkedHashSet(size);
			} else {
				result = new LinkedHashSet();
			}
		} else if (toType == SortedSet.class) {
			result = new TreeSet();
		} else if (Set.class.isAssignableFrom(toType) && !toType.isInterface()
				&& !Modifier.isAbstract(toType.getModifiers())) {
			try {
				result = (Set) toType.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				result = new LinkedHashSet();
			}
		} else {
			if (size > 0) {
				result = new XWorkList(memberType, size);
			} else {
				result = new XWorkList(memberType);
			}
		}

		return result;
	}

}
