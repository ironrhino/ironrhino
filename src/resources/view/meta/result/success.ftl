<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('warnings')}</title>
</head>
<body>
 <div class="alert alert-warning"><a class="close" data-dismiss="alert">&times;</a>${action.getText('template.not.found')}</div>
</body>
</html></#escape>