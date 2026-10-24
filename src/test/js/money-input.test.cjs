const test = require('node:test');
const assert = require('node:assert/strict');
const money = require('../../main/resources/static/js/money-input.js');

test('Hiển thị tiền và gửi số nguyên không có dấu phân cách', () => {
  for (const [raw, display] of [['0','0'],['1000','1.000'],['1500000','1.500.000'],['9999999999999','9.999.999.999.999']]) {
    assert.equal(money.format(raw), display);
    assert.equal(money.canonical(display), raw);
    assert.equal(money.valid(display), true);
  }
  assert.equal(money.format('1.500.000'), '1.500.000');
});
test('Không đổi dấu âm hoặc tiền có dấu phẩy thành số tiền khác', () => {
  for (const bad of ['-1000','1.000,50','abc','1e6','']) assert.equal(money.valid(bad), false);
});
test('Tiền cũ hiển thị nguyên đồng và xử lý số lớn bằng chuỗi', () => {
  assert.equal(money.format(money.fromServer('1500000.00')), '1.500.000');
  assert.equal(money.fromServer('999.50'), '1000');
  assert.equal(money.fromServer('999.49'), '999');
  assert.equal(money.fromServer('9999999999999.50'), '10000000000000');
  assert.equal(money.cents('1000'), 100000n);
  assert.equal(money.cents('999.50'), 99950n);
});
