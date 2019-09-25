<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<#assign entityDisplayName=getText((richtableConfig.alias?has_content)?then(richtableConfig.alias,entityName))/>
<title>${entityDisplayName}${getText('list')}</title>
</head>
<body>
<#assign treeview='treeview'==Parameters.view!/>
<#if (treeable!false) && ((parentEntity.id)!0) gt 0>
<ul class="breadcrumb">
	<li>
		<#if !treeview><a href="${actionBaseUrl}<#if tree??>?tree=${tree}</#if>" class="ajax view">${entityDisplayName}</a><#else>${entityDisplayName}</#if> <span class="divider">/</span>
	</li>
	<#if parentEntity.level gt 1>
	<#assign renderItem=(!tree??||tree<1)/>
	<#list 1..parentEntity.level-1 as level>
	<#assign ancestor=parentEntity.getAncestor(level)>
	<#if !renderItem>
		<#assign renderItem=(ancestor.id==tree!)/>
	</#if>
	<#if renderItem>
	<li>
		<#if !treeview><a href="${actionBaseUrl}?parent=${ancestor.id?string}<#if tree??>&tree=${tree}</#if>" class="ajax view">${ancestor.name}</a><#else>${ancestor.name}</#if> <span class="divider">/</span>
	</li>
	</#if>
	</#list>
	</#if>
	<li class="active">${parentEntity.name}</li>
</ul>
</#if>
<#assign celleditable=richtableConfig.celleditable>
<#if richtableConfig.listHeader?has_content>
<@richtableConfig.listHeader?interpret/>
</#if>
<#if richtableConfig.formid?has_content>
<#assign formid><@richtableConfig.formid?interpret/></#assign>
<#assign formid=formid?markup_string>
</#if>
<#if richtableConfig.formHeader?has_content>
<#assign formHeader><@richtableConfig.formHeader?interpret/></#assign>
<#assign formHeader=formHeader?markup_string>
</#if>
<#if richtableConfig.formFooter?has_content>
<#assign formFooter><@richtableConfig.formFooter?interpret/></#assign>
<#assign formFooter=formFooter?markup_string>
</#if>
<#if celleditable&&versionPropertyName??>
<#assign dynamicAttributes={"data-versionproperty":versionPropertyName}>
</#if>
<#assign parameterNamesInQueryString=getParameterNamesInQueryString()>
<@rtstart formid=formid! entityName=entityName formHeader=formHeader! formCssClass=richtableConfig.listFormCssClass showPageSize=richtableConfig.showPageSize showCheckColumn=richtableConfig.showCheckColumn showQueryForm=richtableConfig.showQueryForm queryFormGridColumns=richtableConfig.queryFormGridColumns dynamicAttributes=dynamicAttributes!/>
<#assign size=0>
<#list uiConfigs as key,value>
	<#assign hidden=value.hiddenInList.value>
	<#if !hidden && value.hiddenInList.expression?has_content>
	<#assign hidden=value.hiddenInList.expression?eval/>
	</#if>
	<#if !hidden && parameterNamesInQueryString?seq_contains(key) && !Parameters[key+'-op']?has_content>
	<#assign hidden=true/>
	</#if>
	<#if !hidden>
		<#assign size++>
	</#if>
</#list>
<#assign viewable=richtableConfig.exportable>
<#assign hasSelect=false>
<#assign columns=[]>
<#assign sumColumns={}>
<#list uiConfigs as key,config>
		<#assign hidden=config.hiddenInList.value>
		<#if !hidden && config.hiddenInList.expression?has_content>
		<#assign hidden=config.hiddenInList.expression?eval>
		</#if>
		<#--
		<#if !hidden && parameterNamesInQueryString?seq_contains(key) && !Parameters[key+'-op']?has_content && request.getParameterValues(key)?size==1>
		<#assign hidden=true/>
		</#if>
		-->
		<#if !hidden>
			<#assign columns+=[key]>
			<#if ((resultPage.result)!list)?size gt 0 && config['showSum']>
				<#assign sumColumns+={key:{"value":0,"template":config['listTemplate']!}}>
			</#if>
			<#assign label=key>
			<#if celleditable&&!config['listTemplate']?has_content&&!(readonly.value||config.readonly.value) && !(naturalIds?keys?seq_contains(key)&&!naturalIdMutable)>
				<#assign cellEdit=config.cellEdit!/>
				<#if !config.multiple && cellEdit=='' && !(idAssigned && key=='id')>
					<#if config.type=='input'>
						<#assign cellEdit='click'/>
					<#elseif config.type=='textarea'>
						<#assign cellEdit='click,textarea'/>
					<#elseif config.type=='checkbox'>
						<#assign cellEdit='click,boolean'/>
					<#elseif config.type=='enum'||config.type=='select'>
						<#assign hasSelect=true>
						<#assign cellEdit='click,select,rt_select_template_'+key/>
					<#elseif config.type=='dictionary'>
						<#assign hasSelect=true>
						<#assign cellEdit='click,select,rt_select_template_'+key/>
					</#if>
				</#if>
			<#else>
				<#assign cellEdit=''/>
			</#if>
			<@rttheadtd name=label alias=config['alias']! description=config['description']! width=config['width']! title=config['title']! class=config['thCssClass']! cellname=entityName+'.'+key cellEdit=cellEdit readonly=readonly.value resizable=viewable||!(readonly.value&&!key?has_next)/>
		<#else>
			<#assign viewable=true>
		</#if>
