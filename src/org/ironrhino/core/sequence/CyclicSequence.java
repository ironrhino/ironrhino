package org.ironrhino.core.sequence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

public interface CyclicSequence extends Sequence {

	@Override
	default int nextIntValue() {
		return Integer.parseInt(nextStringValue().substring(getCycleType().getPattern().length()));
	}

	CycleType getCycleType();

	static enum CycleType {

		MINUTE("yyyyMMddHHmm") {
			@Override
			public boolean isSameCycle(LocalDateTime last, LocalDateTime now) {
				return HOUR.isSameCycle(last, now)
						&& now.get(ChronoField.MINUTE_OF_HOUR) == last.get(ChronoField.MINUTE_OF_HOUR);

			}

			@Override
			public LocalDateTime skipCycles(LocalDateTime datetime, int cycles) {
				return datetime.plus(cycles, ChronoUnit.MINUTES);
			}

			@Override
			public LocalDateTime getCycleStart(LocalDateTime datetime) {
				return datetime.withSecond(0).withNano(0);
			}

		},
		HOUR("yyyyMMddHH") {

			@Override
			public boolean isSameCycle(LocalDateTime last, LocalDateTime now) {
				return DAY.isSameCycle(last, now)
						&& now.get(ChronoField.HOUR_OF_DAY) == last.get(ChronoField.HOUR_OF_DAY);
			}

			@Override
			public LocalDateTime skipCycles(LocalDateTime datetime, int cycles) {
				return datetime.plus(cycles, ChronoUnit.HOURS);
			}

			@Override
			public LocalDateTime getCycleStart(LocalDateTime datetime) {
				return MINUTE.getCycleStart(datetime).withMinute(0);
			}

		},
		DAY("yyyyMMdd") {
			@Override
			public boolean isSameCycle(LocalDateTime last, LocalDateTime now) {
				return MONTH.isSameCycle(last, now)
						&& now.get(ChronoField.DAY_OF_YEAR) == last.get(ChronoField.DAY_OF_YEAR);
			}

			@Override
			public LocalDateTime skipCycles(LocalDateTime datetime, int cycles) {
				return datetime.plus(cycles, ChronoUnit.DAYS);
			}

			@Override
			public LocalDateTime getCycleStart(LocalDateTime datetime) {
				return HOUR.getCycleStart(datetime).withHour(0);
			}

		},
		MONTH("yyyyMM") {
			@Override
			public boolean isSameCycle(LocalDateTime last, LocalDateTime now) {
				return YEAR.isSameCycle(last, now)
						&& now.get(ChronoField.MONTH_OF_YEAR) == last.get(ChronoField.MONTH_OF_YEAR);
			}

			@Override
			public LocalDateTime skipCycles(LocalDateTime datetime, int cycles) {
				return datetime.plus(cycles, ChronoUnit.MONTHS);
			}

			@Override
			public LocalDateTime getCycleStart(LocalDateTime datetime) {
				return DAY.getCycleStart(datetime).withDayOfMonth(1);
			}

		},
		YEAR("yyyy") {
			@Override
			public boolean isSameCycle(LocalDateTime last, LocalDateTime now) {
				return (now.get(ChronoField.YEAR) == last.get(ChronoField.YEAR));
			}

			@Override
			public LocalDateTime skipCycles(LocalDateTime datetime, int cycles) {
				return datetime.plus(cycles, ChronoUnit.YEARS);
			}

			@Override
			public LocalDateTime getCycleStart(LocalDateTime datetime) {
				return MONTH.getCycleStart(datetime).withMonth(1);
			}

		};

		private final String pattern;

		private final DateTimeFormatter formatter;

		private CycleType(String pattern) {
			this.pattern = pattern;
			this.formatter = DateTimeFormatter.ofPattern(pattern);
		}

		public String getPattern() {
			return pattern;
		}

		public String format(LocalDateTime datetime) {
			return formatter.format(datetime);
		}

		public LocalDateTime getCycleEnd(LocalDateTime datetime) {
			return skipCycles(getCycleStart(datetime), 1).minus(1, ChronoUnit.NANOS);
		}

		public abstract LocalDateTime getCycleStart(LocalDateTime datetime);

		public abstract LocalDateTime skipCycles(LocalDateTime datetime, int cycles);

		public abstract boolean isSameCycle(LocalDateTime last, LocalDateTime now);

	}

}