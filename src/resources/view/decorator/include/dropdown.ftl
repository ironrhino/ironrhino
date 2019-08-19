<ul class="dropdown-menu">
	<#if frozenLayout??&&!frozenLayout>
	<li><div style="padding: 3px 20px;">${getText('layout.change')}</div></li>
	<li>
	<div class="btn-group layout-change" style="padding-left: 12px;">
		<button class="btn<#if fluidLayout&&!sidebarLayout> active</#if>">A</button>
		<button class="btn<#if fluidLayout&&sidebarLayout> active</#if>">B</button>
		<button class="btn<#if !fluidLayout&&!sidebarLayout> active</#if>">C</button>
		<button class="btn<#if !fluidLayout&&sidebarLayout> active</#if>">D</button>
	</div>
	</li>
	<li class="divider"></li>
	</#if>
	<@resourcePresentConditional value="resources/view/audit.ftl">
	<li><a href="<@url value="/audit"/>" class="ajax view">${getText('auditEvent')}</a></li>
	<li class="divider"></li>
	</@resourcePresentConditional>
	<#assign divider=false/>
	<#if user.isNew??>
	<li><a href="<@url value="${ssoServerBase!}/${user.class.simpleName?uncap_first}/profile"/>" class="popmodal nocache">${getText('profile')}</a></li>
	<#if !user.getAttribute?? || !user.getAttribute('oauth_provider')??>
	<#assign passwordUrl=getUrl('/password')>
	<#if request.getAttribute('SSO')??><#assign passwordUrl=properties['portal.baseUrl']+'/password'><#elseif ssoServerBase?has_content><#assign passwordUrl=ssoServerBase!+'/password'></#if>
	<li><a href="${passwordUrl}" class="popmodal">${getText('change')}${getText('password')}</a></li>
	</#if>
	<#assign divider=true/>
	</#if>
	<#if !request.getAttribute("javax.servlet.request.X509Certificate")?? && beans['logoutSuccessHandler']??>
	<#if divider><li class="divider"></li></#if>
	<#assign logoutUrl=getUrl('/logout')>
	<#if request.getAttribute('SSO')??><#assign logoutUrl=properties['portal.baseUrl']+'/logout'><#elseif ssoServerBase?has_content><#assign logoutUrl=ssoServerBase!+'/logout'></#if>
	<li><a href="${logoutUrl}">${getText('logout')}</a></li>
	</#if>
</ul>
