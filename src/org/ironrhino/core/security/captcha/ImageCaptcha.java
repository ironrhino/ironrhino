package org.ironrhino.core.security.captcha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

public class ImageCaptcha {

	public static final int width = 200;// 80

	public static final int height = 50;

	private static Random random = new Random();

	private static List<String> fonts = new ArrayList<String>();

	private BufferedImage image;

	static {
		GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();
		String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames(Locale.CHINA);
		for (String s : names) {
			char c = s.charAt(0);
			if (Character.isLowerCase(c) || Character.isUpperCase(c)) {
			} else {
				fonts.add(s);
			}
		}
	}

	public ImageCaptcha(String challenge, String fuzzifiedChallenge) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		char[] use = fonts.size() > 0
				&& StringUtils.isNotBlank(fuzzifiedChallenge) ? fuzzifiedChallenge
				.toCharArray() : challenge.toCharArray();
		g.setColor(new Color(240, 240, 240));
		g.fillRect(0, 0, 200, 50);
		for (int i = 0; i < use.length; i++) {
			Point p = getPoint(i);
			int size = getSize();
			// g.setColor(new
			// Color((int)(Math.random()*256),0,(int)(Math.random()*256)));
			g.setPaint(getPaint(p, size));
			if (fonts.size() > 0)
				g.setFont(new Font(fonts.get((int) (Math.random() * fonts
						.size())), getFace(), size));
			g.drawString("" + use[i], p.x, p.y);
		}
		g.setStroke(new BasicStroke(1.0f));
		g.setPaint(null);
		for (int i = 0; i < use.length; i++) {
			g.setColor(new Color(random.nextInt(0x00FFFFFFF)));
			g.drawLine(random.nextInt(200), random.nextInt(50),
					random.nextInt(200), random.nextInt(50));
		}
		Random random = new Random();
		for (int i = 0; i < 88; i++) {
			int x = random.nextInt(200);
			int y = random.nextInt(50);
			g.setColor(new Color(random.nextInt(0x00FFFFFFF)));
			g.setStroke(getStroke());
			g.drawLine(x, y, x, y);
		}
		g.dispose();
	}

	private static Stroke getStroke() {
		BasicStroke bs = new BasicStroke((float) (Math.random() * 3));
		return bs;
	}

	private static Point getPoint(int index) {
		return new Point(5 + (index * (random.nextInt(10) + 40)), 40);
	}

	private static Paint getPaint(Point p, int size) {
		GradientPaint gp = new GradientPaint(p.x, p.y, new Color(
				random.nextInt(256), 0, random.nextInt(256)), p.x, p.y - size,
				new Color(random.nextInt(256), random.nextInt(256), random
						.nextInt(256)));
		return gp;
	}

	private static int getFace() {
		if (Math.random() * 10 > 5) {
			return Font.BOLD;
		} else {
			return Font.ITALIC;
		}
	}

	private static int getSize() {
		int[] sizes = new int[20];
		for (int i = 0; i < 20; i++) {
			sizes[i] = 30 + i;
		}
		return sizes[(int) (Math.random() * sizes.length)];
	}

	public BufferedImage getImage() {
		return image;
	}

}
