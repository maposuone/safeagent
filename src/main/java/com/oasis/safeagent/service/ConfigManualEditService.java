package com.oasis.safeagent.service;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.oasis.safeagent.model.ConfigDesignDocument;
import com.oasis.safeagent.model.ConfigDesignItem;
import com.oasis.safeagent.model.ManualEditCandidate;

@Service
public class ConfigManualEditService {

    /*
     * 設計書の設定項目を全件表示するための候補生成。
     * 差異がある項目だけではなく、設計書に定義された全項目を対象にする。
     */
    public List<ManualEditCandidate> createCandidates(
            String fileContent,
            ConfigDesignDocument design,
            String environment) {

        List<ManualEditCandidate> result = new ArrayList<>();

        if (design == null || design.items() == null) {
            return result;
        }

        for (ConfigDesignItem item : design.items()) {
            if (item == null) {
                continue;
            }

            String designValue = safe(item.expectedValue(environment));
            String format = safe(item.format()).toLowerCase();

            if ("xml".equals(format)) {
                result.add(createXmlCandidate(fileContent, item, designValue));
                continue;
            }

            if ("conf".equals(format)
                    || "properties".equals(format)
                    || "ini".equals(format)) {

                List<LineMatch> matches = findLineMatches(
                        fileContent,
                        item.targetPath(),
                        item.key()
                );

                if (matches.isEmpty()) {
                    result.add(new ManualEditCandidate(
                            item.no(),
                            "MISSING",
                            safe(item.targetPath()),
                            "",
                            safe(item.key()),
                            "",
                            designValue,
                            false,
                            "設計書には定義されていますが、設定ファイルに対象項目がありません。設定追加は手動修正対象外です。"
                    ));
                    continue;
                }

                for (LineMatch match : matches) {
                    boolean same = valuesEqual(match.currentValue(), designValue);

                    result.add(new ManualEditCandidate(
                            item.no(),
                            same ? "MATCH" : "VALUE_MISMATCH",
                            safe(item.targetPath()),
                            "LINE:" + match.lineNumber(),
                            safe(item.key()),
                            match.currentValue(),
                            designValue,
                            true,
                            matches.size() == 1
                                    ? (same
                                        ? "設計書の設定値と一致しています。必要に応じて手動変更できます。"
                                        : "設計書の設定値と異なります。手動変更できます。")
                                    : "同じ設計項目が実ファイル内に複数あるため、物理行ごとに表示しています。"
                    ));
                }

                continue;
            }

            result.add(new ManualEditCandidate(
                    item.no(),
                    "UNSUPPORTED_FORMAT",
                    safe(item.targetPath()),
                    "",
                    safe(item.key()),
                    "",
                    designValue,
                    false,
                    "このファイル形式は現在の手動修正Toolでは変更できません。"
            ));
        }

        return result;
    }

    private ManualEditCandidate createXmlCandidate(
            String fileContent,
            ConfigDesignItem item,
            String designValue) {

        String target = toXmlTarget(item.targetPath(), item.key());

        try {
            XmlTarget xmlTarget = parseXmlTarget(target);
            Document document = parseXml(fileContent);
            NodeList nodes = evaluateNodes(document, xmlTarget.xpath());

            if (nodes.getLength() != 1) {
                return new ManualEditCandidate(
                        item.no(),
                        nodes.getLength() == 0 ? "MISSING" : "DUPLICATE",
                        safe(item.targetPath()),
                        target,
                        safe(item.key()),
                        "",
                        designValue,
                        false,
                        nodes.getLength() == 0
                                ? "設計書には定義されていますが、対象XML属性が見つかりません。"
                                : "XPathが複数要素に一致するため、安全に修正対象を特定できません。"
                );
            }

            Node node = nodes.item(0);

            if (!(node instanceof Element element)
                    || !element.hasAttribute(xmlTarget.attribute())) {
                return new ManualEditCandidate(
                        item.no(),
                        "MISSING",
                        safe(item.targetPath()),
                        target,
                        safe(item.key()),
                        "",
                        designValue,
                        false,
                        "対象XML属性が存在しません。"
                );
            }

            String currentValue = element.getAttribute(xmlTarget.attribute());
            boolean same = valuesEqual(currentValue, designValue);

            return new ManualEditCandidate(
                    item.no(),
                    same ? "MATCH" : "VALUE_MISMATCH",
                    safe(item.targetPath()),
                    target,
                    safe(item.key()),
                    currentValue,
                    designValue,
                    true,
                    same
                            ? "設計書の設定値と一致しています。必要に応じて手動変更できます。"
                            : "設計書の設定値と異なります。手動変更できます。"
            );

        } catch (Exception e) {
            return new ManualEditCandidate(
                    item.no(),
                    "INVALID_XML_TARGET",
                    safe(item.targetPath()),
                    target,
                    safe(item.key()),
                    "",
                    designValue,
                    false,
                    "XMLの対象位置を安全に解析できませんでした。"
            );
        }
    }

