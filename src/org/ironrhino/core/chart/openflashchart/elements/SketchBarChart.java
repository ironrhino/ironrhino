package org.ironrhino.core.chart.openflashchart.elements;

public class SketchBarChart extends FilledBarChart {

	private static final long serialVersionUID = 7562070898232847510L;

	private static final transient String TYPE = "bar_sketch";

	private Integer offset;

	public SketchBarChart() {
		super(TYPE);
	}

	public SketchBarChart(String colour, String outlineColour, Integer offset) {
		super(TYPE);
		setColour(colour);
		setOutlineColour(outlineColour);
		setOffset(offset);
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public static class Bar extends FilledBarChart.Bar {

		private static final long serialVersionUID = -6406122712221203219L;
		private Integer offset;

		public Bar(Number top) {
			super(top);
		}

		public Bar(Number top, Integer offset) {
			super(top);
			setOffset(offset);
		}

		public Bar(Number top, Number bottom, Integer offset) {
			super(top, bottom);
			setOffset(offset);
		}

		public Integer getOffset() {
			return offset;
		}

		public void setOffset(Integer offset) {
			this.offset = offset;
		}

	}
}
