<#macro richtable columns entityName='' formid='' action='' showActionColumn=true showBottomButtons=true actionColumnWidth='50px' actionColumnButtons='' bottomButtons='' rowid='' resizable=true sortable=true readonly=false readonlyExpression='' creatable=true createable=true viewable=false celleditable=true deletable=true enableable=false searchable=false filterable=true downloadable=true searchButtons='' includeParameters=true showQueryForm=false queryFormGridColumns=3 showPageSize=true showCheckColumn=true multipleCheck=true columnfilterable=true rowDynamicAttributes='' formHeader='' formFooter='' formCssClass=''>
<#if !createable><#local creatable=createable></#if>
<#if !entityName?has_content>
	<#local entityName=request.requestURI?keep_after_last('/')>
</#if>
<@rtstart formid=formid action=action entityName=entityName resizable=resizable sortable=sortable includeParameters=includeParameters showQueryForm=showQueryForm queryFormGridColumns=queryFormGridColumns showPageSize=showPageSize showCheckColumn=showCheckColumn multipleCheck=multipleCheck columnfilterable=columnfilterable formHeader=formHeader formCssClass=formCssClass>
<#nested/>
</@rtstart>
<#local size = columns?size>
<#list columns as name,config>
<#local cellname=((config['trimPrefix']??)?then('',entityName+'.'))+name>
<@rttheadtd name=name alias=config['alias']! description=config['description']! title=config['title']! class=config['thCssClass']! width=config['width']! cellname=cellname cellEdit=celleditable?then(config['cellEdit']!,'') readonly=readonly resizable=(readonly&&name?has_next||!readonly)&&resizable/>
</#list>
<#local showActionColumn=showActionColumn&&(actionColumnButtons?has_content||!readonly||viewable)/>
<@rtmiddle width=actionColumnWidth showActionColumn=showActionColumn/>
<#if resultPage??><#local list=resultPage.result></#if>
<#local sumColumns={}>
<#if list?size gt 0>
<#list columns as name,config>
	<#if config['showSum']?? && config['showSum']>
		<#local sumColumns+={name:{"template":config['template']!}}>
	</#if>
</#list>
</#if>
<#list list as entity>
<#list sumColumns as name,config>
	<#if !name?contains('.')><#local val=entity[name]!><#else><#local val=('(entity.'+name+')!')?eval></#if>
	<#if val?has_content>
	<#if val?is_number><#if config.value??><#local val+=config.value/></#if><#else><#local val=''/></#if>
	<#local sumColumns+={name:{"value":val,"template":config['template']!}}>
	</#if>
</#list>
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
<#list columns as name,config>
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
<@rtend columns=columns?keys sumColumns=sumColumns showCheckColumn=showCheckColumn showActionColumn=showActionColumn showBottomButtons=showBottomButtons buttons=bottomButtons readonly=readonly creatable=creatable celleditable=celleditable deletable=deletable enableable=enableable searchable=searchable filterable=filterable downloadable=downloadable searchButtons=searchButtons showPageSize=showPageSize formFooter=formFooter/>
</#macro>

<#macro rtstart formid='' action='' entityName='' resizable=true sortable=true includeParameters=true showPageSize=true showCheckColumn=true multipleCheck=true columnfilterable=true formHeader='' formCssClass='' showQueryForm=false queryFormGridColumns=3 dynamicAttributes...>
<#if showQueryForm>
<#if !propertyNamesInCriteria?? && uiConfigs??>
<#local propertyNamesInCriteria=statics['org.ironrhino.core.struts.EntityClassHelper'].filterPropertyNamesInCriteria(uiConfigs)>
</#if>
<#if !propertyNamesInCriteria?? && entityClass??>
<#local propertyNamesInCriteria=statics['org.ironrhino.core.struts.EntityClassHelper'].getPropertyNamesInCriteria(entityClass)>
</#if>
<@renderQueryForm propertyNamesInCriteria=propertyNamesInCriteria! gridColumns=queryFormGridColumns/>
</#if>
<#local parameterNamesInQueryString=[]>
<#if !action?has_content>
<#local action=request.requestURI>
<#if request.queryString?has_content>
<#list request.queryString?split('&') as pair>
	<#local name=pair?keep_before('=')>
	<#if name!='_'&&name!='pn'&&(!(showPageSize&&name=='ps'))&&!name?starts_with('resultPage.')&&name!='keyword'&&!formHeader?contains(' name="'+name+'" ')>
		<#local action+=action?contains('?')?then('&','?')+pair>
		<#local parameterNamesInQueryString+=[name]>
	</#if>
