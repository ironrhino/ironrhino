package org.ironrhino.core.idempotent;

import org.ironrhino.core.metadata.Idempotent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Recover;

public class IdempotentTestEntityService extends TestEntityService {

	@Override
	@Idempotent(recover = "tryFind")
	public TestEntity save(Request request) {
		return super.save(request);
	}

	@Recover
	public TestEntity tryFind(DataIntegrityViolationException ex, Request request) {
		entityManager.setEntityClass(TestEntity.class);
		return entityManager.findOne(request.getSeqNo());
	}

}