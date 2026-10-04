package com.oasis.safeagent.service.parser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.oasis.safeagent.model.ConfigActualItem;

@Component
public class PropertiesConfigParser
        implements ConfigParser {

    @Override
    public boolean supports(
            String format,
            String fileName) {

        if ("properties".equalsIgnoreCase(format)) {
            return true;
        }

        return fileName != null
                && fileName.toLowerCase()
                        .endsWith(".properties");
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

        String[] lines =
                content.split("\\R", -1);

        for (int i = 0;
             i < lines.length;
             i++) {

            String rawLine =
                    lines[i];

            String line =
                    rawLine.trim();

            if (line.isEmpty()
                    || line.startsWith("#")
                    || line.startsWith("!")) {
                continue;
            }

            int separator =
                    findSeparator(line);

            String key;
            String value;

            if (separator < 0) {
                key = line;
                value = "";
            } else {
                key =
                        line.substring(
                                0,
                                separator
                        ).trim();

                int valueStart =
                        separator;

                char separatorChar =
                        line.charAt(separator);

                if (separatorChar == '='
                        || separatorChar == ':') {
                    valueStart++;
                }

                while (valueStart
                        < line.length()
                        && Character.isWhitespace(
                                line.charAt(valueStart))) {
                    valueStart++;
                }

                if (valueStart
                        < line.length()
                        && (line.charAt(valueStart) == '='
                        || line.charAt(valueStart) == ':')) {
                    valueStart++;
                }

                while (valueStart
                        < line.length()
                        && Character.isWhitespace(
                                line.charAt(valueStart))) {
                    valueStart++;
                }

                value =
                        valueStart < line.length()
                                ? line.substring(valueStart)
                                : "";
            }

            if (key.isBlank()) {
                continue;
            }

            result.add(
                    new ConfigActualItem(
                            "properties",
                            "root",
                            key,
                            value,
                            "PROPERTY:LINE:"
                                    + (i + 1)
                    )
            );
        }

        return result;
    }

    private int findSeparator(
            String line) {

        boolean escaped =
                false;

        for (int i = 0;
             i < line.length();
             i++) {

            char c =
                    line.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '='
                    || c == ':'
                    || Character.isWhitespace(c)) {
                return i;
            }
        }

        return -1;
    }
}
