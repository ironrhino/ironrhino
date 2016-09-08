<#macro richtable columns entityName formid='' action='' showActionColumn=true showBottomButtons=true actionColumnWidth='50px' actionColumnButtons='' bottomButtons='' rowid='' resizable=true sortable=true readonly=false readonlyExpression='' createable=true viewable=false celleditable=true deletable=true enableable=false searchable=false filterable=true downloadable=true searchButtons='' includeParameters=true showPageSize=true showCheckColumn=true multipleCheck=true columnfilterable=true rowDynamicAttributes='' formHeader='' formFooter='' formCssClass=''>
<@rtstart formid=formid action=action entityName=entityName resizable=resizable sortable=sortable includeParameters=includeParameters showCheckColumn=showCheckColumn multipleCheck=multipleCheck columnfilterable=columnfilterable formHeader=formHeader formCssClass=formCssClass>
<#nested/>
</@rtstart>
<#local size = columns?keys?size>
<#list columns?keys as name>
<#local config = columns[name]>
<#local cellname=((config['trimPrefix']??)?then('',entityName+'.'))+name>
<@rttheadtd name=name alias=config['alias']! title=config['title']! class=config['thCssClass']! width=config['width']! cellname=cellname cellEdit=celleditable?then(config['cellEdit']!,'') readonly=readonly resizable=(readonly&&name?has_next||!readonly)&&resizable excludeIfNotEdited=config['excludeIfNotEdited']!false/>
</#list>
<@rtmiddle width=actionColumnWidth showActionColumn=showActionColumn&&(actionColumnButtons?has_content||!readonly||viewable)/>
<#if resultPage??><#local list=resultPage.result></#if>
<#list list as entity>
<#local entityReadonly = !readonly && readonlyExpression?has_content && readonlyExpression?eval />
<#local _rowDynamicAttributes={}>
<#if rowDynamicAttributes?has_content>
<#local _rowDynamicAttributes><@rowDynamicAttributes?interpret /></#local>
<#if _rowDynamicAttributes?has_content>
<#local _rowDynamicAttributes=_rowDynamicAttributes?eval>
<#else>
<#local _rowDynamicAttributes={}>
</#if>
</#if>
<#if celleditable&&!readonly&&entityReadonly>
<#local _rowDynamicAttributes+={"data-readonly":"true"}>
</#if>
<@rttbodytrstart entity=entity showCheckColumn=showCheckColumn multipleCheck=multipleCheck rowid=rowid dynamicAttributes=_rowDynamicAttributes/>
<#list columns?keys as name>
	<#local config = columns[name]>
	<#if config['value']??>
	<#local value=config['value']>
	<#else>
	<#if !name?contains('.')>
		<#local value=entity[name]!>
	<#else>
		<#local value=('(entity.'+name+')!')?eval>
	</#if>
	</#if>
	<#local dynamicAttributes={}>
	<#if config['readonlyExpression']?has_content && config['readonlyExpression']?eval>
		<#local dynamicAttributes+={'data-readonly':'true'}/>
	</#if>
	<@rttbodytd entity=entity value=value celleditable=celleditable&&config['cellEdit']?? template=config['template']! cellDynamicAttributes=config['dynamicAttributes'] dynamicAttributes=dynamicAttributes/>
</#list>
<@rttbodytrend entity=entity showActionColumn=showActionColumn buttons=actionColumnButtons editable=!readonly viewable=viewable entityReadonly=entityReadonly/>
</#list>
<@rtend showBottomButtons=showBottomButtons buttons=bottomButtons readonly=readonly createable=createable celleditable=celleditable deletable=deletable enableable=enableable searchable=searchable filterable=filterable downloadable=downloadable searchButtons=searchButtons showPageSize=showPageSize formFooter=formFooter/>
</#macro>

<#macro rtstart formid='',action='',entityName='',resizable=true,sortable=true,includeParameters=true showCheckColumn=true multipleCheck=true columnfilterable=true formHeader='' formCssClass='' dynamicAttributes...>
<#local parameterNamesInQueryString=[]>
<#if !action?has_content>
<#local action=request.requestURI>
<#if request.queryString?has_content>
<#list request.queryString?split('&') as pair>
	<#local name=pair?keep_before('=')>
	<#if name!='_'&&name!='pn'&&name!='ps'&&!name?starts_with('resultPage.')&&name!='keyword'&&!formHeader?contains(' name="'+name+'" ')>
		<#local action+=action?contains('?')?then('&','?')+pair>
		<#local parameterNamesInQueryString+=[name]>
	</#if>
