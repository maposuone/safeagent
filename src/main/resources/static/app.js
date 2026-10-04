'use strict';

// 表示用の日本語。APIの項目名・操作コードはそのまま保持する。
const fieldLabels = {
    status: '処理状況', mode: '依頼モード', message: '説明', reason: '中止した理由', fileName: 'ファイル名',
    size: 'ファイルサイズ（バイト）', action: '操作内容', proposedKey: '変更対象', proposedValue: '変更後の値',
    riskLevel: '危険度', decision: '安全確認の結果', permissionAllowed: '操作の許可',
    humanApprovalRequired: '人による承認', firstAction: '最初の操作',
    firstRiskLevel: '最初の操作の危険度', firstDecision: '最初の安全確認',
    firstPermissionAllowed: '最初の操作の許可', secondAction: '次の操作',
    secondRiskLevel: '次の操作の危険度', secondDecision: '次の安全確認',
    secondPermissionAllowed: '次の操作の許可', analysisSummary: 'AIの確認結果',
    aiAnalysis: 'AIの確認結果', approvalStatus: '承認状況', modifiedKey: '変更した設定項目',
    newValue: '変更後の値', executionResult: '実行結果', evidence: '変更結果の確認',
    evidenceResult: '変更結果の確認', detectedSecrets: '検出された機密情報の種類',
    detectedPromptInjections: '検出された不正な指示', detectedPatterns: '検出された不正な指示',
    timestamp: '記録日時', error: 'エラー', code: 'エラーの種類', path: '要求先', service: 'サービス名',
    downloadFileName: 'ダウンロードファイル名', modifiedFileContent: '修正済みファイル内容',
    findingCount: '検出した問題数', proposedChangeCount: '変更候補数',
    maxChangesPerApproval: '1回の最大変更件数', selectedCount: '選択件数', modifiedCount: '変更確認済み件数',
    designFileName: '定数設計書', designTargetFileName: '設計書の対象ファイル',
    environment: '対象環境', designDifferenceCount: '設計書との差異件数',
    designComparisonStatus: '設計書比較結果'
};

const codeLabels = {
    EXPLAIN: '設定内容の説明', ANALYZE: '問題点の分析', FIX: '問題があれば修正',
    READ_CONFIG: '設定ファイルの読み取り', ANALYZE_CONFIG: '設定ファイルの確認',
    COMPARE_CONFIG: '設定ファイルの比較', MODIFY_CONFIG: '設定ファイルの変更',
    DELETE_CONFIG: '設定ファイルの削除', NO_ACTION: '追加の作業は不要',
    LOW: '低い（自動実行できます）', MEDIUM: '中程度（自動実行できます）', HIGH: '高い（変更前に承認が必要です）',
    CRITICAL: '非常に高い（実行できません）', ALLOW: '実行を許可しました',
    APPROVAL_REQUIRED: '変更するには承認が必要です', BLOCK: '実行を許可しません',
    BLOCKED: '安全上の理由で操作を中止しました', SAFE: '事前の安全確認を通過しました',
    SAVED: 'ファイルを保存しました', UP: '稼働中',
    WAITING_FOR_APPROVAL: '変更内容の確認と承認を待っています',
    COMPLETED: '処理が完了しました', COMPLETED_NO_CHANGE: '確認が完了しました。変更は不要です',
    READY_FOR_NEXT_ACTION: '次の操作を実行できます', STOPPED: '処理を停止しました',
    UNSUPPORTED_AUTONOMOUS_ACTION: 'この操作は自動実行に対応していません',
    FAILED: '処理に失敗しました', APPROVED: '承認されました', REJECTED: '承認されなかったため、変更していません',
    PENDING: '承認待ち', NOT_REQUIRED: '不要', NOT_APPLICABLE: '対象外',
    MODIFIED: '設定を変更しました', NOT_EXECUTED: '実行していません',
    VERIFIED: '実ファイルを読み直し、指定した値への変更を確認しました',
    UNVERIFIED: '指定した値に変更できたか確認できませんでした', NOT_CHECKED: '確認していません',
    EXECUTION_REJECTED: '承認されていないため、実行していません',
    EXECUTION_BLOCKED: '安全上の理由で実行を中止しました',
    EXECUTION_REQUIRES_FILE: '実行するには設定ファイルが必要です',
    EXECUTION_WAITING_FOR_APPROVAL: '実行前の承認を待っています',
    SECRET_DETECTED: '機密情報を検出したため、AIへの送信を中止しました',
    PROMPT_INJECTION_DETECTED: 'AIを不正に操作する指示を検出したため、中止しました',
    PASSWORD: 'パスワード', API_KEY: 'APIキー', SECRET: '秘密情報', TOKEN: '認証トークン',
    IGNORE_PREVIOUS_INSTRUCTIONS: '以前の指示を無視させる指示',
    ENVIRONMENT_VARIABLE_EXFILTRATION: '環境変数を外部へ送信させる指示',
    SYSTEM_PROMPT_EXTRACTION: 'AIの内部指示を開示させる指示',
    FILE_NOT_FOUND: 'ファイルが見つかりません', INVALID_KEY: '設定項目が入力されていません',
    INVALID_VALUE: '変更後の値が不正です', KEY_NOT_FOUND: '指定した設定項目が見つかりません',
    DUPLICATE_KEY: '同じ設定項目が複数あるため、変更しませんでした',
    READ_FAILED: 'ファイルを読み取れませんでした', MODIFY_FAILED: '設定を変更できませんでした',
    EVIDENCE: '変更結果の確認', REQUEST_FAILED: '要求を処理できませんでした',
    INVALID_REQUEST: '入力内容を確認してください', FILE_TOO_LARGE: 'ファイルのサイズが上限を超えています',
    IO_ERROR: 'ファイルの読み取り、または書き込みに失敗しました',
    ACCESS_BLOCKED: '許可されていない場所へのアクセスを中止しました',
    INVALID_SELECTION: '変更対象の選択が不正です', TOO_MANY_CHANGES: '1回の変更件数が上限を超えています',
    PARTIAL_OR_FAILED: '一部の変更、または変更確認に失敗しました',
    MATCH: '定数設計書と一致しています', MISMATCH: '定数設計書との差異があります',
    VALUE_MISMATCH: '設定値不一致', MISSING: '設定項目不足', DUPLICATE: '重複設定'
};

