<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('access.denied')}</title>
<@authorize ifNotGranted="ROLE_BUILTIN_USER">
<#assign returnUrl=request.requestURL/>
<#if request.queryString??>
<#assign returnUrl=returnUrl+"?"+request.queryString/>
</#if>
<meta http-equiv="refresh" content="0; url=<@url value="${ssoServerBase!}/login?targetUrl=${returnUrl?url}"/>" />
</@authorize>
</head>
<body>
<h3 style="text-align:center;">
<@authorize ifNotGranted="ROLE_BUILTIN_USER">
<a href="<@url value="${ssoServerBase!}/login?targetUrl=${returnUrl?url}"/>">${action.getText('login.required')}</a>
</@authorize>
<@authorize ifAnyGranted="ROLE_BUILTIN_USER">
${action.getText('access.denied')}
</@authorize>
</h3>
</body>
</html></#escape>