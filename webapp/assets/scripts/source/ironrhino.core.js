var MODERN_BROWSER = !$.browser.msie || $.browser.version > 8;
(function() {
	$$ = function(selector, container) {
		if (!container)
			return $(selector);
		container = $(container);
		return container.is(selector) ? container : $(selector, container);
	}
	var d = document.domain;
	if (!d.match(/^(\d+\.){3}\d+$/)) {
		d = d.split('.');
		try {
			if (d.length > 2)
				document.domain = d[d.length - 2] + '.' + d[d.length - 1];
		} catch (e) {
			if (d.length > 3)
				document.domain = d[d.length - 3] + '.' + d[d.length - 2] + '.'
						+ d[d.length - 1];
		}
	}
	$.ajaxSettings.traditional = true;
	var $ajax = $.ajax;
	if (MODERN_BROWSER)
		$.ajax = function(options) {
			options.url = UrlUtils.absolutize(options.url);
			options.xhrFields = {
				withCredentials : true
			};
			return $ajax(options);
		}

	if (typeof $.rc4EncryptStr != 'undefined'
			&& ($('meta[name="pe"]').attr('content') != 'false')) {
		var temp = $.param;
		$.param = function(a, traditional) {
			if (jQuery.isArray(a) || a.jquery) {
				jQuery.each(a, function() {
					if (/password$/.test(this.name.toLowerCase())) {
						try {
							var key = $.cookie('T');
							if (key && key.length > 10)
								key = key
										.substring(key.length - 10, key.length);
							this.value = $.rc4EncryptStr(
									encodeURIComponent(this.value + key), key);
						} catch (e) {
						}
					}
				});

			}
			return temp(a, traditional);
		}
	}

	/* http://ejohn.org/blog/javascript-micro-templating/ */
	var cache = {};
	$.tmpl = function(str, data) {
		// Figure out if we're getting a template, or if we need to
		// load the template - and be sure to cache the result.
		var fn = !/\W/.test(str) ? cache[str] = cache[str]
				|| tmpl(document.getElementById(str).innerHTML) :

				// Generate a reusable function that will serve as a template
				// generator (and which will be cached).
				new Function("obj",
						"var p=[],print=function(){p.push.apply(p,arguments);};"
								+

								// Introduce the data as local variables using
								// with(){}
								"with(obj){p.push('" +

								// Convert the template into pure JavaScript
								str.replace(/[\r\t\n]/g, " ").split("{{")
										.join("\t").replace(/((^|%>)[^\t]*)'/g,
												"$1\r").replace(/\t(.*?)}}/g,
												"',$1,'").split("\t")
										.join("');").split("}}")
										.join("p.push('").split("\r")
										.join("\\'") + "');}return p.join('');");

		// Provide some basic currying to the user
		return data ? fn(data) : fn;
	};

})();

Indicator = {
	text : '',
	show : function(iserror) {
		if (!$('#indicator').length)
			$('<div id="indicator"></div>').appendTo(document.body);
		var ind = $('#indicator');
		if (iserror && ind.hasClass('loading'))
			ind.removeClass('loading');
		if (!iserror && !ind.hasClass('loading'))
			ind.addClass('loading');
		ind.html(Indicator.text || MessageBundle.get('ajax.loading'));
		if (!iserror)
			ind.prepend('<span class="icon-loading small"></span>');
		ind.show();
		Indicator.text = '';
	},
	showError : function() {
		Indicator.text = MessageBundle.get('ajax.error');
		Indicator.show(true);
	},
	hide : function() {
		Indicator.text = '';
		if ($('#indicator'))
			$('#indicator').hide()
	}
};

UrlUtils = {
	extractDomain : function(a) {
		if (UrlUtils.isAbsolute(a)) {
			a = a.replace(/:\d+/, '');
			a = a.substring(a.indexOf('://') + 3);
			var i = a.indexOf('/');
			if (i > 0)
				a = a.substring(0, i);
			return a;
		} else {
			return document.location.hostname;
		}
	},
	isSameDomain : function(a, b) {
		b = b || document.location.href;
		var ad = UrlUtils.extractDomain(a);
		var bd = UrlUtils.extractDomain(b);
		return ad == bd;
	},
	isSameOrigin : function(a, b) {
		b = b || document.location.href;
		var ad = UrlUtils.extractDomain(a);
		var bd = UrlUtils.extractDomain(b);
		if ($.browser.msie && ad != bd)
			return false;
		var arra = ad.split('.');
		var arrb = bd.split('.');
		return (arra[arra.length - 1] == arrb[arrb.length - 1] && arra[arra.length
				- 2] == arrb[arrb.length - 2]);
	},
	makeSameOrigin : function(url, referrer) {
		referrer = referrer || document.location.href;
		if (!UrlUtils.isSameOrigin(url, referrer))
			return referrer.substring(0, referrer.indexOf('/', referrer
									.indexOf('://')
									+ 3))
					+ CONTEXT_PATH + '/webproxy/' + url;
		else
			return url;
	},
	isAbsolute : function(a) {
		if (!a)
			return false;
		if (a.indexOf('//') == 0)
			return true;
		var index = a.indexOf('://');
		return (index == 4 || index == 5);
	},
	absolutize : function(url) {
		if (UrlUtils.isAbsolute(url))
			return url;
		var a = document.location.href;
		var index = a.indexOf('://');
		if (url.length == 0 || url.indexOf('/') == 0) {
			var host = a.substring(0, a.indexOf('/', index + 3));
			if (CONTEXT_PATH && url.indexOf(CONTEXT_PATH + '/') != 0)
				host += CONTEXT_PATH;
			return host + url;
		} else {
			return a.substring(0, a.lastIndexOf('/') + 1) + url;
		}
	}
}

