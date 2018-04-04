package org.ironrhino.core.hibernate.type;

import java.time.YearMonth;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.LiteralType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

public class YearMonthType extends AbstractSingleColumnStandardBasicType<YearMonth> implements LiteralType<YearMonth> {

	private static final long serialVersionUID = 5461098556102029524L;

	public static final YearMonthType INSTANCE = new YearMonthType();

	public YearMonthType() {
		super(VarcharTypeDescriptor.INSTANCE, YearMonthJavaTypeDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return YearMonth.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(YearMonth value, Dialect dialect) throws Exception {
		return String.valueOf(value);
	}
}