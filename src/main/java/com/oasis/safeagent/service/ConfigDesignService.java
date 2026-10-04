package com.oasis.safeagent.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.oasis.safeagent.model.ConfigDesignDocument;
import com.oasis.safeagent.model.ConfigDesignItem;

@Service
public class ConfigDesignService {

    private final DataFormatter formatter =
            new DataFormatter();

    /**
     * 従来互換。
     * 設計書内で最初に見つかった X-1 / X-2 を読み込む。
     */
    public ConfigDesignDocument load(
            MultipartFile designFile) throws Exception {

        return loadInternal(
                designFile,
                null
        );
    }

    /**
     * 複数ファイル対応。
     *
     * 1-1 / 1-2
     * 2-1 / 2-2
     * 3-1 / 3-2 ...
     *
     * を順番に検索し、X-1のファイル名が
     * actualFileName と一致するX-2を使用する。
     */
    public ConfigDesignDocument loadForTarget(
            MultipartFile designFile,
            String actualFileName) throws Exception {

        if (actualFileName == null
                || actualFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "比較対象の設定ファイル名がありません。"
            );
        }

        return loadInternal(
                designFile,
                actualFileName
        );
    }

    private ConfigDesignDocument loadInternal(
            MultipartFile designFile,
            String actualFileName) throws Exception {

        if (designFile == null
                || designFile.isEmpty()) {

            throw new IllegalArgumentException(
                    "定数設計書が指定されていません。"
            );
        }

        try (InputStream input =
                     designFile.getInputStream();
             Workbook workbook =
                     new XSSFWorkbook(input)) {

            for (int index = 1;
                 index <= 999;
                 index++) {

                String fileSheetName =
                        index + "-1";

                String detailSheetName =
                        index + "-2";

                Sheet fileSheet =
                        workbook.getSheet(
                                fileSheetName
                        );

                Sheet detailSheet =
                        workbook.getSheet(
                                detailSheetName
                        );

                /*
                 * 途中番号が欠番でも、
                 * 後続シートが存在する可能性があるため
                 * すぐにはbreakしない。
                 */
                if (fileSheet == null
                        || detailSheet == null) {
                    continue;
                }

                String designFileName =
                        readTargetFileName(
                                fileSheet
                        );

                if (designFileName.isBlank()) {
                    continue;
                }

                if (actualFileName != null
                        && !sameFileName(
                                actualFileName,
                                designFileName
                        )) {
                    continue;
                }

                List<ConfigDesignItem> items =
                        readDesignItems(
                                detailSheet
                        );

                if (items.isEmpty()) {
                    continue;
                }

                return new ConfigDesignDocument(
                        designFileName,
                        items
                );
            }
        }

        if (actualFileName == null) {
            throw new IllegalArgumentException(
                    "定数設計書にX-1 / X-2形式の設定定義がありません。"
            );
        }

        throw new IllegalArgumentException(
                "定数設計書に対象ファイルの設計がありません。"
                        + " file="
                        + actualFileName
        );
    }

    private String readTargetFileName(
            Sheet sheet) {

        /*
         * 現行フォーマットはB4。
         */
        Row row =
                sheet.getRow(3);

        if (row != null) {
            String value =
                    getCellValue(
                            row.getCell(1)
                    );

            if (!value.isBlank()) {
                return value;
            }
        }

        /*
         * フォーマット差異に備え、
         * B4が空ならシート内の文字列を探索する。
         */
        for (Row currentRow : sheet) {

            for (Cell cell : currentRow) {

                String value =
                        getCellValue(cell);

                if (looksLikeConfigFileName(
                        value)) {

                    return value;
                }
            }
        }

        return "";
    }

    private List<ConfigDesignItem> readDesignItems(
            Sheet sheet) {

        List<ConfigDesignItem> items =
                new ArrayList<>();

        /*
         * 1行目タイトル
         * 2行目ヘッダー
         * 3行目以降データ
         */
        for (int rowIndex = 2;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row =
                    sheet.getRow(
                            rowIndex
                    );

            if (row == null) {
                continue;
            }

            String noText =
                    getCellValue(
                            row.getCell(0)
                    );

            String key =
                    getCellValue(
                            row.getCell(3)
                    );

            if (noText.isBlank()
                    && key.isBlank()) {
                continue;
            }

            int no;

            try {
                no =
                        Integer.parseInt(
                                noText
                                        .replace(
                                                ".0",
                                                ""
                                        )
                                        .trim()
                        );

            } catch (NumberFormatException e) {
                /*
                 * 注記行などは無視。
                 */
                continue;
            }

            items.add(
                    new ConfigDesignItem(
                            no,
                            getCellValue(row.getCell(1)),
                            getCellValue(row.getCell(2)),
                            key,
                            getCellValue(row.getCell(4)),
                            getCellValue(row.getCell(5)),
                            getCellValue(row.getCell(6)),
                            getCellValue(row.getCell(7)),
                            getCellValue(row.getCell(8)),
                            getCellValue(row.getCell(9)),
                            getCellValue(row.getCell(10)),
                            getCellValue(row.getCell(11)),
                            getCellValue(row.getCell(12))
                    )
            );
        }

        return items;
    }

    private boolean sameFileName(
            String actual,
            String design) {

        String actualName =
                normalizeFileName(
                        actual
                );

        String designName =
                normalizeFileName(
                        design
                );

        return actualName.equalsIgnoreCase(
                designName
        );
    }

    private String normalizeFileName(
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

    private boolean looksLikeConfigFileName(
            String value) {

        if (value == null
                || value.isBlank()) {
            return false;
        }

        String lower =
                value.toLowerCase();

        return lower.endsWith(".xml")
                || lower.endsWith(".conf")
                || lower.endsWith(".properties")
                || lower.endsWith(".cfg")
                || lower.endsWith(".ini")
                || lower.endsWith(".yaml")
                || lower.endsWith(".yml")
                || lower.endsWith(".json");
    }

    private String getCellValue(
            Cell cell) {

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }
}
