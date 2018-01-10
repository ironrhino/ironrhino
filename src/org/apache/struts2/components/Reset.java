/*
 * $Id$
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

package org.apache.struts2.components;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.views.annotations.StrutsTag;
import org.apache.struts2.views.annotations.StrutsTagAttribute;

import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;

/**
 * <!-- START SNIPPET: javadoc -->
 * Render a reset button. The reset tag is used together with the form tag.
 * <!-- END SNIPPET: javadoc -->
 */
@StrutsTag(
    name="reset",
    tldTagClass="org.apache.struts2.views.jsp.ui.ResetTag",
    description="Render a reset button",
    allowDynamicAttributes=true)
public class Reset extends FormButton {

    private static final Logger LOG = LoggerFactory.getLogger(Reset.class);
    final public static String OPEN_TEMPLATE = "reset";
    final public static String TEMPLATE = "reset-close";
    protected String src;

    public Reset(ValueStack stack, HttpServletRequest request, HttpServletResponse response) {
        super(stack, request, response);
    }

    public String getDefaultOpenTemplate() {
        return OPEN_TEMPLATE;
    }

    protected String getDefaultTemplate() {
        return TEMPLATE;
    }

    public void evaluateParams() {
        if (this.name == null && value != null) {
        		this.label = value;
        		this.value = null;
        }
        super.evaluateParams();
    }

    public void evaluateExtraParams() {
        super.evaluateExtraParams();

        if (src != null)
            addParameter("src", findString(src));
    }

    /**
     * Indicate whether the concrete button supports the type "image".
     *
     * @return <tt>true</tt> to indicate type image is supported.
     */
    protected boolean supportsImageType() {
        return true;
    }

    @StrutsTagAttribute(description="Supply an image src for <i>image</i> type reset button. Will have no effect for types <i>input</i> and <i>button</i>.")
    public void setSrc(String src) {
        this.src = src;
    }


    @Override
    public boolean usesBody() {
        return true;
    }

    /**
     * Overrides to be able to render body in a template rather than always before the template
     */
    public boolean end(Writer writer, String body) {
        evaluateParams();
        try {
            addParameter("body", body);

            mergeTemplate(writer, buildTemplateName(template, getDefaultTemplate()));
        } catch (Exception e) {
            LOG.error("error when rendering", e);
        }
        finally {
            popComponentStack();
        }

        return false;
    }
}
