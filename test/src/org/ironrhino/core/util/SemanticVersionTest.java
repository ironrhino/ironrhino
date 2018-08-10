package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SemanticVersionTest {

	@Test
	public void test() {

		SemanticVersion version = new SemanticVersion("10");
		assertEquals(10, version.getMajor());
		assertEquals(0, version.getMinor());
		assertNull(version.getPatch());

		version = new SemanticVersion("10.3");
		assertEquals(10, version.getMajor());
		assertEquals(3, version.getMinor());
		assertNull(version.getPatch());

		version = new SemanticVersion("10.3.1");
		assertEquals(10, version.getMajor());
		assertEquals(3, version.getMinor());
		assertEquals(Integer.valueOf(1), version.getPatch());

		version = new SemanticVersion("10.3.1-RC4");
		assertEquals(10, version.getMajor());
		assertEquals(3, version.getMinor());
		assertEquals(Integer.valueOf(1), version.getPatch());
		assertEquals("RC4", version.getPrerelease());

		version = new SemanticVersion("10.3.1-RC4+b2");
		assertEquals(10, version.getMajor());
		assertEquals(3, version.getMinor());
		assertEquals(Integer.valueOf(1), version.getPatch());
		assertEquals("RC4", version.getPrerelease());
		assertEquals("b2", version.getBuild());

	}

	@Test
	public void testCompare() {
		assertTrue(new SemanticVersion("10").equals(new SemanticVersion("10.0")));
		assertTrue(new SemanticVersion("10.3.1-RC4").equals(new SemanticVersion("10.3.1-RC4")));
		assertTrue(new SemanticVersion("10").compareTo(new SemanticVersion("10.1")) < 0);
		assertTrue(new SemanticVersion("10.0.2").compareTo(new SemanticVersion("10.0.10")) < 0);
		assertTrue(new SemanticVersion("10.1.2").compareTo(new SemanticVersion("10.0.10")) > 0);
	}

}
