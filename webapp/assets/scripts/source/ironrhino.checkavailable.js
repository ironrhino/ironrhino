(function($) {
	$.fn.checkavailable = function() {
		this.each(function() {
					var t = $(this);
					t.bind('checkavailable', function() {
						if (!t.val())
							return;
						var inputs = $('input[type=hidden]', t.closest('form'))
								.not('[name^="__"]').not('.nocheck').filter(
										function(i, v) {
											return $(v).val() && (v != t[0])
										}).add(t);
						if (t.data('checkwith')) {
							$.each(t.data('checkwith').split(','), function(i,
											v) {
										var ele = $(':input[name="' + v + '"]',
												t.closest('form'));
										inputs = inputs.add(ele);
									});
						}
						var url = t.data('checkurl');
						if (!url) {
							url = t.closest('form').prop('action');
							url = url.substring(0, url.lastIndexOf('/'))
									+ '/checkavailable';
						}
						ajax({
									global : false,
									headers : {
										'X-Target-Field' : t.attr('name')
									},
									target : t.closest('form')[0],
									url : url,
									data : inputs.serialize()
								});

					}).change(function() {
								t.addClass('dirty');
								if (t.is('select,[type=hidden]'))
									t.trigger('checkavailable');
							}).blur(function() {
						if (t.hasClass('dirty')
								&& !t.next('.field-error').length)
							t.trigger('checkavailable');
					});
				})
		return this;
	};
})(jQuery);

Observation.checkavailable = function(container) {
	$$(':input.checkavailable', container).checkavailable();
};