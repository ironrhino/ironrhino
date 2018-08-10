package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.ironrhino.core.metadata.Scope;
import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void testToCamelCase() {
		assertEquals("fooBar", StringUtils.toCamelCase("foo_bar"));
	}

	@Test
	public void testToUpperCaseWithUnderscores() {
		assertEquals("FOO_BAR", StringUtils.toUpperCaseWithUnderscores("fooBar"));
	}

	@Test
	public void testToLowerCaseWithUnderscores() {
		assertEquals("foo_bar", StringUtils.toLowerCaseWithUnderscores("fooBar"));
	}

	@Test
	public void testMatchesWildcard() {
		assertTrue(StringUtils.matchesWildcard("test", "t?st"));
		assertTrue(StringUtils.matchesWildcard("test", "t*"));
		assertFalse(StringUtils.matchesWildcard("test", "t?"));
		assertFalse(StringUtils.matchesWildcard("test", "T*"));
	}

	@Test
	public void testTrimTail() {
		assertEquals("fooB", StringUtils.trimTail("fooBar", "ar"));
		assertEquals("fooBar", StringUtils.trimTail("fooBar", "bar"));
	}

	@Test
	public void testTrimTailSlash() {
		assertEquals("foo", StringUtils.trimTailSlash("foo/"));
		assertEquals("foo", StringUtils.trimTailSlash("foo"));
	}

	@Test
	public void testCompressRepeat() {
		assertEquals("foo", StringUtils.compressRepeat("foo", "/"));
		assertEquals("foo/", StringUtils.compressRepeat("foo/", "/"));
		assertEquals("foo/", StringUtils.compressRepeat("foo////", "/"));
	}

	@Test
	public void testCompressRepeatSlash() {
		assertEquals("foo", StringUtils.compressRepeatSlash("foo"));
		assertEquals("foo/", StringUtils.compressRepeatSlash("foo/"));
		assertEquals("foo/", StringUtils.compressRepeatSlash("foo////"));
		assertEquals("fo/o/", StringUtils.compressRepeatSlash("fo///o////"));
	}

	@Test
	public void testCompressRepeatSpaces() {
		assertEquals("foo", StringUtils.compressRepeatSpaces("foo"));
		assertEquals("foo", StringUtils.compressRepeatSpaces("foo "));
		assertEquals("foo", StringUtils.compressRepeatSpaces("foo    "));
		assertEquals("fo o", StringUtils.compressRepeatSpaces("fo   o   "));
	}

	@Test
	public void testTrimLocale() {
		assertEquals("foo", StringUtils.trimLocale("foo"));
		assertEquals("foo", StringUtils.trimLocale("foo_zh"));
		assertEquals("foo", StringUtils.trimLocale("foo_zh_CN"));
	}

	@Test
	public void testToString() {
		assertEquals("GLOBAL", StringUtils.toString(Scope.GLOBAL));
		assertEquals("[1, 2, 3]", StringUtils.toString(new Integer[] { 1, 2, 3 }));
		assertEquals("foo", StringUtils.toString("foo"));
		assertEquals("1", StringUtils.toString(1));
	}

}
