package org.ironrhino.core.cache.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import org.nustaq.serialization.FSTConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.CompressionMode;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.transcoders.TranscoderUtils;

class FstTranscoder implements Transcoder<Object> {

	private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	public static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

	public static final String DEFAULT_CHARSET = "UTF-8";

	protected int compressionThreshold = DEFAULT_COMPRESSION_THRESHOLD;
	protected String charset = DEFAULT_CHARSET;
	protected CompressionMode compressMode = CompressionMode.GZIP;
	protected static final Logger log = LoggerFactory.getLogger(FstTranscoder.class);

	public void setCompressionThreshold(int to) {
		this.compressionThreshold = to;
	}

	public CompressionMode getCompressMode() {
		return compressMode;
	}

	public void setCompressionMode(CompressionMode compressMode) {
		this.compressMode = compressMode;
	}

	public void setCharset(String to) {
		// Validate the character set.
		try {
			new String(new byte[97], to);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		this.charset = to;
	}

	protected byte[] serialize(Object o) {
		if (o == null) {
			throw new NullPointerException("Can't serialize null");
		}
		return conf.asByteArray(o);
	}

	protected Object deserialize(byte[] in) {
		if (in == null)
			return null;
		return conf.asObject(in);
	}

	public final byte[] compress(byte[] in) {
		switch (this.compressMode) {
		case GZIP:
			return gzipCompress(in);
		case ZIP:
			return zipCompress(in);
		default:
			return gzipCompress(in);
		}

	}

	private byte[] zipCompress(byte[] in) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(in.length);
		DeflaterOutputStream os = new DeflaterOutputStream(baos);
		try {
			os.write(in);
			os.finish();
			try {
				os.close();
			} catch (IOException e) {
				log.error("Close DeflaterOutputStream error", e);
			}
		} catch (IOException e) {
			throw new RuntimeException("IO exception compressing data", e);
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
				log.error("Close ByteArrayOutputStream error", e);
			}
		}
		return baos.toByteArray();
	}

	private static byte[] gzipCompress(byte[] in) {
		if (in == null) {
			throw new NullPointerException("Can't compress null");
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		GZIPOutputStream gz = null;
		try {
			gz = new GZIPOutputStream(bos);
			gz.write(in);
		} catch (IOException e) {
			throw new RuntimeException("IO exception compressing data", e);
		} finally {
			if (gz != null) {
				try {
					gz.close();
				} catch (IOException e) {
					log.error("Close GZIPOutputStream error", e);
				}
			}
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					log.error("Close ByteArrayOutputStream error", e);
				}
			}
		}
		byte[] rv = bos.toByteArray();
		// log.debug("Compressed %d bytes to %d", in.length, rv.length);
		return rv;
	}

	static int COMPRESS_RATIO = 8;

	protected byte[] decompress(byte[] in) {
		switch (this.compressMode) {
		case GZIP:
			return gzipDecompress(in);
		case ZIP:
			return zipDecompress(in);
		default:
			return gzipDecompress(in);
		}
	}

	private byte[] zipDecompress(byte[] in) {
		int size = in.length * COMPRESS_RATIO;
		ByteArrayInputStream bais = new ByteArrayInputStream(in);
		InflaterInputStream is = new InflaterInputStream(bais);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
		try {
			byte[] uncompressMessage = new byte[size];
			while (true) {
				int len = is.read(uncompressMessage);
				if (len <= 0) {
					break;
				}
				baos.write(uncompressMessage, 0, len);
			}
			baos.flush();
			return baos.toByteArray();

		} catch (IOException e) {
			log.error("Failed to decompress data", e);
			// baos = null;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				log.error("failed to close InflaterInputStream");
			}
			try {
				bais.close();
			} catch (IOException e) {
				log.error("failed to close ByteArrayInputStream");
			}
			try {
				baos.close();
			} catch (IOException e) {
				log.error("failed to close ByteArrayOutputStream");
			}
		}
		return baos == null ? null : baos.toByteArray();
	}

	private byte[] gzipDecompress(byte[] in) {
		ByteArrayOutputStream bos = null;
		if (in != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(in);
			bos = new ByteArrayOutputStream();
			GZIPInputStream gis = null;
			try {
				gis = new GZIPInputStream(bis);

				byte[] buf = new byte[64 * 1024];
				int r = -1;
				while ((r = gis.read(buf)) > 0) {
					bos.write(buf, 0, r);
				}
			} catch (IOException e) {
				log.error("Failed to decompress data", e);
				bos = null;
			} finally {
				if (gis != null) {
					try {
						gis.close();
					} catch (IOException e) {
						log.error("Close GZIPInputStream error", e);
					}
				}
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
						log.error("Close ByteArrayInputStream error", e);
					}
				}
			}
		}
		return bos == null ? null : bos.toByteArray();
	}

	protected String decodeString(byte[] data) {
		String rv = null;
		try {
			if (data != null) {
				rv = new String(data, this.charset);
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return rv;
	}

	protected byte[] encodeString(String in) {
		byte[] rv = null;
		try {
			rv = in.getBytes(this.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return rv;
	}

	public void setPackZeros(boolean packZeros) {
		this.transcoderUtils.setPackZeros(packZeros);
	}

	public void setPrimitiveAsString(boolean primitiveAsString) {
		this.primitiveAsString = primitiveAsString;
	}

	private final int maxSize;

	private boolean primitiveAsString;

	public final int getMaxSize() {
		return this.maxSize;
	}

	// General flags
	public static final int SERIALIZED = 1;
	public static final int COMPRESSED = 2;

	// Special flags for specially handled types.
	public static final int SPECIAL_MASK = 0xff00;
	public static final int SPECIAL_BOOLEAN = (1 << 8);
	public static final int SPECIAL_INT = (2 << 8);
	public static final int SPECIAL_LONG = (3 << 8);
	public static final int SPECIAL_DATE = (4 << 8);
	public static final int SPECIAL_BYTE = (5 << 8);
	public static final int SPECIAL_FLOAT = (6 << 8);
	public static final int SPECIAL_DOUBLE = (7 << 8);
	public static final int SPECIAL_BYTEARRAY = (8 << 8);

	private final TranscoderUtils transcoderUtils = new TranscoderUtils(true);

	public TranscoderUtils getTranscoderUtils() {
		return transcoderUtils;
	}

	/**
	 * Get a serializing transcoder with the default max data size.
	 */
	public FstTranscoder() {
		this(CachedData.MAX_SIZE);
	}

	/**
	 * Get a serializing transcoder that specifies the max data size.
	 */
	public FstTranscoder(int max) {
		this.maxSize = max;
	}

	public boolean isPackZeros() {
		return this.transcoderUtils.isPackZeros();
	}

	public boolean isPrimitiveAsString() {
		return this.primitiveAsString;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.spy.memcached.Transcoder#decode(net.spy.memcached.CachedData)
	 */
	public final Object decode(CachedData d) {
		Object obj = d.decodedObject;
		if (obj != null) {
			return obj;
		}
		byte[] data = d.getData();

		int flags = d.getFlag();
		if ((flags & COMPRESSED) != 0) {
			data = decompress(data);
		}
		flags = flags & SPECIAL_MASK;
		obj = decode0(d, data, flags);
		d.decodedObject = obj;
		return obj;
	}

	protected final Object decode0(CachedData cachedData, byte[] data, int flags) {
		Object rv = null;
		if ((cachedData.getFlag() & SERIALIZED) != 0 && data != null) {
			rv = deserialize(data);
		} else {
			if (this.primitiveAsString) {
				if (flags == 0) {
					return decodeString(data);
				}
			}
			if (flags != 0 && data != null) {
				switch (flags) {
				case SPECIAL_BOOLEAN:
					rv = Boolean.valueOf(this.transcoderUtils.decodeBoolean(data));
					break;
				case SPECIAL_INT:
					rv = Integer.valueOf(this.transcoderUtils.decodeInt(data));
					break;
				case SPECIAL_LONG:
					rv = Long.valueOf(this.transcoderUtils.decodeLong(data));
					break;
				case SPECIAL_BYTE:
					rv = Byte.valueOf(this.transcoderUtils.decodeByte(data));
					break;
				case SPECIAL_FLOAT:
					rv = new Float(Float.intBitsToFloat(this.transcoderUtils.decodeInt(data)));
					break;
				case SPECIAL_DOUBLE:
					rv = new Double(Double.longBitsToDouble(this.transcoderUtils.decodeLong(data)));
					break;
				case SPECIAL_DATE:
					rv = new Date(this.transcoderUtils.decodeLong(data));
					break;
				case SPECIAL_BYTEARRAY:
					rv = data;
					break;
				default:
					log.warn(String.format("Undecodeable with flags %x", flags));
				}
			} else {
				rv = decodeString(data);
			}
		}
		return rv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.spy.memcached.Transcoder#encode(java.lang.Object)
	 */
	public final CachedData encode(Object o) {
		byte[] b = null;
		int flags = 0;
		if (o instanceof String) {
			b = encodeString((String) o);
		} else if (o instanceof Long) {
			if (this.primitiveAsString) {
				b = encodeString(o.toString());
			} else {
				b = this.transcoderUtils.encodeLong((Long) o);
			}
			flags |= SPECIAL_LONG;
		} else if (o instanceof Integer) {
			if (this.primitiveAsString) {
				b = encodeString(o.toString());
			} else {
				b = this.transcoderUtils.encodeInt((Integer) o);
			}
			flags |= SPECIAL_INT;
		} else if (o instanceof Boolean) {
			if (this.primitiveAsString) {
				b = encodeString(o.toString());
			} else {
				b = this.transcoderUtils.encodeBoolean((Boolean) o);
			}
			flags |= SPECIAL_BOOLEAN;
		} else if (o instanceof Date) {
			b = this.transcoderUtils.encodeLong(((Date) o).getTime());
			flags |= SPECIAL_DATE;
		} else if (o instanceof Byte) {
			if (this.primitiveAsString) {
				b = encodeString(o.toString());
			} else {
				b = this.transcoderUtils.encodeByte((Byte) o);
			}
			flags |= SPECIAL_BYTE;
		} else if (o instanceof Float) {
			if (this.primitiveAsString) {
				b = encodeString(o.toString());
			} else {
				b = this.transcoderUtils.encodeInt(Float.floatToRawIntBits((Float) o));
			}
			flags |= SPECIAL_FLOAT;
		} else if (o instanceof Double) {
			if (this.primitiveAsString) {
				b = encodeString(o.toString());
			} else {
				b = this.transcoderUtils.encodeLong(Double.doubleToRawLongBits((Double) o));
			}
			flags |= SPECIAL_DOUBLE;
		} else if (o instanceof byte[]) {
			b = (byte[]) o;
			flags |= SPECIAL_BYTEARRAY;
		} else {
			b = serialize(o);
			flags |= SERIALIZED;
		}
		assert b != null;
		if (this.primitiveAsString) {
			// It is not be SERIALIZED,so change it to string type
			if ((flags & SERIALIZED) == 0) {
				flags = 0;
			}
		}
		if (b.length > this.compressionThreshold) {
			byte[] compressed = compress(b);
			if (compressed.length < b.length) {
				if (log.isDebugEnabled()) {
					log.debug(
							"Compressed " + o.getClass().getName() + " from " + b.length + " to " + compressed.length);
				}
				b = compressed;
				flags |= COMPRESSED;
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Compression increased the size of " + o.getClass().getName() + " from " + b.length
							+ " to " + compressed.length);
				}
			}
		}
		return new CachedData(flags, b, this.maxSize, -1);
	}
}
