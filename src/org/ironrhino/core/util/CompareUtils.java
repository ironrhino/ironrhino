package org.ironrhino.core.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.ironrhino.core.model.Persistable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CompareUtils {

	public static boolean equals(Object a, Object b) {
		return equals(a, b, true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean equals(Object a, Object b, boolean treatEmptySequenceAsNull) {
		if (treatEmptySequenceAsNull) {
			if (a != null && a.getClass().isArray() && Array.getLength(a) == 0)
				a = null;
			if (b != null && b.getClass().isArray() && Array.getLength(b) == 0)
				b = null;
			if (a instanceof Collection && ((Collection<?>) a).isEmpty())
				a = null;
			if (b instanceof Collection && ((Collection<?>) b).isEmpty())
				b = null;
			if (a instanceof Map && ((Map<?, ?>) a).isEmpty())
				a = null;
			if (b instanceof Map && ((Map<?, ?>) b).isEmpty())
				b = null;
		}
		if (a != null && b == null || a == null && b != null)
			return false;
		if (Objects.equals(a, b))
			return true;
		if (a instanceof Persistable && b instanceof Persistable)
			return Objects.equals(((Persistable<?>) a).getId(), ((Persistable<?>) b).getId());
		else if (a.getClass().isArray() && b.getClass().isArray()) {
			if (Array.getLength(a) != Array.getLength(b))
				return false;
			for (int i = 0; i < Array.getLength(a); i++)
				if (!equals(Array.get(a, i), Array.get(b, i), treatEmptySequenceAsNull))
					return false;
			return true;
		} else if (a instanceof Collection && b instanceof Collection) {
			Collection<?> coll1 = (Collection<?>) a;
			Collection<?> coll2 = (Collection<?>) b;
			if (coll1.size() != coll2.size())
				return false;
			Iterator<?> it1 = coll1.iterator();
			Iterator<?> it2 = coll2.iterator();
			while (it1.hasNext())
				if (!equals(it1.next(), it2.next(), treatEmptySequenceAsNull))
					return false;
			return true;
		} else if (a instanceof Map && b instanceof Map) {
			Map map1 = (Map) a;
			Map map2 = (Map) b;
			if (map1.size() != map2.size())
				return false;
			Iterator<Entry> it1 = map1.entrySet().iterator();
			Iterator<Entry> it2 = map2.entrySet().iterator();
			while (it1.hasNext())
				if (!equals(it1.next().getKey(), it2.next().getKey(), treatEmptySequenceAsNull)
						|| !equals(it1.next().getValue(), it2.next().getValue(), treatEmptySequenceAsNull))
					return false;
			return true;
		} else if (!BeanUtils.isSimpleProperty(a.getClass()) && !BeanUtils.isSimpleProperty(b.getClass())) {
			BeanWrapperImpl abw = new BeanWrapperImpl(a);
			BeanWrapperImpl bbw = new BeanWrapperImpl(b);
			PropertyDescriptor[] pds = abw.getPropertyDescriptors();
			if (pds.length > 1) {
				for (PropertyDescriptor pd : pds) {
					if (pd.getReadMethod() == null || pd.getWriteMethod() == null)
						continue;
					if (!equals(abw.getPropertyValue(pd.getName()), bbw.getPropertyValue(pd.getName()),
							treatEmptySequenceAsNull))
						return false;
				}
				return true;
			}
		}
		return false;
	}

}
