const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const elements = new Map();
function el(id) {
    if (!elements.has(id)) elements.set(id, { value: '', files: [], textContent: '', href: '', download: '',
        classList: { hidden: true, add() { this.hidden = true; }, remove() { this.hidden = false; } },
        removeAttribute(name) { this[name] = ''; } });
    return elements.get(id);
}
let nextResponse;
const submitted = [];
class FormData { constructor() { this.values = {}; } append(k, v) { this.values[k] = v; } }
const fakeUrl = { createObjectURL: () => 'blob:safeagent-test', revokeObjectURL: () => {} };
class Blob { constructor(parts, options) { this.parts = parts; this.options = options; } }
const context = vm.createContext({ console, FormData, Blob, URL: fakeUrl, Date, Error, TypeError,
    document: { getElementById: el, querySelectorAll: () => [] }, alert: () => {},
    fetch: async (url, options) => { submitted.push({ url, options }); return nextResponse; } });
vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/app.js'), 'utf8'), context);
function run(code) { return vm.runInContext(code, context); }
function response(data, status = 200) { nextResponse = { ok: status < 400, status, json: async () => data }; }
(async () => {
    const modeOutput = run(`formatResult({mode:'EXPLAIN', status:'COMPLETED'})`);
    assert.match(modeOutput, /依頼モード：設定内容の説明/);
    const output = run(`formatResult({status:'WAITING_FOR_APPROVAL', secondAction:'MODIFY_CONFIG', firstPermissionAllowed:true, newValue:'INFO', fileName:'MODIFIED', detectedSecrets:['API_KEY']})`);
    assert.match(output, /変更内容の確認と承認/);
    assert.match(output, /次の操作：設定ファイルの変更/);
    assert.match(output, /最初の操作の許可：許可されています/);
    assert.match(output, /変更後の値：INFO/); // 設定値を翻訳しない
    assert.match(output, /ファイル名：MODIFIED/); // ファイル名をコードとして解釈しない
    assert.match(output, /APIキー/);
    assert.match(run(`formatResult({executionResult:'MODIFY_FAILED: DUPLICATE_KEY'})`), /同じ設定項目が複数/);
    assert.match(run(`formatResult({executionResult:'MODIFIED: logging.level.root=INFO | EVIDENCE: VERIFIED'})`), /logging.level.root=INFO/);
    assert.match(run(`formatResult({evidence:'UNVERIFIED'})`), /確認できませんでした/);
    const untrusted = '<img src=x onerror=alert(1)>';
    response({ status: 'WAITING_FOR_APPROVAL', analysisSummary: untrusted, proposedKey: 'logging.level.root', proposedValue: 'INFO' });
    el('file').files = [{ name: 'demo.properties' }]; el('request').value = '問題を確認してください';
    await run('runAgent()');
    assert.equal(el('approvalSection').classList.hidden, false);
    assert.ok(el('result').textContent.includes(untrusted)); // textContentで安全に表示
    el('key').value = 'logging.level.root'; el('newValue').value = 'INFO';
    response({ status: 'COMPLETED', evidence: 'VERIFIED', fileName: 'demo.properties', downloadFileName: 'demo.properties', modifiedFileContent: 'logging.level.root=INFO\n' });
    await run('approve()');
    assert.equal(submitted.at(-1).options.body.values.approvalStatus, 'APPROVED');
    assert.equal(submitted.at(-1).options.body.values.newValue, 'INFO');
    assert.match(el('executionResult').textContent, /指定した値への変更を確認/);
    assert.equal(el('downloadSection').classList.hidden, false);
    assert.equal(el('downloadLink').download, 'demo.properties');
    assert.equal(el('downloadLink').href, 'blob:safeagent-test');
    assert.ok(!el('executionResult').textContent.includes('logging.level.root=INFO'));
    response({ status: 'BLOCKED', reason: 'SECRET_DETECTED' }); await run('runAgent()');
    assert.equal(el('approvalSection').classList.hidden, true);
    assert.equal(el('executionSection').classList.hidden, true);
    assert.match(el('result').textContent, /機密情報を検出/);
    response({ message: 'Internal Server Error' }, 500); await run('runAgent()');
    assert.match(el('result').textContent, /サーバーで処理に失敗/);
    assert.ok(!el('result').textContent.includes('Internal Server Error'));
    response({}); await run('loadAudit()'); assert.match(el('auditResult').textContent, /作業履歴はまだ/);
    console.log('日本語表示・承認コード維持・設定値保持・安全な文字表示・エラー表示：すべて成功');
})().catch(error => { console.error(error); process.exitCode = 1; });
