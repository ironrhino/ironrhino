package org.ironrhino.core.elasticsearch.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Settings {

	private int number_of_shards = 1;

	private int number_of_replicas = 1;

}