</#list>
</#if>
</#if>
<form id="<#if formid?has_content>${formid}<#else>${entityName}<#if Parameters.tab?? && Parameters[Parameters.tab]??>_${Parameters.tab+'_'+Parameters[Parameters.tab]}</#if>_form</#if>" action="${action}" method="post" class="richtable ajax view ${dynamicAttributes['class']!}<#if formCssClass?index_of('nohistory') lt 0 && 'treeview'!=Parameters.view!> history</#if> ${formCssClass}"<#if actionBaseUrl!=action&&!action?starts_with(actionBaseUrl+'?')> data-actionbaseurl="${actionBaseUrl}"</#if><@dynAttrs value=dynamicAttributes exclude='class'/>>
${formHeader!}
<#nested/>
<#if includeParameters>
<#list request.parameterMap as name,values>
<#if !parameterNamesInQueryString?seq_contains(name)&&name!='_'&&name!='pn'&&name!='ps'&&!name?starts_with('resultPage.')&&name!='keyword'&&!formHeader?contains(' name="'+name+'" ')>
<#list values as value>
<input type="hidden" name="${name}" value="${value}"/>
</#list>
</#if>
</#list>
</#if>
<table class="table table-hover table-striped table-bordered richtable<#if sortable> sortable</#if><#if columnfilterable> filtercolumn</#if><#if resizable> resizable</#if>">
<thead>
<tr>
<#if showCheckColumn>
<th class="nosort <#if multipleCheck>checkbox<#else>radio</#if>"><#if multipleCheck><input type="checkbox" class="checkall"/></#if></th>
</#if>
</#macro>

<#macro rttheadtd name,alias='',description='',title='',cellname='',cellEdit='',class='',width='',readonly=false,resizable=true>
<th<#if title?has_content> title="${getText(title)}"</#if><#if class?has_content> class="${class}"</#if><#if width?has_content> style="width:${width};"</#if> data-cellname="${cellname}"<#if cellEdit?has_content> data-celledit="${cellEdit}"</#if>>
<#if resizable><span class="resizeTitle"></#if><#if !alias?has_content><#local alias=name/><#if alias?index_of('.') gt 0><#local alias=alias?keep_after_last('.')/></#if></#if>${getText(alias)}<#if resizable></span><#if description?has_content> <span data-content="${getText(description)}" class="poped glyphicon glyphicon-question-sign"></span></#if><span class="resizeBar visible-desktop"></span></#if>
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
<tr<#if entity.enabled??> data-enabled="${entity.enabled?string}"</#if><#if !showCheckColumn&&id?has_content> data-rowid="${id}"</#if><@dynAttrs value=dynamicAttributes/>>
<#if showCheckColumn><td class="<#if multipleCheck>checkbox<#else>radio</#if>"><input type="<#if multipleCheck>checkbox<#else>radio</#if>"<#if id?has_content> value="${id}"</#if>/></td></#if>
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
<#if cellDynamicAttributes['class']?? && dynamicAttributes['class']??>
<#local cellDynamicAttributes+={'class':dynamicAttributes['class']+' '+cellDynamicAttributes['class']}>
</#if>
<#local dynamicAttributes+=cellDynamicAttributes>
<#local hasCellvalue=dynamicAttributes['data-cellvalue']??||dynamicAttributes['dynamicAttributes']??&&dynamicAttributes['dynamicAttributes']['data-cellvalue']??>
<td<#if value??><#if !hasCellvalue&&template?has_content&&value?has_content||value?is_boolean> data-cellvalue="<#if value?is_unknown_date_like>${value?datetime?html}<#else>${value?string?html}</#if>"<#elseif (value.ordinal)!?is_method&&(value.name)!?is_method&&value.name()!=value> data-cellvalue="${value.name()}"</#if></#if><@dynAttrs value=dynamicAttributes/>><#rt>
<#if !template?has_content>
	<#if value??>
		<#if value?is_boolean>
		${getText(value?string)}<#t>
		<#elseif value?is_unknown_date_like>
		${value?datetime}<#t>
		<#elseif value?is_enumerable>
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
<#if showActionColumn>
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
<a class="btn ajax view" href="${actionBaseUrl}?parent=${entity.id}<#if tree??>&tree=${tree}</#if>">${getText("enter")}</a>
</#if>
</#if>
</td>
</#if>
</tr>
</#macro>

