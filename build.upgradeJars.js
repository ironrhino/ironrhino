var File = Java.type('java.io.File');
var FileInputStream = Java.type('java.io.FileInputStream');
var InputStreamReader = Java.type('java.io.InputStreamReader');
var BufferedReader = Java.type('java.io.BufferedReader');
var classpathfile = new File(project.getProperty('basedir') + '/.classpath');
var is = new FileInputStream(classpathfile);
var br = new BufferedReader(new InputStreamReader(is, 'utf-8'));
var paths = [];
var line;
while (line = br.readLine()) {
	if (line.indexOf('kind="lib"') < 0)
		continue;
	var index = line.indexOf('path="') + 6;
	var path = line.substring(index, line.indexOf('"', index));
	paths.push(path);
}
br.close();
is.close();
var replacement = [];
for (var n = 0; n < paths.length; n++) {
	var path = paths[n];
	var arr2 = path.split('/');
	var filename = arr2[arr2.length - 1];
	var fileInfo = getFileInfo(filename);
	var jarname = fileInfo.jarname;
	var version = fileInfo.version;
	var file = new File(basedir + '/' + path);
	var parent = file.getParentFile();
	var files = parent.listFiles();
	if (!files)
		continue;
	for (var i = 0; i < files.length; i++) {
		var f = files[i];
		var filename2 = trimFilename(f);
		var fileInfo2 = getFileInfo(filename2);
		var jarname2 = fileInfo2.jarname;
		var version2 = fileInfo2.version;
		if (f.isFile() && filename != filename2 && jarname == jarname2
			&& version2.length > 0) {
			if (!compareVersion(version, version2))
				continue;
			print('upgrade jar from ' + filename + ' to ' + filename2 + '\n');
			if (file.exists()) {
				var del = project.createTask('delete');
				del.setFile(file);
				del.perform();
			}
			replacement.push(filename);
			replacement.push(f.getName());
			break;
		}
	}
}
for (var i = 0; i < replacement.length; i += 2) {
	var replace = project.createTask('replace');
	replace.setFile(classpathfile);
	replace.setToken(replacement[i]);
	replace.setValue(replacement[i + 1]);
	replace.perform();
}

function trimFilename(file) {
	var filename = file.getName();
	if (filename.toLowerCase().endsWith('.release.jar')
		|| filename.toLowerCase().endsWith('.final.jar')) {
		filename = filename.substring(0, filename.lastIndexOf('.'));
		filename = filename.substring(0, filename.lastIndexOf('.')) + '.jar';
		file.renameTo(new File(file.getParentFile(), filename));
	}
	return filename;
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