    private List<LineMatch> findLineMatches(
            String content,
            String targetPath,
            String key) {

        List<LineMatch> matches = new ArrayList<>();

        if (content == null || content.isBlank()
                || key == null || key.isBlank()) {
            return matches;
        }

        String expectedSection = normalizeSection(targetPath);
        Deque<String> sectionStack = new ArrayDeque<>();
        List<String> lines = content.lines().toList();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = raw.trim();

            if (line.isBlank() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }

            if (isSectionOpen(line)) {
                sectionStack.push(line);
                continue;
            }

            if (isSectionClose(line)) {
                if (!sectionStack.isEmpty()) {
                    sectionStack.pop();
                }
                continue;
            }

            String currentSection = sectionStack.isEmpty()
                    ? "global"
                    : sectionStack.peek();

            if (!sectionEquals(expectedSection, currentSection)) {
                continue;
            }

            if (!matchesKey(line, key)) {
                continue;
            }

            matches.add(new LineMatch(
                    i + 1,
                    extractValue(line, key)
            ));
        }

        return matches;
    }

    private boolean isSectionOpen(String line) {
        return line.startsWith("<")
                && !line.startsWith("</")
                && line.endsWith(">");
    }

    private boolean isSectionClose(String line) {
        return line.startsWith("</") && line.endsWith(">");
    }

    private String normalizeSection(String targetPath) {
        if (targetPath == null || targetPath.isBlank()
                || "root".equalsIgnoreCase(targetPath)
                || "global".equalsIgnoreCase(targetPath)) {
            return "global";
        }
        return targetPath.trim();
    }

    private boolean sectionEquals(String expected, String actual) {
        return expected.equalsIgnoreCase(actual);
    }

    private boolean matchesKey(String line, String key) {
        if (!line.startsWith(key) || line.length() == key.length()) {
            return false;
        }

        char separator = line.charAt(key.length());
        return separator == '=' || Character.isWhitespace(separator);
    }

    private String extractValue(String line, String key) {
        String remainder = line.substring(key.length()).trim();
        if (remainder.startsWith("=")) {
            remainder = remainder.substring(1).trim();
        }
        return remainder;
    }

    private boolean valuesEqual(String actual, String expected) {
        return normalizeValue(actual).equals(normalizeValue(expected));
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() >= 2
                && normalized.startsWith("\"")
                && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.trim();
    }

    private String toXmlTarget(String targetPath, String key) {
        String path = safe(targetPath).trim();

        if (path.startsWith("XML:")) {
            return path;
        }
        if (path.contains("@")) {
            return "XML:" + path;
        }
        return "XML:" + path + "@" + safe(key).trim();
    }

    private XmlTarget parseXmlTarget(String selector) {
        if (selector == null || !selector.startsWith("XML:")) {
            throw new IllegalArgumentException("Invalid XML target");
        }

        String body = selector.substring(4).trim();
        int at = body.lastIndexOf('@');
        if (at <= 0 || at == body.length() - 1) {
            throw new IllegalArgumentException("Invalid XML target");
        }

        return new XmlTarget(
                body.substring(0, at).trim(),
                body.substring(at + 1).trim()
        );
    }

    private Document parseXml(String content) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(content)));
    }

    private NodeList evaluateNodes(
            Document document,
            String xpathExpression) throws Exception {

        XPath xpath = XPathFactory.newInstance().newXPath();
        return (NodeList) xpath.evaluate(
                xpathExpression,
                document,
                XPathConstants.NODESET
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record LineMatch(
            int lineNumber,
            String currentValue) {
    }

    private record XmlTarget(
            String xpath,
            String attribute) {
    }
}
