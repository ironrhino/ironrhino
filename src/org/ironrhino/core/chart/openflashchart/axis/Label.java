package org.ironrhino.core.chart.openflashchart.axis;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonValue;

public class Label implements Serializable {

	private static final long serialVersionUID = -6976582830606939527L;

	private String text;
	private String colour;
	private Integer size;
	private Rotate rotate;
	private Boolean visible;

	public static enum Rotate {
		VERTICAL(-90), HALF_DIAGONAL(-24), DIAGONAL(-45), HORIZONTAL(0);

		private final int degrees;

		Rotate(int degrees) {
			this.degrees = degrees;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(degrees);
		}
	}

	public Label() {
	}

	public Label(String text) {
		setText(text);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public Rotate getRotate() {
		return rotate;
	}

	public void setRotate(Rotate rotate) {
		this.rotate = rotate;
	}

	public Boolean getVisible() {
		return visible;
	}

	public void setVisible(Boolean visible) {
		this.visible = visible;
	}

}
