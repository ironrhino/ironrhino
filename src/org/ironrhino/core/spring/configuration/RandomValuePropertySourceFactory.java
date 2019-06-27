package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

public class RandomValuePropertySourceFactory implements PropertySourceFactory {

	public static final String RANDOM_PROPERTY_SOURCE_NAME = "random";

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
		return new RandomValuePropertySource(name);
	}

	/**
	 * {@link PropertySource} that returns a random value for any property that
	 * starts with {@literal "random."}. Where the "unqualified property name" is
	 * the portion of the requested property name beyond the "random." prefix, this
	 * {@link PropertySource} returns:
	 * <ul>
	 * <li>When {@literal "int"}, a random {@link Integer} value, restricted by an
	 * optionally specified range.</li>
	 * <li>When {@literal "long"}, a random {@link Long} value, restricted by an
	 * optionally specified range.</li>
	 * <li>Otherwise, a {@code byte[]}.</li>
	 * </ul>
	 * The {@literal "random.int"} and {@literal "random.long"} properties supports
	 * a range suffix whose syntax is:
	 * <p>
	 * {@code OPEN value (,max) CLOSE} where the {@code OPEN,CLOSE} are any
	 * character and {@code value,max} are integers. If {@code max} is provided then
	 * {@code value} is the minimum value and {@code max} is the maximum
	 * (exclusive).
	 *
	 * @author Dave Syer
	 * @author Matt Benson
	 */
	static class RandomValuePropertySource extends PropertySource<Random> {

		private static final String PREFIX = "random.";

		public RandomValuePropertySource(String name) {
			super(name, new Random());
		}

		@Override
		public Object getProperty(String name) {
			if (!name.startsWith(PREFIX)) {
				return null;
			}
			return getRandomValue(name.substring(PREFIX.length()));
		}

		private Object getRandomValue(String type) {
			if (type.equals("int")) {
				return getSource().nextInt();
			}
			if (type.equals("long")) {
				return getSource().nextLong();
			}
			String range = getRange(type, "int");
			if (range != null) {
				return getNextIntInRange(range);
			}
			range = getRange(type, "long");
			if (range != null) {
				return getNextLongInRange(range);
			}
			if (type.equals("uuid")) {
				return UUID.randomUUID().toString();
			}
			return getRandomBytes();
		}

		private String getRange(String type, String prefix) {
			if (type.startsWith(prefix)) {
				int startIndex = prefix.length() + 1;
				if (type.length() > startIndex) {
					return type.substring(startIndex, type.length() - 1);
				}
			}
			return null;
		}

		private int getNextIntInRange(String range) {
			String[] tokens = StringUtils.commaDelimitedListToStringArray(range);
			int start = Integer.parseInt(tokens[0]);
			if (tokens.length == 1) {
				return getSource().nextInt(start);
			}
			return start + getSource().nextInt(Integer.parseInt(tokens[1]) - start);
		}

		private long getNextLongInRange(String range) {
			String[] tokens = StringUtils.commaDelimitedListToStringArray(range);
			if (tokens.length == 1) {
				return Math.abs(getSource().nextLong() % Long.parseLong(tokens[0]));
			}
			long lowerBound = Long.parseLong(tokens[0]);
			long upperBound = Long.parseLong(tokens[1]) - lowerBound;
			return lowerBound + Math.abs(getSource().nextLong() % upperBound);
		}

		private Object getRandomBytes() {
			byte[] bytes = new byte[32];
			getSource().nextBytes(bytes);
			return DigestUtils.md5DigestAsHex(bytes);
		}

	}

}