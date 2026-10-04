'use strict';

let payload = null;
let executing = false;

function loadPayload() {
    try {
        if (!window.opener || typeof window.opener.getManualEditPayload !== 'function') {
            throw new Error('メイン画面から「設定ファイル手動修正」を開き直してください。');
        }

        payload = window.opener.getManualEditPayload();

        if (!payload || !payload.runId || !payload.fileName
                || typeof payload.fileContent !== 'string') {
            throw new Error('手動修正に必要な情報を取得できませんでした。');
        }

        document.getElementById('fileInfo').textContent =
            '▤  対象ファイル：' + payload.fileName
            + '   /   runId：' + payload.runId;

        renderCandidates(payload.candidates || []);

    } catch (error) {
        alert(error.message);
        document.getElementById('fileInfo').textContent = error.message;
    }
}

function renderCandidates(candidates) {
    const body = document.getElementById('manualEditBody');
    body.innerHTML = '';

    candidates.forEach((item, index) => {
        const row = document.createElement('tr');
        if (item.type === 'VALUE_MISMATCH') {
            row.dataset.status = 'mismatch';
        }

        const selectCell = document.createElement('td');
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'manual-checkbox';
        checkbox.dataset.index = String(index);
        checkbox.disabled = !item.editable;
        checkbox.addEventListener('change', onSelectionChanged);
        selectCell.appendChild(checkbox);
        row.appendChild(selectCell);

        const statusCell = document.createElement('td');

        const typeLabel = {
            MATCH: '一致',
            VALUE_MISMATCH: '不一致',
            MISSING: '設定なし',
            DUPLICATE: '複数一致',
            INVALID_XML_TARGET: '対象特定不可',
            UNSUPPORTED_FORMAT: '形式未対応'
        }[item.type] || (item.type || '-');

        const status = document.createElement('span');
        const editableText = item.editable ? '修正可能' : '修正不可';

        if (!item.editable) {
            status.className = 'status-chip status-disabled';
        } else if (item.type === 'VALUE_MISMATCH') {
            status.className = 'status-chip status-ng';
        } else {
            status.className = 'status-chip status-ok';
        }

        const icon = document.createElement('span');
        icon.className = 'status-icon';
        icon.textContent =
            !item.editable ? '−'
            : item.type === 'VALUE_MISMATCH' ? '!'
            : '✓';

        const label = document.createElement('span');
        label.textContent = typeLabel + ' / ' + editableText;

        status.appendChild(icon);
        status.appendChild(label);
        statusCell.appendChild(status);
        row.appendChild(statusCell);

        addCell(row, item.targetPath || item.target || '-');
        addCell(row, item.key || '-');
        addCell(row, item.designValue ?? '-');

        const valueCell = document.createElement('td');
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'manual-value';
        input.dataset.index = String(index);
        input.value = item.currentValue ?? '';
        input.disabled = true;
        valueCell.appendChild(input);
        row.appendChild(valueCell);

        addCell(row, item.reason || '-');
        body.appendChild(row);
    });

    updateSelectionCount();
}

function addCell(row, value) {
    const td = document.createElement('td');
    td.textContent = value;
    row.appendChild(td);
}

function onSelectionChanged(event) {
    const checked = document.querySelectorAll('.manual-checkbox:checked');

    document.querySelectorAll('.manual-checkbox').forEach(checkbox => {
        const index = checkbox.dataset.index;
        const input = document.querySelector('.manual-value[data-index="' + index + '"]');
        if (input) {
            input.disabled = !checkbox.checked;
        }
    });

    updateSelectionCount();
}

function updateSelectionCount() {
    const count = document.querySelectorAll('.manual-checkbox:checked').length;
    document.getElementById('manualSelectionCount').textContent = String(count);
}

async function executeManualEdit() {
    if (executing) return;

    const selected = Array.from(
        document.querySelectorAll('.manual-checkbox:checked')
    );

    if (selected.length === 0) {
        alert('修正する項目を1件以上選択してください。');
        return;
    }

    const formData = new FormData();
    const fileBlob = new Blob(
        [payload.fileContent],
        { type: 'text/plain;charset=utf-8' }
    );

    formData.append('file', fileBlob, payload.fileName);
    formData.append('runId', payload.runId);

    for (const checkbox of selected) {
        const index = Number(checkbox.dataset.index);
        const item = payload.candidates[index];
        const input = document.querySelector('.manual-value[data-index="' + index + '"]');
        const newValue = input ? input.value : '';

        if (newValue === item.currentValue) {
            alert('「' + item.key + '」の修正後の値が現在値と同じです。');
            return;
        }

        formData.append('targets', item.target || '');
        formData.append('keys', item.key || '');
        formData.append('currentValues', item.currentValue ?? '');
        formData.append('newValues', newValue);
    }

    executing = true;
    document.querySelectorAll('button').forEach(b => b.disabled = true);
    document.querySelectorAll('.manual-checkbox, .manual-value')
        .forEach(element => element.disabled = true);
    document.getElementById('manualResultSection').classList.remove('hidden');
    document.getElementById('manualResult').textContent =
        '設定ファイルを修正し、変更結果を確認しています。';

    try {
        const response = await fetch(
            '/api/safeagent/manual-edit-execute',
            { method: 'POST', body: formData }
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || '手動修正に失敗しました。');
        }

        document.getElementById('manualResult').textContent =
            '処理状況：' + (data.status || '-') + '\n'
            + '変更件数：' + (data.modifiedCount ?? '-') + '\n'
            + 'Evidence：' + (data.evidence || '-') + '\n'
            + '説明：' + (data.message || '-');

        if (data.status === 'COMPLETED'
                && data.evidence === 'VERIFIED'
                && typeof data.modifiedFileContent === 'string') {

            downloadModifiedFile(
                data.downloadFileName || payload.fileName,
                data.modifiedFileContent
            );
        }

    } catch (error) {
        document.getElementById('manualResult').textContent = error.message;
    } finally {
        executing = false;
        document.querySelectorAll('button').forEach(b => b.disabled = false);

        document.querySelectorAll('.manual-checkbox').forEach(checkbox => {
            const index = Number(checkbox.dataset.index);
            const item = payload?.candidates?.[index];
            checkbox.disabled = !item?.editable;

            const input = document.querySelector(
                '.manual-value[data-index="' + checkbox.dataset.index + '"]'
            );
            if (input) {
                input.disabled = !checkbox.checked || !item?.editable;
            }
        });
    }
}

function downloadModifiedFile(fileName, content) {
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}

loadPayload();
