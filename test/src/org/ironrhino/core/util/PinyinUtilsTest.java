package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinyinUtilsTest {

	@Test
	public void testMatchesAutocomplete() {
		assertTrue(PinyinUtils.matchesAutocomplete("中华人民共和国", "zhrmghg"));
		assertTrue(PinyinUtils.matchesAutocomplete("中华人民共和国", "zhrm"));
		assertFalse(PinyinUtils.matchesAutocomplete("中华人民共和国", "zzhrm"));
	}

	@Test
	public void testPinyin() {
		assertEquals("zhonghuarenmingongheguo", PinyinUtils.pinyin("中华人民共和国"));
	}

	@Test
	public void testPinyinAbbr() {
		assertEquals("zhrmghg", PinyinUtils.pinyinAbbr("中华人民共和国"));
	}

}
