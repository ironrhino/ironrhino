package org.ironrhino.core.hibernate;

import org.springframework.stereotype.Component;

@Component
@HibernateEnabled
public class OpenSessionInViewFilter extends org.springframework.orm.hibernate5.support.OpenSessionInViewFilter {

}
