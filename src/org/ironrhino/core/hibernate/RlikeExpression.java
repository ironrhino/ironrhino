package org.ironrhino.core.hibernate;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.TypedValue;

public class RlikeExpression implements Criterion {

	private static final long serialVersionUID = -6248943814593341122L;

	private final String propertyName;
	private final String value;

	public RlikeExpression(String propertyName, String value) {
		this.propertyName = propertyName;
		this.value = value;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		if (columns.length != 1)
			throw new HibernateException("rlike may only be used with single-column properties");
		String column = columns[0];
		Dialect dialect = criteriaQuery.getFactory().getServiceRegistry().getService(JdbcServices.class).getDialect();
		if (dialect instanceof MySQLDialect)
			return column + " rlike ?";
		else if (dialect instanceof PostgreSQL81Dialect)
			return column + " ~* ?";
		else if (dialect instanceof Oracle8iDialect || dialect instanceof DB2Dialect)
			return " regexp_like (" + column + ", ?)";
		else if (dialect instanceof H2Dialect)
			return column + " regexp ?";
		else
			throw new HibernateException(
					"rlike is not supported with the configured dialect " + dialect.getClass().getCanonicalName());
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] { criteriaQuery.getTypedValue(criteria, propertyName, value) };
	}

	@Override
	public String toString() {
		return propertyName + " rlike '" + value + "'";
	}
}