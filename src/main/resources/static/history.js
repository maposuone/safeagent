'use strict';

let currentPage = 0;
let totalPages = 1;
let totalElements = 0;

const pageSize = 10;


const labels = {

    FIX: '問題があれば修正',
    ANALYZE: '問題点の分析',
    EXPLAIN: '設定内容の説明',

    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '非常に高い',

    ALLOW: '実行を許可',
    APPROVAL_REQUIRED: '変更するには承認が必要',
    BLOCK: '実行を許可しない',

    APPROVED: '承認済',
    PENDING: '承認待ち',
    NOT_REQUIRED: '承認不要',
    NOT_APPLICABLE: '対象外',

    MODIFIED: '設定変更',
    VERIFIED: '変更確認済み',
    NOT_CHECKED: '未確認',

    READ_CONFIG: '設定ファイルの読み取り',
    ANALYZE_CONFIG: '設定ファイルの確認',
    MODIFY_CONFIG: '設定ファイルの変更',
    COMPARE_CONFIG: '設定ファイルの比較'
};


function display(value) {

    if (value === null ||
        value === undefined ||
        value === '') {

        return '-';
    }

    return labels[value] || value;
}


function formatDate(value) {

    if (!value) {
        return '-';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString(
        'ja-JP',
        {
            timeZone: 'Asia/Tokyo'
        }
    );
}


function createCell(row, value) {

    const td =
        document.createElement('td');

    td.textContent =
        display(value);

    row.appendChild(td);
}


async function loadHistory(page) {

    const response =
        await fetch(
            '/api/history?page=' + page
        );

    if (!response.ok) {

        alert(
            '作業履歴を取得できませんでした。'
        );

        return;
    }

    const data =
        await response.json();

    currentPage =
        data.page;

    totalPages =
        data.totalPages;

    totalElements =
        data.totalElements;

    const tbody =
        document.getElementById(
            'historyBody'
        );

    tbody.innerHTML = '';


    for (const item of data.items) {

        const row =
            document.createElement('tr');

        createCell(
            row,
            item.runId
        );

        createCell(
            row,
            formatDate(
                item.createdAt
            )
        );

        createCell(
            row,
            item.fileName
        );

        createCell(
            row,
            item.userRequest
        );

        createCell(
            row,
            item.requestMode
        );

        createCell(
            row,
            item.action
        );

        createCell(
            row,
            item.riskLevel
        );

        createCell(
            row,
            item.decision
        );

        createCell(
            row,
            item.approvalStatus
        );

        createCell(
            row,
            item.executionResult
        );

        createCell(
            row,
            item.evidenceStatus
        );

        tbody.appendChild(row);
    }


    updatePageInfo();
}


function updatePageInfo() {

    let start = 0;
    let end = 0;

    if (totalElements > 0) {

        start =
            currentPage * pageSize + 1;

        end =
            Math.min(
                start + pageSize - 1,
                totalElements
            );
    }

    document.getElementById(
        'pageInfo'
    ).textContent =
        start +
        ' - ' +
        end +
        ' / ' +
        totalElements;


    document.getElementById(
        'firstButton'
    ).disabled =
        currentPage === 0;


    document.getElementById(
        'prevButton'
    ).disabled =
        currentPage === 0;


    document.getElementById(
        'nextButton'
    ).disabled =
        currentPage >=
        totalPages - 1;


    document.getElementById(
        'lastButton'
    ).disabled =
        currentPage >=
        totalPages - 1;
}


document.getElementById(
    'firstButton'
).addEventListener(
    'click',
    () => loadHistory(0)
);


document.getElementById(
    'prevButton'
).addEventListener(
    'click',
    () => {

        if (currentPage > 0) {

            loadHistory(
                currentPage - 1
            );
        }
    }
);


document.getElementById(
    'nextButton'
).addEventListener(
    'click',
    () => {

        if (currentPage <
            totalPages - 1) {

            loadHistory(
                currentPage + 1
            );
        }
    }
);


document.getElementById(
    'lastButton'
).addEventListener(
    'click',
    () => {

        loadHistory(
            totalPages - 1
        );
    }
);


loadHistory(0);