<#macro rtend columns sumColumns={} showCheckColumn=true showActionColumn=true showBottomButtons=true buttons='' readonly=false creatable=true celleditable=true deletable=true enableable=false searchable=false filterable=true downloadable=true searchButtons='' showPageSize=true formFooter='' inputWindowOptions=''>
<#if filterable>
<#if !propertyNamesInCriteria?? && uiConfigs??>
<#local propertyNamesInCriteria=statics['org.ironrhino.core.struts.EntityClassHelper'].filterPropertyNamesInCriteria(uiConfigs)>
</#if>
<#if !propertyNamesInCriteria?? && entityClass??>
<#local propertyNamesInCriteria=statics['org.ironrhino.core.struts.EntityClassHelper'].getPropertyNamesInCriteria(entityClass)>
</#if>
<#local filterable=propertyNamesInCriteria??&&propertyNamesInCriteria?size gt 0>
</#if>
</tbody>
<#if sumColumns?keys?size gt 0>
<tfoot>
<tr>
<#if showCheckColumn><td class="center">∑</td></#if>
<#list columns as name>
<td><#if sumColumns[name]?? && sumColumns[name].value??><#if sumColumns[name].template?has_content><#local template=sumColumns[name].template><#else><#local template=r'${value}'></#if><#local value=sumColumns[name].value><@template?interpret/></#if></td>
</#list>
<#if showActionColumn><td></td></#if>
</tr>
</tfoot>
</#if>
</table>
<div class="toolbar row-fluid">
<div class="pagination span<#if showBottomButtons>4<#else>6</#if>">
<#if resultPage?? && resultPage.paginating && (showPageSize||resultPage.totalPage gt 1)>
<ul>
<#if resultPage.first>
<li class="disabled firstPage"><a title="${getText('firstpage')}"><i class="glyphicon glyphicon-fast-backward"></i></a></li>
<li class="disabled"><a title="${getText('previouspage')}"><i class="glyphicon glyphicon-step-backward"></i></a></li>
<#else>
<li class="firstPage"><a title="${getText('firstpage')}" href="${resultPage.renderUrl(1)}" rel="first"><i class="glyphicon glyphicon-fast-backward"></i></a></li>
<li class="prevPage"><a title="${getText('previouspage')}" href="${resultPage.renderUrl(resultPage.previousPage)}" rel="prev"><i class="glyphicon glyphicon-step-backward"></i></a></li>
</#if>
<#if resultPage.last>
<li class="disabled"><a title="${getText('nextpage')}"><i class="glyphicon glyphicon-step-forward"></i></a></li>
<li class="disabled lastPage"><a title="${getText('lastpage')}"><i class="glyphicon glyphicon-fast-forward"></i></a></li>
<#else>
<li class="nextPage"><a title="${getText('nextpage')}" href="${resultPage.renderUrl(resultPage.nextPage)}" rel="next"><i class="glyphicon glyphicon-step-forward"></i></a></li>
<li class="lastPage"><a title="${getText('lastpage')}" href="${resultPage.renderUrl(resultPage.totalPage)}" rel="last"><i class="glyphicon glyphicon-fast-forward"></i></a></li>
</#if>
<li class="pageNo">
<span class="input-append">
    <input type="text" name="resultPage.pageNo" value="${resultPage.pageNo}" class="inputPage integer positive" title="${getText('currentpage')}"/><span class="add-on totalPage" title="${getText('totalpage')}">${resultPage.totalPage}</span>
