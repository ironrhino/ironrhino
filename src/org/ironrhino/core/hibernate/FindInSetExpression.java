package org.ironrhino.core.hibernate;

import java.util.Arrays;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.StringType;

public class FindInSetExpression implements Criterion {

	private static final long serialVersionUID = 1447679780985973530L;

	private final String propertyName;
	private final String value;

	public FindInSetExpression(String propertyName, String value) {
		this.propertyName = propertyName;
		this.value = value;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		if (columns.length != 1)
			throw new HibernateException("find_in_set may only be used with single-column properties");
		String column = columns[0];
		Dialect dialect = criteriaQuery.getFactory().getServiceRegistry().getService(JdbcServices.class).getDialect();
		if (dialect instanceof MySQLDialect)
			return "find_in_set(?," + column + ")";
		else if (dialect instanceof PostgreSQL81Dialect)
			return "?=any(string_to_array(" + column + ",','))";
		else
			return dialect.getFunctions().get("concat").render(StringType.INSTANCE, Arrays.asList("','", column, "','"),
					criteriaQuery.getFactory()) + " like ?";
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		Dialect dialect = criteriaQuery.getFactory().getServiceRegistry().getService(JdbcServices.class).getDialect();
		TypedValue typedValue;
		if (dialect instanceof MySQLDialect || dialect instanceof PostgreSQL81Dialect)
			typedValue = new TypedValue(StringType.INSTANCE, value);
		else
			typedValue = new TypedValue(StringType.INSTANCE, "%," + value + ",%");
		return new TypedValue[] { typedValue };
	}

	@Override
	public String toString() {
		return "find_in_set('" + value + "'," + propertyName + ")";
	}

}
