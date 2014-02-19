package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonValue;

public class Tooltip implements Serializable {

	private static final long serialVersionUID = 3867120511555479609L;
	private Mouse mouse = null;
	private Boolean shadow;
	private Integer stroke;
	private String colour;
	private String background;
	private String title;
	private String body;

	public Tooltip() {
	}

	public Mouse getMouse() {
		return mouse;
	}

	public void setMouse(Mouse mouse) {
		this.mouse = mouse;
	}

	public void setHover() {
		this.mouse = Mouse.HOVER;

	}

	public void setProximity() {
		this.mouse = Mouse.PROXIMITY;

	}

	public Boolean getShadow() {
		return shadow;
	}

	public void setShadow(Boolean shadow) {
		this.shadow = shadow;

	}

	public Integer getStroke() {
		return stroke;
	}

	public void setStroke(Integer stroke) {
		this.stroke = stroke;

	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;

	}

	public String getBackground() {
		return background;
	}

	public void setBackground(String background) {
		this.background = background;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public enum Mouse {
		PROXIMITY(1), HOVER(2);

		private final int value;

		private Mouse(Integer value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}
	}
}
