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
	<#local group=getText(config.group)>
	<#local hidden=config.hiddenInInput.value>
	<#if !hidden && config.hiddenInInput.expression?has_content>
		<#if config.hiddenInInput.expression?eval>
		<#local hidden=true>
		<div id="control-group-${id}" class="control-group"<#if group?has_content> data-group="${group}"</#if>></div>
		</#if>
	</#if>
	<#if hidden><#return></#if>
	<#local description=getText(config.description)>
	<#local readonly=naturalIds?keys?seq_contains(key)&&!naturalIdMutable&&!isnew||config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
	<#if !isnew&&idAssigned&&key=='id'><#local readonly=true/></#if>
	<#if !(entity.new&&readonly)>
		<#if (Parameters[key]?has_content||Parameters[key+'.id']?has_content)><#local readonly=true/></#if>
		<#local templateName><@config.templateName?interpret/></#local><#local templateName=templateName?markup_string/>
		<#local pickUrl><@config.pickUrl?interpret/></#local><#local pickUrl=pickUrl?markup_string/>
		<#local dynamicAttributes=mergeDynAttrs(config)/>
		<#if config.inputTemplate?has_content>
			<#if config.inputTemplate?index_of('<div class="control-group') gt -1>
			<@config.inputTemplate?interpret/>
			<#else>
			<@controlGroup id=id group=group label=label description=description>
				<@config.inputTemplate?interpret/>
			</@controlGroup>
			</#if>
		<#elseif config.type=='textarea'>
			<@s.textarea id=id label=label name=name class=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='checkbox'>
			<#if readonly><@s.hidden name=name/></#if>
			<@s.checkbox disabled=readonly id=id label=label name=name class=config.cssClass dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='enum'>
			<#if !config.multiple||config.cssClasses?seq_contains('chosen')>
				<#if readonly><@s.hidden name=name value="${(value.name())!}"/></#if>
				<@s.select disabled=readonly multiple=config.multiple id=id label=label name=name class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
			<#else>
				<#if readonly><#if value?has_content><#list value as en><@s.hidden id="" name=name value=en.name()/></#list></#if></#if>
				<@s.checkboxlist disabled=readonly id=id label=label name=name class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue dynamicAttributes=dynamicAttributes/>
			</#if>
		<#elseif config.type=='select'>
			<#if readonly><@s.hidden name=name/></#if>
			<@s.select disabled=readonly id=id label=label name=name class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='multiselect'>
			<#if readonly><@s.hidden name=name/></#if>
			<@s.select disabled=readonly id=id label=label name=name class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='dictionary'>
			<@controlGroup id=id group=group label=label description=description for=id>
				<#if !config.multiple||config.cssClasses?seq_contains('chosen')>
					<#if readonly><@s.hidden name=name/></#if>
					<@selectDictionary disabled=readonly id=id dictionaryName=templateName name=name required=config.required multiple=config.multiple class=config.cssClass dynamicAttributes=dynamicAttributes/>
				<#else>
					<#if readonly><#if value?has_content><#list value as en><@s.hidden id="" name=name value=en.name()/></#list></#if></#if>
					<@checkDictionary disabled=readonly id=id dictionaryName=templateName name=name class=config.cssClass dynamicAttributes=dynamicAttributes/>
				</#if>
			</@controlGroup>
		<#elseif config.type=='treeselect'&&!config.multiple>
			<@s.textfield id=id label=label name=name class="treeselect-inline "+config.cssClass readonly=readonly data\-url=pickUrl data\-text=(value??&&value.fullname??)?then(value.fullname,value!) dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='listpick'||config.type=='treeselect'>
			<div id="control-group-${id}" class="control-group"<#if group?has_content> data-group="${group}"</#if>>
				<@controlLabel label=label description=description for=id/>
				<div class="controls <#if readonly>readonly</#if> ${config.type}" data-options="{'url':'<@url value=pickUrl/>'<#if config.multiple>,'multiple':true</#if>}">
				<#if config.multiple>
					<#local arr=[]><#if value?has_content&&value?is_enumerable><#list value as v><#local arr+=[config.reference?then(v.id!,v?string)]></#list></#if>
					<@s.hidden id=id name=name value=arr?join(',') class=config.type+"-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
				<#else>
					<@s.hidden id=id name=name class=config.type+"-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
				</#if>
				<div class="${config.type}-name input-pseudo"><#if config.multiple&&config.template?has_content><@config.template?interpret/><#else><#if value?has_content><#if value.fullname??>${value.fullname!}<#else>${value!}</#if></#if></#if></div>
				</div>
			</div>
		<#elseif config.type=='attributes'>
			<@controlGroup id=id group=group label=label description=description>
			<input type="hidden" name="__datagrid_${entityName}.attributes"/>
			<table class="datagrid adaptive table table-condensed nullable">
				<thead>
					<tr>
						<td>${getText('name')}</td>
						<td>${getText('value')}</td>
						<td class="manipulate"></td>
					</tr>
				</thead>
				<tbody>
					<#local size=0>
					<#if entity.attributes??&&entity.attributes?size gt 0><#local size=entity.attributes?size-1></#if>
					<#list 0..size as index>
					<tr>
						<td><@s.textfield theme="simple" name="${entityName}.attributes[${index}].name"/></td>
						<td><@s.textfield theme="simple" name="${entityName}.attributes[${index}].value"/></td>
						<td class="manipulate"></td>
					</tr>
					</#list>
				</tbody>
			</table>
			</@controlGroup>	
		<#elseif config.type=='schema'>
			<#if editAttributes??>
				<div id="editAttributes"<#if group?has_content> data-group="${group}"</#if>>
				<@editAttributes schemaName=templateName attributes=entity.attributes parameterNamePrefix=entityName+'.'/>
				</div>
			</#if>
		<#elseif config.type=='imageupload'>
			<#if !readonly>
				<@controlGroup id=id group=group label=label description=description for="${id}-upload-button">
					<@s.hidden id=id name=name class=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?then(config.maxlength,'') dynamicAttributes=dynamicAttributes/>
					<div style="margin-bottom:5px;">
					<button id="${id}-upload-button" class="btn concatimage" type="button" data-target="${id}-image" data-field="${id}" data-maximum="1">${getText('upload')}</button>
					<#if config.cssClasses?seq_contains('concatsnapshot')>
					<button class="btn concatsnapshot" type="button" data-target="${id}-image" data-field="${id}" data-maximum="1">${getText('snapshot')}</button>
					</#if>
					</div>
					<div id="${id}-image" style="text-align:center;min-height:100px;border:1px solid #ccc;">
						<#if value?has_content><img src="${value}" title="${getText('drag.image.file')}"/><#else>${getText('drag.image.file')}</#if>
					</div>
				</@controlGroup>
			<#else>
				<@controlGroup id=id group=group label=label description=description>
					<@s.hidden id=id name=name class=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?then(config.maxlength,'') dynamicAttributes=dynamicAttributes/>
					<span>
					<#if value?has_content><img src="${value}" title="${getText('drag.image.file')}"/></#if>
					</span>
				</@controlGroup>
			</#if>
		<#elseif config.type=='embedded'&&config.embeddedUiConfigs??&&nestedKey==''>
			<#list config.embeddedUiConfigs as nestedKey,nestedConfig>
				<@renderControlGroup key=key config=config nestedKey=nestedKey nestedConfig=nestedConfig/>
			</#list>
		<#elseif config.type=='collection'&&config.embeddedUiConfigs??>
			<@controlGroup id=id group=group label=label description=description>
				<input type="hidden" name="__datagrid_${entityName}.${key}"/>
				<table class="table table-bordered table-fixed middle datagrid adaptive ${config.cssClass}">
				<thead>
					<tr>
						<#list config.embeddedUiConfigs as nestedKey,config>
						<#local hidden=config.hiddenInInput.value>
						<#if !hidden&&config.hiddenInInput.expression?has_content><#local hidden=config.hiddenInInput.expression?eval></#if>
						<#if !hidden>
						<#local label=(config.alias?has_content)?then(config.alias,nestedKey)><#local label=getText(label)>
						<#local description=getText(config.description)>
						<th<#if config.width?has_content> style="width:${config.width};"</#if>>${label}<#if description?has_content> <span data-content="${description}" class="poped glyphicon glyphicon-question-sign"></span></#if></th>
						</#if>
						</#list>
						<th class="manipulate"></th>
					</tr>
				</thead>
				<tbody>
				<#list 0..((value?is_collection&&value?size gt 0)?then(value?size-1,0)) as index>
					<tr>
						<#list config.embeddedUiConfigs as nestedKey,config>
						<#local hidden=config.hiddenInInput.value>
						<#if !hidden&&config.hiddenInInput.expression?has_content><#local hidden=config.hiddenInInput.expression?eval></#if>
						<#if !hidden>
						<td>
						<#local name=entityName+'.'+key+'['+index+'].'+nestedKey>
						<#local value=(entity[key][index][nestedKey])!>
						<#local readonly=config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
						<#local templateName><@config.templateName?interpret/></#local><#local templateName=templateName?markup_string/>
						<#local pickUrl><@config.pickUrl?interpret/></#local><#local pickUrl=pickUrl?markup_string/>
						<#local dynamicAttributes=mergeDynAttrs(config)/>
						<#if config.inputTemplate?has_content>
							<@config.inputTemplate?interpret/>
						<#elseif config.type=='textarea'>
							<@s.textarea readonly=readonly id="" theme="simple" name=name class=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
						<#elseif config.type=='checkbox'>
							<@s.checkbox readonly=readonly id="" theme="simple" name=name class=config.cssClass dynamicAttributes=dynamicAttributes/>
						<#elseif config.type=='enum'>
							<#if !config.multiple||config.cssClasses?seq_contains('chosen')>
								<#if readonly><@s.hidden name=name value="${(value.name())!}"/></#if>
								<@s.select disabled=readonly multiple=config.multiple id="" theme="simple" name=name class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
							<#else>
								<#if readonly><#if value?has_content><#list value as en><@s.hidden id="" name=name value=en.name()/></#list></#if></#if>
								<@s.checkboxlist disabled=readonly id="" theme="simple" name=name class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue dynamicAttributes=dynamicAttributes/>
							</#if>
						<#elseif config.type=='select'>
							<#if readonly><@s.hidden name=name/></#if>
							<@s.select disabled=readonly id="" theme="simple" name=name class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
						<#elseif config.type=='multiselect'>
							<#if readonly><@s.hidden name=name/></#if>
							<@s.select disabled=readonly id="" theme="simple" name=name class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
						<#elseif config.type=='dictionary'>
							<#if !config.multiple||config.cssClasses?seq_contains('chosen')>
								<#if readonly><@s.hidden name=name/></#if>
								<@selectDictionary disabled=readonly id="" dictionaryName=templateName name=name required=config.required multiple=config.multiple class=config.cssClass dynamicAttributes=dynamicAttributes/>
							<#else>
								<#if readonly><#if value?has_content><#list value as en><@s.hidden id="" name=name value=en.name()/></#list></#if></#if>
								<@checkDictionary disabled=readonly id="" dictionaryName=templateName name=name required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
							</#if>
						<#elseif config.type=='treeselect'&&!config.multiple>
							<@s.textfield id="" theme="simple" name=name class="treeselect-inline "+config.cssClass readonly=readonly data\-url=pickUrl data\-text=(value??&&value.fullname??)?then(value.fullname,value!) dynamicAttributes=dynamicAttributes/>
						<#elseif config.type=='listpick'||config.type=='treeselect'>
							<div class="<#if readonly>readonly</#if> ${config.type}" data-options="{'url':'<@url value=pickUrl/>'<#if config.multiple>,'multiple':true</#if>}">
							<#if config.multiple>
								<#local arr=[]><#if value?has_content&&value?is_enumerable><#list value as v><#local arr+=[config.reference?then(v.id!,v?string)]></#list></#if>
								<@s.hidden name=name value=arr?join(',') class=config.type+"-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
							<#else>
								<@s.hidden name=name class=config.type+"-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
							</#if>
							<div class="${config.type}-name input-pseudo"><#if config.multiple&&config.template?has_content><@config.template?interpret/><#else><#if value?has_content><#if value.fullname??>${value.fullname!}<#else>${value!}</#if><#else><i class="glyphicon glyphicon-list"></i></#if></#if></div>
							</div>
						<#else>
							<#if value?is_date_like>
								<#if config.cssClass?contains('datetime')><#local value=value?datetime/><#elseif config.cssClass?contains('time')><#local value=value?time/><#else><#local value=value?date/></#if>
								<@s.textfield id="" theme="simple" name=name type=config.inputType value=value?string class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes/>
							<#else>
								<@s.textfield id="" theme="simple" name=name type=config.inputType class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes/>
							</#if>
						</#if>
						</td>
						</#if>
						</#list>
						<td class="manipulate"></td>
					</tr>
				</#list>
				</tbody>
				</table>
			</@controlGroup>	
		<#else>
			<#if value?is_date_like>
				<#if config.cssClass?contains('datetime')><#local value=value?datetime/><#elseif config.cssClass?contains('time')><#local value=value?time/><#else><#local value=value?date/></#if>
				<@s.textfield id=id label=label name=name type=config.inputType value=value?string class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes/>
			<#else>
				<@s.textfield id=id label=label name=name type=config.inputType class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes/>
			</#if>
		</#if>
	</#if>
