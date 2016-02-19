Observation.treeview = function(container) {
	var selector = '.treeview';
	var c = $(container);
	c = c.is(selector) ? c : $(selector, c);
	c.each(function() {
				var t = $(this);
				t.treeview({
							url : t.data('url'),
							click : function() {
								var click = t.data('click');
								if (click) {
									var func = function() {
										eval(click);
									};
									func.apply($(this).closest('li')
											.data('treenode'));
								}
							},
							collapsed : t.data('collapsed'),
							unique : t.data('unique')
						});
			});
};