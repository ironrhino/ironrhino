package org.ironrhino.core.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.security.core.userdetails.UserDetails;

public class DoubleCheckerGeneration implements AnnotationValueGeneration<DoubleChecker> {

	private static final long serialVersionUID = -4668805161430584880L;

	private ValueGenerator<?> generator;

	@Override
	public void initialize(DoubleChecker annotation, Class<?> propertyType) {
		if (UserDetails.class.isAssignableFrom(propertyType)) {
			generator = new ValueGenerator<UserDetails>() {
				@SuppressWarnings("unchecked")
				@Override
				public UserDetails generateValue(Session session, Object obj) {
					return AuthzUtils.getDoubleChecker((Class<? extends UserDetails>) propertyType);
				}
			};
		} else if (String.class == propertyType) {
			generator = new ValueGenerator<String>() {
				@Override
				public String generateValue(Session session, Object obj) {
					UserDetails ud = AuthzUtils.getDoubleChecker(UserDetails.class);
					return ud != null ? ud.getUsername() : null;
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
