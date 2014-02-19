<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('log')}</title>
<style>
	#result{
		line-height:1.5em;
	}
</style>
<script>
	$(function(){
		$('#download').click(function(){
			document.location.href+='/download?id='+$('#filename').val();
		});
		$('#clear').click(function(){
			$('#result').html('');
		});
		$('#view').click(function(){
			$('#result').html('');
			var source = $('#result').data('source');
			if(source) source.close();
			var url = 'log/event?id='+$('#filename').val();
			if($('#tail').val())
				url+='&tail='+$('#tail').val();
			source = new EventSource(url);
			$('#result').data('source',source);
			source.addEventListener('remove',function(event){
				$('#result').html('');
			},false);
			source.addEventListener('replace',function(event){
				$('#result').html(event.data.replace(/\n/g,'<br>')+'<br/>');
			},false);
			source.addEventListener('append',function(event){
				$('#result').append(event.data.replace(/\n/g,'<br>')+'<br/>');
			},false);
	        source.onmessage = function (event) {
	           	$('#result').append(event.data.replace(/\n/g,'<br>')+'<br/>');
	        };
		});
	});
	</script>
</head>
<body>
<form class="form-inline">
	<div class="control-group">
		<input id="filename" type="text" class="span6"/>
		<input id="tail" type="text"  class="span1" value="4096"/>
		<button type="button" class="btn" id="view">${action.getText('view')}</button>
		<button type="button" class="btn" id="clear">${action.getText('clear')}</button>
		<button type="button" class="btn" id="download">${action.getText('download')}</button>
	</div>
</form>
<div id="result">
</div>
</body>
</html></#escape>