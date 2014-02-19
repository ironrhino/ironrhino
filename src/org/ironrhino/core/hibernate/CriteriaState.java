package org.ironrhino.core.hibernate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CriteriaState implements Serializable {

	private static final long serialVersionUID = 5124542493138454854L;

	private Map<String, String> aliases = new HashMap<String, String>(4);

	private Map<String, Boolean> orderings = new LinkedHashMap<String, Boolean>(
			4);

	private Set<String> criteria = new HashSet<String>();

	public Map<String, String> getAliases() {
		return aliases;
	}

	public Map<String, Boolean> getOrderings() {
		return orderings;
	}

	public Set<String> getCriteria() {
		return criteria;
	}

}
