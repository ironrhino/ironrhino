(function($) {
	$(function() {
		var username = $('#login [name="username"]');
		if (username.hasClass('webAuthn')) {
			username.wrap('<div class="input-append"></div>').after('<span class="add-on clickable" title="U-Key"><i class="glyphicon glyphicon-log-in"></i></span></div>').next().click(function(e) {
				ajax({
					url: '/webAuthnOptions',
					data: { username: username.val() },
					onsuccess: function(options) {
						options.challenge = Uint8Array.from(window.atob(options.challenge), function(s) { return s.charCodeAt(0) });
						$.each(options.allowCredentials, function() {
							this.id = Uint8Array.from(window.atob(this.id), function(s) { return s.charCodeAt(0) });
						});
						navigator.credentials.get({ 'publicKey': options }).then(function(credential) {
							var data = {
								id: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.rawId))),
								response: {
									authenticatorData: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.authenticatorData))),
									clientDataJSON: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.clientDataJSON))),
									signature: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.signature)))
								}
							};
							var userHandle = credential.response.userHandle;
							if (userHandle)
								data.userHandle = window.btoa(String.fromCharCode.apply(null, new Uint8Array(userHandle)));
							$('[name="password"]').removeClass('sha').val(JSON.stringify(data));
							$($(e.target).closest('form')).submit();
						}).catch(function(error) {
							Message.showActionError(error.message);
						});
					}
				});
			});
		}

		$(document).on('click', '#create-webauthn-credential button', function(e) {
			var form = $(e.target).closest('form');
			if(!Form.validate(form))
				return;
			var username = form.find('[name="username"]').val();
			var url = form.attr('action');
			ajax({
				url: url.substring(0, url.lastIndexOf('/')) + '/options',
				data: { username: username },
				onsuccess: function(options) {
					options.user.id = Uint8Array.from(window.atob(options.user.id), function(s) { return s.charCodeAt(0) });
					options.challenge = Uint8Array.from(window.atob(options.challenge), function(s) { return s.charCodeAt(0) });
					$.each(options.excludeCredentials, function() {
						this.id = Uint8Array.from(window.atob(this.id), function(s) { return s.charCodeAt(0) });
					});
					navigator.credentials.create({ 'publicKey': options }).then(function(credential) {
						var data = {
							id: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.rawId))),
							type: credential.type,
							response: {
								attestationObject: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.attestationObject))),
								clientDataJSON: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.clientDataJSON)))
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
})(jQuery);