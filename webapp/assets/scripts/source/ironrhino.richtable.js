Richtable = {
	getBaseUrl : function(form) {
		form = form || $('form.richtable');
		url = form.data('actionbaseurl') || form.prop('action');
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
			if ($.isArray(id)) {
				var ids = [];
				for (var i = 0; i < id.length; i++)
					ids.push('id=' + id[i]);
				url += ids.join('&');
			} else {
				url += 'id=' + id;
			}
		}
		if (includeParams) {
			var action = form.prop('action');
			var qs = '';
			var index = action.indexOf('?');
			if (index > -1)
				qs = action.substring(index + 1);
			$('input[type="hidden"],input[name="keyword"]', form).each(
					function() {
						var name = this.name;
						if (this.value
								&& name
								&& !name.endsWith('-op')
								&& !form.find('[name="' + name + '-op"]').length) {
							if (qs)
								qs += '&';
							qs += name + '=' + encodeURIComponent(this.value);
						}
					});
			if (qs)
				url += (url.indexOf('?') > 0 ? '&' : '?') + qs;
		}
		return url;
	},
	getEntityName : function(form) {
		var entity = form.data('entity');
		if (!entity) {
			var entity = $('table.richtable th[data-cellname]', form)
					.data('cellname');
			if (entity) {
				entity = entity.substring(0, entity.indexOf('.'));
			} else {
				entity = form.prop('action');
				entity = entity.substring(entity.lastIndexOf('/') + 1);
			}
		}
		return entity;
	},
	open : function(url, reloadonclose, useiframe, form, btn) {
		form = form || $('form.richtable');
		var reloadable = form.closest('.reload-container').find('.reloadable');
		reloadonclose = reloadonclose || false;
		useiframe = useiframe || false;
		var winindex = $(document).data('winindex') || 0;
		winindex++;
		$(document).data('winindex', winindex);
		var winid = '_window_' + winindex;
		var win = $('<div id="' + winid + '" class="window-richtable"></div>')
				.appendTo(document.body).dialog();
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
				var inputforms = $('#' + winid + ' form.ajax');
				inputforms.each(function() {
					var inputform = $(this);
					if (!$(	'input[type="hidden"][name="'
									+ Richtable.getEntityName(form) + '.id"]',
							inputform).length)
						$(':input:visible', inputform).filter(function(i) {
							return $(this).attr('name')
									&& !($(this).val()
											|| $(this).hasClass('date')
											|| $(this).hasClass('datetime')
											|| $(this).hasClass('time') || $(this)
											.prop('tagName') == 'BUTTON');
						}).eq(0).focus();
					if (!inputform.hasClass('keepopen')
							&& !inputform.hasClass('richtable')) {
						$(':input[name]', inputform).change(function(e) {
									if (!inputform.hasClass('nodirty'))
										inputform.addClass('dirty');
								});
						if (!$(inputform).hasClass('forcereload'))
							$(inputform).addClass('dontreload');
						var create = url.lastIndexOf('input') == url.length - 5;
						if (create) {
							if ($('input[type="hidden"][name="id"]', inputform)
									.val())
								create = false;
							if ($(
									'input[type="hidden"][name="'
											+ Richtable.getEntityName(form)
											+ '.id"]', inputform).val())
								create = false;
						}
						if (create && inputform.hasClass('sequential_create')) {
							$('[type="submit"]', inputform)
									.addClass('btn-primary')
									.after(' <button type="submit" class="btn sequential_create">'
											+ MessageBundle
													.get('save.and.create')
											+ '</button>');
							$('.sequential_create', inputform).click(
									function() {
										$('form.ajax').addClass('reset');
									});
						}
					}
					var action = inputform.prop('action');
					if (action.indexOf('http') != 0 && action.indexOf('/') != 0) {
						action = pathname
								+ (pathname.indexOf('/') == (pathname.length - 1)
										? ''
										: '/') + action;
						inputform.prop('action', action);
					}
					if (inputform.hasClass('view')
							&& !(inputform.data('replacement')))
						inputform.data('replacement', winid + ':content');
					if (!inputform.hasClass('view')
							&& !inputform.hasClass('keepopen')) {
						$('[type="submit"]', inputform).click(function(e) {
							$(e.target).closest('form')[0].onsuccess = function() {
								inputform.removeClass('dirty');
								inputform.removeClass('dontreload');
								if (!$(e.target).closest('button')
										.hasClass('sequential_create')) {
									// setTimeout(function()
									// {
									$('#' + winid).dialog('close');
									// }, 1000);
								}
								setTimeout(function() {
											reloadable.trigger('reload');
										}, 500);
							};
						});
					}
				});
				$('#' + winid + ' a').each(function() {
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
						replacement : winid + ':content',
						quiet : true,
						beforeSend : function() {
							btn.prop('disabled', true).addClass('loading');
						},
						complete : function() {
							if (!useiframe)
								win.find('.loading-indicator').remove();
							btn.prop('disabled', false).removeClass('loading');
						}
					});
		} else {
			// embed iframe
			btn.prop('disabled', true).addClass('loading');
			win.html('<iframe style="width:100%;height:550px;border:0;"/>');
			url += (url.indexOf('?') > 0 ? '&' : '?') + 'decorator=simple&'
					+ Math.random();
			var iframe = $('#' + winid + ' > iframe')[0];
			iframe.src = url;
			iframe.onload = function() {
				btn.prop('disabled', false).removeClass('loading');
				Dialog.adapt(win, iframe);
			}
		}
		if (!useiframe)
			if (win.html() && typeof $.fn.mask != 'undefined')
				win.mask();
			else
				win
						.html('<div class="loading-indicator" style="text-align:center;">'
								+ MessageBundle.get('ajax.loading') + '</div>');
		var opt = {
			minHeight : 600,
			width : 700,
			// modal : true,
			closeOnEscape : false,
			close : function() {
				if (btn && btn.hasClass('loading'))
					btn.prop('disabled', false).removeClass('loading');
				if (reloadonclose
						&& ($('#' + winid + ' form.richtable').length
								|| $('#' + winid + ' form.ajax')
										.hasClass('forcereload') || !$('#'
								+ winid + ' form.ajax').hasClass('dontreload'))) {
					$('.action .reload', form).addClass('clicked');
					form.submit();
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
		win.data('windowoptions', opt);
		win.dialog(opt);
		win.dialog('open');
		win.closest('.ui-dialog').css('z-index', 2000);
		$('.ui-dialog-titlebar-close', win.closest('.ui-dialog')).blur();
		return winid;
	},
	click : function(event) {
		var btn = $(event.target).closest('button,a');
		var form = btn.closest('form');
		var reloadable = form.closest('.reload-container').find('.reloadable');
		if (btn.attr('onclick') || btn.hasClass('raw'))
			return;
		var idparams;
		var tr = btn.closest('tr');
		var id = tr.data('rowid')
				|| $('input[type="checkbox"],input[type="radio"]',
						$('td:eq(0)', tr)).val();
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
		var action = btn.data('action');
		if (action)
			btn.addClass('clicked');
		var view = btn.data('view');
		if (action == 'save')
			Richtable.save(event);
		else if (action) {
			if (!idparams) {
				Message.showMessage('no.selection');
				return false;
			}
			var url = Richtable.getBaseUrl(form) + '/' + action
					+ Richtable.getPathParams();
			url += (url.indexOf('?') > 0 ? '&' : '?') + idparams;
			var download = btn.attr('download');
			if (download || btn.hasClass('download')) {
				var link = document.createElement('a');
				link.href = url;
				if (download)
					link.download = download;
				document.body.appendChild(link);
				link.click();
				document.body.removeChild(link);
				return;
			}
			var func = function() {
				ajax({
							url : url,
							type : 'POST',
							dataType : 'json',
							beforeSend : function() {
								btn.prop('disabled', true).addClass('loading');
								form.addClass('loading');
							},
							success : function() {
								form.submit();
								setTimeout(function() {
											reloadable.trigger('reload');
										}, 500);
							},
							complete : function() {
								btn.prop('disabled', false)
										.removeClass('loading');
								form.removeClass('loading');
							}
						});

			}
			if ((btn.hasClass('confirm') || action == 'delete')
					&& VERBOSE_MODE != 'LOW') {
				$.alerts({
							type : 'confirm',
							message : (btn.data('confirm') || (action == 'delete'
									? MessageBundle.get('confirm.delete')
									: MessageBundle.get('confirm.action'))),
							callback : function(b) {
								if (b) {
									func();
								}
							}
						});
			} else {
				func();
			}
		} else {
			var options = (new Function("return "
					+ (btn.data('windowoptions') || '{}')))();
			var url = btn.prop('href');
			if (view) {
				url = Richtable.getUrl(view, id, !id || options.includeParams,
						form);
			} else {
				if (!btn.hasClass('noid')) {
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
					? (view != 'view' && !btn.hasClass('view'))
					: options.reloadonclose;
			var winid = Richtable.open(url, reloadonclose, options.iframe,
					form, btn);
			delete options.iframe;
			delete options.reloadonclose;
			for (var key in options)
				$('#' + winid).dialog('option', key, options[key]);
			$('#' + winid).data('windowoptions', options);
			Dialog.adapt($('#' + winid));
			return false;
		}
	},
	save : function(event) {
		var btn = $(event.target).closest('button,a');
		var form = $(event.target).closest('form');
		var reloadable = form.closest('.reload-container').find('.reloadable');
		var func = function() {
			var versionproperty = form.data('versionproperty');
			var modified = false;
			var theadCells = $('.richtable thead:eq(0) th');
			$('.richtable tbody tr', form).each(function() {
				var row = this;
				if ($('td.edited', row).length) {
					modified = true;
					var entity = Richtable.getEntityName(form);
					var params = {};
					var version = $(row).data('version');
					var versionParamName = entity + '.'
							+ (versionproperty || 'version');
					if (version)
						params[versionParamName] = version;
					params[entity + '.id'] = $(this).data('rowid')
							|| $('input[type="checkbox"]:eq(0)', this).val();
					$.each(row.cells, function(i) {
						var theadCell = $(theadCells[i]);
						var name = theadCell.data('cellname');
						if (!name || !$(this).hasClass('edited'))
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
								beforeSend : function() {
									btn.prop('disabled', true)
											.addClass('loading');
									form.addClass('loading');
								},
								onsuccess : function(data, xhr) {
									$('td', row).removeClass('edited')
											.removeData('oldvalue');
									var postback = xhr
											.getResponseHeader('X-Postback');
									if (postback)
										$.each(postback.split(', '), function(
														i, v) {
													var pair = v.split('=', 2);
													if (pair[0] == versionParamName)
														$(row).attr(
																'data-version',
																pair[1]).data(
																'version',
																pair[1]);
												});
									$('[data-action="save"]', form)
											.removeClass('btn-primary').hide();
									setTimeout(function() {
												reloadable.trigger('reload');
											}, 500);
								},
								complete : function() {
									btn.prop('disabled', false)
											.removeClass('loading');
									form.removeClass('loading');
								}
							});
				}
			});
			if (!modified) {
				Message.showMessage('no.modification');
				return false;
			}
		}

		if (btn.hasClass('confirm') && VERBOSE_MODE != 'LOW') {
			$.alerts({
						type : 'confirm',
						message : btn.data('confirm')
								|| MessageBundle.get('confirm.save'),
						callback : function(b) {
							if (b) {
								func();
							}
						}
					});
		} else {
			func();
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
			var ele = $('#' + templateId);
			template = $.trim(ele.is('template') ? ele.html() : ele.text());
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
			input.addClass(type).datetimepicker(option).on('hide', function(e) {
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
		cell.attr('data-cellvalue', value);
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
							var ar = cellEdit.split(/\s*,\s*/);
							var action = ar[0];
							if (action == 'none')
								return;
							var type = ar[1];
							var template = ar[2];
							if (action != 'click' && action != 'dblclick') {
								template = type;
								type = action;
								action = 'click';
							}
							if (!$(cells[i]).data('readonly'))
								$(cells[i]).off(action).on(action, function() {
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
					function(event) {
						var btn = $(event.target).closest('button,a')
								.addClass('clicked');
						var form = btn.closest('form');
						var reloadable = form.closest('.reload-container')
								.find('.reloadable');
						btn.prop('disabled', true).addClass('loading');
						form.submit();
						setTimeout(function() {
									reloadable.trigger('reload');
								}, 500);
					}).on('click', '.richtable .action .filter', function() {
						var f = $(this).closest('form').next('form.criteria');
						var qf = $(this).closest('form').prev('form.query');
						if (f.is(':visible')) {
							f.hide();
							qf.slideDown(100, function() {
										$('html,body').animate({
													scrollTop : qf.offset().top
															- 50
												}, 100);
									});
						} else {
							f.slideDown(100, function() {
										$('html,body').animate({
													scrollTop : f.offset().top
															- 50
												}, 100);
									});
							qf.hide();
						}
					}).on('click', '.richtable .more', function(event) {
				var form = $(event.target).closest('form');
				if (!$('li.nextPage', form).length)
					return;
				$('.inputPage', form).val(function(i, v) {
							return parseInt(v) + 1
						});
				$.ajax({
					url : form.prop('action'),
					type : form.attr('method'),
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
			}).on('click', 'td.action .dropdown-menu li a', function() {
						$('.dropdown-toggle', $(this).closest('.btn-group'))
								.click();
					}).on('keydown', '.window-richtable form input',
					function(evt) {
						if (evt.keyCode == 13) {
							var current, next;
							var inputs = $('input:visible', $(this)
											.closest('form'));
							for (var i = 0; i < inputs.length; i++) {
								var input = $(inputs[i]);
								if (current && input.is(':not([readonly])')
										&& input.is(':not([disabled])')) {
									next = input;
									break;
								}
								if (this == inputs[i])
									current = input;
							}
							if (next) {
								evt.preventDefault();
								next.focus();
							}
						}
					}).on('click', 'strong.force-override', function(e) {
				var t = $(e.target);
				var popup = t.closest('#popup-container');
				if (popup.length) {
					var form = popup.data('target');
					if (form) {
						form = $(form);
						$('input[type="hidden"].version', form).val('');
						form.submit();
						popup.find('.popup-ok').click();
					} else {
						var button = $('button[data-action="save"]:visible');
						$('tr', button.closest('form')).filter(function() {
									return $(this).find('td.edited').length;
								}).removeData('version')
								.removeAttr('data-version');
						button.click();
					}
				} else {
					var msgcontainer = t.closest('.message-container');
					if (msgcontainer.length) {
						var form = msgcontainer.next('form');
						$('input[type="hidden"].version', form).val('');
						msgcontainer.fadeOut().remove();
						form.submit();
					} else {
						var button = $('button[data-action="save"]:visible');
						$('tr', button.closest('form')).filter(function() {
									return $(this).find('td.edited').length;
								}).removeData('version')
								.removeAttr('data-version');
						button.click();
					}
				}
			});
}
Observation._richtable = function(container) {
	$('form.query', container).each(function() {
		var t = $(this);
		var form = t.next('form.richtable');
		if (form.length) {
			t.prop('action', form.prop('action')).attr('data-replacement',
					form.attr('id'));
			$('input[type="reset"]', t).click(function(e) {
						$('.remove', t).click();
						setTimeout(function() {
									t.submit();
								}, 100);
					});
		}
	});
	$('form.criteria', container).each(function() {
		var t = $(this);
		var form = t.prev('form.richtable');
		var entity = Richtable.getEntityName(form);
		t.prop('action', form.prop('action')).attr('data-replacement',
				form.attr('id'));
		var qs = t.prop('action');
		var index = qs.indexOf('?');
		qs = index > -1 ? qs.substring(index + 1) : '';
		if (qs) {
			var arr = qs.split('&');
			for (var i = 0; i < arr.length; i++) {
				var arr2 = arr[i].split('=', 2);
				var name = arr2[0];
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
					$('table.ordering select.property option', t).each(
							function() {
								var v = $(this).attr('value');
								if (v == name || v.indexOf(name + '.') == 0
										|| name.indexOf(v + '.') == 0)
									$(this).remove();
							});
				}

			}
		}
		$('table.criteria select.property', t).change(function() {
			var t = $(this);
			var operator = $('select.operator', t.closest('tr'));
			var allops = operator.data('operators');
			if (!allops) {
				allops = {};
				$('option', operator).each(function() {
							allops[$(this).attr('value')] = {
								parameters : $(this).data('parameters'),
								label : $(this).text()
							};
						});
				operator.data('operators', allops);
			}
			operator.find('option').remove();
			if (t.val()) {
				operator.attr('name', t.val() + '-op');
				$('td:eq(2) :input', t.closest('tr')).attr('name', t.val());
				var ops = $('option:selected', t).data('operators');
				if (ops.indexOf('[') == 0)
					ops = ops.substring(1, ops.length - 1);
				ops = ops.split(/,\s*/);
				$.each(allops, function(k, v) {
							if (jQuery.inArray(k, ops) >= 0)
								operator.append('<option value="' + k
										+ '" data-parameters="' + v.parameters
										+ '">' + v.label + '</option>');
						});
				operator.change();
			} else {
				operator.removeAttr('name');
				$('td:eq(2) :input', t.closest('tr')).removeAttr('name');
				$.each(allops, function(k, v) {
							operator.append('<option value="' + k
									+ '" data-parameters="' + v.parameters
									+ '">' + v.label + '</option>');
						});
			}
		});
		$('table.criteria select.operator', t).change(function() {
			var t = $(this);
			var property = $('select.property', t.closest('tr'));
			var option = $('option:selected', property);
			var size = parseInt($('option:selected', t).data('parameters'));
			var td = $('td:eq(2)', t.closest('tr'));
			$(':input,.input-pseudo,.removeonadd,label', td).remove();
			if (size > 0) {
				if ('select' == option.data('type')) {
					var select = $('<select name="' + property.val()
							+ '" class="removeonadd ' + option.data('class')
							+ '"></select>').appendTo(td);
					if (!select.hasClass('required'))
						$('<option value=""></option>').appendTo(select);
					var map = option.data('map');
					if (map.indexOf('{') == 0)
						map = map.substring(1, map.length - 1);
					map = map.split(/,\s*/);
					for (var i = 0; i < map.length; i++) {
						var arr = map[i].split('=', 2);
						$('<option value="' + arr[0] + '">'
								+ (arr[1] || arr[0]) + '</option>')
								.appendTo(select);
					}
				} else if ('treeselect' == option.data('type')) {
					$('<input name="' + property.val()
							+ '" class="treeselect-inline required" data-url="'
							+ option.data('pickurl') + '"/>').appendTo(td);
				} else if ('listpick' == option.data('type')
						|| 'treeselect' == option.data('type')) {
					var type = option.data('type');
					$('<div class="' + type
							+ ' removeonadd" data-options="{\'url\':\''
							+ option.data('pickurl') + '\'}"><input class="'
							+ type + '-id required" type="hidden" name="'
							+ property.val()
							+ '" class="required"/><div class="' + type
							+ '-name input-pseudo"></div></div>').appendTo(td);
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
				if (map.indexOf('{') == 0)
					map = map.substring(1, map.length - 1);
				map = map.split(/,\s*/);
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
							+ '">' + (arr[1] || arr[0])
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
		$('button.restore', t).click(function(e) {
			var t = $(e.target).closest('form');
			var form = t.prev('form.richtable');
			form.prev('form.query').slideDown(100, function() {
						$('html,body').animate({
									scrollTop : form.offset().top - 50
								}, 100);
					});
			var b;
			$(':input[name]', t).each(function() {
				var h = form.find('input[type="hidden"][name="' + this.name
						+ '"]');
				if (h.length) {
					b = true;
					h.remove();
				}
			});
			if (b) {
				$('.inputPage', form).val(1);
				form.submit();
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
	$('table.richtable', container).each(function() {
				Richtable.enhance(this);
			});
	var uploadBtn = $('.action .upload', container);
	if (uploadBtn.length) {
		var t = uploadBtn;
		var f = t.closest('form');
		var url = t.data('url');
		var maxsize = t.data('maxsize');
		var multiple = t.data('multiple');
		var maximum = t.data('maximum') || 10;
		maxsize = maxsize ? parseInt(maxsize) : 15 * 1024 * 1024;
		if (!url) {
			var action = f.prop('action');
			var abu = f.data('actionbaseurl');
			var i = action.indexOf('?');
			if (abu) {
				url = abu + '/upload';
				if (i > 0)
					url += action.substring(i);
			} else {
				url = i > 0 ? (action.substring(0, i) + '/upload' + action
						.substring(i)) : (action + '/upload');
			}
		}
		var upload = function(files) {
			if (!files || !files.length)
				return;
			if (files.length > maximum) {
				Message.showActionError(MessageBundle.get('maximum.exceeded',
						files.length, maximum));
				return;
			}
			var size = 0;
			$.each(files, function(i, v) {
						size += v.size;
					});
			if (size > maxsize) {
				Message.showActionError(MessageBundle.get('maxsize.exceeded',
						size, maxsize));
				return;
			}
			$.ajaxupload(files, ajaxOptions({
								url : url,
								success : function() {
									f.submit();
									setTimeout(function() {
												f.closest('.reload-container')
														.find('.reloadable')
														.trigger('reload');
											}, 500);
								}
							}));
		}
		t.on('click', function() {
					var file = t.next('input[type="file"]:hidden');
					if (!file.length) {
						file = $('<input type="file"/>').prop('multiple',
								multiple).insertAfter(t).attr('accept',
								t.data('accept')).hide().change(function() {
									upload(this.files);
									$(this).remove();
								});
					}
					file.click();
				});
		f.on('dragover', function(e) {
					$(this).addClass('drophover');
					return false;
				}).on('dragleave', function(e) {
					$(this).removeClass('drophover');
					return false;
				}).on('drop', function(e) {
					e.preventDefault();
					$(this).removeClass('drophover');
					upload(e.originalEvent.dataTransfer.files);
					return true;
				});
	}
};
Observation.richtable = function(container) {
	var f = $('form.query[data-replacement]', container);
	if (f.find('[class^="row"]').length > 1)
		f.addClass('folded');
	var tabs = f.find('.tab-pane');
	if (!tabs.length)
		tabs = f;
	tabs.each(function() {
		var tab = $(this);
		if (tab.find('[class^="row"]').length > 1) {
			$('<div class="fold"><span><i class="glyphicon glyphicon-chevron-up"></i></span></div>')
					.insertAfter(tab.find('[class^="row"]:last')).find('span')
					.click(function(e) {
						$(e.target).closest('span').find('i')
								.toggleClass('glyphicon-chevron-up')
								.toggleClass('glyphicon-chevron-down');
						tab.find('[class^="row"]:gt(0)').toggle();
					}).click();
		}
	});

};