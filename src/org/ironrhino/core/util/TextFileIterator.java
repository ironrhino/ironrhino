package org.ironrhino.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TextFileIterator<T> implements Iterator<T> {

	int currentIndex;

	File currentFile;

	List<File> files = new ArrayList<File>();

	String encoding;

	FileInputStream fis;

	BufferedReader br;

	String nextline;

	public TextFileIterator(File... files) {
		for (File f : files)
			if (f != null && f.exists() && f.length() > 0)
				this.files.add(f);
		if (this.files.size() == 0)
			throw new RuntimeException("no file available");
		try {
			openFile();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public TextFileIterator(String encoding, File... files) {
		this.encoding = encoding;
		for (File f : files)
			if (f != null && f.exists())
				this.files.add(f);
		try {
			if (this.files.size() > 0)
				openFile();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void openFile() {
		try {
			currentFile = files.get(currentIndex);
			fis = new FileInputStream(currentFile);
			if (encoding == null)
				br = new BufferedReader(new InputStreamReader(fis));
			else
				br = new BufferedReader(new InputStreamReader(fis, encoding));
			nextline = br.readLine();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		if (this.files.size() == 0)
			return false;
		return nextline != null || currentIndex < files.size() - 1;
	}

	@Override
	public T next() {
		if (this.files.size() == 0)
			return null;
		try {
			if (nextline == null && currentIndex < files.size() - 1) {
				currentIndex++;
				openFile();
			}
			String result = nextline;
			if (nextline != null) {
				nextline = br.readLine();
				if (nextline == null) {
					br.close();
					fis.close();
				}
			}
			return transform(result, currentFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	protected T transform(String line, File f) {
		// hook to be override
		return (T) line;
	}

}