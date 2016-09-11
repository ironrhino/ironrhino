<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('access.denied')}</title>
<#assign redirectToLogin = false>
<@authorize ifAllGranted="ROLE_BUILTIN_ANONYMOUS">
<#assign redirectToLogin = true>
</@authorize>
<@classPresentConditional value="org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint" negated=true>
<#assign redirectToLogin = false>
</@classPresentConditional>
<#if redirectToLogin>
<#assign returnUrl=request.requestURL/>
<#if request.queryString??>
<#assign returnUrl+="?"+request.queryString/>
</#if>
<meta http-equiv="refresh" content="0; url=<@url value="${ssoServerBase!}/login?targetUrl=${returnUrl?url}"/>" />
</#if>
</head>
<body>
<h3 style="text-align:center;">
<#if redirectToLogin>
<a href="<@url value="${ssoServerBase!}/login?targetUrl=${returnUrl?url}"/>">${action.getText('login.required')}</a>
<#else>
${action.getText('access.denied')}
</#if>
</h3>
</body>
</html>