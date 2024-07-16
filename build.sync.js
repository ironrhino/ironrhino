var File = Java.type('java.io.File');
var FileInputStream = Java.type('java.io.FileInputStream');
var InputStreamReader = Java.type('java.io.InputStreamReader');
var BufferedReader = Java.type('java.io.BufferedReader');
if (!new File(basedir + '/../ironrhino').isDirectory()) {
	print('     directory ../ironrhino doesn\'t exists');
} else {

	var ironrhinoPaths = readClasspath(new File(basedir
		+ '/../ironrhino/.classpath')).paths;
	var classpathfile = new File(basedir + '/.classpath');
	var cp = readClasspath(classpathfile);
	var paths = cp.paths;
	var jarnames = cp.jarnames;

	var replacement = [];
	for (var i = 0; i < paths.length; i++)
		upgradeDependence(paths[i], replacement);

	for (var i = 0; i < replacement.length; i += 2) {
		var replace = project.createTask('replace');
		replace.setFile(classpathfile);
		replace.setToken(replacement[i]);
		replace.setValue(replacement[i + 1]);
		replace.perform();
	}

	var dependence = resolveDependence(['../ironrhino/.dependence',
		'.dependence']);
	var increment = dependence.increment;
	var decrement = dependence.decrement;

	label: for (var i = 0; i < increment.length; i++) {
		var candidate = increment[i];
		if (decrement.indexOf(candidate) > -1)
			continue;
		var j = candidate.indexOf('<-');
		if (j > 0) {
			var dependent = candidate.substring(j + 2);
			candidate = candidate.substring(0, j);
			var dependents = dependent.split(',');
			var satisfied = true;
			for (var k = 0; k < dependents.length; k++) {
				if (jarnames.indexOf(dependents[k]) < 0) {
					satisfied = false;
					break;
				}
			}
			if (!satisfied) {
				decrement.push(candidate);
				continue label;
			}
		}
		if (jarnames.indexOf(candidate) < 0) {
			for (var n = 0; n < ironrhinoPaths.length; n++) {
				var path = ironrhinoPaths[n];
				if (getFileInfo(path.substring(path.lastIndexOf('/') + 1)).jarname == candidate) {
					jarnames.push(candidate);
					addDependence(classpathfile, path);
					continue label;
				}
			}
		}
	}

	for (var i = 0; i < decrement.length; i++)
		removeDependence(classpathfile, decrement[i]);

	cleanup(classpathfile);
}

function readClasspath(classpathfile) {
	is = new FileInputStream(classpathfile);
	br = new BufferedReader(new InputStreamReader(is, 'utf-8'));
	var paths = [];
	var jarnames = [];
	var line;
	while ((line = br.readLine()) != null) {
		if (line.indexOf('kind="lib"') < 0)
			continue;
		var index = line.indexOf('path="') + 6;
		var path = line.substring(index, line.indexOf('"', index));
		var arr2 = path.split('/');
		paths.push(path);
		jarnames.push(getFileInfo(arr2[arr2.length - 1]).jarname);
	}
	br.close();
	is.close();
	return {
		paths: paths,
		jarnames: jarnames
	};
}

function resolveDependence(paths) {
	var increment = [];
	var decrement = [];
	for (var i = 0; i < paths.length; i++) {
		var file = new File(basedir + '/' + paths[i]);
		if (file.exists()) {
			var is = new FileInputStream(file);
			var br = new BufferedReader(new InputStreamReader(is, 'utf-8'));
			var line;
			while (line = br.readLine()) {
				var artifact = line.substring(1);
				if (line.indexOf('+') == 0) {
					increment.push(artifact);
					removeItem(decrement, artifact);
				} else if (line.indexOf('-') == 0) {
					decrement.push(artifact);
					removeItem(increment, artifact);
				}
			}
			br.close();
			is.close();
		}
	}
	return {
		increment: increment,
		decrement: decrement
	}
}