const codeFields = new Set([
    'status', 'mode', 'reason', 'action', 'riskLevel', 'decision', 'approvalStatus', 'executionResult',
    'evidence', 'evidenceResult', 'firstAction', 'secondAction', 'firstRiskLevel', 'secondRiskLevel',
    'firstDecision', 'secondDecision', 'detectedSecrets', 'detectedPromptInjections', 'detectedPatterns', 'code'
]);

function translateCode(value) {
    if (Object.hasOwn(codeLabels, value)) return codeLabels[value];
    if (value.includes(' | EVIDENCE: ')) {
        return value.split(' | EVIDENCE: ').map(translateCode).join('\n変更結果の確認：');
    }
    if (value.startsWith('MODIFIED: ')) return '設定を変更しました：' + value.slice(10);
    for (const prefix of ['MODIFY_FAILED', 'READ_FAILED']) {
        if (value.startsWith(prefix + ': ')) {
            return codeLabels[prefix] + '：' + translateCode(value.slice(prefix.length + 2));
        }
    }
    return value;
}

function displayValue(key, value) {
    if (value === null || value === undefined) return '情報なし';
    if (typeof value === 'boolean') {
        if (key === 'humanApprovalRequired') return value ? '必要です' : '不要です';
        if (key.endsWith('PermissionAllowed') || key === 'permissionAllowed') return value ? '許可されています' : '許可されていません';
        return value ? 'はい' : 'いいえ';
    }
    if (Array.isArray(value)) return value.length ? value.map(v => displayValue(key, v)).join('、') : '検出なし';
    if (typeof value === 'object') return formatResult(value);
    if (key === 'timestamp') {
        const date = new Date(value);
        if (!Number.isNaN(date.getTime())) return date.toLocaleString('ja-JP', { timeZone: 'Asia/Tokyo' }) + '（日本時間）';
    }
    return codeFields.has(key) ? translateCode(String(value)) : String(value);
}

function formatResult(data) {
    if (typeof data === 'string') return translateCode(data);
    if (!data || Object.keys(data).length === 0) return '作業履歴はまだありません。';
    return Object.entries(data)
        .filter(([key]) => ![
            'savedPath', 'modifiedFileContent', 'downloadFileName',
            'analysisSummary', 'findings', 'proposedChanges', 'changeResults', 'designDifferences'
        ].includes(key))
        .map(([key, value]) => (fieldLabels[key] || key) + '：' + displayValue(key, value))
        .join('\n\n');
}

