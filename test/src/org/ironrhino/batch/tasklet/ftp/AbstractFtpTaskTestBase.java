package org.ironrhino.batch.tasklet.ftp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

public abstract class AbstractFtpTaskTestBase {

	static final String FTP_URI = "ftp://admin:admin@localhost:2121/temp";

	public static <T extends AbstractFtpTask> T createTask(Class<T> clazz) throws Exception {
		T task = (T) clazz.getConstructor().newInstance();
		task.setUri(new URI(FTP_URI));
		return task;
	}

	protected static File createAndWrite(int lines) throws IOException {
		File temp = File.createTempFile("test", ".txt");
		temp.deleteOnExit();
		try (FileWriter fw = new FileWriter(temp)) {
			for (int i = 0; i < lines; i++) {
				fw.write(String.valueOf(i));
				if (i < lines - 1)
					fw.write("\n");
			}
		}
		return temp;
	}

}
