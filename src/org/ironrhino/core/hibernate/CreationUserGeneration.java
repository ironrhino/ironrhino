package org.ironrhino.core.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class CreationUserGeneration implements AnnotationValueGeneration<CreationUser> {

	private static final long serialVersionUID = -4668805161430584880L;

	private ValueGenerator<?> generator;

	@Override
	public void initialize(CreationUser annotation, Class<?> propertyType) {
		if (UserDetails.class.isAssignableFrom(propertyType)) {
			generator = (session, obj) -> {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				return auth != null && propertyType.isInstance(auth.getPrincipal()) ? auth.getPrincipal() : null;
			};
		} else if (String.class == propertyType) {
			generator = (session, obj) -> {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				return auth != null ? auth.getName() : null;
			};
		} else {
			throw new HibernateException("Unsupported property type for generator annotation @CreationUser");
		}
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.INSERT;
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
