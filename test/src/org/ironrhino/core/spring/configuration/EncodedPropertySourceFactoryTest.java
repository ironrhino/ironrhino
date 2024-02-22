package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

import org.junit.Test;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;

public class EncodedPropertySourceFactoryTest {

	@Test
	public void base64() throws Exception {
		PropertySource<?> ps = createPropertySource("foo=" + Base64.getEncoder().encodeToString("bar".getBytes()));
		assertThat(ps.getProperty("foo"), is("bar"));
	}

	@Test
	public void decoderIsDefined() throws Exception {
		PropertySource<?> ps = createPropertySource(
				EncodedPropertySourceFactory.KEY_DECODER + "=" + DummyDecoder.class.getName() + "\nfoo=bar");
		assertThat(ps.getProperty("foo"), is("dummybar"));
	}

	@Test(expected = RuntimeException.class)
	public void decoderIsNotFound() throws Exception {
		createPropertySource(EncodedPropertySourceFactory.KEY_DECODER + "=com.example.MyDecoder\nfoo="
				+ Base64.getEncoder().encodeToString("bar".getBytes()));
	}

	@Test(expected = RuntimeException.class)
	public void decoderIsInvalid() throws Exception {
		createPropertySource(EncodedPropertySourceFactory.KEY_DECODER + "=" + InvalidDecoder.class.getName() + "\nfoo="
				+ Base64.getEncoder().encodeToString("bar".getBytes()));
	}

	private PropertySource<?> createPropertySource(String input) throws Exception {
		return new EncodedPropertySourceFactory().createPropertySource("test",
				new EncodedResource(new ByteArrayResource(input.getBytes()), StandardCharsets.UTF_8));
	}

	public static class InvalidDecoder implements Function<String, Long> {

		@Override
		public Long apply(String t) {
			return 0L;
		}

	}

	public static class DummyDecoder implements Function<String, String> {

		@Override
		public String apply(String t) {
			return "dummy" + t;
		}

	}

}