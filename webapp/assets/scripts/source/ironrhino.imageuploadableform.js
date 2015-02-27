(function($) {

	$.fn.imageuploadableform = function() {
		this.each(function() {
					var t = $(this);
					$(' <button type="button" class="btn">'
							+ MessageBundle.get('upload') + '</button> ')
							.appendTo($('.form-actions', t)).click(function() {
								$('<input type="file" accept="image/*" multiple />')
										.appendTo(t).hide().change(function() {
													upload(this.files, null, t);
													$(this).remove();
												}).click();
							});
					$(' <button type="button" class="btn">'
							+ MessageBundle.get('snapshot') + '</button> ')
							.appendTo($('.form-actions', t)).click(function() {
								$.snapshot({
									onsnapshot : function(canvas, timestamp) {
										var filename = $.format.date(
												new Date(timestamp),
												'yyyyMMddHHmmssSSS')
												+ '.png';
										var file;
										if (canvas && canvas.mozGetAsFile)
											upload(
													[canvas
															.mozGetAsFile(filename)],
													null, t);
										else if (canvas && canvas.toBlob)
											canvas.toBlob(function(blob) {
														upload([blob],
																[filename], t);
													}, 'image/png');
										else
											upload(	[dataURLtoBlob(canvas
															.toDataURL())],
													[filename], t);
									},
									onerror : function(msg) {
										Message.showError(msg);
									}
								});
							});
					$(t).find('ul.thumbnails').on('dragover', function(e) {
								$(this).addClass('drophover');
								return false;
							}).on('dragleave', function(e) {
								$(this).removeClass('drophover');
								return false;
							}).on('drop', function(e) {
								e.preventDefault();
								$(this).removeClass('drophover');
								upload(e.originalEvent.dataTransfer.files,
										null, t);
								return true;
							});
				});
		return this;
	}

	function upload(files, filenames, form) {
		if (files && files.length) {
			var folder = form.data('folder');
			if (!folder) {
				var action = form.attr('action');
				var index = action.lastIndexOf('/');
				if (index > 0 && action.substring(index + 1) == 'save') {
					action = action.substring(0, index);
					index = action.lastIndexOf('/');
					if (index > -1)
						folder = action.substring(index + 1);
				}
			}
			var data = {
				folder : folder,
				autorename : true,
				json : true
			};
			if (filenames && filenames.length) {
				data.filename = filenames;
			}
			var imagesfield = form.data('imagesfield') || folder + '.images';
			return $.ajaxupload(files, {
				url : CONTEXT_PATH + '/common/upload',
				name : 'file',
				data : data,
				beforeSend : Indicator.show,
				complete : function(xhr) {
					Indicator.hide();
				},
				success : function(data) {
					var ul = form.find('ul.thumbnails');
					for (var i = 0; i < data.length; i++) {
						var path = data[i];
						var index = path.lastIndexOf('/');
						var filename = index >= 0
								? path.substring(index)
								: path;
						$('<li class="span4"><a class="remove" href="#">&times;</a><input type="hidden" name="'
								+ imagesfield
								+ '" value="'
								+ filename
								+ '"/><div class="thumbnail"><a href="'
								+ path
								+ '" target="_blank"><img src="'
								+ path
								+ '"/></a></div></li>').appendTo(ul);
					}
				}
			});
		}
	}

})(jQuery);

Observation.imageuploadableform = function(container) {
	$('form.imageuploadable', container).imageuploadableform();
};