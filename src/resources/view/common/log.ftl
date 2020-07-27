<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('log')}</title>
<style>
	#result{
		line-height:1.5em;
		background: #333;
		word-break: break-all;
		white-space: pre-wrap;
	}
	#result:not(:empty){
		min-height: 500px;
		padding: 1em;
	}
	#result div.error {
		color: #ef0505;
	}
	#result div.warn {
		color: #c09853;
	}
	#result div {
		color: #1ad027;
	}
</style>
<script>
$(function() {
	$('#download').click(function() {
		document.location.href += '/download?id=' + $('#filename').val();
	});
	$('#clear').click(function() {
		$('#result').html('');
	});
	$('#view').click(function() {
		$('#result').html('');
		var source = $('#result').data('source');
		if (source)
			source.close();
		var url = 'log/event?id=' + $('#filename').val();
		if ($('#tail').val())
			url += '&tail=' + $('#tail').val();
		source = new EventSource(url);
		$('#result').data('source', source);
		source.addEventListener('remove', function(event) {
			$('#result').html('');
		}, false);
		source.addEventListener('replace', function(event) {
			$('#result').html('');
			append(event.data);
		}, false);
		source.addEventListener('append', function(event) {
			append(event.data);
		}, false);
		source.onmessage = function(event) {
			append(event.data);
		};
	});
});

function append(data) {
	var result = $('#result');
	var lines = data.split('\n');
	var buffer = [];
	for (var i = 0; i < lines.length; i++) {
		var line = lines[i];
		if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3}/.test(line) && buffer.length) {
			_append(result, buffer);
			buffer = [];
		}
		buffer.push(line);
	}
	if (buffer.length > 0) {
		_append(result, buffer);
	}
	$('html,body').animate({ scrollTop: $(document.body).height() }, 100);
}

function _append(result, buffer) {
	var line = buffer[0];
	var level = (line.indexOf('ERROR') > 0 || line.indexOf('FATAL') > 0) ? 'error' : line.indexOf('WARN') > 0 ? 'warn' : 'info';
	result.append('<div class="' + level + '">' + buffer.join('\n') + '</div>');
}
</script>
</head>
<body>
<form class="form-inline">
	<div class="control-group">
		<input id="filename" type="text" class="span6"/>
		<input id="tail" type="text" class="span1" value="4096"/>
		<button type="button" class="btn" id="view">${getText('view')}</button>
		<button type="button" class="btn" id="clear">${getText('clear')}</button>
		<button type="button" class="btn" id="download">${getText('download')}</button>
	</div>
</form>
<div id="result"></div>
</body>
</html>