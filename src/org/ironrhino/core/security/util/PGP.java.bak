package org.ironrhino.core.security.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ElGamalParameterSpec;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.ironrhino.core.util.AppInfo;

public class PGP {
	private static Log log = LogFactory.getLog(PGP.class);

	public static final int kEY_SIZE = 1024, STRENGTH = 0;

	public final static String DEFAULT_PASSWORD_LOCATION = "/resources/key/pgp-password";
	public final static String DEFAULT_PRIVATE_KEY_LOCATION = "/resources/key/pgp-private";
	public final static String DEFAULT_PUBLIC_KEY_LOCATION = "/resources/key/pgp-public";
	public static final String KEY_DIRECTORY = "/key/";

	private static String password;
	private static PGPSecretKeyRing secretKeyRing;
	private static PGPSecretKeyRingCollection secretKeyRingCollection;
	private static PGPPublicKey publicKey;
	private static PGPPublicKeyRingCollection publicKeyRingCollection;

	static {
		Security.addProvider(new BouncyCastleProvider());
		try {
			File file = new File(AppInfo.getAppHome() + KEY_DIRECTORY
					+ "pgp-password");
			if (file.exists()) {
				password = FileUtils.readFileToString(file, "UTF-8");
			} else {
				log.warn("[" + file
						+ "] doesn't exists,use classpath resources "
						+ DEFAULT_PASSWORD_LOCATION);
				password = IOUtils.toString(PGP.class
						.getResourceAsStream(DEFAULT_PASSWORD_LOCATION),
						"UTF-8");
			}

			file = new File(AppInfo.getAppHome() + KEY_DIRECTORY
					+ "pgp-private");
			String privateKeyString;
			if (file.exists()) {
				privateKeyString = FileUtils.readFileToString(file, "UTF-8");
			} else {
				log.warn("[" + file
						+ "] doesn't exists,use classpath resources "
						+ DEFAULT_PRIVATE_KEY_LOCATION);
				privateKeyString = IOUtils.toString(PGP.class
						.getResourceAsStream(DEFAULT_PRIVATE_KEY_LOCATION),
						"UTF-8");
			}
			if (StringUtils.isNotBlank(privateKeyString)) {
				secretKeyRingCollection = new PGPSecretKeyRingCollection(
						Collections.EMPTY_LIST);
				secretKeyRing = new PGPSecretKeyRing(new ArmoredInputStream(
						new ByteArrayInputStream(privateKeyString
								.getBytes("UTF-8"))));
				secretKeyRingCollection = PGPSecretKeyRingCollection
						.addSecretKeyRing(secretKeyRingCollection,
								secretKeyRing);
			}

			file = new File(AppInfo.getAppHome() + KEY_DIRECTORY + "pgp-public");
			String publicKeyString;
			if (file.exists()) {
				publicKeyString = FileUtils.readFileToString(file, "UTF-8");
			} else {
				log.warn("[" + file
						+ "] doesn't exists,use classpath resources "
						+ DEFAULT_PUBLIC_KEY_LOCATION);
				publicKeyString = IOUtils.toString(PGP.class
						.getResourceAsStream(DEFAULT_PUBLIC_KEY_LOCATION),
						"UTF-8");
			}
			if (StringUtils.isNotBlank(publicKeyString)) {
				publicKey = parsePGPPublicKey(new ByteArrayInputStream(
						publicKeyString.getBytes("UTF-8")));
				publicKeyRingCollection = parsePGPPublicKeyRingCollection(new ByteArrayInputStream(
						publicKeyString.getBytes("UTF-8")));
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (PGPException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static void encrypt(InputStream in, OutputStream out) {
		try {
			encrypt(in, out, publicKey);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void decrypt(InputStream in, OutputStream out) {
		try {
			decrypt(in, out, secretKeyRingCollection, password);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void sign(InputStream in, OutputStream out) {
		try {
			sign(in, out, secretKeyRing, password);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static boolean verify(InputStream in, InputStream sign) {
		try {
			return verify(in, sign, publicKeyRingCollection);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	public static String encrypt(String str) {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(str
					.getBytes("UTF-8"));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			encrypt(inputStream, outputStream);
			return new String(Base64.encodeBase64(outputStream.toByteArray()),
					"UTF-8");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "";
		}
	}

	public static String decrypt(String str) {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64
					.decodeBase64(str.getBytes("UTF-8")));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			decrypt(inputStream, outputStream);
			return new String(outputStream.toByteArray(), "UTF-8");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "";
		}
	}

	public static String sign(String str) {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(str
					.getBytes("UTF-8"));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			sign(inputStream, outputStream);
			return new String(Base64.encodeBase64(outputStream.toByteArray()),
					"UTF-8");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "";
		}
	}

	public static boolean verify(String input, String sign) {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(input
					.getBytes("UTF-8"));
			ByteArrayInputStream signInputStream = new ByteArrayInputStream(
					Base64.decodeBase64(sign.getBytes("UTF-8")));
			return verify(inputStream, signInputStream);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	/***********************************************/
	/***********************************************/
	/***********************************************/
	/***********************************************/
	/***********************************************/
	/***********************************************/

	public static void main(String... strings) throws Exception {

		String keyId = "zhouyanming";
		generatorKeyPair(keyId, password, new FileOutputStream("pub.key"),
				new FileOutputStream("pri.key"));

		encrypt(new FileInputStream("build.xml"), new FileOutputStream(
				"build.xml.crpt"), new FileInputStream("pub.key"));

		decrypt(new FileInputStream("build.xml.crpt"), new FileOutputStream(
				"build2.xml"), new FileInputStream("pri.key"), password);

		sign(new FileInputStream("build.xml"), new FileOutputStream(
				"build.xml.sign"), new FileInputStream("pri.key"), password);

		boolean is = verify(new FileInputStream("build.xml"),
				new FileInputStream("build.xml.sign"), new FileInputStream(
						"pub.key"));

		System.out.println(is);

		String s = "this is a test";
		s = encrypt(s);
		System.out.println(s);
		s = decrypt(s);
		System.out.println(s);
		String sign = sign(s);
		System.out.println(verify(s, sign));
	}

	/***********************************************/
	/***********************************************/
	/***********************************************/
	/***********************************************/
	/***********************************************/
	/***********************************************/

	public static void generatorKeyPair(String keyId, String password,
			OutputStream pub, OutputStream pri)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			InvalidAlgorithmParameterException, PGPException, IOException {

		KeyPairGenerator dsaKpg = KeyPairGenerator.getInstance("DSA", "BC");
		dsaKpg.initialize(kEY_SIZE);

		KeyPair dsaKp = dsaKpg.generateKeyPair();
		KeyPairGenerator elgKpg = KeyPairGenerator.getInstance("ELGAMAL", "BC");

		// if (STRENGTH > 0) {
		// ElGamalParametersGenerator paramGen = new
		// ElGamalParametersGenerator();
		// paramGen.init(kEY_SIZE, STRENGTH, new SecureRandom());
		// ElGamalParameters genParams = paramGen.generateParameters();
		// ElGamalParameterSpec elParams = new ElGamalParameterSpec(genParams
		// .getP(), genParams.getG());
		// elgKpg.initialize(elParams);
		// } else {
		BigInteger g = new BigInteger(
				"153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc",
				16);
		BigInteger p = new BigInteger(
				"9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b",
				16);
		ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);
		elgKpg.initialize(elParams);
		// }

		KeyPair elgKp = elgKpg.generateKeyPair();

		PGPKeyPair dsaKeyPair = new PGPKeyPair(PublicKeyAlgorithmTags.DSA,
				dsaKp, new Date());
		PGPKeyPair elgKeyPair = new PGPKeyPair(
				PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT, elgKp, new Date());

		PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(
				PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair, keyId,
				SymmetricKeyAlgorithmTags.AES_256, password.toCharArray(),
				true, null, null, new SecureRandom(), "BC");
		keyRingGen.addSubKey(elgKeyPair);

		OutputStream ostream = new ArmoredOutputStream(pub);
		ostream.write(keyRingGen.generatePublicKeyRing().getEncoded());
		ostream.close();
		ostream = new ArmoredOutputStream(pri);
		ostream.write(keyRingGen.generateSecretKeyRing().getEncoded());
		ostream.close();
	}

	public static PGPPublicKey parsePGPPublicKey(InputStream is)
			throws IOException {
		PGPPublicKeyRing ring = new PGPPublicKeyRing(new ArmoredInputStream(is));
		Iterator<PGPPublicKey> iter = ring.getPublicKeys();
		while (iter.hasNext()) {
			PGPPublicKey k = iter.next();
			if (k.isEncryptionKey())
				return k;
		}
		return null;
	}

	public static PGPPublicKeyRingCollection parsePGPPublicKeyRingCollection(
			InputStream is) throws IOException, PGPException {
		PGPPublicKeyRingCollection pubring = new PGPPublicKeyRingCollection(
				Collections.EMPTY_LIST);
		PGPPublicKeyRing newKey = new PGPPublicKeyRing(new ArmoredInputStream(
				is));
		pubring = PGPPublicKeyRingCollection.addPublicKeyRing(pubring, newKey);
		return pubring;
	}

	public static PGPSecretKeyRingCollection parsePGPSecretKeyRingCollection(
			InputStream is) throws IOException, PGPException {
		PGPSecretKeyRingCollection secring = new PGPSecretKeyRingCollection(
				Collections.EMPTY_LIST);
		BufferedInputStream iStream = new BufferedInputStream(is);
		PGPSecretKeyRing ring;
		iStream.mark(1024 * 128);
		ring = new PGPSecretKeyRing(new ArmoredInputStream(iStream));
		secring = PGPSecretKeyRingCollection.addSecretKeyRing(secring, ring);
		return secring;
	}

	public static void encrypt(InputStream in, OutputStream out,
			InputStream keyIs) throws IOException, NoSuchProviderException,
			PGPException {
		encrypt(in, out, keyIs, false, true);
	}

	public static void encrypt(InputStream in, OutputStream out,
			PGPPublicKey key) throws IOException, NoSuchProviderException,
			PGPException {
		encrypt(in, out, key, false, true);
	}

	public static void writeStreamToLiteralData(OutputStream out,
			char fileType, InputStream in) throws IOException {
		PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
		OutputStream pOut = lData.open(out, fileType, "file", new Date(),
				new byte[1024]);
		byte[] buf = new byte[4096];
		int len;

		while ((len = in.read(buf)) > 0) {
			pOut.write(buf, 0, len);
		}
		lData.close();
		in.close();
	}

	public static void encrypt(InputStream in, OutputStream out,
			InputStream keyIs, boolean armor, boolean withIntegrityCheck)
			throws IOException, NoSuchProviderException, PGPException {
		PGPPublicKey encKey = parsePGPPublicKey(keyIs);
		encrypt(in, out, encKey, armor, withIntegrityCheck);
	}

	public static void encrypt(InputStream in, OutputStream out,
			PGPPublicKey publicKey, boolean armor, boolean withIntegrityCheck)
			throws IOException, NoSuchProviderException, PGPException {
		if (armor)
			out = new ArmoredOutputStream(out);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
				CompressionAlgorithmTags.ZIP);
		writeStreamToLiteralData(comData.open(bOut), PGPLiteralData.BINARY, in);
		// PGPUtil.writeFileToLiteralData(comData.open(bOut),
		// PGPLiteralData.BINARY, file);
		comData.close();
		PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
				SymmetricKeyAlgorithmTags.CAST5, withIntegrityCheck,
				new SecureRandom(), "BC");
		cPk.addMethod(publicKey);
		byte[] bytes = bOut.toByteArray();
		OutputStream cOut = cPk.open(out, bytes.length);
		cOut.write(bytes);
		cOut.close();
		out.close();
	}

	public static void decrypt(InputStream data, OutputStream out,
			InputStream keyIs, String password) throws Exception {
		PGPSecretKeyRingCollection secring = parsePGPSecretKeyRingCollection(keyIs);
		decrypt(data, out, secring, password);
	}

	public static void decrypt(InputStream data, OutputStream out,
			PGPSecretKeyRingCollection secring, String password)
			throws Exception {
		PGPPublicKeyEncryptedData pbe = null;
		InputStream in = PGPUtil.getDecoderStream(data);
		PGPObjectFactory pgpF = new PGPObjectFactory(in);
		PGPEncryptedDataList enc;
		Object o = pgpF.nextObject();
		if (o == null)
			throw new Exception("Cannot recognize input data format");
		//
		// the first object might be a PGP marker packet.
		//
		if (o instanceof PGPEncryptedDataList) {
			enc = (PGPEncryptedDataList) o;
		} else {
			enc = (PGPEncryptedDataList) pgpF.nextObject();
		}

		//
		// find the secret key
		//
		Iterator encObjects = enc.getEncryptedDataObjects();
		if (!encObjects.hasNext())
			throw new Exception("No encrypted data");
		pbe = (PGPPublicKeyEncryptedData) encObjects.next();

		PGPPrivateKey sKey = null;
		PGPSecretKey secretKey = secring.getSecretKey(pbe.getKeyID());
		sKey = secretKey.extractPrivateKey(password.toCharArray(), "BC");
		// sKey = findSecretKey(it, passwd);

		InputStream clear = pbe.getDataStream(sKey, "BC");

		PGPObjectFactory plainFact = new PGPObjectFactory(clear);

		Object message = plainFact.nextObject();
		Object sigLiteralData = null;
		PGPObjectFactory pgpFact = null;

		if (message instanceof PGPCompressedData) {
			PGPCompressedData cData = (PGPCompressedData) message;
			pgpFact = new PGPObjectFactory(cData.getDataStream());
			message = pgpFact.nextObject();
			if (message instanceof PGPOnePassSignatureList) {
				sigLiteralData = pgpFact.nextObject();
			}
		}

		if (message instanceof PGPLiteralData) {
			// Message is just encrypted
			processLiteralData((PGPLiteralData) message, out, null);
		} else if (message instanceof PGPOnePassSignatureList) {
			// Message is signed and encrypted
			// ... decrypt without checking signature
			processLiteralData((PGPLiteralData) sigLiteralData, out, null);
		} else
			throw new PGPException(
					"message is not a simple encrypted file - type unknown.");

		if (pbe.isIntegrityProtected())
			if (!pbe.verify())
				throw new Exception("Message failed integrity check");
	}

	private static String processLiteralData(PGPLiteralData ld,
			OutputStream out, PGPOnePassSignature ops) throws IOException,
			SignatureException {
		String outFileName = ld.getFileName();
		InputStream unc = ld.getInputStream();
		int ch;
		if (ops == null) {
			while ((ch = unc.read()) >= 0)
				out.write(ch);
		} else {
			while ((ch = unc.read()) >= 0) {
				out.write(ch);
				ops.update((byte) ch);
			}
		}
		return outFileName;
	}

	public static void sign(InputStream in, OutputStream out,
			InputStream keyIs, String password) throws IOException,
			NoSuchAlgorithmException, NoSuchProviderException, PGPException,
			SignatureException {
		BufferedInputStream iStream = new BufferedInputStream(keyIs);
		PGPSecretKeyRing ring;
		iStream.mark(1024 * 128);
		ring = new PGPSecretKeyRing(new ArmoredInputStream(iStream));
		sign(in, out, ring, password);
	}

	public static void sign(InputStream in, OutputStream out,
			PGPSecretKeyRing ring, String password) throws IOException,
			NoSuchAlgorithmException, NoSuchProviderException, PGPException,
			SignatureException {
		PGPSecretKey key = ring.getSecretKey();
		PGPPrivateKey priK = key
				.extractPrivateKey(password.toCharArray(), "BC");

		PGPSignatureGenerator sGen = new PGPSignatureGenerator(key
				.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1, "BC");

		sGen.initSign(PGPSignature.BINARY_DOCUMENT, priK);

		Iterator it = key.getPublicKey().getUserIDs();
		if (it.hasNext()) {
			PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
			spGen.setSignerUserID(false, (String) it.next());
			sGen.setHashedSubpackets(spGen.generate());
		}

		BCPGOutputStream bOut = new BCPGOutputStream(out);
		int rSize = 0;
		byte[] buf = new byte[1024];

		while ((rSize = in.read(buf)) >= 0)
			sGen.update(buf, 0, rSize);

		PGPSignature sig = sGen.generate();
		sig.encode(bOut);

	}

	public static boolean verify(InputStream dataIn, InputStream in,
			InputStream keyIs) throws Exception {
		PGPPublicKeyRingCollection pubring = parsePGPPublicKeyRingCollection(keyIs);
		return verify(dataIn, in, pubring);
	}

	public static boolean verify(InputStream dataIn, InputStream in,
			PGPPublicKeyRingCollection pubring) throws Exception {
		in = PGPUtil.getDecoderStream(in);
		// dataIn = PGPUtil.getDecoderStream(dataIn);
		PGPObjectFactory pgpFact = new PGPObjectFactory(in);
		PGPSignatureList p3 = null;

		Object o;

		try {
			o = pgpFact.nextObject();
			if (o == null)
				throw new Exception();
		} catch (Exception ex) {
			throw new Exception("Invalid input data");
		}

		if (o instanceof PGPCompressedData) {
			PGPCompressedData c1 = (PGPCompressedData) o;

			pgpFact = new PGPObjectFactory(c1.getDataStream());

			p3 = (PGPSignatureList) pgpFact.nextObject();
		} else {
			p3 = (PGPSignatureList) o;
		}

		int ch;

		PGPSignature sig = p3.get(0);
		PGPPublicKey key = pubring.getPublicKey(sig.getKeyID());
		if (key == null)
			throw new Exception("Cannot find key 0x"
					+ Integer.toHexString((int) sig.getKeyID()).toUpperCase()
					+ " in the pubring");

		sig.initVerify(key, "BC");

		while ((ch = dataIn.read()) >= 0)
			sig.update((byte) ch);

		if (sig.verify())
			return true;
		else
			return false;
	}

}