Message = {
	compose : function(message, className) {
		return '<div class="' + className
				+ '"><a class="close" data-dismiss="alert">&times;</a>'
				+ message + '</div>';
	},
	showMessage : function() {
		Message.showActionMessage(MessageBundle.get.apply(this, arguments));
	},
	showError : function() {
		Message.showActionError(MessageBundle.get.apply(this, arguments));
	},
	showActionError : function(messages, target) {
		Message.showActionMessage(messages, target, true);
	},
	showActionMessage : function(messages, target, error) {
		if (!messages)
			return;
		if (typeof messages == 'string') {
			var a = [];
			a.push(messages);
			messages = a;
		}
		// if ($.alerts) {
		// $.alerts.alert(messages.join('\n'), MessageBundle.get('error'));
		// return;
		// }
		var html = '';
		for (var i = 0; i < messages.length; i++)
			html += Message.compose(messages[i], error
							? 'action-error alert alert-error'
							: 'action-message alert alert-info');
		if (html) {
			var parent = $('#content');
			if ($('.ui-dialog:visible').length)
				parent = $('.ui-dialog:visible .ui-dialog-content').last();
			if ($('.modal:visible').length)
				parent = $('.modal:visible .modal-body').last();
			if (!$('#message', parent).length)
				$('<div id="message"></div>').prependTo(parent);
			var msg = $('#message', parent);
			if (error && target && $(target).prop('tagName') == 'FORM') {
				if (!$(target).attr('id'))
					$(target).attr('id', 'form' + new Date().getTime());
				var fid = $(target).attr('id');
				if ($('#' + fid + '_message').length == 0)
					$('<div id="' + fid
							+ '_message" class="message-container"></div>')
							.insertBefore(target);
				msg = $('#' + fid + '_message');
			}
			msg.html(html);
			_observe(msg);
			$('html,body').animate({
						scrollTop : msg.offset().top - 50
					}, 100);
		}
	},
	showFieldError : function(field, msg, msgKey) {
		var msg = msg || MessageBundle.get(msgKey);
		if (field && $(field).length) {
			field = $(field);
			var tabpane = field.closest('.control-group').parent('.tab-pane');
			if (tabpane.length && !tabpane.hasClass('active'))
				$('a[href$="#' + tabpane.attr('id') + '"]').tab('show');
			var cgroup = field.closest('.control-group');
			cgroup.addClass('error');
			$('.field-error', field.parent()).remove();
			if (field.hasClass('sqleditor'))
				field = field.next('.preview');
			else if (field.hasClass('chzn-done'))
				field = field.next('.chzn-container');
			if (field.is(':visible')) {
				field.parent().css('position', 'relative');
				var prompt = $('<div class="field-error field-error-popover"><div class="field-error-content">'
						+ msg
						+ '<a class="remove pull-right" href="#">&times;</a></div><div>')
						.insertAfter(field);
				var promptTopPosition, promptleftPosition;
				var fieldWidth = field.width();
				var promptHeight = prompt.height();
				promptTopPosition = field.position().top + field.outerHeight()
						+ 6;
				var parentWidth = field.closest('.controls').width();
				if (parentWidth && (parentWidth - fieldWidth) < prompt.width()) {
					promptleftPosition = field.position().left + fieldWidth
							- (prompt.width() + 10);
				} else {
					promptleftPosition = field.position().left + fieldWidth
							- 30;
				}
				prompt.css({
							"top" : promptTopPosition + "px",
							"left" : promptleftPosition + "px",
							"opacity" : 0
						});
				prompt.animate({
							"opacity" : 0.8
						});
			} else if (field.is('[type="hidden"]')) {
				var fp = field.parent('.listpick,.treeselect');
				if (fp.length && !fp.is('.control-group')) {
					cgroup.removeClass('error');
					$('<span class="field-error">' + msg + '</span>')
							.appendTo(fp);
				} else if (cgroup.length) {
					// $('.controls span', cgroup).text('');
					$('<span class="field-error">' + msg + '</span>')
							.appendTo($('.controls', cgroup));
				} else {
					if (field.next('.listpick,.treeselect').length) {
						$('<span class="field-error">' + msg + '</span>')
								.insertAfter(field.next());
					} else {
						Message.showActionError(msg);
					}
				}
			} else
				Message.showActionError(msg);
		} else
			Message.showActionError(msg);
	}
};

Form = {
	focus : function(form) {
		var arr = $(':input:visible', form).get();
		for (var i = 0; i < arr.length; i++) {
			if ($('.field-error', $(arr[i]).parent()).length) {
				setTimeout(function() {
							$(arr[i]).focus();
						}, 50);
				break;
			}
		}
	},
	clearError : function(target) {
		if ($(target).prop('tagName') == 'FORM') {
			$('.control-group.error', target).removeClass('error');
			$('.field-error', target).fadeIn().remove();
		} else if ($(target).prop('tagName') == 'DIV') {
			$(target).removeClass('error');
			$('.field-error', target).fadeIn().remove();
		} else {
			$(target).closest('.control-group').removeClass('error');
			$('.field-error', $(target).parent()).fadeIn().remove();
		}
	},
	validate : function(target, evt) {
		if ($(target).prop('tagName') != 'FORM') {
			Form.clearError(target);
			if ($(target).is('input[type="radio"]')) {
				if ($(target).hasClass('required')) {
					var options = $('input[type="radio"][name="' + target.name
									+ '"]', target.form);
					var checked = false;
					$.each(options, function(i, v) {
								if (v.checked)
									checked = true;
							});
					if (!checked) {
						Message.showFieldError(target, null, 'required');
						return false;
					}
				}
			}
			var valid = true;
			var tabpane = $(target).closest('.control-group')
					.parent('.tab-pane');
			var inhiddenpanel = tabpane.length
					&& $(target).css('display') != 'none'
					&& !tabpane.hasClass('active');
			if (inhiddenpanel
					&& $('.control-group.error', tabpane
									.siblings('.tab-pane.active')).length)
				return;
			if ((inhiddenpanel || $(target)
					.is(':visible,[type="hidden"],.sqleditor,.chzn-done'))
					&& !$(target).prop('disabled')) {
				var value = $(target).val();
				if ($(target).hasClass('required') && $(target).attr('name')
						&& !value) {
					if ($(target).prop('tagName') == 'SELECT'
							|| $(target).is('[type="hidden"]'))
						Message.showFieldError(target, null,
								'selection.required');
					else
						Message.showFieldError(target, null, 'required');
					if (inhiddenpanel)
						$('a[href="#'
								+ $(target).closest('.tab-pane').attr('id')
								+ '"]').tab('show');
					valid = false;
				} else if (evt != 'keyup'
						&& $(target).hasClass('email')
						&& value
						&& !value
								.match(/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/)) {
					Message.showFieldError(target, null, 'email');
					valid = false;
				} else if (evt != 'keyup' && $(target).hasClass('regex')
						&& value
						&& !value.match(new RegExp($(target).data('regex')))) {
					Message.showFieldError(target, null, 'regex');
					valid = false;
				} else if (evt != 'keyup' && $(target).hasClass('phone')
						&& value && !value.match(/^[\d-]+$/)) {
					Message.showFieldError(target, null, 'phone');
					valid = false;
				} else if (($(target).hasClass('integer') || $(target)
						.hasClass('long'))
						&& value) {
					if ($(target).hasClass('positive')
							&& (!value.match(/^[+]?\d*$/) || !$(target)
									.hasClass('zero')
									&& parseInt(value) == 0)) {
						Message
								.showFieldError(target, null,
										'integer.positive');
						valid = false;
					}
					if (!$(target).hasClass('positive')
							&& !value.match(/^[-+]?\d*$/)) {
						Message.showFieldError(target, null, 'integer');
						valid = false;
					}
				} else if ($(target).hasClass('double') && value) {
					if ($(target).hasClass('positive')
							&& (!value.match(/^[+]?\d+\.?(\d+)?$/) || !$(target)
									.hasClass('zero')
									&& parseFloat(value) == 0)) {
						Message.showFieldError(target, null, 'double.positive');
						valid = false;
					}
					if (!$(target).hasClass('positive')
							&& !value.match(/^[-+]?\d+\.?(\d+)?$/)) {
						Message.showFieldError(target, null, 'double');
						valid = false;
					}
					var i = value.indexOf('.');
					if (i > -1) {
						var decimal = value.substring(i + 1);
						var scale = parseInt($(target).data('scale') || '2');
						if (decimal.length > scale) {
							value = value.substring(0, i + 1)
									+ decimal.substring(0, scale);
							$(target).val(value);
						}
					}
				} else if (evt != 'keyup' && $(target).hasClass('repeat')) {
					if (value != $(
							'[name="' + $(target).data('repeatwith') + '"]',
							$(target).closest('form')).val()) {
						Message.showFieldError(target, null,
								'repeat.not.matched');
						valid = false;
					}
				}
			}
			return valid;
		} else {
			var valid = true;
			$(':input', target).each(function() {
						if (!Form.validate(this))
							valid = false;
					});
			if (!valid)
				Form.focus(target);
			var groups = {};
			$('[data-only-one-required-group]', target).each(function() {
				var t = $(this);
				if (t.is(':visible,[type="hidden"],.sqleditor,.chzn-done')
						&& !t.prop('disabled')) {
					var group = t.data('only-one-required-group');
					var inputs = groups[group];
					if (!inputs) {
						inputs = [];
						groups[group] = inputs;
					}
					inputs.push(t);
				}
			});
			for (var group in groups) {
				var inputs = groups[group];
				var matched = 0;
				var labels = [];
				$.each(inputs, function(i, v) {
							labels.push($('.control-label',
									v.closest('.control-group')).text());
							if (v.val())
								matched++;
						});
				if (matched != 1) {
					valid = false;
					$.each(inputs, function(i, v) {
								v.closest('.control-group').addClass('error');
							});
					Message.showActionError(MessageBundle.get(
									'required.only.one', '[' + labels + ']'),
							target);
				}
			}
			groups = {};
			$('[data-at-least-one-required-group]', target).each(function() {
				var t = $(this);
				if (t.is(':visible,[type="hidden"],.sqleditor,.chzn-done')
						&& !t.prop('disabled')) {
					var group = t.data('at-least-one-required-group');
					var inputs = groups[group];
					if (!inputs) {
						inputs = [];
						groups[group] = inputs;
					}
					inputs.push(t);
				}
			});
			for (var group in groups) {
				var inputs = groups[group];
				var matched = 0;
				var labels = [];
				$.each(inputs, function(i, v) {
							labels.push($('.control-label',
									v.closest('.control-group')).text());
							if (v.val())
								matched++;
						});
				if (matched < 1) {
					valid = false;
					$.each(inputs, function(i, v) {
								v.closest('.control-group').addClass('error');
							});
					Message.showActionError(MessageBundle
									.get('required.at.least.one', '[' + labels
													+ ']'), target);
				}
			}
			return valid;
		}
	}
};

