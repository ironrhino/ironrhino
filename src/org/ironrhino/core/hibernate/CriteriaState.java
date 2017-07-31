package org.ironrhino.core.hibernate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

@Getter
public class CriteriaState implements Serializable {

	private static final long serialVersionUID = 5124542493138454854L;

	private Map<String, String> aliases = new HashMap<>(4);

	private Map<String, Boolean> orderings = new LinkedHashMap<>(4);

	private Set<String> criteria = new HashSet<>();

}
