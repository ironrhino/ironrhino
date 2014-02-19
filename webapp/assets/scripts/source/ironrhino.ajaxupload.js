(function($) {
	$.ajaxupload = function(files, options) {
		if (!files)
			return false;
		var _options = {
			url : document.location.href,
			name : 'file'
		};
		options = options || {};
		$.extend(_options, options);
		options = _options;
		var progress;
		if (!options.progress) {
			progress = $('#_uploadprogress');
			if (!progress.length)
				progress = $('<progress id="_uploadprogress" style="position: fixed;z-index: 10001;left: 45%;top: 0px;width: 100px;" min="0" max="100" value="0">0</progress>')
						.appendTo(document.body);
		} else {
			progress = $(options.progress);
		}
		var xhr = new XMLHttpRequest();
		xhr.open('POST', options.url);
		xhr.onreadystatechange = function() {
			if (xhr.readyState == 4) {
				if ((xhr.status >= 200 && xhr.status <= 200)
						|| xhr.status == 304) {
					if (xhr.responseText != '') {
						if (typeof options['success'] != 'undefined')
							options['success'](xhr);
					}
				}
			}
		}
		if (progress && progress.length) {
			xhr.onload = function() {
				progress.val(100).html(100).remove();
			};
			if ("upload" in xhr) {
				xhr.upload.onprogress = function(event) {
					if (event.lengthComputable) {
						var complete = (event.loaded / event.total * 100 | 0);
						progress.val(complete).html(complete).show();
					}
				}
			}
		}
		if (typeof options.data == 'string') {
			var arr = options.data.split('&');
			options.data = {};
			for (var i = 0; i < arr.length; i++) {
				var arr2 = arr[i].split('=', 2);
				options.data[arr2[0]] = arr2.length == 2 ? arr2[1] : '';
			}
		}
		if (!!window.FormData) {
			var formData = new FormData();
			for (var i = 0; i < files.length; i++)
				formData.append(options.name, files[i]);
			if (options.data)
				$.each(options.data, function(k, v) {
							formData.append(k, v);
						});
			xhr.send(formData);
			return true;
		} else {
			var boundary = 'xxxxxxxxx';
			xhr.setRequestHeader('Content-Type',
					'multipart/form-data, boundary=' + boundary);
			if (!window.BlobBuilder && window.WebKitBlobBuilder)
				window.BlobBuilder = window.WebKitBlobBuilder;
			if (typeof FileReader != 'undefined'
					&& typeof BlobBuilder != 'undefined') {
				for (var i = 0; i < files.length; i++) {
					var f = files[i];
					var reader = new FileReader();
					reader.sourceFile = f;
					var completed = 0;
					var boundary = 'xxxxxxxxx';
					var body = new BlobBuilder();
					if (options.data) {
						$.each(options.data, function(k, v) {
							var bb = new BlobBuilder();
							bb.append('--');
							bb.append(boundary);
							bb.append('\r\n');
							bb.append('Content-Disposition: form-data; name="');
							bb.append(k);
							bb.append('" ');
							bb.append(v);
							bb.append('\r\n');
							body.append(bb.getBlob());
						});
					}
					reader.onload = function(evt) {
						var f = evt.target.sourceFile;
						var bb = new BlobBuilder();
						bb.append('--');
						bb.append(boundary);
						bb.append('\r\n');
						bb.append('Content-Disposition: form-data; name=');
						bb.append(options.name);
						bb.append('; filename=');
						bb.append(f.name);
						bb.append('\r\n');
						bb.append('Content-Type: ');
						bb.append(f.type);
						bb.append('\r\n\r\n');
						bb.append(evt.target.result);
						bb.append('\r\n');
						body.append(bb.getBlob());
						completed++;
						if (completed == files.length) {
							body.append('--');
							body.append(boundary);
							body.append('--');
							if (typeof options['beforeSend'] != 'undefined')
								options['beforeSend']();
							xhr.send(body.getBlob());
						}
					};
					reader.readAsArrayBuffer(f);
				}
				return true;
			}
			body = compose(files, options, boundary);
			if (body) {
				if (typeof options['beforeSend'] != 'undefined')
					options['beforeSend']();
				if (xhr.sendAsBinary)
					xhr.sendAsBinary(body);
				else
					xhr.send(body);
				return true;
			} else {
				xhr.abort();
			}
			return true;
		}
	}

	function compose(files, options, boundary) {
		var name = options.name;
		if (typeof FileReaderSync != 'undefined') {
			var bb = new BlobBuilder();
			var frs = new FileReaderSync();
			var files = event.data;
			for (var i = 0; i < files.length; i++) {
				bb.append('--');
				bb.append(boundary);
				bb.append('\r\n');
				bb.append('Content-Disposition: form-data; name=');
				bb.append(name);
				bb.append('; filename=');
				bb.append(files[i].name);
				bb.append('\r\n');
				bb.append('Content-Type: ');
				bb.append(files[i].type);
				bb.append('\r\n\r\n');
				bb.append(frs.readAsArrayBuffer(files[i]));
				bb.append('\r\n');
			}
			bb.append('--');
			bb.append(boundary);
			bb.append('--');
			return bb.getBlob();
		} else if (files[0].getAsBinary) {
			var body = '';
			for (var i = 0; i < files.length; i++) {
				body += '--' + boundary + '\r\n';
				body += 'Content-Disposition: form-data; name=' + name
						+ '; filename=' + files[i].name + '\r\n';
				body += 'Content-Type: ' + files[i].type + '\r\n\r\n';
				body += files[i].getAsBinary() + '\r\n';
			}
			body += '--' + boundary + '--';
			return body;
		} else {
			return null;
		}
	}

})(jQuery);
