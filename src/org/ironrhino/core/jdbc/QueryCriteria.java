package org.ironrhino.core.jdbc;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryCriteria implements Serializable {

	private static final long serialVersionUID = -2581363035277418165L;

	private String query;

	private Map<String, Object> parameters;

}
