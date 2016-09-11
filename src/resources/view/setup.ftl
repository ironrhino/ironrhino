<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('setup')}</title>
<meta name="body_class" content="welcome" />
</head>
<body>
<div class="row">
	<div class="span6 offset3">
	<h2 class="caption">${action.getText('setup')}</h2>
	<div class="hero-unit">
	<@s.form method="post" action="${actionBaseUrl}" class="ajax focus form-horizontal well">
		<#list setupParameters as p>
		<#assign defaultValue = p.defaultValue!/>
		<#if defaultValue?has_content>
		<#assign defaultValue><@defaultValue?interpret/></#assign>
		<#assign defaultValue=defaultValue?markup_string>
		</#if>
		<#if p.type=='enum'>
		<@s.select label="%{getText('${p.label?has_content?string(p.label,p.name)}')}" name=p.name value=defaultValue! class="${p.cssClass}" list="@${p.parameterType.name}@values()" listKey="name" listValue="displayName" dynamicAttributes=p.dynamicAttributes/>
		<#elseif p.type=='boolean'>
		<@s.checkbox label="%{getText('${p.label?has_content?string(p.label,p.name)}')}" name=p.name class="${p.cssClass} custom" dynamicAttributes=p.dynamicAttributes/>
		<#elseif p.type=='integer'>
		<@s.textfield label="%{getText('${p.label?has_content?string(p.label,p.name)}')}" type="number" name=p.name value=defaultValue! placeholder=action.getText(p.placeholder!) class="${p.cssClass} integer" dynamicAttributes=p.dynamicAttributes/>
		<#elseif p.type=='double'>
		<@s.textfield label="%{getText('${p.label?has_content?string(p.label,p.name)}')}" type="number" name=p.name value=defaultValue! placeholder=action.getText(p.placeholder!) class="${p.cssClass} double" dynamicAttributes=p.dynamicAttributes/>
		<#else>
		<@s.textfield label="%{getText('${p.label?has_content?string(p.label,p.name)}')}" name=p.name value=defaultValue! placeholder=action.getText(p.placeholder!) class="${p.cssClass}" dynamicAttributes=p.dynamicAttributes/>
		</#if>
		</#list>
		<@s.submit value="%{getText('confirm')}" class="btn-primary"/>
	</@s.form>
	</div>
	</div>
</div>
</body>
</html>