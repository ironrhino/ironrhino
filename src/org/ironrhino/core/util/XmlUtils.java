package org.ironrhino.core.util;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlUtils {

	private static final XPath _xpath = XPathFactory.newInstance().newXPath();

	private static final JAXBContext context = initContext();

	public static String eval(String xpath, Reader reader) {
		try {
			XPathExpression xpe = _xpath.compile(xpath);
			return xpe.evaluate(new InputSource(reader));
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String eval(String xpath, Reader reader,
			NamespaceContext nsContext) {
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

	public static String eval(String xpath, String source,
			NamespaceContext nsContext) {
		return eval(xpath, new StringReader(source), nsContext);
	}

	public static NodeList evalNodeList(String xpath, Reader reader) {
		try {
			XPathExpression xpe = _xpath.compile(xpath);
			return (NodeList) xpe.evaluate(new InputSource(reader),
					XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static NodeList evalNodeList(String xpath, Reader reader,
			NamespaceContext nsContext) {
		try {
			XPath _xpath = XPathFactory.newInstance().newXPath();
			_xpath.setNamespaceContext(nsContext);
			XPathExpression xpe = _xpath.compile(xpath);
			return (NodeList) xpe.evaluate(new InputSource(reader),
					XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static NodeList evalNodeList(String xpath, String source) {
		return evalNodeList(xpath, new StringReader(source));
	}

	public static NodeList evalNodeList(String xpath, String source,
			NamespaceContext nsContext) {
		return evalNodeList(xpath, new StringReader(source), nsContext);
	}

	private static JAXBContext initContext() {
		Set<Class<?>> classes = ClassScaner.scanAnnotated(
				ClassScaner.getAppPackages(), XmlRootElement.class);
		try {
			return JAXBContext.newInstance(classes.toArray(new Class[0]));
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String toXml(Object obj) throws JAXBException {
		if (obj == null)
			return null;
		Marshaller marshaller = context.createMarshaller();
		marshaller
				.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
				Boolean.TRUE);
		StringWriter sw = new StringWriter();
		marshaller.marshal(obj, sw);
		return sw.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromXml(String xml, Class<T> clazz)
			throws JAXBException {
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return (T) unmarshaller.unmarshal(new StringReader(xml));
	}

}
