(function($) {
	$.fn.imagepick = function() {
		return this.each(function() {
			var t = $(this);
			t
					.addClass('poped')
					.wrap('<div class="input-append"/>')
					.parent()
					.append('<span class="add-on listpick" style="cursor:pointer;" data-options="{\'url\':\''
							+ CONTEXT_PATH
							+ '/common/upload/pick\',\'id\':\'#'
							+ t.attr('id')
							+ '\',\'width\':400}"><i class="glyphicon glyphicon-th-list"></i></span>');
			if (t.val())
				t.attr('data-content', '<img src="' + t.val() + '"/>');
			t.change(function() {
						var html = this.value ? '<img src="' + this.value
								+ '"/>' : '';
						$(this).attr('data-content', html);
						$('.popover-content', $(this).parent()).html(html);
						var options = $(this).data('popover').options;
						if (options)
							options.content = html;
					});
		});
	}
})(jQuery);

Observation._imagepick = function(container) {
	$('input.imagepick', container).imagepick();
};
