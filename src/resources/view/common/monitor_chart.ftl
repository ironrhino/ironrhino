<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('monitor')}</title>
</head>
<body>
<div class="row">
<div class="span6">
<form action="${actionBaseUrl}/chart/${uid}" class="ajax view form-inline" data-replacement="c">
<@s.hidden name="vtype"/>
<@s.hidden name="ctype"/>
<span>${action.getText('date')}</span>
<@s.textfield label="%{getText('date')}" theme="simple" name="date" cssClass="date"/>
<@s.submit value="%{getText('query')}" theme="simple"/>
</form>
</div>
<div class="span6">
<form action="${actionBaseUrl}/chart/${uid}" class="ajax view form-inline" data-replacement="c">
<@s.hidden name="vtype"/>
<@s.hidden name="ctype"/>
<span>${action.getText('date.range')}</span>
<@s.textfield label="%{getText('from')}" theme="simple" name="from" cssClass="date"/>
<i class="glyphicon glyphicon-arrow-right"></i>
<@s.textfield label="%{getText('to')}" theme="simple" name="to" cssClass="date"/>
<@s.submit value="%{getText('query')}" theme="simple"/>
</form>
</div>
</div>
<div id="c">
<#assign dataurl='${actionBaseUrl}/data'/>
<#if uid??>
<#assign dataurl=dataurl+'/'+uid>
</#if>
<#if request.queryString??>
<#assign dataurl=dataurl+'?'+request.queryString>
</#if>
<div id="chart" class="chart" data-url="<@url value="${dataurl}"/>" style="width:1024px; height:300px;"<#if Parameters.live??> data-interval="${Parameters.interval?default('60000')}" data-quiet="true"</#if>>
</div>
</div>
</body>
</html></#escape>
