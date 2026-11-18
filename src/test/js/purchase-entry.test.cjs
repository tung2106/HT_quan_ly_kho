const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const script = fs.readFileSync(
  path.join(__dirname, '../../main/resources/static/js/purchase-entry.js'),
  'utf8'
);

function page() {
  const dom = new JSDOM(
    `<form><div id="rows"><div class="row-item">
    <select class="purchase-group"><option value="MATERIAL">Vật liệu</option><option value="AGRICULTURE">Nông sản</option></select>
    <input name="productName"><datalist><option value="Ngô" data-group="AGRICULTURE" data-unit="kg"></option><option value="Gạch đỏ" data-group="MATERIAL" data-unit="viên"></option></datalist>
    <select name="baseUnit"><option value="kg">kg</option><option value="viên">viên</option></select>
    <div class="agriculture-fields"><select class="entry-mode"><option value="WEIGHT">Kg</option><option value="BAGS">Bao</option><option value="LOT">Lô</option></select>
      <div class="bags-field"><textarea name="bags"></textarea></div><div class="lot-field"><input name="lotAmount"></div></div>
    <div class="qty-field"><label class="qty-label"></label><input name="qty"></div>
    <div class="price-field"><label class="price-label"></label><input name="price"></div>
  </div></div></form>`,
    { runScripts: 'outside-only' }
  );
  dom.window.eval(script);
  const change = (selector, value) => {
    const field = dom.window.document.querySelector(selector);
    field.value = value;
    field.dispatchEvent(
      new dom.window.Event(field.name === 'productName' ? 'input' : 'change', { bubbles: true })
    );
  };
  return { dom, doc: dom.window.document, change };
}

test('Gõ tên hàng đã có tự chọn đúng nhóm và đơn vị, không sửa tên người dùng', () => {
  const { dom, doc, change } = page();
  change('[name="productName"]', '  NGÔ  ');
  assert.equal(doc.querySelector('.purchase-group').value, 'AGRICULTURE');
  assert.equal(doc.querySelector('[name="baseUnit"]').value, 'kg');
  assert.equal(doc.querySelector('[name="productName"]').value, '  NGÔ  ');
  assert.equal(doc.querySelector('.agriculture-fields').hidden, false);
  change('[name="productName"]', 'Gạch đỏ');
  assert.equal(doc.querySelector('.purchase-group').value, 'MATERIAL');
  assert.equal(doc.querySelector('[name="baseUnit"]').value, 'viên');
  assert.equal(doc.querySelector('.agriculture-fields').hidden, true);
  dom.window.close();
});

test('Cân từng bao và mua cả lô chỉ gửi các ô đúng cách nhập', () => {
  const { dom, doc, change } = page();
  change('.purchase-group', 'AGRICULTURE');
  change('.entry-mode', 'BAGS');
  assert.equal(doc.querySelector('[name="qty"]').disabled, true);
  assert.equal(doc.querySelector('[name="bags"]').disabled, false);
  assert.equal(doc.querySelector('[name="price"]').disabled, false);
  assert.equal(doc.querySelector('[name="lotAmount"]').disabled, true);
  change('.entry-mode', 'LOT');
  assert.equal(doc.querySelector('[name="qty"]').disabled, false);
  assert.equal(doc.querySelector('[name="price"]').disabled, true);
  assert.equal(doc.querySelector('[name="lotAmount"]').disabled, false);
  change('.purchase-group', 'MATERIAL');
  assert.equal(doc.querySelector('.entry-mode').value, 'WEIGHT');
  assert.equal(doc.querySelector('[name="price"]').disabled, false);
  assert.equal(doc.querySelector('[name="lotAmount"]').disabled, true);
  dom.window.close();
});
