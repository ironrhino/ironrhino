(function(jQuery, window, undefined) {
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
})(jQuery, window);