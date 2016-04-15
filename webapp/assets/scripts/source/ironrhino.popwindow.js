(function($) {
	$.fn.popwindow = function() {
		this.click(function() {
					var t = $(this);
					var options = (new Function("return "
							+ (t.data('windowoptions') || '{}')))();
					var url = t.attr('href');
					var winid = window.open(url, options);
					delete options.iframe;
					for (var key in options)
						$('#' + winid).dialog('option', key, options[key]);
					Dialog.adapt($('#' + winid));
					return false;
				});
		return this;
	};

	window.open = function(url, options) {
		if (typeof options == 'undefined' || typeof options == 'string') {
			options = {
				iframe : false
			};
		} else if (typeof options == 'boolean') {
			options = {
				iframe : options
			};
		}
		var useiframe = options.iframe;
		delete options.iframe;
		var winindex = $(document).data('winindex') || 0;
		winindex++;
		$(document).data('winindex', winindex);
		var winid = '_window_' + winindex;
		var win = $('<div id="' + winid + '" class="window-pop"></div>')
				.appendTo(document.body).dialog();
		if (!useiframe) {
			// ajax replace
			var target = win.get(0);
			target.onsuccess = function() {
				if (typeof $.fn.mask != 'undefined')
					win.unmask();
				Dialog.adapt(win);
			};
			ajax({
						url : url,
						cache : false,
						target : target,
						replacement : winid + ':content',
						quiet : true
					});
		} else {
			// embed iframe
			win.html('<iframe style="width:100%;height:550px;border:0;"/>');
			url += (url.indexOf('?') > 0 ? '&' : '?') + 'decorator=simple&'
					+ Math.random();
			var iframe = $('#' + winid + ' > iframe')[0];
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
			closeOnEscape : true,
			close : function() {
				win.html('').dialog('destroy').remove();
			}
		};
		win.dialog(opt);
		for (var key in options)
			win.dialog('option', key, options[key]);
		win.dialog('open');
		win.closest('.ui-dialog').css('z-index', 2000);
		$('.ui-dialog-titlebar-close', win.closest('.ui-dialog')).blur();
		return winid;
	}

})(jQuery);

Observation.popwindow = function(container) {
	$$('.popwindow', container).popwindow();
};