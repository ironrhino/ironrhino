package org.ironrhino.core.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

public class BarcodeUtils {

	public static byte[] encodeQRCode(String content) throws WriterException,
			IOException {
		return encodeQRCode(content, null, null, -1, -1);
	}

	public static byte[] encodeQRCode(String content, String encoding,
			String format, int width, int height) throws WriterException,
			IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
		encodeQRCode(content, encoding, format, width, height, stream);
		byte[] bytes = stream.toByteArray();
		return bytes;
	}

	public static void encodeQRCode(String content, OutputStream stream)
			throws WriterException, IOException {
		encodeQRCode(content, null, null, -1, -1, stream);
	}

	public static void encodeQRCode(String content, String encoding,
			String format, int width, int height, OutputStream stream)
			throws WriterException, IOException {
		encodeQRCode(content, encoding, format, width, height, stream, null);
	}

	public static void encodeQRCode(String content, String encoding,
			String format, int width, int height, OutputStream stream,
			InputStream watermark) throws WriterException, IOException {
		if (StringUtils.isBlank(encoding))
			encoding = "UTF-8";
		if (format == null)
			format = "png";
		if (width <= 0)
			width = 400;
		if (height <= 0)
			height = 400;
		BitMatrix matrix = new QRCodeWriter().encode(
				new String(content.getBytes(encoding), "ISO-8859-1"),
				BarcodeFormat.QR_CODE, width, height);
		if (watermark == null) {
			MatrixToImageWriter.writeToStream(matrix, format, stream);
			stream.close();
		} else {
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
			MatrixToImageWriter.writeToStream(matrix, format, os);
			BufferedImage image2 = ImageIO.read(new ByteArrayInputStream(os
					.toByteArray()));
			BufferedImage image = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_RGB);
			BufferedImage watermarkImage = ImageIO.read(watermark);
			Graphics2D g2d = image.createGraphics();
			g2d.drawImage(image2, 0, 0, width, height, null);
			int widthWaterMark = Math.min(watermarkImage.getWidth(null),
					width / 6);
			int heightWaterMark = Math.min(watermarkImage.getHeight(null),
					height / 6);
			float alpha = 0.8f;
			g2d.setComposite(AlphaComposite.getInstance(
					AlphaComposite.SRC_ATOP, alpha));
			g2d.drawImage(watermarkImage, (width - widthWaterMark) / 2,
					(height - heightWaterMark) / 2, widthWaterMark,
					heightWaterMark, null);
			g2d.setComposite(AlphaComposite
					.getInstance(AlphaComposite.SRC_OVER));
			g2d.dispose();
			ImageIO.write(image, format, stream);
		}
	}

	public static byte[] encodeEAN13(String content) throws WriterException,
			IOException {
		return encodeEAN13(content, null, -1, -1);
	}

	public static byte[] encodeEAN13(String content, String format, int width,
			int height) throws WriterException, IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream(256);
		encodeEAN13(content, format, width, height, stream);
		byte[] bytes = stream.toByteArray();
		return bytes;
	}

	public static void encodeEAN13(String content, OutputStream stream)
			throws WriterException, IOException {
		encodeEAN13(content, null, -1, -1, stream);
	}

	public static void encodeEAN13(String content, String format, int width,
			int height, OutputStream stream) throws WriterException,
			IOException {
		if (format == null)
			format = "png";
		if (width <= 0)
			width = 105;
		if (height <= 0)
			height = 50;
		int codeWidth = 3 + // start guard
				(7 * 6) + // left bars
				5 + // middle guard
				(7 * 6) + // right bars
				3; // end guard
		codeWidth = Math.max(codeWidth, width);
		BitMatrix bitMatrix = new MultiFormatWriter().encode(content,
				BarcodeFormat.EAN_13, codeWidth, height, null);
		MatrixToImageWriter.writeToStream(bitMatrix, format, stream);
	}

	public static String decode(String url) throws IOException,
			NotFoundException, ChecksumException, FormatException {
		return decode(url, null);
	}

	public static String decode(String url, String encoding)
			throws IOException, NotFoundException, ChecksumException,
			FormatException {
		URLConnection connection = new URL(url).openConnection();
		return decode(connection.getInputStream(), encoding);
	}

	public static String decode(byte[] bytes) throws IOException,
			NotFoundException, ChecksumException, FormatException {
		return decode(bytes, null);
	}

	public static String decode(byte[] bytes, String encoding)
			throws IOException, NotFoundException, ChecksumException,
			FormatException {
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		return decode(stream, encoding);
	}

	public static String decode(InputStream stream) throws IOException,
			NotFoundException, ChecksumException, FormatException {
		return decode(stream, null);
	}

	public static String decode(InputStream stream, String encoding)
			throws IOException, NotFoundException, ChecksumException,
			FormatException {
		if (StringUtils.isBlank(encoding))
			encoding = "UTF-8";
		BufferedImage image = ImageIO.read(stream);
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		Map<DecodeHintType, String> hints = new HashMap<DecodeHintType, String>(
				2, 1);
		hints.put(DecodeHintType.CHARACTER_SET, encoding);
		Result result = new MultiFormatReader().decode(bitmap, hints);
		return result.getText();
	}

}
