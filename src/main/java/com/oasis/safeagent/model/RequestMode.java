package com.oasis.safeagent.model;

public enum RequestMode {

    EXPLAIN,
    ANALYZE,
    FIX;

    public static RequestMode from(
            String value) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "AIへの依頼モードが指定されていません。"
            );
        }

        try {
            return RequestMode.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "不正なAI依頼モードです。"
                            + " EXPLAIN / ANALYZE / FIX のいずれかを指定してください。"
            );
        }
    }

    public String internalRequest() {

        return switch (this) {

            case EXPLAIN ->
                    "この設定ファイルの内容を日本語で分かりやすく説明してください。"
                    + "変更提案や変更実行は行わないでください。";

            case ANALYZE ->
                    "この設定ファイルを確認・分析してください。"
                    + "定数設計書との比較結果を踏まえて問題点を説明してください。"
                    + "変更実行は行わないでください。";

            case FIX ->
                    "この設定ファイルを確認し、定数設計書との比較結果を踏まえて"
                    + "修正が必要な箇所を提案してください。"
                    + "実際の変更は必ず承認後に行ってください。";
        };
    }
}
