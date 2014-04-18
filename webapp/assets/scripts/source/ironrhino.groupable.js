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
						var group = ct.data('group') || '';
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

})(jQuery);

Observation.groupable = function(container) {
	$('.groupable', container).groupable();
};