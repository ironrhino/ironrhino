package org.ironrhino.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.ironrhino.core.model.BaseTreeableEntity;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceCompactor;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.Tag;

@SuppressWarnings("rawtypes")
public class HtmlUtils {

	public static String getTreeViewHtml(Collection children, boolean async) {
		if (async || children == null || children.size() == 0)
			return "<ul id=\"treeview\"></ul>";
		StringBuilder sb = new StringBuilder();
		sb.append("<ul id=\"treeview\">");
		for (Object c : children)
			sb.append(getTreeViewHtml((BaseTreeableEntity) c));
		sb.append("</ul>");
		return sb.toString();
	}

	public static String getTreeViewHtml(BaseTreeableEntity t) {
		StringBuilder sb = new StringBuilder();
		sb.append("<li id=\"" + t.getId() + "\"><span>" + t.getName() + "</span>");
		if (t.getChildren().size() > 0) {
			sb.append("<ul>");
			for (Object c : t.getChildren())
				sb.append(getTreeViewHtml((BaseTreeableEntity) c));
			sb.append("</ul>");
		}
		sb.append("</li>");
		return sb.toString();
	}

	public static String abbr(String html, int abbr) {
		Source source = new Source(html);
		Iterator<Element> it = source.getAllElements().iterator();
		if (it.hasNext()) {
			return org.apache.commons.lang3.StringUtils.abbreviate(it.next().getTextExtractor().toString(), abbr);
		} else
			return org.apache.commons.lang3.StringUtils.abbreviate(source.toString(), abbr);
	}

	public static String compress(String html, String[] id) {
		StringBuilder sb = new StringBuilder();
		final List<String> ids = Arrays.asList(id);
		Source source = new Source(html);
		Iterator<Element> it = source.getAllElements().iterator();
		while (it.hasNext()) {
			Element element = it.next();
			String tid = element.getAttributeValue("id");
			if (tid != null && ids.contains(tid))
				sb.append(new SourceCompactor(element).toString());
		}
		return sb.toString();
	}

	public static String process(String html, Replacer replacer) {
		Source source = new Source(html);
		source.fullSequentialParse();
		OutputDocument outputDocument = new OutputDocument(source);
		List<Tag> tags = source.getAllTags(StartTagType.NORMAL);
		for (Tag t : tags) {
			StartTag st = (StartTag) t;
			Attributes attrs = st.parseAttributes();
			Iterator<Attribute> it = attrs.iterator();
			while (it.hasNext()) {
				Attribute attr = it.next();
				String temp = replacer.replace(st, attr);
				if (temp != null)
					outputDocument.replace(attr, temp);
			}
		}
		return outputDocument.toString();
	}

	@FunctionalInterface
	public static interface Replacer {
		public String replace(StartTag st, Attribute attr);
	}

}
