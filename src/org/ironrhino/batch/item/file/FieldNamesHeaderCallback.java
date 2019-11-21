package org.ironrhino.batch.item.file;

import java.io.IOException;
import java.io.Writer;

import org.springframework.batch.item.file.FlatFileHeaderCallback;

import lombok.Setter;

public class FieldNamesHeaderCallback implements FlatFileHeaderCallback {

	@Setter
	private String[] names;

	@Setter
	private String delimiter = ",";

	@Override
	public void writeHeader(Writer writer) throws IOException {
		writer.write(String.join(delimiter, names));
	}

}