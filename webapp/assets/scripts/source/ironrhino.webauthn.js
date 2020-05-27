(function($) {
	$(function() {
		var username = $('#login [name="username"]');
		if (username.hasClass('webAuthn') && navigator.credentials) {
			var password = $('#login [name="password"]');
			var verificationCode = $('#login [name="verificationCode"]');
			username.wrap('<div class="input-append"></div>').after('<span class="add-on clickable" title="U-Key"><i class="glyphicon glyphicon-log-in"></i></span></div>').next().click(function(e) {
				ajax({
					url: '/webAuthnOptions',
					data: { username: username.val() },
					target: username.closest('form')[0],
					onsuccess: function(options) {
						options.challenge = _atob(options.challenge);
						$.each(options.allowCredentials, function() {
							this.id = _atob(this.id);
						});
						navigator.credentials.get({ 'publicKey': options }).then(function(credential) {
							var data = {
								id: _btoa(credential.rawId),
								response: {
									authenticatorData: _btoa(credential.response.authenticatorData),
									clientDataJSON: _btoa(credential.response.clientDataJSON),
									signature: _btoa(credential.response.signature)
								}
							};
							var userHandle = credential.response.userHandle;
							if (userHandle)
								data.userHandle = _btoa(userHandle);
							password.removeClass('sha').val(JSON.stringify(data));
							verificationCode.removeClass('required');
							var form = $(e.target).closest('form');
							form[0].onerror = function() {
								password.addClass('sha').val('');
								verificationCode.addClass('required');
							};
							form.submit();
						}).catch(function(error) {
							password.addClass('sha').val('');
							verificationCode.addClass('required');
							Message.showActionError(error.message);
						});
					}
				});
			});
		}

		$(document).on('click', '#create-webauthn-credential button', function(e) {
			var form = $(e.target).closest('form');
			if (!Form.validate(form))
				return;
			var username = form.find('[name="username"]').val();
			var url = form.attr('action');
			ajax({
				url: url.substring(0, url.lastIndexOf('/')) + '/options',
				data: { username: username },
				onsuccess: function(options) {
					options.user.id = _atob(options.user.id);
					options.challenge = _atob(options.challenge);
					$.each(options.excludeCredentials, function() {
						this.id = _atob(this.id);
					});
					navigator.credentials.create({ 'publicKey': options }).then(function(credential) {
						var data = {
							id: _btoa(credential.rawId),
							type: credential.type,
							response: {
								attestationObject: _btoa(credential.response.attestationObject),
								clientDataJSON: _btoa(credential.response.clientDataJSON)
							}
						};
						form.find('[name="credential"]').val(JSON.stringify(data));
						form.submit();
					}).catch(function(error) {
						Message.showActionError(error.message);
					});
				}
			});

		});
	});


	function _atob(input) {
		return Uint8Array.from(window.atob(input), function(s) { return s.charCodeAt(0) });
	}

	function _btoa(input) {
		return window.btoa(String.fromCharCode.apply(null, new Uint8Array(input)))
	}

})(jQuery);