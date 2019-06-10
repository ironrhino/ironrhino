<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.3.0/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
<title>stomp</title>
	<script>
	
	var client = null;
	
	$(function () {
		$('#connect').click(function(){
			var socket = new SockJS('/stomp/websocket');
			client = Stomp.over(socket);
			client.connect({}, function (frame) {
				setConnected(true);
				$('#username').text(frame.headers['user-name']);
				client.subscribe('/topic/greetings', function (frame) {
					$('#greetings').append('<tr><td>' + frame.body + '</td></tr>');
				});
			}, function(message){
				$('#disconnect').click();
			});	
		});
		$('#disconnect').click(function(){
			if (client == null)
				return;
			client.disconnect();
			setConnected(false);
			console.log('Disconnected');
		});
		$('#send').click(function() {
			if (client == null)
				return;
			client.send('/app/hello', {}, $('#name').val());
		});
	});
	
	function setConnected(connected) {
		$('#connect').prop('disabled', connected);
		$('#disconnect').prop('disabled', !connected);
		$('#conversation')[connected?'show':'hide']();
		$('#username').html('');
		$('#greetings').html('');
	}

	</script>
</head>
<body>
<div id="main-content" class="container">
	<div class="row">
		<div class="span6">
			<div class="control-group">
				<button id="connect" class="btn" type="button">Connect</button>
				<button id="disconnect" class="btn" type="button" disabled>Disconnect</button>
			</div>
		</div>
		<div class="span6">
			<form class="form-inline">
				<input type="text" id="name" placeholder="Your name here...">
				<button id="send" class="btn" type="button">Send</button>
			</form>
		</div>
	</div>
	<div class="row">
		<div class="span12">
			<table id="conversation" class="table" style="display: none;">
				<thead><tr><th>Greetings to <span id="username"></span></th></tr></thead>
                <tbody id="greetings"></tbody>
			</table>
		</div>
	</div>
</div>
</body>
</html>