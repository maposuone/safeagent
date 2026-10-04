package com.oasis.safeagent.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.oasis.safeagent.model.ConfigActualItem;
import com.oasis.safeagent.model.ConfigDesignDocument;
import com.oasis.safeagent.model.ConfigDesignItem;
import com.oasis.safeagent.model.ConfigDifference;

@Service
public class ConfigComparisonService {

    private final ConfigParserService configParserService;

    public ConfigComparisonService(
            ConfigParserService configParserService) {

        this.configParserService =
                configParserService;
    }

    /**
     * 設計書と実設定ファイルを形式共通で比較する。
     *
     * environment:
     * PROD / TEST
     */
    public List<ConfigDifference> compare(
            String actualFileName,
            String fileContent,
            ConfigDesignDocument design,
            String environment) {

        if (actualFileName == null
                || actualFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "設定ファイル名がありません。"
            );
        }

        if (!sameFileName(
                actualFileName,
                design.fileName())) {

            throw new IllegalArgumentException(
                    "設定ファイルと定数設計書の対象ファイルが一致しません。"
                            + " 設定ファイル="
                            + actualFileName
                            + ", 設計書="
                            + design.fileName()
            );
        }

        String format =
                detectDesignFormat(
                        design
                );

        List<ConfigActualItem> actualItems =
                configParserService.parse(
                        format,
                        actualFileName,
                        fileContent
                );

        Map<String, List<ConfigActualItem>>
                actualByKey =
                new LinkedHashMap<>();

        for (ConfigActualItem actual
                : actualItems) {

            String comparisonKey =
                    createComparisonKey(
                            actual.targetPath(),
                            actual.key()
                    );

            actualByKey
                    .computeIfAbsent(
                            comparisonKey,
                            k -> new ArrayList<>()
                    )
                    .add(actual);
        }

        List<ConfigDifference> differences =
                new ArrayList<>();

        Set<String> designKeys =
                new LinkedHashSet<>();

        for (ConfigDesignItem item
                : design.items()) {

            /*
             * 1つの詳細シート内に別形式が混在していても、
             * 対象ファイル形式と一致する行だけ比較する。
             */
            if (!item.format().isBlank()
                    && !sameFormat(
                            format,
                            item.format()
                    )) {
                continue;
            }

            String comparisonKey =
                    createComparisonKey(
                            item.targetPath(),
                            item.key()
                    );

            designKeys.add(
                    comparisonKey
            );

            String expectedValue =
                    normalizeValue(
                            item.expectedValue(
                                    environment
                            )
                    );

            List<ConfigActualItem> matches =
                    actualByKey.get(
                            comparisonKey
                    );

            /*
             * 設計書には存在するが
             * 実設定ファイルには存在しない。
             */
            if (matches == null
                    || matches.isEmpty()) {

                differences.add(
                        new ConfigDifference(
                                item.no(),
                                "MISSING",
                                item.targetPath(),
                                item.key(),
                                null,
                                expectedValue,
                                item.description(),
                                item.rationale(),
                                "定数設計書に定義されていますが、設定ファイルに存在しません。"
                        )
                );

                continue;
            }

            /*
             * 同じ対象位置 + 同じキーが複数存在する。
             */
            if (matches.size() > 1) {

                String actualValues =
                        matches.stream()
                                .map(ConfigActualItem::value)
                                .map(this::normalizeValue)
                                .reduce(
                                        (a, b) ->
                                                a
                                                        + " / "
                                                        + b
                                )
                                .orElse("");

                differences.add(
                        new ConfigDifference(
                                item.no(),
                                "DUPLICATE",
                                item.targetPath(),
                                item.key(),
                                actualValues,
                                expectedValue,
                                item.description(),
                                item.rationale(),
                                "同じ対象位置と設定項目が複数存在します。"
                        )
                );

                continue;
            }

            String actualValue =
                    normalizeValue(
                            matches.get(0)
                                    .value()
                    );

            if (!expectedValue.equals(
                    actualValue)) {

                differences.add(
                        new ConfigDifference(
                                item.no(),
                                "VALUE_MISMATCH",
                                item.targetPath(),
                                item.key(),
                                actualValue,
                                expectedValue,
                                item.description(),
                                item.rationale(),
                                "設定ファイルの値が定数設計書と一致していません。"
                        )
                );
            }
        }

