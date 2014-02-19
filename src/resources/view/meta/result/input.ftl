<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if !entity??><#assign entity=entityName?eval></#if><#assign isnew = !entity??||entity.new/><#assign isnew = !entity??||entity.new/><#if idAssigned><#assign isnew=!entity??||!entity.id?has_content/></#if><#if isnew>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText(entityName)}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/save" method="post" cssClass="ajax form-horizontal${richtableConfig.importable?string(' importable','')}">
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
	<#list uiConfigs?keys as key>
		<#assign config=uiConfigs[key]>
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
		<#assign readonly=naturalIds?keys?seq_contains(key)&&!naturalIdMutable&&!isnew||config.readonly.value||config.readonly.expression?has_content&&config.readonly.expression?eval>
		<#if !isnew&&idAssigned&&key=='id'>
		<#assign readonly=true/>
		</#if>
		<#if !(entity.new && readonly)>
			<#assign id=(config.id?has_content)?string(config.id!,entityName+'-'+key)/>
			<#if config.type=='textarea'>
				<#assign dynamicAttributes=config.dynamicAttributes/>
				<#if config.maxlength gt 0>
				<#assign dynamicAttributes=dynamicAttributes+{"maxlength":config.maxlength}>
				</#if>
				<@s.textarea id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass+(config.cssClass?contains('span')||config.cssClass?contains('input-'))?string('',' input-xxlarge') readonly=readonly dynamicAttributes=dynamicAttributes/>
			<#elseif config.type=='checkbox'>
				<#if !readonly>
					<@s.checkbox id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass+config.cssClass?has_content?string(' ','')+"custom" dynamicAttributes=config.dynamicAttributes />
				<#else>
					<@s.hidden name=entityName+"."+key />
					<@s.checkbox id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass disabled="true" dynamicAttributes=config.dynamicAttributes />
				</#if>
			<#elseif config.type=='enum'>
				<#if !readonly>
					<@s.select id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=config.dynamicAttributes/>
				<#else>
					<@s.hidden name=entityName+"."+key value="%{${entityName+'.'+key+'.id'}}"/>
					<@s.select id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass disabled=true list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=config.dynamicAttributes />
				</#if>
			<#elseif config.type=='select'>
				<#if !readonly>
					<@s.select id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=config.dynamicAttributes/>
				<#else>
					<@s.hidden name=entityName+"."+key value="%{${entityName+'.'+key+'.id'}}"/>
					<@s.select id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass disabled=true list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=config.dynamicAttributes />
				</#if>
			<#elseif config.type=='multiselect'>
				<#if !readonly>
					<@s.select id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" multiple=true dynamicAttributes=config.dynamicAttributes/>
				<#else>
					<@s.hidden name=entityName+"."+key value="%{${entityName+'.'+key+'.id'}}"/>
					<@s.select id=id label="%{getText('${label}')}" name=entityName+"."+key cssClass=config.cssClass disabled=true list=config.optionsExpression?eval listKey=config.listKey listValue=config.listValue headerKey="" multiple=true headerValue="" dynamicAttributes=config.dynamicAttributes />
				</#if>	
			<#elseif config.type=='listpick'>
				<#if !readonly>
					<div class="control-group listpick" data-options="{'url':'<@url value=config.pickUrl/>','name':'#${id}-control','id':'#${id}'}">
						<@s.hidden id=id name=entityName+"."+key+".id" cssClass=config.cssClass/>
						<label class="control-label" for="${id}-control">${action.getText(label)}</label>
						<div class="controls">
						<span id="${id}-control"><#if entity[key]??><#if entity[key].fullname??>${entity[key].fullname!}<#else>${entity[key]!}</#if></#if></span>
						</div>
					</div>
				<#else>
					<div class="control-group">
						<label class="control-label">${action.getText(label)}</label>
						<div class="controls">
						<span>${entity[key]!}</span>
						</div>
					</div>
				</#if>
			<#elseif config.type=='dictionary' && selectDictionary??>
				<div class="control-group">
				<label class="control-label" for="${id}">${action.getText(label)}</label>
				<div class="controls">
				<#if !readonly>
					<@selectDictionary id=id dictionaryName=templateName name=entityName+"."+key value="${entity[key]!}" required=config.required class=config.cssClass dynamicAttributes=config.dynamicAttributes/>
				<#else>
					<@s.hidden id=id name=entityName+"."+key/>
					<span id="${id}"><@displayDictionaryLabel dictionaryName=templateName value=entity[key]!/></span>
				</#if>
				</div>
				</div>
			<#elseif config.type=='attributes'>
				<div class="control-group">
				<label class="control-label" for="${id}">${action.getText(label)}</label>
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
					<div id="editAttributes">
					<@editAttributes schemaName=templateName attributes=entity.attributes parameterNamePrefix=entityName+'.'/>
					</div>
				</#if>
			<#elseif config.type=='imageupload'>
				<#if !readonly>
					<div class="control-group">
						<@s.hidden id=id name=entityName+"."+key cssClass=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?string(config.maxlength,'') dynamicAttributes=config.dynamicAttributes/>
						<label class="control-label" for="${id}-upload-button">${action.getText(label)}</label>
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
					<div class="control-group">
						<@s.hidden id=id name=entityName+"."+key cssClass=config.cssClass+" nocheck" maxlength=(config.maxlength gt 0)?string(config.maxlength,'') dynamicAttributes=config.dynamicAttributes/>
						<label class="control-label">${action.getText(label)}</label>
						<div class="controls">
							<span>
							<#if entity[key]?has_content>
								<img src="${entity[key]}" title="${action.getText('drag.image.file')}"/>
							</#if>
							</span>
						</div>
					</div>
				</#if>
			<#else>
				<#if config.cssClass?contains('datetime')>
					<@s.textfield value=(entity[key]?string('yyyy-MM-dd HH:mm:ss'))! id=id label="%{getText('${label}')}" name=entityName+"."+key type=config.inputType cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=config.dynamicAttributes />
				<#elseif config.cssClass?contains('time')>
					<@s.textfield value=(entity[key]?string('HH:mm:ss'))! id=id label="%{getText('${label}')}" name=entityName+"."+key type=config.inputType cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=config.dynamicAttributes />
				<#else>
					<@s.textfield id=id label="%{getText('${label}')}" name=entityName+"."+key type=config.inputType cssClass=config.cssClass maxlength="${(config.maxlength gt 0)?string(config.maxlength,'')}" readonly=readonly dynamicAttributes=config.dynamicAttributes />
				</#if>
			</#if>
		</#if>
		</#if>
	</#list>
	<@s.submit value="%{getText('save')}" cssClass="btn-primary"/>
</@s.form>
</body>
</html></#escape>