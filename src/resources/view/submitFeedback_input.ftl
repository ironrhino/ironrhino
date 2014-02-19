<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('feedback')}</title>
</head>
<body>
<#if actionMessages?? && actionMessages?size gt 0>
<div>${action.getText('thanks')}</div>
<#else>
<@s.form method="post" cssClass="ajax focus form-horizontal disposable">
	<@s.hidden name="domain" />
	<@s.textfield label="%{getText('name')}" name="name" cssClass="span2" />
	<@s.textfield label="%{getText('contact')}" name="contact" cssClass="span6" />
	<@s.textarea label="%{getText('content')}" name="content" cssClass="span6" />
	<@s.submit value="%{getText('submit')}"/>
</@s.form>
</#if>
</body>
</html></#escape>