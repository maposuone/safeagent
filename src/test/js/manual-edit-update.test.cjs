const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/manual-edit.js'), 'utf8');
const context = vm.createContext({});
vm.runInContext(source.replace(/loadPayload\(\);\s*$/, ''), context);
vm.runInContext(`payload = { fileContent: 'old', candidates: [
    { currentValue: '100', designValue: '200', type: 'VALUE_MISMATCH' },
    { currentValue: 'On', designValue: 'On', type: 'MATCH' }
] };`, context);
assert.equal(vm.runInContext(`applyVerifiedEdits({status:'COMPLETED', evidence:'UNVERIFIED', modifiedFileContent:'bad'}, [{index:0,newValue:'200'}])`, context), false);
assert.equal(vm.runInContext('payload.fileContent', context), 'old');
assert.equal(vm.runInContext(`applyVerifiedEdits({status:'COMPLETED', evidence:'VERIFIED', modifiedFileContent:'new'}, [{index:0,newValue:'200'},{index:1,newValue:'Off'}])`, context), true);
assert.equal(vm.runInContext('payload.fileContent', context), 'new');
assert.equal(vm.runInContext('payload.candidates[0].type', context), 'MATCH');
assert.equal(vm.runInContext('payload.candidates[1].type', context), 'VALUE_MISMATCH');
assert.equal(vm.runInContext('payload.candidates[1].currentValue', context), 'Off');
console.log('Verified manual edit updates values and comparison state; unverified results excluded: PASS');
