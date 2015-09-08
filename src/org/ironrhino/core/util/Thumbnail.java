package org.ironrhino.core.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Thumbnail {

	public static BufferedImage resize(Image image, int w, int h) throws IOException {
		BufferedImage bufImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		bufImage.getGraphics().drawImage(image, 0, 0, w, h, null);
		return bufImage;
	}

	public static BufferedImage resizeByHeight(Image image, int h) throws IOException {
		int w = (image.getWidth(null) * h / image.getHeight(null));
		return resize(image, w, h);
	}

	public static BufferedImage resizeByWidth(Image image, int w) throws IOException {
		int h = (image.getHeight(null) * w / image.getWidth(null));
		return resize(image, w, h);
	}

	public static BufferedImage resizeFix(Image image, int w, int h) throws IOException {
		if (image.getWidth(null) / image.getHeight(null) > w / h) {
			return resizeByWidth(image, w);
		} else {
			return resizeByHeight(image, h);
		}
	}

}
