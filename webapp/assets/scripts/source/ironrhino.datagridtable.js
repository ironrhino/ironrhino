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
			var tbody = $(this).addClass('datagrided').children('tbody');
			tbody.sortable({
						// handle : '.glyphicon-plus',
						helper : function(e, tr) {
							var originals = tr.children();
							var helper = tr.clone();
							helper.children().each(function(index) {
										$(this).width(originals.eq(index)
												.width())
									});
							return helper;
						},
						opacity : 0.6,
						update : function(event, ui) {
							rename(tbody);
							if (options.onsort)
								options.onsort.apply(tbody.get(0));
						}
					});
			$('td.manipulate,th.manipulate', this).each(function() {
				var t = $(this);
				if (!t.html()) {
					if (t.parent().parent().prop('tagName') == 'THEAD') {
						t
								.html('<i class="glyphicon glyphicon-plus manipulate add clickable"></i>');
					} else {
						t
								.html('<i class="glyphicon glyphicon-plus manipulate add clickable"></i><i class="glyphicon glyphicon-minus manipulate remove clickable"></i>');
					}
				}
			});
			if ($(this).parents('.datagrided').length)
				return;
			if ($(this).hasClass('keydown')) {
				$('tbody input:last', this).keydown(function(event) {
							if (event.keyCode == 13
									&& !$(this).hasClass('tags')) {
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
			}
			$('thead .add', this).click(function(event) {
				var table = $(event.target).closest('table.datagrided');
				var row = table.children('tbody')
						.children(':not(.nontemplate):eq(0)');
				if (!row.length)
					row = table.children('tfoot').children('tr:hidden');
				if (row.length > 0)
					addRow(event, options, row.eq(0), true);
			});
			$('tbody td > .add', this).click(function(event) {
						addRow(event, options)
					});
			$('tbody .remove', this).click(function(event) {
						removeRow(event, options)
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
			var currentrows = table.children('tbody').children('tr').length;
			if (currentrows == maxrows) {
				Message.showActionError(MessageBundle.get('max.rows.reached',
								currentrows), table.closest('form'));
				return;
			}
		}
		var r = row.clone(true);
		if (r.is(':hidden'))
			r.show().find('._disabled:input').removeClass('_disabled').prop(
					'disabled', false);
		$('*', r).removeAttr('id');
		$('.resettable', r).html('');
		$('span.info', r).html('');
		$(':input[type!=checkbox][type!=radio]:not(.fixedvalue)', r).val('')
				.change();
		$('select', r).each(function() {
					var option = $('option:first', this);
					if (!option.prop('selected')) {
						option.prop('selected', true);
						$(this).change();
					}
				});
		$('input[type=checkbox],input[type=radio]', r).prop('checked', false)
				.change();
		$(':input', r).not('.readonly').prop('readonly', false)
				.removeAttr('keyupValidate');
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
		}).change();
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
		$('.datagrided tbody > tr', r).each(function(i) {
					if (i > 0)
						$(this).remove();
				});
		if (first)
			r.prependTo(table.children('tbody:eq(0)'));
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
		$(
				'.listpick-name:not(.input-pseudo),.treeselect-name:not(.input-pseudo)',
				r).html('<i class="glyphicon glyphicon-list"></i>');
		$('.removeonadd,.field-error', r).remove();
		$('.error', r).removeClass('error');
		$('.hideonadd', r).hide();
		$('.showonadd', r).show();
		$(':input', r).eq(0).focus();
		r.removeClass('required');
		if (options && options.onadd)
			options.onadd.apply(r.get(0));
		table.trigger('addRow');
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
		if (table.hasClass('nullable') && $('tr', tbody).length == 1) {
			var tfoot = table.children('tfoot');
			if (!tfoot.length)
				tfoot = $('<tfoot></tfoot>').appendTo(table);
			$(':input:not([disabled])', row).addClass('_disabled').prop(
					'disabled', true);
			if (tfoot.find('tr:hidden').length)
				row.remove();
			else
				row.appendTo(tfoot).hide();
		} else {
			row.remove();
		}
		rename(tbody);
		if (options.onremove)
			options.onremove();
		table.trigger('removeRow');
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
	$$('table.datagrid', container).datagridTable();
};