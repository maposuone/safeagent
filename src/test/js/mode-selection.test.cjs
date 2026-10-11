const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const modes = ['EXPLAIN', 'ANALYZE', 'FIX'].map(value => ({
    value, checked: value === 'EXPLAIN', handlers: {},
    addEventListener(name, handler) { this.handlers[name] = handler; }
}));
const cards = modes.map(radio => ({
    selected: radio.checked, querySelector: () => radio,
    classList: { toggle(name, value) { cards[modes.indexOf(radio)].selected = value; } }
}));
const elements = new Map();
function element(id) {
    if (!elements.has(id)) elements.set(id, {
        textContent: '', disabled: false,
        addEventListener() {}, removeAttribute() {}, scrollIntoView() {}, classList: { add() {}, remove() {}, toggle() {} }
    });
    return elements.get(id);
}
const environments = [{ disabled: false }, { disabled: false }];
const document = {
    getElementById: element,
    querySelector: () => modes.find(r => r.checked),
    querySelectorAll(selector) {
        if (selector === '.mode-card') return cards;
        if (selector.includes('requestMode')) return modes;
        if (selector.includes('environment')) return environments;
        return [];
    }
};
const requests = [];
class FormData {
    constructor() { this.values = {}; }
    append(key, value) { this.values[key] = value; }
}
const context = vm.createContext({ document, window: {}, console, Date, FormData,
    fetch: async (url, options) => {
        requests.push(options.body.values);
        return { ok: true, json: async () => ({ status: 'COMPLETED' }) };
    }
});
vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/app.js'), 'utf8'), context);
for (let i = 0; i < modes.length; i++) {
    element('file').value = 'previous.conf';
    element('designFile').value = 'previous.xlsx';
    element('result').textContent = 'previous result';
    vm.runInContext("currentRunId = 'old-run'; currentFileContent = 'old content'; currentProposedChanges = [{}]", context);
    modes.forEach((radio, index) => radio.checked = index === i);
    assert.equal(typeof modes[i].handlers.change, 'function');
    modes[i].handlers.change();
    assert.equal(element('file').value, '');
    assert.equal(element('designFile').value, '');
    assert.match(element('result').textContent, /設定ファイルを選び/);
    assert.equal(vm.runInContext('currentRunId', context), null);
    assert.equal(vm.runInContext('currentProposedChanges.length', context), 0);
    assert.equal(vm.runInContext('currentFileContent', context), '');
    assert.deepEqual(cards.map(card => card.selected), modes.map(radio => radio.checked));
    assert.equal(element('designFile').disabled, i === 0);
    assert.ok(environments.every(radio => radio.disabled === (i === 0)));
    assert.ok(element('modeRequirement').textContent.length > 0);
}
console.log('EXPLAIN / ANALYZE / FIX selection and required inputs: PASS');
(async () => {
    modes.forEach(radio => radio.checked = radio.value === 'EXPLAIN');
    modes[0].handlers.change();
    element('file').files = [{ name: 'httpd.conf', text: async () => 'Timeout 60' }];
    element('designFile').files = [{ name: 'old-design.xlsx' }];
    await vm.runInContext('runAgent()', context);
    await vm.runInContext('runAgent()', context);
    assert.equal(requests.length, 2);
    assert.ok(requests.every(request => !('designFile' in request) && !('environment' in request)));
    assert.equal(vm.runInContext('busy', context), false);
    assert.equal(element('designComparisonMessage').textContent,
        'EXPLAINでは定数設計書との比較は行いません。');
    vm.runInContext("renderDesignDifferences([], 'MATCH', 'ANALYZE')", context);
    assert.equal(element('designComparisonMessage').textContent,
        '定数設計書との比較結果：差異はありません。');
    vm.runInContext("renderDesignDifferences([], undefined, 'ANALYZE')", context);
    assert.equal(element('designComparisonMessage').textContent,
        '定数設計書との比較結果は確認できていません。');
    element('file').files[0].text = async () => { throw new Error('file read failed'); };
    await vm.runInContext('runAgent()', context);
    assert.equal(vm.runInContext('busy', context), false);
    assert.equal(element('validationMessage').textContent, 'file read failed');
    assert.match(element('result').textContent, /上部のメッセージ/);
    console.log('Repeated execution, EXPLAIN inputs, file read error recovery: PASS');
})().catch(error => { console.error(error); process.exitCode = 1; });
