package org.ironrhino.core.exception;

import java.math.BigDecimal;

import org.junit.Test;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExceptionCreatorTest {

	@Test
	public void throwBusinessException() {
		boolean thrown = false;
		try {
			BusinessExceptions.INSTANCE.throwUnsatisfiedBalance("800010001", new BigDecimal("23.23"),
					new BigDecimal("34.34"));
		} catch (BusinessException exception) {
			thrown = true;
			assertThat(exception.getCode(), equalTo("XXX-101-F-0001"));
			assertThat(exception.getMessage(), containsString("800010001"));
			assertThat(exception.getMessage(), containsString("23.23"));
			assertThat(exception.getMessage(), containsString("34.34"));
		}
		assertThat(thrown, is(true));
	}

	@Test
	public void createBusinessException() {
		BusinessException exception = BusinessExceptions.INSTANCE.createUnsatisfiedBalance("800010001",
				new BigDecimal("23.23"), new BigDecimal("34.34"));
		assertThat(exception.getCode(), equalTo("XXX-101-F-0001"));
		assertThat(exception.getMessage(), containsString("800010001"));
		assertThat(exception.getMessage(), containsString("23.23"));
		assertThat(exception.getMessage(), containsString("34.34"));
	}

	@Test
	public void throwCustomException() {
		boolean thrown = false;
		try {
			CustomExceptions.INSTANCE.throwUnsatisfiedBalance("800010001", new BigDecimal("23.23"),
					new BigDecimal("34.34"));
		} catch (CustomException exception) {
			thrown = true;
			assertThat(exception.getCode(), equalTo("XXX-101-F-00001"));
			assertThat(exception.getMessage(), containsString("800010001"));
			assertThat(exception.getMessage(), containsString("23.23"));
			assertThat(exception.getMessage(), containsString("34.34"));
		}
		assertThat(thrown, is(true));
	}

	@Test
	public void createCustomException() {
		CustomException exception = CustomExceptions.INSTANCE.createUnsatisfiedBalance("800010001",
				new BigDecimal("23.23"), new BigDecimal("34.34"));
		assertThat(exception.getCode(), equalTo("XXX-101-F-00001"));
		assertThat(exception.getMessage(), containsString("800010001"));
		assertThat(exception.getMessage(), containsString("23.23"));
		assertThat(exception.getMessage(), containsString("34.34"));
	}

	@ExceptionCreator(project = "XXX", module = "101")
	public interface BusinessExceptions {

		public static final BusinessExceptions INSTANCE = ExceptionCreators.get(BusinessExceptions.class);

		@ExceptionDetail(message = "The balance of your account [%s] is %.2f but required %.2f", type = "F", id = 1)
		public void throwUnsatisfiedBalance(String accountNo, BigDecimal balance, BigDecimal required)
				throws BusinessException;

		@ExceptionDetail(message = "The balance of your account [%s] is %.2f but required %.2f", type = "F", id = 1)
		public BusinessException createUnsatisfiedBalance(String accountNo, BigDecimal balance, BigDecimal required);

	}

	@ExceptionCreator(project = "XXX", module = "101", type = CustomException.class, length = 5)
	public interface CustomExceptions {

		public static final CustomExceptions INSTANCE = ExceptionCreators.get(CustomExceptions.class);

		@ExceptionDetail(message = "The balance of your account [%s] is %.2f but required %.2f", type = "F", id = 1)
		public void throwUnsatisfiedBalance(String accountNo, BigDecimal balance, BigDecimal required)
				throws CustomException;

		@ExceptionDetail(message = "The balance of your account [%s] is %.2f but required %.2f", type = "F", id = 1)
		public CustomException createUnsatisfiedBalance(String accountNo, BigDecimal balance, BigDecimal required);

	}

	@RequiredArgsConstructor
	@ToString
	public static class CustomException extends RuntimeException {

		private static final long serialVersionUID = 0L;

		@Getter
		private final String code;

		@Getter
		private final String message;

	}
}