</#macro>
<!DOCTYPE html>
<html>
<head>
<title><#if !entity??><#assign entity=entityName?eval></#if><#assign isnew=!entity??||entity.new/><#assign isnew=!entity??||entity.new/><#if idAssigned><#assign isnew=!entity??||!entity.id?has_content/></#if><#if isnew>${getText('create')}<#else>${getText('edit')}</#if>${getText((richtableConfig.alias?has_content)?then(richtableConfig.alias,entityName))}</title>
</head>
<body>
<#assign formDynamicAttributes={}/>
<#if attachmentable>
<#assign formDynamicAttributes+={'data-attachments':entity.attachments!}/>
</#if>
<#if richtableConfig.inputGridColumns gt 0>
<#assign formDynamicAttributes+={'data-columns':richtableConfig.inputGridColumns}/>
</#if>
<@s.form id="${entityName}_input" action="${actionBaseUrl}/save" method="post" class="ajax form-horizontal${richtableConfig.importable?then(' importable','')}${attachmentable?then(' attachmentable','')}${doubleCheck?then(' double-check','')} groupable ${richtableConfig.inputFormCssClass}" autocomplete="off" dynamicAttributes=formDynamicAttributes>
	<#if !isnew>
		<#if !idAssigned><@s.hidden name="${entityName}.id" class="id"/></#if>
	<#else>
		<#if idAssigned><input type="hidden" name="_isnew" class="disabled-on-success" value="true"/></#if>
		<#if treeable!false><@s.hidden name="parent"/></#if>
	</#if>
	<#if versionPropertyName??><@s.hidden name=entityName+'.'+versionPropertyName class="version"/></#if>
	<#list uiConfigs as key,config>
		<@renderControlGroup key=key config=config/>
	</#list>
	<@s.submit label=getText('save') class="btn-primary"/>
</@s.form>
</body>
</html>