package com.oasis.safeagent.model;

/**
 * 実際の設定ファイルから抽出した共通設定項目。
 *
 * properties / conf / xml などの形式差を吸収して、
 * 比較処理ではこの共通形式だけを使用する。
 */
public record ConfigActualItem(
        String format,
        String targetPath,
        String key,
        String value,
        String editTarget) {
}
