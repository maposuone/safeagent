'use strict';

let currentPage = 0;
let totalPages = 1;
let pageSize = 10;
let totalElements = 0;

const codeLabels = {
    EXPLAIN: 'EXPLAIN',
    ANALYZE: 'ANALYZE',
    FIX: 'FIX',
    READ_CONFIG: '設定ファイルの読み取り',
    ANALYZE_CONFIG: '設定ファイルの確認',
    COMPARE_CONFIG: '設定ファイルの比較',
    MODIFY_CONFIG: '設定ファイルの変更',
    NO_ACTION: '追加の作業は不要',
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '非常に高い',
    ALLOW: '実行許可',
    APPROVAL_REQUIRED: '変更するには承認が必要',
    BLOCK: '実行不可',
    APPROVED: '承認済',
    PENDING: '承認待ち',
    WAITING_FOR_APPROVAL: '承認待ち',
    REJECTED: '承認不可',
    NOT_REQUIRED: '承認不要',
    MODIFIED: '設定変更',
    NOT_EXECUTED: '-',
    VERIFIED: '変更確認済み',
    UNVERIFIED: '未確認',
    NOT_CHECKED: '未確認',
    COMPLETED: '完了'
};

function text(value, fallback = '-') {
    if (value === null || value === undefined || value === '') return fallback;
    return String(value);
}

function label(value) {
    const raw = text(value);
    return codeLabels[raw] || raw;
}

function pick(row, ...keys) {
    for (const key of keys) {
        if (row && row[key] !== null && row[key] !== undefined && row[key] !== '') {
            return row[key];
        }
    }
    return null;
}

function formatDate(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return text(value);
    return date.toLocaleString('ja-JP', {
        timeZone: 'Asia/Tokyo',
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    }).replace(' ', '\n');
}

function chip(value, kind) {
    const span = document.createElement('span');
    span.className = 'chip ' + kind;
    span.textContent = label(value);
    return span;
}

function modeChip(value) {
    const mode = text(value);
    const shown = {
        EXPLAIN: '説明',
        ANALYZE: '分析',
        FIX: '修正'
    }[mode] || label(mode);
    const span = document.createElement('span');
    span.className = 'chip chip-blue';
    span.textContent = shown;
    return span;
}

function riskChip(value) {
    const v = text(value);
    let kind = 'chip-gray';
    if (v === 'HIGH' || v === 'CRITICAL' || v === '高') kind = 'chip-red';
    else if (v === 'MEDIUM' || v === '中') kind = 'chip-orange';
    else if (v === 'LOW' || v === '低') kind = 'chip-green';
    return chip(v, kind);
}

function approvalChip(value) {
    const v = text(value);
    if (['APPROVED', '承認済'].includes(v)) return chip(v, 'chip-green');
    if (['PENDING', 'WAITING_FOR_APPROVAL', '承認待ち'].includes(v)) return chip(v, 'chip-orange');
    if (['REJECTED', '承認不可'].includes(v)) return chip(v, 'chip-red');
    return chip(v, 'chip-gray');
}

function evidenceChip(value) {
    const v = text(value);
    if (['VERIFIED', '変更確認済み'].includes(v)) return chip(v, 'chip-green');
    if (['UNVERIFIED', 'NOT_CHECKED', '未確認'].includes(v)) return chip(v, 'chip-gray');
    return chip(v, 'chip-gray');
}

function addCell(row, value, className) {
    const td = document.createElement('td');
    td.textContent = text(value);
    if (className) td.className = className;
    row.appendChild(td);
    return td;
}

function renderRows(rows) {
    const body = document.getElementById('historyBody');
    body.innerHTML = '';

    rows.forEach(item => {
        const tr = document.createElement('tr');

        addCell(tr, pick(item, 'runId', 'run_id', 'id'), 'run-id');
        addCell(tr, formatDate(pick(item, 'createdAt', 'created_at', 'timestamp', 'recordedAt')));
        addCell(tr, pick(item, 'fileName', 'file_name'), 'file-cell');
        addCell(tr, pick(item, 'userRequest', 'request', 'user_request'), 'truncate');

        const modeTd = document.createElement('td');
        modeTd.appendChild(modeChip(pick(item, 'requestMode', 'mode', 'request_mode')));
        tr.appendChild(modeTd);

        addCell(tr,
            label(pick(item, 'secondAction', 'action', 'firstAction', 'second_action', 'first_action')));

        const riskTd = document.createElement('td');
        riskTd.appendChild(riskChip(
            pick(item, 'secondRiskLevel', 'riskLevel', 'firstRiskLevel',
                'secondRisk', 'firstRisk', 'second_risk', 'first_risk')
        ));
        tr.appendChild(riskTd);

        addCell(tr,
            label(pick(item, 'secondDecision', 'decision', 'firstDecision',
                'second_decision', 'first_decision')));

        const approvalTd = document.createElement('td');
        approvalTd.appendChild(approvalChip(
            pick(item, 'approvalStatus', 'approval_status')
        ));
        tr.appendChild(approvalTd);

        addCell(tr,
            label(pick(item, 'executionResult', 'execution_result')));

        const evidenceTd = document.createElement('td');
        evidenceTd.appendChild(evidenceChip(
            pick(item, 'evidenceStatus', 'evidence', 'evidence_status')
        ));
        tr.appendChild(evidenceTd);

        body.appendChild(tr);
    });
}

