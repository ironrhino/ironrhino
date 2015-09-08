package org.ironrhino.core.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.core.io.Resource;

public class WaterMark {

	private Resource mark;

	public Resource getMark() {
		return mark;
	}

	public void setMark(Resource mark) {
		this.mark = mark;
	}

	public BufferedImage mark(Image image) throws IOException {
		return mark(image, mark.getFile());
	}

	public static BufferedImage mark(Image image, File markFile) throws IOException {
		int widthOriginal = image.getWidth(null);
		int heightOriginal = image.getHeight(null);

		BufferedImage bufImage = new BufferedImage(widthOriginal, heightOriginal, BufferedImage.TYPE_INT_RGB);

		float alpha = 0.25f;
		Graphics2D g2d = bufImage.createGraphics();
		g2d.drawImage(image, 0, 0, widthOriginal, heightOriginal, null);
		Image imageWaterMark = ImageIO.read(markFile);
		int widthWaterMark = imageWaterMark.getWidth(null);
		int heightWaterMark = imageWaterMark.getHeight(null);

		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));

		// g2d.drawImage(imageWaterMark, widthOriginal - widthWaterMark,
		// heightOriginal - heightWaterMark, widthWaterMark,
		// heightWaterMark, null);

		g2d.drawImage(imageWaterMark, (widthOriginal - widthWaterMark) / 2, (heightOriginal - heightWaterMark) / 2,
				widthWaterMark, heightWaterMark, null);

		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		g2d.dispose();
		return bufImage;
	}
}