</#list>
<#assign showActionColumn=richtableConfig.showActionColumn && (richtableConfig.actionColumnButtons?has_content||!readonly.value&&!appendOnly||viewable)>
<@rtmiddle showActionColumn=showActionColumn/>
<#list ((resultPage.result)!list) as entity>
<#assign entityReadonly = readonly.value/>
<#if !entityReadonly && readonly.expression?has_content><#assign entityReadonly=readonly.expression?eval></#if>
<#assign rowDynamicAttributes={}>
<#if richtableConfig.rowDynamicAttributes?has_content>
<#assign rowDynamicAttributes><@richtableConfig.rowDynamicAttributes?interpret /></#assign>
<#assign rowDynamicAttributes=rowDynamicAttributes?markup_string>
<#if rowDynamicAttributes?has_content>
<#assign rowDynamicAttributes=rowDynamicAttributes?eval>
<#else>
<#assign rowDynamicAttributes={}>
</#if>
</#if>
<#if !readonly.value&&entityReadonly>
<#assign rowDynamicAttributes+={"data-readonly":"true"}>
<#if !readonly.deletable>
<#assign rowDynamicAttributes+={"data-deletable":"false"}>
</#if>
</#if>
<#if celleditable&&versionPropertyName??>
<#assign rowDynamicAttributes+={"data-version":entity[versionPropertyName]}>
</#if>
<@rttbodytrstart entity=entity showCheckColumn=richtableConfig.showCheckColumn dynamicAttributes=rowDynamicAttributes/>
<#list uiConfigs as key,config>
	<#assign value=entity[key]!>
	<#assign hidden=config.hiddenInList.value>
	<#if !hidden && config.hiddenInList.expression?has_content>
	<#assign hidden=config.hiddenInList.expression?eval>
	</#if>
	<#--
	<#if !hidden && parameterNamesInQueryString?seq_contains(key) && !Parameters[key+'-op']?has_content>
	<#assign hidden=true/>
	</#if>
	-->
	<#if !hidden>
		<#assign _celleditable=celleditable>
		<#if config.inverseRelation||config.readonly.value>
			<#assign _celleditable=false>
		</#if>
		<#assign dynamicAttributes={}>
		<#if (config.type=='listpick'||config.type=='treeselect')&&celleditable&&!entityReadonly&&!(naturalIds?keys?seq_contains(key)&&!naturalIdMutable)&&!config.readonly.value&&!(config.readonly.expression?has_content&&config.readonly.expression?eval)>
			<#if _celleditable>
			<#assign pickUrl><@config.pickUrl?interpret/></#assign>
			<#assign pickUrl=pickUrl?markup_string>
			<#assign cellvalue=(value.id?string)!>
			<#if config.multiple && value?has_content>
				<#assign ids=[]>
				<#list value as v>
				<#assign ids+=[config.reference?then(v.id!,v)]>
				</#list>
				<#assign cellvalue=ids?join(',')>
			</#if>
			<#assign dynamicAttributes={"class":config.type,"data-cellvalue":cellvalue,"data-options":"{'url':'"+pickUrl+"','name':'this','id':'this@data-cellvalue','multiple':"+config.multiple?string+"}"}>
			</#if>
		</#if>
		<#if config.readonly.expression?has_content && config.readonly.expression?eval>
		<#assign dynamicAttributes+={'data-readonly':'true'}/>
		</#if>
		<#assign value = entity[key]!>
		<#if value?has_content>
			<#if config.multiple>
				<#if _celleditable>
				<#assign temp = []>
				<#if config.type=='dictionary'><#assign templateName><@config.templateName?interpret /></#assign><#assign templateName=templateName?markup_string/></#if>
				<#list value as v>
				<#if config.type=='dictionary'>
				<#assign temp+=[getDictionaryLabel(templateName,v)]/>
				<#elseif config.type=='enum'>
				<#assign temp+=[v.name()]/>
				<#else>
				<#assign temp+=[config.reference?then(v.id!,v)]/>
				</#if>
				</#list>
				<#assign dynamicAttributes+={'data-cellvalue':temp?join(',')}/>
				</#if>
			<#elseif config.type=='dictionary'>
				<#if _celleditable>
				<#assign dynamicAttributes+={'data-cellvalue':value}/>
				</#if>
				<#assign templateName><@config.templateName?interpret /></#assign>
				<#assign templateName=templateName?markup_string/>
				<#assign value=getDictionaryLabel(templateName,value)/>
			</#if>
		</#if>
		<#assign template=config.listTemplate/>
		<#list sumColumns as name,config>
			<#if key==name && value?has_content><#assign sumColumns+={name:{"value":config.value+value,"template":template!}}></#if>
		</#list>
		<@rttbodytd entity=entity value=value celleditable=_celleditable template=template cellDynamicAttributes=config.cellDynamicAttributes dynamicAttributes=dynamicAttributes/>
	</#if>