function upgradeDependence(path, replacement) {
	var filename = path.substring(path.lastIndexOf('/') + 1);
	var fileInfo = getFileInfo(filename);
	var jarname = fileInfo.jarname;
	var version = fileInfo.version;
	var copy = project.createTask('copy');
	var file = new File(basedir + '/../ironrhino/'
		+ (filename.startsWith('ironrhino') ? 'target/' + filename : path));
	var tofile = new File(basedir, path);
	var func = function() {
		var parent = file.getParentFile();
		if (!parent.exists())
			return;
		var files = parent.listFiles();
		for (var i = 0; i < files.length; i++) {
			var f = files[i];
			var filename2 = f.getName();
			if (filename2.endsWith('-sources.jar'))
				continue;
			var fileInfo2 = getFileInfo(filename2);
			var jarname2 = fileInfo2.jarname;
			var version2 = fileInfo2.version;
			if (f.isFile() && filename != filename2 && jarname == jarname2
				&& version2.length > 0) {
				if (!compareVersion(version, version2))
					continue;
				print('     [sync] Upgrading ' + filename + ' to ' + f.getName());
				file = f;
				if (tofile.exists()) {
					var del = project.createTask('delete');
					del.setFile(tofile);
					del.perform();
				}
				tofile = new File(basedir, path.replaceAll(filename, filename2));
				replacement.push(filename);
				replacement.push(filename2);
				break;
			}
		}
	};
	if (!file.exists()) {
		func();
	}
	if (!file.exists()) {
		file = new File(basedir + '/extralib', filename);
		func();
	}
	copy.setFile(file);
	copy.setTofile(tofile);
	copy.setPreserveLastModified(true);
	copy.setOverwrite(true);
	if (file.exists() && (!tofile.exists() || file.length() != tofile.length()))
		copy.perform();
}

function addDependence(classpathfile, path) {
	var f = new File(basedir + '/../ironrhino/' + path);
	var filename = f.getName();
	print('     [sync] Adding ' + filename);
	var copy = project.createTask('copy');
	copy.setFile(f);
	copy.setTofile(new File(basedir + '/' + path));
	copy.setPreserveLastModified(true);
	copy.setOverwrite(true);
	copy.perform();
	var line = '	<classpathentry kind="lib" path="' + path + '"/>\n';
	var token = '	<classpathentry kind="output"';
	var replace = project.createTask('replace');
	replace.setFile(classpathfile);
	replace.setToken(token);
	replace.setValue(line + token);
	replace.perform();
}

function removeDependence(classpathfile, jarname) {
	var replaceregexp = project.createTask('replaceregexp');
	replaceregexp.setFile(classpathfile);
	replaceregexp.setMatch('^.*' + jarname + '-[\\d.]+\\.jar.*$');
	replaceregexp.setReplace('');
	replaceregexp.setByLine(true);
	replaceregexp.perform();
}

function cleanup(classpathfile) {
	var paths = readClasspath(classpathfile).paths;
	var lib = new File(basedir, 'webapp/WEB-INF/lib');
	var func = function() {
		if (!lib.isDirectory())
			return;
		var files = lib.listFiles();
		for (var i = 0; i < files.length; i++) {
			var f = files[i];
			if (!f.getName().endsWith('.jar'))
				continue;
			var exists = false;
			for (var j = 0; j < paths.length; j++) {
				if (paths[j].indexOf(f.getName()) >= 0) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				var del = project.createTask('delete');
				del.setFile(f);
				del.perform();
			}
		}
	}
	func();
	lib = new File(basedir, 'lib');
	func();
	lib = new File(basedir, 'extralib');
	func();

	is = new FileInputStream(classpathfile);
	br = new BufferedReader(new InputStreamReader(is, 'utf-8'));
	var hasBlankLine = false;
	while ((line = br.readLine()) != null) {
		if (!line) {
			hasBlankLine = true;
			break;
		}
	}
	br.close();
	is.close();
	if (hasBlankLine) {
		var antcall = project.createTask('antcall');
		antcall.setTarget('refineclasspathfile');
		antcall.perform();
	}
}

function getFileInfo(filename) {
	if (filename.indexOf('.') > 0)
		filename = filename.substring(0, filename.lastIndexOf('.'));
	var jarname = filename, version = '';
	var i = filename.search(/\-\d+/);
	if(i > 0) {
		jarname = filename.substring(0, i);
		version = filename.substring(i + 1);
	}
	return {
		jarname: jarname,
		version: version
	};
}

function compareVersion(v1, v2) {
	var verarr1 = v1.split('\.');
	var verarr2 = v2.split('\.');
	for (var i = 0; i < verarr2.length; i++) {
		if (i >= verarr1.length)
			return true;
		var a = parseInt(verarr1[i]);
		var b = parseInt(verarr2[i]);
		if (a != b)
			return a < b;
	}
	return v1 != v2;
}

function removeItem(arr, item) {
	var index = arr.indexOf(item);
	if (index > -1) {
		arr.splice(index, 1);
	}
	return arr;
}