(function($) {
	$.fn.datagridTable = function(options) {
		if (arguments.length == 2) {
			if (arguments[0] == 'addRows') {
				var count = arguments[1];
				$(this).each(function() {
					var table = $(this);
					for (var i = 0; i < count; i++)
						addRow({
									target : table.children('tbody')
											.children('tr').first()
											.children('td').last()
								}, null, null, false, true);
					rename(table.children('tbody'));
				});
			}
			return this;
		}

		options = options || {};
		$(this).each(function() {
			if ($(this).hasClass('datagrided'))
				return;
			$(this).addClass('datagrided');
			$('td.manipulate,th.manipulate', this).css({
						'width' : '80px',
						'padding-left' : '10px',
						'text-align' : 'left'
					}).each(function() {
				var t = $(this);
				if (!t.html()) {
					if (t.parent().parent().prop('tagName') == 'THEAD') {
						t
								.html('<i class="glyphicon glyphicon-plus manipulate add clickable"></i>');
					} else {
						t
								.html('<i class="glyphicon glyphicon-plus manipulate add clickable"></i><i class="glyphicon glyphicon-minus manipulate remove clickable"></i><i class="glyphicon glyphicon-arrow-up manipulate moveup clickable"></i><i class="glyphicon glyphicon-arrow-down manipulate movedown clickable"></i>');
					}
				}
			});
			if ($(this).parents('.datagrided').length)
				return;
			$('tbody input:last', this).keydown(function(event) {
						if (event.keyCode == 13 && !$(this).hasClass('tags')) {
							event.preventDefault();
							addRow(event, options);
						}
					});
			$('tbody input:first', this).keydown(function(event) {
						if (event.keyCode == 8 && !$(event.target).val()) {
							event.preventDefault();
							removeRow(event, options);
						}
					});
			$('thead .add', this).click(function(event) {
				var row = $(event.target).closest('table.datagrided')
						.children('tbody').children(':not(.nontemplate):eq(0)');
				if (row.length > 0)
					addRow(event, options, row.eq(0), true);
			});
			$('tbody .add', this).click(function(event) {
						addRow(event, options)
					});
			$('tbody .remove', this).click(function(event) {
						removeRow(event, options)
					});
			$('tbody .moveup', this).click(function(event) {
						moveupRow(event, options)
					});
			$('tbody .movedown', this).click(function(event) {
						movedownRow(event, options)
					});
		})

		return this;
	};

	var addRow = function(event, options, row, first, skipRename) {
		var current = $(event.target).closest('tr');
		var table = current.closest('table.datagrided');
		var row = row
				|| $(event.target).closest('tbody')
						.children(':not(.nontemplate):eq(0)');
		if (!row.length)
			return;
		var maxrows = parseInt(table.data('maxrows'));
		if (maxrows) {
			var currentrows = $('tbody tr', table).length;
			if (currentrows == maxrows) {
				Message.showActionError(MessageBundle.get('max.rows.reached',
								currentrows), table.closest('form'));
				return;
			}
		}
		var r = row.clone(true);
		$('*', r).removeAttr('id');
		$('span.info', r).html('');
		$(':input[type!=checkbox][type!=radio]', r).val('');
		$('input[type=checkbox],input[type=radio]', r).prop('checked', false);
		$(':input', r).not('.readonly').prop('readonly', false)
				.removeAttr('keyupValidate');
		if (MODERN_BROWSER)
			$('input[type="checkbox"].custom,input[type="radio"].custom', r)
					.each(function(i) {
						$(this).hide();
						if (!this.id)
							this.id = ('a' + (i + Math.random())).replace('.',
									'').substring(0, 9);
						var label = $(this).next('label.custom');
						if (!label.length)
							$(this).after($('<label class="custom" for="'
									+ this.id + '"></label>'));
						else
							label.attr('for', this.id);
					});
		$('select.decrease', r).each(function() {
			var selectedValues = $.map($('select.decrease', table), function(e,
							i) {
						return $(e).val();
					});
			$('option', this).each(function() {
				var t = $(this);
				t.prop('disabled', false).css('display', '');
				var selected = false;
				for (var j = 0; j < selectedValues.length; j++) {
					if (selectedValues[j]
							&& t.attr('value') == selectedValues[j]) {
						selected = true;
						break;
					}
				}
				if (selected)
					t.prop('disabled', true).css('display', 'none');
			});
		});
		$('input.tags', r).each(function() {
			var t = $(this);
			var p = t.closest('.text-core');
			t.attr('name', $('input[type="hidden"]', p).attr('name')).attr(
					'style', '').attr('value', '').show();
			p.replaceWith(t[0].outerHTML);
			setTimeout(function() {
						r.find('input.tags').tags();
					}, 100);
		});
		$('input.imagepick', r).each(function() {
					$(this).removeData('popover').popover({
								'html' : true
							}).change();
				});
		$('.datagrided tr', r).each(function(i) {
					if (i > 0)
						$(this).remove();
				});
		if (first)
			row.parent().prepend(r);
		else
			current.after(r);
		if (!skipRename)
			rename(row.closest('tbody'));
		if (typeof $.fn.chosen != 'undefined') {
			$('.chosen-container', r).remove();
			$('.chosen', r).show().data('chosen', false).chosen({
						search_contains : true,
						placeholder_text : MessageBundle.get('select'),
						no_results_text : ' '
					});
		}
		$('select.textonadd,div.combobox', r).each(function() {
			$(this).replaceWith('<input type="text" name="'
					+ ($(this).attr('name') || $(':input', this).attr('name'))
					+ '">');
		});
		var checkboxname = '';
		$('input.textonadd[type=checkbox]', r).each(function() {
			if (!checkboxname || checkboxname != $(this).attr('name')) {
				$(this).replaceWith('<input type="text" name="'
						+ $(this).attr('name') + '">');
				checkboxname = $(this).attr('name');
			} else {
				$(this).remove();
			}
		});
		$('.listpick-name,.treeselect-name', r)
				.html('<i class="glyphicon glyphicon-list"></i>');
		$('.removeonadd,.field-error', r).remove();
		$('.error', r).removeClass('error');
		$('.hideonadd', r).hide();
		$('.showonadd', r).show();
		$(':input', r).eq(0).focus();
		r.removeClass('required');
		if (options && options.onadd)
			options.onadd.apply(r.get(0));
	};
	var removeRow = function(event, options) {
		var row = $(event.target).closest('tr');
		if (row.hasClass('required'))
			return;
		var tbody = row.closest('tbody');
		var table = tbody.closest('table.datagrided');
		if (!table.hasClass('nullable') && $('tr', tbody).length == 1
				|| options.onbeforeremove
				&& options.onbeforeremove.apply(row.get(0)) === false)
			return;
		$(':input', row.prev()).eq(0).focus();
		row.remove();
		rename(tbody);
		if (options.onremove)
			options.onremove();
	};
	var moveupRow = function(event, options) {
		var row = $(event.target).closest('tr');
		if (row.closest('tbody').children().length > 1) {
			$(row).fadeOut(function() {
						if ($(this).prev().length)
							$(this).insertBefore($(this).prev()).fadeIn();
						else
							$(this).insertAfter($(this).siblings(':last'))
									.fadeIn();
						rename($(this).closest('tbody'));
						if (options.onmoveup)
							options.onmoveup.apply(this);
					});
		}
	};
	var movedownRow = function(event, options) {
		var row = $(event.target).closest('tr');
		if (row.closest('tbody').children().length > 1) {
			$(row).fadeOut(function() {
						if ($(this).next().length)
							$(this).insertAfter($(this).next()).fadeIn();
						else
							$(this).insertBefore($(this).siblings(':first'))
									.fadeIn();
						rename($(this).closest('tbody'));
						if (options.onmovedown)
							options.onmovedown.apply(this);
					});
		}
	};
	var rename = function(tbody) {
		var level = $(tbody).parents('table.datagrided').length;
		$(tbody).children('tr').each(function(i) {
			$(':input', this).each(function() {
				var name = $(this).prop('name');
				var j = -1;
				for (var k = 0; k < level; k++)
					j = name.indexOf('[', j + 1);
				if (j < 0)
					return;
				name = name.substring(0, j + 1) + i
						+ name.substring(name.indexOf(']', j));
				$(this).prop('name', name);
			});
		}).closest('form').addClass('dirty');
	}
})(jQuery);

Observation.datagridTable = function(container) {
	var c = $(container);
	var selector = 'table.datagrid';
	if (c.is(selector))
		c.datagridTable();
	$(selector, c).datagridTable();
};