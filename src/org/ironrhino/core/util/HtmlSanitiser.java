package org.ironrhino.core.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.EndTagType;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.HTMLElements;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.Tag;

public class HtmlSanitiser {

	private static final Set<String> VALID_ELEMENT_NAMES = new HashSet<>(Arrays.asList(new String[] {
			HTMLElementName.DIV, HTMLElementName.SPAN, HTMLElementName.STRONG, HTMLElementName.EM, HTMLElementName.BR,
			HTMLElementName.P, HTMLElementName.B, HTMLElementName.I, HTMLElementName.OL, HTMLElementName.UL,
			HTMLElementName.LI, HTMLElementName.A, HTMLElementName.TABLE, HTMLElementName.THEAD, HTMLElementName.TBODY,
			HTMLElementName.TR, HTMLElementName.TH, HTMLElementName.TD }));

	private static final Set<String> VALID_ATTRIBUTE_NAMES = new HashSet<>(
			Arrays.asList(new String[] { "id", "class", "href", "target", "title", "style", "align" }));

	private static final Object VALID_MARKER = new Object();

	public static String sanitise(String pseudoHTML) {
		return sanitise(pseudoHTML, false);
	}

	public static String sanitise(String pseudoHTML, boolean stripInvalidElements) {
		return sanitise(pseudoHTML, stripInvalidElements, false);
	}

	public static String sanitise(String pseudoHTML, boolean stripInvalidElements, boolean formatWhiteSpace) {
		Source source = new Source(pseudoHTML);
		source.fullSequentialParse();
		OutputDocument outputDocument = new OutputDocument(source);
		List<Tag> tags = source.getAllTags();
		int pos = 0;
		for (Tag tag : tags) {
			if (processTag(tag, outputDocument)) {
				tag.setUserData(VALID_MARKER);
			} else {
				if (!stripInvalidElements)
					continue; // element will be encoded along with surrounding
				// text
				outputDocument.remove(tag);
			}
			reencodeTextSegment(source, outputDocument, pos, tag.getBegin(), formatWhiteSpace);
			pos = tag.getEnd();
		}
		reencodeTextSegment(source, outputDocument, pos, source.getEnd(), formatWhiteSpace);
		return outputDocument.toString();
	}

	private static boolean processTag(Tag tag, OutputDocument outputDocument) {
		String elementName = tag.getName();
		if (!VALID_ELEMENT_NAMES.contains(elementName))
			return false;
		if (tag.getTagType() == StartTagType.NORMAL) {
			Element element = tag.getElement();
			if (HTMLElements.getEndTagRequiredElementNames().contains(elementName)) {
				if (element.getEndTag() == null)
					return false; // refect start tag if its required end tag is
				// missing
			} else if (HTMLElements.getEndTagOptionalElementNames().contains(elementName)) {
				if (elementName == HTMLElementName.LI && !isValidLITag(tag))
					return false; // reject invalid LI tags
				if (element.getEndTag() == null)
					outputDocument.insert(element.getEnd(), getEndTagHTML(elementName)); // insert
																							// optional
																							// end
				// tag if it is
				// missing
			}
			outputDocument.replace(tag, getStartTagHTML(element.getStartTag()));
		} else if (tag.getTagType() == EndTagType.NORMAL) {
			if (tag.getElement() == null)
				return false; // reject end tags that aren't associated with a
			// start tag
			if (elementName == HTMLElementName.LI && !isValidLITag(tag))
				return false; // reject invalid LI tags
			outputDocument.replace(tag, getEndTagHTML(elementName));
		} else {
			return false; // reject abnormal tags
		}
		return true;
	}

	private static boolean isValidLITag(Tag tag) {
		Element parentElement = tag.getElement().getParentElement();
		if (parentElement == null)
			return false; // ignore LI elements without a parent
		if (parentElement.getStartTag().getUserData() != VALID_MARKER)
			return false; // ignore LI elements who's parent is not valid
		return parentElement.getName() == HTMLElementName.UL || parentElement.getName() == HTMLElementName.OL; // only
																												// accept
		// LI tags
		// who's
		// immediate
		// parent is
		// UL or OL.
	}

	private static void reencodeTextSegment(Source source, OutputDocument outputDocument, int begin, int end,
			boolean formatWhiteSpace) {
		if (begin >= end)
			return;
		Segment textSegment = new Segment(source, begin, end);
		String decodedText = CharacterReference.decode(textSegment);
		String encodedText = formatWhiteSpace ? CharacterReference.encodeWithWhiteSpaceFormatting(decodedText)
				: CharacterReference.encode(decodedText);
		outputDocument.replace(textSegment, encodedText);
	}

	private static CharSequence getStartTagHTML(StartTag startTag) {
		// tidies and filters out non-approved attributes
		StringBuilder sb = new StringBuilder();
		sb.append('<').append(startTag.getName());
		for (Attribute attribute : startTag.getAttributes()) {
			if (VALID_ATTRIBUTE_NAMES.contains(attribute.getKey())) {
				sb.append(' ').append(attribute.getName());
				if (attribute.getValue() != null) {
					sb.append("=\"");
					sb.append(CharacterReference.encode(attribute.getValue()));
					sb.append('"');
				}
			}
		}
		if (startTag.getElement().getEndTag() == null
				&& !HTMLElements.getEndTagOptionalElementNames().contains(startTag.getName()))
			sb.append(" /");
		sb.append('>');
		return sb;
	}

	private static String getEndTagHTML(String tagName) {
		return new StringBuilder().append("</").append(tagName).append('>').toString();
	}

}