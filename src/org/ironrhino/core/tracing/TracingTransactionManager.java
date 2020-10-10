package org.ironrhino.core.tracing;

import java.io.Serializable;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TracingTransactionManager implements PlatformTransactionManager {

	private static final Serializable[] TAGS = new String[] { "component", "tx" };

	private final PlatformTransactionManager underlying;

	@Override
	public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
		return Tracing.executeCheckedCallable("transactionManager.getTransaction",
				() -> underlying.getTransaction(transactionDefinition), TAGS);
	}

	@Override
	public void commit(TransactionStatus transactionStatus) throws TransactionException {
		Tracing.executeCheckedRunnable("transactionManager.commit", () -> underlying.commit(transactionStatus), TAGS);
	}

	@Override
	public void rollback(TransactionStatus transactionStatus) throws TransactionException {
		Tracing.executeCheckedRunnable("transactionManager.rollback", () -> underlying.rollback(transactionStatus),
				TAGS);
	}

}
