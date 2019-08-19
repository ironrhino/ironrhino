<#ftl output_format='HTML'>
<#macro renderControlGroup key config nestedKey='' nestedConfig={}>
	<#local group=getText(config.group)>
	<#if nestedKey==''>
		<#local id=(config.id?has_content)?then(config.id,entityName+'-'+key)/>
		<#local label=(config.alias?has_content)?then(config.alias,key)/><#local label=getText(label)>
		<#local name=entityName+'.'+key>
		<#local value=(entity[key])!>
	<#else>
		<#local config=nestedConfig>
		<#local id=(config.id?has_content)?then(config.id,entityName+'-'+key+'-'+nestedKey)/>
		<#local label=(config.alias?has_content)?then(config.alias,nestedKey)/><#local label=getText(label)>
		<#local name=entityName+'.'+key+'.'+nestedKey>
		<#local value=(entity[key][nestedKey])!>
	</#if>
	<#local hidden=config.hiddenInView.value>
	<#if !hidden && config.hiddenInView.expression?has_content>
		<#local hidden=config.hiddenInView.expression?eval/>
	</#if>
	<#if hidden><#return></#if>
	<#local description=getText(config.description)>
	<#if config.type=='embedded'&&config.embeddedUiConfigs??&&nestedKey==''>
		<#list config.embeddedUiConfigs as nestedKey,nestedConfig>
		<@renderControlGroup key=key config=config nestedKey=nestedKey nestedConfig=nestedConfig/>
		</#list>
	<#else>
	<@controlGroup id=id group=group label=label description=description>
	<#if config.type=='collection'&&config.embeddedUiConfigs??>
		<table class="table table-bordered table-fixed middle ${config.cssClass}">
		<thead>
			<tr>
				<#list config.embeddedUiConfigs as nestedKey,config>
				<#local hidden=config.hiddenInView.value>
				<#if !hidden && config.hiddenInView.expression?has_content>
					<#local hidden=config.hiddenInView.expression?eval>
				</#if>
				<#if !hidden>
				<#local label=(config.alias?has_content)?then(config.alias,nestedKey)><#local label=getText(label)>
				<#local description=getText(config.description)>
				<th<#if config.width?has_content> style="width:${config.width};"</#if>>${label}<#if description?has_content> <span data-content="${description}" class="poped glyphicon glyphicon-question-sign"></span></#if></th>
				</#if>
				</#list>
			</tr>
		</thead>
		<tbody>
		<#if value?has_content>
		<#list value as element>
			<#if element??>
			<tr>
				<#list config.embeddedUiConfigs as nestedKey,config>
				<#local value=element[nestedKey]!>
				<#local hidden=config.hiddenInView.value>
				<#if !hidden && config.hiddenInView.expression?has_content>
					<#local hidden=config.hiddenInView.expression?eval>
				</#if>
				<#if !hidden>
				<td>
					<@renderValue value=value config=config/>
				</td>
				</#if>
				</#list>
			</tr>
			</#if>
		</#list>
		</#if>
		</tbody>
		</table>
	<#else>
		<@renderValue value=value config=config/>
	</#if>
	</@controlGroup>
	</#if>
</#macro>
<#macro renderValue value config>
	<#local template=config.viewTemplate/>
	<#if !template?has_content>
		<#if config.type=='textarea'>
			<#local cssClass=config.cssClass?replace('input-[^ ]+', '', 'r')>
			<#if value?has_content>
				<#if cssClass?has_content&&cssClass?contains('htmlarea')>
				${value?no_esc}
				<#else>
				<p<#if cssClass?has_content> class="${cssClass}"</#if>>${value}</p>
				</#if>
			</#if>
		<#elseif config.type=='dictionary'>
			<#if displayDictionaryLabel??>
				<#local templateName><@config.templateName?interpret /></#local><#local templateName=templateName?markup_string/>
				<@displayDictionaryLabel dictionaryName=templateName value=value!/>
			<#else>
				${value!}
			</#if>
		<#elseif config.type=='schema'>
			<#if printAttributes??&&entity.attributes??><@printAttributes attributes=entity.attributes grouping=true/></#if>
		<#elseif config.type=='attributes'>
			<div>
			<#if value?has_content>
			<#list value as var>
			<span class="label tiped" title="${var.name}">${var.value}</span>
			</#list>
			</#if>
			</div>
		<#elseif config.type=='imageupload'>
			<#if value?has_content><img src="${value}"/></#if>
		<#elseif value??>
			<#if value?is_enumerable>
			<ol class="unstyled">
			<#list value as item>
				<li>
				<#if item?is_enumerable>
						<ol class="unstyled">
						<#list item as it>
							<li>${it}</li>
						</#list>
						</ol>
				<#elseif item?is_boolean>
					${getText(item?c)}
				<#elseif item?is_unknown_date_like>
					${item?datetime}	
				<#elseif item?is_string||item?is_number||item?is_date_like>
					${item?string}
				<#elseif item?is_hash_ex>
						<#if item.isNew?? && item.getId??>
							${item}
						<#else>
						<ul class="unstyled">
						<#list item as k,v>
							<#if k!='class' && v?? && !v?is_method>
							<li><em>${k}:</em> ${v?string}</li>
							</#if>
						</#list>
						</ul>
						</#if>
				<#else>
						${item!}
				</#if>
				</li>
			</#list>
			</ol>
			<#elseif value?is_boolean>
			${getText(value?c)}
			<#elseif value?is_unknown_date_like>
			${value?datetime}
			<#else>
			${value?string!}
			</#if>
		</#if>
	<#else>
		<@template?interpret/>
	</#if>
</#macro>
<html>
<head>
<title>${getText('view')}${getText((richtableConfig.alias?has_content)?then(richtableConfig.alias!,entityName))}</title>
</head>
<body>
	<div id="${entityName}_view" class="view form-horizontal groupable"<#if richtableConfig.viewGridColumns gt 0> data-columns="${richtableConfig.viewGridColumns}"</#if>>
	<#list uiConfigs as key,config>
		<@renderControlGroup key=key config=config/>
	</#list>
	<#if attachmentable && entity.attachments?has_content>
	<#list entity.attachments as attachment>
		<#assign filename=attachment?keep_after_last('/') />
		<#assign url=attachment?keep_before_last('/')+'/'+filename?url />
		<#assign att=attachment?lower_case />
		<a href="${url}" target="_blank">
		<#if ['jpg','gif','png','webp','bmp']?seq_contains(att?keep_after_last('.')?lower_case)>
		<img style="display:block;" src="${url}"/>
		<#else>
		<div>${filename}</div>
		</#if>
		</a>
	</#list>
	</#if>
	<#if richtableConfig.exportable>
	<div class="form-actions">
		<a href="${actionBaseUrl}/export/${entity.id}" class="btn">${getText('export')}</a>
	</div>
	</#if>
	</div>
</body>
</html>