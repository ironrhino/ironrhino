package org.ironrhino.core.hibernate;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.dataroute.RoutingDataSource;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class OpenSessionInViewFilter extends org.springframework.orm.hibernate5.support.OpenSessionInViewFilter {

	@Autowired(required = false)
	@Qualifier("dataSource")
	private RoutingDataSource routingDataSource;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (routingDataSource == null)
			super.doFilterInternal(request, response, chain);
		else
			chain.doFilter(request, response);
	}

}
