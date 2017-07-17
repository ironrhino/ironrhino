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

import com.opensymphony.xwork2.config.ConfigurationException;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.StrutsConstants;
import org.apache.struts2.StrutsException;
import org.apache.struts2.components.template.Template;
import org.apache.struts2.components.template.TemplateEngine;
import org.apache.struts2.components.template.TemplateEngineManager;
import org.apache.struts2.components.template.TemplateRenderingContext;
import org.apache.struts2.views.annotations.StrutsTagAttribute;
import org.apache.struts2.views.util.ContextUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * UIBean is the standard superclass of all Struts UI components.
 * It defines common Struts and html properties all UI components should present for usage.
 *
 * <!-- START SNIPPET: templateRelatedAttributes -->
 *
 * <table border="1">
 *    <thead>
 *       <tr>
 *          <td>Attribute</td>
 *          <td>Theme</td>
 *          <td>Data Types</td>
 *          <td>Description</td>
 *       </tr>
 *    </thead>
 *    <tbody>
 *       <tr>
 *          <td>templateDir</td>
 *          <td>n/a</td>
 *          <td>String</td>
 *          <td>define the template directory</td>
 *       </td>
 *       <tr>
 *          <td>theme</td>
 *          <td>n/a</td>
 *          <td>String</td>
 *          <td>define the theme name</td>
 *       </td>
 *       <tr>
 *          <td>template</td>
 *          <td>n/a</td>
 *          <td>String</td>
 *          <td>define the template name</td>
 *       </td>
 *       <tr>
 *          <td>themeExpansionToken</td>
 *          <td>n/a</td>
 *          <td>String</td>
 *          <td>special token (defined with struts.ui.theme.expansion.token) used to search for template in parent theme
 *          (don't use it separately!)</td>
 *       </td>
 *       <tr>
 *          <td>expandTheme</td>
 *          <td>n/a</td>
 *          <td>String</td>
 *          <td>concatenation of themeExpansionToken and theme which tells internal template loader mechanism
 *          to try load template from current theme and then from parent theme (and parent theme, and so on)
 *          when used with &lt;#include/&gt; directive</td>
 *       </td>
 *    </tbody>
 * </table>
 *
 * <!-- END SNIPPET: templateRelatedAttributes -->
 *
 * <p/>
 *
 * <!-- START SNIPPET: generalAttributes -->
 *
 * <table border="1">
 *    <thead>
 *       <tr>
 *          <td>Attribute</td>
 *          <td>Theme</td>
 *          <td>Data Types</td>
 *          <td>Description</td>
 *       </tr>
 *    </thead>
 *    <tbody>
 *       <tr>
 *          <td>cssClass</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>define html class attribute</td>
 *       </tr>
 *       <tr>
 *          <td>cssStyle</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>define html style attribute</td>
 *       </tr>
 *       <tr>
 *          <td>cssClass</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>error class attribute</td>
 *       </tr>
 *       <tr>
 *          <td>cssStyle</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>error style attribute</td>
 *       </tr>
 *       <tr>
 *          <td>title</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>define html title attribute</td>
 *       </tr>
 *       <tr>
 *          <td>disabled</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>define html disabled attribute</td>
 *       </tr>
 *       <tr>
 *          <td>label</td>
 *          <td>xhtml</td>
 *          <td>String</td>
 *          <td>define label of form element</td>
 *       </tr>
 *       <tr>
 *          <td>labelPosition</td>
 *          <td>xhtml</td>
 *          <td>String</td>
 *          <td>define label position of form element (top/left), default to left</td>
 *       </tr>
 *       <tr>
 *          <td>requiredPosition</td>
 *          <td>xhtml</td>
 *          <td>String</td>
 *          <td>define required label position of form element (left/right), default to right</td>
 *       </tr>
  *       <tr>
 *          <td>errorPosition</td>
 *          <td>xhtml</td>
 *          <td>String</td>
 *          <td>define error position of form element (top|bottom), default to top</td>
 *       </tr>
 *       <tr>
 *          <td>name</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>Form Element's field name mapping</td>
 *       </tr>
 *       <tr>
 *          <td>required</td>
 *          <td>xhtml</td>
 *          <td>Boolean</td>
 *          <td>add * to label (true to add false otherwise)</td>
 *       </tr>
 *       <tr>
 *          <td>tabIndex</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>define html tabindex attribute</td>
 *       </tr>
 *       <tr>
 *          <td>value</td>
 *          <td>simple</td>
 *          <td>Object</td>
 *          <td>define value of form element</td>
 *       </tr>
 *    </tbody>
 * </table>
 *
 * <!-- END SNIPPET: generalAttributes -->
 *
 * <p/>
 *
 * <!-- START SNIPPET: javascriptRelatedAttributes -->
 *
 * <table border="1">
 *    <thead>
 *       <tr>
 *          <td>Attribute</td>
 *          <td>Theme</td>
 *          <td>Data Types</td>
 *          <td>Description</td>
 *       </tr>
 *    </thead>
 *    <tbody>
 *       <tr>
 *          <td>onclick</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onclick attribute</td>
 *       </tr>
 *       <tr>
 *          <td>ondblclick</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript ondbclick attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onmousedown</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onmousedown attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onmouseup</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onmouseup attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onmouseover</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onmouseover attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onmouseout</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onmouseout attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onfocus</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onfocus attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onblur</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onblur attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onkeypress</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onkeypress attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onkeyup</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onkeyup attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onkeydown</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onkeydown attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onselect</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onselect attribute</td>
 *       </tr>
 *       <tr>
 *          <td>onchange</td>
 *          <td>simple</td>
 *          <td>String</td>
 *          <td>html javascript onchange attribute</td>
 *       </tr>
 *    </tbody>
 * </table>
 *
 * <!-- END SNIPPET: javascriptRelatedAttributes -->
 *
 * <p/>
 *
 * <!-- START SNIPPET: tooltipattributes -->
 *
 * <table border="1">
 *  <tr>
 *     <td>Attribute</td>
 *     <td>Data Type</td>
 *     <td>Default</td>
 *     <td>Description</td>
 *  </tr>
 *  <tr>
 *      <td>tooltip</td>
 *      <td>String</td>
 *      <td>none</td>
 *      <td>Set the tooltip of this particular component</td>
 *  </tr>
 *  <tr>
 *      <td>jsTooltipEnabled</td>
 *      <td>String</td>
 *      <td>false</td>
 *      <td>Enable js tooltip rendering</td>
 *  </tr>
 *    <tr>
 *      <td>tooltipIcon</td>
 *      <td>String</td>
 *      <td>/struts/static/tooltip/tooltip.gif</td>
 *      <td>The url to the tooltip icon</td>
 *   <tr>
 *      <td>tooltipDelay</td>
 *      <td>String</td>
 *      <td>500</td>
 *      <td>Tooltip shows up after the specified timeout (miliseconds). A behavior similar to that of OS based tooltips.</td>
 *   </tr>
 *   <tr>
 *      <td>key</td>
 *      <td>simple</td>
 *      <td>String</td>
 *      <td>The name of the property this input field represents.  This will auto populate the name, label, and value</td>
 *   </tr>
 * </table>
 *
 * <!-- END SNIPPET: tooltipattributes -->
 *
 *
 * <!-- START SNIPPET: tooltipdescription -->
 * <b>tooltipConfig is deprecated, use individual tooltip configuration attributes instead </b>
 *
 * Every Form UI component (in xhtml / css_xhtml or any other that extends them) can
 * have tooltips assigned to them. The Form component's tooltip related attribute, once
 * defined, will be applied to all form UI components that are created under it unless
 * explicitly overriden by having the Form UI component itself defined with their own tooltip attribute.
 *
 * <p/>
 *
 * In Example 1, the textfield will inherit the tooltipDelay and tooltipIconPath attribte from
 * its containing form. In other words, although it doesn't define a tooltipIconPath
 * attribute, it will have that attribute inherited from its containing form.
 *
 * <p/>
 *
 * In Example 2, the  textfield will inherite both the tooltipDelay and
 * tooltipIconPath attribute from its containing form, but the tooltipDelay
 * attribute is overriden at the textfield itself. Hence, the textfield actually will
 * have its tooltipIcon defined as /myImages/myIcon.gif, inherited from its containing form, and
 * tooltipDelay defined as 5000.
 *
 * <p/>
 *
 * Example 3, 4 and 5 show different ways of setting the tooltip configuration attribute.<br/>
 * <b>Example 3:</b> Set tooltip config through the body of the param tag<br/>
 * <b>Example 4:</b> Set tooltip config through the value attribute of the param tag<br/>
 * <b>Example 5:</b> Set tooltip config through the tooltip attributes of the component tag<br/>
 *
 * <!-- END SNIPPET: tooltipdescription -->
 *
 *
 * <pre>
 * <!-- START SNIPPET: tooltipexample -->
 *
 * &lt;!-- Example 1: --&gt;
 * &lt;s:form
 *          tooltipDelay="500"
 *          tooltipIconPath="/myImages/myIcon.gif" .... &gt;
 *   ....
 *     &lt;s:textfield label="Customer Name" tooltip="Enter the customer name" .... /&gt;
 *   ....
 * &lt;/s:form&gt;
 *
 * &lt;!-- Example 2: --&gt;
 * &lt;s:form
 *          tooltipDelay="500"
 *          tooltipIconPath="/myImages/myIcon.gif" .... &gt;
 *   ....
 *     &lt;s:textfield label="Address"
 *          tooltip="Enter your address"
 *          tooltipDelay="5000" /&gt;
 *   ....
 * &lt;/s:form&gt;
 *
 *
 * &lt;-- Example 3: --&gt;
 * &lt;s:textfield
 *        label="Customer Name"
 *        tooltip="One of our customer Details"&gt;
 *        &lt;s:param name="tooltipDelay"&gt;
 *             500
 *        &lt;/s:param&gt;
 *        &lt;s:param name="tooltipIconPath"&gt;
 *             /myImages/myIcon.gif
 *        &lt;/s:param&gt;
 * &lt;/s:textfield&gt;
 *
 *
 * &lt;-- Example 4: --&gt;
 * &lt;s:textfield
 *          label="Customer Address"
 *          tooltip="Enter The Customer Address" &gt;
 *          &lt;s:param
 *              name="tooltipDelay"
 *              value="500" /&gt;
 * &lt;/s:textfield&gt;
 *
 *
 * &lt;-- Example 5: --&gt;
 * &lt;s:textfield
 *          label="Customer Telephone Number"
 *          tooltip="Enter customer Telephone Number"
 *          tooltipDelay="500"
 *          tooltipIconPath="/myImages/myIcon.gif" /&gt;
 *
 * <!-- END SNIPPET: tooltipexample -->
 * </pre>
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public abstract class UIBean extends Component {
    private static final Logger LOG = LoggerFactory.getLogger(UIBean.class);

    protected HttpServletRequest request;
    protected HttpServletResponse response;

    public UIBean(ValueStack stack, HttpServletRequest request, HttpServletResponse response) {
        super(stack);
        this.request = request;
        this.response = response;
        this.templateSuffix = ContextUtil.getTemplateSuffix(stack.getContext());
    }

    // The templateSuffic to use, overrides the default one if not null.
    protected String templateSuffix;

    // The template to use, overrides the default one.
    protected String template;

    // templateDir and theme attributes
    protected String templateDir;
    protected String theme;

    // shortcut, sets label, name, and value
    protected String key;

    protected String id;
    protected String cssClass;
    protected String cssStyle;
    protected String disabled;
    protected String label;
    protected String name;
    protected String value;

    // dynamic attributes
    protected Map<String,Object> dynamicAttributes = new HashMap<String,Object>();

    protected String defaultTemplateDir;
    protected String defaultUITheme;
    protected String uiThemeExpansionToken;
    protected TemplateEngineManager templateEngineManager;

    // dynamic attributes support for tags used with FreeMarker templates
    protected static ConcurrentMap<Class, Set<String>> standardAttributesMap = new ConcurrentHashMap<Class, Set<String>>();

    @Inject(StrutsConstants.STRUTS_UI_TEMPLATEDIR)
    public void setDefaultTemplateDir(String dir) {
        this.defaultTemplateDir = dir;
    }

    @Inject(StrutsConstants.STRUTS_UI_THEME)
    public void setDefaultUITheme(String theme) {
        this.defaultUITheme = theme;
    }

    @Inject(StrutsConstants.STRUTS_UI_THEME_EXPANSION_TOKEN)
    public void setUIThemeExpansionToken(String uiThemeExpansionToken) {
        this.uiThemeExpansionToken = uiThemeExpansionToken;
    }

    @Inject
    public void setTemplateEngineManager(TemplateEngineManager mgr) {
        this.templateEngineManager = mgr;
    }

    public boolean end(Writer writer, String body) {
        evaluateParams();
        try {
            super.end(writer, body, false);
            mergeTemplate(writer, buildTemplateName(template, getDefaultTemplate()));
        } catch (Exception e) {
            throw new StrutsException(e);
        }
        finally {
            popComponentStack();
        }

        return false;
    }

    /**
     * A contract that requires each concrete UI Tag to specify which template should be used as a default.  For
     * example, the CheckboxTab might return "checkbox.vm" while the RadioTag might return "radio.vm".  This value
     * <strong>not</strong> begin with a '/' unless you intend to make the path absolute rather than relative to the
     * current theme.
     *
     * @return The name of the template to be used as the default.
     */
    protected abstract String getDefaultTemplate();

    protected Template buildTemplateName(String myTemplate, String myDefaultTemplate) {
        String template = myDefaultTemplate;

        if (myTemplate != null) {
            template = findString(myTemplate);
        }

        String templateDir = getTemplateDir();
        String theme = getTheme();

        return new Template(templateDir, theme, template);

    }

    protected void mergeTemplate(Writer writer, Template template) throws Exception {
        final TemplateEngine engine = templateEngineManager.getTemplateEngine(template, templateSuffix);
        if (engine == null) {
            throw new ConfigurationException("Unable to find a TemplateEngine for template " + template);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Rendering template " + template);
        }

        final TemplateRenderingContext context = new TemplateRenderingContext(template, writer, getStack(), getParameters(), this);
        engine.renderTemplate(context);
    }

    public String getTemplateDir() {
        String templateDir = null;

        if (this.templateDir != null) {
            templateDir = findString(this.templateDir);
        }

        // If templateDir is not explicitly given,
        // try to find attribute which states the dir set to use
        if ((templateDir == null) || (templateDir.equals(""))) {
            templateDir = stack.findString("#attr.templateDir");
        }

        // Default template set
        if ((templateDir == null) || (templateDir.equals(""))) {
            templateDir = defaultTemplateDir;
        }

        // Defaults to 'template'
        if ((templateDir == null) || (templateDir.equals(""))) {
            templateDir = "template";
        }

        return templateDir;
    }

    public String getTheme() {
        String theme = null;

        if (this.theme != null) {
            theme = findString(this.theme);
        }

        if ( theme == null || theme.equals("") ) {
            Form form = (Form) findAncestor(Form.class);
            if (form != null) {
                theme = form.getTheme();
            }
        }

        // If theme set is not explicitly given,
        // try to find attribute which states the theme set to use
        if ((theme == null) || (theme.equals(""))) {
            theme = stack.findString("#attr.theme");
        }

        // Default theme set
        if ((theme == null) || (theme.equals(""))) {
            theme = defaultUITheme;
        }

        return theme;
    }

    public void evaluateParams() {
        String templateDir = getTemplateDir();
        String theme = getTheme();
        
        addParameter("templateDir", templateDir);
        addParameter("theme", theme);
        addParameter("template", template != null ? findString(template) : getDefaultTemplate());
        addParameter("dynamicAttributes", dynamicAttributes);
        addParameter("themeExpansionToken", uiThemeExpansionToken);
        addParameter("expandTheme", uiThemeExpansionToken + theme);

        if (name != null) {
            addParameter("name", findString(name));
        }

        if (label != null) {
            addParameter("label", findString(label));
        }
        
        if (disabled != null) {
            addParameter("disabled", findValue(disabled, Boolean.class));
        }

        if (cssClass != null) {
            addParameter("cssClass", findString(cssClass));
        }

        if (cssStyle != null) {
            addParameter("cssStyle", findString(cssStyle));
        }



        // see if the value was specified as a parameter already
        if (parameters.containsKey("value")) {
            parameters.put("nameValue", parameters.get("value"));
        } else {
            if (value != null) {
                addParameter("nameValue", value);
            } else if (name != null && evaluateNameValue()) {
                addParameter("nameValue", findValue(name, getValueClassType()));
            }
        }

        final Form form = (Form) findAncestor(Form.class);

        // create HTML id element
        populateComponentHtmlId(form);

        if (form != null ) {
            addParameter("form", form.getParameters());

            if ( name != null ) {
                // list should have been created by the form component
                List<String> tags = (List<String>) form.getParameters().get("tagNames");
                tags.add(name);
            }
        }

        evaluateExtraParams();
    }

	protected String escape(String name) {
        // escape any possible values that can make the ID painful to work with in JavaScript
        if (name != null) {
            return name.replaceAll("[\\/\\.\\[\\]]", "_");
        } else {
            return null;
        }
    }

    /**
     * Ensures an unescaped attribute value cannot be vulnerable to XSS attacks
     *
     * @param val The value to check
     * @return The escaped value
     */
    protected String ensureAttributeSafelyNotEscaped(String val) {
        if (val != null) {
            return val.replaceAll("\"", "&#34;");
        } else {
            return null;
        }
    }

    protected void evaluateExtraParams() {
    }

    protected boolean evaluateNameValue() {
        return true;
    }

    protected Class getValueClassType() {
        return String.class;
    }

    public void addFormParameter(String key, Object value) {
        Form form = (Form) findAncestor(Form.class);
        if (form != null) {
            form.addParameter(key, value);
        }
    }

    protected void enableAncestorFormCustomOnsubmit() {
        Form form = (Form) findAncestor(Form.class);
        if (form != null) {
            form.addParameter("customOnsubmitEnabled", Boolean.TRUE);
        } else {
            if (LOG.isWarnEnabled()) {
        	LOG.warn("Cannot find an Ancestor form, custom onsubmit is NOT enabled");
            }
        }
    }

    /**
     * Create HTML id element for the component and populate this component parameter
     * map. Additionally, a parameter named escapedId is populated which contains the found id value filtered by
     * {@link #escape(String)}, needed eg. for naming Javascript identifiers based on the id value.
     *
     * The order is as follows :-
     * <ol>
     *   <li>This component id attribute</li>
     *   <li>[containing_form_id]_[this_component_name]</li>
     *   <li>[this_component_name]</li>
     * </ol>
     *
     * @param form enclosing form tag
     */
    protected void populateComponentHtmlId(Form form) {
        String tryId;
        if (id != null) {
            tryId = id;
        } else if (form != null) {
        	String formId = (String)form.getParameters().get("id") ;
        	if( formId != null && name != null )
        		tryId = formId + '-' + name.replaceAll("\\.", "-");
        	else
        		tryId = "";
        } else {
            tryId = "";
        }
        addParameter("id", tryId);
        addParameter("escapedId", escape(tryId));
    }

    /**
     * Get's the id for referencing element.
     * @return the id for referencing element.
     */
    public String getId() {
        return id;
    }

    @StrutsTagAttribute(description="HTML id attribute")
    public void setId(String id) {
        if (id != null) {
            this.id = findString(id);
        }
    }

    @StrutsTagAttribute(description="The template directory.")
    public void setTemplateDir(String templateDir) {
        this.templateDir = templateDir;
    }

    @StrutsTagAttribute(description="The theme (other than default) to use for rendering the element")
    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getTemplate() {
        return template;
    }

    @StrutsTagAttribute(description="The template (other than default) to use for rendering the element")
    public void setTemplate(String template) {
        this.template = template;
    }

    @StrutsTagAttribute(description="The css class to use for element")
    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }
    
    @StrutsTagAttribute(description="The css class to use for element - it's an alias of cssClass attribute.")
    public void setClass(String cssClass) {
        this.cssClass = cssClass;
    }

    @StrutsTagAttribute(description="The css style definitions for element to use")
    public void setCssStyle(String cssStyle) {
        this.cssStyle = cssStyle;
    }
    
    @StrutsTagAttribute(description="The css style definitions for element to use - it's an alias of cssStyle attribute.")
    public void setStyle(String cssStyle) {
        this.cssStyle = cssStyle;
    }

    @StrutsTagAttribute(description="Set the html disabled attribute on rendered html element")
    public void setDisabled(String disabled) {
        this.disabled = disabled;
    }

    @StrutsTagAttribute(description="Label expression used for rendering an element specific label")
    public void setLabel(String label) {
        this.label = label;
    }
    
    @StrutsTagAttribute(description="The name to set for element")
    public void setName(String name) {
        this.name = name;
    }

    @StrutsTagAttribute(description="Preset the value of input element.")
    public void setValue(String value) {
        this.value = value;
    }

    @StrutsTagAttribute(description="Set the key (name, value, label) for this particular component")
    public void setKey(String key) {
        this.key = key;
    }

	public void setDynamicAttributes(Map<String, Object> dynamicAttributes) {
		this.dynamicAttributes.putAll(dynamicAttributes);
    }

	@Override
	/**
	 * supports dynamic attributes for freemarker ui tags
	 * @see https://issues.apache.org/jira/browse/WW-3174
	 */
	public void copyParams(Map params) {
		super.copyParams(params);
		Set<String> standardAttributes = getStandardAttributes();
        for (Object o : params.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String) entry.getKey();
            if (!key.equals("dynamicAttributes") && !standardAttributes.contains(key)){
                dynamicAttributes.put(key, entry.getValue());
            }
        }
	}

	protected Set<String> getStandardAttributes() {
        Class clz = getClass();
        Set<String> standardAttributes = standardAttributesMap.get(clz);
        if (standardAttributes == null) {
            standardAttributes = new HashSet<String>();
            for (Method m : clz.getMethods()) {
            	if(m.isAnnotationPresent(StrutsTagAttribute.class))
            		standardAttributes.add(StringUtils.uncapitalize(m.getName().substring(3)));
			}
            Set<String> exists = standardAttributesMap.putIfAbsent(clz, standardAttributes);
            if (exists != null)
            	standardAttributes = exists;
        }
		return standardAttributes;
	}

}
