package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.ironrhino.core.model.Displayable;
import org.junit.Test;

public class EnumUtilsTest {

	public static enum Enum1 implements Displayable {
		TEST1, TEST2;

		@Override
		public String getName() {
			return name();
		}
	}

	public static enum Enum2 {
		TEST1, TEST2;
	}

	@Test
	public void testEnumToMap() {
		Map<String, String> map = EnumUtils.enumToMap(Enum1.class);
		assertEquals(2, map.size());
		assertEquals("TEST1", map.get("TEST1"));
		map = EnumUtils.enumToMap(Enum2.class);
		assertEquals(2, map.size());
		assertEquals("TEST1", map.get("TEST1"));
	}

	@Test
	public void testEnumToList() {
		List<String> list = EnumUtils.enumToList(Enum1.class);
		assertEquals(2, list.size());
		assertEquals("TEST1", list.get(0));
		list = EnumUtils.enumToList(Enum2.class);
		assertEquals(2, list.size());
		assertEquals("TEST1", list.get(0));
	}

}
