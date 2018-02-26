<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>event</title>
<style>
	#result{
		line-height:1.5em;
	}
</style>
<script>
	document.addEventListener('DOMContentLoaded',function(event){
		var url = '/api/sse/reactiveEvent';
		var source = new EventSource(url);
		var result = document.getElementById('result');
		source.addEventListener('remove',function(event){
			result.innerHTML='';
		},false);
		source.addEventListener('replace',function(event){
			result.innerHTML=event.data.replace(/\n/g,'<br>')+'<br/>';
		},false);
		source.addEventListener('append',function(event){
			result.innerHTML+=event.data.replace(/\n/g,'<br>')+'<br/>';
		},false);
		source.onmessage = function (event) {
			result.innerHTML+=event.data.replace(/\n/g,'<br>')+'<br/>';
		};
	});
	</script>
</head>
<body>
<div id="result">
</div>
</body>
</html>