<ul class="dropdown-menu">
  <@resourcePresentConditional value="resources/view/audit.ftl">
  <li><a href="<@url value="/audit"/>" class="ajax view">${action.getText('auditEvent')}</a></li>
  <li class="divider"></li>
  </@resourcePresentConditional>
  <#assign divider=false/>
  <#if user.class.name=='org.ironrhino.security.model.User'>
  <li><a href="<@url value="${ssoServerBase!}/user/profile"/>" class="popmodal nocache">${action.getText('profile')}</a></li>
  <#if !user.getAttribute('oauth_provider')??>
  <li><a href="<@url value="${ssoServerBase!}/user/password"/>" class="popmodal">${action.getText('change')}${action.getText('password')}</a></li>
  </#if>
  <#assign divider=true/>
  </#if>
  <#if !request.getAttribute("javax.servlet.request.X509Certificate")?? && beans['logoutSuccessHandler']??>
  <#if divider>
  <li class="divider"></li>
  </#if>
  <li><a href="<@url value="${ssoServerBase!}/logout"/>">${action.getText('logout')}</a></li>
  </#if>
</ul>