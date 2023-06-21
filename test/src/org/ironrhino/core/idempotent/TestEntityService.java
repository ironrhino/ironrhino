package org.ironrhino.core.idempotent;

import org.ironrhino.core.service.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TestEntityService {

	@Autowired
	protected EntityManager<TestEntity> entityManager;

	@Transactional
	public TestEntity save(Request request) {
		TestEntity entity = new TestEntity();
		entity.setSeqNo(request.getSeqNo());
		entityManager.save(entity);
		return entity;
	}

}