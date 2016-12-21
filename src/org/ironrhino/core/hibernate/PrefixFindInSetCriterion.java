package org.ironrhino.core.hibernate;

import java.util.Arrays;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.StringType;

public class PrefixFindInSetCriterion implements Criterion {

	private static final long serialVersionUID = 1447679780985973530L;

	private final String propertyName;
	private final String value;

	public PrefixFindInSetCriterion(String propertyName, String value) {
		this.propertyName = propertyName;
		this.value = value;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		String column = criteriaQuery.findColumns(propertyName, criteria)[0];
		Dialect dialect = criteriaQuery.getFactory().getServiceRegistry().getService(JdbcServices.class).getDialect();
		return dialect.getFunctions().get("concat").render(StringType.INSTANCE, Arrays.asList("','", column),
				criteriaQuery.getFactory()) + " like ?";
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		return new TypedValue[] { new TypedValue(StringType.INSTANCE, "%," + value + "%") };
	}

}
