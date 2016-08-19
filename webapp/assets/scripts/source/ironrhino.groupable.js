;
(function($) {

	$.fn.groupable = function() {
		this.each(function() {
			var t = $(this);
			if (!$('[data-group]', t).length)
				return;
			var groups = {};
			$('[data-group],.control-group', t).each(function() {
						var ct = $(this);
						var group = ct.data('group')
								|| MessageBundle.get('other');
						var cts = groups[group];
						if (!cts) {
							cts = [];
							groups[group] = cts;
						}
						cts.push(ct);
					});
			if ($('.form-actions', t).length)
				$('<ul class="nav nav-tabs"></ul><div class="tab-content"></div>')
						.insertBefore($('.form-actions', t));
			else
				$('<ul class="nav nav-tabs"></ul><div class="tab-content"></div>')
						.appendTo(t);
			var navtabs = $('.nav-tabs', t);
			var tabcontent = $('.tab-content', t);
			var index = 0;
			var prefix = t.attr('id')
					|| ('a' + (Math.random())).replace('.', '').substring(0, 5);
			for (var key in groups) {
				var id = prefix + index;
				var tab = $('<li><a href="#' + id + '" data-toggle="tab">'
						+ key + '</a></li>').appendTo(navtabs);
				if (index == 0)
					tab.addClass('active');
				var pane = $('<div class="tab-pane" id="' + id + '"></div>')
						.appendTo(tabcontent);
				if (index == 0)
					pane.addClass('active');
				var cts = groups[key];
				for (var i = 0; i < cts.length; i++) {
					$(cts[i]).appendTo(pane);
				}
				index++;
			}
		});
		return this;
	}

	$.fn.groupColumns = function() {
		this.each(function() {
					var t = $(this);
					var columns = parseInt(t.data('columns'));
					var intab = t.find('.tab-pane > .control-group').length > 0;
					if (intab) {
						t.find('.tab-pane').each(function() {
									transform($(this), columns);
								});
					} else {
						transform(t, columns);
					}
				});
		return this;
	}

	transform = function(container, columns) {
		var rowclass = container.parents('.container-fluid').length
				? 'row-fluid'
				: 'row';
		var span = 'span' + (12 / columns);
		var current = 0;
		container.find('.control-group').filter(function(i) {
					return !$(this).parent('[class*="span"]').length;
				}).each(function(i, v) {
			var t = $(v);
			if (t.find('.controls')
					.find('textarea,table,p,.input-xxlarge,.newline').length) {
				t.wrap('<div class="' + rowclass
						+ '"><div class="span12"/></div>');
				current = 0;
			} else {
				if (current % columns == 0) {
					t.wrap('<div class="' + rowclass + '"><div class="' + span
							+ '"/></div>');
				} else {
					var prev = t.prev('.' + rowclass);
					t.wrap('<div class="' + span + '"/>').parent()
							.appendTo(prev);
				}
				current++;
			}
		});
	}

})(jQuery);

Observation.groupable = function(container) {
	$$('.groupable', container).groupable();
	$$('[data-columns]', container).groupColumns();
};