        /*
         * 実ファイルにはあるが
         * 定数設計書には定義されていない項目。
         */
        int undesignedNo =
                -1;

        for (Map.Entry<String, List<ConfigActualItem>>
                entry : actualByKey.entrySet()) {

            if (designKeys.contains(
                    entry.getKey())) {
                continue;
            }

            for (ConfigActualItem actual
                    : entry.getValue()) {

                differences.add(
                        new ConfigDifference(
                                undesignedNo--,
                                "UNDESIGNED",
                                actual.targetPath(),
                                actual.key(),
                                normalizeValue(
                                        actual.value()
                                ),
                                null,
                                "",
                                "",
                                "設定ファイルに存在しますが、定数設計書に定義されていません。"
                        )
                );
            }
        }

        return differences;
    }

    private String detectDesignFormat(
            ConfigDesignDocument design) {

        return design.items()
                .stream()
                .map(ConfigDesignItem::format)
                .filter(v ->
                        v != null
                                && !v.isBlank())
                .findFirst()
                .orElseGet(() ->
                        detectFormatFromFileName(
                                design.fileName()
                        )
                );
    }

    private String detectFormatFromFileName(
            String fileName) {

        if (fileName == null) {
            return "";
        }

        String lower =
                fileName.toLowerCase(
                        Locale.ROOT
                );

        if (lower.endsWith(".xml")) {
            return "xml";
        }

        if (lower.endsWith(".properties")) {
            return "properties";
        }

        if (lower.endsWith(".conf")
                || lower.endsWith(".cfg")
                || lower.endsWith(".ini")) {
            return "conf";
        }

        return "";
    }

    private boolean sameFormat(
            String left,
            String right) {

        String a =
                normalizeFormat(left);

        String b =
                normalizeFormat(right);

        return a.equals(b);
    }

    private String normalizeFormat(
            String value) {

        if (value == null) {
            return "";
        }

        String lower =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if ("cfg".equals(lower)
                || "ini".equals(lower)) {
            return "conf";
        }

        return lower;
    }

    /**
     * 比較キーは
     *
     * 対象位置／パス + 設定項目名
     *
     * XMLの [1] は省略しても同一とみなす。
     * [2]以降は区別する。
     */
    private String createComparisonKey(
            String targetPath,
            String key) {

        return canonicalPath(
                targetPath
        )
                + "|"
                + normalizeKey(
                        key
                );
    }

    private String canonicalPath(
            String path) {

        if (path == null
                || path.isBlank()) {
            return "global";
        }

        String normalized =
                path.trim()
                        .replaceAll(
                                "\\[1\\]",
                                ""
                        )
                        .replaceAll(
                                "\\s*/\\s*",
                                "/"
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        return normalized;
    }

    private String normalizeKey(
            String key) {

        if (key == null) {
            return "";
        }

        return key.trim();
    }

    private String normalizeValue(
            String value) {

        if (value == null) {
            return "";
        }

        String normalized =
                value.trim();

        if (normalized.length() >= 2
                && ((normalized.startsWith("\"")
                && normalized.endsWith("\""))
                || (normalized.startsWith("'")
                && normalized.endsWith("'")))) {

            normalized =
                    normalized.substring(
                            1,
                            normalized.length() - 1
                    );
        }

        return normalized.trim();
    }

    private boolean sameFileName(
            String actual,
            String design) {

        return baseName(actual)
                .equalsIgnoreCase(
                        baseName(design)
                );
    }

    private String baseName(
            String value) {

        if (value == null) {
            return "";
        }

        String normalized =
                value.replace(
                        "\\",
                        "/"
                );

        int slash =
                normalized.lastIndexOf('/');

        if (slash >= 0) {
            normalized =
                    normalized.substring(
                            slash + 1
                    );
        }

        return normalized.trim();
    }
}
