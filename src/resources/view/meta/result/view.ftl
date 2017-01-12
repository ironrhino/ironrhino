<#ftl output_format='HTML'>
<html>
<head>
<title>${getText('view')}${getText((richtableConfig.alias?has_content)?then(richtableConfig.alias!,entityName))}</title>
</head>
<body>
	<div id="${entityName}_view" class="view form-horizontal groupable"<#if richtableConfig.viewGridColumns gt 0> data-columns="${richtableConfig.viewGridColumns}"</#if>>
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
		<#if config.alias?has_content>
			<#assign label=config.alias>
		</#if>
		<#assign label=getText(label)>
		<#assign group=getText(config.group)>
		<#assign description=getText(config.description)>
		<#assign id=(config.id?has_content)?then(config.id,entityName+'-'+key)/>
		<#if config.type=='embedded'&&config.embeddedUiConfigs??>
				<#list config.embeddedUiConfigs.entrySet() as entry>
				<#assign config=entry.value>
				<#assign value=(entity[key][entry.key])!>
				<#assign hidden=config.hiddenInView.value>
				<#if !hidden && config.hiddenInView.expression?has_content>
					<#assign hidden=config.hiddenInView.expression?eval>
				</#if>
				<#if !hidden>
				<#assign label=entry.key>
				<#if config.alias?has_content>
					<#assign label=config.alias>
				</#if>
				<#assign label=getText(label)>
				<#assign description=getText(config.description)>
				<#assign id=(config.id?has_content)?then(config.id,entityName+'-'+key+'-'+entry.key)/>
				<@controlGroup id=id group=group/>
					<@controlLabel label=label description=description/>
					<div class="controls">
					<#assign template=config.template/>
					<#if config.viewTemplate!=''>
					<#assign template=config.viewTemplate/>
					</#if>
					<#if !template?has_content>
						<#assign cssClass=config.cssClass?replace('input-[^ ]+', '', 'r')>
						<#if config.type=='textarea'>
							<#if value?has_content>
							<#if cssClass?has_content&&cssClass?contains('htmlarea')>
							${value?no_esc}
							<#else>
							<p<#if cssClass?has_content> class="${cssClass}"</#if>>${value}</p>
							</#if>
							</#if>
						<#elseif config.type=='dictionary'>
							<#if displayDictionaryLabel??>
								<#assign templateName><@config.templateName?interpret /></#assign>
								<#assign templateName=templateName?markup_string/>
								<@displayDictionaryLabel dictionaryName=templateName value=value!/>
							<#else>
								${value!}
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
								${getText(value?string)}
							<#elseif value?is_unknown_date_like>
								${value?datetime}
							<#elseif ((value.class.simpleName)!)=='String'||value?is_number||value?is_date_like>
								${value?string}
							<#elseif value?is_enumerable>
								<ol class="unstyled">
								<#list value as item>
									<li>
									<#if item?is_boolean>
										${getText(item?string)}
									<#elseif item?is_unknown_date_like>
										${item?datetime}
									<#elseif ((item.class.simpleName)!)=='String'||item?is_number||item?is_date_like>
										${item?string}
									<#elseif item?is_enumerable>
											<ol class="unstyled">
											<#list item as it>
												<li>${it}</li>
											</#list>
											</ol>
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
		<#else>		
		<@controlGroup id=id group=group/>
			<@controlLabel label=label description=description/>
			<div class="controls">
			<#assign template=config.template/>
			<#if config.viewTemplate!=''>
			<#assign template=config.viewTemplate/>
			</#if>
			<#if !template?has_content>
				<#assign cssClass=config.cssClass?replace('input-[^ ]+', '', 'r')>
				<#if config.type=='textarea'>
					<#if value?has_content>
					<#if cssClass?has_content&&cssClass?contains('htmlarea')>
					${value?no_esc}
					<#else>
					<p<#if cssClass?has_content> class="${cssClass}"</#if>>${value}</p>
					</#if>
					</#if>
				<#elseif config.type=='dictionary'>
					<#if displayDictionaryLabel??>
						<#assign templateName><@config.templateName?interpret /></#assign>
						<#assign templateName=templateName?markup_string/>
						<@displayDictionaryLabel dictionaryName=templateName value=value!/>
					<#else>
						${value!}
					</#if>
				<#elseif config.type=='schema'>
					<#if printAttributes??>
						<@printAttributes attributes=entity.attributes grouping=true/>
					</#if>
				<#elseif config.type=='imageupload'>
					<#if value?has_content>
						<img src="${value}"/>
					</#if>
				<#elseif config.type=='collection'&&config.embeddedUiConfigs??>
					<#assign embeddedUiConfigs=config.embeddedUiConfigs/>
					<table class="table table-bordered table-fixed middle ${config.cssClass}">
					<thead>
						<tr>
							<#list embeddedUiConfigs.entrySet() as entry>
							<#assign config=entry.value>
							<#assign cssClass=config.cssClass?replace('input-[^ ]+', '', 'r')>
							<#assign hidden=config.hiddenInView.value>
							<#if !hidden && config.hiddenInView.expression?has_content>
								<#assign hidden=config.hiddenInView.expression?eval>
							</#if>
							<#if !hidden>
							<#assign label2=entry.key>
							<#if config.alias?has_content>
								<#assign label2=config.alias>
							</#if>
							<#assign label2=getText(label2)>
							<#assign description2=getText(config.description)>
							<th<#if entry.value.width?has_content> style="width:${entry.value.width};"</#if>>${label2}<#if description2?has_content> <span data-content="${description2}" class="poped glyphicon glyphicon-question-sign"></span></#if></th>
							</#if>
							</#list>
						</tr>
					</thead>
					<tbody>
					<#assign size=0>
					<#assign collections=entity[key]!>
					<#if collections?is_collection && collections?size gt 0>
					<#list collections as element>
						<#if element?has_content>
						<tr>
							<#list embeddedUiConfigs.entrySet() as entry>
							<#assign config = entry.value>
							<#assign cssClass=config.cssClass?replace('input-[^ ]+', '', 'r')>
							<#assign value=element[entry.key]!>
							<#assign hidden=config.hiddenInView.value>
							<#if !hidden && config.hiddenInView.expression?has_content>
								<#assign hidden=config.hiddenInView.expression?eval>
							</#if>
							<#if !hidden>
							<td>
							<#assign template=config.template/>
							<#if config.viewTemplate!=''>
							<#assign template=config.viewTemplate/>
							</#if>
							<#if !template?has_content>
								<#if config.type=='textarea'>
									<#if value?has_content>
									<p<#if cssClass?has_content> class="${cssClass}"</#if>>${value!}</p>
									</#if>
								<#elseif config.type=='dictionary'>
									<#if displayDictionaryLabel??>
										<#assign templateName><@config.templateName?interpret /></#assign>
										<#assign templateName=templateName?markup_string/>
										<@displayDictionaryLabel dictionaryName=templateName value=value!/>
									<#else>
										${value!}
									</#if>
								<#elseif config.type=='imageupload'>
									<#if value?has_content>
										<img src="${value}"/>
									</#if>
								<#elseif value??>
										<#if value?is_boolean>
											${getText(value?string)}
										<#elseif value?is_unknown_date_like>
											${value?datetime}
										<#elseif ((value.class.simpleName)!)=='String'||value?is_number||value?is_date_like>
											${value?string}
										<#elseif value?is_enumerable>
											<ol class="unstyled">
											<#list value as item>
												<li>
												<#if item?is_boolean>
													${getText(item?string)}
												<#elseif item?is_unknown_date_like>
													${item?datetime}
												<#elseif ((item.class.simpleName)!)=='String'||item?is_number||item?is_date_like>
													${item?string}
												<#elseif item?is_enumerable>
														<ol class="unstyled">
														<#list item as it>
															<li>${it}</li>
														</#list>
														</ol>
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
										<#else>
										${value?string!}
										</#if>
								</#if>
							<#else>
								<@template?interpret/>
							</#if>
							</td>
							</#if>
							</#list>
						</tr>
						</#if>
					</#list>
					</#if>
					</tbody>
					</table>
				<#elseif value??>
						<#if value?is_boolean>
							${getText(value?string)}
						<#elseif value?is_unknown_date_like>
							${value?datetime}
						<#elseif ((value.class.simpleName)!)=='String'||value?is_number||value?is_date_like>
							${value?string}
						<#elseif value?is_enumerable>
							<ol class="unstyled">
							<#list value as item>
								<li>
									<#if item?is_boolean>
										${getText(item?string)}
									<#elseif item?is_unknown_date_like>
										${item?datetime}
									<#elseif ((item.class.simpleName)!)=='String'||item?is_number||item?is_date_like>
										${item?string}
									<#elseif item?is_enumerable>
										<ol class="unstyled">
										<#list item as it>
											<li>${it}</li>
										</#list>
										</ol>
								<#elseif item?is_hash_ex>
										<#if item.isNew?? && item.getId??>
											${item}
										<#else>
										<ul class="unstyled">
										<#list item as k,v>
											<#if k!='class' && v?? && !v?is_method && item['set'+k?cap_first]??>
											<li><em>${getText(k)}:</em> ${v?string}</li>
											</#if>
										</#list>
										</#if>
										</ul>
								<#else>
										${item!}
								</#if>
								</li>
							</#list>
							</ol>
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
		</#if>
	</#list>
	<#if attachmentable && entity.attachments?has_content>
	<#list entity.attachments as attachment>
		<#assign filename=attachment?keep_after_last('/') />
		<#assign url=attachment?keep_before_last('/')+'/'+filename?url />
		<#assign att=attachment?lower_case />
		<a href="${url}" target="_blank">
		<#if att?ends_with('jpg')||att?ends_with('png')||att?ends_with('gif')||att?ends_with('bmp')||att?ends_with('webp')>
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