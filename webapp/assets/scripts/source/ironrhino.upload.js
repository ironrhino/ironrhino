Initialization.upload = function() {
	$(document).on('click', '#files button.reload', function() {
				ajax({
							type : $('#upload_form').attr('method'),
							url : $('#upload_form').attr('action'),
							data : $('#upload_form').serialize(),
							replacement : 'files'
						});
			}).on('click', '#files button.mkdir', function() {
		$.alerts.prompt('', 'newfolder', '', function(t) {
					if (t) {
						var folder = $('#current_folder').text() + t;
						var url = CONTEXT_PATH + '/common/upload/mkdir'
								+ encodeURI(folder);
						ajax({
									url : url,
									dataType : 'json',
									success : function() {
										$('#folder').val(folder);
										$('#files button.reload').click();
										if (typeof history.pushState != 'undefined') {
											var url = CONTEXT_PATH
													+ '/common/upload/list'
													+ encodeURI(folder)
											history.pushState(url, '', url);
										}
									}
								});
					}
				});
	}).on('click', '#files button.snapshot', function() {
		$.snapshot({
			onsnapshot : function(canvas, timestamp) {
				var filename = 'snapshot_'
						+ new Date(timestamp).format('%Y%m%d%H%M%S') + '.png';
				var file;
				if (canvas && canvas.mozGetAsFile)
					uploadFiles([canvas.mozGetAsFile(filename)]);
				else if (canvas && canvas.toBlob)
					canvas.toBlob(function(blob) {
								uploadFiles([blob], [filename]);
							}, 'image/png');
				else
					uploadFiles([dataURLtoBlob(canvas.toDataURL())], [filename]);
			},
			onerror : function(msg) {
				Message.showError(msg);
			}
		});
	}).on('click', '#files button.delete', function() {
				if (!$('#files tbody input:checked').length)
					Message.showMessage('no.selection');
				else
					deleteFiles()
			}).on('keyup', '#files input.filter', function(event) {
		var tbody = $('tbody', $(event.target).closest('table'));
		var keyword = this.value.toLowerCase();
		if (event.keyCode == 8) {
			if (!keyword)
				$('tr:hidden', tbody).show();
			else
				$('tr:hidden', tbody).each(function(i, v) {
							var tr = $(v);
							var filename = $('td:eq(1)', tr).text()
									.toLowerCase();
							if (filename.indexOf(keyword) >= 0)
								tr.show();
						});
		} else {
			$('tr:visible', tbody).each(function(i, v) {
						var tr = $(v);
						var filename = $('td:eq(1)', tr).text().toLowerCase();
						if (filename.indexOf(keyword) < 0)
							tr.hide();
					});
		}
	});;
}
Observation.upload = function(container) {
	var upload_form = $('#upload_form', container);
	if (upload_form.length && typeof window.FileReader != 'undefined') {
		$('input[type="file"]', upload_form).change(function() {
					if (uploadFiles(this.files)) {
						$(this).closest('div').remove();
						addMore(1);
						return false;
					}
				});
		upload_form.bind('dragover', function(e) {
					$(this).addClass('drophover');
					return false;
				}).bind('dragleave', function(e) {
					$(this).removeClass('drophover');
					return false;
				}).get(0).ondrop = function(e) {
			e.preventDefault();
			$(this).removeClass('drophover');
			uploadFiles(e.dataTransfer.files);
			return true;
		};
		$(document.body).bind('dragover', function(e) {
					return false;
				})[0].ondrop = function(e) {
			var id = e.dataTransfer.getData('Text');
			var target = $(e.target);
			if (!id || target.is('#upload_form')
					|| target.parents('#upload_form').length)
				return true;
			var i = id.lastIndexOf('/');
			if (i > 0)
				id = id.substring(i + 1);
			if (e.preventDefault)
				e.preventDefault();
			if (e.stopPropagation)
				e.stopPropagation();
			deleteFiles(id);
		}
	}

	$('.uploaditem', container).prop('draggable', true).each(function() {
		var t = $(this);
		this.ondragstart = function(e) {
			e.dataTransfer.effectAllowed = 'copy';
			e.dataTransfer.setData('Text', $(':input:eq(0)', t.closest('tr'))
							.attr('value'));
		};
	});

	$('#more', container).click(function() {
				addMore(1);
			});

	$('.filename').dblclick(function() {
		if (this.contentEditable !== true) {
			$(this).removeAttr('draggable').data('oldvalue', $(this).text())
					.css('cursor', 'text');
			this.contentEditable = true;
			$(this).focus();
		}
	}).blur(function() {
		var oldvalue = $(this).data('oldvalue');
		var newvalue = $(this).text();
		$.ajax({
			url : CONTEXT_PATH + '/common/upload/rename/' + encodeURI(oldvalue),
			data : {
				folder : $('#upload_form [name="folder"]').val(),
				filename : newvalue
			},
			beforeSend : Indicator.show,
			success : function(data) {
				Indicator.hide();
				if (typeof data == 'string') {
					var html = data.replace(/<script(.|\s)*?\/script>/g, '');
					var div = $('<div/>').html(html);
					var message = $('#message', div);
					if (message.html()) {
						if ($('.action-error', message).length
								|| !$('#upload_form input[name="pick"]').length)
							if ($('#message').length)
								$('#message').html(message.html());
							else
								$('<div id="message">' + message.html()
										+ '</div>').prependTo($('#content'));
					}
				} else {
					Message.showActionError(data.actionErrors);
					Message.showActionMessage(data.actionMessages);
				}
				$('#files button.reload').trigger('click');
			}
		});
	});

};

