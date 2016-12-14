<#ftl output_format='HTML'>
<#function mergeDynAttrs config>
	<#local dynamicAttributes={}/>
	<#if config.internalDynamicAttributes?has_content>
		<#local dynamicAttributes+=config.internalDynamicAttributes/>
	</#if>
	<#if config.dynamicAttributes?has_content>
		<#local da><@config.dynamicAttributes?interpret/></#local>
		<#local da=da?markup_string?eval>
		<#local dynamicAttributes+=da/>
	</#if>
	<#return dynamicAttributes>
</#function>
<#macro controlGroup id="" group="">
<div<#if id?has_content> id="control-group-${id}"</#if> class="control-group"<#if group?has_content> data-group="${group}"</#if>>
</#macro>
<#macro controlLabel label description for="">
<label class="control-label"<#if for?has_content> for="${for}"</#if>><#if description?has_content><span data-content="${description}" class="poped glyphicon glyphicon-question-sign"></span> </#if>${label}</label>
</#macro>
<!DOCTYPE html>
<html>
<head>
<title><#if !entity??><#assign entity=entityName?eval></#if><#assign isnew = !entity??||entity.new/><#assign isnew = !entity??||entity.new/><#if idAssigned><#assign isnew=!entity??||!entity.id?has_content/></#if><#if isnew>${getText('create')}<#else>${getText('edit')}</#if>${getText((richtableConfig.alias?has_content)?then(richtableConfig.alias,entityName))}</title>
</head>
<body>
<#assign formDynamicAttributes={}/>
<#if attachmentable>
<#assign formDynamicAttributes+={'data-attachments':entity.attachments!}/>
</#if>
<#if richtableConfig.inputGridColumns gt 0>
<#assign formDynamicAttributes+={'data-columns':richtableConfig.inputGridColumns}/>
</#if>
<@s.form id="${entityName}_input" action="${actionBaseUrl}/save" method="post" class="ajax form-horizontal${richtableConfig.importable?then(' importable','')}${attachmentable?then(' attachmentable','')} groupable ${richtableConfig.inputFormCssClass}" dynamicAttributes=formDynamicAttributes>
	<#if !isnew>
	<#if !idAssigned>
	<@s.hidden name="${entityName}.id" />
	</#if>
	<#else>
	<#if idAssigned>
	<input type="hidden" name="_isnew" value="true" />
	</#if>
	<#if treeable??&&treeable>
	<@s.hidden name="parent"/>
	</#if>
	</#if>
	<#if versionPropertyName??>
	<@s.hidden name="${entityName+'.'+versionPropertyName}" class="version" />
	</#if>
	<#list uiConfigs.entrySet() as entry>
		<#assign key=entry.key>
		<#assign config=entry.value>
		<#assign templateName><@config.templateName?interpret/></#assign>
		<#assign templateName=templateName?markup_string/>
		<#assign pickUrl><@config.pickUrl?interpret/></#assign>
		<#assign pickUrl=pickUrl?markup_string/>
		<#assign value=entity[key]!>
		<#assign hidden=config.hiddenInInput.value>
		<#if !hidden && config.hiddenInInput.expression?has_content>
			<#assign hidden=config.hiddenInInput.expression?eval>
		</#if>
		<#if !hidden>
		<#assign label=key>
		<#if config.alias?has_content>
			<#assign label=config.alias>
		</#if>
		<#assign label=getText(label)>
		<#assign group=getText(config.group)>
		<#assign description=getText(config.description)>
		<#assign readonly=naturalIds?keys?seq_contains(key)&&!naturalIdMutable&&!isnew||config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
		<#if !isnew&&idAssigned&&key=='id'>
		<#assign readonly=true/>
		</#if>
		<#if !(entity.new && readonly)>
			<#if (Parameters[key]?has_content||Parameters[key+'.id']?has_content)>
				<#assign readonly=true/>
			</#if>
			<#assign id=(config.id?has_content)?then(config.id,entityName+'-'+key)/>
			<#assign dynamicAttributes=mergeDynAttrs(config)/>
			<#if config.inputTemplate?has_content>
				<#if config.inputTemplate?index_of('<div class="control-group') gt -1>
				<@config.inputTemplate?interpret/>
				<#else>
				<@controlGroup id=id group=group/>
					<@controlLabel label=label description=description/>
					<div class="controls">
					<@config.inputTemplate?interpret/>
					</div>
				</div>
				</#if>
			<#elseif config.type=='textarea'>
				<@s.textarea id=id label=label name=entityName+"."+key class=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
			<#elseif config.type=='checkbox'>
				<#if readonly>
					<@s.hidden name=entityName+"."+key/>
				</#if>
				<@s.checkbox disabled=readonly id=id label=label name=entityName+"."+key class=config.cssClass+config.cssClass?has_content?then(' ','')+"custom" dynamicAttributes=dynamicAttributes />
			<#elseif config.type=='enum'>
				<#if !config.multiple>
				<#if readonly>
					<@s.hidden name=entityName+"."+key value="${(entity[key].name())!}"/>
				</#if>
				<@s.select disabled=readonly id=id label=label name=entityName+"."+key class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
				<#else>
				<#if readonly>
				<#if entity[key]?has_content>
				<#list entity[key] as en>
				<@s.hidden id="" name=entityName+"."+key value=en.name()/>
				</#list>
				</#if>
				</#if>
				<@s.checkboxlist disabled=readonly id=id label=label name=entityName+"."+key class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
				</#if>
			<#elseif config.type=='select'>
				<#if readonly>
					<@s.hidden name=entityName+"."+key />
				</#if>
				<@s.select disabled=readonly id=id label=label name=entityName+"."+key class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
			<#elseif config.type=='multiselect'>
				<#if readonly>
					<@s.hidden name=entityName+"."+key />
				</#if>
				<@s.select disabled=readonly id=id label=label name=entityName+"."+key class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
			<#elseif config.type=='listpick'>
				<div id="control-group-${id}" class="control-group <#if readonly>_</#if>listpick" data-options="{'url':'<@url value=pickUrl/>'<#if config.pickMultiple>,'multiple':true</#if>}"<#if group?has_content> data-group="${group}"</#if>>
					<#assign _name=entityName+"."+key+"${config.reference?then('.id','')}">
					<#if config.collectionType??><#assign _value=(_name?eval?join(','))!></#if>
					<@s.hidden id=id name=_name value=_value class="listpick-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
					<@controlLabel label=label description=description/>
					<div class="controls<#if readonly> text</#if>">
					<span class="listpick-name"><#if config.pickMultiple&&config.template?has_content><@config.template?interpret/><#else><#if entity[key]??><#if entity[key].fullname??>${entity[key].fullname!}<#else>${entity[key]!}</#if></#if></#if></span>
					</div>
				</div>
			<#elseif config.type=='treeselect'>
				<div id="control-group-${id}" class="control-group <#if readonly>_</#if>treeselect" data-options="{'url':'<@url value=pickUrl/>','cache':false<#if config.pickMultiple>,'multiple':true</#if>}"<#if group?has_content> data-group="${group}"</#if>>
					<#assign _name=entityName+"."+key+"${config.reference?then('.id','')}">
					<#if config.collectionType??><#assign _value=(_name?eval?join(','))!></#if>
					<@s.hidden id=id name=_name value=_value class="treeselect-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
					<@controlLabel label=label description=description/>
					<div class="controls<#if readonly> text</#if>">
					<span class="treeselect-name"><#if config.pickMultiple&&config.template?has_content><@config.template?interpret/><#else><#if entity[key]??><#if entity[key].fullname??>${entity[key].fullname!}<#else>${entity[key]!}</#if></#if></#if></span>
					</div>
				</div>
			<#elseif config.type=='dictionary' && selectDictionary??>
				<@controlGroup id=id group=group/>
				<@controlLabel label=label description=description for=id/>
				<div class="controls">
					<#if !config.multiple>
					<#if readonly>
						<@s.hidden name=entityName+"."+key />
					</#if>
					<@selectDictionary disabled=readonly id=id dictionaryName=templateName name=entityName+"."+key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
					<#else>
					<#if readonly>
					<#if entity[key]?has_content>
					<#list entity[key] as en>
					<@s.hidden id="" name=entityName+"."+key value=en.name()/>
					</#list>
					</#if>
					</#if>
					<@checkDictionary disabled=readonly id=id dictionaryName=templateName name=entityName+"."+key class=config.cssClass dynamicAttributes=dynamicAttributes/>
					</#if>
				</div>
				</div>
			<#elseif config.type=='attributes'>
				<@controlGroup id=id group=group/>
				<@controlLabel label=label description=description/>
				<div class="controls">
				<table class="datagrid table table-condensed nullable">
					<thead>
						<tr>
							<td>${getText('name')}</td>
							<td>${getText('value')}</td>
							<td class="manipulate"></td>
						</tr>
					</thead>
					<tbody>
						<#assign size = 0>
						<#if entity.attributes?? && entity.attributes?size gt 0>
							<#assign size = entity.attributes?size-1>
						</#if>
						<#list 0..size as index>
						<tr>
							<td><@s.textfield theme="simple" name="${entityName}.attributes[${index}].name"/></td>
							<td><@s.textfield theme="simple" name="${entityName}.attributes[${index}].value"/></td>
							<td class="manipulate"></td>
						</tr>
						</#list>
					</tbody>
				</table>
				</div>
				</div>	
			<#elseif config.type=='schema'>
				<#if editAttributes??>
					<div id="editAttributes"<#if group?has_content> data-group="${group}"</#if>>
					<@editAttributes schemaName=templateName attributes=entity.attributes parameterNamePrefix=entityName+'.'/>
					</div>
				</#if>
			<#elseif config.type=='imageupload'>
				<#if !readonly>
					<@controlGroup id=id group=group/>
						<@s.hidden id=id name=entityName+"."+key class=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?then(config.maxlength,'') dynamicAttributes=dynamicAttributes/>
						<@controlLabel label=label description=description for="${id}-upload-button"/>
						<div class="controls">
							<div style="margin-bottom:5px;">
							<button id="${id}-upload-button" class="btn concatimage" type="button" data-target="${id}-image" data-field="${id}" data-maximum="1">${getText('upload')}</button>
							<#if config.cssClasses?seq_contains('concatsnapshot')>
							<button class="btn concatsnapshot" type="button" data-target="${id}-image" data-field="${id}" data-maximum="1">${getText('snapshot')}</button>
							</#if>
							</div>
							<div id="${id}-image" style="text-align:center;min-height:100px;border:1px solid #ccc;">
								<#if entity[key]?has_content>
									<img src="${entity[key]}" title="${getText('drag.image.file')}"/>
								<#else>
									${getText('drag.image.file')}
								</#if>
							</div>
						</div>
					</div>
				<#else>
					<@controlGroup id=id group=group/>
						<@s.hidden id=id name=entityName+"."+key class=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?then(config.maxlength,'') dynamicAttributes=dynamicAttributes/>
						<@controlLabel label=label description=description/>
						<div class="controls">
							<span>
							<#if entity[key]?has_content>
								<img src="${entity[key]}" title="${getText('drag.image.file')}"/>
							</#if>
							</span>
						</div>
					</div>
				</#if>
			<#elseif config.type=='embedded'&&config.embeddedUiConfigs??>
				<#list config.embeddedUiConfigs.entrySet() as entry>
					<#assign config = entry.value>
					<#assign templateName><@config.templateName?interpret/></#assign>
					<#assign templateName=templateName?markup_string/>
					<#assign pickUrl><@config.pickUrl?interpret/></#assign>
					<#assign pickUrl=pickUrl?markup_string/>
					<#assign value=(entity[key][entry.key])!>
					<#assign hidden=config.hiddenInInput.value>
					<#if !hidden && config.hiddenInInput.expression?has_content>
						<#assign hidden=config.hiddenInInput.expression?eval>
					</#if>
					<#if !hidden>
					<#assign label=entry.key>
					<#if config.alias?has_content>
						<#assign label=config.alias>
					</#if>
					<#assign label=getText(label)>
					<#assign description=getText(config.description)>
					<#assign readonly=config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
					<#assign id=(config.id?has_content)?then(config.id,entityName+'-'+key+'-'+entry.key)/>
					<#assign dynamicAttributes=mergeDynAttrs(config)/>
					<#if config.inputTemplate?has_content>
						<@config.inputTemplate?interpret/>
					<#elseif config.type=='textarea'>
						<@s.textarea id=id label=label name=entityName+'.'+key+'.'+entry.key class=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
					<#elseif config.type=='checkbox'>
						<@s.checkbox readonly=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key class=config.cssClass+config.cssClass?has_content?then(' ','')+"custom" dynamicAttributes=dynamicAttributes />
					<#elseif config.type=='enum'>
						<#if !config.multiple>
						<#if readonly>
						<@s.hidden name=entityName+"."+key value="${(entity[key].name())!}"/>
						</#if>
						<@s.select disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
						<#else>
						<#if readonly>
						<#if entity[key]?has_content>
						<#list entity[key] as en>
						<@s.hidden id="" name=entityName+"."+key value=en.name()/>
						</#list>
						</#if>
						</#if>
						<@s.checkboxlist disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
						</#if>
					<#elseif config.type=='select'>
						<#if readonly>
						<@s.hidden name=entityName+"."+key/>
						</#if>
						<@s.select disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
					<#elseif config.type=='multiselect'>
						<#if readonly>
						<@s.hidden name=entityName+"."+key/>
						</#if>
						<@s.select disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
					<#elseif config.type=='dictionary' && selectDictionary??>
						<@controlGroup id=id group=group/>
						<@controlLabel label=label description=description/>
						<div class="controls">
						<#if !config.multiple>
						<#if readonly>
							<@s.hidden name=entityName+"."+key />
						</#if>
						<@selectDictionary disabled=readonly id="" dictionaryName=templateName name=entityName+'.'+key+'.'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
						<#else>
						<#if readonly>
						<#if entity[key]?has_content>
						<#list entity[key] as en>
						<@s.hidden id="" name=entityName+"."+key value=en.name()/>
						</#list>
						</#if>
						</#if>
						<@checkDictionary disabled=readonly id="" dictionaryName=templateName name=entityName+'.'+key+'.'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
						</#if>
						</div>
						</div>
					<#elseif config.type=='listpick'>
						<div id="control-group-${id}" class="control-group <#if readonly>_</#if>listpick" data-options="{'url':'<@url value=pickUrl/>'<#if config.pickMultiple>,'multiple':true</#if>}"<#if group?has_content> data-group="${group}"</#if>>
						<#assign _name=entityName+'.'+key+'.'+entry.key+"${config.reference?then('.id','')}">
						<#if config.collectionType??><#assign _value=(_name?eval?join(','))!></#if>
						<@s.hidden id=id name=_name value=_value class="listpick-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
						<@controlLabel label=label description=description/>
							<div class="controls<#if readonly> text</#if>">
							<span class=" listpick-name"><#if config.pickMultiple&&config.template?has_content><@config.template?interpret/><#else><#if entity[key][entry.key]??><#if entity[key][entry.key].fullname??>${entity[key][entry.key].fullname!}<#else>${entity[key][entry.key]!}</#if></#if></#if></span>
							</div>
						</div>
					<#elseif config.type=='treeselect'>
						<div id="control-group-${id}" class="control-group <#if readonly>_</#if>treeselect" data-options="{'url':'<@url value=pickUrl/>','cache':false<#if config.pickMultiple>,'multiple':true</#if>}"<#if group?has_content> data-group="${group}"</#if>>
							<#assign _name=entityName+'.'+key+'.'+entry.key+"${config.reference?then('.id','')}">
							<#if config.collectionType??><#assign _value=(_name?eval?join(','))!></#if>
							<@s.hidden id=id name=_name value=_value class="treeselect-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
							<@controlLabel label=label description=description/>
							<div class="controls<#if readonly> text</#if>">
							<span class="treeselect-name"><#if config.pickMultiple&&config.template?has_content><@config.template?interpret/><#else><#if entity[key][entry.key]??><#if entity[key][entry.key].fullname??>${entity[key][entry.key].fullname!}<#else>${entity[key][entry.key]!}</#if></#if></#if></span>
							</div>
						</div>
					<#else>
						<#if (entity[key][entry.key])!?is_date_like>
							<#assign value=entity[key][entry.key]/><#if config.cssClass?contains('datetime')><#assign value=value?datetime/><#elseif config.cssClass?contains('time')><#assign value=value?time/><#else><#assign value=value?date/></#if>
							<@s.textfield id=id label=label name=entityName+'.'+key+'.'+entry.key type=config.inputType value=value?string class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
						<#else>
							<@s.textfield id=id label=label name=entityName+'.'+key+'.'+entry.key type=config.inputType class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
						</#if>
					</#if>
					</#if>
				</#list>
			<#elseif config.type=='collection'&&config.embeddedUiConfigs??>
				<#assign embeddedUiConfigs=config.embeddedUiConfigs/>
				<@controlGroup id=id group=group/>
					<input type="hidden" name="__datagrid_${entityName}.${key}"/>
					<@controlLabel label=label description=description/>
					<div class="controls">
						<table class="table table-bordered table-fixed middle datagrid ${config.cssClass}">
						<thead>
							<tr>
								<#list embeddedUiConfigs.entrySet() as entry>
								<#assign config=entry.value>
								<#assign hidden=config.hiddenInInput.value>
								<#if !hidden && config.hiddenInInput.expression?has_content>
									<#assign hidden=config.hiddenInInput.expression?eval>
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
								<th class="manipulate"></th>
							</tr>
						</thead>
						<tbody>
						<#assign size=0>
						<#assign collections=entity[key]!>
						<#if collections?is_collection_ex && collections?size gt 0>
							<#assign size = collections?size-1>
						</#if>
						<#list 0..size as index>
							<tr>
								<#list embeddedUiConfigs.entrySet() as entry>
								<#assign config = entry.value>
								<#assign hidden=config.hiddenInInput.value>
								<#if !hidden && config.hiddenInInput.expression?has_content>
									<#assign hidden=config.hiddenInInput.expression?eval>
								</#if>
								<#if !hidden>
								<td>
								<#assign value=(entity[key][index][entry.key])!>
								<#assign templateName><@config.templateName?interpret/></#assign>
								<#assign templateName=templateName?markup_string/>
								<#assign pickUrl><@config.pickUrl?interpret/></#assign>
								<#assign pickUrl=pickUrl?markup_string/>
								<#assign readonly=config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
								<#assign dynamicAttributes=mergeDynAttrs(config)/>
								<#if config.inputTemplate?has_content>
									<@config.inputTemplate?interpret/>
								<#elseif config.type=='textarea'>
									<@s.textarea readonly=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key class=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
								<#elseif config.type=='checkbox'>
									<@s.checkbox readonly=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key class=config.cssClass+config.cssClass?has_content?then(' ','')+"custom" dynamicAttributes=dynamicAttributes />
								<#elseif config.type=='enum'>
									<#if !config.multiple>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key value="${(entity[key][index][entry.key].name())!}"/>
									</#if>
									<@s.select disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
									<#else>
									<#if readonly>
									<#if (entity[key][index][entry.key])?has_content>
									<#list entity[key][index][entry.key] as en>
									<@s.hidden id="" name=entityName+"."+key+"["+index+"]."+entry.key value=en.name()/>
									</#list>
									</#if>
									</#if>
									<@s.checkboxlist disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key class=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
									</#if>
								<#elseif config.type=='select'>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key />
									</#if>
									<@s.select disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
								<#elseif config.type=='multiselect'>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key />
									</#if>
									<@s.select disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key class=config.cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
								<#elseif config.type=='dictionary' && selectDictionary??>
									<#if !config.multiple>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key />
									</#if>
									<@selectDictionary disabled=readonly id="" dictionaryName=templateName name=entityName+"."+key+'['+index+'].'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
									<#else>
									<#if readonly>
									<#if (entity[key][index][entry.key])?has_content>
									<#list entity[key][index][entry.key] as en>
									<@s.hidden id="" name=entityName+"."+key+"["+index+"]."+entry.key value=en.name()/>
									</#list>
									</#if>
									</#if>
									<@checkDictionary disabled=readonly id="" dictionaryName=templateName name=entityName+"."+key+'['+index+'].'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
									</#if>
								<#elseif config.type=='listpick'>
										<div class="<#if readonly>_</#if>listpick" data-options="{'url':'<@url value=pickUrl/>'<#if config.pickMultiple>,'multiple':true</#if>}">
										<#assign _name=entityName+"."+key+'['+index+'].'+entry.key+"${config.reference?then('.id','')}">
										<#if config.collectionType??><#assign _value=(_name?eval?join(','))!></#if>
										<@s.hidden name=_name value=_value class="listpick-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
										<span class="listpick-name"><#if config.pickMultiple&&config.template?has_content><@config.template?interpret/><#else><#if (entity[key][index][entry.key])??><#if entity[key][index][entry.key].fullname??>${entity[key][index][entry.key].fullname!}<#else>${entity[key][index][entry.key]!}</#if><#else><i class="glyphicon glyphicon-list"></i></#if></#if></span>
										</div>
								<#elseif config.type=='treeselect'>
										<div class="<#if readonly>_</#if>treeselect" data-options="{'url':'<@url value=pickUrl/>','cache':false<#if config.pickMultiple>,'multiple':true</#if>}">
										<#assign _name=entityName+"."+key+'['+index+'].'+entry.key+"${config.reference?then('.id','')}">
										<#if config.collectionType??><#assign _value=(_name?eval?join(','))!></#if>
										<@s.hidden name=_name value=_value class="treeselect-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
										<span class="treeselect-name"><#if config.pickMultiple&&config.template?has_content><@config.template?interpret/><#else><#if (entity[key][index][entry.key])??><#if entity[key][index][entry.key].fullname??>${entity[key][index][entry.key].fullname!}<#else>${entity[key][index][entry.key]!}</#if><#else><i class="glyphicon glyphicon-list"></i></#if></#if></span>
										</div>
								<#else>
									<#if (entity[key][index][entry.key])!?is_date_like>
										<#assign value=entity[key][index][entry.key]/><#if config.cssClass?contains('datetime')><#assign value=value?datetime/><#elseif config.cssClass?contains('time')><#assign value=value?time/><#else><#assign value=value?date/></#if>
										<@s.textfield id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key type=config.inputType value=value?string class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
									<#else>
										<@s.textfield id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key type=config.inputType class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
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
					</div>
				</div>
			<#else>
				<#if (entity[key])!?is_date_like>
					<#assign value=entity[key]/><#if config.cssClass?contains('datetime')><#assign value=value?datetime/><#elseif config.cssClass?contains('time')><#assign value=value?time/><#else><#assign value=value?date/></#if>
					<@s.textfield id=id label=label name=entityName+"."+key type=config.inputType value=value?string class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
				<#else>
					<@s.textfield id=id label=label name=entityName+"."+key type=config.inputType class=config.cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
				</#if>
			</#if>
		</#if>
		</#if>
	</#list>
	<@s.submit value=getText('save') class="btn-primary"/>
</@s.form>
</body>
</html>