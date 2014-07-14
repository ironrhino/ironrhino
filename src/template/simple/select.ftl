<#--
/*
 * $Id: select.ftl 804072 2009-08-14 03:16:35Z musachy $
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
<#setting number_format="#.#####">
<select<#rt/>
<#if parameters.name?has_content>
 name="${parameters.name?html}"<#rt/>
</#if>
<#if parameters.disabled!false>
 disabled="disabled"<#rt/>
</#if>
<#if parameters.tabindex?has_content>
 tabindex="${parameters.tabindex?html}"<#rt/>
</#if>
<#if parameters.id?has_content>
 id="${parameters.id?html}"<#rt/>
</#if>
<#include "/${parameters.templateDir}/simple/css.ftl" />
<#if parameters.title?has_content>
 title="${parameters.title?html}"<#rt/>
</#if>
<#if parameters.multiple!false>
 multiple="multiple"<#rt/>
</#if>
<#include "/${parameters.templateDir}/simple/scripting-events.ftl" />
<#include "/${parameters.templateDir}/simple/common-attributes.ftl" />
<#include "/${parameters.templateDir}/simple/dynamic-attributes.ftl" />
>
<#if parameters.headerKey?? && parameters.headerValue??>
    <option value="${parameters.headerKey?html}"
    <#if tag.contains(parameters.nameValue, parameters.headerKey) || tag.contains(parameters.nameValue, parameters.headerValue)>
    selected="selected"
    </#if>
    >${parameters.headerValue?html}</option>
</#if>
<#if parameters.emptyOption!false>
    <option value=""></option>
</#if>
<@s.iterator value="parameters.list">
        <#if parameters.listKey?has_content>
            <#if stack.findValue(parameters.listKey)?has_content>
              <#assign itemKey = stack.findValue(parameters.listKey)/>
              <#assign itemKeyStr = stack.findString(parameters.listKey)/>
            <#else>
              <#assign itemKey = ''/>
              <#assign itemKeyStr = ''/>
            </#if>
        <#else>
            <#assign itemKey = stack.findValue('top')/>
            <#assign itemKeyStr = stack.findString('top')>
        </#if>
        <#if parameters.listValue?has_content>
            <#if stack.findString(parameters.listValue)?has_content>
              <#assign itemValue = stack.findString(parameters.listValue)/>
            <#else>
              <#assign itemValue = ''/>
            </#if>
        <#else>
            <#assign itemValue = stack.findString('top')/>
        </#if>
    <option value="${itemKeyStr?html}"<#rt/>
        <#if tag.contains(parameters.nameValue, itemKey) || tag.contains(parameters.nameValue, itemValue)>
 selected="selected"<#rt/>
        </#if>
    >${itemValue?html}</option><#lt/>
</@s.iterator>

<#include "/${parameters.templateDir}/simple/optgroup.ftl" />

</select>
<#if parameters.multiple!false>
<input type="hidden" id="__multiselect_${parameters.id?html}" name="__multiselect_${parameters.name?html}" value=""<#rt/>
<#if parameters.disabled!false>
 disabled="disabled"<#rt/>
</#if>
 />
</#if>