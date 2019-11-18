<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('WebAuthn')}</title>
<script>
$(function(){
$('#request').click(function(){
	$.post('/webauthn/authenticate/options').done(function(options){
		options.challenge = Uint8Array.from(window.atob(options.challenge), function(s){return s.charCodeAt(0)});
		$.each(options.allowCredentials,function(){
			this.id = Uint8Array.from(window.atob(this.id), function(s){return s.charCodeAt(0)});
		});
		navigator.credentials.get({'publicKey':options}).then(function(credential){
			
			var data = {
				id: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.rawId))),
				response: {
					authenticatorData: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.authenticatorData))),
					clientDataJSON: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.clientDataJSON))),
					signature: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.signature)))
				}
			};
			
			var userHandle = credential.response.userHandle;
			if(userHandle)
				data.userHandle = window.btoa(String.fromCharCode.apply(null, new Uint8Array(userHandle)));
			
			$.ajax({
				url: '/webauthn/authenticate',
				method: 'POST',
				contentType: 'json',
				data: JSON.stringify(data)
			});
		}).catch(function(error){
			console.log('FAIL', error);
		});
	});
});
});
</script>
</head>
<body>
<@btn id="request" label="request"/>
</body>
</html>
