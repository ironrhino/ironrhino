<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('view')}${action.getText(entityName)}</title>
</head>
<body>
	<div class="form-horizontal">
	<#list uiConfigs.entrySet() as entry>
		<#assign key=entry.key>
		<#assign config=entry.value>
		<#assign value=entity[key]!>
		<#assign hidden=config.hiddenInView.value>
		<#if !hidden && config.hiddenInView.expression?has_content>
			<#assign hidden=config.hiddenInView.expression?eval>
		</#if>
		<#if !hidden>
		<#assign label=key>
		<#if config.alias??>
			<#assign label=config.alias>
		</#if>
		<div class="control-group">
			<label class="control-label">${action.getText(label)}</label>
			<div class="controls">
			<#assign template=config.template/>
			<#if config.viewTemplate!=''>
			<#assign template=config.viewTemplate/>
			</#if>
			<#if !template?has_content>
				<#if config.type=='textarea'>
					<#if value?has_content>
					<div style="white-space:pre-wrap;word-break:break-all;"<#if config.cssClass?has_content> class="${config.cssClass}"</#if>>${value!}</div>
					</#if>
				<#elseif config.type=='dictionary'>
					<#if displayDictionaryLabel??>
						<#assign templateName><@config.templateName?interpret /></#assign>
						<@displayDictionaryLabel dictionaryName=templateName value=value!/>
					</#if>
				<#elseif config.type=='schema'>
					<#if printAttributes??>
						<@printAttributes attributes=entity.attributes grouping=true/>
					</#if>
				<#elseif config.type=='imageupload'>
					<#if value?has_content>
						<img src="${value}"/>
					</#if>
				<#elseif value??>
						<#if value?is_boolean>
							${action.getText(value?string)}
						<#elseif value?is_sequence>
							<ol class="unstyled">
							<#list value as item>
								<li>
								<#if item?is_sequence>
										<ol class="unstyled" style="padding-bottom:10px;">
										<#list item as it>
											<li>${it}</li>
										</#list>
										</ol>
								<#elseif item?is_hash_ex>
										<ul class="unstyled" style="padding-bottom:10px;">
										<#list item?keys as k>
											<#if k!='class' && item[k]?? && !item[k]?is_method>
											<li>${k}: ${item[k]?string}</li>
											</#if>
										</#list>
										</ul>
								<#else>
										${item!}
								</#if>
								</li>
							</#list>
							</ol>
						<#elseif value?is_hash_ex && value.displayName??>
								${value.displayName!}
						<#else>
						${value?string!}
						</#if>
				</#if>
			<#else>
				<@template?interpret/>
			</#if>
			</div>
		</div>
		</#if>
	</#list>
	<#if richtableConfig.exportable>
	<div class="form-actions">
		<a href="${actionBaseUrl}/export/${entity.id}" class="btn" download="${entity.id}.json">${action.getText('export')}</a>
	</div>
	</#if>
	</div>
</body>
</html></#escape>