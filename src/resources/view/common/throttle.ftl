<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('throttle')}</title>
<style>
	h3 {
		margin-bottom: 20px;
	}
	form {
		margin: 0;
	}
	form span.label{
		display: inline-block;
		width: 100px;
		text-align: center;
	}
	li>div{
		line-height: 30px;
	}
	div.key{
		text-align: right;
	}
</style>
</head>
<body>
<#if beans['circuitBreakerRegistry']??>
<#assign circuitBreakers = beans['circuitBreakerRegistry'].circuitBreakers>
<#if circuitBreakers?size gt 0>
<h3 class="center">${getText('circuitBreaker')}</h3>
<ul class="thumbnails">
<#list circuitBreakers as key,value>
<li class="span6">
<div class="row-fluid">
<div class="key span5">${key}</div>
<div class="span7">
<@s.form id="cb-"+key?keep_before('(') action=actionBaseUrl method="post" class="form-inline ajax view">
<@s.hidden name="name" value=key/>
<#assign state=value.state>
<@s.hidden name="oldState" value=state/>
<span class="label ${state.name()?switch('CLOSED','label-success','DISABLED','label-info','label-warning')}">${state}</span>
 -->
<@s.select theme="simple" name="newState" class="input-medium" list="@io.github.resilience4j.circuitbreaker.CircuitBreaker$State@values()" value=state/>
<@s.submit theme="simple" label=getText('change')/>
</@s.form>
</div>
</div>
</li>
</#list>
</ul>
<hr/>
</#if>
</#if>

<#if beans['rateLimiterRegistry']??>
<#assign rateLimiters = beans['rateLimiterRegistry'].rateLimiters>
<#if rateLimiters?size gt 0>
<h3 class="center">${getText('rateLimiter')}</h3>
<ul class="thumbnails">
<#list rateLimiters as key,value>
<li class="span6">
<div class="row-fluid">
<div class="key span5">${key}</div>
<div class="span7">
<@s.form id="rl-"+key?keep_before('(') action=actionBaseUrl method="post" class="form-inline ajax view">
<@s.hidden name="name" value=key/>
<@s.hidden name="oldLimitForPeriod" value=value.rateLimiterConfig.limitForPeriod/>
<span class="badge badge-info">${value.rateLimiterConfig.limitForPeriod}</span>
 -->
<@s.textfield theme="simple" type="number" name="newLimitForPeriod" class="input-small integer positive" value=value.rateLimiterConfig.limitForPeriod/>
/${value.rateLimiterConfig.limitRefreshPeriod.toMillis()}ms
<@s.submit theme="simple" label=getText('change')/>
</@s.form>
</div>
</div>
</li>
</#list>
</ul>
<hr/>
</#if>
</#if>


<#if beans['bulkheadRegistry']??>
<#assign bulkheads = beans['bulkheadRegistry'].bulkheads>
<#if bulkheads?size gt 0>
<h3 class="center">${getText('bulkhead')}</h3>
<ul class="thumbnails">
<#list bulkheads as key,value>
<li class="span6">
<div class="row-fluid">
<div class="key span5">${key}</div>
<div class="span7">
<@s.form id="bh-"+key?keep_before('(') action=actionBaseUrl method="post" class="form-inline ajax view">
<@s.hidden name="name" value=key/>
<@s.hidden name="oldMaxConcurrentCalls" value=value.bulkheadConfig.maxConcurrentCalls/>
<span class="badge badge-info">${value.bulkheadConfig.maxConcurrentCalls}</span>
 -->
<@s.textfield theme="simple" type="number" name="newMaxConcurrentCalls" class="input-small integer positive" value=value.bulkheadConfig.maxConcurrentCalls/>
<@s.submit theme="simple" label=getText('change')/>
</@s.form>
</div>
</div>
</li>
</#list>
</ul>
<hr/>
</#if>
</#if>

</body>
</html>