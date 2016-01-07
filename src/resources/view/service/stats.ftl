<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('stats')}</title>
<style>
form.form-inline{
	margin-bottom: 0;
}
.row-fluid strong{
	font-size: 18px;
}
div.section{
	border: 1px solid #DEDEDE;
    border-radius: 5px;
    margin: 10px 4px;
    box-shadow: 0 0 10px rgba(189, 189, 189, 0.4);
	padding: 10px;
	margin: 20px 0;
}
#count:empty{
	display: none;
}
div.section ul{
	margin: 0;
}
</style>
<script src="<@url value="/assets/components/flot/jquery.flot.js"/>" type="text/javascript"></script>
<script src="<@url value="/assets/components/flot/jquery.flot.time.js"/>" type="text/javascript"></script>
<script src="<@url value="/assets/components/flot/ironrhino.flot.js"/>" type="text/javascript"></script>
<script>
function toggleUrl(url,clientSide){
	if (url.indexOf('?') > 0) {
		var uri = url.substring(0, url.indexOf('?'));
		var query = url.substring(url.indexOf('?') + 1);
		var params = query.split('&');
		var arr = [];
		for (var i = 0; i < params.length; i++) {
			var arr2 = params[i].split('=', 2);
			if (arr2[0] == 'clientSide') {
				continue;
			} else {
				arr.push(params[i]);
			}
		}
		arr.push('clientSide=' + clientSide);
		url = uri + '?' + arr.join('&');
	} else {
		url += '?clientSide=' + clientSide;
	}
	return url;
}
Initialization.stats = function() {
	$('#side button').click(function() {
				var clientSide = $(this).data('side')=='client';
				$('a.ajax.view').each(function(){
					this.href = toggleUrl(this.href,clientSide);
				});
				$('form.ajax.view').each(function(){
					this.action = toggleUrl(this.action,clientSide);
				});
				$('.ajaxpanel').each(function() {
					var t = $(this);
					t.data('url', toggleUrl(t.data('url'),clientSide));
					t.trigger('load');
				});
			});
}
</script>
</head>
<body>

<div class="row">
	<div id="side" class="btn-group btn-switch span2 offset5" style="margin-bottom:10px;">
	  <button class="btn<#if !clientSide> active</#if>" data-side="server">Server Side</button>
	  <button class="btn<#if clientSide> active</#if>" data-side="client">Client Side</button>
	</div>
</div>

<div class="accordion" id="services-accordion">
<#assign baseurl=actionBaseUrl>
<#if request.queryString?has_content>
<#list request.queryString?split('&') as pair>
	<#assign name=pair?keep_before('=')>
	<#if name!='_'&&name!='serviceName'&&name!='method'>
		<#assign baseurl+=baseurl?contains('?')?then('&','?')+pair>
	</#if>
</#list>
</#if>
<#list services.entrySet() as entry>	
  <div class="accordion-group">
    <div class="accordion-heading">
      <a class="accordion-toggle" data-toggle="collapse" data-parent="#services-accordion" href="#service-${entry?index}">
     	<h4>${entry.key}</h4>
      </a>
    </div>
    <div id="service-${entry?index}" class="accordion-body collapse<#if serviceName?? && serviceName==entry.key> in</#if>">
      <div class="accordion-inner">
        <ul class="nav nav-list">
        	<#list entry.value as var>
			<li<#if serviceName?? && serviceName==entry.key && method?? && method==var> class="active"</#if>><#assign href=baseurl+baseurl?contains('?')?then('&','?')+'serviceName='+entry.key?url+'&method='+var?url/><a href="<@url value="${href}"/>" class="ajax view" data-replacement="count">${var}</a></li>
			</#list>
		</ul>
      </div>
    </div>
  </div>
 </#list> 
 </div> 
		  

<div id="count" class="section count"><#if serviceName?has_content && method?has_content>
<div class="row-fluid">
<div class="span12">
<strong>${serviceName}.${method}</strong>
</div>
</div>
<div class="row-fluid">
<div class="span6">
<#assign baseaction=actionBaseUrl>
<#if request.queryString?has_content>
<#list request.queryString?split('&') as pair>
	<#assign name=pair?keep_before('=')>
	<#if name!='_'&&name!='date'&&name!='from'&&name!='to'>
		<#assign baseaction+=baseaction?contains('?')?then('&','?')+pair>
	</#if>
</#list>
</#if>
<form action="${baseaction}" class="ajax view form-inline" data-replacement="count_result">
<span>${action.getText('date')}</span>
<@s.textfield label="%{getText('date')}" theme="simple" id="" name="date" class="date"/>
<@s.submit value="%{getText('query')}" theme="simple"/>
</form>
</div>
<div class="span6">
<form action="${baseaction}" class="ajax view form-inline" data-replacement="count_result">
<span>${action.getText('date')}${action.getText('range')}</span>
<@s.textfield label="%{getText('from')}" theme="simple" id="" name="from" class="date"/>
<i class="glyphicon glyphicon-arrow-right"></i>
<@s.textfield label="%{getText('to')}" theme="simple" id="" name="to" class="date"/>
<@s.submit value="%{getText('query')}" theme="simple"/>
</form>
</div>
</div>
<div id="count_result">
<#assign dataurl=actionBaseUrl+"/count"/>
<#if request.queryString?has_content>
<#assign dataurl+='?'+request.queryString/>
</#if>
<div class="ajaxpanel" data-url="${dataurl}"></div>
</div>
</#if></div>



</body>
</html></#escape>
