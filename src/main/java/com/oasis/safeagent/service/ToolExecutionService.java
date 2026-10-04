package com.oasis.safeagent.service;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
public class ToolExecutionService {

    private static final String XML_PREFIX = "XML:";
    private static final String LINE_PREFIX = "LINE:";

    private final Path workDirectory;

    public ToolExecutionService() {
        this.workDirectory = Paths.get(
                System.getProperty("java.io.tmpdir"),
                "safeagent"
        ).toAbsolutePath().normalize();
    }

    /*
     * 今までのAgentExecutionServiceとの互換用。
     * ファイルを指定しない状態では、本物のToolを実行させない。
     */
    public String execute(String action) {

        if (action == null || action.isBlank()) {
            return "EXECUTION_REJECTED";
        }

        return switch (action.toUpperCase()) {
            case "READ_CONFIG",
                 "ANALYZE_CONFIG",
                 "COMPARE_CONFIG",
                 "MODIFY_CONFIG" ->
                    "EXECUTION_REQUIRES_FILE";

            default ->
                    "EXECUTION_BLOCKED";
        };
    }

    /*
     * アップロードされた設定ファイルをSafeAgent専用領域に保存する。
     */
    public Path saveFile(
            String originalFileName,
            byte[] content) throws IOException {

        Files.createDirectories(workDirectory);

        String safeFileName = sanitizeFileName(originalFileName);

        Path target = workDirectory
                .resolve(System.currentTimeMillis() + "-" + safeFileName)
                .normalize();

        validateWorkspacePath(target);

        Files.write(
                target,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return target;
    }

    /*
     * 実際に設定ファイルを読むTool
     */
    public String readConfig(Path filePath) throws IOException {

        Path safePath = validateWorkspacePath(filePath);

        if (!Files.exists(safePath)) {
            return "READ_FAILED: FILE_NOT_FOUND";
        }

        return Files.readString(
                safePath,
                StandardCharsets.UTF_8
        );
    }

    /*
     * properties / Apache形式 / XML属性の変更に対応する。
     *
     * XMLの変更対象は次の形式を使用する。
     * XML:/Server/Service/Engine/Host@autoDeploy
     */
    public String modifyConfig(
            Path filePath,
            String key,
            String newValue) throws IOException {

        Path safePath = validateWorkspacePath(filePath);

        if (!Files.exists(safePath)) {
            return "MODIFY_FAILED: FILE_NOT_FOUND";
        }

        if (key == null || key.isBlank()) {
            return "MODIFY_FAILED: INVALID_KEY";
        }

        if (newValue == null) {
            return "MODIFY_FAILED: INVALID_VALUE";
        }

        if (isXmlSelector(key)) {
            return modifyXmlAttribute(safePath, key, newValue);
        }

        return modifyLineBasedConfig(safePath, key, newValue);
    }



    /*
     * 複数候補対応。
     *
     * LINE:<物理行番号> で、同じキーが複数ある設定でも変更場所を明示できる。
     * XMLは従来どおり XML:<絶対XPath>@<属性名> を使用する。
     *
     * expectedCurrentValue まで一致することを実行前に確認するため、
     * AIが誤った行番号や対象を返した場合は変更しない。
     */
    public boolean isEditableTarget(
            String fileContent,
            String target,
            String key,
            String expectedCurrentValue) {

        if (fileContent == null
                || target == null
                || target.isBlank()
                || key == null
                || key.isBlank()
                || expectedCurrentValue == null) {
            return false;
        }

        if (isXmlSelector(target)) {
            try {
                XmlTarget xmlTarget = parseXmlTarget(target);
                if (!xmlTarget.attribute().equals(key)) {
                    return false;
                }

                Document document = parseXml(fileContent);
                NodeList nodes = evaluateNodes(document, xmlTarget.xpath());

                if (nodes.getLength() != 1) {
                    return false;
                }

                Node node = nodes.item(0);

                return node instanceof Element element
                        && element.hasAttribute(xmlTarget.attribute())
                        && expectedCurrentValue.equals(
                                element.getAttribute(xmlTarget.attribute())
                        );

            } catch (Exception e) {
                return false;
            }
        }

        if (!isLineTarget(target)) {
            return false;
        }

        try {
            int lineNumber = parseLineNumber(target);
            List<String> lines = fileContent.lines().toList();

            if (lineNumber < 1 || lineNumber > lines.size()) {
                return false;
            }

            String trimmed = lines.get(lineNumber - 1).trim();

            if (trimmed.isEmpty()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith("!")) {
                return false;
            }

            return matchesKey(trimmed, key)
                    && expectedCurrentValue.equals(
                            extractValue(trimmed, key)
                    );

        } catch (RuntimeException e) {
            return false;
        }
    }

    public String modifyConfigAtTarget(
            Path filePath,
            String target,
            String key,
            String expectedCurrentValue,
            String newValue) throws IOException {

        Path safePath = validateWorkspacePath(filePath);

        if (!Files.exists(safePath)) {
            return "MODIFY_FAILED: FILE_NOT_FOUND";
        }

        if (target == null || target.isBlank()) {
            return "MODIFY_FAILED: INVALID_TARGET";
        }

        if (key == null || key.isBlank()) {
            return "MODIFY_FAILED: INVALID_KEY";
        }

        if (expectedCurrentValue == null || newValue == null) {
            return "MODIFY_FAILED: INVALID_VALUE";
        }

        String currentContent = readConfig(safePath);

        if (!isEditableTarget(
                currentContent,
                target,
                key,
                expectedCurrentValue
        )) {
            return "MODIFY_FAILED: TARGET_STATE_CHANGED";
        }

        if (isXmlSelector(target)) {
            return modifyXmlAttribute(safePath, target, newValue);
        }

        if (isLineTarget(target)) {
            return modifyLineBasedConfigAtLine(
                    safePath,
                    parseLineNumber(target),
                    key,
                    expectedCurrentValue,
                    newValue
            );
        }

        return "MODIFY_FAILED: INVALID_TARGET";
    }

    public boolean verifyValueAtTarget(
            Path filePath,
            String target,
            String key,
            String expectedValue) throws IOException {

        if (target == null || target.isBlank()) {
            return false;
        }

        if (isXmlSelector(target)) {
            try {
                XmlTarget xmlTarget = parseXmlTarget(target);
                if (!xmlTarget.attribute().equals(key)) {
                    return false;
                }
            } catch (RuntimeException e) {
                return false;
            }

            return verifyXmlAttribute(
                    filePath,
                    target,
                    expectedValue
            );
        }

        if (!isLineTarget(target)) {
            return false;
        }

        Path safePath = validateWorkspacePath(filePath);
        List<String> lines = Files.readAllLines(
                safePath,
                StandardCharsets.UTF_8
        );

        int lineNumber;
        try {
            lineNumber = parseLineNumber(target);
        } catch (RuntimeException e) {
            return false;
        }

        if (lineNumber < 1 || lineNumber > lines.size()) {
            return false;
        }

        String trimmed = lines.get(lineNumber - 1).trim();

        return !trimmed.isEmpty()
                && !trimmed.startsWith("#")
                && !trimmed.startsWith("!")
                && matchesKey(trimmed, key)
                && expectedValue.equals(extractValue(trimmed, key));
    }

    /*
     * Geminiが提案した対象が実ファイル内で一意に変更できるかを、
     * Java側で再確認する。XMLではXPathが1要素だけを指し、
     * かつ対象属性が既に存在する場合だけtrueにする。
     */
    public boolean isUniqueEditableTarget(
            String fileContent,
            String key) {

        if (fileContent == null
                || key == null
                || key.isBlank()) {
            return false;
        }

        if (isXmlSelector(key)) {
            try {
                XmlTarget target = parseXmlTarget(key);
                Document document = parseXml(fileContent);
                NodeList nodes = evaluateNodes(document, target.xpath());

                if (nodes.getLength() != 1) {
                    return false;
                }

                Node node = nodes.item(0);

                return node instanceof Element element
                        && element.hasAttribute(target.attribute());

            } catch (Exception e) {
                return false;
            }
        }

        long count = fileContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.startsWith("!"))
                .filter(line -> matchesKey(line, key))
                .count();

        return count == 1;
    }

