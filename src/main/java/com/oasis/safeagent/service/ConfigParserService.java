package com.oasis.safeagent.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.oasis.safeagent.model.ConfigActualItem;
import com.oasis.safeagent.service.parser.ConfigParser;

@Service
public class ConfigParserService {

    private final List<ConfigParser> parsers;

    public ConfigParserService(
            List<ConfigParser> parsers) {

        this.parsers = parsers;
    }

    public List<ConfigActualItem> parse(
            String format,
            String fileName,
            String content) {

        ConfigParser parser =
                parsers.stream()
                        .filter(p ->
                                p.supports(
                                        format,
                                        fileName
                                ))
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "対応していない設定ファイル形式です。"
                                                + " format="
                                                + format
                                                + ", file="
                                                + fileName
                                )
                        );

        return parser.parse(content);
    }
}
