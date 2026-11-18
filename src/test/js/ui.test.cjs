const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { JSDOM } = require('jsdom');

function page(t, html, scripts = ['ui.js']) {
  const dom = new JSDOM(html, {
    url: 'http://localhost/stock',
    runScripts: 'outside-only',
    pretendToBeVisual: true,
  });
  t.after(() => dom.window.close());
  const { window } = dom;
  window.HTMLDialogElement.prototype.showModal = function () {
    this.open = true;
  };
  window.HTMLDialogElement.prototype.close = function () {
    this.open = false;
    this.dispatchEvent(new window.Event('close'));
  };
  for (const name of scripts)
    window.eval(readFileSync(`src/main/resources/static/js/${name}`, 'utf8'));
  return window;
}

function submit(window, form, submitter) {
  const event = new window.SubmitEvent('submit', { bubbles: true, cancelable: true, submitter });
  form.dispatchEvent(event);
  return event;
}

test('Cài đặt đóng bằng nút hoặc Escape trả focus, không gửi form đang nhập', (t) => {
  const w = page(
    t,
    '<button data-dialog-open="settings">Cài đặt</button><dialog id="settings"><button data-dialog-close>Đóng</button></dialog><form><input value="đang nhập"></form>'
  );
  const trigger = w.document.querySelector('[data-dialog-open]');
  const dialog = w.document.getElementById('settings');
  trigger.click();
  assert.equal(dialog.open, true);
  assert.equal(trigger.getAttribute('aria-expanded'), 'true');
  dialog.querySelector('button').click();
  assert.equal(dialog.open, false);
  assert.equal(w.document.activeElement, trigger);
  trigger.click();
  // Escape của dialog native đóng qua cùng sự kiện close.
  dialog.close();
  assert.equal(trigger.getAttribute('aria-expanded'), 'false');
  assert.equal(w.document.querySelector('input').value, 'đang nhập');
  assert.equal(w.document.body.classList.contains('ui-dialog-open'), false);
  dialog.getBoundingClientRect = () => ({ left: 100, right: 400, top: 20, bottom: 500 });
  const point = (type, x, y) =>
    dialog.dispatchEvent(new w.MouseEvent(type, { bubbles: true, clientX: x, clientY: y }));
  trigger.click();
  point('pointerdown', 110, 30);
  point('click', 110, 30);
  assert.equal(dialog.open, true, 'Bấm trong khung không đóng');
  point('pointerdown', 110, 30);
  point('click', 10, 600);
  assert.equal(dialog.open, true, 'Kéo từ trong ra ngoài không đóng nhầm');
  point('pointerdown', 10, 600);
  point('click', 10, 600);
  assert.equal(dialog.open, false, 'Chạm nền bên ngoài đóng khung');
  assert.equal(w.document.activeElement, trigger);
  assert.equal(w.document.querySelector('input').value, 'đang nhập');
});

test('Lỗi bắt buộc được đọc qua nhãn, hiện tại ô và focus ô đầu tiên', (t) => {
  const w = page(
    t,
    '<form><label>Tên sản phẩm</label><input name="name" required><button>Lưu</button></form>'
  );
  const field = w.document.querySelector('input');
  assert.equal(submit(w, field.form).defaultPrevented, true);
  assert.equal(w.document.activeElement, field);
  assert.equal(field.labels[0].textContent, 'Tên sản phẩm');
  assert.equal(field.getAttribute('aria-invalid'), 'true');
  assert.equal(w.document.getElementById(field.getAttribute('aria-describedby')).hidden, false);
  field.value = 'Sắn tươi';
  assert.equal(submit(w, field.form).defaultPrevented, false);
});

