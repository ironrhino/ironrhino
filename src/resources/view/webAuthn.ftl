<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('index')}</title>
<script>
$(function(){
$('#register').click(function(){
	$.post('/webAuthn/creationOptions').done(function(options){
		options.user.id = Uint8Array.from(window.atob(options.user.id), function(s){return s.charCodeAt(0)});
		options.challenge = Uint8Array.from(window.atob(options.challenge), function(s){return s.charCodeAt(0)});
		$.each(options.excludeCredentials,function(){
			this.id = Uint8Array.from(window.atob(this.id), function(s){return s.charCodeAt(0)});
		});
		navigator.credentials.create({'publicKey':options}).then(function(credential){
			var data = {
				id: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.rawId))),
				type: credential.type,
				response: {
					attestationObject: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.attestationObject))),
					clientDataJSON: window.btoa(String.fromCharCode.apply(null, new Uint8Array(credential.response.clientDataJSON)))
				}
			};
			
			$.ajax({
				url: '/webAuthn/register',
				method: 'POST',
				contentType: 'json',
				data: JSON.stringify(data)
			});
		}).catch(function(error){
			console.log('FAIL', error);
		});
	});
});

$('#request').click(function(){
	$.post('/webAuthn/requestOptions').done(function(options){
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
				url: '/webAuthn/authenticate',
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
<@btn id="register" label="register"/>
<@btn id="request" label="request"/>
</body>
</html>
