package org.ironrhino.core.hibernate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Embeddable;

import org.apache.commons.lang3.ClassUtils;
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
		boolean supports(Type type) {
			if (!(type instanceof Class))
				return false;
			Class<?> clazz = (Class<?>) type;
			return clazz == Boolean.class || clazz == String.class || Number.class.isAssignableFrom(clazz)
					|| Number.class.isAssignableFrom(ClassUtils.primitiveToWrapper(clazz))
					|| Date.class.isAssignableFrom(clazz) || Temporal.class.isAssignableFrom(clazz)
					|| Persistable.class.isAssignableFrom(clazz) || clazz.isEnum();
		}

		@Override
		public boolean isEffective(Type type, String... values) {
			return values != null && values.length == 1;
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			Object value = values[0];
			if (value == null)
				return null;
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value))
				return Restrictions.between(name, value, DateUtils.endOfDay((Date) value));
			else if (value instanceof LocalDateTime) {
				LocalDateTime datetime = ((LocalDateTime) value);
				if (datetime.getHour() == 0 && datetime.getMinute() == 0 && datetime.getSecond() == 0
						&& datetime.getNano() == 0) {
					return Restrictions.between(name, datetime,
							datetime.withHour(23).withMinute(59).withSecond(59).withNano(99999));
				}
			}
			return Restrictions.eq(name, value);
		}
	},
	NEQ(1) {
		@Override
		boolean supports(Type type) {

			return EQ.supports(type);
		}

		@Override
		public boolean isEffective(Type type, String... values) {
			return values != null && values.length == 1;
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			Object value = values[0];
			if (value == null)
				return null;
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value))
				return Restrictions.or(Restrictions.isNull(name), Restrictions.or(Restrictions.lt(name, value),
						Restrictions.gt(name, DateUtils.endOfDay((Date) value))));
			else if (value instanceof LocalDateTime) {
				LocalDateTime datetime = ((LocalDateTime) value);
				if (datetime.getHour() == 0 && datetime.getMinute() == 0 && datetime.getSecond() == 0
						&& datetime.getNano() == 0) {
					return Restrictions.or(Restrictions.isNull(name),
							Restrictions.or(Restrictions.lt(name, datetime), Restrictions.gt(name,
									datetime.withHour(23).withMinute(59).withSecond(59).withNano(99999))));
				}
			}
			return Restrictions.or(Restrictions.ne(name, value), Restrictions.isNull(name));
		}
	},
	START(1) {

		@Override
		boolean supports(Type type) {
			return type == String.class;
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.like(name, (String) values[0], MatchMode.START);
		}
	},
	NOTSTART(1) {

		@Override
		boolean supports(Type type) {
			return type == String.class;
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.not(START.operator(name, type, values));
		}
	},
	END(1) {

		@Override
		boolean supports(Type type) {
			return START.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.like(name, (String) values[0], MatchMode.END);
		}
	},
	NOTEND(1) {

		@Override
		boolean supports(Type type) {
			return START.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.not(END.operator(name, type, values));
		}
	},
	INCLUDE(1) {

		@Override
		boolean supports(Type type) {
			return type == String.class;
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			String value = String.valueOf(values[0]);
			return Restrictions.like(name, value, MatchMode.ANYWHERE);
		}
	},
	NOTINCLUDE(1) {

		@Override
		boolean supports(Type type) {
			return INCLUDE.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.or(Restrictions.isNull(name), Restrictions.not(INCLUDE.operator(name, type, values)));
		}
	},
	CONTAINS(1) {
		@Override
		boolean supports(Type type) {
			return type instanceof ParameterizedType
					&& Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
					|| type instanceof Class && ((Class<?>) type).isArray();
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			Object value = values[0];
			if (value instanceof Collection) {
				value = ((Collection<?>) value).iterator().next();
			}
			return CriterionUtils.matchTag(name, String.valueOf(value));
		}
	},
	NOTCONTAINS(1) {
		@Override
		boolean supports(Type type) {
			return CONTAINS.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.not(CONTAINS.operator(name, type, values));
		}
	},
	LT(1) {
		@Override
		boolean supports(Type type) {
			if (!(type instanceof Class))
				return false;
			Class<?> clazz = (Class<?>) type;
			return clazz == String.class || Number.class.isAssignableFrom(clazz)
					|| Number.class.isAssignableFrom(ClassUtils.primitiveToWrapper(clazz))
					|| Date.class.isAssignableFrom(clazz) || Temporal.class.isAssignableFrom(clazz);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.lt(name, values[0]);
		}
	},
	LE(1) {
		@Override
		boolean supports(Type type) {
			return LT.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			Object value = values[0];
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value)) {
				return Restrictions.le(name, DateUtils.endOfDay((Date) value));
			} else if (value instanceof LocalDateTime) {
				LocalDateTime datetime = ((LocalDateTime) value);
				if (datetime.getHour() == 0 && datetime.getMinute() == 0 && datetime.getSecond() == 0
						&& datetime.getNano() == 0) {
					return Restrictions.le(name, datetime.withHour(23).withMinute(59).withSecond(59).withNano(99999));
				}
			}
			return Restrictions.le(name, value);
		}
	},
	GT(1) {
		@Override
		boolean supports(Type type) {
			return LT.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			Object value = values[0];
			if (value instanceof Date && DateUtils.isBeginOfDay((Date) value)) {
				return Restrictions.gt(name, DateUtils.endOfDay((Date) value));
			} else if (value instanceof LocalDateTime) {
				LocalDateTime datetime = ((LocalDateTime) value);
				if (datetime.getHour() == 0 && datetime.getMinute() == 0 && datetime.getSecond() == 0
						&& datetime.getNano() == 0) {
					return Restrictions.gt(name, datetime.withHour(23).withMinute(59).withSecond(59).withNano(99999));
				}
			}
			return Restrictions.gt(name, value);
		}
	},
	GE(1) {
		@Override
		boolean supports(Type type) {
			return LT.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.ge(name, values[0]);
		}
	},
	BETWEEN(2) {
		@Override
		boolean supports(Type type) {
			if (!(type instanceof Class))
				return false;
			Class<?> clazz = (Class<?>) type;
			return clazz == String.class || Number.class.isAssignableFrom(clazz)
					|| Number.class.isAssignableFrom(ClassUtils.primitiveToWrapper(clazz))
					|| Date.class.isAssignableFrom(clazz) || Temporal.class.isAssignableFrom(clazz);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			Object value1 = values[0];
			Object value2 = values[1];
			if (value2 instanceof Date && DateUtils.isBeginOfDay((Date) value2))
				value2 = DateUtils.endOfDay((Date) value2);
			else if (value2 instanceof LocalDateTime) {
				LocalDateTime datetime = ((LocalDateTime) value2);
				if (datetime.getHour() == 0 && datetime.getMinute() == 0 && datetime.getSecond() == 0
						&& datetime.getNano() == 0) {
					datetime = datetime.withHour(23).withMinute(59).withSecond(59).withNano(99999);
					value2 = datetime;
				}
			}
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
		boolean supports(Type type) {
			return BETWEEN.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			Object value1 = values[0];
			Object value2 = values[1];
			if (value2 instanceof Date && DateUtils.isBeginOfDay((Date) value2))
				value2 = DateUtils.endOfDay((Date) value2);
			else if (value2 instanceof LocalDateTime) {
				LocalDateTime datetime = ((LocalDateTime) value2);
				if (datetime.getHour() == 0 && datetime.getMinute() == 0 && datetime.getSecond() == 0
						&& datetime.getNano() == 0) {
					datetime = datetime.withHour(23).withMinute(59).withSecond(59).withNano(99999);
					value2 = datetime;
				}
			}
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
		boolean supports(Type type) {
			if (!(type instanceof Class))
				return false;
			Class<?> clazz = (Class<?>) type;
			return !clazz.isPrimitive() && clazz.getAnnotation(Embeddable.class) == null;

		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.isNotNull(name);
		}
	},
	ISNULL(0) {
		@Override
		boolean supports(Type type) {
			return ISNOTNULL.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.isNull(name);
		}
	},
	ISNOTEMPTY(0) {
		@Override
		boolean supports(Type type) {
			return type == String.class;
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.not(Restrictions.or(Restrictions.isNull(name), Restrictions.eq(name, "")));
		}
	},
	ISEMPTY(0) {

		@Override
		boolean supports(Type type) {
			return ISNOTEMPTY.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.or(Restrictions.isNull(name), Restrictions.eq(name, ""));
		}
	},
	ISTRUE(0) {
		@Override
		boolean supports(Type type) {
			return type == boolean.class || type == Boolean.class;
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.eq(name, true);
		}
	},
	ISFALSE(0) {
		@Override
		boolean supports(Type type) {
			return ISTRUE.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.eq(name, false);
		}
	},
	IN(-1) {
		@Override
		boolean supports(Type type) {
			return type instanceof Class && ((Class<?>) type).isEnum();
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.in(name, values);
		}
	},
	NOTIN(-1) {
		@Override
		boolean supports(Type type) {
			return IN.supports(type);
		}

		@Override
		public Criterion operator(String name, Type type, Object... values) {
			return Restrictions.not(Restrictions.in(name, values));
		}
	};

	private int parametersSize;

	private CriterionOperator(int parametersSize) {
		this.parametersSize = parametersSize;
	}

	public abstract Criterion operator(String name, Type type, Object... values);

	public int getParametersSize() {
		return parametersSize;
	}

	public boolean isEffective(Type type, String... values) {
		if (!supports(type))
			return false;
		int size = getParametersSize();
		if (size == 0) {
			return values.length == 0;
		} else {
			if (size > 0 && values.length != size)
				return false;
			boolean allBlank = true;
			for (String s : values)
				if (StringUtils.isNotBlank(s)) {
					allBlank = false;
					break;
				}
			return !allBlank;
		}
	}

	abstract boolean supports(Type type);

	public static List<String> getSupportedOperators(Type type) {
		if (type == null)
			return Collections.emptyList();
		List<String> list = new ArrayList<>();
		for (CriterionOperator op : values())
			if (op.supports(type))
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
