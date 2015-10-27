<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if !entity??><#assign entity=entityName?eval></#if><#assign isnew = !entity??||entity.new/><#assign isnew = !entity??||entity.new/><#if idAssigned><#assign isnew=!entity??||!entity.id?has_content/></#if><#if isnew>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText((richtableConfig.alias?has_content)?string(richtableConfig.alias!,entityName))}</title>
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
	<@s.hidden name="${entityName+'.'+versionPropertyName}" cssClass="version" />
	</#if>
	<#list uiConfigs.entrySet() as entry>
		<#assign key=entry.key>
		<#assign config=entry.value>
		<#assign templateName><@config.templateName?interpret /></#assign>
		<#assign value=entity[key]!>
		<#assign hidden=config.hiddenInInput.value>
		<#if !hidden && config.hiddenInInput.expression?has_content>
			<#assign hidden=config.hiddenInInput.expression?eval>
		</#if>
		<#if !hidden>
		<#assign label=key>
		<#if config.alias??>
			<#assign label=config.alias>
		</#if>
		<#assign label=action.getText(label)>
		<#assign readonly=naturalIds?keys?seq_contains(key)&&!naturalIdMutable&&!isnew||config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
		<#if !isnew&&idAssigned&&key=='id'>
		<#assign readonly=true/>
		</#if>
		<#if !(entity.new && readonly)>
			<#if (Parameters[key]?has_content||Parameters[key+'.id']?has_content)>
				<#assign readonly=true/>
			</#if>
			<#assign id=(config.id?has_content)?string(config.id!,entityName+'-'+key)/>
			<#assign dynamicAttributes=config.dynamicAttributes/>
			<#if config.group?has_content>
				<#assign group=action.getText(config.group)/>
				<#assign dynamicAttributes+={'data-group':group}/>
			</#if>
			<#if config.inputTemplate?has_content>
				<#if config.inputTemplate?index_of('<div class="control-group') gt -1>
				<@config.inputTemplate?interpret/>
				<#else>
				<div class="control-group"<#if group?has_content> data-group="${group}"</#if>>
					<label class="control-label">${label}</label>
					<div class="controls">
					<@config.inputTemplate?interpret/>
					</div>
				</div>
				</#if>
			<#elseif config.type=='textarea'>
				<#if config.maxlength gt 0>
				<#assign dynamicAttributes+={"maxlength":config.maxlength}>
				</#if>
				<@s.textarea id=id label=label name=entityName+"."+key cssClass=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
			<#elseif config.type=='checkbox'>
				<#if readonly>
					<@s.hidden name=entityName+"."+key/>
				</#if>
				<@s.checkbox disabled=readonly id=id label=label name=entityName+"."+key cssClass=config.cssClass+config.cssClass?has_content?then(' ','')+"custom" dynamicAttributes=dynamicAttributes />
			<#elseif config.type=='enum'>
				<#if !config.multiple>
				<#if readonly>
					<@s.hidden name=entityName+"."+key value="${(entity[key].name())!}"/>
				</#if>
				<@s.select disabled=readonly id=id label=label name=entityName+"."+key cssClass=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
				<#else>
				<#if readonly>
				<#if entity[key]?has_content>
				<#list entity[key] as en>
				<@s.hidden id="" name=entityName+"."+key value=en.name()/>
				</#list>
				</#if>
				</#if>
				<@s.checkboxlist disabled=readonly id=id label=label name=entityName+"."+key cssClass=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
				</#if>
			<#elseif config.type=='select'>
				<#if readonly>
					<@s.hidden name=entityName+"."+key />
				</#if>
				<@s.select disabled=readonly id=id label=label name=entityName+"."+key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
			<#elseif config.type=='multiselect'>
				<#if readonly>
					<@s.hidden name=entityName+"."+key />
				</#if>
				<@s.select disabled=readonly id=id label=label name=entityName+"."+key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
			<#elseif config.type=='listpick'>
				<#assign pickUrl><@config.pickUrl?interpret/></#assign>
				<div class="control-group <#if readonly>_</#if>listpick" data-options="{'url':'<@url value=pickUrl/>'}"<#if group?has_content> data-group="${group}"</#if>>
					<@s.hidden id=id name=entityName+"."+key+".id" cssClass="listpick-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
					<label class="control-label">${label}</label>
					<div class="controls<#if readonly> text</#if>">
					<span class="listpick-name"><#if entity[key]??><#if entity[key].fullname??>${entity[key].fullname!}<#else>${entity[key]!}</#if></#if></span>
					</div>
				</div>
			<#elseif config.type=='treeselect'>
				<#assign pickUrl><@config.pickUrl?interpret/></#assign>
				<div class="control-group <#if readonly>_</#if>treeselect" data-options="{'url':'<@url value=pickUrl/>','cache':false}"<#if group?has_content> data-group="${group}"</#if>>
					<@s.hidden id=id name=entityName+"."+key+".id" cssClass="treeselect-id ${config.cssClass}"/>
					<label class="control-label">${label}</label>
					<div class="controls<#if readonly> text</#if>">
					<span class="treeselect-name"><#if entity[key]??><#if entity[key].fullname??>${entity[key].fullname!}<#else>${entity[key]!}</#if></#if></span>
					</div>
				</div>
			<#elseif config.type=='dictionary' && selectDictionary??>
				<div class="control-group"<#if group?has_content> data-group="${group}"</#if>>
				<label class="control-label" for="${id}">${label}</label>
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
				<div class="control-group"<#if group?has_content> data-group="${group}"</#if>>
				<label class="control-label" for="${id}">${label}</label>
				<div class="controls">
				<table class="datagrid table table-condensed nullable">
					<thead>
						<tr>
							<td>${action.getText('name')}</td>
							<td>${action.getText('value')}</td>
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
					<div class="control-group"<#if group?has_content> data-group="${group}"</#if>>
						<@s.hidden id=id name=entityName+"."+key cssClass=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?string(config.maxlength,'') dynamicAttributes=dynamicAttributes/>
						<label class="control-label" for="${id}-upload-button">${label}</label>
						<div class="controls">
							<div style="margin-bottom:5px;">
							<button id="${id}-upload-button" class="btn concatimage" type="button" data-target="${id}-image" data-field="${id}" data-maximum="1">${action.getText('upload')}</button>
							<#if config.cssClasses?seq_contains('concatsnapshot')>
							<button class="btn concatsnapshot" type="button" data-target="${id}-image" data-field="${id}" data-maximum="1">${action.getText('snapshot')}</button>
							</#if>
							</div>
							<div id="${id}-image" style="text-align:center;min-height:100px;border:1px solid #ccc;">
								<#if entity[key]?has_content>
									<img src="${entity[key]}" title="${action.getText('drag.image.file')}"/>
								<#else>
									${action.getText('drag.image.file')}
								</#if>
							</div>
						</div>
					</div>
				<#else>
					<div class="control-group"<#if group?has_content> data-group="${group}"</#if>>
						<@s.hidden id=id name=entityName+"."+key cssClass=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?string(config.maxlength,'') dynamicAttributes=dynamicAttributes/>
						<label class="control-label">${label}</label>
						<div class="controls">
							<span>
							<#if entity[key]?has_content>
								<img src="${entity[key]}" title="${action.getText('drag.image.file')}"/>
							</#if>
							</span>
						</div>
					</div>
				</#if>
			<#elseif config.type=='embedded'&&config.embeddedUiConfigs??>
				<#list config.embeddedUiConfigs.entrySet() as entry>
					<#assign config = entry.value>
					<#assign value=(entity[key][entry.key])!>
					<#assign hidden=config.hiddenInInput.value>
					<#if !hidden && config.hiddenInInput.expression?has_content>
						<#assign hidden=config.hiddenInInput.expression?eval>
					</#if>
					<#if !hidden>
					<#assign label=entry.key>
					<#if config.alias??>
						<#assign label=config.alias>
					</#if>
					<#assign label=action.getText(label)>
					<#assign readonly=config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
					<#assign id=(config.id?has_content)?string(config.id!,entityName+'-'+key+'-'+entry.key)/>
					<#assign dynamicAttributes=config.dynamicAttributes/>	
					<#if config.group?has_content>
						<#assign group=action.getText(config.group)/>
						<#assign dynamicAttributes+={'data-group':group}/>
					</#if>			
					<#if config.inputTemplate?has_content>
						<@config.inputTemplate?interpret/>
					<#elseif config.type=='textarea'>
						<#if config.maxlength gt 0>
						<#assign dynamicAttributes+={"maxlength":config.maxlength}>
						</#if>
						<@s.textarea id=id label=label name=entityName+'.'+key+'.'+entry.key cssClass=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
					<#elseif config.type=='checkbox'>
						<@s.checkbox readonly=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key cssClass=config.cssClass+config.cssClass?has_content?then(' ','')+"custom" dynamicAttributes=dynamicAttributes />
					<#elseif config.type=='enum'>
						<#if !config.multiple>
						<#if readonly>
						<@s.hidden name=entityName+"."+key value="${(entity[key].name())!}"/>
						</#if>
						<@s.select disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key cssClass=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
						<#else>
						<#if readonly>
						<#if entity[key]?has_content>
						<#list entity[key] as en>
						<@s.hidden id="" name=entityName+"."+key value=en.name()/>
						</#list>
						</#if>
						</#if>
						<@s.checkboxlist disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key cssClass=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
						</#if>
					<#elseif config.type=='select'>
						<#if readonly>
						<@s.hidden name=entityName+"."+key/>
						</#if>
						<@s.select disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
					<#elseif config.type=='multiselect'>
						<#if readonly>
						<@s.hidden name=entityName+"."+key/>
						</#if>
						<@s.select disabled=readonly id=id label=label name=entityName+'.'+key+'.'+entry.key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
					<#elseif config.type=='dictionary' && selectDictionary??>
						<div class="control-group"<#if group?has_content> data-group="${group}"</#if>>
						<label class="control-label">${label}</label>
						<div class="controls">
						<#if !config.multiple>
						<#if readonly>
							<@s.hidden name=entityName+"."+key />
						</#if>
						<@selectDictionary disabled=readonly id="" dictionaryName=config.templateName name=entityName+'.'+key+'.'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
						<#else>
						<#if readonly>
						<#if entity[key]?has_content>
						<#list entity[key] as en>
						<@s.hidden id="" name=entityName+"."+key value=en.name()/>
						</#list>
						</#if>
						</#if>
						<@checkDictionary disabled=readonly id="" dictionaryName=config.templateName name=entityName+'.'+key+'.'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
						</#if>
						</div>
						</div>
					<#elseif config.type=='listpick'>
						<#assign pickUrl><@config.pickUrl?interpret/></#assign>
						<div class="control-group <#if readonly>_</#if>listpick" data-options="{'url':'<@url value=pickUrl/>'}"<#if group?has_content> data-group="${group}"</#if>>
						<@s.hidden id=id name=entityName+'.'+key+'.'+entry.key+".id" cssClass="listpick-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
						<label class="control-label">${label}</label>
							<div class="controls<#if readonly> text</#if>">
							<span class=" listpick-name"><#if entity[key][entry.key]??><#if entity[key][entry.key].fullname??>${entity[key][entry.key].fullname!}<#else>${entity[key][entry.key]!}</#if></#if></span>
							</div>
						</div>
					<#elseif config.type=='treeselect'>
						<#assign pickUrl><@config.pickUrl?interpret/></#assign>
						<div class="control-group <#if readonly>_</#if>treeselect" data-options="{'url':'<@url value=pickUrl/>','cache':false}"<#if group?has_content> data-group="${group}"</#if>>
							<@s.hidden id=id name=entityName+'.'+key+'.'+entry.key+".id" cssClass="treeselect-id ${config.cssClass}"/>
							<label class="control-label">${label}</label>
							<div class="controls<#if readonly> text</#if>">
							<span class="treeselect-name"><#if entity[key][entry.key]??><#if entity[key][entry.key].fullname??>${entity[key][entry.key].fullname!}<#else>${entity[key][entry.key]!}</#if></#if></span>
							</div>
						</div>
					<#else>
						<#if config.cssClass?contains('datetime')>
							<@s.textfield id=id label=label name=entityName+'.'+key+'.'+entry.key type=config.inputType value=(entity[key][entry.key]?string('yyyy-MM-dd HH:mm:ss'))! cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
						<#elseif config.cssClass?contains('time')>
							<@s.textfield id=id label=label name=entityName+'.'+key+'.'+entry.key type=config.inputType value=(entity[key][entry.key]?string('HH:mm:ss'))! cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
						<#else>
							<@s.textfield id=id label=label name=entityName+'.'+key+'.'+entry.key type=config.inputType cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
						</#if>
					</#if>
					</#if>
				</#list>
			<#elseif config.type=='collection'&&config.embeddedUiConfigs??>
				<#assign embeddedUiConfigs=config.embeddedUiConfigs/>
				<div class="control-group"<#if group?has_content> data-group="${group}"</#if>>
					<input type="hidden" name="__datagrid_${entityName}.${key}"/>
					<label class="control-label">${label}</label>
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
								<#if config.alias??>
									<#assign label2=config.alias>
								</#if>
								<#assign label2=action.getText(label2)>
								<th<#if entry.value.width?has_content> style="width:${entry.value.width};"</#if>>${label2}</th>
								</#if>
								</#list>
								<th class="manipulate"></th>
							</tr>
						</thead>
						<tbody>
						<#assign size=0>
						<#assign collections=entity[key]!>
						<#if collections?is_collection && collections?size gt 0>
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
								<#assign readonly=config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
								<#assign dynamicAttributes=config.dynamicAttributes/>
								<#if config.inputTemplate?has_content>
									<@config.inputTemplate?interpret/>
								<#elseif config.type=='textarea'>
									<#if config.maxlength gt 0>
									<#assign dynamicAttributes+={"maxlength":config.maxlength}>
									</#if>
									<@s.textarea readonly=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key cssClass=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?then('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
								<#elseif config.type=='checkbox'>
									<@s.checkbox readonly=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key cssClass=config.cssClass+config.cssClass?has_content?then(' ','')+"custom" dynamicAttributes=dynamicAttributes />
								<#elseif config.type=='enum'>
									<#if !config.multiple>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key value="${(entity[key][index][entry.key].name())!}"/>
									</#if>
									<@s.select disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key cssClass=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
									<#else>
									<#if readonly>
									<#if (entity[key][index][entry.key])?has_content>
									<#list entity[key][index][entry.key] as en>
									<@s.hidden id="" name=entityName+"."+key+"["+index+"]."+entry.key value=en.name()/>
									</#list>
									</#if>
									</#if>
									<@s.checkboxlist disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key cssClass=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
									</#if>
								<#elseif config.type=='select'>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key />
									</#if>
									<@s.select disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
								<#elseif config.type=='multiselect'>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key />
									</#if>
									<@s.select disabled=readonly id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=dynamicAttributes/>
								<#elseif config.type=='dictionary' && selectDictionary??>
									<#if !config.multiple>
									<#if readonly>
										<@s.hidden name=entityName+"."+key+"["+index+"]."+entry.key />
									</#if>
									<@selectDictionary disabled=readonly id="" dictionaryName=config.templateName name=entityName+"."+key+'['+index+'].'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
									<#else>
									<#if readonly>
									<#if (entity[key][index][entry.key])?has_content>
									<#list entity[key][index][entry.key] as en>
									<@s.hidden id="" name=entityName+"."+key+"["+index+"]."+entry.key value=en.name()/>
									</#list>
									</#if>
									</#if>
									<@checkDictionary disabled=readonly id="" dictionaryName=config.templateName name=entityName+"."+key+'['+index+'].'+entry.key required=config.required class=config.cssClass dynamicAttributes=dynamicAttributes/>
									</#if>
								<#elseif config.type=='listpick'>
										<#assign pickUrl><@config.pickUrl?interpret/></#assign>
										<div class="<#if readonly>_</#if>listpick" data-options="{'url':'<@url value=pickUrl/>'}">
										<@s.hidden name=entityName+"."+key+'['+index+'].'+entry.key+".id" cssClass="listpick-id ${config.cssClass}" dynamicAttributes=dynamicAttributes/>
										<span class="listpick-name"><#if (entity[key][index][entry.key])??><#if entity[key][index][entry.key].fullname??>${entity[key][index][entry.key].fullname!}<#else>${entity[key][index][entry.key]!}</#if><#else><i class="glyphicon glyphicon-list"></i></#if></span>
										</div>
								<#elseif config.type=='treeselect'>
										<#assign pickUrl><@config.pickUrl?interpret/></#assign>
										<div class="<#if readonly>_</#if>treeselect" data-options="{'url':'<@url value=pickUrl/>','cache':false}">
										<@s.hidden name=entityName+"."+key+'['+index+'].'+entry.key+".id" cssClass="treeselect-id ${config.cssClass}"/>
										<span class="treeselect-name"><#if (entity[key][index][entry.key])??><#if entity[key][index][entry.key].fullname??>${entity[key][index][entry.key].fullname!}<#else>${entity[key][index][entry.key]!}</#if><#else><i class="glyphicon glyphicon-list"></i></#if></span>
										</div>
								<#else>
									<#if config.cssClass?contains('datetime')>
										<@s.textfield id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key type=config.inputType value=(entity[key][index][entry.key]?string('yyyy-MM-dd HH:mm:ss'))! cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
									<#elseif config.cssClass?contains('time')>
										<@s.textfield id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key type=config.inputType value=(entity[key][index][entry.key]?string('HH:mm:ss'))! cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
									<#else>
										<@s.textfield id="" theme="simple" name=entityName+"."+key+'['+index+'].'+entry.key type=config.inputType cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
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
				<#assign dynamicAttributes=config.dynamicAttributes/>
				<#if config.group?has_content>
				<#assign dynamicAttributes+={'data-group':action.getText(config.group)}/>
				</#if>
				<#if config.cssClass?contains('datetime')>
					<@s.textfield id=id label=label name=entityName+"."+key type=config.inputType value=(entity[key]?string('yyyy-MM-dd HH:mm:ss'))! cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
				<#elseif config.cssClass?contains('time')>
					<@s.textfield id=id label=label name=entityName+"."+key type=config.inputType value=(entity[key]?string('HH:mm:ss'))! cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
				<#else>
					<@s.textfield id=id label=label name=entityName+"."+key type=config.inputType cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=dynamicAttributes />
				</#if>
			</#if>
		</#if>
		</#if>
	</#list>
	<@s.submit value=action.getText('save') class="btn-primary"/>
</@s.form>
</body>
</html></#escape>