function addMore(n) {
	var f = $('input[type="file"]:last').parent();
	var r;
	for (var i = 0; i < n; i++) {
		r = f.clone(true);
		f.after(r);
		f = r;
	}
}
function deleteFiles(file) {
	$.alerts.confirm(MessageBundle.get('confirm.delete'), MessageBundle
					.get('select'), function(b) {
				if (b) {
					var options = {
						type : $('#upload_form').attr('method'),
						url : CONTEXT_PATH + '/common/upload/delete',
						dataType : 'json',
						complete : function() {
							$('#files button.reload').click();
						}
					};
					if (file) {
						var data = $('#upload_form').serialize();
						var params = [];
						params.push('id=' + file);
						if (data) {
							var arr = data.split('&');
							for (var i = 0; i < arr.length; i++) {
								var arr2 = arr[i].split('=', 2);
								if (arr2[0] != 'id')
									params.push(arr[i]);
							}
						}
						options.data = params.join('&');
					} else {
						options.data = $('#upload_form').serialize();
					}
					ajax(options);
				}
			});

}
function uploadFiles(files, filenames) {
	if (files && files.length) {
		var data = {
			folder : $('#upload_form [name="folder"]').val(),
			autorename : $('#upload_form [name="autorename"]').is(':checked')
		};
		if (filenames && filenames.length)
			data.filename = filenames;
		return $.ajaxupload(files, {
					url : $('#upload_form').attr('action'),
					name : $('#upload_form input[type="file"]').attr('name'),
					data : data,
					beforeSend : Indicator.show,
					success : function(xhr) {
						Indicator.hide();
						var data = xhr.responseText;
						var html = data
								.replace(/<script(.|\s)*?\/script>/g, '');
						var div = $('<div/>').html(html);
						var message = $('#message', div);
						if (message.html()) {
							if ($('.action-error', message).length
									|| !$('#upload_form input[name="pick"]').length)
								if ($('#message').length)
									$('#message').html(message.html());
								else
									$('<div id="message">' + message.html()
											+ '</div>')
											.prependTo($('#content'));
							if ($('.action-error', message).length)
								return;
						}
						$('#files button.reload').trigger('click');
					}
				});
	}
}

/*
 * JavaScript Canvas to Blob 2.0.5
 * https://github.com/blueimp/JavaScript-Canvas-to-Blob
 * 
 * Copyright 2012, Sebastian Tschan https://blueimp.net
 * 
 * Licensed under the MIT license: http://www.opensource.org/licenses/MIT
 * 
 * Based on stackoverflow user Stoive's code snippet:
 * http://stackoverflow.com/q/4998908
 */

/* jslint nomen: true, regexp: true */
/* global window, atob, Blob, ArrayBuffer, Uint8Array, define */

(function(window) {
	'use strict';
	var CanvasPrototype = window.HTMLCanvasElement
			&& window.HTMLCanvasElement.prototype, hasBlobConstructor = window.Blob
			&& (function() {
				try {
					return Boolean(new Blob());
				} catch (e) {
					return false;
				}
			}()), hasArrayBufferViewSupport = hasBlobConstructor
			&& window.Uint8Array && (function() {
				try {
					return new Blob([new Uint8Array(100)]).size === 100;
				} catch (e) {
					return false;
				}
			}()), BlobBuilder = window.BlobBuilder || window.WebKitBlobBuilder
			|| window.MozBlobBuilder || window.MSBlobBuilder, dataURLtoBlob = (hasBlobConstructor || BlobBuilder)
			&& window.atob
			&& window.ArrayBuffer
			&& window.Uint8Array
			&& function(dataURI) {
				var byteString, arrayBuffer, intArray, i, mimeString, bb;
				if (dataURI.split(',')[0].indexOf('base64') >= 0) {
					// Convert base64 to raw binary data held in a string:
					byteString = atob(dataURI.split(',')[1]);
				} else {
					// Convert base64/URLEncoded data component to raw binary
					// data:
					byteString = decodeURIComponent(dataURI.split(',')[1]);
				}
				// Write the bytes of the string to an ArrayBuffer:
				arrayBuffer = new ArrayBuffer(byteString.length);
				intArray = new Uint8Array(arrayBuffer);
				for (i = 0; i < byteString.length; i += 1) {
					intArray[i] = byteString.charCodeAt(i);
				}
				// Separate out the mime component:
				mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];
				// Write the ArrayBuffer (or ArrayBufferView) to a blob:
				if (hasBlobConstructor) {
					return new Blob([hasArrayBufferViewSupport
									? intArray
									: arrayBuffer], {
								type : mimeString
							});
				}
				bb = new BlobBuilder();
				bb.append(arrayBuffer);
				return bb.getBlob(mimeString);
			};
	if (window.HTMLCanvasElement && !CanvasPrototype.toBlob) {
		if (CanvasPrototype.mozGetAsFile) {
			CanvasPrototype.toBlob = function(callback, type, quality) {
				if (quality && CanvasPrototype.toDataURL && dataURLtoBlob) {
					callback(dataURLtoBlob(this.toDataURL(type, quality)));
				} else {
					callback(this.mozGetAsFile('blob', type));
				}
			};
		} else if (CanvasPrototype.toDataURL && dataURLtoBlob) {
			CanvasPrototype.toBlob = function(callback, type, quality) {
				callback(dataURLtoBlob(this.toDataURL(type, quality)));
			};
		}
	}
	if (typeof define === 'function' && define.amd) {
		define(function() {
					return dataURLtoBlob;
				});
	} else {
		window.dataURLtoBlob = dataURLtoBlob;
	}
}(this));