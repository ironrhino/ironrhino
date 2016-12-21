package org.ironrhino.core.hibernate;

import org.hibernate.SessionFactory;
import org.ironrhino.core.spring.configuration.BeanPresentConditional;
import org.springframework.stereotype.Component;

@Component
@BeanPresentConditional(type = SessionFactory.class)
public class OpenSessionInViewFilter extends org.springframework.orm.hibernate5.support.OpenSessionInViewFilter {

}
