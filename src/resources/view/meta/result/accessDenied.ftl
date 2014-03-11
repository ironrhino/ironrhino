<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('access.denied')}</title>
<#assign notlogin = false>
<@authorize ifAllGranted="ROLE_BUILTIN_ANONYMOUS">
<#assign notlogin = true>
</@authorize>
<#if notlogin>
<#assign returnUrl=request.requestURL/>
<#if request.queryString??>
<#assign returnUrl=returnUrl+"?"+request.queryString/>
</#if>
<meta http-equiv="refresh" content="0; url=<@url value="${ssoServerBase!}/login?targetUrl=${returnUrl?url}"/>" />
</#if>
</head>
<body>
<h3 style="text-align:center;">
<#if notlogin>
<a href="<@url value="${ssoServerBase!}/login?targetUrl=${returnUrl?url}"/>">${action.getText('login.required')}</a>
<#else>
${action.getText('access.denied')}
</#if>
</h3>
</body>
</html></#escape>