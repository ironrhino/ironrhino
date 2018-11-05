$(function() {
	$(document).on('click', '.mce-i-image', function() {
		var uploadurl = $('#' + tinymce.EditorManager.activeEditor.id)
				.data('uploadurl');
		if (uploadurl)
			setTimeout(function() {
						var interval = setInterval(function() {
									if (appendIcon())
										clearInterval(interval);
								}, 100);
					}, 200);
	}).on('click', '.mce-combobox button', function() {
		if (!$('#mce-browse-modal').length) {
			var modal = $('<div id="mce-browse-modal" class="modal" style="z-index:65537;height:500px;"><input id="mce-browse-folder" type="hidden"/><div class="modal-close"><button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button></div><div id="mce-browse-modal-body" class="modal-body" style="height:400px;"></div><div  id="mce-browse-modal-footer" class="modal-footer"></div></div>')
					.appendTo(document.body);
			var textarea = $('#' + tinymce.EditorManager.activeEditor.id);
			var folder = textarea.data('uploadfolder');
			if (!folder
					&& $('input[name="page.id"]', textarea.closest('form')).length) {
				var folder = '/page/';
				if ($('input[name="page.id"]').val())
					folder += $('input[name="page.id"]').val() + '/';
			}
			$('#mce-browse-folder').val(folder);
			$('#mce-browse-modal-body').on('dragover', function(e) {
						$(this).css('border', '2px dashed #333');
						return false;
					}).on('dragleave', function(e) {
						$(this).css('border', '0');
						return false;
					}).on('drop', function(e) {
						e.preventDefault();
						$(this).css('border', '0');
						upload(e.originalEvent.dataTransfer.files);
						return true;
					});
			$('button.close', modal).click(function() {
						modal.fadeOut().remove();
					});
			browse();
		}
	});
	$(document).on('dragover', function(e) {
				return false;
			}).on('drop', function(e) {
		var id = e.originalEvent.dataTransfer.getData('Text');
		var target = $(e.target);
		if (!id || target.is('#mce-browse-modal-body')
				|| target.parents('#mce-browse-modal-body').length)
			return true;
		var i = id.lastIndexOf('/');
		if (i > 0)
			id = id.substring(i + 1);
		if (e.preventDefault)
			e.preventDefault();
		if (e.stopPropagation)
			e.stopPropagation();
		var func = function() {
			$.post(	CONTEXT_PATH
							+ ($('#' + tinymce.EditorManager.activeEditor.id)
									.data('uploadurl')) + '/delete', {
						folder : $('#mce-browse-folder').val(),
						id : id
					}, browse);
		}
		if (VERBOSE_MODE != 'LOW') {
			$.alerts({
						type : 'confirm',
						message : MessageBundle.get('confirm.delete'),
						callback : function(b) {
							if (b) {
								func();
							}
						}
					});
		} else {
			func();
		}
	});
});

function appendIcon() {
	var combobox = $('.mce-combobox');
	if (combobox.length) {
		combobox
				.addClass('mce-has-open')
				.append('<div class="mce-btn mce-open" tabindex="-1"><button hidefocus="" tabindex="-1"><i class="mce-ico mce-i-browse"></i></button></div>')
				.find('input.mce-textbox').width($('input.mce-textbox:eq(1)')
						.width()
						- 33);
		return true;
	}
	return false;
}

function browse() {
	var folder = $('#mce-browse-folder').val() || '/';
	var panel = $('#mce-browse-modal-body');
	$.getJSON(CONTEXT_PATH
					+ $('#' + tinymce.EditorManager.activeEditor.id)
							.data('uploadurl') + '/files?folder=' + folder
					+ '&suffix=jpg,gif,png,bmp', function(data) {
				var html = '';
				$.each(data, function(key, val) {

					if (val) {
						var name = key.substring(key.lastIndexOf('/') + 1);
						html += '<img src="'
								+ key
								+ '" alt="'
								+ name
								+ '" title="'
								+ name
								+ '" style="margin:5px;width:92px;height:92px;cursor:pointer;"/>';
					} else {
						var name = key.substring(0, key.length - 1);
						var name = name.substring(name.lastIndexOf('/') + 1);
						html += '<a href="#" style="display:block;" data-folder="'
								+ key + '">' + name + '</a>';
					}
				});
				if (folder != '/') {
					var tmp = folder.substring(1);
					tmp = tmp.substring(0, tmp.length - 1);
					var arr = tmp.split('/');
					arr.pop();
					html += '<a href="#" style="display:block;" data-folder="/'
							+ (arr.length != 0 ? arr.join('/') + '/' : '')
							+ '"> <span class="glyphicon glyphicon-arrow-up"></span> </a>';
				}
				panel.html(html);
				$('img', panel).attr('draggable', true).each(function() {
					var t = $(this);
					t.on('dragstart', function(e) {
								e.originalEvent.dataTransfer.effectAllowed = 'copy';
								e.originalEvent.dataTransfer.setData('Text', t
												.attr('title'));
							});
				}).click(function() {
							$('#mce-browse-modal').fadeIn().remove();
							$('.mce-combobox input.mce-textbox').val($(this).attr('src'));
							$('input.mce-textbox:eq(1)').focus();
						});
				$('a', panel).click(function() {
							$('#mce-browse-folder').val($(this).data('folder'));
							browse();
							return false;
						});
				$('#mce-browse-modal-footer')
						.html('<input id="files" type="file" multiple="true"/>')
						.find('input[type="file"]').change(function() {
									upload(this.files);
								});
			});
	return false;
}

function upload(files) {
	var folder = $('#mce-browse-folder').val() || '/';
	$.ajaxupload(files, ajaxOptions({
						url : CONTEXT_PATH
								+ $('#' + tinymce.EditorManager.activeEditor.id)
										.data('uploadurl') + '?folder='
								+ folder,
						dataType : 'JSON',
						success : browse
					}));
}