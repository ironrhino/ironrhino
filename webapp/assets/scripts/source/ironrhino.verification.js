(function($) {
	$(function() {
		$(document).on('click', 'img.captcha', Captcha.refresh).on('focus',
				'input.captcha', function() {
					var t = $(this);
					if (t.siblings('img.captcha').length)
						return;
					t.after('<img class="captcha" src="' + t.data('captcha')
							+ '"/>');
				}).on('keyup', 'input.captcha', function() {
					var t = $(this);
					t.removeClass('input-error');
					if (t.val().length == this.maxLength)
						t.trigger('verify');
				})
				/*
				 * .on('focusout', 'input.captcha', function() {
				 * $(this).trigger('verify'); })
				 */.on('verify', 'input.captcha', function() {
			var t = $(this);
			var img = t.next('img.captcha');
			if (t.val() && img.length) {
				var token = img.attr('src');
				var index = token.indexOf('token=');
				if (index > -1)
					token = token.substring(index + 6);
				index = token.indexOf('&');
				if (index > -1)
					token = token.substring(0, index);
				$.ajax({
							global : false,
							type : "POST",
							url : CONTEXT_PATH + '/verifyCaptcha',
							data : {
								captcha : t.val(),
								token : token
							},
							success : function(result) {
								result == 'false' ? t.addClass('input-error')
										.focus() : t.removeClass('input-error')
										.blur();
							}
						});
			}
		}).on('focus', '[name="verificationCode"]', function() {
					var t = $(this);
					if (!t.data('autosend')) {
						t.next('.sendVerificationCode').trigger('click');
						t.data('autosend', true);
					}
				}).on('click', '.sendVerificationCode', function() {
			var btn = $(this).addClass('clicked');
			var f = btn.closest('form');
			var cooldown = parseInt(btn.data('cooldown') || 60);
			var userparam = btn.data('username') || 'username';
			var userinput = f.find('[name="' + userparam + '"]');
			var data = {};
			data[userparam] = userinput.val();
			if (Form.validate(userinput)) {
				ajax({
							type : 'POST',
							url : btn.data('url') || f.prop('action')
									+ '/sendVerificationCode',
							target : f[0],
							data : data,
							onsuccess : function() {
								var input = f.find('[name="verificationCode"]');
								Form.clearError(input);
								input.val('').focus();
								btn.prop('disabled', true).css('width',
										btn.outerWidth()).data('text',
										btn.text()).text(cooldown);
								var intv = setInterval(function() {
											var cd = parseInt(btn.text()) - 1;
											if (cd <= 0) {
												clearInterval(intv);
												btn.prop('disabled', false)
														.text(btn.data('text'));
											} else {
												btn.text(parseInt(btn.text())
														- 1);
											}
										}, 1000);
							}
						});
			}
			return false;
		});
	});
})(jQuery);