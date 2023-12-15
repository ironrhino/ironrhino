package org.ironrhino.common.support;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.BasePollingEntity;
import org.ironrhino.common.model.PollingStatus;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.NameableThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractPollingControl<T extends BasePollingEntity> implements SmartLifecycle {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected EntityManager<T> entityManager;

	@Autowired
	protected StringRedisTemplate stringRedisTemplate;

	@Getter
	@Setter
	private int batchSize = 50;

	@Getter
	@Setter
	private int threads = 5;

	@Getter
	@Setter
	private int maxAttempts = 3;

	@Getter
	@Setter
	private int intervalFactorInSeconds = 60;

	@Getter
	@Setter
	private int resubmitIntervalInSeconds = 600;

	private AtomicBoolean running = new AtomicBoolean();

	private ThreadPoolExecutor threadPoolExecutor;

	private BoundListOperations<String, String> boundListOperations;

	protected final Class<T> entityClass;

	protected String simpleUpdateStatusHql;

	protected String defaultUpdateStatusHql;

	protected String conditionalUpdateStatusHql;

	protected final AtomicInteger cycles = new AtomicInteger();

	@SuppressWarnings("unchecked")
	public AbstractPollingControl() {
		entityClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), AbstractPollingControl.class);
	}

	@PostConstruct
	private void init() {
		simpleUpdateStatusHql = "update " + entityClass.getSimpleName()
				+ " t set t.status=?3,t.modifyDate=?4,t.errorInfo=?5 where t.id=?1 and t.status=?2";
		defaultUpdateStatusHql = "update " + entityClass.getSimpleName()
				+ " t set t.status=?3,t.modifyDate=?4,t.errorInfo=?5,t.attempts=t.attempts+1 where t.id=?1 and t.status=?2";
		conditionalUpdateStatusHql = "update " + entityClass.getSimpleName()
				+ " t set t.status=case when t.attempts+1>=?3 then ?4 else ?5 end,t.modifyDate=?6,t.errorInfo=?7,t.attempts=t.attempts+1 where t.id=?1 and t.status=?2";

		boundListOperations = stringRedisTemplate.boundListOps(getQueueName());
	}

	@Scheduled(fixedRateString = "${pollingControl.enqueue.fixedRate:10000}")
	public void enqueue() {
		if (!isRunning() || threadPoolExecutor.isShutdown())
			return;
		threadPoolExecutor.execute(this::doEnqueue);
	}

	protected void doEnqueue() {
		cycles.incrementAndGet();
		int current = cycles.get();
		entityManager.setEntityClass(entityClass);

		// normal
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("status", PollingStatus.INITIALIZED));
		dc.add(Restrictions.or(Restrictions.isNull("scheduledFor"),
				Restrictions.le("scheduledFor", LocalDateTime.now())));
		doEnqueue(dc, Mode.NORMAL);

		if (current % 5 == 1) {
			// normal retryable
			for (int attempts = 1; attempts < getMaxAttempts(); attempts++) {
				dc = entityManager.detachedCriteria();
				dc.add(Restrictions.eq("status", PollingStatus.TEMPORARY_ERROR));
				dc.add(Restrictions.eq("attempts", attempts));
				dc.add(Restrictions.lt("modifyDate",
						LocalDateTime.now().minusSeconds(getIntervalFactorInSeconds() * attempts)));
				doEnqueue(dc, Mode.RETRIED);
			}
			// abnormal retryable
			dc = entityManager.detachedCriteria();
			dc.add(Restrictions.eq("status", PollingStatus.TEMPORARY_ERROR));
			dc.add(Restrictions.ge("attempts", getMaxAttempts()));
			dc.add(Restrictions.lt("modifyDate",
					LocalDateTime.now().minusSeconds(getIntervalFactorInSeconds() * getMaxAttempts())));
			doEnqueue(dc, Mode.RETRIED);
		}
		if (current % 10 == 1) {
			// resubmit entities which status are PROCESSING, maybe caused by jvm killed
			dc = entityManager.detachedCriteria();
			dc.add(Restrictions.eq("status", PollingStatus.PROCESSING));
			dc.add(Restrictions.lt("modifyDate", LocalDateTime.now().minusSeconds(getResubmitIntervalInSeconds())));
			doEnqueue(dc, Mode.RESUBMITTED);
		}
	}

	protected void doEnqueue(DetachedCriteria dc, Mode mode) {
		dc.addOrder(Order.asc("precedence"));
		dc.addOrder(Order.asc("createDate"));
		dc.setLockMode(LockMode.UPGRADE_SKIPLOCKED);
		String s = "enqueue {}";
		if (mode != Mode.NORMAL)
			s = s + ' ' + mode.name().toLowerCase(Locale.ROOT);
		String message = s;
		while (true) {
			List<T> updatedEntities = entityManager.execute(session -> {
				session.setJdbcBatchSize(batchSize);
				Criteria c = dc.getExecutableCriteria(session);
				c.setMaxResults(batchSize);
				@SuppressWarnings("unchecked")
				List<T> entities = c.list();
				for (T entity : entities) {
					entity.setStatus(PollingStatus.PROCESSING);
					entity.setModifyDate(LocalDateTime.now());
					session.update(entity);
				}
				return entities;
			});
			if (updatedEntities.isEmpty())
				break;
			push(updatedEntities.stream().map(entity -> {
				logger.info(message, entity.getId());
				return entity.getId();
			}).collect(Collectors.toList()), mode != Mode.NORMAL);
		}
	}

	@Scheduled(fixedDelayString = "${pollingControl.dequeue.fixedDelay:10000}")
	public void dequeue() {
		if (!isRunning() || threadPoolExecutor.isShutdown())
			return;
		for (int i = 0; i < getThreads() - threadPoolExecutor.getActiveCount(); i++)
			threadPoolExecutor.execute(this::doDequeue);
	}

	protected abstract void doDequeue();

	protected String getQueueName() {
		return entityClass.getName();
	}

	public long getQueueDepth() {
		Long size = boundListOperations.size();
		return size != null ? size : 0;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void push(List<Long> ids, boolean deduplication) {
		if (ids.isEmpty())
			return;
		if (deduplication) {
			List<String> prepend = new ArrayList<>();
			List<String> append = new ArrayList<>();
			List results = stringRedisTemplate.executePipelined((SessionCallback) redisOperations -> {
				for (Long id : ids)
					redisOperations.opsForList().remove(boundListOperations.getKey(), 1, String.valueOf(id));
				return null;
			});
			for (int i = 0; i < ids.size(); i++) {
				String id = String.valueOf(ids.get(i));
				Long removed = (Long) results.get(i);
				if (removed != null && removed > 0) {
					prepend.add(id);
					logger.warn("cutting {}", id);
				} else {
					append.add(id);
				}
			}
			if (prepend.size() > 0)
				boundListOperations.rightPushAll(prepend.toArray(new String[0]));
			if (append.size() > 0)
				boundListOperations.leftPushAll(append.toArray(new String[0]));
		} else {
			boundListOperations.leftPushAll(ids.stream().map(i -> i.toString()).toArray(String[]::new));
		}
	}

	protected Long pop() {
		String id = boundListOperations.rightPop();
		return id != null ? Long.valueOf(id) : null;
	}

	protected boolean isTemporaryError(Exception e) {
		return e instanceof IOException || e.getCause() instanceof IOException;
	}

	protected void afterUpdated(Session session, T entity) {

	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public void start() {
		if (running.compareAndSet(false, true)) {
			threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getThreads() + 1,
					new NameableThreadFactory(StringUtils.uncapitalize(getClass().getSimpleName()), (t, e) -> {
						logger.error(e.getMessage(), e);
					}));
			logger.info("Created thread pool {}", threadPoolExecutor);
		} else {
			logger.error("{} is already running", this);
		}
	}

	@Override
	public void stop() {
		if (running.compareAndSet(true, false)) {
			threadPoolExecutor.shutdown();
			try {
				threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
			logger.info("Shutdown thread pool {}", threadPoolExecutor);
		} else {
			logger.error("{} is not running", this);
		}
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable runnable) {
		stop();
		runnable.run();
	}

	static enum Mode {
		NORMAL, RETRIED, RESUBMITTED;
	}

}
