(function($) {
	$(function() {
		$(document).on('reset', 'form', function(e) {
					var t = $(e.target);
					t.find('.resettable').html('');
					setTimeout(function() {
								t.find(':input').filter(function() {
											return $(this).val()
										}).change();
							}, 200);
					$('.action-error .remove').click();
					$('.field-error .remove', t).click();
					var nav = t.find('.nav-tabs');
					if (nav.length)
						nav.find('li:first a').click();
				}).on('click', 'form.ajax :submit', function() {
					$(this).addClass('clicked');
				}).on('click', 'label[for]', function(event) {
			var input = $(document.getElementById($(this).attr('for')));
			if (input.prop('readonly') || input.prop('disabled')
					|| input.hasClass('readonly') || input.hasClass('disabled'))
				event.preventDefault();
			else if (input.is('.input-pseudo'))
				input.focus();
		}).on('focus', 'input', function() {
					// move cursor to the end
					try {
						this.selectionStart = this.selectionEnd = this.value.length;
					} catch (e) {
					}
				}).on('paste',
				'input:not([type="password"]):not([type="number"])',
				function() {
					var t = $(this);
					setTimeout(function() {
								t.val($.trim(t.val()));
							}, 0);
				}).on('validate', ':input', function(ev) {
					Form.validate(this, 'validate');
				}).on('keyup', 'input,textarea', $.debounce(200, function(ev) {
							if ($(this).val()) {
								if (ev.keyCode != 13)
									Form.validate(this, 'keyup');
							} else {
								Form.clearError($(this));
							}
							return true;
						})).on('change', 'input,textarea', function(ev) {
			if (this.type == 'hidden' || !this.defaultValue
					|| this.value != this.defaultValue)
				Form.validate(this, 'change');
			return true;
		}).on('change', 'select', function() {
					Form.validate(this, 'change');
					return true;
				}).on('change', 'select', function(e) {
					var t = $(this);
					var option = t.find('option:eq(0)');
					if (!option.attr('value') && option.text()) {
						if (!t.val())
							t.addClass('empty');
						else
							t.removeClass('empty');
					}
				});
	});
})(jQuery);