test('Hủy xác nhận không gửi phiếu; chấp nhận chỉ gửi lại đúng nút một lần', (t) => {
  const w = page(
    t,
    '<form data-confirm="Ngừng sử dụng, giữ lịch sử" data-confirm-title="Xóa Sắn tươi?" data-confirm-action="Ngừng kinh doanh"><button id="delete">Xóa</button></form>'
  );
  const form = w.document.querySelector('form');
  const trigger = w.document.getElementById('delete');
  let sent = 0;
  form.requestSubmit = (button) => {
    assert.equal(button, trigger);
    if (!submit(w, form, button).defaultPrevented) sent++;
  };
  assert.equal(submit(w, form, trigger).defaultPrevented, true);
  const dialog = w.document.querySelector('dialog');
  assert.equal(dialog.open, true);
  assert.equal(dialog.querySelector('h2').textContent, 'Xóa Sắn tươi?');
  assert.equal(w.document.activeElement.textContent, 'Hủy');
  dialog.querySelector('button').click();
  assert.equal(dialog.open, false);
  assert.equal(sent, 0);
  assert.equal(w.document.activeElement, trigger);
  submit(w, form, trigger);
  dialog.querySelector('button:last-child').click();
  assert.equal(sent, 1);
  assert.equal(dialog.open, false);
});

test('Đóng dialog rồi mở lại không tự ghi tiền', (t) => {
  const w = page(t, '<form data-confirm="Ghi đã trả đủ"><button>Tất toán</button></form>');
  const form = w.document.querySelector('form');
  const trigger = form.querySelector('button');
  form.requestSubmit = () => assert.fail('Không được gửi khi đóng hộp xác nhận');
  submit(w, form, trigger);
  w.document.querySelector('dialog').close();
  assert.equal(submit(w, form, trigger).defaultPrevented, true);
  assert.equal(w.document.querySelector('dialog').open, true);
});

test('Dòng nhân bản có nhãn riêng, không mang lỗi và ID của dòng trước', async (t) => {
  const w = page(
    t,
    '<form><div class="row-item"><label>Số lượng</label><input name="items[0].qty" type="number" required><label>Đơn giá</label><input name="items[0].unitPrice" value="1000" min="0"></div></form>',
    ['ui.js', 'money-input.js']
  );
  const doc = w.document;
  submit(w, doc.querySelector('form'));
  const clone = doc.querySelector('.row-item').cloneNode(true);
  clone.querySelectorAll('input').forEach((field) => {
    field.name = field.name.replace('[0]', '[1]');
  });
  doc.querySelector('form').append(clone);
  await new Promise((resolve) => w.setTimeout(resolve, 0));
  const ids = [...doc.querySelectorAll('[id]')].map((node) => node.id);
  assert.equal(new Set(ids).size, ids.length);
  for (const field of clone.querySelectorAll('input')) {
    assert.equal(field.labels.length, 1);
    assert.equal(clone.contains(field.labels[0]), true);
    assert.equal(field.classList.contains('is-invalid'), false);
  }
  assert.equal(clone.querySelector('[name$="unitPrice"]').value, '1.000');
  assert.equal(clone.querySelectorAll('.money-input-wrap').length, 1);
});

test('Tiền vượt nợ hoặc có dấu phẩy bị chặn; số lượng 6 vẫn là 6', (t) => {
  const w = page(
    t,
    '<form><label>Tiền trả</label><input name="paidAmount" value="1000" max="5000" min="0"><label>Số lượng</label><input name="qty" type="number" value="6"></form>',
    ['ui.js', 'money-input.js']
  );
  const money = w.document.querySelector('[name="paidAmount"]');
  for (const value of ['6000', '1,5']) {
    money.value = value;
    money.dispatchEvent(new w.Event('input', { bubbles: true }));
    assert.equal(submit(w, money.form).defaultPrevented, true);
  }
  money.value = '5000';
  money.dispatchEvent(new w.Event('input', { bubbles: true }));
  assert.equal(money.value, '5.000');
  assert.equal(submit(w, money.form).defaultPrevented, false);
  assert.equal(w.document.querySelector('[name="qty"]').value, '6');
  assert.equal(money.parentElement.textContent, 'VNĐ');
});

