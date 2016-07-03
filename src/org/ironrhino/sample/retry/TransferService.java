package org.ironrhino.sample.retry;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferService {

	private AtomicInteger count = new AtomicInteger(0);

	@Retryable(include = ConcurrencyFailureException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, maxDelay = 5000, multiplier = 2))
	@Transactional
	public void transfer(String fromAccountNo, String toAccountNo, BigDecimal amount) {
		int i = count.addAndGet(1);
		if (i % 5 != 0)
			throw new OptimisticLockingFailureException("data already changed");
	}

}
