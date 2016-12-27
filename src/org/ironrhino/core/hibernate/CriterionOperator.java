package org.ironrhino.core.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.util.DateUtils;

public enum CriterionOperator implements Displayable {

	EQ(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class || clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz == Boolean.class || clazz == String.class
					|| Number.class.isAssignableFrom(clazz) || Date.class.isAssignableFrom(clazz)
					|| Persistable.class.isAssignableFrom(clazz) || clazz.isEnum();
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... values) {
			return values != null && values.length == 1;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			Object value = values[0];
			if (value == null)
				return null;
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value))
				return Restrictions.between(name, value, DateUtils.endOfDay((Date) value));
			else
				return Restrictions.eq(name, value);
		}
	},
	NEQ(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return EQ.supports(clazz);
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... values) {
			return values != null && values.length == 1;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			Object value = values[0];
			if (value == null)
				return null;
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value))
				return Restrictions.or(Restrictions.isNull(name), Restrictions.or(Restrictions.lt(name, value),
						Restrictions.gt(name, DateUtils.endOfDay((Date) value))));
			else
				return Restrictions.ne(name, value);
		}
	},
	START(1) {

		@Override
		boolean supports(Class<?> clazz) {
			return clazz == String.class;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.like(name, (String) values[0], MatchMode.START);
		}
	},
	NOTSTART(1) {

		@Override
		boolean supports(Class<?> clazz) {
			return clazz == String.class;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.not(START.operator(name, values));
		}
	},
	END(1) {

		@Override
		boolean supports(Class<?> clazz) {
			return START.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.like(name, (String) values[0], MatchMode.END);
		}
	},
	NOTEND(1) {

		@Override
		boolean supports(Class<?> clazz) {
			return START.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.not(END.operator(name, values));
		}
	},
	INCLUDE(1) {

		@Override
		boolean supports(Class<?> clazz) {
			return clazz == String.class;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			String value = String.valueOf(values[0]);
			if (name.endsWith("AsString")) {
				return CriterionUtils.matchTag(name, value);
			} else {
				return Restrictions.like(name, value, MatchMode.ANYWHERE);
			}
		}
	},
	NOTINCLUDE(1) {

		@Override
		boolean supports(Class<?> clazz) {
			return INCLUDE.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.or(Restrictions.isNull(name), Restrictions.not(INCLUDE.operator(name, values)));
		}
	},
	CONTAINS(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return Set.class.isAssignableFrom(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			Object value = values[0];
			if (value instanceof Set) {
				value = ((Set<?>) value).iterator().next();
			}
			return CriterionUtils.matchTag(name, String.valueOf(value));
		}
	},
	NOTCONTAINS(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return CONTAINS.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.not(CONTAINS.operator(name, values));
		}
	},
	LT(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class || clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz == String.class || Number.class.isAssignableFrom(clazz)
					|| Date.class.isAssignableFrom(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.lt(name, values[0]);
		}
	},
	LE(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return LT.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			Object value = values[0];
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value))
				return Restrictions.le(name, DateUtils.endOfDay((Date) value));
			else
				return Restrictions.le(name, value);
		}
	},
	GT(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return LT.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			Object value = values[0];
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value))
				return Restrictions.gt(name, DateUtils.endOfDay((Date) value));
			else
				return Restrictions.gt(name, value);
		}
	},
	GE(1) {
		@Override
		boolean supports(Class<?> clazz) {
			return LT.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.ge(name, values[0]);
		}
	},
	BETWEEN(2) {
		@Override
		boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class || clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz == String.class || Number.class.isAssignableFrom(clazz)
					|| Date.class.isAssignableFrom(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			Object value1 = values[0];
			Object value2 = values[1];
			if (value2 instanceof Date && DateUtils.isBeginOfDay((Date) value2))
				value2 = DateUtils.endOfDay((Date) value2);
			if (value1 != null && value2 != null)
				return Restrictions.between(name, value1, value2);
			else if (value1 != null)
				return Restrictions.ge(name, value1);
			else if (value2 != null)
				return Restrictions.le(name, value2);
			else
				return null;
		}
	},
	NOTBETWEEN(2) {
		@Override
		boolean supports(Class<?> clazz) {
			return BETWEEN.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			Object value1 = values[0];
			Object value2 = values[1];
			if (value2 instanceof Date && DateUtils.isBeginOfDay((Date) value2))
				value2 = DateUtils.endOfDay((Date) value2);
			if (value1 != null && value2 != null)
				return Restrictions.or(Restrictions.lt(name, value1), Restrictions.gt(name, value2));
			else if (value1 != null)
				return Restrictions.lt(name, value1);
			else if (value2 != null)
				return Restrictions.gt(name, value2);
			else
				return null;
		}
	},
	ISNOTNULL(0) {
		@Override
		boolean supports(Class<?> clazz) {
			return !clazz.isPrimitive() && clazz.getAnnotation(Embeddable.class) == null;

		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.isNotNull(name);
		}
	},
	ISNULL(0) {
		@Override
		boolean supports(Class<?> clazz) {
			return ISNOTNULL.supports(clazz) && clazz.getAnnotation(Embeddable.class) == null;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.isNull(name);
		}
	},
	ISNOTEMPTY(0) {
		@Override
		boolean supports(Class<?> clazz) {
			return clazz == String.class;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.not(Restrictions.or(Restrictions.isNull(name), Restrictions.eq(name, "")));
		}
	},
	ISEMPTY(0) {

		@Override
		boolean supports(Class<?> clazz) {
			return ISNOTEMPTY.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.or(Restrictions.isNull(name), Restrictions.eq(name, ""));
		}
	},
	ISTRUE(0) {
		@Override
		boolean supports(Class<?> clazz) {
			return clazz == boolean.class || clazz == Boolean.class;
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.eq(name, true);
		}
	},
	ISFALSE(0) {
		@Override
		boolean supports(Class<?> clazz) {
			return ISTRUE.supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.eq(name, false);
		}
	},
	IN(-1) {
		@Override
		boolean supports(Class<?> clazz) {
			return clazz.isEnum();
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.in(name, values);
		}
	},
	NOTIN(-1) {
		@Override
		boolean supports(Class<?> clazz) {
			return clazz.isEnum();
		}

		@Override
		public Criterion operator(String name, Object... values) {
			return Restrictions.not(Restrictions.in(name, values));
		}
	};

	private int parametersSize;

	private CriterionOperator(int parametersSize) {
		this.parametersSize = parametersSize;
	}

	@Override
	public String getName() {
		return Displayable.super.getName();
	}

	@Override
	public String getDisplayName() {
		return Displayable.super.getDisplayName();
	}

	public static CriterionOperator parse(String name) {
		if (name != null)
			for (CriterionOperator en : values())
				if (name.equals(en.name()) || name.equals(en.getDisplayName()))
					return en;
		return null;
	}

	public abstract Criterion operator(String name, Object... values);

	public int getParametersSize() {
		return parametersSize;
	}

	public boolean isEffective(Class<?> clazz, String... values) {
		if (!supports(clazz))
			return false;
		int size = getParametersSize();
		if (size == 0) {
			return values.length == 0;
		} else {
			if (size > 0 && values.length != size)
				return false;
			for (String s : values)
				if (StringUtils.isBlank(s))
					return false;
			return true;
		}
	}

	abstract boolean supports(Class<?> clazz);

	public static List<String> getSupportedOperators(Class<?> clazz) {
		if (clazz == null)
			return Collections.emptyList();
		List<String> list = new ArrayList<>();
		for (CriterionOperator op : values())
			if (op.supports(clazz))
				list.add(op.name());
		return list;
	}

	public static List<String> getSupportedOperators(String className) {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			return Collections.emptyList();
		}
		return getSupportedOperators(clazz);
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

}