</span>
<#if showPageSize>
<li class="visible-desktop">
<select name="resultPage.pageSize" class="pageSize" title="${getText('pagesize')}">
<#local array=[5,10,20,50,100,500]>
<#local selected=false>
<#list array as ps>
<option value="${ps}"<#if resultPage.pageSize==ps><#local selected=true> selected</#if>>${ps}</option>
</#list>
<#if resultPage.canListAll>
<option value="${resultPage.totalResults}"<#if !selected && resultPage.pageSize==resultPage.totalResults><#local selected=true> selected</#if>>${getText('all')}</option>
</#if>
<#if !selected !array?seq_contains(resultPage.pageSize)>
<option value="${resultPage.pageSize}" selected>${resultPage.pageSize}</option>
</#if>
</select>
</li>
<#elseif !resultPage.defaultPageSize>
<input type="hidden" name="resultPage.pageSize" value="${resultPage.pageSize}"/>
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
<#if creatable><@btn view="input" label="create" windowoptions="${inputWindowOptions}"/></#if>
</#if>
<#if celleditable><@btn action="save" confirm=true/></#if>
<#if enableable>
<@btn action="enable" confirm=true/>
<@btn action="disable" confirm=true/>
</#if>
</#if>
<#if !readonly||deletable><button type="button" class="btn confirm" data-action="delete" data-shown="selected" data-filterselector="<#if enableable>[data-enabled='false']</#if>:not([data-deletable='false'])">${getText("delete")}</button></#if>
<#if 'treeview'!=Parameters.view!&&treeable??&&treeable&&parentEntity??>
<#if parentEntity.parent?? && (!tree??||parent!=tree)>
<a class="btn ajax view" href="${actionBaseUrl+"?parent="+parentEntity.parent.id}<#if tree??>&tree=${tree}</#if>" rel="up">${getText("upward")}</a>
<#else>
<a class="btn ajax view" href="${actionBaseUrl}<#if tree??>?tree=${tree}</#if>" rel="up">${getText("upward")}</a>
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
    <input type="text" name="keyword" value="${keyword!?html}" class="ignore-blank" placeholder="${getText('search')}"/><span class="add-on"><i class="glyphicon glyphicon-search clickable"></i></span>
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
${totalResults}<span class="recordLabel"> ${getText('record')}</span>
<#if downloadable && request.requestURI?ends_with(actionBaseUrl) && totalResults gt 0 && totalResults lte (csvMaxRows!10000) && action.csv??>
<#local downloadUrl=actionBaseUrl+'/csv'>
<#list request.parameterMap as name,values>
<#list values as value>
<#if name!='_'&&name!='pn'&&name!='ps'&&!name?starts_with('resultPage.')&&(name!='keyword'||value?has_content)>
<#local downloadUrl+=downloadUrl?contains('?')?then('&','?')+name+'='+value?url>
</#if>
</#list>
</#list>
<a download="data.csv" href="${downloadUrl}"><span class="glyphicon glyphicon-download-alt clickable"></span></a>
</#if>
</div>
</div>
${formFooter!}
</form>
<#if filterable>
<form method="post" class="ajax view criteria" style="display:none;">
<table class="table datagrid criteria center">
	<tbody>
		<tr>
			<td class="center" style="width:38%;">
				<select class="decrease property">
					<option value=""></option>
					<#list propertyNamesInCriteria as key,value>
					<#local label=value.alias!/>
					<#if !label?has_content>
						<#local label=key/>
						<#if label?index_of('.') gt 0>
							<#local label=label?keep_after_last('.')/>
						</#if>
					</#if>
					<#if value.propertyType.enum>
					<option value="${key}" data-class="${value.cssClass}" data-type="select" data-map="{<#list statics['org.ironrhino.core.util.EnumUtils'].enumToMap(value.propertyType) as key,value>${key}=${value}<#sep>,</#list>}" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators(value.genericPropertyType)?join(',')}">${getText(label)}</option>
					<#elseif value.type='dictionary' && selectDictionary??>
					<#local templateName><@value.templateName?interpret /></#local>
					<option value="${key}" data-class="${value.cssClass}" data-type="select" data-map="{<#list beans['dictionaryControl'].getItemsAsMap(templateName) as key,value>${key}=${value}<#sep>,</#list>}" data-operators="<#if value.multiple>CONTAINS,NOTCONTAINS<#else>EQ,NEQ,ISNOTNULL,ISNULL,ISNOTEMPTY,ISEMPTY,IN,NOTIN</#if>">${getText(label)}</option>
					<#elseif value.type='select'>
					<#local options=value.listOptions?eval>
					<option value="${key}" data-class="${value.cssClass}" data-type="select" data-map="{<#if value.listOptions?starts_with('{')><#list options as key,value>${key}=${value}<#sep>,</#list><#elseif value.listOptions?starts_with('[')><#list options as key>${key}=${key}<#sep>,</#list></#if>}" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators(value.genericPropertyType)?join(',')}">${getText(label)}</option>
					<#elseif value.type='listpick'||value.type='treeselect'>
					<#local pickUrl><@value.pickUrl?interpret/></#local>
					<option value="${key}" data-type="${value.type}" data-pickurl="<@url value=pickUrl/>" data-operators="<#if value.multiple&&value.reference>CONTAINS<#else>${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators(value.genericPropertyType)?join(',')}</#if>">${getText(label)}</option>
					<#else>
					<option value="${key}" data-class="${value.cssClass}" data-inputtype="${value.inputType}" data-operators="${statics['org.ironrhino.core.hibernate.CriterionOperator'].getSupportedOperators(value.genericPropertyType)?join(',')}">${getText(label)}</option>
					</#if>
					</#list>
				</select>
			</td>
			<td class="center" style="width:24%;">
			<select class="operator">
			<#list statics['org.ironrhino.core.hibernate.CriterionOperator'].values() as op>
			<option value="${op.name()}" data-parameters="${op.parametersSize}">${op}</option>
			</#list>
			</select>
			</td>
			<td class="center"></td>
			<td class="manipulate" style="width:5%;text-align:right;"></td>
		</tr>
	</tbody>