test('Xóa tìm kiếm phát input để bỏ lọc và giữ focus', (t) => {
  const w = page(t, '<label>Tìm nhà cung cấp</label><input type="search" value="anh">');
  const input = w.document.querySelector('input');
  let changed = 0;
  input.addEventListener('input', () => changed++);
  const clear = w.document.querySelector('.ui-search button');
  clear.click();
  assert.equal(input.value, '');
  assert.equal(changed, 1);
  assert.equal(clear.hidden, true);
  assert.equal(w.document.activeElement, input);
});

test('Hiện mật khẩu chỉ đổi cách nhìn, giữ giá trị và focus', (t) => {
  const w = page(t, '<label>Mật khẩu</label><input type="password" value="test-password">');
  const input = w.document.querySelector('input');
  const toggle = w.document.querySelector('.ui-secret button');
  assert.equal(toggle.getAttribute('aria-controls'), input.id);
  assert.equal(toggle.getAttribute('aria-label'), 'Hiện mật khẩu');
  assert.equal(toggle.querySelector('use').getAttribute('href').split('#').pop(), 'eye');
  input.setSelectionRange(2, 5);
  toggle.click();
  assert.equal(input.type, 'text');
  assert.equal(toggle.getAttribute('aria-pressed'), 'true');
  assert.equal(toggle.getAttribute('aria-label'), 'Ẩn mật khẩu');
  assert.equal(toggle.querySelector('use').getAttribute('href').split('#').pop(), 'eye-off');
  assert.deepEqual([input.selectionStart, input.selectionEnd], [2, 5]);
  toggle.click();
  assert.equal(input.type, 'password');
  assert.equal(input.value, 'test-password');
  assert.equal(w.document.activeElement, input);
});

test('Tab công nợ tách riêng, tìm không dấu và nút xóa phục hồi danh sách', (t) => {
  const w = page(
    t,
    `
    <div role="tablist">
      <button id="supplier-debts-tab" role="tab" aria-controls="supplier-debts">Trả nợ</button>
      <button id="customer-debts-tab" role="tab" aria-controls="customer-debts">Thu khách</button>
    </div>
    <section id="supplier-debts" role="tabpanel">
      <label>Tìm nhà cung cấp</label><input class="debt-search" type="search">
      <div class="debt-card" data-partner="Chị Ánh">Phiếu 1</div>
      <p class="debt-search-empty" hidden>Không tìm thấy</p>
    </section>
    <section id="customer-debts" role="tabpanel">
      <label>Tìm khách hàng</label><input class="debt-search" type="search">
      <div class="debt-card" data-partner="Anh Đông">Phiếu 2</div>
      <p class="debt-search-empty" hidden>Không tìm thấy</p>
    </section>`,
    ['ui.js', 'debt-tabs.js']
  );
  const doc = w.document;
  const supplier = doc.getElementById('supplier-debts');
  const customer = doc.getElementById('customer-debts');
  assert.equal(supplier.hidden, false);
  assert.equal(customer.hidden, true);
  doc
    .getElementById('supplier-debts-tab')
    .dispatchEvent(new w.KeyboardEvent('keydown', { key: 'ArrowRight' }));
  assert.equal(supplier.hidden, true);
  assert.equal(customer.hidden, false);
  assert.equal(w.location.hash, '#customer-debts');
  const input = customer.querySelector('input');
  input.value = 'dong';
  input.dispatchEvent(new w.Event('input', { bubbles: true }));
  assert.equal(customer.querySelector('.debt-card').classList.contains('d-none'), false);
  input.value = 'không tồn tại';
  input.dispatchEvent(new w.Event('input', { bubbles: true }));
  assert.equal(customer.querySelector('.debt-search-empty').hidden, false);
  customer.querySelector('.ui-search button').click();
  assert.equal(customer.querySelector('.debt-card').classList.contains('d-none'), false);
  assert.equal(customer.querySelector('.debt-search-empty').hidden, true);
});
