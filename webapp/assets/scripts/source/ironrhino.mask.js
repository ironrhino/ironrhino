(function($) {

	$.fn.mask = function() {
		$(this).each(function() {
			var t = $(this);
			if (t.hasClass('masked'))
				return;
			if (t.css('position') == 'static')
				t.addClass('masked-relative');
			if (t.height() < 50)
				t.addClass('masked-min-height');
			t
					.addClass('masked')
					.append('<div class="mask spinner"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>');
		});
		return this;
	};

	$.fn.unmask = function() {
		$(this).each(function() {
			var t = $(this);
			if (t.data('mhc'))
				t.css('min-height', '');
			t.removeClass('masked').removeClass('masked-relative')
					.removeClass('masked-min-height').find('.mask').remove();
		});
		return this;
	};

})(jQuery);