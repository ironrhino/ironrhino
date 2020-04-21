package org.ironrhino.core.elasticsearch.index;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mappings {

	private Map<String, Field> properties;

}
