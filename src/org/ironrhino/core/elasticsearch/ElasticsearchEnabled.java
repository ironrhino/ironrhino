package org.ironrhino.core.elasticsearch;

import static org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional.ANY;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ApplicationContextPropertiesConditional(key = Constants.KEY_ELASTICSEARCH_URL, value = ANY)
public @interface ElasticsearchEnabled {

}
