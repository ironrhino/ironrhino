package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class RequestUtilsTest {

	@Test
	public void testIsSameOrigin() {
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "/"), equalTo(true));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "/test2"), equalTo(true));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "http://www.test.com/test2"), equalTo(true));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "https://www.test.com/test2"), equalTo(true));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "//www.test.com/test2"), equalTo(true));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "https://www.test2.com/test2"),
				equalTo(false));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "www.test2.com"), equalTo(false));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "http://@www.test2.com"), equalTo(false));
		assertThat(RequestUtils.isSameOrigin("http://www.test.com/test", "http://www.test.com@www.test2.com"),
				equalTo(false));
		assertThat(RequestUtils.isSameOrigin("www.test.com/test", "www.test.com/test2"), equalTo(false));
	}

}