</#list>
</#if>
</#if>
<#if dynamicAttributes['dynamicAttributes']??>
<#local dynamicAttributes+=dynamicAttributes['dynamicAttributes']>
</#if>
<form id="<#if formid?has_content>${formid}<#else>${entityName}<#if Parameters.tab?? && Parameters[Parameters.tab]??>_${Parameters.tab+'_'+Parameters[Parameters.tab]}</#if>_form</#if>" action="${action}" method="post" class="richtable ajax view<#if formCssClass?index_of('nohistory') lt 0 && 'treeview'!=Parameters.view!> history</#if> ${formCssClass}"<#if actionBaseUrl!=action> data-actionbaseurl="${actionBaseUrl}"</#if><#if entityName!=action&&entityName?has_content> data-entity="${entityName}"</#if><#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>>
${formHeader!}
<#nested/>
<#if includeParameters>
<#list Parameters?keys as name>
<#if !parameterNamesInQueryString?seq_contains(name)&&name!='_'&&name!='pn'&&name!='ps'&&!name?starts_with('resultPage.')&&name!='keyword'&&!formHeader?contains(' name="'+name+'" ')>
<input type="hidden" name="${name}" value="${Parameters[name]}" />
</#if>
</#list>
</#if>
<table class="table table-hover table-striped table-bordered richtable<#if sortable> sortable</#if><#if columnfilterable> filtercolumn</#if><#if resizable> resizable</#if>">
<thead>
<tr>
<#if showCheckColumn>
<th class="nosort <#if multipleCheck>checkbox<#else>radio</#if>"><#if multipleCheck><input type="checkbox" class="checkall custom"/></#if></th>
</#if>
</#macro>

<#macro rttheadtd name,alias='',title='',cellname='',cellEdit='',class='',width='',readonly=false,resizable=true,excludeIfNotEdited=false>
<th<#if title?has_content> title="${action.getText(title)}"</#if><#if excludeIfNotEdited||class?has_content> class="<#if excludeIfNotEdited> excludeIfNotEdited</#if><#if class?has_content> ${class}</#if>"</#if><#if width?has_content> style="width:${width};"</#if> data-cellname="${cellname}"<#if cellEdit?has_content> data-cellEdit="${cellEdit}"</#if>>
<#if resizable><span class="resizeTitle"></#if><#if !alias?has_content><#local alias=name/><#if alias?index_of('.') gt 0><#local alias=alias?keep_after_last('.')/></#if></#if>${action.getText(alias)}<#if resizable></span><span class="resizeBar visible-desktop"></span></#if>
</th>
</#macro>
<#macro rtmiddle width='50px' showActionColumn=true>
<#if showActionColumn>
<th class="nosort" style="width:${width};"></th>
</#if>
</tr>
</thead>
<tbody>
</#macro>

<#macro rttbodytrstart entity showCheckColumn=true multipleCheck=true rowid='' dynamicAttributes...>
<#if !rowid?has_content>
	<#local id=(entity.id?string)!/>
<#else>
	<#local id><@rowid?interpret/></#local>
</#if>
<tr<#if entity.enabled??> data-enabled="${entity.enabled?string}"</#if><#if !showCheckColumn&&id?has_content> data-rowid="${id}"</#if><#list dynamicAttributes?keys as attr><#if attr=='dynamicAttributes'><#list dynamicAttributes['dynamicAttributes']?keys as attr> ${attr}="${dynamicAttributes['dynamicAttributes'][attr]?string}"</#list><#else> ${attr}="${dynamicAttributes[attr]?string}"</#if></#list>>
<#if showCheckColumn><td class="<#if multipleCheck>checkbox<#else>radio</#if>"><input type="<#if multipleCheck>checkbox<#else>radio</#if>"<#if id?has_content> value="${id}"</#if> class="custom"/></td></#if>
</#macro>

<#macro rttbodytd value,entity,celleditable=true,template='',cellDynamicAttributes='',dynamicAttributes...>
<#if cellDynamicAttributes?is_string>
	<#local _cellDynamicAttributes={}>
	<#if cellDynamicAttributes?has_content>
		<#local _cellDynamicAttributes><@cellDynamicAttributes?interpret /></#local>
		<#if _cellDynamicAttributes?has_content>
			<#local _cellDynamicAttributes=_cellDynamicAttributes?eval>
		<#else>
			<#local _cellDynamicAttributes={}>
		</#if>
	</#if>
	<#local cellDynamicAttributes=_cellDynamicAttributes>
