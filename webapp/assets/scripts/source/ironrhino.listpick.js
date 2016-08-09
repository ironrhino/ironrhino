(function($) {
	var current;
	function find(expr, container) {
		if (!container || expr.indexOf('#') > -1)
			container = document;
		var i = expr.indexOf('@');
		if (i == 0)
			return current;
		else if (i > 0)
			expr = expr.substring(0, i);
		return (expr == 'this') ? current : $(expr, container);
	}
	function val(expr, container, val, html) {// expr #id #id@attr .class@attr
		// @attr
		if (!container || expr.indexOf('#') > -1)
			container = document;
		if (!expr)
			return;
		if (arguments.length > 2) {
			var i = expr.indexOf('@');
			if (i < 0) {
				var ele = expr == 'this' ? current : $(expr, container);
				if (ele.is(':input')) {
					ele.val(val).trigger('change').trigger('validate');
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
				var ele = selector == 'this' ? current : $(selector, container);
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
				var ele = expr == 'this' ? current : $(expr, container);
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
				var ele = selector == 'this' ? current : $(selector, container);
				return ele.attr(expr.substring(i + 1));
			}
		}
	}
	function removeAction(event) {
		current = $(event.target).closest('.listpick');
		var options = current.data('_options');
		if (options.name)
			var nametarget = find(options.name, current);
		var viewlink = current.find('a.view[rel="richtable"]');
		if (current.is('td') && viewlink.length)
			viewlink.text('');
		else
			val(options.name, current, nametarget.is(':input,td')
							? ''
							: '<i class="glyphicon glyphicon-list"></i>', true);
		val(options.id, current, '', false);
		if (options.mapping) {
			for (var k in options.mapping)
				val(k, current, '', false);
		}
		$(this).remove();
		event.stopPropagation();
		return false;

	}
	$.fn.listpick = function() {
		$(this).each(function() {
			current = $(this);
			var options = {
				separator : ',',
				id : '.listpick-id',
				name : '.listpick-name',
				idindex : 0,
				nameindex : 1,
				multiple : false
			}
			$.extend(options, (new Function("return "
							+ (current.data('options') || '{}')))());
			current.data('_options', options);
			var nametarget = null;
			if (options.name) {
				nametarget = find(options.name, current);
				var remove = nametarget.children('a.remove');
				if (remove.length) {
					remove.click(removeAction);
				} else {
					var text = val(options.name, current);
					var viewlink = current.find('a.view[rel="richtable"]');
					if (current.is('td') && viewlink.length)
						text = viewlink.text();
					if (text) {
						if (text.indexOf('...') < 0)
							$('<a class="remove" href="#">&times;</a>')
									.appendTo(nametarget).click(removeAction);
					} else if (!nametarget.is(':input,td')) {
						val(options.name, current,
								'<i class="glyphicon glyphicon-list"></i>',
								true);
					}
				}
			}
			var func = function(event) {
				if ($(event.target).is('a.view[rel="richtable"]'))
					return true;
				current = $(event.target).closest('.listpick');
				var options = current.data('_options');
				var winid = current.data('winid');
				if (winid) {
					$('#' + winid).remove();
				} else {
					var winindex = $(document).data('winindex') || 0;
					winindex++;
					$(document).data('winindex', winindex);
					var winid = '_window_' + winindex;
					current.data('winid', winid);
				}
				var win = $('<div id="' + winid + '" title="'
						+ MessageBundle.get('select')
						+ '" class="window-listpick"></div>')
						.appendTo(document.body).dialog({
									width : options.width || 800,
									minHeight : options.minHeight || 500,
									close : function() {
										win.html('').dialog('destroy').remove();
									}
								});
				win.data('windowoptions', options);
				win.closest('.ui-dialog').css('z-index', 2000);
				if (win.html() && typeof $.fn.mask != 'undefined')
					win.mask(MessageBundle.get('ajax.loading'));
				else
					win.html('<div style="text-align:center;">'
							+ MessageBundle.get('ajax.loading') + '</div>');
				var target = win.get(0);
				target.onsuccess = function() {
					$(target).data('listpick', current);
					if (typeof $.fn.mask != 'undefined')
						win.unmask();
					Dialog.adapt(win);
					if (!options.multiple) {
						$(target).on('click', 'tbody input[type=radio]',
								function() {
									var cell = $($(this).closest('tr')[0].cells[options.idindex]);
									var id = options.idindex == 0 ? $(this)
											.val() : cell.data('cellvalue')
											|| cell.text();
									cell = $($(this).closest('tr')[0].cells[options.nameindex]);
									var name = cell.data('cellvalue')
											|| cell.text();
									if (options.name) {
										var nametarget = find(options.name,
												$(target).data('listpick'));
										var viewlink = nametarget
												.find('a.view[rel="richtable"]');
										if (nametarget.is('td')
												&& viewlink.length) {
											var href = viewlink.attr('href');
											viewlink
													.attr(
															'href',
															href
																	.substring(
																			0,
																			href
																					.lastIndexOf('/')
																					+ 1)
																	+ id);
											viewlink.text(name);
										} else
											val(options.name, $(target)
															.data('listpick'),
													name);
										if (!nametarget.is(':input')
												&& !nametarget.find('a.remove').length)
											$('<a class="remove" href="#">&times;</a>')
													.appendTo(nametarget)
													.click(removeAction);
									}
									if (options.id) {
										val(options.id, $(target)
														.data('listpick'), id);
										var idtarget = find(options.id,
												$(target).data('listpick'));
									}
									if (options.mapping) {
										for (var k in options.mapping) {
											cell = $($(this).closest('tr')[0].cells[options.mapping[k]]);
											val(k, $(target).data('listpick'),
													cell.data('cellvalue')
															|| cell.text());
										}
									}
									win.dialog('close');
									return false;
								});

					} else {
						$(target).on('click', 'button.pick', function() {
							var checkbox = $('tbody :checked', target);
							var ids = [], names = [];
							checkbox.each(function() {
								var cell = $($(this).closest('tr')[0].cells[options.idindex]);
								var id = options.idindex == 0
										? $(this).val()
										: cell.data('cellvalue') || cell.text();
								cell = $($(this).closest('tr')[0].cells[options.nameindex]);
								var name = cell.data('cellvalue')
										|| cell.text();
								ids.push(id);
								names.push(name);
							});
							var separator = options.separator;
							if (options.name) {
								var nametarget = find(options.name, $(target)
												.data('listpick'));
								var name = names.join(separator);
								if (nametarget.is(':input')) {
									var _names = val(options.name, $(target)
													.data('listpick'))
											|| '';
									val(
											options.name,
											$(target).data('listpick'),
											ArrayUtils
													.unique((_names
															+ (_names
																	? separator
																	: '') + name)
															.split(separator))
													.join(separator));
								} else {
									var picked = nametarget.data('picked')
											|| '';
									picked = ArrayUtils.unique(((picked
											? picked + separator
											: '') + name).split(separator))
											.join(separator);
									nametarget.data('picked', picked);
									val(options.name, $(target)
													.data('listpick'), picked);
									$('<a class="remove" href="#">&times;</a>')
											.appendTo(nametarget)
											.click(removeAction);
								}
							}
							if (options.id) {
								var idtarget = find(options.id, $(target)
												.data('listpick'));
								var id = ids.join(separator);
								var _ids = val(options.id, $(target)
												.data('listpick'))
										|| '';
								val(options.id, $(target).data('listpick'),
										ArrayUtils.unique((_ids
												+ (_ids ? separator : '') + id)
												.split(separator))
												.join(separator));
							}
							win.dialog('close');
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
							replacement : winid + ':content',
							quiet : true
						});

			};
			var handle = current.find('.listpick-handle');
			if (!handle.length)
				handle = current;
			handle.css('cursor', 'pointer').click(func).keydown(
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
	$$('.listpick', container).listpick();
};