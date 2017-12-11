(function($) {
	$.fn.treeselectinline = function() {
		this.each(function() {
			var t = $(this).attr('type', 'hidden')
					.removeClass('.treeselect-inline').addClass('resettable')
					.wrap('<div class="treeselect-inline" tabindex="0"></div>');
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
			$('<i class="glyphicon glyphicon-menu-down"/><i class="glyphicon glyphicon-remove"/><div class="options"/>')
					.appendTo(treeselect);
			treeselect.hover(function(e) {
						var t = $(e.target).closest('.treeselect-inline');
						var text = t.find('.text').text();
						if (text)
							t.attr('title', text);
					}, function(e) {
						var t = $(e.target).closest('.treeselect-inline');
						t.removeAttr('title');
					});
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
		if ($(e.target).is('.glyphicon-remove')) {
			input.val('').trigger('change');
			text.text('');
			return;
		}
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
								input.val(node.id).trigger('change');
								text.text(node.fullname);
								treeselect.click();
							},
							value : input.data('text'),
							separator : input.data('separator')
						});
			}
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