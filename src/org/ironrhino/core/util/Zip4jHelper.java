package org.ironrhino.core.util;

import java.io.IOException;
import java.io.OutputStream;

import lombok.experimental.UtilityClass;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

@UtilityClass
public class Zip4jHelper {

	public static OutputStream wrapStreamWithPassword(OutputStream targetOutputStream, String fileName, String password)
			throws IOException {
		ZipParameters parameters = new ZipParameters();
		parameters.setEncryptFiles(true);
		parameters.setEncryptionMethod(EncryptionMethod.AES);
		ZipOutputStream zos = new ZipOutputStream(targetOutputStream, password.toCharArray());
		parameters.setFileNameInZip(fileName);
		zos.putNextEntry(parameters);
		return zos;
	}
}
