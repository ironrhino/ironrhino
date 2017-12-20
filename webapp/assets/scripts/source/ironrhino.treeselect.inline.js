(function($) {
	$.fn.treeselectinline = function() {
		this.each(function() {
			var t = $(this)
					.attr('type', 'hidden')
					.addClass('resettable')
					.removeClass('.treeselect-inline')
					.wrap('<div class="input-pseudo treeselect-inline" tabindex="0"></div>');
			var treeselect = t.parent();
			if (t.prop('disabled'))
				treeselect.addClass('disabled');
			if (t.prop('readonly'))
				treeselect.addClass('readonly');
			var text = $('<div class="text resettable"/>').appendTo(treeselect);
			if (t.data('text')) {
				text.text(t.data('text'));
				t.removeAttr('data-text');
			}
			if (t.attr('id')) {
				treeselect.attr('id', t.attr('id'));
				t.removeAttr('id');
			}
			$.each(	$.grep(t.attr('class').split(' '), function(v) {
								return v.indexOf('input-') == 0
										|| v.indexOf('span') == 0;
							}), function(k, v) {
						t.removeClass(v);
						treeselect.addClass(v);
					});
			$('<i class="indicator glyphicon glyphicon-menu-down"/><i class="remove glyphicon glyphicon-remove-sign"/><div class="options"/>')
					.appendTo(treeselect);
		});
		return this;
	};
})(jQuery);

$(function() {
	$(document).on('click', '.treeselect-inline', function(e) {
		var t = $(e.target).closest('.treeselect-inline');
		var input = t.children('input');
		var text = t.children('.text');
		if (input.prop('disabled') || input.prop('readonly'))
			return;
		if (!$(e.target).is('.treeselect-inline,.text,.glyphicon'))
			return;
		var treeselect = $(e.target).closest('.treeselect-inline');
		treeselect.find('.glyphicon').toggleClass('glyphicon-menu-down')
				.toggleClass('glyphicon-menu-up');
		var options = treeselect.find('.options').toggle();
		if (treeselect.find('.glyphicon-menu-up').length) {
			if (input.data('url') && !options.html()) {
				var treeview = $('<div class="treeview"/>').appendTo(options);
				treeview.treeview({
							url : input.data('url'),
							click : function(e) {
								var treeselect = $(e.target)
										.closest('.treeselect-inline');
								var input = treeselect.children('input');
								var text = treeselect.children('.text');
								var node = $(this).closest('li')
										.data('treenode');
								treeselect.trigger('val', [{
													key : node.id,
													value : node.fullname
												}]);
								treeselect.click();
							},
							value : input.data('text'),
							separator : input.data('separator')
						});
			}
		}
	}).on('keydown', '.treeselect-inline', function(e) {
				if (e.keyCode == 13) {
					$(this).click();
					return false;
				}
			});
	$(document).click(function(e) {
				var target = $(e.target);
				if (!target.closest('.treeselect-inline').length)
					$('.treeselect-inline .glyphicon-menu-up').parent().click();
			});
});

Observation.treeselectinline = function(container) {
	$$('input.treeselect-inline', container).treeselectinline();

}