</table>
<table class="table datagrid ordering">
	<tbody>
		<tr>
			<td class="center" style="width:38%;">
				<select class="decrease property">
					<option value=""></option>
					<#list propertyNamesInCriteria as key,value>
					<#if !value.excludedFromOrdering>
					<#local label=value.alias!/>
					<#if !label?has_content>
						<#local label=key/>
						<#if label?index_of('.') gt 0>
							<#local label=label?keep_after_last('.')/>
						</#if>
					</#if>
					<option value="${key}">${getText(label)}</option>
					</#if>
					</#list>
				</select>
			</td>
			<td class="center" style="width:24%;">
			<select class="ordering">
			<option value="asc">${getText('ascending')}</option>
			<option value="desc">${getText('descending')}</option>
			</select>
			</td>
			<td class="center"></td>
			<td class="manipulate" style="width:5%;text-align:right;"></td>
		</tr>
	</tbody>
	<tfoot>
		<tr>
			<td colspan="4" class="center"><button type="submit" class="btn btn-primary">${getText('search')}</button> <button type="button" class="btn restore">${getText('restore')}</button></td>
		</tr>
	</tfoot>
</table>
</form>
</#if>
</#macro>

<#macro btn view='' action='' class='' label='' confirm=false windowoptions='' dynamicAttributes...>
<#if windowoptions?has_content>
<#local windowoptions><@windowoptions?interpret/></#local>
<#local windowoptions=windowoptions?replace('"',"'")/>
</#if>
<#if class?has_content && !(view?has_content||action?has_content)><button type="<#if class='reload'>submit<#else>button</#if>" class="btn ${class}"<@dynAttrs value=dynamicAttributes/>>${getText(label?has_content?then(label,class))}</button><#else><button type="button" class="btn ${class}<#if confirm&&action?has_content> confirm</#if>" data-<#if view?has_content>view="${view}"<#elseif action?has_content>action="${action}"</#if><#if action='delete'> data-shown="selected" data-filterselector=":not([data-deletable='false'])"<#elseif action='enable'> data-shown="selected" data-filterselector="[data-enabled='false']:not([data-readonly='true'])"<#elseif action='disable'> data-shown="selected" data-filterselector="[data-enabled='true']:not([data-readonly='true'])"</#if><#if view?has_content&&windowoptions?has_content> data-windowoptions="${windowoptions}"</#if><@dynAttrs value=dynamicAttributes/>>${getText(label?has_content?then(label,view?has_content?then(view,action)))}</button></#if>
</#macro>

