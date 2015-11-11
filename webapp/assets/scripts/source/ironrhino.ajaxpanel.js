(function($) {
	$.fn.ajaxpanel = function() {
		$(this).each(function() {
					var t = $(this);
					t.bind('load', function() {
								ajaxpanel(t)
							});
					if (t.data('timeout')) {
						setTimeout(function() {
									ajaxpanel(t);
								}, parseInt(t.data('timeout')));
					} else if (t.data('interval')) {
						ajaxpanel(t);
						var _interval = setInterval(function() {
									ajaxpanel(t);
								}, parseInt(t.data('interval')));
						t.addClass('intervaled').data('_interval', _interval);
					} else if (!t.hasClass('manual'))
						ajaxpanel(t);
				});
		return this;
	};
	function ajaxpanel(ele) {
		if (ele.hasClass('tab-pane') && ele.hasClass('cache')
				&& ele.hasClass('loaded'))
			return;
		var url = ele.data('url');
		var options = {
			target : ele[0],
			url : url || document.location.href,
			global : false,
			quiet : true,
			beforeSend : function() {
				if (!ele.data('quiet')) {
					if (typeof $.fn.mask != 'undefined') {
						if (ele.css('min-height') == '0px')
							ele.data('mhc', 'true').css('min-height', '100px');
						ele.mask(MessageBundle.get('ajax.loading'));
					} else {
						ele.html('<div style="text-align:center;">'
								+ MessageBundle.get('ajax.loading') + '</div>');
					}
				}
				if (ele.parent('.portlet-content').length) {
					ele.css('height', window.getComputedStyle(ele[0]).height);
				}
			},
			complete : function() {
				if (!ele.data('quiet') && typeof $.fn.unmask != 'undefined') {
					ele.unmask();
					if (ele.data('mhc'))
						ele.css('min-height', '');
				}
				if (ele.parent('.portlet-content').length) {
					var height = window.getComputedStyle(ele[0]).height;
					ele.css('height', 'auto');
					var targetHeight = window.getComputedStyle(ele[0]).height;
					ele.css('height', height);
					setTimeout(function() {
								ele.css('height', targetHeight);
								setTimeout(function() {
											ele.css('height', 'auto');
										}, 5000);
							}, 15);
				}
			},
			success : function(data) {
				if (url && url.indexOf('://') > 0
						&& !UrlUtils.isSameDomain(document.location.href, url)) {
					var base1 = url.substring(0, url.indexOf('/', url
											.indexOf('://')
											+ 3));
					var base2 = url.substring(0, url.lastIndexOf('/') + 1);
					$('a,form,button', ele).each(function() {
						var t = $(this);
						var arr = 'href,action,formaction'.split(',');
						for (var k = 0; k < arr.length; k++) {
							var attr = arr[k];
							var value = t.attr(attr);
							if (value && value.indexOf('://') < 0) {
								t.attr(attr, (value.indexOf('/') == 0
												? base1
												: base2)
												+ value);
							}
						}
					});
				}
				ele.addClass('loaded');
			}
		};
		if (url)
			options.replacement = ele.attr('id') + ':'
					+ (ele.data('replacement') || 'content');
		else
			options.replacement = ele.attr('id');
		ajax(options);
	}
	$(document).on('click', '.ajaxpanel .load', function() {
				$(this).closest('.ajaxpanel').trigger('load');
			});
})(jQuery);

Observation.ajaxpanel = function(container) {
	$('.ajaxpanel', container).ajaxpanel();
};