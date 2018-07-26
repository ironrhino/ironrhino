package org.ironrhino.sample.polling;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import org.ironrhino.common.support.BasePollingControl;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.util.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile(Profiles.SANDBOX)
public class BalanceQueryControl extends BasePollingControl<BalanceQuery> {

	// 可选代码--开始 作用是设置自己的配置参数
	@Value("${balanceQueryControl.threads:3}")
	private int threads = 3;

	@Value("${balanceQueryControl.maxAttempts:3}")
	private int maxAttempts = 3;

	@Value("${balanceQueryControl.intervalFactorInSeconds:60}")
	private int intervalFactorInSeconds = 60;

	@Override
	public int getThreads() {
		return threads;
	}

	@Override
	public int getMaxAttempts() {
		return maxAttempts;
	}

	@Override
	public int getIntervalFactorInSeconds() {
		return intervalFactorInSeconds;
	}

	@Override
	@Scheduled(fixedRateString = "${balanceQueryControl.enqueue.fixedRate:5000}")
	public void enqueue() {
		super.enqueue();
	}

	@Override
	@Scheduled(fixedDelayString = "${balanceQueryControl.dequeue.fixedDelay:5000}")
	public void dequeue() {
		super.dequeue();
	}
	// 可选代码--结束

	/**
	 * 纯测试用
	 */
	@Transactional
	public void initData(int size) {
		String prefix = String.valueOf(System.currentTimeMillis());
		entityManager.execute(session -> {
			session.setJdbcBatchSize(getBatchSize());
			for (int i = 0; i < size; i++) {
				String accountNo = prefix + NumberUtils.format(i, 5);
				BalanceQuery bq = new BalanceQuery();
				bq.setAccountNo(accountNo);
				session.save(bq);
				if ((i + 1) % getBatchSize() == 0) {
					session.flush(); // 触发jdbc的statement.executeBatch()提交
					session.clear(); // 清理session一级缓存防止大数据量下内存溢出
				}
			}
			return null;
		});
	}

	/**
	 * 处理并且返回需要更新的字段
	 */
	@Override
	protected Map<String, Object> handle(BalanceQuery bq) throws Exception {
		BigDecimal balance = queryBalance(bq.getAccountNo());
		bq.setBalance(balance);
		return Collections.singletonMap("balance", balance);
	}

	private BigDecimal queryBalance(String accountNo) throws IOException {
		Random random = new Random();
		if (accountNo.endsWith("11"))
			throw new IllegalArgumentException("Illegal accountNo: " + accountNo);
		if (accountNo.endsWith("60"))
			throw new IOException("I/O Error");
		try {
			Thread.sleep(random.nextInt(10) * 100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new BigDecimal(random.nextInt(10000));
	}

}
