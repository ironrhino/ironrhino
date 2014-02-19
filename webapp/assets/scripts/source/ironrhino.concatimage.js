(function($) {

	$.fn.concatimage = function() {
		this.each(function() {
					var t = $(this);
					var target = t.data('target') ? document.getElementById(t
							.data('target')) : this.parentNode();
					$(target).data('original-width', $(target).innerWidth());
					var field = document.getElementById(t.data('field'));
					var maximum = parseInt(t.data('maximum'));
					t.click(function() {
								$('<input type="file" multiple/>')
										.appendTo(target).hide().change(
												function() {
													concatenateImages(
															this.files, target,
															field, maximum,
															t.data('error'));
													$(this).remove();
												}).click();
							});
					$(target).bind('dragover', function(e) {
								$(this).addClass('drophover');
								return false;
							}).bind('dragleave', function(e) {
								$(this).removeClass('drophover');
								return false;
							}).get(0).ondrop = function(e) {
						e.preventDefault();
						$(this).removeClass('drophover');
						concatenateImages(e.dataTransfer.files, target, field,
								maximum, t.data('error'));
						return true;
					};
				});
		return this;
	}

	function concatenateImages(files, target, field, maximum, error) {
		if (files.length > maximum) {
			Message.showActionError(MessageBundle.get(error
							|| 'maximum.exceeded', files.length, maximum));
			return;
		}
		window.URL = window.URL || window.webkitURL || window.mozURL;
		$(target).text('');
		var count = files.length;
		for (var i = 0; i < count; i++) {
			var img = document.createElement("img");
			var url = window.URL.createObjectURL(files[i]);
			img.src = url;
			img.onload = function() {
				window.URL.revokeObjectURL(url);
				count--;
				if (count == 0)
					doConcatenateImages(target, field);
			}
			img.onerror = function() {
				this.parentNode.removeChild(this);
				window.URL.revokeObjectURL(url);
				count--;
				if (count == 0)
					doConcatenateImages(target, field);
			}
			target.appendChild(img);
		}
	}

	function doConcatenateImages(target, field) {
		var imgs = target.querySelectorAll("img");
		var maxWidth = 0;
		var height = 0;
		for (var i = 0; i < imgs.length; i++) {
			var img = imgs[i];
			maxWidth = Math.max(img.naturalWidth || img.width, maxWidth);
			height += img.naturalHeight || img.height;
		}
		var canvas = document.createElement("canvas");
		canvas.width = maxWidth;
		canvas.height = height;
		var context = canvas.getContext('2d');
		height = 0;
		for (var i = 0; i < imgs.length; i++) {
			var img = imgs[i];
			context.drawImage(img, 0, height);
			target.removeChild(img);
			height += img.height;
		}
		target.appendChild(canvas);
		if (canvas.width > $(target).data('original-width'))
			$(canvas).css('width', '100%');
		if (field) {
			var maxwidth = parseInt($(field).data('maxwidth'));
			if (maxwidth && canvas.width > maxwidth)
				scale(canvas, maxwidth / canvas.width);
			var data = canvas.toDataURL();
			var maxlength = parseInt($(field).data('maximum'))
					|| parseInt($(field).attr('maxlength'));
			if (data.length > maxlength) {
				Message.showActionError(MessageBundle.get($(field)
								.data('error')
								|| 'maximum.exceeded', data.length, maxlength));
				canvas.parentNode.removeChild(canvas);
				$(target).data('count', '0');
			} else {
				$(field).val(data);
				$(target).data('count', imgs.length);
				var form = $(field).closest('form');
				if (!form.hasClass('nodirty'))
					form.addClass('dirty');
			}
		}
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

Observation.concatimage = function(container) {
	$('.concatimage', container).concatimage();
};