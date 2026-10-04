package com.oasis.safeagent.service.parser;

import java.util.List;

import com.oasis.safeagent.model.ConfigActualItem;

public interface ConfigParser {

    /**
     * このParserが対象形式を処理できるか判定する。
     */
    boolean supports(
            String format,
            String fileName);

    /**
     * 設定ファイルを共通形式へ変換する。
     */
    List<ConfigActualItem> parse(
            String content);
}
