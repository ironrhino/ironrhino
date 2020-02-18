package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class FileUtilsTest {

	@Test
	public void test() {
		assertThat(FileUtils.normalizePath("/"), equalTo("/"));
		assertThat(FileUtils.normalizePath("test"), equalTo("test"));
		assertThat(FileUtils.normalizePath("/test"), equalTo("/test"));
		assertThat(FileUtils.normalizePath("/test/test"), equalTo("/test/test"));
		assertThat(FileUtils.normalizePath("test/test"), equalTo("test/test"));
		assertThat(FileUtils.normalizePath("/test//test"), equalTo("/test/test"));
		assertThat(FileUtils.normalizePath("//test//test"), equalTo("/test/test"));
		assertThat(FileUtils.normalizePath("./test"), equalTo("test"));
		assertThat(FileUtils.normalizePath("//test/./test"), equalTo("/test/test"));
		assertThat(FileUtils.normalizePath("./test/./test"), equalTo("test/test"));
		assertThat(FileUtils.normalizePath("../test"), equalTo("/test"));
		assertThat(FileUtils.normalizePath("//test/../test"), equalTo("/test"));
		assertThat(FileUtils.normalizePath("../test/../test"), equalTo("/test"));
	}

}
