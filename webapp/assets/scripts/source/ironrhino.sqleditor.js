(function($) {
	var BLOCK_COMMENT = /\/\*(?:.|[\n\r])*?\*\//g;
	var LINE_COMMENT = /\r?\n?\s*--.*\r?(\n|$)/g;
	var PARAMETER = /(:({\s*)?(\w|[^'\)\sx00-xff])*(\s*})?)(,|;|\)|\s|\||\+|$)/g;
	$.sqleditor = {
		highlight: function(sql) {
			return sql.replace(PARAMETER, '<strong>$1</strong>$5').replace(
				/\?/g, '<strong>$&</strong>').replace(BLOCK_COMMENT,
					'<span class="comment">$&</span>').replace(LINE_COMMENT,
						'<span class="comment">$&</span>');
		}
	}
	function preview(input) {
		if (!input.val())
			return;
		var t = $(input).hide();
		var p = t.next('div.preview');
		if (!p.length) {
			p = $('<div class="preview codeblock"></div>').insertAfter(t);
			if (!(t.prop('readonly') || t.prop('disabled')))
				p.click(function() {
					$(this).hide().prev('.sqleditor:input').show()
						.focus();
				});
		}
		var width = t.width();
		var hasWidthClass = false;
		$.each($.grep(t.attr('class').split(' '), function(v) {
			var b = v.indexOf('input-') == 0
				|| v.indexOf('span') == 0;
			if (b)
				hasWidthClass = true;
			return b;
		}), function(k, v) {
			p.addClass(v);
		});
		if (width > 0 && !hasWidthClass)
			p.width(width);
		p.css('height', t.height() + 'px').html($.sqleditor.highlight(t.val()))
			.show();

	}
	$.fn.sqleditor = function() {
		$(this).each(function() {
			var t = $(this);
			if (t.is(':input')) {
				preview(t);
				t.blur(function() {
					preview(t)
				}).change(function() {
					preview(t)
				});
			} else {
				t.addClass('preview codeblock').html($.sqleditor.highlight(t
					.text()));
			}
		});
		return this;
	};
})(jQuery);

Observation.sqleditor = function(container) {
	$$('.sqleditor', container).sqleditor();
};