Ajax = {
	defaultRepacement : 'content',
	fire : function(target, funcName) {
		if (!target)
			return true;
		var func = target[funcName];
		if (typeof func == 'undefined')
			func = $(target).attr(funcName);
		if (typeof func == 'undefined' || !func)
			return true;
		var args = [];
		if (arguments.length > 2)
			for (var i = 2; i < arguments.length; i++)
				args[i - 2] = arguments[i];
		var ret;
		if (typeof(func) == 'function') {
			ret = func.apply(target, args);
		} else {
			if (func.indexOf('return') > -1)
				func = func.replace('return', '');
			target._temp = function() {
				return eval(func)
			};
			try {
				ret = target._temp();
			} catch (e) {
				alert(e);
			}
		}
		if (false == ret)
			return false;
		return true;
	},
	handleResponse : function(data, options) {
		if (!data)
			return;
		var hasError = false;
		var target = options.target;
		if (target && $(target).parents('div.ui-dialog').length)
			options.quiet = true;
		if ((typeof data == 'string')
				&& (data.indexOf('{') == 0 || data.indexOf('[') == 0))
			data = $.parseJSON(data);
		if (typeof data == 'string') {
			var i = data.indexOf('<title>');
			if (i >= 0 && data.indexOf('</title>') > 0) {
				Ajax.title = data.substring(data.indexOf('<title>') + 7, data
								.indexOf('</title>'));
				if (options.replaceTitle)
					document.title = Ajax.title;
				if (i == 0)
					data = data.substring(data.indexOf('</title>') + 8);
			}
			var html = data.replace(/<script(.|\s)*?\/script>/g, '');
			var div = $('<div/>').html(html);
			var replacement = options.replacement;
			if (typeof replacement == 'string') {
				var map = {};
				var entries = replacement.split(',');
				var arr = [];
				for (var i = 0; i < entries.length; i++) {
					var entry = entries[i];
					var ss = entry.split(':', 2);
					var sss = ss.length == 2 ? ss[1] : ss[0];
					map[ss[0]] = sss;
					arr.push(sss != Ajax.defaultRepacement ? sss : '_');
				}
				replacement = map;
			}
			for (var key in replacement) {
				var r = $('#' + key);
				if (key == Ajax.defaultRepacement && !r.length)
					r = $('body');
				$('.intervaled', r).each(function() {
							clearInterval($(this).data('_interval'));
						});
				if (!options.quiet && r.length) {
					var pin = $('.pin', r);
					var top = pin.length ? pin.offset().top : r.offset().top;
					$('html,body').animate({
								scrollTop : top - 50
							}, 100);
				}
				var rep = div.find('#' + replacement[key]);
				if (rep.length) {
					if (rep.children().length == 0)
						r.replaceWith(rep);
					else
						r.html(rep.html());
				} else {
					if (div.find('#content').length)
						r.html(div.find('#content').html());
					else if (div.find('body').length)
						r.html(div.find('body').html());
					else
						r.html(html);
				}
				if (!options.quiet && (typeof $.effects != 'undefined'))
					r.effect('highlight');
				_observe(r);
			}
			div.remove();
			if (options.onsuccess)
				options.onsuccess.apply(window);
			Ajax.fire(target, 'onsuccess', data);
		} else {
			Ajax.jsonResult = data;
			if (data.fieldErrors || data.actionErrors) {
				hasError = true;
				if (options.onerror)
					options.onerror.apply(window);
				Ajax.fire(target, 'onerror', data);
			} else {
				if (options.onsuccess)
					options.onsuccess.apply(window);
				Ajax.fire(target, 'onsuccess', data);
			}
			setTimeout(function() {
						Message.showActionError(data.actionErrors, target);
						Message.showActionMessage(data.actionMessages, target);
					}, 500);

			if (data.fieldErrors) {
				if (target) {
					for (key in data.fieldErrors)
						Message.showFieldError(target[key],
								data.fieldErrors[key]);
					Form.focus(target);
				} else {
					for (key in data.fieldErrors)
						Message.showActionError(data.fieldErrors[key]);
				}
			}
		}
		if (options.submitForm) {
			if (!hasError && $(target).hasClass('disposable'))
				setTimeout(function() {
							$(':input', target).prop('disabled', true)
						}, 100);
			else
				setTimeout(function() {
							$(':submit', target).prop('disabled', false);
							Captcha.refresh()
						}, 100);
			if (!hasError && $(target).hasClass('reset') && target.reset) {
				target.reset();
				$(target).find('.resetable').html('');
			}
		}
		Indicator.text = '';
		Ajax.fire(target, 'oncomplete', data);
	},
	jsonResult : null,
	title : ''
};

