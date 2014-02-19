(function($) {

	$.fn.decodeqrcode = function() {
		this.click(function(e) {
					var target = $($(e.target).data('target'));
					$.snapshot({
								oncapture : function(canvas, success) {
									try {
										var context = canvas.getContext('2d');
										qrcode.width = canvas.width;
										qrcode.height = canvas.height;
										qrcode.imagedata = context
												.getImageData(0, 0,
														qrcode.width,
														qrcode.height);
										var data = qrcode.process(context);
										if (target.is(':input'))
											target.val(data);
										else
											target.text(data);
										if (success)
											success();
									} catch (e) {
									}
								},
								onerror : function(msg) {
									Message.showError(msg);
								}
							});
				});
		return this;
	};

})(jQuery);

Observation.decodeqrcode = function(container) {
	$('.decodeqrcode', container).decodeqrcode();
};