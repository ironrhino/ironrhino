package org.ironrhino.core.sequence;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

public interface CyclicSequence extends Sequence {

	public long nextLongValue();

	public CycleType getCycleType();

	public static enum CycleType {

		MINUTE("yyyyMMddHHmm") {
			@Override
			public boolean isSameCycle(Calendar lastCal, Calendar nowCal) {
				return HOUR.isSameCycle(lastCal, nowCal)
						&& nowCal.get(Calendar.MINUTE) <= lastCal
								.get(Calendar.MINUTE);

			}

			@Override
			public void skipToNextCycleStart(Calendar cal) {
				cal.add(Calendar.MINUTE, 1);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}

			@Override
			public void skipToLastCycleEnd(Calendar cal) {
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				cal.add(Calendar.MILLISECOND, -1);
			}
		},
		HOUR("yyyyMMddHH") {
			@Override
			public boolean isSameCycle(Calendar lastCal, Calendar nowCal) {
				return DAY.isSameCycle(lastCal, nowCal)
						&& nowCal.get(Calendar.HOUR_OF_DAY) <= lastCal
								.get(Calendar.HOUR_OF_DAY);
			}

			@Override
			public void skipToNextCycleStart(Calendar cal) {
				cal.add(Calendar.HOUR_OF_DAY, 1);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}

			@Override
			public void skipToLastCycleEnd(Calendar cal) {
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				cal.add(Calendar.MILLISECOND, -1);
			}

		},
		DAY("yyyyMMdd") {
			@Override
			public boolean isSameCycle(Calendar lastCal, Calendar nowCal) {
				return MONTH.isSameCycle(lastCal, nowCal)
						&& nowCal.get(Calendar.DAY_OF_YEAR) <= lastCal
								.get(Calendar.DAY_OF_YEAR);
			}

			@Override
			public void skipToNextCycleStart(Calendar cal) {
				cal.add(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}

			@Override
			public void skipToLastCycleEnd(Calendar cal) {
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				cal.add(Calendar.MILLISECOND, -1);
			}

		},
		MONTH("yyyyMM") {
			@Override
			public boolean isSameCycle(Calendar lastCal, Calendar nowCal) {
				return YEAR.isSameCycle(lastCal, nowCal)
						&& nowCal.get(Calendar.MONTH) <= lastCal
								.get(Calendar.MONTH);
			}

			@Override
			public void skipToNextCycleStart(Calendar cal) {
				cal.add(Calendar.MONTH, 1);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}

			@Override
			public void skipToLastCycleEnd(Calendar cal) {
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				cal.add(Calendar.MILLISECOND, -1);
			}

		},
		YEAR("yyyy") {
			@Override
			public boolean isSameCycle(Calendar lastCal, Calendar nowCal) {
				return (nowCal.get(Calendar.YEAR) <= lastCal.get(Calendar.YEAR));
			}

			@Override
			public void skipToNextCycleStart(Calendar cal) {
				cal.add(Calendar.YEAR, 1);
				cal.set(Calendar.MONTH, 0);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}

			@Override
			public void skipToLastCycleEnd(Calendar cal) {
				cal.set(Calendar.MONTH, 0);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				cal.add(Calendar.MILLISECOND, -1);
			}

		};
		private FastDateFormat format;

		private CycleType(String pattern) {
			this.format = FastDateFormat.getInstance(pattern);
		}

		public String getPattern() {
			return format.getPattern();
		}

		public String format(Date date) {
			return format.format(date);
		}

		public boolean isSameCycle(Date last, Date now) {
			if (last == null)
				return true;
			Calendar lastCalendar = Calendar.getInstance();
			lastCalendar.setTime(last);
			Calendar nowCalendar = Calendar.getInstance();
			nowCalendar.setTime(now);
			return isSameCycle(lastCalendar, nowCalendar);
		}

		public Date getNextCycleStart(Date date) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			skipToNextCycleStart(cal);
			return cal.getTime();
		}

		public Date getLastCycleEnd(Date date) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			skipToLastCycleEnd(cal);
			return cal.getTime();
		}

		public abstract void skipToNextCycleStart(Calendar cal);

		public abstract void skipToLastCycleEnd(Calendar cal);

		public abstract boolean isSameCycle(Calendar lastCal, Calendar nowCal);

	}

}