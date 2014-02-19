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
		ele.css('min-height', '100px');
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
				if (!ele.data('quiet'))
					if (typeof $.fn.mask != 'undefined')
						ele.mask(MessageBundle.get('ajax.loading'));
					else
						ele.html('<div style="text-align:center;">'
								+ MessageBundle.get('ajax.loading') + '</div>');
			},
			complete : function() {
				if (!ele.data('quiet') && typeof $.fn.unmask != 'undefined')
					ele.unmask();
			},
			success : function(data) {
				ele.addClass('loaded');
				ele.css('min-height', '');
			}
		};
		if (url)
			options.replacement = ele.attr('id') + ':'
					+ (ele.data('replacement') || 'content');
		else
			options.replacement = ele.attr('id');
		ajax(options);
	}
})(jQuery);

Observation.ajaxpanel = function(container) {
	$('.ajaxpanel', container).ajaxpanel();
	$('.ajaxpanel .load', container).click(function() {
				$(this).closest('.ajaxpanel').trigger('load');
			});
};