let selectedFile = null;
let currentRunId = null;
let currentProposedChanges = [];
let downloadObjectUrl = null;
let busy = false;
let currentManualEditCandidates = [];
let currentFileContent = '';
let currentRequestMode = 'EXPLAIN';


function setText(id, value) {
    const element = document.getElementById(id);
    if (element) element.textContent = value;
}

function resetDashboardMetrics() {
    setText('metricDifferences', '0');
    setText('metricAuto', '0');
    setText('metricManual', '0');
    setText('metricStatus', '未実行');

    const statusCard = document.getElementById('metricStatusCard');
    if (statusCard) {
        statusCard.classList.remove('metric-ok', 'metric-ng');
    }

    const manualButton = document.getElementById('manualEditButton');
    if (manualButton) manualButton.classList.add('hidden');
}

function updateDashboardMetrics(data) {
    const differences = Array.isArray(data?.designDifferences)
        ? data.designDifferences.length
        : Number(data?.designDifferenceCount || 0);

    const autoChanges = Array.isArray(data?.proposedChanges)
        ? data.proposedChanges.length
        : Number(data?.proposedChangeCount || 0);

    const manualCount = Array.isArray(data?.manualEditCandidates)
        ? data.manualEditCandidates.length
        : Number(data?.manualEditCandidateCount || 0);

    setText('metricDifferences', String(differences));
    setText('metricAuto', String(autoChanges));
    setText('metricManual', String(manualCount));

    const comparison = data?.designComparisonStatus || 'NOT_APPLICABLE';
    const statusText = comparison === 'MATCH'
        ? '一致'
        : comparison === 'MISMATCH'
            ? '差異あり'
            : '対象外';

    setText('metricStatus', statusText);

    const statusCard = document.getElementById('metricStatusCard');
    if (statusCard) {
        statusCard.classList.remove('metric-ok', 'metric-ng');
        if (comparison === 'MATCH') statusCard.classList.add('metric-ok');
        if (comparison === 'MISMATCH') statusCard.classList.add('metric-ng');
    }

    const manualButton = document.getElementById('manualEditButton');
    if (manualButton) {
        const canOpen =
            currentRequestMode === 'FIX'
            && currentRunId
            && manualCount > 0;

        manualButton.classList.toggle('hidden', !canOpen);
    }
}

function updateFileLabel(inputId, labelId, emptyText) {
    const input = document.getElementById(inputId);
    const label = document.getElementById(labelId);
    if (!input || !label) return;

    const file = input.files?.[0];
    label.textContent = file ? file.name : emptyText;
    label.classList.toggle('has-file', Boolean(file));
}