</#if>
<#if dynamicAttributes['dynamicAttributes']??>
<#local dynamicAttributes+=dynamicAttributes['dynamicAttributes']>
</#if>
<#if cellDynamicAttributes['class']?? && dynamicAttributes['class']??>
<#local cellDynamicAttributes+={'class':dynamicAttributes['class']+' '+cellDynamicAttributes['class']}>
</#if>
<#local dynamicAttributes+=cellDynamicAttributes>
<td<#if value??><#if !dynamicAttributes['data-cellvalue']??&&template?has_content&&value?has_content||value?is_boolean> data-cellvalue="<#if value?is_unknown_date_like>${value?datetime?html}<#else>${value?string?html}</#if>"<#elseif value?is_hash&&value.displayName??> data-cellvalue="${value.name()?html}"</#if></#if><#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>><#rt>
<#if !template?has_content>
	<#if value??>
		<#if value?is_boolean>
		${action.getText(value?string)}<#t>
		<#elseif value?is_unknown_date_like>
		${value?datetime}<#t>
		<#elseif value?is_indexable>
		<#list value as var>${var?html}<#sep> </#list><#t>
		<#else>
		${value?html}<#t>
		</#if>
	</#if>
<#else>
	<@template?interpret/><#t>
</#if>
</td>
</#macro>

<#macro rttbodytrend entity showActionColumn=true buttons='' editable=true viewable=false entityReadonly=false inputWindowOptions='' viewWindowOptions=''>
<#if showActionColumn && (buttons?has_content || editable || viewable)>
<td class="action">
<#if buttons?has_content>
<@buttons?interpret/>
<#else>
<#if viewable>
<@btn view="view" windowoptions="${viewWindowOptions}"/>
</#if>
<#if editable && !entityReadonly>
<@btn view="input" label="edit" windowoptions="${inputWindowOptions}"/>
</#if>
<#if 'treeview'!=Parameters.view!&&treeable??&&treeable>
<@btn view="move"/>
<a class="btn ajax view" href="${actionBaseUrl}?parent=${entity.id}<#if tree??>&tree=${tree}</#if>">${action.getText("enter")}</a>
</#if>
</#if>
</td>
</#if>
</tr>
</#macro>

<#macro rtend showBottomButtons=true buttons='' readonly=false createable=true celleditable=true deletable=true enableable=false searchable=false filterable=true downloadable=true searchButtons='' showPageSize=true formFooter='' inputWindowOptions=''>
<#if filterable>
<#if !propertyNamesInCriteria?? && uiConfigs??>
<#local propertyNamesInCriteria=statics['org.ironrhino.core.struts.EntityClassHelper'].filterPropertyNamesInCriteria(uiConfigs)>
</#if>
<#if !propertyNamesInCriteria?? && entityClass??>
<#local propertyNamesInCriteria=statics['org.ironrhino.core.struts.EntityClassHelper'].getPropertyNamesInCriteria(entityClass)>
</#if>
<#local filterable=propertyNamesInCriteria??&&propertyNamesInCriteria?keys?size gt 0>
</#if>
</tbody>
</table>
<div class="toolbar row-fluid">
<div class="pagination span<#if showBottomButtons>4<#else>6</#if>">
<#if resultPage?? && resultPage.paginating>
<ul>
<#if resultPage.first>
<li class="disabled firstPage"><a title="${action.getText('firstpage')}"><i class="glyphicon glyphicon-fast-backward"></i></a></li>
<li class="disabled"><a title="${action.getText('previouspage')}"><i class="glyphicon glyphicon-step-backward"></i></a></li>
<#else>
<li class="firstPage"><a title="${action.getText('firstpage')}" href="${resultPage.renderUrl(1)}"><i class="glyphicon glyphicon-fast-backward"></i></a></li>
<li class="prevPage"><a title="${action.getText('previouspage')}" href="${resultPage.renderUrl(resultPage.previousPage)}"><i class="glyphicon glyphicon-step-backward"></i></a></li>
</#if>
<#if resultPage.last>
<li class="disabled"><a title="${action.getText('nextpage')}"><i class="glyphicon glyphicon-step-forward"></i></a></li>
<li class="disabled lastPage"><a title="${action.getText('lastpage')}"><i class="glyphicon glyphicon-fast-forward"></i></a></li>
<#else>
<li class="nextPage"><a title="${action.getText('nextpage')}" href="${resultPage.renderUrl(resultPage.nextPage)}"><i class="glyphicon glyphicon-step-forward"></i></a></li>
<li class="lastPage"><a title="${action.getText('lastpage')}" href="${resultPage.renderUrl(resultPage.totalPage)}"><i class="glyphicon glyphicon-fast-forward"></i></a></li>
</#if>
<li class="pageNo">
<span class="input-append">
    <input type="text" name="resultPage.pageNo" value="${resultPage.pageNo}" class="inputPage integer positive" title="${action.getText('currentpage')}"/><span class="add-on totalPage" title="${action.getText('totalpage')}">${resultPage.totalPage}</span>
