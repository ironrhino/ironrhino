(function($) {
	$.fn.unmask = function() {
		if (!this.data('unmaskurl')) {
			return this;
		}
		return this.after(' <i class="clickable glyphicon glyphicon-eye-open"></i>').next().click(function(e) {
			var icon = $(e.target);
			var t = icon.prev();
			if (icon.hasClass('glyphicon-eye-open')) {
				var unmasked = t.data('content');
				if (!unmasked) {
					$.alerts({
						type: 'confirm',
						message: MessageBundle.get('confirm.action'),
						callback: function(b) {
							if (b) {
								$.ajax({
									url: t.data('unmaskurl'),
									success: function(data) {
										t.data('content', t.text()).text(data);
										icon.removeClass('glyphicon-eye-open').addClass('glyphicon-eye-close');
									}
								});
							}
						}
					});
				} else {
					t.data('content', t.text()).text(unmasked);
					icon.removeClass('glyphicon-eye-open').addClass('glyphicon-eye-close');
				}
			} else {
				var masked = t.data('content');
				t.data('content', t.text()).text(masked);
				icon.addClass('glyphicon-eye-open').removeClass('glyphicon-eye-close');
			}
		});
	};
})(jQuery);

Observation.unmask = function(container) {
	$$('[data-unmaskurl]', container).unmask();
};