function ajaxOptions(options) {
	options = options || {};
	if (!options.dataType)
		options.dataType = 'text';
	if (!options.headers)
		options.headers = {};

	$.extend(options.headers, {
				'X-Data-Type' : options.dataType
			});
	var target = $(options.target);
	var replacement = {};
	var entries = (options.replacement
			|| $(options.target).data('replacement')
			|| ($(options.target).prop('tagName') == 'FORM' ? $(target)
					.attr('id') : null) || Ajax.defaultRepacement).split(',');
	var arr = [];
	for (var i = 0; i < entries.length; i++) {
		var entry = entries[i];
		var ss = entry.split(':', 2);
		var sss = ss.length == 2 ? ss[1] : ss[0];
		replacement[ss[0]] = sss;
		arr.push(sss != Ajax.defaultRepacement ? sss : '_');
	}
	options.replacement = replacement;
	$.extend(options.headers, {
				'X-Fragment' : arr.join(',')
			});
	var beforeSend = options.beforeSend;
	options.beforeSend = function(xhr) {
		if (beforeSend)
			beforeSend(xhr);
		Indicator.text = options.indicator;
		Ajax.fire(null, options.onloading);
	}
	var success = options.success;
	options.success = function(data, textStatus, XMLHttpRequest) {
		Ajax.handleResponse(data, options);
		if (success && !(data.fieldErrors || data.actionErrors))
			success(data, textStatus, XMLHttpRequest);
	};
	return options;
}

function ajax(options) {
	return $.ajax(ajaxOptions(options));
}

var CONTEXT_PATH = $('meta[name="context_path"]').attr('content') || '';

if (typeof(Initialization) == 'undefined')
	Initialization = {};
if (typeof(Observation) == 'undefined')
	Observation = {};
function _init() {
	var array = [];
	for (var key in Initialization) {
		if (typeof(Initialization[key]) == 'function')
			array.push(key);
	}
	array.sort();
	for (var i = 0; i < array.length; i++)
		Initialization[array[i]].call(this);
	_observe();
}
function _observe(container) {
	if (!container)
		container = document;
	$('.chart,form.ajax,.ajaxpanel', container).each(function(i) {
		if (!$(this).attr('id'))
			$(this).attr(
					'id',
					('a' + (i + Math.random())).replace('.', '')
							.substring(0, 5));
	});
	var array = [];
	for (var key in Observation) {
		if (typeof(Observation[key]) == 'function')
			array.push(key);
	}
	array.sort();
	for (var i = 0; i < array.length; i++)
		Observation[array[i]].call(this, container);
}
$(_init);

