package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class PinyinUtilsTest {

	@Test
	public void testMatchesAutocomplete() {
		assertThat(PinyinUtils.matchesAutocomplete("中华人民共和国", "zhrmghg"), equalTo(true));
		assertThat(PinyinUtils.matchesAutocomplete("中华人民共和国", "zhrm"), equalTo(true));
		assertThat(PinyinUtils.matchesAutocomplete("中华人民共和国", "zzhrm"), equalTo(false));
	}

	@Test
	public void testPinyin() {
		assertThat(PinyinUtils.pinyin("中华人民共和国"), equalTo("zhonghuarenmingongheguo"));
	}

	@Test
	public void testPinyinAbbr() {
		assertThat(PinyinUtils.pinyinAbbr("中华人民共和国"), equalTo("zhrmghg"));
	}

}
