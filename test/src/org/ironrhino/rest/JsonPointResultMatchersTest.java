package org.ironrhino.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.rest.MockMvcResultMatchers.jsonPoint;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

public class JsonPointResultMatchersTest {

	@Test
	public void test() throws Exception {
		MvcResult mvcResult = mock(MvcResult.class);
		MockHttpServletResponse response = mock(MockHttpServletResponse.class);
		given(mvcResult.getResponse()).willReturn(response);

		given(response.getContentAsString()).willReturn(
				"{\"i\":100, \"pi\":3.1415, \"str\":\"str\", \"arr\":[1, 3.1415, \"str\"], \"map\":{\"i\":100, \"pi\":3.1415, \"str\":\"str\"}}");
		jsonPoint("/i").value(100).match(mvcResult);
		jsonPoint("/pi").value(3.1415F).match(mvcResult);
		jsonPoint("/pi").value(3.1415D).match(mvcResult);
		jsonPoint("/str").value("str").match(mvcResult);
		jsonPoint("/arr").value(new Object[] { 1, 3.1415, "str" }).match(mvcResult);
		jsonPoint("/arr").value(Arrays.asList(1, 3.1415, "str")).match(mvcResult);
		jsonPoint("/arr/1").value(3.1415D).match(mvcResult);
		jsonPoint("/arr/2").value("str").match(mvcResult);

		Map<String, Object> map = new HashMap<>();
		map.put("i", 100);
		map.put("pi", 3.1415);
		map.put("str", "str");
		jsonPoint("/map").value(map).match(mvcResult);

		jsonPoint("/notExist").value(null).match(mvcResult);
		Error actual = null;
		try {
			jsonPoint("/notExist").value("").match(mvcResult);
		} catch (AssertionError e) {
			actual = e;
		}
		assertThat(actual, is(notNullValue()));
	}
}
