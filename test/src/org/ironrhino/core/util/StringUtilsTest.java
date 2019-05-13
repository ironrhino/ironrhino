package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.ironrhino.core.metadata.Scope;
import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void testToCamelCase() {
		assertThat(StringUtils.toCamelCase("foo_bar"), equalTo("fooBar"));
	}

	@Test
	public void testToUpperCaseWithUnderscores() {
		assertThat(StringUtils.toUpperCaseWithUnderscores("fooBar"), equalTo("FOO_BAR"));
	}

	@Test
	public void testToLowerCaseWithUnderscores() {
		assertThat(StringUtils.toLowerCaseWithUnderscores("fooBar"), equalTo("foo_bar"));
	}

	@Test
	public void testMatchesWildcard() {
		assertThat(StringUtils.matchesWildcard("test", "t?st"), equalTo(true));
		assertThat(StringUtils.matchesWildcard("test", "t*"), equalTo(true));
		assertThat(StringUtils.matchesWildcard("test", "t?"), equalTo(false));
		assertThat(StringUtils.matchesWildcard("test", "T*"), equalTo(false));
	}

	@Test
	public void testTrimTail() {
		assertThat(StringUtils.trimTail("fooBar", "ar"), equalTo("fooB"));
		assertThat(StringUtils.trimTail("fooBar", "bar"), equalTo("fooBar"));
	}

	@Test
	public void testTrimTailSlash() {
		assertThat(StringUtils.trimTailSlash("foo/"), equalTo("foo"));
		assertThat(StringUtils.trimTailSlash("foo"), equalTo("foo"));
	}

	@Test
	public void testCompressRepeat() {
		assertThat(StringUtils.compressRepeat("foo", "/"), equalTo("foo"));
		assertThat(StringUtils.compressRepeat("foo/", "/"), equalTo("foo/"));
		assertThat(StringUtils.compressRepeat("foo////", "/"), equalTo("foo/"));
	}

	@Test
	public void testCompressRepeatSlash() {
		assertThat("foo", StringUtils.compressRepeatSlash("foo"), equalTo("foo"));
		assertThat(StringUtils.compressRepeatSlash("foo/"), equalTo("foo/"));
		assertThat(StringUtils.compressRepeatSlash("foo////"), equalTo("foo/"));
		assertThat(StringUtils.compressRepeatSlash("fo///o////"), equalTo("fo/o/"));
	}

	@Test
	public void testCompressRepeatSpaces() {
		assertThat(StringUtils.compressRepeatSpaces("foo"), equalTo("foo"));
		assertThat(StringUtils.compressRepeatSpaces("foo "), equalTo("foo"));
		assertThat(StringUtils.compressRepeatSpaces("foo    "), equalTo("foo"));
		assertThat(StringUtils.compressRepeatSpaces("fo   o   "), equalTo("fo o"));
	}

	@Test
	public void testTrimLocale() {
		assertThat(StringUtils.trimLocale("foo"), equalTo("foo"));
		assertThat(StringUtils.trimLocale("foo_zh"), equalTo("foo"));
		assertThat(StringUtils.trimLocale("foo_zh_CN"), equalTo("foo"));
	}

	@Test
	public void testToString() {
		assertThat(StringUtils.toString(Scope.GLOBAL), equalTo("GLOBAL"));
		assertThat(StringUtils.toString(new Integer[] { 1, 2, 3 }), equalTo("[1, 2, 3]"));
		assertThat(StringUtils.toString("foo"), equalTo("foo"));
		assertThat(StringUtils.toString(1), equalTo("1"));
	}

}
