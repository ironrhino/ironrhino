package org.ironrhino.core.elasticsearch.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {

	private Settings settings;

	private Mappings mappings;

}
