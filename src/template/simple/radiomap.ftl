<#--
/*
 * $Id: radiomap.ftl 1328526 2012-04-20 22:19:41Z lukaszlenart $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
-->
<@s.iterator value="parameters.list">
    <#if parameters.listKey?has_content>
        <#assign itemKey = stack.findValue(parameters.listKey)/>
    <#else>
        <#assign itemKey = stack.findValue('top')/>
    </#if>
    <#assign itemKeyStr = itemKey.toString() />
    <#if parameters.listValue?has_content>
        <#assign itemValue = stack.findString(parameters.listValue)/>
    <#else>
        <#assign itemValue = stack.findString('top')/>
    </#if>
<label for="${parameters.id?html}${itemKeyStr?html}" class="radio inline"><#rt/>    
<input type="radio"<#rt/>
<#if parameters.name?has_content>
 name="${parameters.name?html}"<#rt/>
</#if>
 id="${parameters.id?html}${itemKeyStr?html}"<#rt/>
<#if tag.contains(parameters.nameValue!'', itemKeyStr) || tag.contains(parameters.nameValue!'', itemValue)>
 checked="checked"<#rt/>
</#if>
<#if itemKey?has_content>
 value="${itemKeyStr?html}"<#rt/>
</#if>
<#if parameters.readonly!false>
 readonly="readonly"<#rt/>
</#if>
<#if parameters.disabled!false>
 disabled="disabled"<#rt/>
</#if>
<#if parameters.tabindex?has_content>
 tabindex="${parameters.tabindex?html}"<#rt/>
</#if>
<#if parameters.cssClass?has_content>
 class="${parameters.cssClass?html}"<#rt/>
</#if>
<#if parameters.cssStyle?has_content>
 style="${parameters.cssStyle?html}"<#rt/>
</#if>
<#include "/${parameters.templateDir}/simple/css.ftl" />
<#if parameters.title?has_content>
 title="${parameters.title?html}"<#rt/>
</#if>
<#include "/${parameters.templateDir}/simple/scripting-events.ftl" />
<#include "/${parameters.templateDir}/simple/common-attributes.ftl" />
<#include "/${parameters.templateDir}/simple/dynamic-attributes.ftl" />
/><#rt/>
    ${itemValue}<#t/>
</label>
</@s.iterator>