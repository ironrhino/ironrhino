package org.ironrhino.sample.polling;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.throttle.Mutex;
import org.ironrhino.core.util.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile(Profiles.DUAL)
public class BalanceQueryService {

	private final static String key = "balance_query";

	@Autowired
	private Logger logger;

	@Autowired
	private EntityManager<BalanceQuery> entityManager;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Mutex
	@Scheduled(fixedRate = 10000)
	public void enqueue() {
		entityManager.setEntityClass(BalanceQuery.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("status", BalanceQueryStatus.INITIALIZED));
		entityManager.iterate(10, (entities, session) -> {
			for (BalanceQuery bq : entities) {
				stringRedisTemplate.opsForList().leftPush(key, bq.getId());
				bq.setStatus(BalanceQueryStatus.PROCESSING);
				bq.setModifyDate(new Date());
				session.update(bq);
				logger.info("enqueue {}#{}", bq.getId(), bq.getAccountNo());
			}
		}, dc, true);
	}

	@Scheduled(fixedDelay = 5000)
	public void dequeue() {
		entityManager.setEntityClass(BalanceQuery.class);
		while (true) {
			String id = stringRedisTemplate.opsForList().rightPop(key);
			if (id == null)
				break;
			logger.info("dequeue {}", id);
			BalanceQuery bq = entityManager.get(id);
			if (bq == null) {
				logger.warn("not found: {}", id);
				continue;
			}
			try {
				int result = entityManager.executeUpdate(
						"update BalanceQuery t set t.balance=?3,t.status=?4,t.modifyDate=?5 where t.id=?1 and t.status=?2",
						bq.getId(), BalanceQueryStatus.PROCESSING, queryBalance(bq.getAccountNo()),
						BalanceQueryStatus.SUCCESSFUL, new Date());
				if (result == 1)
					logger.info("process {} successfully", id);
				else
					logger.warn("process {} successfully and ignored", id);
			} catch (Exception e) {
				int result = entityManager.executeUpdate(
						"update BalanceQuery t set t.errorInfo=?3,t.status=?4,t.modifyDate=?5 where t.id=?1 and t.status=?2",
						bq.getId(), BalanceQueryStatus.PROCESSING, ExceptionUtils.getDetailMessage(e),
						BalanceQueryStatus.FAILED, new Date());
				if (result == 1)
					logger.info("process {} failed", id);
				else
					logger.warn("process {} failed and ignored", id);
			}
		}
	}

	protected BigDecimal queryBalance(String accountNo) {
		Random random = new Random();
		if (accountNo.length() < 5)
			throw new IllegalArgumentException("Illegal accountNo: " + accountNo);
		try {
			Thread.sleep(random.nextInt(5) * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new BigDecimal(random.nextInt(10000));
	}

}
