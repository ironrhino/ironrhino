<#ftl output_format='HTML'>
<#assign view=Parameters.view!/>
<!DOCTYPE html>
<html>
<head>
<title>${getText('query')}</title>
<script>
$(function(){
	$(document).on('click','#result tbody .btn',function(){
		if(!$('#row-modal').length)
			var modal = $('<div id="row-modal" class="modal" style="width:800px;margin-left:-400px;z-index:10000;"><div class="modal-close"><button type="button" class="close" data-dismiss="modal" aria-hidden="true"/></div><div id="row-modal-body" class="modal-body" style="min-height:300px;max-height:initial;"></div></div>')
									.appendTo(document.body).fadeIn().find('.close').click(function() {
						$(this).closest('.modal').fadeOut().remove();
					});
		var ths = $('th:not(:first-child)',$(this).closest('table'));
		var tds = $('td:not(:first-child)',$(this).closest('tr'));
		var arr = [];
		arr.push('<div class="form-horizontal label-large">');
		for(var i=0;i<ths.length;i++){
			arr.push('<div class="control-group"><label class="control-label">');
			arr.push(ths.eq(i).text());
			arr.push('</label><div class="controls"><p>');
			arr.push(tds.eq(i).text());
			arr.push('</p></div></div>');
		}
		arr.push('</div>');
		$('#row-modal-body').html(arr.join(''));
	});
	
	$(document).on('change','#tables',function(){
		var t = $(this);
		var table = t.val();
		if(table){
			var textarea = $('textarea[name="sql"]',t.closest('form'));
			textarea.next('div.preview').click();
			if(!textarea.val())
				textarea.val('select * from '+table);
			else
				textarea.val(textarea.val()+' '+table);
			setTimeout(function(){
			var pos = textarea.val().length;
			var ta = textarea.get(0);
			if (ta.setSelectionRange) {
		    	ta.setSelectionRange(pos, pos);
		    } else if (ta.createTextRange) {
		    	var range = ta.createTextRange();
		    	range.collapse(true);
		    	range.moveEnd('character', pos);
		    	range.moveStart('character', pos);
		    	range.select();
		    }else{
		    	textarea.focus();
		    }
			},100);
		}
	});
	
});
</script>
</head>
<body>
<@s.form id="query-form" action="${actionBaseUrl}" method="post" class="form-horizontal label-large ajax view history">
	<#list Parameters as name,value>
	<#if name=='view'||name?ends_with('-type')>
	<input type="hidden" name="${name}" value="${value}" />
	</#if>
	</#list>
	<#if view=='embedded'>
	<@s.hidden name="sql"/>
	<#else>
	<div class="row<#if fluidLayout>-fluid</#if>">
	<#assign readonly=(view=='brief')&&sql?has_content>
	<div class="span10">
	<@s.textarea name="sql" class="required sqleditor codeblock conjunct" style="width:750px;" data\-replacement="params" readonly=readonly placeholder="select username,name,email from user where username=:username and createDate>:createDate/*--datetime--*/"/>
	</div>
	<div class="span2">
	<#if !readonly && tables?? && tables?size gt 0>
	<@s.select id="tables" theme="simple" class="chosen input-medium" list="tables" headerKey="" headerValue=""/>
	</div>
	</div>
	</#if>
	</#if>
	<div id="params">
	<#if params??>
	<#list params as var,type>
	<#if type=='date'||type=='datetime'||type=='time'>
	<@s.textfield id="param-"+var?index label=var name="paramMap['${var}']" class="conjunct-addition ${type}"/>
	<#elseif type=='int'||type=='integer'||type=='long'>
	<@s.textfield type="number" id="param-"+var?index label=var name="paramMap['${var}']" class="conjunct-addition ${type}"/>
	<#elseif type=='double'||type='decimal'>
	<@s.textfield type="number" id="param-"+var?index label=var name="paramMap['${var}']" class="conjunct-addition double" step="0.01"/>
	<#elseif type=='boolean'>
	<@s.select id="param-"+var?index label=var name="paramMap['${var}']" class="conjunct-addition" list={'true':getText('true'),'false':getText('false')}/>
	<#elseif type=='bit'>
	<@s.select id="param-"+var?index label=var name="paramMap['${var}']" class="conjunct-addition" list={'1':getText('true'),'0':getText('false')}/>
	<#elseif type=='textarea'>
	<@s.textarea id="param-"+var?index label=var name="paramMap['${var}']" class="conjunct-addition input-xxlarge" style="height:50px;"/>
	<#else>
	<@s.textfield id="param-"+var?index label=var name="paramMap['${var}']" class="conjunct-addition"/>
	</#if>
	</#list>
	</#if>
	</div>
	<@s.submit label=getText('submit') class="btn-primary"/>
	<#if resultPage??>
	<#if resultPage.result?size gt 0>
	<#assign map=resultPage.result[0]/>
	<div id="result">
		<table class="pin table table-hover table-striped table-bordered sortable filtercolumn resizable middle" style="white-space: nowrap;">
			<thead>
			<tr>
				<th class="nosort filtercolumn" style="width:50px;"></th>
				<#list map?keys as name>
				<th>${name}</th>
				</#list>
			</tr>
			</thead>
			<tbody>
			<#list resultPage.result as row>
			<tr>
				<td><button type="button" class="btn">${getText('view')}</button></td>
				<#list row as key,value>
				<td><#if value??><#if value.class.simpleName=='byte[]'>[...]<#else>${value?string}</#if></#if></td>
				</#list>
			</tr>
			</#list>
			</tbody>
		</table>
		<div class="toolbar row-fluid">
			<div class="span5">
				<#if resultPage.paginating>
				<div class="pagination">
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
				<li>
				<span class="input-append">
				    <input type="text" name="resultPage.pageNo" value="${resultPage.pageNo}" class="inputPage integer positive" title="${getText('currentpage')}"/><span class="add-on totalPage"><strong title="${getText('totalpage')}">${resultPage.totalPage}</strong></span>
				</span>
				<li class="visible-desktop">
				<select name="resultPage.pageSize" class="pageSize" title="${getText('pagesize')}">
				<#assign array=[5,10,20,50,100,500]>
				<#assign selected=false>
				<#list array as ps>
				<option value="${ps}"<#if resultPage.pageSize==ps><#assign selected=true> selected</#if>>${ps}</option>
				</#list>
				<#if resultPage.canListAll>
				<option value="${resultPage.totalResults}"<#if !selected && resultPage.pageSize==resultPage.totalResults> selected</#if>>${getText('all')}</option>
				</#if>
				</select>
				</li>
				</ul>
				</div>
				</#if>
			</div>
			<div class="action span2">
				<#assign downloadUrl=actionBaseUrl+'/export'>
				<#list Parameters as name,value>
				<#if name!='_'&&name!='pn'&&name!='ps'&&!name?starts_with('resultPage.')&&(name!='keyword'||value?has_content)>
				<#assign downloadUrl+=downloadUrl?contains('?')?then('&','?')+name+'='+value?url>
				</#if>
				</#list>
				<a target="_blank" download="data.csv" class="btn" href="${downloadUrl}">${getText('export')}</a>
			</div>
			<div class="status span5">
				${resultPage.totalResults} ${getText('record')} , ${getText('tookInMillis',[resultPage.tookInMillis])}
			</div>
		</div>
	</div>
	<#elseif resultPage.executed>
	<div class="alert">
	  <a class="close" data-dismiss="alert"></a>
	  <strong>${getText('query.result.empty')}</strong>
	</div>
	</#if>
	</#if>
</@s.form>
</body>
</html>