    /*
     * Evidence Validatorから再確認するときにも使える。
     */
    public boolean verifyValue(
            Path filePath,
            String key,
            String expectedValue) throws IOException {

        if (isXmlSelector(key)) {
            return verifyXmlAttribute(filePath, key, expectedValue);
        }

        String content = readConfig(filePath);

        return content.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.startsWith("!"))
                .anyMatch(line -> matchesExpectedValue(
                        line,
                        key,
                        expectedValue
                ));
    }

    private String modifyLineBasedConfig(
            Path safePath,
            String key,
            String newValue) throws IOException {

        List<String> lines = Files.readAllLines(
                safePath,
                StandardCharsets.UTF_8
        );

        int matchCount = 0;
        int targetIndex = -1;
        boolean equalsStyle = false;

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith("!")) {
                continue;
            }

            if (!matchesKey(trimmed, key)) {
                continue;
            }

            matchCount++;
            targetIndex = i;
            equalsStyle = isEqualsStyle(trimmed, key);
        }

        if (matchCount == 0) {
            return "MODIFY_FAILED: KEY_NOT_FOUND";
        }

        if (matchCount > 1) {
            return "MODIFY_FAILED: DUPLICATE_KEY";
        }

        createBackup(safePath);

        List<String> updatedLines = new ArrayList<>(lines);
        String originalLine = lines.get(targetIndex);
        String trimmedOriginal = originalLine.trim();
        String indentation = originalLine.substring(
                0,
                originalLine.indexOf(trimmedOriginal)
        );

        String updatedSetting = equalsStyle
                ? key + "=" + newValue
                : key + " " + newValue;

        updatedLines.set(
                targetIndex,
                indentation + updatedSetting
        );

        Files.write(
                safePath,
                updatedLines,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return "MODIFIED: " + updatedSetting;
    }



    private String modifyLineBasedConfigAtLine(
            Path safePath,
            int lineNumber,
            String key,
            String expectedCurrentValue,
            String newValue) throws IOException {

        List<String> lines = Files.readAllLines(
                safePath,
                StandardCharsets.UTF_8
        );

        if (lineNumber < 1 || lineNumber > lines.size()) {
            return "MODIFY_FAILED: LINE_NOT_FOUND";
        }

        int targetIndex = lineNumber - 1;
        String originalLine = lines.get(targetIndex);
        String trimmedOriginal = originalLine.trim();

        if (trimmedOriginal.isEmpty()
                || trimmedOriginal.startsWith("#")
                || trimmedOriginal.startsWith("!")
                || !matchesKey(trimmedOriginal, key)) {
            return "MODIFY_FAILED: TARGET_MISMATCH";
        }

        String actualCurrentValue = extractValue(trimmedOriginal, key);

        if (!expectedCurrentValue.equals(actualCurrentValue)) {
            return "MODIFY_FAILED: TARGET_STATE_CHANGED";
        }

        createBackup(safePath);

        boolean equalsStyle = isEqualsStyle(trimmedOriginal, key);
        String indentation = originalLine.substring(
                0,
                originalLine.indexOf(trimmedOriginal)
        );

        String updatedSetting = equalsStyle
                ? key + "=" + newValue
                : key + " " + newValue;

        List<String> updatedLines = new ArrayList<>(lines);
        updatedLines.set(targetIndex, indentation + updatedSetting);

        Files.write(
                safePath,
                updatedLines,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return "MODIFIED: " + targetIndexToDisplay(lineNumber)
                + " " + updatedSetting;
    }

    private String modifyXmlAttribute(
            Path safePath,
            String selector,
            String newValue) throws IOException {

        try {
            String originalContent = Files.readString(
                    safePath,
                    StandardCharsets.UTF_8
            );

            boolean hadXmlDeclaration =
                    originalContent.stripLeading().startsWith("<?xml");

            XmlTarget target = parseXmlTarget(selector);
            Document document = parseXml(originalContent);
            NodeList nodes = evaluateNodes(document, target.xpath());

            if (nodes.getLength() == 0) {
                return "MODIFY_FAILED: XML_TARGET_NOT_FOUND";
            }

            if (nodes.getLength() > 1) {
                return "MODIFY_FAILED: XML_TARGET_NOT_UNIQUE";
            }

            Node node = nodes.item(0);

            if (!(node instanceof Element element)) {
                return "MODIFY_FAILED: XML_TARGET_NOT_ELEMENT";
            }

            if (!element.hasAttribute(target.attribute())) {
                return "MODIFY_FAILED: XML_ATTRIBUTE_NOT_FOUND";
            }

            createBackup(safePath);

            element.setAttribute(target.attribute(), newValue);
            writeXml(document, safePath, hadXmlDeclaration);

            return "MODIFIED: " + selector + "=" + newValue;

        } catch (SecurityException e) {
            return "MODIFY_FAILED: XML_SECURITY_BLOCKED";
        } catch (Exception e) {
            return "MODIFY_FAILED: INVALID_XML";
        }
    }

    private boolean verifyXmlAttribute(
            Path filePath,
            String selector,
            String expectedValue) throws IOException {

        try {
            Path safePath = validateWorkspacePath(filePath);
            String content = Files.readString(
                    safePath,
                    StandardCharsets.UTF_8
            );

            XmlTarget target = parseXmlTarget(selector);
            Document document = parseXml(content);
            NodeList nodes = evaluateNodes(document, target.xpath());

            if (nodes.getLength() != 1) {
                return false;
            }

            Node node = nodes.item(0);

            return node instanceof Element element
                    && element.hasAttribute(target.attribute())
                    && expectedValue.equals(
                            element.getAttribute(target.attribute())
                    );

        } catch (Exception e) {
            return false;
        }
    }

    private boolean isXmlSelector(String key) {
        return key != null && key.startsWith(XML_PREFIX);
    }

    private XmlTarget parseXmlTarget(String selector) {

        if (!isXmlSelector(selector)) {
            throw new IllegalArgumentException("XML selector is required.");
        }

        String body = selector.substring(XML_PREFIX.length()).trim();
        int attributeSeparator = body.lastIndexOf('@');

        if (attributeSeparator <= 0
                || attributeSeparator == body.length() - 1) {
            throw new IllegalArgumentException("Invalid XML selector.");
        }

        String xpath = body.substring(0, attributeSeparator).trim();
        String attribute = body.substring(attributeSeparator + 1).trim();

        validateXPath(xpath);

        if (!attribute.matches("[A-Za-z_][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException("Invalid XML attribute.");
        }

        return new XmlTarget(xpath, attribute);
    }

    private void validateXPath(String xpath) {

        if (!xpath.startsWith("/")
                || xpath.contains("//")
                || xpath.contains("..")
                || xpath.contains("*")
                || xpath.contains("|")
                || xpath.contains("(")
                || xpath.contains(")")
                || !xpath.matches("[A-Za-z0-9_:/@.\\-\\[\\]='\"\\s]+")) {
            throw new IllegalArgumentException("Unsafe XML XPath.");
        }
    }

    private Document parseXml(String content) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

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

        return builder.parse(
                new InputSource(new StringReader(content))
        );
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

    private void writeXml(
            Document document,
            Path target,
            boolean includeDeclaration) throws Exception {

        TransformerFactory factory =
                TransformerFactory.newInstance();

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(
                OutputKeys.OMIT_XML_DECLARATION,
                includeDeclaration ? "no" : "yes"
        );
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(
                new DOMSource(document),
                new StreamResult(target.toFile())
        );
    }

    private void createBackup(Path safePath) throws IOException {

        Path backupPath = safePath.resolveSibling(
                safePath.getFileName().toString() + ".bak"
        );

        /*
         * 複数変更時も最初の原本バックアップを保持する。
         * 2件目以降の変更で .bak を上書きしない。
         */
        if (!Files.exists(backupPath)) {
            Files.copy(
                    safePath,
                    backupPath
            );
        }
    }

    private boolean matchesKey(
            String line,
            String key) {

        if (!line.startsWith(key)
                || line.length() == key.length()) {
            return false;
        }

        char separator = line.charAt(key.length());

        return separator == '='
                || Character.isWhitespace(separator);
    }

    private boolean isEqualsStyle(
            String line,
            String key) {

        int index = key.length();

        while (index < line.length()
                && Character.isWhitespace(line.charAt(index))) {
            index++;
        }

        return index < line.length()
                && line.charAt(index) == '=';
    }

    private boolean matchesExpectedValue(
            String line,
            String key,
            String expectedValue) {

        if (!matchesKey(line, key)) {
            return false;
        }

        String remainder = line.substring(key.length()).trim();

        if (remainder.startsWith("=")) {
            remainder = remainder.substring(1).trim();
        }

        return expectedValue.equals(remainder);
    }



    private boolean isLineTarget(String target) {
        return target != null && target.startsWith(LINE_PREFIX);
    }

    private int parseLineNumber(String target) {
        if (!isLineTarget(target)) {
            throw new IllegalArgumentException("LINE target is required.");
        }

        String value = target.substring(LINE_PREFIX.length()).trim();
        int lineNumber = Integer.parseInt(value);

        if (lineNumber < 1) {
            throw new IllegalArgumentException("Invalid line number.");
        }

        return lineNumber;
    }

    private String extractValue(
            String line,
            String key) {

        if (!matchesKey(line, key)) {
            return "";
        }

        String remainder = line.substring(key.length()).trim();

        if (remainder.startsWith("=")) {
            remainder = remainder.substring(1).trim();
        }

        return remainder;
    }

    private String targetIndexToDisplay(int lineNumber) {
        return LINE_PREFIX + lineNumber + ":";
    }

    private Path validateWorkspacePath(Path path) {

        Path normalized =
                path.toAbsolutePath().normalize();

        if (!normalized.startsWith(workDirectory)) {
            throw new SecurityException(
                    "SafeAgentの作業領域の外にはアクセスできません。"
            );
        }

        return normalized;
    }

    private String sanitizeFileName(String originalFileName) {

        if (originalFileName == null
                || originalFileName.isBlank()) {
            return "config.properties";
        }

        String normalized =
                originalFileName.replace("\\", "/");

        int lastSlash =
                normalized.lastIndexOf('/');

        if (lastSlash >= 0) {
            normalized =
                    normalized.substring(lastSlash + 1);
        }

        normalized =
                normalized.replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
                );

        if (normalized.isBlank()) {
            return "config.properties";
        }

        return normalized;
    }

    private record XmlTarget(
            String xpath,
            String attribute) {
    }
}
