package org.ironrhino.core.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;
import org.ironrhino.core.metadata.DoubleCheck;
import org.ironrhino.core.servlet.RequestContext;
import org.springframework.security.core.userdetails.UserDetails;

public class DoubleCheckerGeneration implements AnnotationValueGeneration<DoubleChecker> {

	private static final long serialVersionUID = -4668805161430584880L;

	private ValueGenerator<?> generator;

	@Override
	public void initialize(DoubleChecker annotation, Class<?> propertyType) {
		if (UserDetails.class.isAssignableFrom(propertyType)) {
			generator = new ValueGenerator<UserDetails>() {
				@Override
				public UserDetails generateValue(Session session, Object obj) {
					try {
						return (UserDetails) RequestContext.getRequest()
								.getAttribute(DoubleCheck.ATTRIBUTE_NAME_DOUBLE_CHECKER);
					} catch (Exception e) {
						return null;
					}
				}
			};
		} else if (String.class == propertyType) {
			generator = new ValueGenerator<String>() {
				@Override
				public String generateValue(Session session, Object obj) {
					try {
						return ((UserDetails) RequestContext.getRequest()
								.getAttribute(DoubleCheck.ATTRIBUTE_NAME_DOUBLE_CHECKER)).getUsername();
					} catch (Exception e) {
						return null;
					}
				}
			};
		} else {
			throw new HibernateException("Unsupported property type for generator annotation @DoubleChecker");
		}
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.ALWAYS;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return generator;
	}

	@Override
	public boolean referenceColumnInSql() {
		return false;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return null;
	}

}
