-- 新規ローカル環境用。既存テーブルの定義変更は行わない。
-- RunHistoryServiceが使用する列に合わせた最小構成。
CREATE TABLE IF NOT EXISTS agent_run (
    id BIGSERIAL PRIMARY KEY,
    run_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    file_name TEXT,
    user_request TEXT,
    request_mode TEXT,
    first_action TEXT,
    first_risk TEXT,
    first_decision TEXT,
    second_action TEXT,
    second_risk TEXT,
    second_decision TEXT,
    approval_status TEXT,
    execution_result TEXT,
    evidence_status TEXT,
    final_status TEXT,
    analysis_summary TEXT
);
