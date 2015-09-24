<#macro renderTR node>
<tr id="node-${node.id}"<#if node.parent??&&node.parent.id gt 0> class="child-of-node-${node.parent.id}"</#if>>
        <td>${node.name}</td>
        <td <#if node.level gt 1>style="padding-left:${(node.level-1)*19}px"</#if>><#if node.value.longValue gt 0><a href="monitor/chart/${node.key?string}?vtype=l<#if request.queryString??>&${request.queryString}</#if>" target="_blank">${node.value.longValue}</a><span class="pull-right">${node.longPercent!}<#if node.value.longValue?? && !node.leaf && node.children.size() lte 10> <a href="monitor/pie/${node.key?string}?vtype=l<#if request.queryString??>&${request.queryString}</#if>" target="_blank"><span class="glyphicon glyphicon-picture clickable"></span></a></#if></span></#if></td>
        <td <#if node.level gt 1>style="padding-left:${(node.level-1)*19}px"</#if>><#if node.value.doubleValue gt 0><a href="monitor/chart/${node.key?string}?vtype=d<#if request.queryString??>&${request.queryString}</#if>" target="_blank">${node.value.doubleValue}</a><span class="pull-right">${node.doublePercent!}<#if node.value.doubleValue?? && !node.leaf && node.children.size() lte 10> <a href="monitor/pie/${node.key?string}?vtype=d<#if request.queryString??>&${request.queryString}</#if>" target="_blank"><span class="glyphicon glyphicon-picture clickable"></span></a></#if></span></#if></td>
</tr>
<#if node.leaf>
	<#return>
<#else>
<#list node.children as var>
	<@renderTR var/>
</#list>
</#if>
</#macro>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('monitor')}</title>
</head>
<body>
<div class="row">
<div class="span6">
<form class="ajax view form-inline" data-replacement="result">
<span>${action.getText('date')}</span>
<@s.textfield label="%{getText('date')}" theme="simple" name="date" class="date"/>
<@s.submit value="%{getText('query')}" theme="simple"/>
</form>
</div>
<div class="span6">
<form class="ajax view form-inline" data-replacement="result">
<span>${action.getText('date')}${action.getText('range')}</span>
<@s.textfield label="%{getText('from')}" theme="simple" name="from" class="date"/>
<i class="glyphicon glyphicon-arrow-right"></i>
<@s.textfield label="%{getText('to')}" theme="simple" name="to" class="date"/>
<@s.submit value="%{getText('query')}" theme="simple"/>
</form>
</div>
</div>
<div id="result"<#if Parameters.live??> class="ajaxpanel" data-interval="${Parameters.interval?default('60000')}" data-quiet="true"</#if>>
<#list result.entrySet() as entry>
<table class="treeTable expanded table table-hover table-bordered sortable" style="width:100%;">
  <#if entry.key??>
  <caption><h3>${entry.key}</h3></caption>
  </#if>
  <thead>
    <tr>
      <th class="nosort">${action.getText('key')}</th>
      <th style="width:20%;"></th>
      <th style="width:20%;"></th>
    </tr>
  </thead>
  <tbody>
    <#list entry.value as var>
      <@renderTR var/>
    </#list>
  </tbody>
</table>
</#list>
</div>
</body>
</html></#escape>