</#list>
<@rttbodytrend entity=entity showActionColumn=showActionColumn buttons=richtableConfig.actionColumnButtons editable=!readonly.value viewable=viewable entityReadonly=entityReadonly inputWindowOptions=richtableConfig.inputWindowOptions! viewWindowOptions=richtableConfig.viewWindowOptions!/>
</#list>
<@rtend columns=columns sumColumns=sumColumns showCheckColumn=richtableConfig.showCheckColumn showActionColumn=showActionColumn showBottomButtons=richtableConfig.showBottomButtons celleditable=celleditable readonly=readonly.value deletable=!readonly.value||readonly.deletable searchable=searchable filterable=richtableConfig.filterable downloadable=richtableConfig.downloadable showPageSize=richtableConfig.showPageSize! buttons=richtableConfig.bottomButtons! enableable=enableable formFooter=formFooter! inputWindowOptions=richtableConfig.inputWindowOptions!/>
<#if !readonly.value && hasSelect>
<div style="display: none;">
<#list uiConfigs as key,config>
	<#assign hidden=config.hiddenInList.value||config.multiple>
	<#if !hidden && config.hiddenInList.expression?has_content>
	<#assign hidden=config.hiddenInList.expression?eval/>
	</#if>
	<#--
	<#if !hidden && parameterNamesInQueryString?seq_contains(key) && !Parameters[key+'-op']?has_content>
	<#assign hidden=true/>
	</#if>
	-->
	<#if !hidden>
		<#if config.type=='enum'>
		<template id="rt_select_template_${key}">
		<#if config.required>
		<@s.select theme="simple" name=entityName+"."+key list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue/>
		<#else>
		<@s.select theme="simple" name=entityName+"."+key list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue=""/>
		</#if>
		</template>
		<#elseif config.type=='select'>
		<template id="rt_select_template_${key}">
		<#if config.required>
		<@s.select theme="simple" name=entityName+"."+key list=config.listOptions?eval listKey=config.listKey listValue=config.listValue/>
		<#else>
		<@s.select theme="simple" name=entityName+"."+key list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue=""/>
		</#if>
		</template>
		<#elseif config.type=='dictionary'>
		<template id="rt_select_template_${key}">
		<#assign templateName><@config.templateName?interpret /></#assign>
		<#assign templateName=templateName?markup_string/>
		<@s.select theme="simple" name=entityName+"."+key class=config.cssClass list=beans['dictionaryControl'].getItemsAsMap(templateName) headerKey="" headerValue=""/>
		</template>
		</#if>
	</#if>
</#list></div>
</#if>
<#if richtableConfig.listFooter?has_content>
<@richtableConfig.listFooter?interpret/>
</#if>
</body>
</html>