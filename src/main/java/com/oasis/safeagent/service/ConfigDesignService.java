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

import com.oasis.safeagent.exception.SafeAgentException;
import com.oasis.safeagent.model.ConfigDesignDocument;
import com.oasis.safeagent.model.ConfigDesignItem;

@Service
public class ConfigDesignService {

    private final DataFormatter formatter =
            new DataFormatter();

    /*
     * 従来互換。
     *
     * 最初に見つかった
     * X-1 / X-2 を読み込む。
     */
    public ConfigDesignDocument load(
            MultipartFile designFile)
            throws Exception {

        return loadInternal(
                designFile,
                null
        );
    }

    /*
     * 実際の設定ファイル名を指定して
     * 対応する設計を取得する。
     */
    public ConfigDesignDocument loadForTarget(
            MultipartFile designFile,
            String actualFileName)
            throws Exception {

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
            String actualFileName)
            throws Exception {

        if (designFile == null
                || designFile.isEmpty()) {

            throw SafeAgentException
                    .designFileRequired();
        }

        /*
         * Excel内で見つかった対象ファイル名。
         *
         * FILE_MISMATCH判定用。
         */
        List<String> discoveredTargets =
                new ArrayList<>();

        /*
         * actualFileName と一致する
         * X-1が存在したか。
         */
        boolean targetMatched = false;

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
                 * X-1またはX-2がなければ
                 * 次の番号を見る。
                 */
                if (fileSheet == null
                        || detailSheet == null) {

                    continue;
                }

                String designFileName =
                        readTargetFileName(
                                fileSheet
                        );

                /*
                 * X-1から対象ファイル名を
                 * 読めなかった。
                 */
                if (designFileName.isBlank()) {
                    continue;
                }

                discoveredTargets.add(
                        designFileName
                );

                /*
                 * load()の場合は
                 * 最初の有効設計を利用。
                 */
                if (actualFileName == null) {

                    List<ConfigDesignItem> items =
                            readDesignItems(
                                    detailSheet
                            );

                    if (!items.isEmpty()) {

                        return new ConfigDesignDocument(
                                designFileName,
                                items
                        );
                    }

                    continue;
                }

                /*
                 * 設定ファイルと
                 * 設計書の対象ファイルを比較。
                 */
                if (!sameFileName(
                        actualFileName,
                        designFileName)) {

                    continue;
                }

                /*
                 * ファイル名は一致した。
                 */
                targetMatched = true;

                List<ConfigDesignItem> items =
                        readDesignItems(
                                detailSheet
                        );

                /*
                 * ファイル名は一致したが
                 * X-2に設定項目がない。
                 */
                if (items.isEmpty()) {

                    throw SafeAgentException
                            .designNotFound();
                }

                return new ConfigDesignDocument(
                        designFileName,
                        items
                );
            }
        }

        /*
         * load()で有効な設計が
         * 何もなかった。
         */
        if (actualFileName == null) {

            throw SafeAgentException
                    .designNotFound();
        }

        /*
         * 同名のX-1は存在したが
         * 有効な設計を取得できなかった。
         */
        if (targetMatched) {

            throw SafeAgentException
                    .designNotFound();
        }

        /*
         * 設計書には対象ファイルがあるが、
         * 実際に選択した設定ファイル名とは違う。
         *
         * 例:
         * actual = httpd.conf
         * design = server.xml
         */
        if (!discoveredTargets.isEmpty()) {

            String designTarget =
                    discoveredTargets.get(0);

            throw SafeAgentException
                    .fileMismatch(
                            actualFileName,
                            designTarget
                    );
        }

        /*
         * X-1 / X-2や対象ファイル名自体を
         * 見つけられない。
         */
        throw SafeAgentException
                .designNotFound();
    }

    /*
     * X-1から対象設定ファイル名を取得。
     */
    private String readTargetFileName(
            Sheet sheet) {

        /*
         * 現在の標準テンプレートではB4。
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
         * B4にない場合は
         * シート全体から設定ファイル名を探索。
         */
        for (Row currentRow : sheet) {

            for (Cell cell : currentRow) {

                String value =
                        getCellValue(
                                cell
                        );

                if (looksLikeConfigFileName(
                        value)) {

                    return value;
                }
            }
        }

        return "";
    }

    /*
     * X-2から定数一覧を読み込む。
     */
    private List<ConfigDesignItem>
            readDesignItems(
                    Sheet sheet) {

        List<ConfigDesignItem> items =
                new ArrayList<>();

        /*
         * 1行目 タイトル
         * 2行目 ヘッダー
         * 3行目以降 データ
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

            /*
             * 完全な空行。
             */
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
                 * 注記やタイトル行は無視。
                 */
                continue;
            }

            items.add(
                    new ConfigDesignItem(
                            no,
                            getCellValue(
                                    row.getCell(1)
                            ),
                            getCellValue(
                                    row.getCell(2)
                            ),
                            key,
                            getCellValue(
                                    row.getCell(4)
                            ),
                            getCellValue(
                                    row.getCell(5)
                            ),
                            getCellValue(
                                    row.getCell(6)
                            ),
                            getCellValue(
                                    row.getCell(7)
                            ),
                            getCellValue(
                                    row.getCell(8)
                            ),
                            getCellValue(
                                    row.getCell(9)
                            ),
                            getCellValue(
                                    row.getCell(10)
                            ),
                            getCellValue(
                                    row.getCell(11)
                            ),
                            getCellValue(
                                    row.getCell(12)
                            )
                    )
            );
        }

        return items;
    }

    /*
     * パスを除いたファイル名で比較する。
     */
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
                value
                        .trim()
                        .toLowerCase();

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
                .formatCellValue(
                        cell
                )
                .trim();
    }
}