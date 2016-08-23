package org.ironrhino.core.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.security.core.userdetails.UserDetails;

public class UpdateUserGeneration implements AnnotationValueGeneration<UpdateUser> {

	private static final long serialVersionUID = -4668805161430584880L;

	private ValueGenerator<?> generator;

	@Override
	public void initialize(UpdateUser annotation, Class<?> propertyType) {
		if (UserDetails.class.isAssignableFrom(propertyType)) {
			generator = new ValueGenerator<UserDetails>() {
				@SuppressWarnings("unchecked")
				@Override
				public UserDetails generateValue(Session session, Object obj) {
					return AuthzUtils.getUserDetails((Class<? extends UserDetails>) propertyType);
				}
			};
		} else if (String.class == propertyType) {
			generator = new ValueGenerator<String>() {
				@Override
				public String generateValue(Session session, Object obj) {
					return AuthzUtils.getUsername();
				}
			};
		} else {
			throw new HibernateException("Unsupported property type for generator annotation @UpdateUser");
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
