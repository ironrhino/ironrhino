package org.ironrhino.core.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.util.DateUtils;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

public enum CriterionOperator implements Displayable {

	EQ(1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz == boolean.class
					|| clazz == Boolean.class
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(String.class) || clazz.equals(Date.class)
					|| Persistable.class.isAssignableFrom(clazz)
					|| clazz.isEnum();
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			if (value1 instanceof Date && DateUtils.isBeginOfDay((Date) value1))
				return Restrictions.between(name, value1,
						DateUtils.endOfDay((Date) value1));
			else
				return Restrictions.eq(name, value1);
		}
	},
	NEQ(1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz.equals(String.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class)
					|| Persistable.class.isAssignableFrom(clazz)
					|| clazz.isEnum();
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			if (value1 instanceof Date && DateUtils.isBeginOfDay((Date) value1))
				return Restrictions.or(Restrictions.isNull(name), Restrictions
						.or(Restrictions.lt(name, value1),
								Restrictions.gt(name,
										DateUtils.endOfDay((Date) value1))));
			else
				return Restrictions.or(Restrictions.isNull(name),
						Restrictions.not(Restrictions.eq(name, value1)));
		}
	},
	LT(1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz.equals(String.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			return Restrictions.lt(name, value1);
		}
	},
	LE(1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz.equals(String.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			if (value1 instanceof Date && DateUtils.isBeginOfDay((Date) value1))
				return Restrictions.le(name, DateUtils.endOfDay((Date) value1));
			else
				return Restrictions.le(name, value1);
		}
	},
	GT(1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz.equals(String.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			if (value1 instanceof Date && DateUtils.isBeginOfDay((Date) value1))
				return Restrictions.gt(name, DateUtils.endOfDay((Date) value1));
			else
				return Restrictions.gt(name, value1);
		}
	},
	GE(1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz.equals(String.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			return Restrictions.ge(name, value1);
		}
	},
	BETWEEN(2) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz.equals(String.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class);
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return (value != null && value.length == 2)
					&& (StringUtils.isNotBlank(value[0]) || StringUtils
							.isNotBlank(value[1]));
		}

		@Override
		public Criterion operator(String name, Object... value) {
			Object value1 = null;
			Object value2 = null;
			if (value != null) {
				if (value.length > 0)
					value1 = value[0];
				if (value.length > 1)
					value2 = value[1];
			}
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
		public boolean supports(Class<?> clazz) {
			return clazz == short.class || clazz == int.class
					|| clazz == long.class || clazz == float.class
					|| clazz == double.class || clazz.equals(String.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class);
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return (value != null && value.length == 2)
					&& (StringUtils.isNotBlank(value[0]) || StringUtils
							.isNotBlank(value[1]));
		}

		@Override
		public Criterion operator(String name, Object... value) {
			Object value1 = null;
			Object value2 = null;
			if (value != null) {
				if (value.length > 0)
					value1 = value[0];
				if (value.length > 1)
					value2 = value[1];
			}
			if (value2 instanceof Date && DateUtils.isBeginOfDay((Date) value2))
				value2 = DateUtils.endOfDay((Date) value2);
			if (value1 != null && value2 != null)
				return Restrictions.or(Restrictions.lt(name, value1),
						Restrictions.gt(name, value2));
			else if (value1 != null)
				return Restrictions.lt(name, value1);
			else if (value2 != null)
				return Restrictions.gt(name, value2);
			else
				return null;
		}
	},
	ISNULL(0) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class) || clazz.equals(Boolean.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class)
					|| Persistable.class.isAssignableFrom(clazz)
					|| clazz.isEnum();
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.isNull(name);
		}
	},
	ISNOTNULL(0) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class) || clazz.equals(Boolean.class)
					|| Number.class.isAssignableFrom(clazz)
					|| clazz.equals(Date.class)
					|| Persistable.class.isAssignableFrom(clazz)
					|| clazz.isEnum();
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.isNotNull(name);
		}
	},
	ISEMPTY(0) {
		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.eq(name, "");
		}
	},
	ISNOTEMPTY(0) {
		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.not(Restrictions.eq(name, ""));
		}
	},
	ISBLANK(0) {
		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.or(Restrictions.isNull(name),
					Restrictions.eq(name, ""));
		}
	},
	ISNOTBLANK(0) {
		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.not(Restrictions.eqOrIsNull(name, ""));
		}
	},
	INCLUDE(1) {

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			return Restrictions.like(name, (String) value1, MatchMode.ANYWHERE);
		}
	},
	NOTINCLUDE(1) {

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			if (value == null || value.length == 0)
				return null;
			Object value1 = value[0];
			return Restrictions.not(Restrictions.like(name, (String) value1,
					MatchMode.ANYWHERE));
		}
	},
	ISTRUE(0) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == boolean.class || clazz.equals(Boolean.class);
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.eq(name, true);
		}
	},
	ISFALSE(0) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz == boolean.class || clazz.equals(Boolean.class);
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz);
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.eq(name, false);
		}
	},
	IN(-1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.isEnum();
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz) && value != null && value.length > 0;
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.in(name, value);
		}
	},
	NOTIN(-1) {
		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.isEnum();
		}

		@Override
		public boolean isEffective(Class<?> clazz, String... value) {
			return supports(clazz) && value != null && value.length > 0;
		}

		@Override
		public Criterion operator(String name, Object... value) {
			return Restrictions.not(Restrictions.in(name, value));
		}
	};

	private int parametersSize;

	private CriterionOperator(int parametersSize) {
		this.parametersSize = parametersSize;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public String getDisplayName() {
		try {
			return LocalizedTextUtil.findText(getClass(), name(), ActionContext
					.getContext().getLocale(), name(), null);
		} catch (Exception e) {
			return name();
		}
	}

	public static CriterionOperator parse(String name) {
		if (name != null)
			for (CriterionOperator en : values())
				if (name.equals(en.name()) || name.equals(en.getDisplayName()))
					return en;
		return null;
	}

	public abstract Criterion operator(String name, Object... value);

	public int getParametersSize() {
		return parametersSize;
	}

	public boolean isEffective(Class<?> clazz, String... value) {
		if (!supports(clazz))
			return false;
		if (value == null || value.length == 0 || StringUtils.isBlank(value[0]))
			return false;
		return true;
	}

	public abstract boolean supports(Class<?> clazz);

	public static List<String> getSupportedOperators(Class<?> clazz) {
		if (clazz == null)
			return Collections.emptyList();
		List<String> list = new ArrayList<String>();
		for (CriterionOperator op : values())
			if (op.supports(clazz)
					&& !(op == EQ && (clazz == Boolean.class || clazz == boolean.class)))
				list.add(op.name());
		return list;
	}

	public static List<String> getSupportedOperators(String className) {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(className);
		} catch (Exception e) {

		}
		return getSupportedOperators(clazz);
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

}
