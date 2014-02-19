(function($) {
	var current;
	function find(expr) {
		var i = expr.indexOf('@');
		if (i == 0)
			return current;
		else if (i > 0)
			expr = expr.substring(0, i);
		return (expr == 'this') ? current : $(expr);
	}
	function val(expr, val, html) {// expr #id #id@attr .class@attr @attr
		if (!expr)
			return;
		if (arguments.length > 1) {
			var i = expr.indexOf('@');
			if (i < 0) {
				var ele = expr == 'this' ? current : $(expr);
				if (ele.is(':input')) {
					ele.val(val);
					Form.validate(ele);
				} else {
					if (html)
						ele.html(val);
					else
						ele.text(val);
				}
			} else if (i == 0) {
				current.attr(expr.substring(i + 1), val);
			} else {
				var selector = expr.substring(0, i);
				var ele = selector == 'this' ? current : $(selector);
				if (ele.parents('.richtable').length
						&& ele.prop('tagName') == 'TD'
						&& expr.indexOf('data-cellvalue') > -1)
					Richtable.updateValue(ele, val);
				else
					ele.attr(expr.substring(i + 1), val);
			}
		} else {
			var i = expr.indexOf('@');
			if (i < 0) {
				var ele = expr == 'this' ? current : $(expr);
				if (ele.is(':input'))
					return ele.val();
				else
					return ele.contents().filter(function() {
								return this.nodeType == 3;
							}).text();
			} else if (i == 0) {
				return current.attr(expr.substring(i + 1));
			} else {
				var selector = expr.substring(0, i);
				var ele = selector == 'this' ? current : $(selector);
				return ele.attr(expr.substring(i + 1));
			}
		}
	}
	function removeAction(event) {
		current = $(event.target).closest('.listpick');
		var options = current.data('_options');
		var nametarget = find(options.name);
		val(options.name, nametarget.is(':input,td')
						? ''
						: '<i class="glyphicon glyphicon-list"></i>', true);
		val(options.id, '');
		$(this).remove();
		event.stopPropagation();
		return false;

	}
	$.fn.listpick = function() {
		$(this).each(function() {
			current = $(this);
			var options = {
				separator : ',',
				nameindex : 1,
				multiple : false
			}
			$.extend(options, (new Function("return "
							+ (current.data('options') || '{}')))());
			current.data('_options', options);
			var nametarget = null;
			if (options.name) {
				nametarget = find(options.name);
				var remove = nametarget.children('a.remove');
				if (remove.length) {
					remove.click(removeAction);
				} else {
					var text = val(options.name);
					if (text) {
						if (text.indexOf('...') < 0)
							$('<a class="remove" href="#">&times;</a>')
									.appendTo(nametarget).click(removeAction);
					} else if (!nametarget.is(':input,td')) {
						val(options.name,
								'<i class="glyphicon glyphicon-list"></i>',
								true);
					}
				}
			}
			var func = function(event) {
				current = $(event.target).closest('.listpick');
				$('#_pick_window').remove();
				var win = $('<div id="_pick_window" title="'
						+ MessageBundle.get('select') + '"></div>')
						.appendTo(document.body).dialog({
							width : current.data('_options').width || 800,
							minHeight : current.data('_options').minHeight
									|| 500
						});
				win.closest('.ui-dialog').css('z-index', '2001');
				if (win.html() && typeof $.fn.mask != 'undefined')
					win.mask(MessageBundle.get('ajax.loading'));
				else
					win.html('<div style="text-align:center;">'
							+ MessageBundle.get('ajax.loading') + '</div>');
				var target = win.get(0);
				target.onsuccess = function() {
					if (typeof $.fn.mask != 'undefined')
						win.unmask();
					Dialog.adapt(win);
					if (!options.multiple) {
						$(target).on('click', 'tbody input[type=radio]',
								function() {
									var id = $(this).val();
									var name = $($(this).closest('tr')[0].cells[options.nameindex])
											.text();
									if (options.name) {
										val(options.name, name);
										var nametarget = find(options.name);
										if (nametarget.is(':input')) {
											nametarget.trigger('change');
											var form = nametarget
													.closest('form');
											if (!form.hasClass('nodirty'))
												form.addClass('dirty');
										} else {
											$('<a class="remove" href="#">&times;</a>')
													.appendTo(nametarget)
													.click(removeAction);
										}
									}
									if (options.id) {
										val(options.id, id);
										var idtarget = find(options.id);
										if (idtarget.is(':input')) {
											idtarget.trigger('change');
											var form = idtarget.closest('form');
											if (!form.hasClass('nodirty'))
												form.addClass('dirty');
										}
									}
									win.dialog('destroy').remove();
									return false;
								});

					} else {
						$(target).on('click', 'button.pick', function() {
							var checkbox = $('tbody :checked', target);
							var ids = [], names = [];
							checkbox.each(function() {
								ids.push($(this).val());
								names
										.push($($(this).closest('tr')[0].cells[options.nameindex])
												.text());
							});
							var separator = options.separator;
							if (options.name) {
								var nametarget = find(options.name);
								var name = names.join(separator);
								if (nametarget.is(':input')) {
									nametarget.trigger('change');
									var _names = val(options.name) || '';
									val(
											options.name,
											ArrayUtils
													.unique((_names
															+ (_names
																	? separator
																	: '') + name)
															.split(separator))
													.join(separator));
									var form = nametarget.closest('form');
									if (!form.hasClass('nodirty'))
										form.addClass('dirty');
								} else {
									var picked = nametarget.data('picked')
											|| '';
									picked = ArrayUtils.unique(((picked
											? picked + separator
											: '') + name).split(separator))
											.join(separator);
									nametarget.data('picked', picked);
									val(options.name, picked);
									$('<a class="remove" href="#">&times;</a>')
											.appendTo(nametarget)
											.click(removeAction);
								}
							}
							if (options.id) {
								var idtarget = find(options.id);
								var id = ids.join(separator);
								var _ids = val(options.id) || '';
								val(options.id, ArrayUtils.unique((_ids
												+ (_ids ? separator : '') + id)
												.split(separator))
												.join(separator));
								if (idtarget.is(':input')) {
									idtarget.trigger('change');
									var form = idtarget.closest('form');
									if (!form.hasClass('nodirty'))
										form.addClass('dirty');
								}
							}
							win.dialog('destroy').remove();
							return false;
						});
					}
				};
				var url = options.url;
				if (url.indexOf('multiple') < 0 && options.multiple)
					url += (url.indexOf('?') > 0 ? '&' : '?') + 'multiple=true'
				ajax({
							url : url,
							cache : false,
							target : target,
							replacement : '_pick_window:content',
							quiet : true
						});

			};
			current.css('cursor', 'pointer').click(func).keydown(
					function(event) {
						if (event.keyCode == 13) {
							func(event);
							return false;
						}
					});
		});
		return this;
	};

})(jQuery);

Observation.listpick = function(container) {
	$('.listpick', container).listpick();
};