const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { JSDOM } = require('jsdom');

function page(t, template, selected = false) {
  const dom = new JSDOM(readFileSync(`src/main/resources/templates/${template}`, 'utf8'), {
    url: 'http://localhost/outbound/new',
    runScripts: 'outside-only',
  });
  t.after(() => dom.window.close());
  const w = dom.window;
  const control = w.document.querySelector('.settle-all');
  if (control.type === 'checkbox') control.checked = selected;
  else control.value = String(selected);
  const amount = control.form.querySelector('[name="paidAmount"]');
  amount.value = '123456';
  w.eval(readFileSync('src/main/resources/static/js/settlement.js', 'utf8'));
  return { w, control, amount, button: control.form.querySelector('[data-settle-toggle]') };
}

test('Bấm Tất toán chỉ chọn thu đủ; bỏ chọn giữ tiền từng phần và dữ liệu gửi đúng', (t) => {
  const { w, control, amount, button } = page(t, 'outbound/form.html');
  control.form.addEventListener('submit', () => assert.fail('Chọn tất toán không được gửi phiếu'));
  button.click();
  assert.equal(button.getAttribute('aria-pressed'), 'true');
  assert.equal(amount.disabled, true);
  let data = new w.FormData(control.form);
  assert.equal(data.get('settleAll'), 'true');
  assert.equal(data.has('paidAmount'), false);
  button.click();
  data = new w.FormData(control.form);
  assert.equal(data.get('settleAll'), 'false');
  assert.equal(data.get('paidAmount'), '123456');
  assert.equal(button.getAttribute('aria-pressed'), 'false');
  assert.equal(amount.disabled, false);
});

test('Trang trả lỗi giữ lựa chọn tất toán và vẫn bỏ chọn được', (t) => {
  const { amount, button } = page(t, 'outbound/form.html', true);
  assert.equal(button.getAttribute('aria-pressed'), 'true');
  assert.equal(amount.disabled, true);
  button.click();
  assert.equal(amount.disabled, false);
  assert.equal(amount.value, '123456');
});

test('Ô tích tất toán của nhập hàng vẫn khóa và mở số tiền đúng', (t) => {
  const { w, control, amount } = page(t, 'purchases/form.html', true);
  assert.equal(amount.disabled, true);
  control.checked = false;
  control.dispatchEvent(new w.Event('change'));
  assert.equal(amount.disabled, false);
  assert.equal(amount.value, '123456');
});
