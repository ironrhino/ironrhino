<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('feedback')}</title>
</head>
<body>
<#if actionMessages?? && actionMessages?size gt 0>
<div>${action.getText('thanks')}</div>
<#else>
<@s.form id="submitFeedback_input" method="post" class="ajax focus form-horizontal disposable">
	<@s.hidden name="domain" />
	<@s.textfield label="%{getText('name')}" name="name" class="span2" />
	<@s.textfield label="%{getText('contact')}" name="contact" class="span6" />
	<@s.textarea label="%{getText('content')}" name="content" class="span6" />
	<@s.submit value="%{getText('submit')}" class="btn-primary"/>
</@s.form>
</#if>
</body>
</html></#escape>