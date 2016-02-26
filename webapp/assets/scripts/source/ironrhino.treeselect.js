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
		current = $(event.target).closest('.treeselect');
		var options = current.data('_options');
		var nametarget = find(options.name);
		val(options.name, current, nametarget.is(':input,td')
						? ''
						: '<i class="glyphicon glyphicon-list"></i>', true);
		val(options.id, current, '');
		if (options.id) {
			var idtarget = find(options.id);
			idtarget.removeData('treenode');
		}
		$(this).remove();
		event.stopPropagation();
		return false;

	}
	$.fn.treeselect = function() {
		$(this).each(function() {
			current = $(this);
			var options = {
				idproperty : 'id',
				separator : '',
				id : '.treeselect-id',
				name : '.treeselect-name',
				full : true
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
				current = $(event.target).closest('.treeselect');
				var winid = '_tree_window';
				current.data('winid', winid);
				$('#' + winid).remove();
				var win = $('<div id="' + winid + '" title="'
						+ MessageBundle.get('select')
						+ '"><div class="tree"></div></div>')
						.appendTo(document.body);
				win.dialog({
							width : current.data('_options').width || 500,
							minHeight : current.data('_options').minHeight
									|| 500,
							close : function() {
								win.html('').dialog('destroy').remove();
							}
						});
				win.closest('.ui-dialog').css('z-index', 2500);
				if (nametarget && nametarget.length)
					options.value = val(options.name, current) || '';
				if (options.type != 'treeview') {
					options.click = function(treenode) {
						doclick(current, treenode, options);
					};
					win.find('.tree').treearea(options);
				} else {
					var treeviewoptions = {
						url : options.url,
						click : function() {
							var treenode = $(this).closest('li')
									.data('treenode');
							doclick(current, treenode, options);
						},
						collapsed : true,
						placeholder : MessageBundle.get('ajax.loading'),
						unique : true,
						separator : options.separator,
						value : options.value,
						root : options.root
					};
					win.find('.tree').treeview(treeviewoptions);
				}

			};
			var handle = current.find('.treeselect-handle');
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

	function doclick(current, treenode, options) {
		if (options.name) {
			var nametarget = find(options.name, current);
			var name = options.full ? treenode.fullname : treenode.name;
			val(options.name, current, name);
			if (!nametarget.is(':input'))
				$('<a class="remove" href="#">&times;</a>')
						.appendTo(nametarget).click(removeAction);
		}
		if (options.id) {
			var idtarget = find(options.id, current);
			var id = treenode[options.idproperty];
			val(options.id, current, id);
			if (idtarget.is(':input'))
				idtarget.data('treenode', treenode);
		}
		$('#' + current.data('winid')).dialog('close');
		if (options.select)
			options.select(treenode);
	}

})(jQuery);

Observation.treeselect = function(container) {
	$$('.treeselect', container).treeselect();
};