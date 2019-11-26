package org.ironrhino.sample.batch;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

public class MessagePartitioner implements Partitioner {

	private static final String PARTITION_KEY = "partition";

	private static final String MOD_KEY = "mod";

	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		Map<String, ExecutionContext> map = new HashMap<>(gridSize);
		for (int i = 0; i < gridSize; i++) {
			ExecutionContext ctx = new ExecutionContext();
			ctx.putInt(MOD_KEY, i);
			map.put(PARTITION_KEY + i, ctx);
		}
		return map;
	}

}
