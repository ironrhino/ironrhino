<#if !entityName??>
<#assign entityName=action.class.simpleName?uncap_first/>
<#if entityName?ends_with('Action')>
<#assign entityName=entityName?substring(0,entityName?length-6)/>
</#if>
</#if>
<#assign requestURI=request.requestURI>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('pick')}${action.getText(entityName)}</title>
</head>
<body>

<#assign multiple=false>
<#if Parameters.multiple?? && Parameters.multiple=='true'>
	<#assign multiple=true>
</#if>
<#if Parameters.columns??>
	<#assign propertyNames=Parameters.columns?split(',')>
	<#assign columnNames=[]>
	<#if uiConfigs??>
	<#list propertyNames as column>
		<#if uiConfigs[column]??>
		<#assign config=uiConfigs[column]>
		<#assign shown=!config.hiddenInList.value>
		<#if shown && config.hiddenInList.expression?has_content>
		<#assign shown=!config.hiddenInList.expression?eval/>
		</#if>
		<#if shown>
			<#assign columnNames=columnNames+[column]>
		</#if>
		</#if>
	</#list>
	<#else>
	<#assign columnNames=propertyNames>
	</#if>
<#elseif uiConfigs??>
	<#assign propertyNames=uiConfigs?keys>
	<#assign columnNames=[]>
	<#list uiConfigs.entrySet() as entry>
		<#assign column=entry.key>
		<#if (column=='name' && !propertyNames?seq_contains('fullname') || column=='fullname' || column=='code' || entry.value.shownInPick || naturalIds?? && naturalIds?keys?seq_contains(column))>
			<#assign columnNames=columnNames+[column]>
		</#if>
	</#list>
</#if>
<#if !treeable??>
<#assign treeable = action.getParent??>
</#if>
<#if treeable>
	<#if !columnNames?? || columnNames?size == 0>
	<#assign columnNames=['fullname']>
	</#if>
	<#assign href=requestURI>
	<#if request.queryString??>
		<#assign params=request.queryString?split('&')/>
		<#list params as param>
		<#if !(param?starts_with('_=')||param?starts_with('parent=')||param?starts_with('keyword='))>
		<#assign href=href+(param_index==0)?string('?','&')+param>
		</#if>
		</#list>
	</#if>
</#if>
<#assign columns={}>
<#if columnNames??>
	<#list columnNames as column>
		<#if treeable && column == 'name'||column == 'fullname'>
			<#assign columns=columns+{column:{'template':r'<#if entity.leaf??&&!entity.leaf><a href="${href}${href?contains("?")?string("&","?")+"parent="+entity.id}" class="ajax view" data-replacement="${entityName}_pick">${value}</a><#else>${value}</#if>'}}/>
		<#else>
			<#assign columns=columns+{column:{}}/>
		</#if>
	</#list>
</#if>
<#if treeable>
<#assign _parent=0>
<#if parentEntity??>
	<#if parentEntity.parent??>
	<#assign _parent=parentEntity.parent.id>
	</#if>
<#else>
	<#assign _entity=entityName?eval!>
	<#if _entity??>
	<#assign parentEntity=_entity>
	<#if _entity.parent??><#assign _parent=_entity.parent.id></#if>
	</#if>
	</#if>
</#if>
<#if multiple>
<#assign bottomButtons=r'<button type="button" class="btn pick" data-shown="selected">${action.getText("confirm")}</button> '/>
<#else>
<#assign bottomButtons=''/>
</#if>
<#assign bottomButtons=bottomButtons+r'
<#if treeable&&Parameters.parent??>
<a href="${href}<#if _parent?? && _parent gt 0>${href?contains("?")?string("&","?")+"parent="+_parent}</#if>" class="btn ajax view" data-replacement="${entityName}_pick">${action.getText("upward")}</a>
</#if>
<#if filterable><button type="button" class="btn filter">${action.getText("filter")}</button></#if>
'>
<div id="${entityName}_pick">
<#if _parent?? && parentEntity?? && parentEntity.id?? && parentEntity.id gt 0>
<ul class="breadcrumb">
	<li>
    	<a href="${href}" class="ajax view">${action.getText(entityName)}</a> <span class="divider">/</span>
	</li>
	<#if parentEntity.level gt 1>
	<#list 1..parentEntity.level-1 as level>
	<#assign ancestor=parentEntity.getAncestor(level)>
	<li>
    	<a href="${href}<#if _parent?? && _parent gt 0>${href?contains("?")?string("&","?")+"parent="+ancestor.id}</#if>" class="ajax view">${ancestor.name}</a> <span class="divider">/</span>
	</li>
	</#list>
	</#if>
	<li class="active">${parentEntity.name}</li>
</ul>
</#if>
<@richtable entityName=entityName formid=entityName+'_pick_form' action=requestURI columns=columns bottomButtons=bottomButtons searchable=true readonly=true showCheckColumn=true multipleCheck=multiple columnfilterable=false resizable=false sortable=false showPageSize=false/>
</div>
</body>
</html></#escape>