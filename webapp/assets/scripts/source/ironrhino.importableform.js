(function($) {

	$.fn.importableform = function() {
		this.each(function() {
			var t = $(this);
			var button = $(
					' <button type="button" class="btn">'
							+ MessageBundle.get('import') + '</button>')
					.appendTo($('.form-actions', t)).click(
							function() {
								$('<input type="file"/>').appendTo(t).hide()
										.change(function() {
											fill(this.files, t);
											$(this).remove();
										}).click();
							});
			$(t).bind('dragover', function(e) {
				$(this).addClass('drophover');
				return false;
			}).bind('dragleave', function(e) {
				$(this).removeClass('drophover');
				return false;
			}).get(0).ondrop = function(e) {
				e.preventDefault();
				$(this).removeClass('drophover');
				fill(e.dataTransfer.files, t);
				return true;
			};
		});
		return this;
	}

	function fill(files, form) {
		var maximum = parseInt(form.data('maximum') || 1024 * 1024);
		var file = files[0];
		if (file.size > maximum) {
			Message.showActionError(MessageBundle.get('maximum.exceeded',
					file.size, maximum));
			return;
		}
		var reader = new FileReader();
		reader.onload = function(e) {
			var data;
			var json = e.target.result;
			try {
				var data = $.parseJSON(json);
			} catch (e) {
				Message.showActionError(MessageBundle.get('data.invalid'));
				return;
			}
			$(':input:not([readonly])', form).each(function() {
				var input = $(this);
				var name = input.attr('name');
				if (!name || name.indexOf('__') > 0 || name.indexOf('[') > 0)
					return;
				var value = data[name];
				if (!value && name.indexOf('.') > 0)
					value = data[name.substring(name.indexOf('.') + 1)];
				bind_value(value, input);
			});
			filter(form, 'table.datagrided').each(function() {
				var t = $(this);
				var name = $(':input', $('tbody tr:eq(0)', t)).attr('name');
				var i = name.indexOf('[');
				if (i > 0) {
					var prefix = name.substring(0, i);
					var list = data[prefix];
					var j = prefix.indexOf('.');
					if (!list && j > 0)
						list = data[prefix.substring(j + 1)];
					if (list instanceof Array) {
						bind_datagrid(list, t, 1);
					}
				}

			});
			$(':submit:eq(0)', form).focus();
		}
		reader.readAsText(file);
	}

	function bind_datagrid(list, datagrid, currentlevel) {
		if (!list || !list.length || !datagrid)
			return;
		var rows = datagrid.children('tbody').children('tr');
		rows.each(function(n, v) {
			if (n >= list.length)
				$(this).remove();
		});
		var k = list.length - rows.length;
		datagrid
				.datagridTable('addRows', k)
				.children('tbody')
				.children('tr')
				.each(
						function(n, v) {
							var rowdata = list[n];
							$(':input:not([readonly])', v)
									.each(
											function() {
												var name = $(this).attr('name');
												if (!name)
													return;
												if (name.split('[').length - 1 > currentlevel)
													return;
												var index = -1;
												for ( var x = 0; x < currentlevel; x++)
													index = name.indexOf(']',
															index + 1);
												if (name.lastIndexOf(']') != name.length - 1) {
													name = name
															.substring(index + 2);
													var v = rowdata[name];
													bind_value(v, $(this));
												} else {
													var v = list[parseInt(name
															.substring(
																	name
																			.lastIndexOf('[') + 1,
																	name
																			.lastIndexOf(']')))];
													bind_value(v, $(this));
												}

											});
							filter($(v), 'table.datagrided').each(
									function(k, dg) {
										var name = $(':input[name]', dg).attr(
												'name');
										var index = -1;
										for ( var x = 0; x < currentlevel; x++)
											index = name
													.indexOf(']', index + 1);
										name = name.substring(index + 2);
										name = name.substring(0, name
												.indexOf('['));
										bind_datagrid(rowdata[name], $(dg),
												currentlevel + 1);
									});

						});
	}

	function bind_value(value, input) {
		if (typeof value != 'undefined') {
			if (input.is('select[multiple]')) {
				if (typeof value == 'string')
					value = value.split(',');
				$('option', input).prop('selected', false).each(function() {
					if ($.inArray($(this).attr('value'), value) > -1)
						$(this).prop('selected', true);
				});
			} else if (input.is('input[type="checkbox"]'))
				input.prop('checked', value);
			else
				input.val(value);
			input.trigger('change');
			if (input.hasClass('chosen'))
				input.trigger('chosen:updated.chosen');
		}
	}

	function filter(container, selector) {
		return $(selector, container).filter(function() {
			var count = 0, parent = $(this);
			while ((parent = parent.parent()).length) {
				if (parent[0] == container[0])
					break;
				if (parent.is(selector))
					count++;
			}
			return count == 0;
		});
	}

})(jQuery);

if (window.FileReader)
	Observation.importableform = function(container) {
		$('form.importable', container).importableform();
	};