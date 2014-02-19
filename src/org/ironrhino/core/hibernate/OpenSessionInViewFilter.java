package org.ironrhino.core.hibernate;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class OpenSessionInViewFilter extends
		org.springframework.orm.hibernate4.support.OpenSessionInViewFilter {

}
