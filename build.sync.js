try {
	load('nashorn:mozilla_compat.js');
} catch (e) {
}
importPackage(java.io);
if (!new File(basedir + '/../ironrhino').isDirectory()) {
	print("	directory ../ironrhino doesn't exists");
} else {
	var is = new FileInputStream(new File(basedir + '/../ironrhino/.dependence'));
	var br = new BufferedReader(new InputStreamReader(is, 'utf-8'));
	var increment = [];
	var decrement = [];
	var line;
	while (line = br.readLine()) {
		if (line.indexOf('+') == 0) {
			increment.push(line.substring(1));
		} else if (line.indexOf('-') == 0) {
			decrement.push(line.substring(1));
		}
	}
	br.close();
	is.close();
	var classpathfile = new File(basedir + '/.classpath');
	is = new FileInputStream(classpathfile);
	br = new BufferedReader(new InputStreamReader(is, 'utf-8'));
	var lines = [];
	while ((line = br.readLine()) != null) {
		if (line.indexOf('kind="lib"') < 0)
			continue;
		lines.push(line);
	}
	br.close();
	is.close();
	var replacement = [];
	for (var n = 0; n < lines.length; n++) {
		var line = lines[n];
		var index = line.indexOf('path="') + 6;
		var path = line.substring(index, line.indexOf('"', index));
		var arr2 = path.split('/');
		var filename = arr2[arr2.length - 1];
		var jarname = filename.substring(0, filename.lastIndexOf(filename
						.lastIndexOf('-') > 0 ? '-' : '.'));
		var version = filename.substring(jarname.length() + 1);
		copy = project.createTask("copy");
		var file = new File(basedir
				+ '/../ironrhino/'
				+ (filename.startsWith('ironrhino')
						? 'target/' + filename
						: path));
		var tofile = new File(basedir, path);
		if (!file.exists()) {
			var parent = file.getParentFile();
			if (!parent.exists())
				continue;
			var files = parent.listFiles();
			for (var i = 0; i < files.length; i++) {
				var f = files[i];
				var filename2 = f.getName();
				var jarname2 = filename2.substring(0,
						filename2.lastIndexOf(filename2.lastIndexOf('-') > 0
								? '-'
								: '.'));
				var version2 = filename2.substring(jarname2.length() + 1);
				if (f.isFile() && filename != filename2 && jarname == jarname2
						&& version2.length() > 0) {
					if (version.length() > 4)
						version = version.substring(0, version.length() - 4);
					if (version2.length() > 4)
						version2 = version2.substring(0, version2.length() - 4);
					var verarr1 = version.split("\\.");
					var verarr2 = version2.split("\\.");
					var upgrade = false;
					for (var j = 0; j < verarr2.length; j++) {
						if (j == verarr1.length || verarr2[j] > verarr1[j]
								|| verarr2[j].length() > verarr1[j].length()) {
							upgrade = true;
							break;
						}
					}
					if (!upgrade)
						continue;
					print('	[sync] Upgrading ' + filename + ' to '
							+ f.getName());
					file = f;
					if (tofile.exists()) {
						var del = project.createTask("delete");
						del.setFile(tofile);
						del.perform();
					}
					tofile = new File(basedir, path.replaceAll(filename, f
											.getName()));
					replacement.push(filename);
					replacement.push(f.getName());
					break;
				}
			}
		}
		if (!file.exists()) {
			file = new File(basedir + '/extralib', filename);
			var parent = file.getParentFile();
			if (!file.exists() && parent.exists()) {
				var files = parent.listFiles();
				for (var i = 0; i < files.length; i++) {
					var f = files[i];
					var filename2 = f.getName();
					var jarname2 = filename2.substring(0, filename2
									.lastIndexOf(filename2.lastIndexOf('-') > 0
											? '-'
											: '.'));
					var version2 = filename2.substring(jarname2.length() + 1);
					if (f.isFile() && filename != filename2
							&& jarname == jarname2 && version2.length() > 0) {
						if (version.length() > 4)
							version = version
									.substring(0, version.length() - 4);
						if (version2.length() > 4)
							version2 = version2.substring(0, version2.length()
											- 4);
						var verarr1 = version.split("\\.");
						var verarr2 = version2.split("\\.");
						var upgrade = false;
						for (var j = 0; j < verarr2.length; j++) {
							if (j == verarr1.length
									|| verarr2[j] > verarr1[j]
									|| verarr2[j].length() > verarr1[j]
											.length()) {
								upgrade = true;
								break;
							}
						}
						if (!upgrade)
							continue;
						print('	[sync] Upgrading ' + filename + ' to '
								+ f.getName());
						file = f;
						if (tofile.exists()) {
							var del = project.createTask("delete");
							del.setFile(tofile);
							del.perform();
						}
						tofile = new File(basedir, path.replaceAll(filename, f
												.getName()));
						replacement.push(filename);
						replacement.push(f.getName());
					}
				}
			}
		}
		copy.setFile(file);
		copy.setTofile(tofile);
		copy.setPreserveLastModified(true);
		copy.setOverwrite(true);
		if (file.exists()
				&& (!tofile.exists() || file.length() != tofile.length()))
			copy.perform();
	}
	for (var i = 0; i < replacement.length; i += 2) {
		var replace = project.createTask("replace");
		replace.setFile(classpathfile);
		replace.setToken(replacement[i]);
		replace.setValue(replacement[i + 1]);
		replace.perform();
	}
	label : for (var i = 0; i < increment.length; i++) {
		var candidate = increment[i];
		var j = candidate.indexOf('<-');
		if (j > 0) {
			var dependent = candidate.substring(j + 2);
			candidate = candidate.substring(0, j);
			var exists = false;
			for (var n = 0; n < lines.length; n++) {
				if (lines[n].match(new RegExp(dependent + '-[\\d.]+\\.jar'))) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				decrement.push(candidate);
				continue label;
			}
		}
		for (var n = 0; n < lines.length; n++) {
			if (lines[n].match(new RegExp(candidate + '-[\\d.]+\\.jar'))) {
				continue label;
			}
		}
		var files = new File(basedir + '/../ironrhino/webapp/WEB-INF/lib')
				.listFiles();
		for (var j = 0; j < files.length; j++) {
			var f = files[j];
			var filename = f.getName();
			var token = '	<classpathentry kind="output"';
			var copy = project.createTask('copy');
			var replace = project.createTask('replace');
			if (filename.match(new RegExp(candidate + '-[\\d.]+\\.jar'))
					&& !new File(basedir + '/webapp/WEB-INF/lib/' + filename)
							.exists()) {
				print('	[sync] Adding ' + filename);
				copy.setFile(f);
				copy.setTofile(new File(basedir + '/webapp/WEB-INF/lib/'
						+ filename));
				copy.setPreserveLastModified(true);
				copy.perform();
				var line = '	<classpathentry kind="lib" path="webapp/WEB-INF/lib/'
						+ filename + '"/>\n';
				replace.setFile(classpathfile);
				replace.setToken(token);
				replace.setValue(line + token);
				replace.perform();
			}
		}
	}
	for (var i = 0; i < decrement.length; i++) {
		var replaceregexp = project.createTask('replaceregexp');
		var match = '^.*' + decrement[i] + '-[\\d.]+\\.jar.*$';
		replaceregexp.setFile(classpathfile);
		replaceregexp.setMatch(match);
		replaceregexp.setReplace('');
		replaceregexp.setByLine(true);
		replaceregexp.perform();
	}
	var antcall = project.createTask('antcall');
	antcall.setTarget('refineclasspathfile');
	antcall.perform();
	// clean unused jar
	is = new FileInputStream(new File(basedir + '/.classpath'));
	br = new BufferedReader(new InputStreamReader(is, 'utf-8'));
	var jarnames = [];
	while ((line = br.readLine()) != null) {
		if (line.indexOf('kind="lib"') < 0)
			continue;
		var index = line.indexOf('path="') + 6;
		var path = line.substring(index, line.indexOf('"', index));
		var arr2 = path.split('/');
		if (arr2[0] == 'webapp')
			jarnames.push(arr2[arr2.length - 1]);
	}
	br.close();
	is.close();
	var lib = new File(basedir, 'webapp/WEB-INF/lib');
	var files = lib.listFiles();
	for (var i = 0; i < files.length; i++) {
		var f = files[i];
		var jarname = f.getName();
		var contains = false;
		for (var j = 0; j < jarnames.length; j++) {
			if (jarnames[j].equals(jarname)) {
				contains = true;
				break;
			}
		}
		if (jarname.endsWith('.jar') && !contains) {
			var del = project.createTask("delete");
			del.setFile(f);
			del.perform();
		}
	}
}