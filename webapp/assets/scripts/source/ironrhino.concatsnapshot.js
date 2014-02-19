(function($) {

	$.fn.concatsnapshot = function() {
		this.click(function() {
			var t = $(this);
			var target = t.data('target') ? document.getElementById($(this)
					.data('target')) : this.parentNode();
			$(target).data('original-width', $(target).innerWidth());
			var field = document.getElementById(t.data('field'));
			var count = $(target).data('count');
			if (count && parseInt(count) >= parseInt(t.data('maximum'))) {
				Message.showActionError(MessageBundle
						.get(t.data('error') || 'maximum.exceeded', count, t
										.data('maximum')));
				return false;
			}

			$.snapshot({
						onsnapshot : function(canvas) {
							var count = $(target).data('count');
							count
									? $(target).data('count',
											parseInt(count) + 1)
									: $(target).data('count', 1);
							var image = target.querySelector('canvas');
							if (!image) {
								image = document.createElement('canvas');
								image.width = canvas.width;
								image.height = canvas.height;
								target.innerHTML = '';
								target.appendChild(image);
								var context = image.getContext('2d');
								context.drawImage(canvas, 0, 0);
							} else {
								var image2 = image;
								image2.style.display = 'none';
								image = document.createElement('canvas');
								image.width = Math.max(image2.width,
										canvas.width);
								image.height = image2.height + canvas.height;
								target.appendChild(image);
								var context = image.getContext('2d');
								context.drawImage(image2, 0, 0);
								context.drawImage(canvas, 0, image2.height);
								image2.parentNode.removeChild(image2);
							}
							if (image.width > $(target).data('original-width'))
								$(image).css('width', '100%');
							if (field) {
								var maxwidth = parseInt($(field)
										.data('maxwidth'));
								if (maxwidth && canvas.width > maxwidth)
									scale(canvas, maxwidth / canvas.width);
								var data = image.toDataURL();
								var maxlength = parseInt($(field)
										.data('maximum'))
										|| parseInt($(field).attr('maxlength'));
								if (data.length > maxlength) {
									Message.showActionError(MessageBundle.get(
											$(field).data('error')
													|| 'maximum.exceeded',
											data.length, maxlength));
									$(target).data('count', '0');
									image.parentNode.removeChild(image);
								} else {
									$(field).val(data);
									var form = $(field).closest('form');
									if (!form.hasClass('nodirty'))
										form.addClass('dirty');
								}
							}
						},
						onerror : function(error) {
							Message.showActionError(error);
						}
					});
		});
		return this;
	}

	function scale(canvas, stretchRatio) {
		var copy = document.createElement('canvas');
		copy.width = canvas.width;
		copy.height = canvas.height;
		var context = copy.getContext('2d');
		context.drawImage(canvas, 0, 0, canvas.width, canvas.height);
		context = canvas.getContext('2d');
		var width = canvas.width = Math.floor(copy.width * stretchRatio);
		var height = canvas.height = Math.floor(copy.height * stretchRatio);
		context.drawImage(copy, 0, 0, copy.width, copy.height, 0, 0, width,
				height);
		copy = null;
	};
})(jQuery);

Observation.concatsnapshot = function(container) {
	$('.concatsnapshot', container).concatsnapshot();
};