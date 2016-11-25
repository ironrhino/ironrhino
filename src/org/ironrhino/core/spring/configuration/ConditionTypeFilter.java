package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

public class ConditionTypeFilter implements TypeFilter {

	public static final ConditionTypeFilter INSTANCE = new ConditionTypeFilter();

	private List<SimpleCondition<? extends Annotation>> conditions;

	private ConditionTypeFilter() {
		this.conditions = new ArrayList<>();
		conditions.add(new RunLevelCondition());
		conditions.add(new StageCondition());
		conditions.add(new ClassPresentCondition());
		conditions.add(new ResourcePresentCondition());
		conditions.add(new ApplicationContextPropertiesCondition());
		conditions.add(new AddressAvailabilityCondition());
	}

	@Override
	public boolean match(MetadataReader mr, MetadataReaderFactory mrf) throws IOException {
		AnnotationMetadata metadata = mr.getAnnotationMetadata();
		for (SimpleCondition<? extends Annotation> condition : conditions) {
			if (!condition.matches(metadata))
				return false;
		}
		return true;
	}

}
