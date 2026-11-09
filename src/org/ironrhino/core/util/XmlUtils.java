package org.ironrhino.core.util;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.experimental.UtilityClass;

@UtilityClass
public class XmlUtils {

	private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
	private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
	private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
	private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	
	public static final DocumentBuilderFactory DEFAULT_DOCUMENT_BUILDER_FACTORY = createDocumentBuilderFactory();

	private static final XPath _xpath = XPathFactory.newInstance().newXPath();

	public static DocumentBuilderFactory createDocumentBuilderFactory() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		setRequiredFeature(factory, DISALLOW_DOCTYPE, true);
		setRequiredFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setXIncludeAware(false);
		factory.setNamespaceAware(true);
		trySetAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
		trySetAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		trySetFeature(factory, EXTERNAL_GENERAL_ENTITIES, false);
		trySetFeature(factory, EXTERNAL_PARAMETER_ENTITIES, false);
		trySetFeature(factory, LOAD_EXTERNAL_DTD, false);
		factory.setExpandEntityReferences(false);
		return factory;
	}

	private static void setRequiredFeature(DocumentBuilderFactory factory, String name, boolean value) {
		try {
			factory.setFeature(name, value);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static void trySetFeature(DocumentBuilderFactory factory, String name, boolean value) {
		try {
			factory.setFeature(name, value);
		} catch (Exception ignored) {
			// ignore
		}
	}

	private static void trySetAttribute(DocumentBuilderFactory factory, String name, Object value) {
		try {
			factory.setAttribute(name, value);
		} catch (Exception ignored) {
			// ignore
		}
	}

	public static String eval(String xpath, Reader reader) {
		try {
			XPathExpression xpe = _xpath.compile(xpath);
			return xpe.evaluate(new InputSource(reader));
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String eval(String xpath, Reader reader, NamespaceContext nsContext) {
		try {
			XPath _xpath = XPathFactory.newInstance().newXPath();
			_xpath.setNamespaceContext(nsContext);
			XPathExpression xpe = _xpath.compile(xpath);
			return xpe.evaluate(new InputSource(reader));
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String eval(String xpath, String source) {
		return eval(xpath, new StringReader(source));
	}

	public static String eval(String xpath, String source, NamespaceContext nsContext) {
		return eval(xpath, new StringReader(source), nsContext);
	}

	public static NodeList evalNodeList(String xpath, Reader reader) {
		try {
			XPathExpression xpe = _xpath.compile(xpath);
			return (NodeList) xpe.evaluate(new InputSource(reader), XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static NodeList evalNodeList(String xpath, Reader reader, NamespaceContext nsContext) {
		try {
			XPath _xpath = XPathFactory.newInstance().newXPath();
			_xpath.setNamespaceContext(nsContext);
			XPathExpression xpe = _xpath.compile(xpath);
			return (NodeList) xpe.evaluate(new InputSource(reader), XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static NodeList evalNodeList(String xpath, String source) {
		return evalNodeList(xpath, new StringReader(source));
	}

	public static NodeList evalNodeList(String xpath, String source, NamespaceContext nsContext) {
		return evalNodeList(xpath, new StringReader(source), nsContext);
	}

}
