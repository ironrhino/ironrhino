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
	<#if user.class.name=='org.ironrhino.security.model.User'>
	<li><a href="<@url value="${ssoServerBase!}/user/profile"/>" class="popmodal nocache">${getText('profile')}</a></li>
	<#if !user.getAttribute('oauth_provider')??>
	<li><a href="<@url value="${ssoServerBase!}/user/password"/>" class="popmodal">${getText('change')}${getText('password')}</a></li>
	</#if>
	<#assign divider=true/>
	</#if>
	<#if !request.getAttribute("javax.servlet.request.X509Certificate")?? && beans['logoutSuccessHandler']??>
	<#if divider>
	<li class="divider"></li>
	</#if>
	<@authorize ifAnyGranted="ROLE_PREVIOUS_ADMINISTRATOR">
	<li><a href="<@url value="${ssoServerBase!}/switch/back"/>">${getText('back')}</a></li>
	</@authorize>
	<li><a href="<@url value="${ssoServerBase!}/logout"/>">${getText('logout')}</a></li>
	</#if>
</ul>
