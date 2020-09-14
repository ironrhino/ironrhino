package org.ironrhino.core.tracing;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TracingTransactionManager implements PlatformTransactionManager {

	private final PlatformTransactionManager underlying;

	@Override
	public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
		return underlying.getTransaction(transactionDefinition);
	}

	@Override
	public void commit(TransactionStatus transactionStatus) throws TransactionException {
		Tracing.executeCheckedRunnable("transactionManager.commit", () -> underlying.commit(transactionStatus),
				"component", "tx");
	}

	@Override
	public void rollback(TransactionStatus transactionStatus) throws TransactionException {
		Tracing.executeCheckedRunnable("transactionManager.rollback", () -> underlying.rollback(transactionStatus),
				"component", "tx");
	}

}
