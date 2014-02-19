Richtable = {
	getBaseUrl : function(form) {
		form = form || $('form.richtable');
		var action = form.attr('action');
		var entity = form.data('entity');
		var url;
		if (action.indexOf('/') == 0 || action.indexOf('://') > 0)
			url = entity ? action.substring(0, action.lastIndexOf('/') + 1)
					+ entity : action;
		else
			url = entity ? entity : form.attr('action');
		if (form.data('actionbaseurl'))
			url = form.data('actionbaseurl');
		var p = url.indexOf('?');
		if (p > 0)
			url = url.substring(0, p);
		p = url.indexOf(';');
		if (p > 0)
			url = url.substring(0, p);
		if (url.indexOf('/') != 0 && url.indexOf('://') < 0) {
			var hash = document.location.hash;
			if (hash.indexOf('!') == 1)
				url = CONTEXT_PATH
						+ hash.substring(2, hash.lastIndexOf('/') + 1) + url;
		}
		return url;
	},
	getPathParams : function() {
		var url = document.location.href;
		var p = url.indexOf('?');
		if (p > 0)
			url = url.substring(0, p);
		p = url.indexOf(';');
		if (p > 0)
			return url.substring(p);
		else
			return '';
	},
	getUrl : function(type, id, includeParams, form) {
		form = form || $('form.richtable');
		var url = Richtable.getBaseUrl(form) + '/' + type
				+ Richtable.getPathParams();
		if (id) {
			url += (url.indexOf('?') > 0 ? '&' : '?');
			if (typeof id == 'string') {
				url += 'id=' + id;
			} else {
				var ids = [];
				for (var i = 0; i < id.length; i++)
					ids.push('id=' + id[i]);
				url += ids.join('&');
			}
		}
		var data = $('input[type="hidden"],input[name="keyword"]', form)
				.serialize();
		if (includeParams && data)
			url += (url.indexOf('?') > 0 ? '&' : '?') + data;
		return url;
	},
	open : function(url, reloadonclose, useiframe, form) {
		form = form || $('form.richtable');
		reloadonclose = reloadonclose || false;
		useiframe = useiframe || false;
		var win = $('#_window_');
		if (!win.length)
			win = $('<div id="_window_"></div>').appendTo(document.body)
					.dialog();
		if (!useiframe) {
			// ajax replace
			var target = win.get(0);
			target.onsuccess = function() {
				if (typeof $.fn.mask != 'undefined')
					win.unmask();
				Dialog.adapt(win);
				if (url.indexOf('?') > 0)
					url = url.substring(0, url.indexOf('?'));
				var pathname = document.location.pathname;
				var hash = document.location.hash;
				if (hash.indexOf('!') == 1)
					pathname = CONTEXT_PATH + hash.substring(2);
				$('#_window_ form.ajax').each(function() {
					var inputform = $(this);
					$(':input:visible', inputform).filter(function(i) {
						return $(this).attr('name')
								&& !($(this).val() || $(this).hasClass('date')
										|| $(this).hasClass('datetime')
										|| $(this).hasClass('time') || $(this)
										.prop('tagName') == 'BUTTON');
					}).eq(0).focus();
					if (!inputform.hasClass('keepopen')) {
						$(':input', inputform).change(function(e) {
									if (!inputform.hasClass('nodirty'))
										inputform.addClass('dirty');
								});
						$(inputform).addClass('dontreload');
						var create = url.lastIndexOf('input') == url.length - 5;
						if (create) {
							if ($('input[type="hidden"][name="id"]', inputform)
									.val())
								create = false;
							if ($(
									'input[type="hidden"][name="'
											+ (form.data('entity') || form
													.attr('action')) + '.id"]',
									inputform).val())
								create = false;
						}
						if (create) {
							$('button[type="submit"]', inputform)
									.addClass('btn-primary')
									.after(' <button type="submit" class="btn save_and_create">'
											+ MessageBundle
													.get('save.and.create')
											+ '</button>');
							$('.save_and_create', inputform).click(function() {
										$('form.ajax').addClass('reset');
									});
						}
					}
					var action = inputform.attr('action');
					if (action.indexOf('http') != 0 && action.indexOf('/') != 0) {
						action = pathname
								+ (pathname.indexOf('/') == (pathname.length - 1)
										? ''
										: '/') + action;
						inputform.attr('action', action);
					}
					if (inputform.hasClass('view')
							&& !(inputform.data('replacement')))
						inputform.data('replacement', '_window_:content');
					if (!inputform.hasClass('view')
							&& !inputform.hasClass('keepopen')) {
						$('button[type=submit]', inputform).click(function(e) {
							inputform[0].onsuccess = function() {
								$(this).removeClass('dirty');
								$(this).removeClass('dontreload');
								if (!$(e.target).closest('button')
										.hasClass('save_and_create'))
									// setTimeout(function()
									// {
									$('#_window_').dialog('close');
								// }, 1000);

							};
						});
					}
				});
				$('#_window_ a').each(function() {
					var href = $(this).attr('href');
					if (href && href.indexOf('http') != 0
							&& href.indexOf('/') != 0
							&& href.indexOf('javascript:') != 0) {
						href = pathname
								+ (pathname.indexOf('/') == (pathname.length - 1)
										? ''
										: '/') + href;
						this.href = href;
					}
				});
			};
			ajax({
						url : url,
						cache : false,
						target : target,
						replacement : '_window_:content',
						quiet : true
					});
		} else {
			// embed iframe
			win.html('<iframe style="width:100%;height:550px;border:0;"/>');
			url += (url.indexOf('?') > 0 ? '&' : '?') + 'decorator=simple&'
					+ Math.random();
			var iframe = $('#_window_ > iframe')[0];
			iframe.src = url;
			iframe.onload = function() {
				Dialog.adapt(win, iframe);
			}
		}
		if (!useiframe)
			if (win.html() && typeof $.fn.mask != 'undefined')
				win.mask(MessageBundle.get('ajax.loading'));
			else
				win.html('<div style="text-align:center;">'
						+ MessageBundle.get('ajax.loading') + '</div>');
		var opt = {
			minHeight : 600,
			width : 700,
			// modal : true,
			closeOnEscape : false,
			close : function() {
				if (reloadonclose
						&& ($('#_window_ form.ajax').hasClass('forcereload') || !$('#_window_ form.ajax')
								.hasClass('dontreload'))) {
					$('.action .reload', form).addClass('clicked');
					$(form).submit();
				}
				win.html('').dialog('destroy').remove();
			},
			beforeClose : function(event, ui) {
				var form = $('form', win);
				if (!form.length && $('iframe', win).length) {
					try {
						var iframe = $('iframe', win)[0];
						var doc = iframe.document;
						if (iframe.contentDocument) {
							doc = iframe.contentDocument;
						} else if (iframe.contentWindow) {
							doc = iframe.contentWindow.document;
						}
						form = $('form', doc);
					} catch (e) {

					}
				}
				if (form.hasClass('dirty')) {
					return confirm($('form', win).data('confirm')
							|| MessageBundle.get('confirm.exit'));
				}
			}
		};
		if ($.browser.msie && $.browser.version <= 8)
			opt.height = 600;
		win.dialog(opt);
		win.dialog('open');
		win.closest('.ui-dialog').css('z-index', '2000');
		$('.ui-dialog-titlebar-close', win.closest('.ui-dialog')).blur();
	},
	click : function(event) {
		var btn = event.target;
		var form = $(btn).closest('form');
		if ($(btn).prop('tagName') != 'BUTTON' || $(btn).prop('tagName') != 'A')
			btn = $(btn).closest('button,a');
		if (btn.attr('onclick') || btn.hasClass('raw'))
			return;
		var idparams;
		var tr = $(btn).closest('tr');
		var id = tr.data('rowid')
				|| $('input[type="checkbox"]:eq(0)', tr).val();
		if (id) {
			idparams = 'id=' + id;
		} else {
			var arr = [];
			$('form.richtable tbody input[type="checkbox"]').each(function() {
				if (this.checked) {
					var _id = $(this).closest('tr').data('rowid') || this.value;
					arr.push('id=' + _id);
				}
			});
			idparams = arr.join('&');
		}
		var action = $(btn).data('action');
		if (action)
			btn.addClass('clicked');
		var view = $(btn).data('view');
		if (action == 'save')
			Richtable.save(event);
		else if (action) {
			if (!idparams) {
				Message.showMessage('no.selection');
				return false;
			}
			if (action == 'delete') {
				$.alerts.confirm($(btn).data('confirm')
								|| MessageBundle.get('confirm.delete'),
						MessageBundle.get('select'), function(b) {
							if (b) {
								var url = Richtable.getBaseUrl(form) + '/'
										+ action + Richtable.getPathParams();
								url += (url.indexOf('?') > 0 ? '&' : '?')
										+ idparams;
								ajax({
											url : url,
											type : 'POST',
											dataType : 'json',
											success : function() {
												$(form).submit();
											}
										});
							}
						});
			} else {
				var url = Richtable.getBaseUrl(form) + '/' + action
						+ Richtable.getPathParams();
				url += (url.indexOf('?') > 0 ? '&' : '?') + idparams;
				var action = function() {
					ajax({
								url : url,
								type : 'POST',
								dataType : 'json',
								success : function() {
									$(form).submit();
								}
							});
				}
				if ($(btn).hasClass('confirm')) {
					$.alerts.confirm($(btn).data('confirm')
									|| MessageBundle.get('confirm.action'),
							MessageBundle.get('select'), function(b) {
								if (b) {
									action();
								}
							});
				} else {
					action();
				}

			}
		} else {
			var options = (new Function("return "
					+ ($(btn).data('windowoptions') || '{}')))();
			var url = $(btn).attr('href');
			if (view) {
				url = Richtable.getUrl(view, id, !id || options.includeParams,
						form);
			} else {
				if (!$(btn).hasClass('noid')) {
					if (!id) {
						// from bottom
						if (!idparams) {
							Message.showMessage('no.selection');
							return false;
						}
						if (!url)
							return true;
						url += (url.indexOf('?') > 0 ? '&' : '?') + idparams;
					}
				}
			}
			var reloadonclose = typeof(options.reloadonclose) == 'undefined'
					? (view != 'view')
					: options.reloadonclose;
			Richtable.open(url, reloadonclose, options.iframe, form);
			delete options.iframe;
			delete options.reloadonclose;
			for (var key in options)
				$('#_window_').dialog('option', key, options[key]);
			Dialog.adapt($('#_window_'));
			return false;
		}
	},
	save : function(event) {
		var action = function() {
			var form = $(event.target).closest('form');
			var versionproperty = form.data('versionproperty');
			var modified = false;
			var theadCells = $('.richtable thead:eq(0) th');
			$('.richtable tbody tr', form).each(function() {
				var row = this;
				if ($('td.edited', row).length) {
					modified = true;
					var entity = form.data('entity') || form.attr('action');
					var params = {};
					var version = $(row).data('version');
					if (version != undefined)
						params[entity + '.' + (versionproperty || 'version')] = version;
					params[entity + '.id'] = $(this).data('rowid')
							|| $('input[type="checkbox"]:eq(0)', this).val();
					$.each(row.cells, function(i) {
						var theadCell = $(theadCells[i]);
						var name = theadCell.data('cellname');
						if (!name || !$(this).hasClass('edited')
								&& theadCell.hasClass('excludeIfNotEdited'))
							return;
						var value = $(this).data('cellvalue') || $(this).text();
						params[name] = value;
					});
					var url = Richtable.getBaseUrl(form) + '/save'
							+ Richtable.getPathParams();
					ajax({
								url : url,
								type : 'POST',
								data : params,
								dataType : 'json',
								headers : {
									'X-Edit' : 'cell'
								},
								onsuccess : function() {
									$('td', row).removeClass('edited')
											.removeData('oldvalue');
									if (version != undefined)
										$(row).data('version', version + 1);
									$('[data-action="save"]', form)
											.removeClass('btn-primary').hide();
								}
							});
				}
			});
			if (!modified) {
				Message.showMessage('no.modification');
				return false;
			}
		}

		var btn = event.target;
		if ($(btn).prop('tagName') != 'BUTTON' || $(btn).prop('tagName') != 'A')
			btn = $(btn).closest('button,a');
		if ($(btn).hasClass('confirm')) {
			$.alerts.confirm($(btn).data('confirm')
							|| MessageBundle.get('confirm.save'), MessageBundle
							.get('select'), function(b) {
						if (b) {
							action();
						}
					});
		} else {
			action();
		}
	},
	editCell : function(cell, type, templateId) {
		var cell = $(cell);
		if (cell.hasClass('editing'))
			return;
		var value = cell.data('cellvalue');
		if (value === undefined)
			value = $.trim(cell.text());
		else
			value = '' + value;
		if (cell.data('oldvalue') === undefined)
			cell.data('oldvalue', value);
		cell.addClass('editing');
		var template = '';
		if (templateId) {
			template = $('#' + templateId).text();
		} else {
			if (type == 'textarea')
				template = '<textarea type="text" class="text"/>';
			else if (type == 'boolean')
				template = '<select><option value="true">'
						+ MessageBundle.get('true')
						+ '</option><option value="false">'
						+ MessageBundle.get('false') + '</option></select>';
			else
				template = '<input type="text" class="text"/>';
		}
		cell.html(template);
		var input = $(':input', cell).val(value).blur(function() {
					if (!$(this).is('.date,.datetime,.time'))
						Richtable.updateCell(this);
				});
		if (type == 'date' || type == 'datetime' || type == 'time') {
			var option = {
				language : MessageBundle.lang().replace('_', '-')
			};
			if (type == 'datetime') {
				option.format = input.data('format') || 'yyyy-MM-dd HH:mm:ss';
			} else if (type == 'time') {
				option.format = input.data('format') || 'HH:mm:ss';
				option.pickDate = false;
			} else {
				option.format = input.data('format') || 'yyyy-MM-dd';
				option.pickTime = false;
			}
			input.addClass(type).datetimepicker(option).bind('hide',
					function(e) {
						Richtable.updateCell(this)
					});
		}
		input.focus();
	},
	updateCell : function(cellEdit) {
		var ce = $(cellEdit);
		var cell = ce.parent();
		var value = ce.val();
		var label = value;
		var editType = ce.prop('tagName');
		if (editType == 'SELECT')
			label = $('option:selected', ce).text();
		else if (editType == 'CHECKBOX' || editType == 'RADIO')
			label = ce.next().text();
		Richtable.updateValue(cell, value, label);
	},
	updateValue : function(cell, value, label) {
		if (cell.data('oldvalue') === undefined)
			cell.data('oldvalue', '' + cell.data('cellvalue'));
		cell.removeClass('editing');
		cell.data('cellvalue', value);
		if (typeof label != 'undefined')
			cell.text(label);
		if (cell.data('oldvalue') != cell.data('cellvalue')) {
			cell.addClass('edited');
			cell.removeAttr('data-tooltip');
		} else
			cell.removeClass('edited');
		var savebtn = $('[data-action="save"]', cell.closest('form'));
		$('td.edited', cell.closest('form')).length ? savebtn
				.addClass('btn-primary').show() : savebtn
				.removeClass('btn-primary').hide();
	},
	enhance : function(table) {
		var t = $(table);
		var theadCells = $('thead:eq(0) th', t);
		$('tbody:eq(0) tr', t).each(function() {
			var cells = this.cells;
			if (!$(this).data('readonly'))
				theadCells.each(function(i) {
							var cellEdit = $(this).data('celledit');
							if (!cellEdit)
								return;
							var ar = cellEdit.split(',');
							var action = ar[0];
							var type = ar[1];
							var template = ar[2];
							if (action != 'click' && action != 'dblclick') {
								template = type;
								type = action;
								action = 'click';
							}
							if (!$(cells[i]).data('readonly'))
								$(cells[i]).unbind(action).bind(action,
										function() {
											Richtable.editCell(this, type,
													template);
										});
						});
		});

		var need = false;
		var classes = {};
		$('th', t).each(function(i) {
					var arr = [];
					var tt = $(this);
					var cls = tt.attr('class');
					if (cls)
						$.each(cls.split(/\s+/), function(i, v) {
									if (v.indexOf('hidden-') == 0)
										arr.push(v);
								});
					if (arr.length) {
						need = true;
						classes['' + i] = arr;
					}
				});
		$('tbody tr', t).each(function() {
					$('td', $(this)).each(function(i) {
								var arr = classes['' + i];
								var tt = $(this);
								if (arr) {
									$.each(arr, function(i, v) {
												tt.addClass(v);
											});
								}
							});
				});
	}
};
Initialization.richtable = function() {
	$(document)
			.on(
					'click',
					'.richtable .action [data-view],.richtable .action [data-action],form.richtable a[rel="richtable"]',
					Richtable.click).on('click', '.richtable .action .reload',
					function() {
						$(this).closest('.reload').addClass('clicked')
								.closest('form').submit();
					}).on('click', '.richtable .action .filter', function() {
						var f = $(this).closest('form').next('form.criteria');
						f.toggle();
						if (f.is(':visible'))
							$('html,body').animate({
										scrollTop : f.offset().top - 50
									}, 100);
					}).on('click', '.richtable .more', function(event) {
				var form = $(event.target).closest('form');
				if (!$('li.nextPage', form).length)
					return;
				$('.inputPage', form).val(function(i, v) {
							return parseInt(v) + 1
						});
				$.ajax({
					url : $(form).attr('action'),
					type : $(form).attr('method'),
					data : form.serialize(),
					success : function(data) {
						var html = data
								.replace(/<script(.|\s)*?\/script>/g, '');
						var div = $('<div/>').html(html);
						var append = false;
						$('table.richtable tbody:eq(0) tr', div).each(
								function(i, v) {
									if (!append) {
										var id = $(v).data('rowid')
												|| $(
														'input[type="checkbox"],input[type="radio"]',
														v).prop('value');
										if (id) {
											var rows = $(
													'table.richtable tbody:eq(0) tr',
													form);
											var exists = false;
											for (var i = rows.length - 1; i >= 0; i--) {
												if (($(rows[i]).data('rowid') || $(
														'input[type="checkbox"],input[type="radio"]',
														rows[i]).prop('value')) == id) {
													exists = true;
													break;
												}
											}
											if (!exists)
												append = true;
										} else {
											append = true;
										}
									}
									if (append) {
										$(v).appendTo($(
												'table.richtable tbody', form));
										_observe(v);
									}
								});
						if (append)
							Richtable.enhance($('table.richtable', form));
						$('.pageSize', form).val($('table.richtable tbody tr',
								form).length);
						$('div.pagination', form).replaceWith($(
								'div.pagination', div));
						$('div.pagination ul', form).hide();
						$('div.status', form).replaceWith($('div.status', div));
					}
				});

			});
}
Observation._richtable = function(container) {
	$('form.criteria', container).each(function() {
		var t = $(this);
		var f = t.prev('form.richtable');
		var entity = f.data('entity') || f.attr('action');
		t.attr('action', f.attr('action')).data('replacement', f.attr('id'));
		$('input[type="hidden"]', f).clone().prependTo(t).each(function() {
			var name = $(this).attr('name');
			if (name.indexOf('-op') == name.length - 3)
				name = name.substring(0, name.length - 3);
			if (name.indexOf(entity + '.') == 0)
				name = name.substring(name.indexOf('.') + 1);
			$('table.criteria select.property option', t).each(function() {
				var v = $(this).attr('value');
				if (v == name || v.indexOf(name + '.') == 0
						|| name.indexOf(v + '.') == 0)
					$(this).remove();
			});
			if (name.indexOf('-od') == name.length - 3) {
				name = name.substring(0, name.length - 3);
				$('table.ordering select.property option', t).each(function() {
					var v = $(this).attr('value');
					if (v == name || v.indexOf(name + '.') == 0
							|| name.indexOf(v + '.') == 0)
						$(this).remove();
				});
			}
		});
		$('table.criteria select.property', t).change(function() {
			var t = $(this);
			var operator = $('select.operator', t.closest('tr'));
			if (t.val()) {
				operator.attr('name', t.val() + '-op');
				$('td:eq(2) :input', t.closest('tr')).attr('name', t.val());
				var ops = $('option:selected', t).data('operators');
				ops = ops.substring(1, ops.length - 1);
				ops = ops.split(', ');
				var val;
				$('option', operator).each(function() {
							var temp = $(this).attr('value');
							if (jQuery.inArray(temp, ops) < 0) {
								$(this).prop('disabled', true).css('display',
										'none');
							} else {
								if (!val)
									val = temp;
								$(this).prop('disabled', false).css('display',
										'');
							}
						});
				operator.val(val);
				operator.change();
			} else {
				operator.removeAttr('name');
				$('td:eq(2) :input', t.closest('tr')).removeAttr('name');
				$('option', operator).prop('disabled', false)
						.css('display', '');
			}
		});
		$('table.criteria select.operator', t).change(function() {
			var t = $(this);
			var property = $('select.property', t.closest('tr'));
			var option = $('option:selected', property);
			var size = parseInt($('option:selected', t).data('parameters'));
			var td = $('td:eq(2)', t.closest('tr'));
			$(':input,.removeonadd,label', td).remove();
			if (size > 0) {
				if ('select' == option.data('type')) {
					var select = $('<select name="' + property.val()
							+ '" class="removeonadd ' + option.data('class')
							+ '"></select>').appendTo(td);
					if (!select.hasClass('required'))
						$('<option value=""></option>').appendTo(select);
					var map = option.data('map');
					map = map.substring(1, map.length - 1);
					map = map.split(', ');
					for (var i = 0; i < map.length; i++) {
						var arr = map[i].split('=', 2);
						$('<option value="' + arr[0] + '">'
								+ (arr[1] || arr[0]) + '</option>')
								.appendTo(select);
					}
				} else if ('listpick' == option.data('type')) {
					$('<input id="filter_'
							+ property.val().replace(/\./g, '_')
							+ '" type="hidden" name="'
							+ property.val()
							+ '" class="required"/><span class="listpick removeonadd" data-options="{\'url\':\''
							+ option.data('pickurl')
							+ '\',\'name\':\'this\',\'id\':\'#filter_'
							+ property.val().replace(/\./g, '_')
							+ '\'}"></span>').appendTo(td);
				} else {
					$('<input type="' + (option.data('inputtype') || 'text')
							+ '" name="' + property.val()
							+ '" class="input-medium removeonadd '
							+ option.data('class') + '"/>').appendTo(td);
				}
				if (size == 2)
					$(':input', td).clone().appendTo(td).css('margin-left',
							'10px');
				_observe(td);
			} else if (size < 0 && 'select' == option.data('type')) {

				var map = option.data('map');
				map = map.substring(1, map.length - 1);
				map = map.split(', ');
				for (var i = 0; i < map.length; i++) {
					var arr = map[i].split('=', 2);
					var cbid = '-filter-' + property.val() + '-' + i;
					$('<label for="'
							+ cbid
							+ '" class="checkbox inline"><input type="checkbox" name="'
							+ property.val()
							+ '" value="'
							+ arr[0]
							+ '" id="'
							+ cbid
							+ '" class="custom" style="display: none;" /><label class="custom" for="'
							+ cbid + '"></label>' + (arr[1] || arr[0])
							+ '</label>').appendTo(td);
				}
			}
		});
		$('table.ordering select.property', t).change(function() {
					var t = $(this);
					var ordering = $('select.ordering', t.closest('tr'));
					if (t.val()) {
						ordering.attr('name', t.val() + '-od');
					} else {
						ordering.removeAttr('name');
					}
				});
		$('button.restore', t).click(function() {
			var b;
			$(':input[name]', t).each(function() {
				var h = $('input[type="hidden"][name="' + $(this).attr('name')
								+ '"]', f);
				if (h.length)
					b = true;
				h.remove();
			});
			if (b) {
				$('.inputPage', f).val(1);
				f.submit();
			}
			$(
					'table.criteria tbody tr:not(:eq(0)),table.ordering tbody tr:not(:eq(0))',
					t).remove();
			$('tr select option', t).prop('disabled', false).css('display', '');
			$('tr td:eq(2)', t).text('');
			$('select.property', t).val('');
			$('select.operator', t).removeAttr('name').val('EQ');
			$('select.ordering', t).removeAttr('name').val('asc');
			t.hide();
		});
	});
	$('table.richtable', container)
			.on(
					'change',
					'input[type="checkbox"][name="check"],input[type="checkbox"]:not([name])',
					function() {
						var rows = [];
						if ($(this).attr('name') === undefined) {
							if (this.checked)
								$('tbody tr',
										$(this).closest('table.richtable'))
										.each(function() {
													rows.push(this);
												});
						} else {
							$('tbody tr', $(this).closest('table.richtable'))
									.each(function() {
										if ($(
												'td:eq(0) input[type="checkbox"]',
												this).is(':checked'))
											rows.push(this);
									});
						}
						var form = $(this).closest('form.richtable');
						$('.toolbar [data-shown]', form).each(function() {
							var t = $(this);
							var filter = t.data('filterselector');
							var allmatch = t.data('allmatch');
							if (allmatch == undefined)
								allmatch = true;
							var count = 0;
							$.each(rows, function(i, v) {
										var row = $(v);
										try {
											if (!filter || row.is(filter)
													|| row.find(filter) > 0)
												count++;
										} catch (e) {

										}
									});
							t.is('[data-shown="selected"]')
									&& (!allmatch || count == rows.length)
									&& count > 0
									|| t.is('[data-shown="singleselected"]')
									&& (!allmatch || count == rows.length)
									&& count == 1
									|| t.is('[data-shown="multiselected"]')
									&& (!allmatch || count == rows.length)
									&& count > 1 ? t.addClass('btn-primary')
									.show() : t.removeClass('btn-primary')
									.hide();
						});

					});
	$('table.richtable', container).each(function() {
				Richtable.enhance(this);
			});
};