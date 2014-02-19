<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('service')}${action.getText('console')}</title>
<script>
$(function(){
$('.service').click(function(){
	var t = $(this);
	if(t.next('ul').length){
		t.next('ul').remove();
	}else{
		var url = '${actionBaseUrl}/hosts/'+t.text();
		$.getJSON(url,function(data){
			if(!data.length){
				alert('no active providers for this service');
				return;
			}
			var ul = $('<ul class="thumbnails"/>').insertAfter(t);
			$.each(data,function(i,v){
				$('<li class="span2"/>').appendTo(ul).html('<a class="host" href="#">'+v+'</a>');
			});
		});
	}
});
$(document).on('click','a.host',function(e){
	$('#discovered-services').remove();
	var t = $(this);
	var url = '${actionBaseUrl}/services/'+t.text();
	$.getJSON(url,function(data){
		if(!$('#discovered-services').length)
			$('<div id="discovered-services"/>').insertAfter($('hr'));
		var ul = $('<ul class="unstyled"/>').appendTo($('#discovered-services'));
		$.each(data,function(k,v){
			$('<li/>').appendTo(ul).html(k+'<a class="host" href="#" style="margin-left:20px;">'+v+'</a>');
		});	
		if(!$('#discovered-services li').length){
			$('#discovered-services').remove();
			alert('no discovered services');
		}
	});
	return false;
});
});
</script>
</head>
<body>


<#assign services = serviceRegistry.getAllServices()>
<#if services?size gt 0>
<div id="services">
	<ul class="thumbnails">
	<#list services as service>
	<li class="span6">
	<button type="button" class="btn btn-block service">${service}</button>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>
</body>
</html></#escape>