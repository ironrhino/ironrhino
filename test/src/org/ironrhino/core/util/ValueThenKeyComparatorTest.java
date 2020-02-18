package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ValueThenKeyComparatorTest {

	@Test
	public void test() {
		Map<String, Integer> map = new HashMap<>();
		map.put("a", 1);
		map.put("b", 1);
		map.put("c", -1);
		map.put("d", 0);
		List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
		list.sort(ValueThenKeyComparator.<String, Integer>getDefaultInstance());
		assertThat(list.get(0).getKey(), equalTo("c"));
		assertThat(list.get(0).getValue(), equalTo(-1));
		assertThat(list.get(1).getKey(), equalTo("d"));
		assertThat(list.get(1).getValue(), equalTo(0));
		assertThat(list.get(2).getKey(), equalTo("a"));
		assertThat(list.get(2).getValue(), equalTo(1));
		assertThat(list.get(3).getKey(), equalTo("b"));
		assertThat(list.get(3).getValue(), equalTo(1));
	}

}
