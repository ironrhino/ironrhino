<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText((richtableConfig.alias?has_content)?string(richtableConfig.alias!,entityName))}${action.getText('list')}</title>
</head>
<body>
<#if Parameters.tab?has_content>
		<#if uiConfigs[Parameters.tab]??>
		<#assign config=uiConfigs[Parameters.tab]>
		<#if config.type=='checkbox'||config.type=='enum'||selectDictionary??&&config.type=='dictionary'>
		<#assign propertyName=Parameters.tab>
		</#if>
		</#if>
<#else>
<#list uiConfigs as key,value>
	<#if (value.type=='enum'||selectDictionary??&&value.type=='dictionary')&&!value.multiple>
		<#assign propertyName=key>
		<#assign config=value>
		<#break/>
	</#if>
</#list>
</#if>
<#if propertyName??>
<#assign dataurl=actionBaseUrl/>
<#if request.queryString??>
<#assign dataurl+='?'+request.queryString>
</#if>
<ul class="nav nav-tabs">
	<li><strong style="display:block;padding-top: 8px;padding-bottom: 8px;line-height: 20px;padding-right: 12px;padding-left: 12px;margin-right: 2px;">${action.getText(propertyName)}:</strong></li>
	<li class="active"><a href="#all" data-toggle="tab">${action.getText('all')}</a></li>
	<#if config.type=='enum'>
	<#assign values=statics[config.propertyType.name].values()>
	<#list values as value>
	<li><a href="#${propertyName+'-'+value.name()}" data-toggle="tab">${value}</a></li>
	</#list>
	<#elseif selectDictionary??&&config.type=='dictionary'>
	<#assign map=beans['dictionaryControl'].getItemsAsMap(config.templateName!propertyName)>
	<#list map as key,value>
	<li><a href="#${propertyName+'-'+key}" data-toggle="tab">${value}</a></li>
	</#list>
	<#elseif config.type=='checkbox'>
	<li><a href="#${propertyName}-true" data-toggle="tab">${action.getText('true')}</a></li>
	<li><a href="#${propertyName}-false" data-toggle="tab">${action.getText('false')}</a></li>
	</#if>
</ul>
<div class="tab-content">
	<div id="all" class="tab-pane ajaxpanel active" data-url="${dataurl}"></div>
	<#if config.type=='enum'>
	<#list values as value>
	<div id="${propertyName+'-'+value.name()}" class="tab-pane ajaxpanel manual" data-url="${dataurl+dataurl?contains('?')?then('&','?')}tab=${propertyName}&${propertyName}=${value.name()}"></div>
	</#list>
	<#elseif selectDictionary??&&config.type=='dictionary'>
	<#assign map=beans['dictionaryControl'].getItemsAsMap(config.templateName!propertyName)>
	<#list map as key,value>
	<div id="${propertyName+'-'+key}" class="tab-pane ajaxpanel manual" data-url="${dataurl+dataurl?contains('?')?then('&','?')}tab=${propertyName}&${propertyName}=${key?url}"></div>
	</#list>
	<#elseif config.type=='checkbox'>
	<div id="${propertyName}-true" class="tab-pane ajaxpanel manual" data-url="${dataurl+dataurl?contains('?')?then('&','?')}tab=${propertyName}&${propertyName}=true"></div>
	<div id="${propertyName}-false" class="tab-pane ajaxpanel manual" data-url="${dataurl+dataurl?contains('?')?then('&','?')}tab=${propertyName}&${propertyName}=false"></div>
	</#if>
</div>
</#if>
</body>
</html>