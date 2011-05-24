package bit.lin.myimpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;

import proj.zoie.api.DataConsumer.DataEvent;
import proj.zoie.api.DefaultZoieVersion;
import proj.zoie.impl.indexing.StreamDataProvider;

public class MyDataProvider extends
		StreamDataProvider<MyDoc, DefaultZoieVersion> {
	private File _dir;
	private Stack<Iterator<File>> _stack;
	private Iterator<File> _iterator;
	private boolean _looping;
	private DefaultZoieVersion _version = new DefaultZoieVersion();

	public MyDataProvider(File dir) {
		if (!dir.exists()) {
			throw new IllegalArgumentException("dir: " + dir
					+ " does not exist.");
		}

		_dir = dir;
		_stack = new Stack<Iterator<File>>();
		_looping = false;
		reset();
	}

	private DefaultZoieVersion nextZoieVersion() {
		_version.setVersionId(_version.getVersionId() + 1);
		return _version;
	}

	@Override
	public DataEvent<MyDoc, DefaultZoieVersion> next() {
		if (_iterator.hasNext()) {
			File f = _iterator.next();
			if (f.isFile()) {
				String fname = f.getName();
				System.out.println("file found: " + fname);
				MyDoc doc = new MyDoc(f.getName(), getFileContentAndBackup(f),
						new Date(), nextZoieVersion().getVersionId());

				if (f.delete())
					System.out.println("file deleted: " + fname);

				return new DataEvent<MyDoc, DefaultZoieVersion>(doc, _version);
			} else {
				_stack.push(_iterator);
				_iterator = Arrays.asList(f.listFiles()).iterator();
				return next();
			}
		} else {
			if (_stack.isEmpty()) {
				if (_looping) {
					reset();
					return next();
				} else {
					 reset();
					return null;
				}
			} else {
				_iterator = _stack.pop();
				return next();
			}
		}
	}

	private String getFileContentAndBackup(File f) {
		File backup = new File("./backup/" + f.getName());

		String content = "", tmp;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));

			tmp = br.readLine();
			while (tmp != null) {
				content += tmp;
				tmp = br.readLine();
			}

			if (!backup.exists()) {
				backup.createNewFile();
				BufferedWriter bw = new BufferedWriter(new FileWriter(backup));
				bw.write(content);
				bw.flush();
				bw.close();
				System.out.println("file backup: " + backup.getName());
			}

		} catch (IOException e) {
		}
		return content;
	}

	@Override
	public void setStartingOffset(DefaultZoieVersion version) {
		throw new UnsupportedOperationException("");

	}

	@Override
	public void reset() {
		_stack.clear();
		if (_dir.isFile()) {
			_iterator = Arrays.asList(new File[] { _dir }).iterator();
		} else {
			_iterator = Arrays.asList(_dir.listFiles()).iterator();
		}
	}

}
