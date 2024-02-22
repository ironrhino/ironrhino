package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;

public class YamlPropertySourceFactoryTest {

	@Test
	public void test() throws Exception {
		String yaml = "test: value\nfoo:\n  bar: foobar";
		PropertySource<?> ps = createPropertySource(yaml);
		assertThat(ps.getProperty("test"), is("value"));
		assertThat(ps.getProperty("foo.bar"), is("foobar"));
	}

	private PropertySource<?> createPropertySource(String input) throws Exception {
		return new YamlPropertySourceFactory().createPropertySource("test",
				new EncodedResource(new ByteArrayResource(input.getBytes()), StandardCharsets.UTF_8));
	}

}