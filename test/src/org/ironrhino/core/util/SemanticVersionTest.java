package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class SemanticVersionTest {

	@Test
	public void test() {

		SemanticVersion version = new SemanticVersion("10");
		assertThat(version.getMajor(), equalTo(10));
		assertThat(version.getMinor(), equalTo(0));
		assertThat(version.getPatch(), nullValue());

		version = new SemanticVersion("10.3");
		assertThat(version.getMajor(), equalTo(10));
		assertThat(version.getMinor(), equalTo(3));
		assertThat(version.getPatch(), nullValue());

		version = new SemanticVersion("10.3.1");
		assertThat(version.getMajor(), equalTo(10));
		assertThat(version.getMinor(), equalTo(3));
		assertThat(version.getPatch(), equalTo(1));

		version = new SemanticVersion("10.3.1-RC4");
		assertThat(version.getMajor(), equalTo(10));
		assertThat(version.getMinor(), equalTo(3));
		assertThat(version.getPatch(), equalTo(1));
		assertThat(version.getPrerelease(), equalTo("RC4"));

		version = new SemanticVersion("10.3.1-RC4+b2");
		assertThat(version.getMajor(), equalTo(10));
		assertThat(version.getMinor(), equalTo(3));
		assertThat(version.getPatch(), equalTo(1));
		assertThat(version.getPrerelease(), equalTo("RC4"));
		assertThat(version.getBuild(), equalTo("b2"));

	}

	@Test
	public void testCompare() {
		assertThat(new SemanticVersion("10").equals(new SemanticVersion("10.0")), equalTo(true));
		assertThat(new SemanticVersion("10.3.1-RC4").equals(new SemanticVersion("10.3.1-RC4")), equalTo(true));
		assertThat(new SemanticVersion("10").compareTo(new SemanticVersion("10.1")) < 0, equalTo(true));
		assertThat(new SemanticVersion("10.0.2").compareTo(new SemanticVersion("10.0.10")) < 0, equalTo(true));
		assertThat(new SemanticVersion("10.1.2").compareTo(new SemanticVersion("10.0.10")) > 0, equalTo(true));
	}

}
