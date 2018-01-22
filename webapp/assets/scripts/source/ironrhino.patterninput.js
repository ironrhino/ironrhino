(function($) {
	$.fn.patterninput = function() {
		return this.each(function() {
			var t = $(this);
			var options = $.extend({
						minCoords : 3,
						maxCoords : 20
					},
					(new Function("return " + (t.data('options') || '{}')))());
			t
					.wrap('<div class="input-append"/>')
					.parent()
					.append('<span class="add-on clickable"><i class="glyphicon glyphicon-lock"></i></span>');
			t.next('.clickable').click(function() {
				$('#pattern-modal').remove();
				var modal = $('<div id="pattern-modal" class="modal" style="z-index:10000;"><div class="modal-close"><a class="close" data-dismiss="modal" aria-hidden="true"/></div><div class="modal-body" style="max-height:600px;"><div class="message" style="height: 38px;"></div><div class="pattern" style="margin-top: -38px;"></div></div></div>')
						.appendTo(topDocument.body);
				modal.find('.close').click(function() {
							modal.remove();
						});
				options.oncomplete = function(coords) {
					if (coords.length >= options.minCoords
							&& coords.length <= options.maxCoords) {
						modal.remove();
						t.val(JSON.stringify(coords)).trigger('validate');
						if (t.hasClass('submit')) {
							var f = t.closest('form');
							var inputed = true;
							$(':input', f).each(function() {
								if ($(this).hasClass('required')
										&& !$(this).val())
									inputed = false;
							});
							if (inputed)
								f.submit();
						}
					} else {
						var msg = modal
								.find('.message')
								.html('<div class="alert alert-error unselectable" style="padding:0;">'
										+ MessageBundle.get(
												'pattern.coords.invalid',
												options.minCoords,
												options.maxCoords) + '</div>');
					}
				};
				modal.find('.pattern').pattern(options);
			});
		});
	}
})(jQuery);

Observation._patterninput = function(container) {
	$$('input.input-pattern', container).patterninput();
};