function normalizeResponse(data) {
    if (Array.isArray(data)) {
        return {
            rows: data,
            page: currentPage,
            size: 10,
            totalElements: data.length,
            totalPages: data.length > 0 ? 1 : 0
        };
    }

    const rows =
        data?.content
        || data?.items
        || data?.records
        || data?.history
        || data?.rows
        || [];

    const size = Number(
        data?.size
        ?? data?.pageSize
        ?? data?.page_size
        ?? 10
    ) || 10;

    const total = Number(
        data?.totalElements
        ?? data?.total
        ?? data?.totalCount
        ?? data?.total_count
        ?? rows.length
    ) || 0;

    const page = Number(
        data?.number
        ?? data?.page
        ?? data?.pageNumber
        ?? currentPage
    ) || 0;

    const pages = Number(
        data?.totalPages
        ?? data?.pages
        ?? Math.ceil(total / size)
    );

    return {
        rows,
        page,
        size,
        totalElements: total,
        totalPages: Number.isFinite(pages) ? pages : 0
    };
}

function updatePagination(rowsCount) {
    const start = totalElements === 0 ? 0 : currentPage * pageSize + 1;
    const end = totalElements === 0
        ? 0
        : Math.min(currentPage * pageSize + rowsCount, totalElements);

    document.getElementById('pageLabel').textContent =
        start + ' - ' + end + ' / ' + totalElements;

    document.getElementById('totalCount').textContent = String(totalElements);

    const first = currentPage <= 0;
    const last = totalPages <= 1 || currentPage >= totalPages - 1;

    document.getElementById('firstButton').disabled = first;
    document.getElementById('prevButton').disabled = first;
    document.getElementById('nextButton').disabled = last;
    document.getElementById('lastButton').disabled = last;
}

async function loadHistory(page = 0) {
    const message = document.getElementById('historyMessage');
    const wrap = document.getElementById('historyTableWrap');

    message.className = 'loading';
    message.textContent = '作業履歴を読み込んでいます。';
    message.style.display = 'block';
    wrap.style.display = 'none';

    try {
        const response = await fetch('/api/history?page=' + page, {
            headers: { 'Accept': 'application/json' }
        });

        let data = null;
        try {
            data = await response.json();
        } catch (_) {
            throw new Error('作業履歴の応答を読み取れませんでした。');
        }

        if (!response.ok) {
            throw new Error(data?.message || '作業履歴を取得できませんでした。');
        }

        const normalized = normalizeResponse(data);

        currentPage = normalized.page;
        pageSize = normalized.size;
        totalElements = normalized.totalElements;
        totalPages = normalized.totalPages;

        renderRows(normalized.rows);
        updatePagination(normalized.rows.length);

        if (normalized.rows.length === 0) {
            message.textContent = '作業履歴はまだありません。';
            message.style.display = 'block';
            wrap.style.display = 'none';
        } else {
            message.style.display = 'none';
            wrap.style.display = 'block';
        }

    } catch (error) {
        message.className = 'error-message';
        message.textContent = error.message;
        message.style.display = 'block';
        wrap.style.display = 'none';
        totalElements = 0;
        totalPages = 0;
        updatePagination(0);
    }
}

function goFirst() {
    if (currentPage > 0) loadHistory(0);
}

function goPrev() {
    if (currentPage > 0) loadHistory(currentPage - 1);
}

function goNext() {
    if (currentPage + 1 < totalPages) loadHistory(currentPage + 1);
}

function goLast() {
    if (totalPages > 0) loadHistory(totalPages - 1);
}

loadHistory(0);
