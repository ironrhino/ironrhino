package org.ironrhino.core.hibernate.type;

import java.time.YearMonth;
import java.util.Comparator;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

public class YearMonthJavaTypeDescriptor extends AbstractTypeDescriptor<YearMonth> {

	private static final long serialVersionUID = -2439159016195930125L;

	public static final YearMonthJavaTypeDescriptor INSTANCE = new YearMonthJavaTypeDescriptor();

	public static class YearMonthComparator implements Comparator<YearMonth> {
		public static final YearMonthComparator INSTANCE = new YearMonthComparator();

		public int compare(YearMonth o1, YearMonth o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}

	@SuppressWarnings("unchecked")
	public YearMonthJavaTypeDescriptor() {
		super(YearMonth.class, ImmutableMutabilityPlan.INSTANCE);
	}

	@Override
	public Comparator<YearMonth> getComparator() {
		return YearMonthComparator.INSTANCE;
	}

	public String toString(YearMonth value) {
		return value.toString();
	}

	public YearMonth fromString(String string) {
		if (string == null || string.isEmpty()) {
			return null;
		}
		return YearMonth.parse(string);
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(YearMonth value, Class<X> type, WrapperOptions options) {
		if (value == null) {
			return null;
		}
		if (String.class.isAssignableFrom(type)) {
			return (X) value.toString();
		}
		throw unknownUnwrap(type);
	}

	public <X> YearMonth wrap(X value, WrapperOptions options) {
		if (value == null) {
			return null;
		}
		if (String.class.isInstance(value)) {
			return fromString((String) value);
		}
		throw unknownWrap(value.getClass());
	}

}