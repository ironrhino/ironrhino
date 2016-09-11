<#macro pagination align="center" theme="" dynamicAttributes...>
<#if resultPage.totalPage gt 1>
<#if dynamicAttributes['dynamicAttributes']??>
	<#local dynamicAttributes+=dynamicAttributes['dynamicAttributes']/>
</#if>
<#if theme=="simple">
<ul class="pager">
  <li class="previous<#if resultPage.first> disabled</#if>">
  	<#if resultPage.first>
    <span>&larr; ${action.getText('previouspage')}</span>
    <#else>
    <a href="${resultPage.renderUrl(resultPage.previousPage)}"<@dynAttrs value=dynamicAttributes/>>&larr; ${action.getText('previouspage')}</a>
    </#if>
  </li>
  <li class="next<#if resultPage.last> disabled</#if>">
  	<#if resultPage.last>
    <span>${action.getText('nextpage')} &rarr;</span>
    <#else>
    <a href="${resultPage.renderUrl(resultPage.nextPage)}"<@dynAttrs value=dynamicAttributes/>>${action.getText('nextpage')} &rarr;</a>
    </#if>
  </li>
</ul>
<#else>
<div class="pagination<#if align="center"> pagination-centered<#elseif align="right"> pagination-right</#if>">
<ul>
<#if resultPage.first>
<li class="disabled"><a title="${action.getText('firstpage')}">&lt;&lt;</a></li>
<li class="disabled"><a title="${action.getText('previouspage')}">&lt;</a></li>
<#else>
<li><a href="${resultPage.renderUrl(1)}" title="${action.getText('firstpage')}"<@dynAttrs value=dynamicAttributes/>>&lt;&lt;</a></li>
<li><a href="${resultPage.renderUrl(resultPage.previousPage)}" title="${action.getText('previouspage')}"<@dynAttrs value=dynamicAttributes/>>&lt;</a></li>
</#if>
<#if resultPage.totalPage lt 11>
<#list 1..resultPage.totalPage as index>
<li<#if index==resultPage.pageNo> class="active"</#if>><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>
<#else>
<#if resultPage.pageNo lt 6>
<#list 1..(resultPage.pageNo+2) as index>
<li<#if index==resultPage.pageNo> class="active"</#if>><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>
<li class="disabled"><a>...</a></li>
<#list (resultPage.totalPage-1)..resultPage.totalPage as index>
<li><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>
<#elseif resultPage.pageNo gt resultPage.totalPage-5>
<#list 1..2 as index>
<li><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>
<li class="disabled"><a>...</a></li>
<#list (resultPage.pageNo-2)..resultPage.totalPage as index>
<li<#if index==resultPage.pageNo> class="active"</#if>><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>        
<#else>
<#list 1..2 as index>
<li><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>
<li class="disabled"><a>...</a></li>
<#list (resultPage.pageNo-2)..(resultPage.pageNo+2) as index>
<li<#if index==resultPage.pageNo> class="active"</#if>><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>
<li class="disabled"><a>...</a></li>
<#list (resultPage.totalPage-1)..resultPage.totalPage as index>
<li><a href="${resultPage.renderUrl(index)}"<@dynAttrs value=dynamicAttributes/>>${index}</a></li>
</#list>
</#if>
</#if>
<#if resultPage.last>
<li class="disabled"><a title="${action.getText('nextpage')}">&gt;</a><li>
<li class="disabled"><a title="${action.getText('lastpage')}">&gt;&gt;</a></li>
<#else>
<li><a href="${resultPage.renderUrl(resultPage.nextPage)}" title="${action.getText('nextpage')}"<@dynAttrs value=dynamicAttributes/>>&gt;</a></li>
<li><a href="${resultPage.renderUrl(resultPage.totalPage)}" title="${action.getText('lastpage')}"<@dynAttrs value=dynamicAttributes/>>&gt;&gt;</a></li>
</#if>
</ul>
</div>
</#if>
</#if>
</#macro>