</span>
<#if showPageSize>
<li class="visible-desktop">
<select name="resultPage.pageSize" class="pageSize" title="${action.getText('pagesize')}">
<#local array=[5,10,20,50,100,500]>
<#local selected=false>
<#list array as ps>
<option value="${ps}"<#if resultPage.pageSize==ps><#local selected=true> selected</#if>>${ps}</option>
</#list>
<#if resultPage.canListAll>
<option value="${resultPage.totalResults}"<#if !selected && resultPage.pageSize==resultPage.totalResults><#local selected=true> selected</#if>>${action.getText('all')}</option>
</#if>
<#if !selected !array?seq_contains(resultPage.pageSize)>
<option value="${resultPage.pageSize}" selected>${resultPage.pageSize}</option>
</#if>
</select>
</li>
</#if>
</ul>
</#if>
</div>
<#if showBottomButtons>
<div class="action span4">
<#if buttons?has_content>
<@buttons?interpret/>
<#else>
<#if !readonly>
<#if !(treeable?? && treeable && tree?? && tree gt 0 && (!parent??||parent lt 1))>
<#if createable><@btn view="input" label="create" windowoptions="${inputWindowOptions}"/></#if>
</#if>
<#if celleditable><@btn action="save" confirm=true/></#if>
<#if enableable>
<@btn action="enable" confirm=true/>
<@btn action="disable" confirm=true/>
</#if>
</#if>
<#if !readonly||deletable><button type="button" class="btn confirm" data-action="delete" data-shown="selected" data-filterselector="<#if enableable>[data-enabled='false']</#if>:not([data-deletable='false'])">${action.getText("delete")}</button></#if>
<#if 'treeview'!=Parameters.view!&&treeable??&&treeable&&parentEntity??>
<#if parentEntity.parent?? && (!tree??||parent!=tree)>
<a class="btn ajax view" href="${actionBaseUrl+"?parent="+parentEntity.parent.id}<#if tree??>&tree=${tree}</#if>">${action.getText("upward")}</a>
<#else>
<a class="btn ajax view" href="${actionBaseUrl}<#if tree??>?tree=${tree}</#if>">${action.getText("upward")}</a>
</#if>
</#if>
<@btn class="reload"/>
<#if filterable><@btn class="filter"/></#if>
</#if>
</div>
</#if>
<div class="search span<#if showBottomButtons>2<#else>3</#if>">
<#if searchable>
<span class="input-append search">
    <input type="text" name="keyword" value="${keyword!?html}" placeholder="${action.getText('search')}"/><span class="add-on"><i class="glyphicon glyphicon-search clickable"></i></span>
