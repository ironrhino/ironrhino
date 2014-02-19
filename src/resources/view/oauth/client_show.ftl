<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('apply')}${action.getText('client')}</title>
</head>
<body>
	<p>client_id:<span style="margin-left:5px;font-weight:bold;">${client.id}</span></p>
	<p>client_secret:<span style="margin-left:5px;font-weight:bold;">${client.secret}</span></p>
	<p>${action.getText('description')}:<span style="margin-left:5px;font-weight:bold;">${client.description}</span></p>
</body>
</html></#escape>