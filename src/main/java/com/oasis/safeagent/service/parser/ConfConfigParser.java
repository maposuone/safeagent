package com.oasis.safeagent.service.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.oasis.safeagent.model.ConfigActualItem;

@Component
public class ConfConfigParser
        implements ConfigParser {

    @Override
    public boolean supports(
            String format,
            String fileName) {

        if ("conf".equalsIgnoreCase(format)
                || "ini".equalsIgnoreCase(format)) {
            return true;
        }

        if (fileName == null) {
            return false;
        }

        String lower =
                fileName.toLowerCase(Locale.ROOT);

        return lower.endsWith(".conf")
                || lower.endsWith(".cfg")
                || lower.endsWith(".ini");
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

        Deque<String> sections =
                new ArrayDeque<>();

        String[] lines =
                content.split("\\R", -1);

        for (int i = 0;
             i < lines.length;
             i++) {

            String line =
                    lines[i].trim();

            if (line.isEmpty()
                    || line.startsWith("#")
                    || line.startsWith(";")) {
                continue;
            }

            /*
             * Apache形式:
             * <Directory />
             * <IfModule worker.c>
             */
            if (line.startsWith("</")
                    && line.endsWith(">")) {

                if (!sections.isEmpty()) {
                    sections.removeLast();
                }

                continue;
            }

            if (line.startsWith("<")
                    && line.endsWith(">")) {

                sections.addLast(line);
                continue;
            }

            /*
             * INI形式:
             * [section]
             */
            if (line.startsWith("[")
                    && line.endsWith("]")) {

                sections.clear();
                sections.addLast(line);
                continue;
            }

            int separator =
                    findWhitespaceOrEquals(
                            line
                    );

            if (separator < 0) {
                continue;
            }

            String key =
                    line.substring(
                            0,
                            separator
                    ).trim();

            int valueStart =
                    separator;

            while (valueStart < line.length()
                    && Character.isWhitespace(
                            line.charAt(valueStart))) {
                valueStart++;
            }

            if (valueStart < line.length()
                    && line.charAt(valueStart) == '=') {
                valueStart++;
            }

            while (valueStart < line.length()
                    && Character.isWhitespace(
                            line.charAt(valueStart))) {
                valueStart++;
            }

            String value =
                    valueStart < line.length()
                            ? line.substring(valueStart).trim()
                            : "";

            String targetPath =
                    sections.isEmpty()
                            ? "global"
                            : String.join(
                                    " / ",
                                    sections
                            );

            result.add(
                    new ConfigActualItem(
                            "conf",
                            targetPath,
                            key,
                            value,
                            "CONF:LINE:"
                                    + (i + 1)
                    )
            );
        }

        return result;
    }

    private int findWhitespaceOrEquals(
            String line) {

        for (int i = 0;
             i < line.length();
             i++) {

            char c =
                    line.charAt(i);

            if (Character.isWhitespace(c)
                    || c == '=') {
                return i;
            }
        }

        return -1;
    }
}