Initialization.common = function() {
	$(document).ajaxStart(function() {
				Indicator.show()
			}).ajaxError(function() {
				Indicator.showError()
			}).ajaxSuccess(function(ev, xhr) {
		Indicator.hide();
		var url = xhr.getResponseHeader('X-Redirect-To');
		if (url) {
			$('body')
					.html('<div class="modal"><div class="modal-body"><div class="progress progress-striped active"><div class="bar" style="width: 50%;"></div></div></div></div>');
			var url = UrlUtils.absolutize(url);
			try {
				var href = top.location.href;
				if (href && UrlUtils.isSameDomain(href, url))
					top.location.href = url;
				else
					document.location.href = url;
			} catch (e) {
				document.location.href = url;
			}
			return;
		}
	}).keyup(function(e) {
		if (e.keyCode == 27) {
			if ($('.modal:visible').length)
				$('.modal:visible').last().find('.close').click();
			else if ($('.ui-dialog:visible').length)
				$('.ui-dialog:visible').last()
						.find('.ui-dialog-titlebar-close').click();
		}
	}).on('click', 'form.ajax :submit', function() {
				$(this).addClass('clicked');
			}).on('click', 'label[for]', function(event) {
				if ($(document.getElementById($(this).attr('for')))
						.prop('readonly'))
					event.preventDefault();
			}).on('click', '#message .close,.message-container .close',
			function() {
				$('#message,.message-container').each(function(i, v) {
							if (!$.trim($(v).text()))
								$(v).remove();
						});
			}).on('click', '.removeonclick', function() {
				$(this).remove()
			}).on('click', '.field-error .remove', function(e) {
				Form.clearError($(e.target).closest('.control-group'));
				$(e.target).closest('.field-error').remove();
				return false;
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
				if (this.value != this.defaultValue)
					Form.validate(this, 'change');
				return true;
			}).on('change', 'select', function() {
				Form.validate(this, 'change');
				return true;
			}).on('dblclick', '.ui-dialog-titlebar', function() {
		Dialog.toggleMaximization($('.ui-dialog-content', $(this)
						.closest('.ui-dialog')));
	}).on('click', '.ui-dialog .dialog-close', function(evt) {
		evt.preventDefault();
		$(evt.target).closest('.ui-dialog').find('.ui-dialog-titlebar-close')
				.click();
	}).on('mouseenter', '.popover,.tooltip', function() {
				$(this).remove()
			}).on('click', '.action-error strong.force-override', function(e) {
		var msgcontainer = $(e.target).closest('.message-container');
		if (msgcontainer.length) {
			var form = msgcontainer.next('form');
			$('input[type="hidden"].version', form).remove();
			msgcontainer.fadeOut().remove();
			form.submit();
		} else {
			var button = $('button[data-action="save"]:visible');
			$('tr', button.closest('form')).removeData('version')
					.removeAttr('data-version');
			button.click();
		}

	}).on('change', 'select', function(e) {
				var t = $(this);
				var option = t.find('option:eq(0)');
				if (!option.attr('value') && option.text()) {
					if (!t.val())
						t.addClass('empty');
					else
						t.removeClass('empty');
				}
			}).on('click', 'img.captcha', Captcha.refresh).on('focus',
			'input.captcha', function() {
				var t = $(this);
				if (t.siblings('img.captcha').length)
					return;
				t.after('<img class="captcha" src="' + t.data('captcha')
						+ '"/>');
			}).on('keyup', 'input.captcha', function() {
				var t = $(this);
				t.removeClass('input-error');
				if (t.val().length >= 4)
					t.trigger('verify');
			})
			/*
			 * .on('focusout', 'input.captcha', function() {
			 * $(this).trigger('verify'); })
			 */.on('verify', 'input.captcha', function() {
		var t = $(this);
		var img = t.next('img.captcha');
		if (t.val() && img.length) {
			var token = img.attr('src');
			var index = token.indexOf('token=');
			if (index > -1)
				token = token.substring(index + 6);
			index = token.indexOf('&');
			if (index > -1)
				token = token.substring(0, index);
			$.ajax({
						global : false,
						type : "POST",
						url : CONTEXT_PATH + '/verifyCaptcha',
						data : {
							captcha : t.val(),
							token : token
						},
						success : function(result) {
							result == 'false' ? t.addClass('input-error')
									.focus() : t.removeClass('input-error')
									.blur();
						}
					});
		}
	});
	$.alerts.okButton = MessageBundle.get('confirm');
	$.alerts.cancelButton = MessageBundle.get('cancel');
	Nav.init();
	Nav.activate(document.location.pathname);
	var hash = document.location.hash;
	if (hash) {
		$('.nav-tabs').each(function() {
			var found = false;
			$('a[data-toggle="tab"]', this).each(function() {
				var t = $(this);
				if (!found) {
					var selector = t.attr('data-target');
					if (!selector) {
						selector = t.attr('href');
						selector = selector
								&& selector.replace(/.*(?=#[^\s]*$)/, '');
					}
					if (selector == hash) {
						found = true;
						t.tab('show');
						$target = $(selector);
						if ($target.hasClass('ajaxpanel'))
							$target.removeClass('manual');

					}
				}
			});
		});
	}
	if ($(document.body).hasClass('render-location-qrcode')) {
		$('<div id="render-location-qrcode" class="hidden-phone hidden-tablet" style="width:15px;position:fixed;bottom:0;right:0;cursor:pointer;"><i class="glyphicon glyphicon-qrcode"></i></div>')
				.appendTo(document.body);
		$('#render-location-qrcode').click(function() {
			var _this = $(this);
			_this.hide();
			var modal = $('<div class="modal" style="z-index:10000;"><div class="modal-close"><button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button></div><div class="modal-body" style="max-height:600px;"><div class="location-qrcode">'
					+ document.location.href + '</div></div></div>')
					.appendTo(document.body);
			$('.location-qrcode', modal).encodeqrcode();
			$('button.close', modal).click(function() {
						modal.remove();
						_this.show();
					});
		});
	}
	if (document.location.search.indexOf('printpage=true') != -1) {
		window.print();
		setTimeout(function() {
					window.close();
				}, 1000);
	}
	if ($('.time[title]').length) {
		setInterval(function() {
					$('.time[title]').each(function() {
								var t = $(this);
								t.text(DateUtils.humanRead(t.attr('title')));
							});
				}, 60 * 1000);
	}
	if ($('body').hasClass('welcome')) {
		$('<section id="powered-by">Powered by Ironrhino</section>')
				.appendTo(document.body);
	}
	$(document).on('click', '.scroll-list .load-more', function(e) {
		var t = $(this);
		if (t.hasClass('loading'))
			return;
		t.addClass('loading').data('orginaltext', t.text()).text(MessageBundle
				.get('ajax.loading'));
		var list = t.closest('.scroll-list');
		var pagesize = 10;
		if (list.data('pagesize'))
			pagesize = parseInt(list.data('pagesize'));
		var url = list.data('scrollurl') || document.location.href;
		var params = {
			since : $('.scroll-item:last', list).data('position')
		}
		var headers = {};
		if (list.attr('id'))
			headers['X-Fragment'] = list.attr('id');
		$.ajax({
			global : false,
			url : url,
			data : params,
			headers : headers,
			success : function(data) {
				var items = $('<div/>').html(data).find('.scroll-item');
				if (items.length < pagesize) {
					t.prop('disabled', true);
				} else {
					$(window).bind('scroll', function() {
						if ($(window).scrollTop() + $(window).height() > $(document)
								.height()
								- 10) {
							$(window).unbind('scroll');
							$('.scroll-list .load-more').click();
						}
					});
				}
				items.each(function(i, v) {
							t.before(v);
							_observe(v);
						});
			},
			complete : function() {
				t.removeClass('loading').text(t.data('orginaltext'));
			}
		});

	});
	if ($('.scroll-list .load-more').length)
		$(window).scroll(function() {
			if ($(window).scrollTop() + $(window).height() > $(document)
					.height()
					- 10) {
				$(window).unbind('scroll');
				$('.scroll-list .load-more').click();
			}
		});
};

var HISTORY_ENABLED = MODERN_BROWSER
		&& (typeof history.pushState != 'undefined' || typeof $.history != 'undefined')
		&& ($('meta[name="history_enabled"]').attr('content') != 'false');
if (HISTORY_ENABLED) {
	var SESSION_HISTORY_SUPPORT = typeof history.pushState != 'undefined'
			&& document.location.hash.indexOf('#!/') != 0;
	var _historied_ = false;
	Initialization.history = function() {

		if (SESSION_HISTORY_SUPPORT) {
			window.onpopstate = function(event) {
				var url = document.location.href;
				Nav.activate(url);
				if (event.state) {
					ajax({
								url : url,
								replaceTitle : true,
								replacement : event.state.replacement,
								cache : false
							});
				}
			};
			return;
		}
		$.history.init(function(hash) {
					if ((!hash && !_historied_)
							|| (hash && hash.indexOf('!') < 0))
						return;
					var url = document.location.href;
					if (url.indexOf('#') > 0)
						url = url.substring(0, url.indexOf('#'));
					if (hash.length) {
						hash = hash.substring(1);
						if (UrlUtils.isSameDomain(hash)) {
							if (CONTEXT_PATH)
								hash = CONTEXT_PATH + hash;
						}
						url = hash;
					}
					_historied_ = true;
					ajax({
								url : url,
								cache : true,
								replaceTitle : true,
								success : function() {
									Nav.activate(url);
								}
							});
				}, {
					unescape : true
				});
	}
}

Observation.common = function(container) {
	$('select', container).each(function(e) {
				var t = $(this);
				var option = t.find('option:eq(0)');
				if (!option.attr('value') && option.text() && !t.val())
					t.addClass('empty');
			});
	$('.controls .field-error', container).each(function() {
				var text = $(this).text();
				var field = $(':input', $(this).parent());
				$(this).remove();
				Message.showFieldError(field, text);
			});
	var ele = ($(container).prop('tagName') == 'FORM' && $(container)
			.hasClass('focus')) ? container : $('.focus:eq(0)', container);
	if (ele.prop('tagName') != 'FORM' && ele.attr('name')) {
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
	$('form', container).each(function() {
				if (!$(this).hasClass('ajax'))
					$(this).submit(function() {
								$('.action-error').remove();
								return Form.validate(this)
							});
			});
	$('input[type="text"]', container).on('paste', function() {
				var t = $(this);
				setTimeout(function() {
							t.val($.trim(t.val()));
						}, 50);
			}).each(function() {
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
	if (MODERN_BROWSER)
		$('input[type="checkbox"].custom,input[type="radio"].custom', container)
				.each(function(i) {
					$(this).hide();
					if (!this.id)
						this.id = ('a' + (i + Math.random())).replace('.', '')
								.substring(0, 9);
					var label = $(this).next('label.custom');
					if (!label.length)
						$(this).after($('<label class="custom" for="' + this.id
								+ '"></label>'));
					else
						label.attr('for', this.id);
				});
	$('.linkage', container).each(function() {
		var c = $(this);
		c.data('originalclass', c.attr('class'));
		var sw = $('.linkage_switch', c);
		$('.linkage_component', c).show();
		$('.linkage_component', c).not('.' + sw.val()).hide().filter(':input')
				.val('');
		c.attr('class', c.data('originalclass') + ' ' + sw.val());
		sw.change(function() {
					var c = $(this).closest('.linkage');
					var sw = $(this);
					$('.linkage_component', c).show();
					$('.linkage_component', c).not('.' + sw.val()).hide()
							.filter(':input').val('');
					c.attr('class', c.data('originalclass') + ' ' + sw.val());
				});
	});
	$(':input.conjunct', container).bind('conjunct', function() {
		var t = $(this);
		var f = $(this).closest('form');
		var data = {};
		var url = f.prop('action');
		if (url.indexOf('/') > -1) {
			if (url.substring(url.lastIndexOf('/') + 1) == 'save')
				url = url.substring(0, url.lastIndexOf('/')) + '/input';
		} else if (url == 'save')
			url = 'input';
		var hid = $('input[type=hidden][name$=".id"]', f);
		if (hid.val())
			data['id'] = hid.val();
		$(':input.conjunct,input[type=hidden]:not(.nocheck)', f).each(
				function() {
					data[$(this).attr('name')] = $(this).val();
				});
		ajax({
					global : false,
					quiet : true,
					type : t.data('method') || f.attr('method'),
					url : url,
					data : data,
					target : f[0],
					replacement : t.data('replacement')
				});
	});
	$(':input.conjunct', container).change(function() {
				var t = $(this).trigger('conjunct');
			});
	// if (typeof $.fn.datepicker != 'undefined')
	// $('input.date:not([readonly]):not([disabled])', container).datepicker({
	// dateFormat : 'yy-mm-dd'
	// });
	if (typeof $.fn.datetimepicker != 'undefined')
		$('input.date,input.datetime,input.time', container).not('[readonly]')
				.not('[disabled]').each(function() {
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
					} else {
						option.format = t.data('format') || 'yyyy-MM-dd';
						option.pickTime = false;
					}
					t.focus(function() {
						var dp = t.data('datetimepicker');
						if (!dp) {
							t.datetimepicker(option).on('changeDate',
									function(e) {
										t.data('changed', true);
										if (t.hasClass('date'))
											t.blur();
									});
							dp = t.data('datetimepicker');
							dp.show();
						}
					}).blur(function() {
						var dp = t.data('datetimepicker');
						if (dp) {
							if (t.data('changed'))
								t.removeData('changed').trigger('validate')
										.trigger('conjunct');
							dp.widget.remove();
							t.removeData('datetimepicker');
						}
					});
				});
	if (typeof $.fn.treeTable != 'undefined')
		$('.treeTable', container).each(function() {
			$(this).treeTable({
				initialState : $(this).hasClass('expanded')
						? 'expanded'
						: 'collapsed'
			});
		});
	if (typeof $.fn.chosen != 'undefined')
		$('.chosen', container).chosen({
					search_contains : true,
					placeholder_text : MessageBundle.get('select'),
					no_results_text : ' '
				});
	if (typeof $.fn.htmlarea != 'undefined')
		$('textarea.htmlarea', container).htmlarea({
					css : CONTEXT_PATH + '/assets/styles/ironrhino-min.css'
				});
	// bootstrap start
	$('a[data-toggle="tab"]', container).on('shown', function(e) {
				$this = $(e.target);
				var selector = $this.attr('data-target');
				if (!selector) {
					selector = $this.attr('href');
					selector = selector
							&& selector.replace(/.*(?=#[^\s]*$)/, '');
				}
				$target = $(selector);
				if ($target.hasClass('ajaxpanel'))
					$target.trigger('load');
			});
	$('.carousel', container).each(function() {
				var t = $(this);
				t.carousel((new Function("return "
						+ (t.data('options') || '{}')))());
			});
	$(':input[data-helpurl]', container).each(function() {
		var t = $(this);
		var href = '<a href="'
				+ t.data('helpurl')
				+ '" style="padding-left: 5px;" target="_blank"><span class="glyphicon glyphicon-question-sign"></span></a>'
		href = $(href).insertAfter(t);
		if (t.is('textarea'))
			href.find('span').css('vertical-align', 'top');
	});
	$('.tiped', container).each(function() {
		var t = $(this);
		var options = {
			html : true,
			trigger : t.data('trigger') || 'hover',
			placement : t.data('placement') || 'top'
		};
		if (!t.attr('title') && t.data('tipurl'))
			t.attr('title', MessageBundle.get('ajax.loading'));
		t.bind(options.trigger == 'hover' ? 'mouseenter' : options.trigger,
				function() {
					if (!t.hasClass('_tiped')) {
						t.addClass('_tiped');
						$.ajax({
									url : t.data('tipurl'),
									global : false,
									dataType : 'html',
									success : function(data) {
										t.attr('data-original-title', data);
										t.tooltip(options).tooltip('show');
									}
								});
					}
				});
		if (t.is(':input')) {
			options.trigger = 'focus';
			options.placement = 'right';
		}
		t.tooltip(options);
	});
	$('.poped', container).each(function() {
		var t = $(this);
		var options = {
			html : true,
			trigger : t.data('trigger') || 'hover',
			placement : t.data('placement') || 'right'
		};
		if (t.data('popurl')) {
			if (!options.content && t.data('popurl'))
				options.title = MessageBundle.get('ajax.loading');
			t.bind(options.trigger == 'hover' ? 'mouseenter' : options.trigger,
					function() {
						if (!t.hasClass('_poped')) {
							t.addClass('_poped');
							$.ajax({
								url : t.data('popurl'),
								global : false,
								dataType : 'html',
								success : function(data) {
									$('div.popover').remove();
									if (data.indexOf('<title>') >= 0
											&& data.indexOf('</title>') > 0)
										t
												.attr(
														'data-original-title',
														data
																.substring(
																		data
																				.indexOf('<title>')
																				+ 7,
																		data
																				.indexOf('</title>')));
									if (data.indexOf('<body') >= 0
											&& data.indexOf('</body>') > 0)
										t
												.attr(
														'data-content',
														data
																.substring(
																		data
																				.indexOf(
																						'>',
																						data
																								.indexOf('<body')
																								+ 5)
																				+ 1,
																		data
																				.indexOf('</body>')));
									t.popover(options).popover('show');
								}
							});
						}
					});
		}
		t.popover(options);
	});
	// bootstrap end
	$('.btn-switch', container).each(function() {
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
	$('a.popmodal', container).each(function() {
		var t = $(this);
		var id = t.attr('href');
		if (id.indexOf('/') > -1)
			id = id.substring(id.lastIndexOf('/') + 1);
		id += '_modal';
		while ($('#' + id).length)
			id += '_';
		t.click(function(e) {
			if (!$('#' + id).length) {
				$.get(t.attr('href'), function(data) {
					if (typeof data == 'object') {
						if (data.actionErrors) {
							Message.showActionError(data.actionErrors);
						} else {
							console.log(JSON.stringify(data));
						}
					} else {
						var html = data
								.replace(/<script(.|\s)*?\/script>/g, '');
						var div = $('<div/>').html(html);
						var title = $('title', div).html();
						var body = $('#content', div).html();
						var modalwidth = t.data('modalwidth');
						$('<div id="'
								+ id
								+ '" class="modal pop hide fade in"'
								+ (modalwidth ? ' style="width:' + modalwidth
										+ ';"' : '')
								+ '><div class="modal-header"><a class="close" data-dismiss="modal">&times;</a><h3 style="text-align:center;">'
								+ title + '</h3></div><div class="modal-body">'
								+ body + '</div></div>')
								.appendTo(document.body);
						_observe($('#' + id));
						$('form', $('#' + id)).each(function() {
									this.onsuccess = function() {
										$('#' + id).modal('hide');
									};
								});
						t.data('originalhref', t.attr('href')).attr('href',
								'#' + id).attr('data-toggle', 'modal');
						$('#' + id).modal('show');
						if (t.hasClass('nocache'))
							$('#' + id).on('hidden', function() {
										t.attr('href', t.data('originalhref'));
										$(this).remove();
									})
					}
				});
			} else {
				$('#' + id).modal('show');
			}
			return false;
		});

	});
	if (typeof swfobject != 'undefined') {
		$('.chart', container).each(function() {
			var t = $(this);
			var id = t.attr('id');
			var width = t.width();
			var height = t.height();
			var data = t.data('url');
			if (data.indexOf('/') == 0)
				data = document.location.protocol + '//'
						+ document.location.host + data;
			if (!id || !width || !height || !data)
				alert('id,width,height,data all required');
			swfobject.embedSWF(CONTEXT_PATH
							+ '/assets/images/open-flash-chart.swf', id, width,
					height, '9.0.0', CONTEXT_PATH
							+ '/assets/images/expressInstall.swf', {
						'data-file' : encodeURIComponent(data),
						'loading' : MessageBundle.get('ajax.loading')
					}, {
						wmode : 'transparent'
					});
			if (t.hasClass('intervaled')) {
				t.removeClass('intervaled');
				clearInterval(parseInt(t.data('_interval')));
			}
			if (t.data('interval')) {
				var _interval = setInterval(function() {
							if (t.data('quiet')) {
								$.ajax({
											global : false,
											url : data,
											dataType : 'text',
											success : function(json) {
												document.getElementById(id)
														.load(json);
											}
										});
							} else {
								document.getElementById(id).reload(data);
							}
						}, parseInt(t.data('interval')));
				t.addClass('intervaled').data('_interval', _interval);
			}
		});

		window.save_image = function() {
			var content = [];
			content
					.push('<html><head><title>Charts: Export as Image<\/title><\/head><body>');
			$('object[data]').each(function() {
				content.push('<img src="data:image/png;base64,'
						+ this.get_img_binary() + '"/>');
			});
			content.push('<\/body><\/html>');
			var img_win = window.open('', '_blank');
			img_win.document.write(content.join(''));
			img_win.document.close();
		}
	}
	$('a.ajax,form.ajax', container).each(function() {
		var target = this;
		var _opt = ajaxOptions({
					'target' : target,
					onsuccess : function(data) {
						var version = $('input[type="hidden"].version', target);
						version.val(parseInt(version.val() || '0') + 1);
					}
				});
		if (this.tagName == 'FORM') {
			var options = {
				beforeSerialize : function() {
					$('.action-error').remove();
					if (!Form.validate(target))
						return false;
					if (!Ajax.fire(target, 'onprepare'))
						return false;
					Ajax.fire(target, 'onbeforeserialize');
				},
				beforeSubmit : function() {
					$(target).addClass('disabled');
					Indicator.text = $(target).data('indicator');
					$(':submit', target).prop('disabled', true);
					Ajax.fire(target, 'onloading');
					var form = $(target);
					var pushstate = false;
					if (form.hasClass('history'))
						pushstate = true;
					if (options.pushState === false
							|| form.parents('.ui-dialog,.tab-content').length)
						pushstate = false;
					if (pushstate && HISTORY_ENABLED) {
						var url = form.prop('action');
						var index = url.indexOf('://');
						if (index > -1) {
							url = url.substring(index + 3);
							url = url.substring(url.indexOf('/'));
						}
						var params = form.serializeArray();
						var realparams = [];
						if (params) {
							$.map(params, function(v, i) {
										if (v.name == 'resultPage.pageNo')
											v.name = 'pn';
										else if (v.name == 'resultPage.pageSize')
											v.name = 'ps';
										if (!(v.name == 'check'
												|| v.name == 'keyword'
												&& !v.value || v.name == 'pn'
												&& v.value == '1' || v.name == 'ps'
												&& v.value == '10'))
											realparams.push({
														name : v.name,
														value : v.value
													});

									});
							var param = $.param(realparams);
							if (param)
								url += (url.indexOf('?') > 0 ? '&' : '?')
										+ param;
						}
						var location = document.location.href;
						if (SESSION_HISTORY_SUPPORT) {
							history.replaceState({
										url : location
									}, '', location);
							history.pushState(url, '', url);
						} else {
							var hash = url;
							if (CONTEXT_PATH)
								hash = hash.substring(CONTEXT_PATH.length);
							$.history.load('!' + hash);
						}
					}
					if (Ajax.fire(target, 'onbeforesubmit') === false)
						return false;
				},
				error : function() {
					Form.focus(target);
					if (_opt.submitForm)
						setTimeout(function() {
									$('button[type="submit"]', target).prop(
											'disabled', false);
								}, 100);
					Ajax.fire(target, 'onerror');
				},
				success : function(data) {
					Ajax.handleResponse(data, _opt);
				},
				complete : function() {
					$(target).removeClass('disabled');
				},
				headers : _opt.headers
			};
			if (!$(this).hasClass('view'))
				$.extend(options.headers, {
							'X-Data-Type' : 'json'
						});
			$(this).bind('submit', function(e) {
						_opt.submitForm = true;
						var form = $(this);
						var btn = $('.clicked', form);
						if (!btn.length)
							btn = $(':input:focus[type=submit]', form);
						if (btn.hasClass('noajax'))
							return true;
						if (btn.hasClass('reload') || btn.data('action'))
							options.pushState = false;
						if ('multipart/form-data' == $(this).attr('enctype')) {
							var files = [];
							$('input[type="file"]', form).each(function() {
										var fs = this.files;
										if (fs && fs.length > 0)
											for (var i = 0; i < fs.length; i++)
												files.push(fs[i]);
									});
							options.target = target;
							$.ajaxupload(files, options);
						} else {
							$(this).ajaxSubmit(options);
						}
						btn.removeClass('clicked');
						return false;
					});
			return;
		} else {
			$(this).click(function() {
				if (!Ajax.fire(target, 'onprepare'))
					return false;
				var addHistory = HISTORY_ENABLED
						&& $(this).hasClass('view')
						&& !$(this).hasClass('nohistory')
						&& ($(this).hasClass('history') || !($(this)
								.data('replacement')));
				if (addHistory) {
					$('.ui-dialog:visible').children().remove();
					var hash = this.href;
					if (UrlUtils.isSameDomain(hash)) {
						hash = hash.substring(hash.indexOf('://') + 3);
						hash = hash.substring(hash.indexOf('/'));
						if (SESSION_HISTORY_SUPPORT) {
							var location = document.location.href;
							history.replaceState({
										url : location
									}, '', location);
							history.pushState({
										replacement : $(this)
												.data('replacement'),
										url : hash
									}, '', hash);
						} else {
							if (CONTEXT_PATH)
								hash = hash.substring(CONTEXT_PATH.length);
							hash = hash.replace(/^.*#/, '');
							$.history.load('!' + hash);
							return false;
						}
					}

				}
				var t = $(this).addClass('disabled');
				var options = {
					target : this,
					url : this.href,
					type : $(this).data('method') || 'GET',
					cache : $(this).hasClass('cache'),
					beforeSend : function() {
						$('.action-error').remove();
						Indicator.text = $(target).data('indicator');
						Ajax.fire(target, 'onloading');
					},
					error : function() {
						Ajax.fire(target, 'onerror');
					},
					complete : function() {
						t.removeClass('disabled');
					}
				};
				if (!$(this).hasClass('view'))
					$.extend(options.headers, {
								'X-Data-Type' : 'json'
							});
				else if (!$(this).data('replacement'))
					options.replaceTitle = true;
				if (addHistory)
					options.onsuccess = function() {
						Nav.activate(options.url);
					};
				ajax(options);
				return false;
			});
		}
	});
};

var Nav = {
	init : function() {
		$(document).on('click', '.nav:not(.nav-tabs):not(.nav-list) li a',
				function() {
					$('li', $(this).closest('.nav')).removeClass('active');
					Nav.indicate($(this));
				});
	},
	activate : function(url) {
		url = UrlUtils.absolutize(url);
		$('.nav:not(.nav-tabs):not(.nav-list) li').removeClass('active open');
		$('.nav:not(.nav-tabs):not(.nav-list) li a').each(function() {
					if (this.href == url || url.indexOf(this.href + '?') == 0) {
						Nav.indicate($(this));
					}
				});
	},
	indicate : function(a) {
		var dropdown = a.closest('li.dropdown');
		if (!dropdown.length) {
			$(a).closest('li').addClass('active');
			$('#nav-breadcrumb').remove();
			return;
		}
		if (a.hasClass('dropdown-toggle'))
			return;
		dropdown.addClass('active');
		var nb = $('#nav-breadcrumb');
		if (nb.length)
			nb.html('');
		else
			nb = $('<ul id="nav-breadcrumb" class="breadcrumb"/>')
					.prependTo($('#content'));
		nb.append('<li>' + dropdown.children('a').text()
				+ ' <span class="divider">/</span></li>')
				.append('<li class="active">' + a.text() + '</li>');
	}
}

var Dialog = {
	adapt : function(d, iframe) {
		var useiframe = iframe != null;
		var hasRow = false;
		var hideCloseButton = false;
		if (!iframe) {
			$(d).dialog('option', 'title', Ajax.title);
			hasRow = $('div.row', d).length > 0;
			hideCloseButton = d.find('.custom-dialog-close').length;
		} else {
			var doc = iframe.document;
			if (iframe.contentDocument) {
				doc = iframe.contentDocument;
			} else if (iframe.contentWindow) {
				doc = iframe.contentWindow.document;
			}
			$(d).dialog('option', 'title', doc.title);
			$(d).dialog('option', 'minHeight', height);
			var height = $(doc).height() + 20;
			$(iframe).height(height);
			hasRow = $('div.row', doc).length > 0;
			hideCloseButton = $(doc).find('.custom-dialog-close').length;
		}
		d.dialog('moveToTop');
		if (hasRow
				&& !(d.data('windowoptions') && d.data('windowoptions').width)) {
			d.dialog('option', 'width', $(window).width() > 1345
							? '90%'
							: ($(window).width() > 1210 ? '95%' : '100%'));
		}
		if (hideCloseButton)
			$('.ui-dialog-titlebar-close', d.closest('.ui-dialog')).hide();
		var height = d.outerHeight();
		if (height >= $(window).height()) {
			d.dialog('option', 'position', {
						my : 'top',
						at : 'top',
						of : window
					});
		} else {
			d.dialog('option', 'position', {
						my : 'center',
						at : 'center',
						of : window
					});
		}
	},
	toggleMaximization : function(d) {
		var dialog = $(d).closest('.ui-dialog');
		var orginalWidth = dialog.data('orginal-width');
		if (orginalWidth) {
			dialog.width(orginalWidth);
			dialog.height(dialog.data('orginal-height'));
			dialog.removeData('orginal-width').removeData('orginal-height');
			Dialog.adapt($(d));
		} else {
			dialog.data('orginal-width', dialog.width() + 0.2).data(
					'orginal-height', dialog.height() + 0.2);
			var viewportWidth = $(window).width() - 10;
			var viewportHeight = $(window).height() - 10;
			dialog.outerWidth(viewportWidth);
			if (dialog.outerHeight() < viewportHeight)
				dialog.outerHeight(viewportHeight);
			d.dialog('option', 'position', {
						my : 'top',
						at : 'top',
						of : window
					});
		}
	}
}

Captcha = {
	refresh : function() {
		var r = Math.random();
		$('img.captcha').each(function() {
					var src = this.src;
					var i = src.lastIndexOf('&');
					if (i > 0)
						src = src.substring(0, i);
					this.src = src + '&' + r;
				});
		$('input.captcha').val('').focus();
	}
};
ArrayUtils = {
	unique : function(arr) {
		if (arr) {
			var arr2 = [];
			var provisionalTable = {};
			for (var i = 0, item; (item = arr[i]) != null; i++) {
				if (!provisionalTable[item]) {
					arr2.push(item);
					provisionalTable[item] = true;
				}
			}
			return arr2;
		}
	}
};
DateUtils = {
	humanRead : function(date) {
		if (typeof date == 'string') {
			var string = date;
			date = new Date(date);
			if (isNaN(date.getTime())) {
				var arr = string.split(' ');
				date = new Date(arr[0]);
				string = arr[1];
				arr = string.split(':');
				date.setHours(parseInt(arr[0]));
				date.setMinutes(parseInt(arr[1]));
				date.setSeconds(parseInt(arr[2]));
			}
		}
		var now = new Date();
		var delta = now.getTime() - date.getTime();
		var before = (delta >= 0);
		delta = delta < 0 ? -delta : delta;
		delta /= 1000;
		var s;
		if (delta <= 60) {
			return "1分钟内";
		} else if (delta < 3600) {
			delta = Math.floor(delta / 60);
			if (delta == 30)
				s = "半个小时";
			else
				s = delta + "分钟";
		} else if (delta < 86400) {
			var d = delta / 3600;
			var h = Math.floor(d);
			var m = (d - h) * 3600;
			m = Math.floor(m / 60);
			if (m == 0)
				s = h + "个小时";
			else if (m == 30)
				s = h + "个半小时";
			else
				s = h + "个小时" + m + "分钟";
		} else if (delta < 2592000) {
			s = Math.floor(delta / 86400) + "天";
		} else if (delta < 31104000) {
			s = Math.floor(delta / 2592000) + "个月";
		} else {
			s = Math.floor(delta / 3110400) / 10 + "年";
		}
		return s + (before ? "前" : "后");

	},
	addDays : function(date, days) {
		var string = false;
		if (typeof date == 'string') {
			string = true;
			date = new Date(date);
		}
		var time = date.getTime();
		time += (24 * 3600 * 1000) * days;
		date = new Date();
		date.setTime(time);
		return string ? $.format.date(date, 'yyyy-MM-dd') : date;
	},
	getIntervalDays : function(startDate, endDate) {
		if (typeof startDate == 'string')
			startDate = new Date(startDate);
		if (typeof endDate == 'string')
			endDate = new Date(endDate);
		var diff = endDate.getTime() - startDate.getTime();
		return diff / (24 * 3600 * 1000) + 1;
	},

	isLeapYear : function(year) {
		return new Date(year, 1, 29).getMonth() == 1;
	},
	nextLeapDay : function(since) {
		if (typeof since == 'string')
			since = new Date(since);
		var year = since.getFullYear();
		if (DateUtils.isLeapYear(year)) {
			var leapDay = new Date(year, 1, 29);
			if (since.getTime() <= leapDay.getTime())
				return leapDay;
		}
		while (!DateUtils.isLeapYear(++year)) {
		};
		return new Date(year, 1, 29);
	},

	isSpanLeapDay : function(startDate, endDate) {
		if (typeof startDate == 'string')
			startDate = new Date(startDate);
		if (typeof endDate == 'string')
			endDate = new Date(endDate);
		return endDate.getTime() >= DateUtils.nextLeapDay(startDate).getTime();
	}
};