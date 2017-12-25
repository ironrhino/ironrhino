(function(jQuery, window) {

	// copy from jquery-migrate-1.4.1.js
	var ua = navigator.userAgent.toLowerCase();
	var match = /(chrome)[ \/]([\w.]+)/.exec(ua)
			|| /(webkit)[ \/]([\w.]+)/.exec(ua)
			|| /(opera)(?:.*version|)[ \/]([\w.]+)/.exec(ua)
			|| /(msie) ([\w.]+)/.exec(ua) || ua.indexOf("compatible") < 0
			&& /(mozilla)(?:.*? rv:([\w.]+)|)/.exec(ua) || [];
	var matched = {
		browser : match[1] || "",
		version : match[2] || "0"
	};
	var browser = {};
	if (matched.browser) {
		browser[matched.browser] = true;
		browser.version = matched.version;
	}
	if (browser.chrome)
		browser.webkit = true;
	else if (browser.webkit)
		browser.safari = true;
	jQuery.browser = browser;

	// copy from jquery-migrate-3.0.0.js
	var oldInit = jQuery.fn.init;
	jQuery.fn.init = function(arg1) {
		var args = Array.prototype.slice.call(arguments);
		if (typeof arg1 === "string" && arg1 === "#") {
			args[0] = [];
		}
		return oldInit.apply(this, args);
	};
	jQuery.fn.init.prototype = jQuery.fn;

	jQuery.fn.extend({
				bind : function(types, data, fn) {
					return this.on(types, null, data, fn);
				},
				unbind : function(types, fn) {
					return this.off(types, null, fn);
				},
				delegate : function(selector, types, data, fn) {
					return this.on(types, selector, data, fn);
				},
				undelegate : function(selector, types, fn) {
					return arguments.length === 1
							? this.off(selector, "**")
							: this.off(types, selector || "**", fn);
				}
			});

	var oldOffset = jQuery.fn.offset;
	jQuery.fn.offset = function() {
		var docElem, elem = this[0], origin = {
			top : 0,
			left : 0
		};
		if (!elem || !elem.nodeType) {
			return origin;
		}
		docElem = (elem.ownerDocument || document).documentElement;
		if (!jQuery.contains(docElem, elem)) {
			return origin;
		}
		return oldOffset.apply(this, arguments);
	};
})(jQuery, window);
