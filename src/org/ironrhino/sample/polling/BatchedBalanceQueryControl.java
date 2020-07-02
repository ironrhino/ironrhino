package org.ironrhino.sample.polling;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.ironrhino.common.support.BatchedPollingControl;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.util.NumberUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile(Profiles.SANDBOX)
public class BatchedBalanceQueryControl extends BatchedPollingControl<BalanceQuery> {

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
	protected Map<BalanceQuery, Result> handle(List<BalanceQuery> bqs) throws Exception {
		Map<BalanceQuery, Result> result = new HashMap<>(bqs.size() / 3 * 4);
		for (BalanceQuery bq : bqs) {
			try {
				BigDecimal balance = queryBalance(bq.getAccountNo());
				bq.setBalance(balance);
				result.put(bq, Result.of(Collections.singletonMap("balance", balance)));
			} catch (Exception e) {
				result.put(bq, Result.of(e));
			}
		}
		return result;
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

	@Override
	protected int getBatchLimit() {
		return 10;
	}

}