<#function mergeDynAttrs config>
	<#local dynamicAttributes={}/>
	<#if config.internalDynamicAttributes?has_content>
		<#local dynamicAttributes+=config.internalDynamicAttributes/>
	</#if>
	<#if config.dynamicAttributes?has_content>
		<#local da><@config.dynamicAttributes?interpret/></#local>
		<#local da=da?eval>
		<#local dynamicAttributes+=da/>
	</#if>
	<#return dynamicAttributes>
</#function>

<#macro controlGroup label description for="" id="" group="">
<div<#if id?has_content> id="control-group-${id}"</#if> class="control-group"<#if group?has_content> data-group="${group}"</#if>>
	<@controlLabel label=label description=description for=for/>
	<div class="controls">
	<#nested>
	</div>
</div>
</#macro>

<#macro controlLabel label description for="">
<label class="control-label"<#if for?has_content> for="${for}"</#if>><#if description?has_content><span data-content="${description}" class="poped glyphicon glyphicon-question-sign"></span> </#if>${label}</label>
</#macro>

<#function getParameterNamesInQueryString>
<#local parameterNamesInQueryString=[]>
<#if request.queryString?has_content>
<#list request.queryString?split('&') as pair>
	<#local pname=pair?keep_before('=')>
	<#if entityName?has_content&&pname?starts_with(entityName+'.')>
	<#local pname=pname?keep_after('.')>
	</#if>
	<#if pname?index_of('.') gt 0>
	<#local pname=pname?keep_before('.')>
	</#if>
	<#local parameterNamesInQueryString+=[pname]>
</#list>
</#if>
<#return parameterNamesInQueryString>
</#function>