function scrollToSection(id) {
    const element = document.getElementById(id);
    if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

function openManualEdit() {
    if (currentRequestMode !== 'FIX') {
        alert('手動修正はFIXモードで利用してください。');
        return;
    }

    if (!currentRunId || !selectedFile || currentManualEditCandidates.length === 0) {
        alert('先にFIXモードで設定ファイルを確認してください。');
        return;
    }

    window.open(
        '/manual-edit.html',
        'safeagent-manual-edit',
        'width=1500,height=850,resizable=yes,scrollbars=yes'
    );
}

function getManualEditPayload() {
    return {
        runId: currentRunId,
        fileName: selectedFile?.name || '',
        fileContent: currentFileContent,
        candidates: currentManualEditCandidates
    };
}

window.getManualEditPayload = getManualEditPayload;

function resetDownload() {
    const section = document.getElementById('downloadSection');
    const link = document.getElementById('downloadLink');
    section.classList.add('hidden');
    link.removeAttribute('href');
    link.removeAttribute('download');
    if (downloadObjectUrl) {
        URL.revokeObjectURL(downloadObjectUrl);
        downloadObjectUrl = null;
    }
}

function prepareDownload(data) {
    resetDownload();
    if (data.status !== 'COMPLETED' || data.evidence !== 'VERIFIED' ||
            typeof data.modifiedFileContent !== 'string') {
        return;
    }

    const blob = new Blob([data.modifiedFileContent], { type: 'text/plain;charset=utf-8' });
    downloadObjectUrl = URL.createObjectURL(blob);

    const link = document.getElementById('downloadLink');
    link.href = downloadObjectUrl;
    link.download = data.downloadFileName || data.fileName || 'safeagent-modified.conf';
    document.getElementById('downloadSection').classList.remove('hidden');
}

function setBusy(value) {
    busy = value;
    document.querySelectorAll('button').forEach(button => { button.disabled = value; });
}

async function requestJson(url, options) {
    const response = await fetch(url, options);
    let data;
    try { data = await response.json(); } catch (_) {
        throw new Error('サーバーの応答を読み取れませんでした。SafeAgentの起動状態を確認してください。');
    }
    if (!response.ok) {
        const defaults = { 400: '入力内容を確認してください。', 413: 'ファイルが大きすぎます。',
            500: 'サーバーで処理に失敗しました。', 502: 'AIから正常な応答を受け取れませんでした。',
            504: 'AIの応答が時間内に届きませんでした。' };
        throw new Error(data.message && /[ぁ-んァ-ヶ一-龯]/.test(data.message)
            ? data.message : (defaults[response.status] || '要求を処理できませんでした。') + '（HTTP ' + response.status + '）');
    }
    return data;
}

function showError(id, error) {
    document.getElementById(id).textContent = error instanceof TypeError
        ? 'SafeAgentに接続できませんでした。起動状態とネットワークを確認してください。'
        : error.message;
}

function clearFindings() {
    document.getElementById('findingsSection').classList.add('hidden');
    document.getElementById('findingsBody').innerHTML = '';
    document.getElementById('findingCount').textContent = '0';
}

function renderFindings(findings) {
    clearFindings();

    if (!Array.isArray(findings) || findings.length === 0) {
        return;
    }

    const body = document.getElementById('findingsBody');

    findings.forEach((item, index) => {
        const row = document.createElement('tr');
        const values = [
            index + 1,
            item.finding || '-',
            item.current || '-',
            item.recommendation || '-',
            item.reason || '-',
            item.editable ? '変更可能' : '変更対象外'
        ];

        values.forEach(value => {
            const td = document.createElement('td');
            td.textContent = value;
            row.appendChild(td);
        });

        body.appendChild(row);
    });

    document.getElementById('findingCount').textContent = String(findings.length);
    document.getElementById('findingsSection').classList.remove('hidden');
}


function clearDesignDifferences() {
    const section = document.getElementById('designDifferencesSection');
    const body = document.getElementById('designDifferencesBody');
    section.classList.add('hidden');
    body.innerHTML = '';
    document.getElementById('designDifferenceCount').textContent = '0';
}

function renderDesignDifferences(differences) {
    clearDesignDifferences();

    if (!Array.isArray(differences) || differences.length === 0) {
        document.getElementById('designComparisonMessage').textContent =
            '定数設計書との比較結果：差異はありません。';
        document.getElementById('designComparisonMessage').classList.remove('hidden');
        return;
    }

    document.getElementById('designComparisonMessage').classList.add('hidden');
    const body = document.getElementById('designDifferencesBody');

    differences.forEach((item, index) => {
        const row = document.createElement('tr');
        const values = [
            index + 1,
            translateCode(String(item.type || '-')),
            item.targetPath || '-',
            item.key || '-',
            item.actualValue ?? '-',
            item.expectedValue ?? '-',
            item.message || '-'
        ];

        values.forEach(value => {
            const td = document.createElement('td');
            td.textContent = value;
            row.appendChild(td);
        });

        body.appendChild(row);
    });

    document.getElementById('designDifferenceCount').textContent = String(differences.length);
    document.getElementById('designDifferencesSection').classList.remove('hidden');
}

function updateSelectionCount() {
    const checked = document.querySelectorAll('.change-checkbox:checked').length;
    document.getElementById('selectionCount').textContent = String(checked);
}

function onChangeSelection(event) {
    const checked = document.querySelectorAll('.change-checkbox:checked').length;

    if (checked > 5) {
        event.target.checked = false;
        alert('1回に変更できるのは最大5件です。');
    }

    updateSelectionCount();
}

function renderApprovalChanges(changes) {
    const body = document.getElementById('approvalChangesBody');
    body.innerHTML = '';

    currentProposedChanges = Array.isArray(changes) ? changes : [];

    currentProposedChanges.forEach((item, index) => {
        const row = document.createElement('tr');

        const selectCell = document.createElement('td');
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'change-checkbox';
        checkbox.dataset.index = String(index);
        checkbox.addEventListener('change', onChangeSelection);
        selectCell.appendChild(checkbox);
        row.appendChild(selectCell);

        [item.finding, item.target, item.current, item.key, item.currentValue, item.newValue, item.reason].forEach(value => {
            const td = document.createElement('td');
            td.textContent = value || '-';
            row.appendChild(td);
        });

        body.appendChild(row);
    });

    updateSelectionCount();
}

function renderExecutionResults(changeResults) {
    const section = document.getElementById('executionChangesSection');
    const body = document.getElementById('executionChangesBody');
    body.innerHTML = '';
    section.classList.add('hidden');

    if (!Array.isArray(changeResults) || changeResults.length === 0) {
        return;
    }

    changeResults.forEach((item, index) => {
        const row = document.createElement('tr');
        const values = [
            index + 1,
            item.target || '-',
            item.key || '-',
            item.newValue || '-',
            translateCode(String(item.executionResult || '-')),
            translateCode(String(item.evidence || '-'))
        ];

        values.forEach(value => {
            const td = document.createElement('td');
            td.textContent = value;
            row.appendChild(td);
        });

        body.appendChild(row);
    });

    section.classList.remove('hidden');
}

async function runAgent() {
    if (busy) return;

    const file = document.getElementById('file').files[0];
    const designFile = document.getElementById('designFile').files[0];
    const requestMode = document.querySelector('input[name="requestMode"]:checked')?.value;
    const environment = document.querySelector('input[name="environment"]:checked')?.value;

    if (!file) {
        alert('確認する設定ファイルを選んでください。');
        return;
    }

    if (!requestMode) {
        alert('AIへの依頼を選択してください。');
        return;
    }

    if ((requestMode === 'ANALYZE' || requestMode === 'FIX') && !designFile) {
        alert('ANALYZE / FIXでは定数設計書を選択してください。');
        return;
    }

    if ((requestMode === 'ANALYZE' || requestMode === 'FIX') && !environment) {
        alert('本番環境またはテスト環境を選んでください。');
        return;
    }

    selectedFile = null;
    currentRunId = null;
    currentProposedChanges = [];
    currentManualEditCandidates = [];
    currentFileContent = '';
    currentRequestMode = requestMode;
    resetDashboardMetrics();
    resetDownload();
    clearFindings();
    clearDesignDifferences();
    document.getElementById('designComparisonMessage').classList.add('hidden');

    document.getElementById('approvalSection').classList.add('hidden');
    document.getElementById('executionSection').classList.add('hidden');
    document.getElementById('executionChangesSection').classList.add('hidden');
    document.getElementById('executionResult').textContent = '';
    document.getElementById('result').textContent = '設定ファイルを確認しています。しばらくお待ちください。';

    currentFileContent = await file.text();

    const formData = new FormData();
    formData.append('requestMode', requestMode);
    formData.append('file', file);

    if (designFile) {
        formData.append('designFile', designFile);
    }

    if (environment) {
        formData.append('environment', environment);
    }

    setBusy(true);

    try {
        const data = await requestJson('/api/safeagent/run', { method: 'POST', body: formData });

        currentRunId = data.runId || null;
        currentManualEditCandidates = Array.isArray(data.manualEditCandidates)
            ? data.manualEditCandidates
            : [];

        if (requestMode === 'FIX') {
            selectedFile = file;
        }

        document.getElementById('result').textContent = formatResult(data);
        renderFindings(data.findings || []);
        renderDesignDifferences(data.designDifferences || []);
        updateDashboardMetrics(data);

        if (requestMode === 'FIX' && data.status === 'WAITING_FOR_APPROVAL') {
            const proposedChanges = Array.isArray(data.proposedChanges)
                ? data.proposedChanges
                : [];

            if (proposedChanges.length === 0) {
                document.getElementById('approvalSection').classList.add('hidden');
                document.getElementById('result').textContent +=
                    '\n\n安全に自動変更できる候補がありません。';
                return;
            }

            renderApprovalChanges(proposedChanges);
            document.getElementById('approvalSection').classList.remove('hidden');
        }

    } catch (error) {
        showError('result', error);
    } finally {
        setBusy(false);
    }
}

async function approve() {
    if (busy) return;

    if (!selectedFile) {
        alert('設定ファイルを選び、先に確認を実行してください。');
        return;
    }

    if (!currentRunId) {
        alert('実行履歴IDがありません。もう一度確認を実行してください。');
        return;
    }

    const selected = Array.from(
        document.querySelectorAll('.change-checkbox:checked')
    );

    if (selected.length === 0) {
        alert('変更する項目を1件以上選択してください。');
        return;
    }

    if (selected.length > 5) {
        alert('1回に変更できるのは最大5件です。');
        return;
    }

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('runId', currentRunId);

    selected.forEach(checkbox => {
        const index = Number(checkbox.dataset.index);
        const change = currentProposedChanges[index];
        formData.append('targets', change.target || '');
        formData.append('keys', change.key);
        formData.append('currentValues', change.currentValue || '');
        formData.append('newValues', change.newValue);
    });

    formData.append('approvalStatus', 'APPROVED');

    document.getElementById('executionSection').classList.remove('hidden');
    document.getElementById('executionResult').textContent = '選択した設定を1件ずつ変更し、結果を確認しています。';
    document.getElementById('executionChangesSection').classList.add('hidden');
    setBusy(true);

    try {
        const data = await requestJson(
            '/api/safeagent/approve-and-execute',
            { method: 'POST', body: formData }
        );

        document.getElementById('executionResult').textContent = formatResult(data);
        renderExecutionResults(data.changeResults || []);
        prepareDownload(data);
        document.getElementById('approvalSection').classList.add('hidden');

        selectedFile = null;
        currentRunId = null;
        currentProposedChanges = [];
        currentManualEditCandidates = [];
        currentFileContent = '';
        const manualButton = document.getElementById('manualEditButton');
        if (manualButton) manualButton.classList.add('hidden');

    } catch (error) {
        showError('executionResult', error);
        document.getElementById('executionResult').textContent +=
            '\n変更済みの可能性があります。再実行する前に作業履歴とサーバーの状態を確認してください。';
        document.getElementById('approvalSection').classList.add('hidden');

        selectedFile = null;
        currentRunId = null;
        currentProposedChanges = [];
        currentManualEditCandidates = [];
        currentFileContent = '';
        const manualButton = document.getElementById('manualEditButton');
        if (manualButton) manualButton.classList.add('hidden');
    } finally {
        setBusy(false);
    }
}

async function loadAudit() {
    window.open(
        '/history.html',
        'safeagent-history',
        'width=1500,height=800,resizable=yes,scrollbars=yes'
    );
}


function updateModeRequirement() {
    const requestMode =
        document.querySelector('input[name="requestMode"]:checked')?.value;

    const message =
        document.getElementById('modeRequirement');

    const designFile = document.getElementById('designFile');
    const environmentRadios =
        document.querySelectorAll('input[name="environment"]');

    currentRequestMode = requestMode || 'EXPLAIN';

    if (requestMode === 'EXPLAIN') {
        if (message) {
            message.textContent =
                '設定ファイルの内容をAIが分かりやすく説明します。変更は行いません。';
        }
        if (designFile) designFile.disabled = true;
        environmentRadios.forEach(radio => radio.disabled = true);
    } else if (requestMode === 'ANALYZE') {
        if (message) {
            message.textContent =
                '定数設計書と比較して、差異・問題点を確認します。変更は行いません。';
        }
        if (designFile) designFile.disabled = false;
        environmentRadios.forEach(radio => radio.disabled = false);
    } else if (requestMode === 'FIX') {
        if (message) {
            message.textContent =
                '定数設計書と比較して修正候補を提示し、承認された変更だけを実行します。';
        }
        if (designFile) designFile.disabled = false;
        environmentRadios.forEach(radio => radio.disabled = false);
    }

    document.querySelectorAll('.mode-card').forEach(card => {
        const radio = card.querySelector('input[type="radio"]');
        card.classList.toggle('selected', Boolean(radio?.checked));
    });
}

document.querySelectorAll(
    'input[name="requestMode"]'
).forEach(radio => {
    radio.addEventListener(
        'change',
        updateModeRequirement
    );
});

document.getElementById('file')?.addEventListener('change', () => {
    updateFileLabel('file', 'configFileName', '設定ファイルを選択');
});

document.getElementById('designFile')?.addEventListener('change', () => {
    updateFileLabel('designFile', 'designFileName', '定数設計書を選択');
});

resetDashboardMetrics();
updateModeRequirement();
