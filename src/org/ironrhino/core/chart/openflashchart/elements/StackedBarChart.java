package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StackedBarChart extends Element {

	private static final long serialVersionUID = -4495162733156231531L;
	private List<Key> keys = new ArrayList<>();

	public StackedBarChart() {
		super("bar_stack");
	}

	public List<Key> getKeys() {
		return keys;
	}

	public void addKeys(Key... keys) {
		addKeys(Arrays.asList(keys));
	}

	public void addKeys(List<Key> keys) {
		getKeys().addAll(keys);

	}

	public void addStack(Stack... stacks) {
		copy(Arrays.asList(stacks));
	}

	public void addStack(List<Stack> stacks) {
		copy(stacks);
	}

	public Stack newStack() {
		Stack s = new Stack();
		copy(Arrays.asList(s));
		return s;
	}

	public Stack lastStack() {
		if (getValues().isEmpty()) {
			return newStack();
		} else {
			return stack(getStackCount() - 1);
		}
	}

	@SuppressWarnings("unchecked")
	public Stack stack(int index) {
		return new Stack((List<Object>) getValues().get(index));
	}

	public int getStackCount() {
		return getValues().size();
	}

	private void copy(List<Stack> stacks) {
		for (Stack s : stacks) {
			getValues().add(s.getBackingList());
		}

	}

	public static class Stack implements Serializable {

		private static final long serialVersionUID = 5436766430239448801L;

		private List<Object> values;

		public Stack() {
			values = new ArrayList<>();
		}

		Stack(List<Object> values) {
			this.values = values;
		}

		public void addStackValues(StackValue... values) {
			doAdd(Arrays.asList(values));
		}

		public void addStackValues(List<StackValue> values) {
			doAdd(values);
		}

		public void addValues(Number... numbers) {
			addValues(Arrays.asList(numbers));
		}

		public void addValues(List<Number> numbers) {
			for (Number number : numbers) {
				if (number != null) {
					this.doAdd(Collections.singletonList(new StackValue(number)));
				}
			}

		}

		private void doAdd(List<? extends Object> values) {
			this.values.addAll(values);

		}

		List<Object> getBackingList() {
			return this.values;
		}
	}

	public static class StackValue implements Serializable {

		private static final long serialVersionUID = -2712623023061331779L;
		private Number val;
		private String colour;

		public StackValue(Number value) {
			this(value, null);
		}

		public StackValue(Number value, String colour) {
			setValue(value);
			setColour(colour);
		}

		public Number getValue() {
			return val;
		}

		public void setValue(Number val) {
			this.val = val;

		}

		public String getColour() {
			return colour;
		}

		public void setColour(String colour) {
			this.colour = colour;

		}
	}

	public static class Key implements Serializable {
		private static final long serialVersionUID = 2221314445193990267L;
		private String colour;
		private String text;
		@JsonProperty(value = "font-size")
		private Integer fontSize;

		public Key(String colour, String text, Integer fontSize) {
			this.colour = colour;
			this.text = text;
			this.fontSize = fontSize;
		}

		public String getColour() {
			return colour;
		}

		public void setColour(String colour) {
			this.colour = colour;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Integer getFontSize() {
			return fontSize;
		}

		public void setFontSize(Integer fontSize) {
			this.fontSize = fontSize;
		}
	}
}
