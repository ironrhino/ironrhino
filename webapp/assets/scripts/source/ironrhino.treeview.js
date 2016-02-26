Observation.treeview = function(container) {
	$$('.treeview', container).each(function() {
				var t = $(this);
				var head = t.data('head');
				var template = $.trim(t.find('template').html());
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
							unique : t.data('unique'),
							template : template
						}).html('');
				if (head) {
					if (template && !t.data('head-plain')) {
						head = $.tmpl(template, {
									id : 0,
									name : head
								});
						t.html(head).children().each(function() {
									_observe(this);
								});
					} else {
						t.text(head);
					}
				}
			});
};