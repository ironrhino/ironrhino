(function($) {

	$.fn.ajaxsubmit = function(options) {

		var url = this.find('.clicked:submit').attr('formaction')
				|| this.prop('action') || window.location.pathname;

		options = $.extend(true, {
					url : url,
					type : this.attr('method') || 'GET'
				}, options);

		// hook for manipulating the form data before it is extracted;
		// convenient for use with rich editors like tinyMCE or FCKEditor
		var veto = {};
		this.trigger('form-pre-serialize', [this, options, veto]);
		if (veto.veto) {
			return this;
		}

		// provide opportunity to alter form data before it is serialized
		if (options.beforeSerialize
				&& options.beforeSerialize(this, options) === false) {
			return this;
		}

		var traditional = options.traditional;
		if (traditional === undefined) {
			traditional = $.ajaxSettings.traditional;
		}

		var a = $.formToArray(this[0]);

		// give pre-submit callback an opportunity to abort the submit
		if (options.beforeSubmit
				&& options.beforeSubmit(a, this, options) === false) {
			return this;
		}

		// fire vetoable 'validate' event
		this.trigger('form-submit-validate', [a, this, options, veto]);
		if (veto.veto) {
			return this;
		}

		var $form = this.eq(0);
		var fileselector = 'input[name][type="file"]:enabled';
		var hasfile = false;
		$form.find(fileselector).each(function() {
					if (this.value)
						hasfile = true;
				});
		if (hasfile) {
			var formdata = new FormData();
			for (var i = 0; i < a.length; i++)
				formdata.append(a[i].name, a[i].value);
			if (options.data)
				for (var k in options.data)
					formdata.append(k, options.data[k])
			options.data = formdata;
			var files = [];
			$form.find(fileselector).each(function() {
						var fs = this.files;
						for (var i = 0; i < fs.length; i++)
							files.push({
										name : this.name,
										value : fs[i]
									});
					});
			$.ajaxupload(files, options);
		} else {
			var q = $.param(a, traditional);
			if (options.data) {
				// extra data
				var qx = $.param(options.data, traditional);
				q = (q ? (q + '&' + qx) : qx);
			}
			options.data = q;
			$.ajax(options);
		}
		// fire 'notify' event
		this.trigger('form-submit-notify', [this, options]);
		return this;

	};

	$.formToArray = function(form) {
		var a = [];
		var els = form.elements;
		for (var i = 0; i < els.length; i++) {
			var el = els[i];
			var n = el.name;
			var v = $.fieldValue(el);
			if (v && v.constructor == Array) {
				for (var j = 0; j < v.length; j++) {
					a.push({
								name : n,
								type : el.type,
								value : v[j]
							});
				}
			} else if (v !== null && typeof v != 'undefined') {
				a.push({
							name : n,
							value : v,
							type : el.type
						});
			}
		}
		return a;
	};

	$.fieldValue = function(el) {
		var n = el.name, t = el.type, tag = el.tagName.toLowerCase(), $t = $(el), $f = $t
				.closest('form');
		if (!n || el.disabled || t == 'reset' || t == 'button' || t == 'file'
				|| (t == 'checkbox' || t == 'radio') && !el.checked
				|| (t == 'submit' || t == 'image') && !$t.hasClass('clicked')
				|| tag == 'select' && el.selectedIndex < 0) {
			return null;
		}
		var value;
		if (tag == 'select') {
			var index = el.selectedIndex;
			var a = [], ops = el.options;
			var one = (t == 'select-one');
			var max = (one ? index + 1 : ops.length);
			for (var i = (one ? index : 0); i < max; i++) {
				var op = ops[i];
				if (op.selected) {
					var v = op.value;
					if (!v) { // extra pain for IE...
						v = (op.attributes && op.attributes['value'] && !(op.attributes['value'].specified))
								? op.text
								: op.value;
					}
					if (one) {
						value = v;
						break;
					}
					a.push(v);
				}
			}
			if (!one)
				return a;
		} else {
			value = $t.val();
		}
		var ignoreBlank = $t.hasClass('ignore-blank')
				|| !$t.hasClass('not-ignore-blank')
				&& $f.hasClass('ignore-blank');
		if (ignoreBlank && !value)
			return null;
		if (n.endsWith('-op') && $f.is('.query')) {
			var $t2 = $f.find('[name="' + n.substring(0, n.lastIndexOf('-'))
					+ '"]');
			ignoreBlank = $t2.hasClass('ignore-blank')
					|| !$t2.hasClass('not-ignore-blank')
					&& $f.hasClass('ignore-blank');
			if (ignoreBlank && !$t2.val())
				return null;
		}
		if (value && t == 'password') {
			if ($t.hasClass('sha') && typeof sha1 != 'undefined') {
				try {
					value = sha1(value);
				} catch (e) {
					console.log(e);
				}
			}
			if (n.toLowerCase().endsWith('password')
					&& typeof $.rc4EncryptStr != 'undefined') {
				try {
					var key = $.cookie('X');
					if (!key) {
						key = '' + Math.random();
						$.cookie('X', key);
					}
					if (key && key.length > 10)
						key = key.substring(key.length - 10, key.length);
					value = $.rc4EncryptStr(value + key, key);
				} catch (e) {
				}
			}
		}
		return value;
	};

})(jQuery);