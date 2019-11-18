<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('create')}${getText('webAuthnCredential')}</title>
<script>
$(function() {
	$('#create :button').click(function(e) {
		var form = $(e.target).closest('form');
		var username = $('#create [name="username"]').val();
		ajax({
			url: form.attr('action') + '/options',
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
					ajax({
						url: form.attr('action') + '/create',
						method: 'POST',
						data: {
							username: username,
							credential: JSON.stringify(data)
						}
					});
				}).catch(function(error) {
					console.log('FAIL', error);
				});
			}
		});
	});
});
</script>
</head>
<body>
<@s.form theme="simple" id="create" action=actionBaseUrl method="post" class="form-inline focus">
<div class="control-group">
	<@s.textfield theme="simple" name="username" class="required"/>
	<button type="button" class="btn btn-primary">${getText('create')}</button>
</div>
</@s.form>
</body>
</html>