<#macro renderQueryForm propertyNamesInCriteria gridColumns=3>
<#if propertyNamesInCriteria?has_content>
<#local parameterNamesInQueryString=getParameterNamesInQueryString()>
<form method="post" class="ajax view form-horizontal groupable query ignore-blank" data-columns="${gridColumns}">
	<#list propertyNamesInCriteria as key,config>
		<#local templateName><@config.templateName?interpret/></#local>
		<#local pickUrl><@config.pickUrl?interpret/></#local>
		<#local label=key>
		<#if config.alias?has_content>
			<#local label=config.alias>
		</#if>
		<#local label=getText(label)>
		<#local group=getText(config.group)>
		<#local description=getText(config.description)>
		<#local id='query-'+(config.id?has_content)?then(config.id,(entityName!)+'-'+key)/>
		<#local dynamicAttributes=mergeDynAttrs(config)/>
		<#local disabled=parameterNamesInQueryString?seq_contains(key)>
		<#local cssClass=config.cssClass>
		<#if !config.excludedFromQuery>
		<#if config.multiple>
			<@s.hidden name=key+'-op' value="CONTAINS"/>
		</#if>
		<#if config.type=='checkbox'>
			<@s.select disabled=disabled id=id label=label name=key value=(Parameters[key]!) class=cssClass list={'true':getText('true'),'false':getText('false')} headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='enum'>
			<@s.select disabled=disabled id=id label=label name=key value=(Parameters[key]!) class=cssClass list="@${config.propertyType.name}@values()" listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='select'>
			<@s.select disabled=disabled id=id label=label name=key value=(Parameters[key]!) class=cssClass list=config.listOptions?eval listKey=config.listKey listValue=config.listValue headerKey="" headerValue="" dynamicAttributes=dynamicAttributes/>
		<#elseif config.type=='treeselect' && !disabled>
			<@s.textfield id=id label=label name=key value=(Parameters[key]!) class='treeselect-inline '+cssClass data\-url=pickUrl dynamicAttributes=dynamicAttributes/>
		<#elseif (config.type=='listpick' || config.type=='treeselect') && !disabled>
			<div id="control-group-${id}" class="control-group"<#if group?has_content> data-group="${group}"</#if>>
				<@controlLabel label=label description=description for=id/>
				<div class="controls ${config.type}" data-options="{'url':'<@url value=pickUrl/>'}">
				<@s.hidden id=id name=key class=config.type+"-id ${cssClass}" dynamicAttributes=dynamicAttributes/>
				<div class="${config.type}-name input-pseudo"></div>
				</div>
			</div>
		<#elseif config.type=='dictionary' && selectDictionary??>
			<@controlGroup id=id group=group label=label description=description for=id>
				<@selectDictionary disabled=disabled id=id dictionaryName=templateName name=key value=(Parameters[key]!) class=cssClass dynamicAttributes=dynamicAttributes/>
			</@controlGroup>
		<#elseif config.type=='input'>
			<#if !disabled && config.queryWithRange><#local cssClass+=' not-ignore-blank'/></#if>
			<@s.textfield disabled=disabled id=id label=label name=key value=(Parameters[key]!) type=config.inputType class=cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" dynamicAttributes=dynamicAttributes>
			<#if !disabled>
				<#if config.queryMatchMode?? && config.propertyType.simpleName=='String' && 'EXACT'!=(config.queryMatchMode.name())!>
				<@s.param name='after'>
				<#local opname=config.queryMatchMode.name()>
				<@s.hidden name=key+'-op' value=(opname=='ANYWHERE')?then('INCLUDE',opname)/>
				</@s.param>
				<#elseif config.queryWithRange>
				<@s.param name='after'>
				<@s.hidden name=key+'-op' value='BETWEEN'/>
				- <@s.textfield theme="simple" label=label name=key value=(Parameters[key]!) type=config.inputType class=cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" dynamicAttributes=dynamicAttributes/>
				</@s.param>
				</#if>
			<#else>
				<#if (Parameters[key+'-op']!)=='BETWEEN'>
				<@s.param name='after'>
				- <@s.textfield theme="simple" disabled=true name=key value=(request.parameterMap[key][1]!) type=config.inputType class=cssClass maxlength="${(config.maxlength gt 0)?then(config.maxlength,'')}" dynamicAttributes=dynamicAttributes/>
				</@s.param>
				</#if>
			</#if>
			</@s.textfield>
		</#if>
		</#if>
	</#list>
	<@s.submit label=getText('query') class="btn-primary">
		<@s.param name="after"> <input type="reset" class="btn" value="${getText('reset')}"></@s.param>
	</@s.submit>
</form>
</#if>
</#macro>