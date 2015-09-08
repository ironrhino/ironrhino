package org.ironrhino.core.chart.openflashchart;

import java.io.Serializable;

public class Text implements Serializable {

	public static final String TEXT_ALIGN_CENTER = "center";
	public static final String TEXT_ALIGN_LEFT = "left";
	public static final String TEXT_ALIGN_RIGHT = "right";
	private static final long serialVersionUID = -2390229886841547192L;
	private String text;
	private String style;

	public Text() {
		this(null, null);
	}

	public Text(String text) {
		this(text, null);
	}

	public Text(String text, String style) {
		setText(text);
		setStyle(style);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;

	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;

	}

	public static String createStyle(int fontsize, String color, String textalign) {
		StringBuilder sb = new StringBuilder();
		if (fontsize != 0) {
			sb.append("font-size: ");
			sb.append(fontsize);
			sb.append("px;");
		}
		if (color != null) {
			sb.append("color: ");
			sb.append(color);
			sb.append(";");
		}
		if (textalign != null) {
			sb.append("text-align: ");
			sb.append(textalign);
			sb.append(";");
		}
		return sb.toString();
	}

	public static String createStyle(int fontsize) {
		return createStyle(fontsize, null, null);
	}
}