Observation.form = function(container) {
	$$('td.action,.toolbar div.action,.form-actions', container).each(
			function() {
				var children = $(this).contents();
				for (var i = 0; i < children.length - 1; i++) {
					var current = children.eq(i);
					if (current.hasClass('btn')
							&& children.get(i + 1).nodeType != 3)
						current.after(' ');
				}
			});
	$$('input.double', container).each(function() {
				var t = $(this);
				var value = t.val();
				if (value && value.indexOf('E-') > 0) {
					value = parseFloat(value);
					t.val(value);
				}
			});
	$$('select', container).each(function() {
				var t = $(this);
				var option = t.find('option:eq(0)');
				if (!option.attr('value') && option.text() && !t.val())
					t.addClass('empty');
			});
	$$('.controls .field-error', container).each(function() {
				var text = $(this).text();
				var field = $(':input', $(this).parent());
				$(this).remove();
				Message.showFieldError(field, text);
			});
	var ele = ($(container).is('form') && $(container).hasClass('focus'))
			? container
			: $('.focus:eq(0)', container);
	if (!ele.is('form') && ele.attr('name')) {
		ele.focus();
	} else {
		var arr = $(':input:visible', ele).toArray();
		for (var i = 0; i < arr.length; i++) {
			var e = $(arr[i]);
			if (e.attr('name') && !e.val()) {
				e.focus();
				break;
			}
		}
	}
	$$('form', container).each(function() {
				if ($('input[name][type="file"]', this).length)
					$(this).attr('enctype', 'multipart/form-data');
				if (!$(this).hasClass('ajax'))
					$(this).submit(function() {
								$('.action-error').remove();
								return Form.validate(this)
							});
			});
	$$('input[type="text"]', container).each(function() {
				var t = $(this);
				if (!t.attr('autocomplete'))
					t.attr('autocomplete', 'off');
				if (!t.hasClass('unlimited')) {
					var maxlength = t.attr('maxlength');
					if (!maxlength || maxlength > 4000) {
						if (t.hasClass('date'))
							t.attr('maxlength', '10');
						else if (t.hasClass('datetime'))
							t.attr('maxlength', '19');
						else if (t.hasClass('time'))
							t.attr('maxlength', '8');
						else if (t.hasClass('yearmonth'))
							t.attr('maxlength', '7');
						else if (t.hasClass('integer'))
							t.attr('maxlength', t.hasClass('positive')
											? '9'
											: '10');
						else if (t.hasClass('long'))
							t.attr('maxlength', t.hasClass('positive')
											? '18'
											: '19');
						else if (t.hasClass('double'))
							t.attr('maxlength', t.hasClass('positive')
											? '21'
											: '22');
						else
							t.attr('maxlength', '255');
					}
				}
			});
	$$('.custom[type="file"]', container).each(function() {
		var t = $(this);
		t.change(function(e) {
					var t = $(this);
					var names = [];
					for (var i = 0; i < this.files.length; i++) {
						var f = this.files[i];
						var size = f.size;
						size = size / 1024;
						size = Math.round(size * 100) / 100;
						if (size >= 1024) {
							size = size / 1024;
							size = Math.round(size * 100) / 100;
							if (size >= 1024) {
								size = size / 1024;
								size = Math.round(size * 100) / 100;
								size = size + ' GB';
							} else {
								size = size + ' MB';
							}
						} else {
							size = size + ' KB';
						}
						names.push('<span class="tiped" title="' + size + '">'
								+ f.name + '</span>');
					}
					t.closest('.filepick').trigger(
							'val',
							[!this.multiple && names.length ? names[0] : names,
									true]);
				});
		var width = t.outerWidth();
		var fp = t
				.wrap('<div class="filepick input-pseudo" tabindex="0"/>')
				.after('<div class="text resettable"></div>'
						+ '<i class="indicator glyphicon glyphicon-folder-open"/>'
						+ '<i class="remove glyphicon glyphicon-remove-sign"/>')
				.parent();
		if (t.prop('disabled'))
			fp.addClass('disabled').removeAttr('tabindex');
		if (t.prop('readonly'))
			fp.addClass('readonly').removeAttr('tabindex');
		var hasWidthClass = false;
		$.each(	$.grep(t.attr('class').split(' '), function(v) {
							var b = v.indexOf('input-') == 0
									|| v.indexOf('span') == 0;
							if (b)
								hasWidthClass = true;
							return b;
						}), function(k, v) {
					t.removeClass(v);
					fp.addClass(v);
				});
		if (width > 0 && !hasWidthClass)
			fp.outerWidth(width);
		fp.click(function(e) {
			var t = $(e.target);
			if (t.is('.remove') || t.is('.tag-remove')
					|| t.is('input[type="file"]'))
				return true;
			var fp = t.closest('.filepick');
			if (fp.is('.disabled,.readonly'))
				return false;
			fp.find('input[type="file"]').click();
			return false;
		}).keydown(function(e) {
					if (e.keyCode == 13) {
						$(this).click();
						return false;
					}
				});
	});
	$$('.linkage_switch', container).each(function() {
		var c = $(this).closest('.linkage');
		c.data('originalclass', c.attr('class'));
		$(this).on('linkage', function() {
			var c = $(this).closest('.linkage');
			var sw = $(this);
			if (sw.is(':hidden'))
				return false;
			var val = sw.val() || 'linkage_default';
			$('.linkage_component', c).each(function() {
				var t = $(this);
				var hide = t.is(':not(.' + val + ')');
				if (!hide
						&& t.closest('.linkage')[0] != sw.closest('.linkage')[0])
					hide = t.parent().closest('.linkage_component').is(':not(.'
							+ val + ')');
				if (hide) {
					t.hide();
					$$(':input:not([disabled])', t).addClass('_disabled').prop(
							'disabled', true);
				} else {
					t.show();
					$$('._disabled:input', t).removeClass('_disabled').prop(
							'disabled', false).filter('.linkage_switch').each(
							function() {
								var t = $(this);
								setTimeout(function() {
											t.change()
										}, 20);
							});
				}
			});
			c.attr('class', c.data('originalclass') + ' ' + val);
		}).change(function() {
					$(this).trigger('linkage')
				}).trigger('linkage');
	});
	$$(':input.conjunct', container).on('conjunct', function() {
		var t = $(this);
		var f = $(this).closest('form');
		var data = {};
		var url = f.prop('action');
		if (url.indexOf('/') > -1) {
			if (url.substring(url.lastIndexOf('/') + 1) == 'save')
				url = url.substring(0, url.lastIndexOf('/')) + '/input';
		} else if (url == 'save')
			url = 'input';
		var hid = $('input[type=hidden][name$=".id"],:input.id:not(:disabled)',
				f);
		if (hid.val())
			data['id'] = hid.val();
		$(
				':input.conjunct,:input.conjunct-addition,input[type=hidden]:not(.nocheck)',
				f).each(function() {
					var t = $(this);
					if (!t.is('[type="checkbox"]') || t.is(':checked'))
						data[t.attr('name')] = t.val();
				});
		ajax({
					global : t.data('global'),
					preflight : true,
					quiet : true,
					async : false,
					type : t.data('method') || 'GET',
					url : url,
					data : data,
					target : f[0],
					replacement : t.data('replacement'),
					headers : {
						'X-Exact-Fragment' : '1'
					},
					beforeSend : function() {
						f.addClass('loading');
					},
					complete : function() {
						f.removeClass('loading');
						var arr = $('input:visible', f).get();
						var after = false;
						for (var i = 0; i < arr.length; i++) {
							if (arr[i] == t[0] && i < arr.length - 1) {
								after = true;
								continue;
							}
							if (after && !$(arr[i]).val()) {
								$(arr[i]).focus();
								break;
							}
						}
					}
				});
	}).change(function() {
				var t = $(this).trigger('conjunct');
			});
	// if (typeof $.fn.datepicker != 'undefined')
	// $$('input.date:not([readonly]):not([disabled])', container).datepicker({
	// dateFormat : 'yy-mm-dd'
	// });
	if (typeof $.fn.bootstrapSwitch != 'undefined')
		$$('.switch', container).each(function() {
			var t = $(this);
			if (t.is('input[type="checkbox"]')) {
				var p = t.wrap('<div/>').parent();
				$.each(	$.grep(t.attr('class').split(' '), function(v) {
									return v == 'switch'
											|| v.indexOf('switch-') == 0
											|| v.indexOf('input-') == 0
											|| v.indexOf('span') == 0;
								}), function(k, v) {
							t.removeClass(v);
							p.addClass(v);
						});
				t = p;
			}
			t.bootstrapSwitch().closest('.switch').addClass('input-pseudo')
					.attr('tabindex', '0').keydown(function(e) {
								if (e.keyCode == 13) {
									$(this).find('.switch-left').click();
									return false;
								}
							});
		});
	if (typeof $.fn.datetimepicker != 'undefined')
		$$('input.date,input.datetime,input.time,input.yearmonth', container)
				.not('[readonly]').not('[disabled]').each(function() {
					var t = $(this);
					var option = {
						language : MessageBundle.lang().replace('_', '-')
					};
					if (t.hasClass('datetime')) {
						option.format = t.data('format')
								|| 'yyyy-MM-dd HH:mm:ss';
					} else if (t.hasClass('time')) {
						option.format = t.data('format') || 'HH:mm:ss';
						option.pickDate = false;
					} else if (t.hasClass('yearmonth')) {
						option.format = t.data('format') || 'yyyy-MM';
						option.pickTime = false;
					} else {
						option.format = t.data('format') || 'yyyy-MM-dd';
						option.pickTime = false;
					}
					t.focus(function() {
						var _t = $(this);
						var dp = _t.data('datetimepicker');
						if (!dp) {
							_t.datetimepicker(option).on('changeDate',
									function(e) {
										var _t = $(e.target);
										_t.data('changed', true);
										if (_t.hasClass('date'))
											_t.blur();
									});
							dp = _t.data('datetimepicker');
							dp.show();
						}
					}).blur(function() {
						var _t = $(this);
						var dp = _t.data('datetimepicker');
						if (dp) {
							if (_t.data('changed'))
								_t.removeData('changed').trigger('validate')
										.trigger('conjunct');
							dp.widget.remove();
							_t.removeData('datetimepicker');
						}
					});
				});
	if (typeof $.fn.chosen != 'undefined') {
		$$('.chosen', container).each(function() {
			var t = $(this);
			t.chosen({
						search_contains : true,
						placeholder_text : MessageBundle.get('select'),
						no_results_text : ' '
					}).prependTo(t.next('.chosen-container')).parent()
					.addClass('input-pseudo');
		});
	}
	$$(':input[data-helpurl]', container).each(function() {
		var t = $(this);
		var href = '<a href="'
				+ t.data('helpurl')
				+ '" style="padding-left: 5px;" target="_blank"><span class="glyphicon glyphicon-question-sign"></span></a>'
		href = $(href).insertAfter(t);
		if (t.is('textarea'))
			href.find('span').css('vertical-align', 'top');
	});
	$$('.btn-switch', container).each(function() {
				var t = $(this);
				t.children().css('cursor', 'pointer').click(function() {
							t.children().removeClass('active').css({
										'font-weight' : 'normal'
									});
							$(this).addClass('active').css({
										'font-weight' : 'bold'
									});
						});
			});
};

Observation.z_form = function(container) {
	// LOWEST_PRECEDENCE
	if (typeof $.fn.htmlarea != 'undefined')
		$$('textarea.htmlarea', container).htmlarea();
};