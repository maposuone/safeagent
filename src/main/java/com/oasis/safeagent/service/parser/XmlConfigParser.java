package com.oasis.safeagent.service.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.oasis.safeagent.model.ConfigActualItem;

@Component
public class XmlConfigParser
        implements ConfigParser {

    @Override
    public boolean supports(
            String format,
            String fileName) {

        if ("xml".equalsIgnoreCase(format)) {
            return true;
        }

        return fileName != null
                && fileName.toLowerCase(Locale.ROOT)
                        .endsWith(".xml");
    }

    @Override
    public List<ConfigActualItem> parse(
            String content) {

        List<ConfigActualItem> result =
                new ArrayList<>();

        if (content == null
                || content.isBlank()) {
            return result;
        }

        try {
            Document document =
                    parseSecurely(content);

            Element root =
                    document.getDocumentElement();

            if (root == null) {
                return result;
            }

            String rootPath =
                    "/"
                            + root.getTagName();

            collect(
                    root,
                    rootPath,
                    result
            );

            return result;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "XML設定ファイルを解析できません。",
                    e
            );
        }
    }

    private void collect(
            Element element,
            String path,
            List<ConfigActualItem> result) {

        NamedNodeMap attributes =
                element.getAttributes();

        for (int i = 0;
             i < attributes.getLength();
             i++) {

            Node attribute =
                    attributes.item(i);

            String key =
                    attribute.getNodeName();

            String value =
                    attribute.getNodeValue();

            result.add(
                    new ConfigActualItem(
                            "xml",
                            path,
                            key,
                            value,
                            "XML:"
                                    + path
                                    + "@"
                                    + key
                    )
            );
        }

        NodeList children =
                element.getChildNodes();

        for (int i = 0;
             i < children.getLength();
             i++) {

            Node node =
                    children.item(i);

            if (!(node instanceof Element child)) {
                continue;
            }

            int index =
                    indexAmongSameNamedSiblings(
                            child
                    );

            String childPath =
                    path
                            + "/"
                            + child.getTagName()
                            + "["
                            + index
                            + "]";

            collect(
                    child,
                    childPath,
                    result
            );
        }
    }

    private int indexAmongSameNamedSiblings(
            Element element) {

        int index = 0;

        Node parent =
                element.getParentNode();

        if (parent == null) {
            return 1;
        }

        NodeList children =
                parent.getChildNodes();

        for (int i = 0;
             i < children.getLength();
             i++) {

            Node node =
                    children.item(i);

            if (node instanceof Element sibling
                    && sibling.getTagName()
                            .equals(element.getTagName())) {

                index++;

                if (sibling == element) {
                    return index;
                }
            }
        }

        return 1;
    }

    private Document parseSecurely(
            String content) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory
                        .newInstance();

        factory.setNamespaceAware(false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        DocumentBuilder builder =
                factory.newDocumentBuilder();

        return builder.parse(
                new InputSource(
                        new StringReader(content)
                )
        );
    }
}
