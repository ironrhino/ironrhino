package org.ironrhino.core.spring.configuration;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

public enum PropertySourceMode {

	PREFERRED {

		@Override
		public void add(MutablePropertySources mps, PropertySource<?> ps) {
			mps.addFirst(ps);
		}

	},
	FALLBACK {

		@Override
		public void add(MutablePropertySources mps, PropertySource<?> ps) {
			mps.addLast(ps);
		}

	};

	public abstract void add(MutablePropertySources mps, PropertySource<?> ps);
}