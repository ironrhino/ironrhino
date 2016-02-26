(function($) {
	$.fn.editme = function() {
		return $(this).attr('contenteditable', 'true').keyup(function() {
					$(this).addClass('edited')
				}).blur(blur);
	};
	function blur() {
		var t = $(this);
		var url = t.data('url') || document.location.href;
		var name = t.data('name') || 'content';
		var data = {};
		data[name] = t.html();
		if (t.hasClass('edited'))
			$.alerts.confirm(MessageBundle.get('confirm.save'), MessageBundle
							.get('select'), function(b) {
						if (b) {
							ajax({
										url : url,
										type : 'POST',
										data : data,
										global : false,
										success : function() {
											t.removeClass('edited');
										}
									});
						}
					});

	}
})(jQuery);

Observation.editme = function(container) {
	$$('.editme', container).editme();
};