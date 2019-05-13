package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Map;

import org.ironrhino.core.model.Displayable;
import org.junit.Test;

public class EnumUtilsTest {

	public static enum Enum1 implements Displayable {
		TEST1, TEST2;
	}

	public static enum Enum2 {
		TEST1, TEST2;
	}

	@Test
	public void testEnumToMap() {
		Map<String, String> map = EnumUtils.enumToMap(Enum1.class);
		assertThat(map.size(), equalTo(2));
		assertThat(map.get("TEST1"), equalTo("TEST1"));
		map = EnumUtils.enumToMap(Enum2.class);
		assertThat(map.size(), equalTo(2));
		assertThat(map.get("TEST1"), equalTo("TEST1"));
	}

	@Test
	public void testEnumToList() {
		List<String> list = EnumUtils.enumToList(Enum1.class);
		assertThat(list.size(), equalTo(2));
		assertThat(list.get(0), equalTo("TEST1"));
		list = EnumUtils.enumToList(Enum2.class);
		assertThat(list.size(), equalTo(2));
		assertThat(list.get(0), equalTo("TEST1"));
	}

}
