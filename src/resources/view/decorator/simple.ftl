<#ftl output_format='HTML'>
<#assign requestURI=request.requestURI[request.contextPath?length..]/>
<#compress>
<!DOCTYPE html>
<html>
<head>
<title>${title?no_esc}</title>
<meta charset="utf-8">
<#if request.contextPath!=''>
<meta name="context_path" content="${request.contextPath}" />
</#if>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="shortcut icon" href="<@url value="/assets/images/favicon.ico"/>" />
<link href="<@url value="/assets/styles/ironrhino${devMode?then('','-min')}.css"/>" media="all" rel="stylesheet" type="text/css" />
<script src="<@url value="/assets/scripts/ironrhino${devMode?then('','-min')}.js"/>" type="text/javascript"<#if !head?contains('</script>')> defer</#if>></script>
<#include "include/assets.ftl" ignore_missing=true/>
${head?no_esc}
</head>
<body lang="${.lang}" class="simple">
<div id="content" class="simple">
<#if action.hasActionMessages() || action.hasActionErrors()>
<div id="message">
<@s.actionerror />
<@s.actionmessage />
</div>
</#if>
${body?no_esc}
</div>
</body>
</html></#compress>