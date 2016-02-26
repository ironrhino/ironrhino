(function($) {

	$.fn.attachmentableform = function() {
		this.each(function() {
			var t = $(this);
			var folder = t.data('folder');
			if (!folder) {
				var action = t.prop('action');
				var index = action.lastIndexOf('/');
				if (index > 0 && action.substring(index + 1) == 'save') {
					action = action.substring(0, index);
					index = action.lastIndexOf('/');
					if (index > -1)
						folder = action.substring(index + 1);
				}
				t.data('folder', folder);
			}
			if (!t.data('attachmentsfield'))
				t.data('attachmentsfield', folder + '.attachments');
			var formactions = $('.form-actions', t)
					.append($(' <button type="button" class="btn upload">'
							+ MessageBundle.get('upload')
							+ '</button> <button type="button" class="btn snapshot">'
							+ MessageBundle.get('snapshot') + '</button>'));
			formactions.find('.btn[type="submit"]').addClass('btn-primary');
			if (!$('ul.attachments.thumbnails', t).length)
				formactions
						.before('<div style="padding: 10px 50px;"><ul class="attachments resetable thumbnails" style="min-height:50px;"></ul></div>');
			t.find('button.upload').click(function() {
				$('<input type="file" multiple />').appendTo(t).hide().change(
						function() {
							upload(this.files, null, t);
							$(this).remove();
						}).click();
			});
			t.find('button.snapshot').click(function() {
				$.snapshot({
							onsnapshot : function(canvas, timestamp) {
								var filename = $.format.date(
										new Date(timestamp),
										'yyyyMMddHHmmssSSS')
										+ '.png';
								var file;
								if (canvas && canvas.mozGetAsFile)
									upload([canvas.mozGetAsFile(filename)],
											null, t);
								else if (canvas && canvas.toBlob)
									canvas.toBlob(function(blob) {
												upload([blob], [filename], t);
											}, 'image/png');
								else
									upload([dataURLtoBlob(canvas.toDataURL())],
											[filename], t);
							},
							onerror : function(msg) {
								Message.showError(msg);
							}
						});
			});
			var ul = t.find('ul.attachments').on('dragover', function(e) {
						$(this).addClass('drophover');
						return false;
					}).on('dragleave', function(e) {
						$(this).removeClass('drophover');
						return false;
					}).on('drop', function(e) {
						e.preventDefault();
						$(this).removeClass('drophover');
						upload(e.originalEvent.dataTransfer.files, null, t);
						return true;
					}).on('click', 'li .remove', function() {
						$(this).closest('li').remove();
						return false;
					});
			appendAttachments(t, t.data('attachments'));
		});
		return this;
	}

	function upload(files, filenames, form) {
		if (files && files.length) {
			var data = {
				folder : form.data('folder'),
				autorename : true,
				json : true
			};
			if (filenames && filenames.length) {
				data.filename = filenames;
			}
			return $.ajaxupload(files, {
						url : CONTEXT_PATH + '/common/upload',
						name : 'file',
						data : data,
						beforeSend : Indicator.show,
						complete : function(xhr) {
							Indicator.hide();
						},
						success : function(data) {
							appendAttachments(form, data);
						}
					});
		}
	}

	function appendAttachments(form, attachments) {
		ul = form.find('ul.attachments');
		if (attachments && typeof attachments == 'string') {
			if (attachments.indexOf('[') == 0)
				attachments = attachments.substring(1, attachments.length - 1);
			attachments = attachments.split(/\s*,\s*/);
		}
		if (attachments)
			for (var i = 0; i < attachments.length; i++) {
				var path = attachments[i];
				var filename = path.substring(path.lastIndexOf('/') + 1);
				var uri = encodeURI(path);
				var image = false;
				var index = uri.lastIndexOf('.');
				if (index > 0) {
					var suffix = uri.substring(index + 1);
					image = suffix == 'jpg' || suffix == 'png'
							|| suffix == 'gif' || suffix == 'bmp'
							|| suffix == 'webp';
				}
				$('<li class="span2"><a class="remove" href="#">&times;</a><input type="hidden" name="'
						+ form.data('attachmentsfield')
						+ '" value="'
						+ path
						+ '"/><div class="thumbnail"><a href="'
						+ uri
						+ '" target="_blank">'
						+ (image ? '<img src="' + uri + '"/>' : '<span>'
								+ filename + '</span>') + '</a></div></li>')
						.appendTo(ul);
			}
	}

})(jQuery);

Observation.attachmentableform = function(container) {
	$$('form.attachmentable', container).attachmentableform();
};