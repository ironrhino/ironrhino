package org.ironrhino.common.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.query.Query;
import org.ironrhino.common.model.BasePollingEntity;
import org.ironrhino.common.model.PollingStatus;
import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.util.ExceptionUtils;

public abstract class BatchedPollingControl<T extends BasePollingEntity> extends AbstractPollingControl<T> {

	@SuppressWarnings("rawtypes")
	@Override
	protected void doDequeue() {
		entityManager.setEntityClass(entityClass);
		int batchLimit = getBatchLimit();
		while (true) {
			List<T> entities = new ArrayList<>();
			while (entities.size() < batchLimit) {
				String id = pop();
				if (id == null)
					break;
				if (entities.stream().map(BasePollingEntity::getId).anyMatch(s -> s.equals(id)))
					continue;
				T entity = entityManager.get(id);
				if (entity == null) {
					logger.warn("not found: {}", id);
					continue;
				}
				logger.info("dequeue {}", entity);
				if (entity.getStatus() == PollingStatus.SUCCESSFUL || entity.getStatus() == PollingStatus.FAILED) {
					logger.warn("status is {}: {}", entity.getStatus(), entity);
					continue;
				}
				if (entity.getAttempts() >= getMaxAttempts()) {
					logger.error("max attempts reached: {}", entity);
					entityManager.executeUpdate(simpleUpdateStatusHql, entity.getId(), entity.getStatus(),
							PollingStatus.FAILED, new Date(), "max attempts reached");
					continue;
				}
				entities.add(entity);
			}
			if (entities.isEmpty())
				break;
			try {
				Map<T, Result> results = Metrics.recordTimer("polling." + entityClass.getName(), () -> handle(entities),
						"batch", "true");
				for (T entity : entities) {
					Result obj = results.get(entity);
					if (obj == null) {
						logger.error("process {} failed cause updated fields is null or invalid", entity);
					}
				}
				entityManager.execute(session -> {
					results.entrySet().stream().filter(entry -> entry.getValue().getResult() != null).forEach(entry -> {
						T entity = entry.getKey();
						Map<String, Object> fields = entry.getValue().getResult();
						StringBuilder sb = new StringBuilder("update ");
						sb.append(entityClass.getSimpleName());
						sb.append(" t set t.status=?3,t.modifyDate=?4,t.errorInfo=null,t.attempts=t.attempts+1");
						int i = 5;
						for (String field : fields.keySet())
							sb.append(",t.").append(field).append("=?").append(i++);
						sb.append(" where t.id=?1 and t.status=?2");
						final String hql = sb.toString();
						Query query = session.createQuery(hql);
						query.setParameter(1, entity.getId());
						query.setParameter(2, entity.getStatus());
						query.setParameter(3, PollingStatus.SUCCESSFUL);
						query.setParameter(4, new Date());
						int index = 5;
						for (String field : fields.keySet())
							query.setParameter(index++, fields.get(field));
						int ret = query.executeUpdate();
						if (ret == 1) {
							afterUpdated(session, entity);
							logger.info("process {} successful", entity);
						} else {
							logger.warn("process {} successful but ignored", entity);
						}
					});
					return null;
				});
				entityManager.execute(session -> {
					results.entrySet().stream().filter(entry -> entry.getValue().getException() != null)
							.forEach(entry -> {
								T entity = entry.getKey();
								Exception e = entry.getValue().getException();
								logger.error(e.getMessage(), e);
								String err = ExceptionUtils.getDetailMessage(e);
								if (err.length() > 4000)
									err = err.substring(0, 4000);
								final String errorInfo = err;
								boolean retryable = isTemporaryError(e);
								entityManager.execute(s -> {
									int result;
									if (retryable) {
										result = entityManager.executeUpdate(conditionalUpdateStatusHql, entity.getId(),
												entity.getStatus(), getMaxAttempts(), PollingStatus.FAILED,
												PollingStatus.TEMPORARY_ERROR, new Date(), errorInfo);
									} else {
										result = entityManager.executeUpdate(defaultUpdateStatusHql, entity.getId(),
												entity.getStatus(), PollingStatus.FAILED, new Date(), errorInfo);
									}
									if (result == 1)
										logger.info("process {} failed", entity);
									else
										logger.warn("process {} failed but ignored", entity);
									return null;
								});
							});
					return null;
				});
			} catch (

			Exception e) {
				logger.error(e.getMessage(), e);
				String err = ExceptionUtils.getDetailMessage(e);
				if (err.length() > 4000)
					err = err.substring(0, 4000);
				final String errorInfo = err;
				boolean retryable = isTemporaryError(e);
				entityManager.execute(session -> {
					for (T entity : entities) {
						int result;
						if (retryable) {
							result = entityManager.executeUpdate(conditionalUpdateStatusHql, entity.getId(),
									entity.getStatus(), getMaxAttempts(), PollingStatus.FAILED,
									PollingStatus.TEMPORARY_ERROR, new Date(), errorInfo);
						} else {
							result = entityManager.executeUpdate(defaultUpdateStatusHql, entity.getId(),
									entity.getStatus(), PollingStatus.FAILED, new Date(), errorInfo);
						}
						if (result == 1)
							logger.info("process {} failed", entity);
						else
							logger.warn("process {} failed but ignored", entity);
					}
					return null;
				});
			}
		}
	}

	protected abstract Map<T, Result> handle(List<T> entity) throws Exception;

	protected abstract int getBatchLimit();

	public static class Result {

		private final Map<String, Object> result;

		private final Exception exception;

		private Result(Map<String, Object> result) {
			Objects.requireNonNull(result);
			this.result = result;
			this.exception = null;
		}

		private Result(Exception exception) {
			Objects.requireNonNull(exception);
			this.result = null;
			this.exception = exception;
		}

		public static Result of(Map<String, Object> result) {
			return new Result(result);
		}

		public static Result of(Exception exception) {
			return new Result(exception);
		}

		public Map<String, Object> getResult() {
			return result;
		}

		public Exception getException() {
			return exception;
		}

	}

}
