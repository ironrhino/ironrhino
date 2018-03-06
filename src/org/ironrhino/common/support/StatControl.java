package org.ironrhino.common.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.Stat;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.KeyValuePair;
import org.ironrhino.core.stat.Value;
import org.ironrhino.core.stat.analysis.AbstractAnalyzer;
import org.ironrhino.core.stat.analysis.Analyzer;
import org.ironrhino.core.stat.analysis.CumulativeAnalyzer;
import org.ironrhino.core.stat.analysis.PeriodAnalyzer;
import org.ironrhino.core.stat.analysis.TreeNode;
import org.ironrhino.core.throttle.Mutex;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CompositeIterator;
import org.ironrhino.core.util.DateUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("rawtypes")
public class StatControl {

	@Autowired
	private Logger logger;

	@Autowired
	private EntityManager<Stat> entityManager;

	@Scheduled(cron = "${statControl.archive.cron:0 5 0 * * ?}")
	@Mutex(scope = Scope.LOCAL)
	public void archive() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		Date yesterday = cal.getTime();
		archive(yesterday);
	}

	@Mutex(scope = Scope.LOCAL)
	public void archive(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		Map<String, File> map = AbstractAnalyzer.getLogFile(date, true);
		for (Map.Entry<String, File> entry : map.entrySet()) {
			final String host = entry.getKey();
			final File file = entry.getValue();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			Date start = cal.getTime();
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			Date end = cal.getTime();
			entityManager.setEntityClass(Stat.class);
			DetachedCriteria dc = entityManager.detachedCriteria();
			dc.add(Restrictions.eq("host", host));
			dc.add(Restrictions.between("date", start, end));
			dc.addOrder(Order.desc("date"));
			Stat stat = entityManager.findByCriteria(dc);
			final Date lastStatDate;
			if (stat != null)
				lastStatDate = stat.getDate();
			else
				lastStatDate = null;
			try {
				Analyzer analyzer = new AbstractAnalyzer(file) {
					Calendar calendar = Calendar.getInstance();
					int currentHour = 0;
					Map<Key, Value> map = new HashMap<>();

					private void save() {
						Map<Key, Value> sortedMap = new TreeMap<>(map);
						for (Map.Entry<Key, Value> entry : sortedMap.entrySet())
							entityManager.save(new Stat(entry.getKey(), entry.getValue(),
									new Date(entry.getKey().getLastWriteTime()), host));
						map.clear();
					}

					@Override
					protected void process(KeyValuePair pair) {
						if (!pair.getKey().isCumulative())
							return;
						if (lastStatDate != null) {
							long time = lastStatDate.getTime();
							// hibernate doesn't handle mysql's
							// timestamp
							if (time % 1000 == 0)
								time += 999;
							if (pair.getDate().getTime() <= time)
								return;
						}
						Key lastKey = null;
						Value lastValue = null;
						for (Map.Entry<Key, Value> entry : map.entrySet()) {
							if (pair.getKey().equals(entry.getKey())) {
								lastKey = entry.getKey();
								lastValue = entry.getValue();
								break;
							}
						}
						if (lastValue == null) {
							lastKey = pair.getKey();
							lastValue = pair.getValue();
							lastKey.setLastWriteTime(pair.getDate().getTime());
							map.put(lastKey, lastValue);
						} else {
							if (calendar.get(Calendar.HOUR_OF_DAY) == currentHour) {
								lastKey.setLastWriteTime(pair.getDate().getTime());
								lastValue.accumulate(pair.getValue());
							} else {
								save();
								lastKey = pair.getKey();
								lastValue = pair.getValue();
								lastKey.setLastWriteTime(pair.getDate().getTime());
								map.put(lastKey, lastValue);
							}
						}
						calendar.setTime(pair.getDate());
						currentHour = calendar.get(Calendar.HOUR_OF_DAY);
					}

					@Override
					protected void postAnalyze() {
						save();
					}

					@Override
					public Object getResult() {
						return null;
					}
				};
				analyzer.analyze();
			} catch (FileNotFoundException e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
	}

	public Map<String, List<TreeNode>> getResult(Date date) {
		return getResult(date, date);
	}

	public Map<String, List<TreeNode>> getResult(Date date, boolean localhost) {
		return getResult(date, date, localhost);
	}

	public Map<String, List<TreeNode>> getResult(Date from, Date to) {
		return getResult(from, to, false);
	}

	@SuppressWarnings("unchecked")
	public Map<String, List<TreeNode>> getResult(Date from, Date to, boolean localhost) {
		Date today = new Date();
		if (from == null)
			throw new IllegalArgumentException("from is null");
		if (to == null || to.after(today))
			to = today;
		if (to.before(from))
			throw new IllegalArgumentException("to is before of from");
		Date criticalDate = getCriticalDate(from, to, localhost);
		boolean allInFile = DateUtils.isSameDay(criticalDate, from);
		CumulativeAnalyzer analyzer = null;
		if (allInFile) {
			try {
				analyzer = new CumulativeAnalyzer(from, to, localhost);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage(), e);
			}
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(from);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			Date start = cal.getTime();
			if (criticalDate == null) {
				cal.setTime(to);
			} else {
				cal.setTime(criticalDate);
				cal.add(Calendar.DAY_OF_YEAR, -1);
			}
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			Date end = cal.getTime();
			entityManager.setEntityClass(Stat.class);
			DetachedCriteria dc = entityManager.detachedCriteria();
			dc.add(Restrictions.between("date", start, end));
			if (localhost)
				dc.add(Restrictions.eq("host", AppInfo.getHostName()));
			List<Stat> list = entityManager.findListByCriteria(dc);
			try {
				if (list.size() > 0) {
					Iterator<? extends KeyValuePair> it1 = list.iterator();
					if (criticalDate != null) {
						Iterator<? extends KeyValuePair> it2 = new CumulativeAnalyzer(criticalDate, to, localhost)
								.iterate();
						analyzer = new CumulativeAnalyzer(new CompositeIterator(it1, it2));
					} else {
						analyzer = new CumulativeAnalyzer(it1);
					}

				} else {
					if (criticalDate != null) {
						analyzer = new CumulativeAnalyzer(criticalDate, to, localhost);
					}
				}
			} catch (FileNotFoundException e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
		if (analyzer != null) {
			analyzer.analyze();
			return analyzer.getResult();
		} else {
			return new HashMap<>();
		}
	}

	private Date getCriticalDate(Date from, Date to, boolean localhost) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(to);
		Date criticalDate = null;
		while (AbstractAnalyzer.hasLogFile(cal.getTime(), localhost) && !DateUtils.isSameDay(criticalDate, from)) {
			criticalDate = cal.getTime();
			cal.add(Calendar.DAY_OF_YEAR, -1);
		}
		return criticalDate;
	}

	@SuppressWarnings("unchecked")
	public List<Value> getPeriodResult(Key key, Date date, boolean cumulative, boolean localhost) {
		return (List<Value>) getPeriodResult(key, date, cumulative, false, localhost);
	}

	@SuppressWarnings("unchecked")
	public Map<String, List<Value>> getPerHostPeriodResult(Key key, Date date, boolean cumulative) {
		return (Map<String, List<Value>>) getPeriodResult(key, date, cumulative, true);
	}

	private Object getPeriodResult(Key key, Date date, boolean cumulative, boolean perHost, boolean localhost) {
		PeriodAnalyzer analyzer;
		try {
			analyzer = new PeriodAnalyzer(key, date, localhost);
			analyzer.setCumulative(cumulative);
		} catch (FileNotFoundException e) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			Date start = cal.getTime();
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			Date end = cal.getTime();
			entityManager.setEntityClass(Stat.class);
			DetachedCriteria dc = entityManager.detachedCriteria();
			dc.add(Restrictions.eq("keyAsString", key.toString()));
			dc.add(Restrictions.between("date", start, end));
			if (localhost)
				dc.add(Restrictions.eq("host", AppInfo.getHostName()));
			List<Stat> list = entityManager.findListByCriteria(dc);
			analyzer = new PeriodAnalyzer(key, list.iterator());
			analyzer.setCumulative(cumulative);
		}
		if (perHost)
			analyzer.setPerHostEnabled(true);
		analyzer.analyze();
		if (perHost)
			return analyzer.getPerHostResult();
		return analyzer.getResult();
	}

}