</span>
</#if>
<#if searchButtons?has_content>
<@searchButtons?interpret/>
<#else>
</#if>
</div>
<div class="status span<#if showBottomButtons>2<#else>3</#if>">
<#local totalResults=0/>
<#if resultPage?? && resultPage.totalResults gt 0>
<#local totalResults=resultPage.totalResults/>
<#elseif list?? && list?size gt 0>
<#local totalResults=list?size/>
</#if>
${totalResults}<span class="recordLabel"> ${action.getText('record')}</span>
<#if downloadable && request.requestURI?ends_with(actionBaseUrl) && totalResults gt 0 && totalResults lte (csvMaxRows!10000) && action.csv??>
<button type="submit" class="noajax plain" formaction="${actionBaseUrl}/csv" style="padding-top:2px;">
<span class="glyphicon glyphicon-download-alt clickable">
</span>
</button>
</#if>
</div>
</div>
${formFooter!}
</form>
<#if filterable>
<form method="post" class="ajax view criteria" style="display:none;">
<table class="table datagrid criteria">
	<tbody>
		<tr>
			<td style="width:30%;">
				<select class="decrease property">
					<option value=""></option>
					<#list propertyNamesInCriteria.entrySet() as entry>
					<#local label=entry.value.alias!/>
					<#if !label?has_content>
						<#local label=entry.key/>
						<#if label?index_of('.') gt 0>
							<#local label=label?keep_after_last('.')/>
						</#if>
					</#if>
					<#if entry.value.propertyType.enum>
					<option value="${entry.key}" data-class="${entry.value.cssClass}" data-type="select" data-map="${statics['org.ironrhino.core.struts.I18N'].getTextForEnum(entry.value.propertyType)}" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators(entry.value.propertyType)}">${statics['org.ironrhino.core.struts.I18N'].getText(label)}</option>
					<#elseif entry.value.type='dictionary' && selectDictionary??>
					<#assign templateName><@entry.value.templateName?interpret /></#assign>
					<option value="${entry.key}" data-class="${entry.value.cssClass}" data-type="select" data-map="${beans['dictionaryControl'].getItemsAsMap(templateName)}" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators('org.ironrhino.core.hibernate.CriterionOperator')}">${statics['org.ironrhino.core.struts.I18N'].getText(label)}</option>
					<#elseif entry.value.type='select'>
					<option value="${entry.key}" data-class="${entry.value.cssClass}" data-type="select" data-map="${entry.value.optionsExpression?eval}" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators('org.ironrhino.core.hibernate.CriterionOperator')}">${statics['org.ironrhino.core.struts.I18N'].getText(label)}</option>
					<#elseif entry.value.type='listpick'>
					<#assign pickUrl><@entry.value.pickUrl?interpret/></#assign>
					<option value="${entry.key}.id" data-type="listpick" data-pickurl="<@url value=pickUrl/>" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators(entry.value.propertyType)}">${statics['org.ironrhino.core.struts.I18N'].getText(label)}</option>
					<#else>
					<option value="${entry.key}" data-class="${entry.value.cssClass?replace('checkavailable','')}" data-inputtype="${entry.value.inputType}" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators(entry.value.propertyType)}">${statics['org.ironrhino.core.struts.I18N'].getText(label)}</option>
					</#if>
					</#list>
				</select>
			</td>
			<td style="width:30%;text-align:right;">
			<select class="operator">
			<#list statics['org.ironrhino.core.hibernate.CriterionOperator'].values() as op>
			<option value="${op.name()}" data-parameters="${op.parametersSize}">${op}</option>
			</#list>
			</select>
			</td>
			<td style="text-align:center;"></td>
			<td class="manipulate"></td>
		</tr>
	</tbody>
</table>
<table class="table datagrid ordering">
	<tbody>
		<tr>
			<td style="width:30%;">
				<select class="decrease property">
					<option value=""></option>
					<#list propertyNamesInCriteria.entrySet() as entry>
					<#if !entry.value.excludedFromOrdering>
					<#local label=entry.value.alias!/>
					<#if !label?has_content>
						<#local label=entry.key/>
						<#if label?index_of('.') gt 0>
							<#local label=label?keep_after_last('.')/>
						</#if>
					</#if>
					<option value="${entry.key}">${statics['org.ironrhino.core.struts.I18N'].getText(label)}</option>
					</#if>
					</#list>
				</select>
			</td>
			<td style="width:30%;text-align:right;">
			<select class="ordering">
			<option value="asc">${statics['org.ironrhino.core.struts.I18N'].getText('ascending')}</option>
			<option value="desc">${statics['org.ironrhino.core.struts.I18N'].getText('descending')}</option>
			</select>
			</td>
			<td style="text-align:center;"></td>
			<td class="manipulate"></td>
		</tr>
	</tbody>
	<tfoot>
		<tr>
			<td colspan="4" style="text-align:center;"><button type="submit" class="btn btn-primary">${statics['org.ironrhino.core.struts.I18N'].getText('search')}</button> <button type="button" class="btn restore">${statics['org.ironrhino.core.struts.I18N'].getText('restore')}</button></td>
		</tr>
	</tfoot>
</table>
</form>
</#if>
</#macro>

<#macro btn view='' action='' class='' label='' confirm=false windowoptions=''>
<#if windowoptions?has_content>
<#local windowoptions><@windowoptions?interpret/></#local>
<#local windowoptions=windowoptions?replace('"',"'")/>
</#if>
<#if class?has_content && !(view?has_content||action?has_content)><button type="button" class="btn ${class}">${statics['org.ironrhino.core.struts.I18N'].getText(label?has_content?string(label,class))}</button><#else><button type="button" class="btn ${class}<#if confirm&&action?has_content> confirm</#if>" data-<#if view?has_content>view="${view}"<#elseif action?has_content>action="${action}"</#if><#if action='delete'> data-shown="selected" data-filterselector=":not([data-deletable='false'])"<#elseif action='enable'> data-shown="selected" data-filterselector="[data-enabled='false']:not([data-readonly='true'])"<#elseif action='disable'> data-shown="selected" data-filterselector="[data-enabled='true']:not([data-readonly='true'])"</#if><#if view?has_content&&windowoptions?has_content> data-windowoptions="${windowoptions}"</#if>>${statics['org.ironrhino.core.struts.I18N'].getText(label?has_content?string(label,view?has_content?string(view,action)))}</button></#if>
</#macro>