const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { JSDOM } = require('jsdom');

const source = readFileSync('src/main/resources/static/js/loading.js', 'utf8');

async function page(t, readyState = 'complete') {
  const dom = new JSDOM(
    '<script data-cat-src="/cat.png"></script><a href="/stock">Tồn kho</a><form></form>',
    { url: 'http://localhost/', runScripts: 'outside-only' }
  );
  const w = dom.window;
  t.after(() => w.close());
  await new Promise((resolve) => w.addEventListener('pageshow', resolve, { once: true }));
  t.mock.timers.enable({ apis: ['setTimeout'] });
  w.setTimeout = setTimeout;
  w.clearTimeout = clearTimeout;
  Object.defineProperty(w.document, 'readyState', { value: readyState });
  w.eval(source);
  w.document.addEventListener('click', (event) => event.preventDefault());
  return {
    w,
    link: w.document.querySelector('a'),
    form: w.document.querySelector('form'),
    overlay: w.document.querySelector('.sky-loading'),
  };
}

test('Trang mới hiển thị trực tiếp khi tài nguyên còn đang tải', async (t) => {
  const { overlay } = await page(t, 'interactive');
  t.mock.timers.tick(2000);
  assert.equal(overlay.hidden, true);
  assert.equal(overlay.querySelector('img').hasAttribute('src'), false);
});

test('Chuyển trang nhanh không hiện mèo, chờ lâu mới mở màn chờ', async (t) => {
  const { w, link, overlay } = await page(t);
  link.click();
  t.mock.timers.tick(900);
  assert.equal(overlay.hidden, true);
  w.dispatchEvent(new w.Event('pagehide'));
  t.mock.timers.tick(2000);
  assert.equal(overlay.hidden, true);

  w.dispatchEvent(new w.PageTransitionEvent('pageshow', { persisted: true }));
  link.click();
  t.mock.timers.tick(1499);
  assert.equal(overlay.hidden, true);
  t.mock.timers.tick(1);
  assert.equal(overlay.hidden, false);
  assert.match(overlay.querySelector('img').src, /cat\.png$/);
  w.dispatchEvent(new w.PageTransitionEvent('pageshow', { persisted: true }));
  assert.equal(overlay.hidden, true);
});

test('Chặn gửi biểu mẫu lặp ngay cả khi màn chờ vẫn ẩn', async (t) => {
  const { w, form, overlay } = await page(t);
  const submit = () =>
    form.dispatchEvent(new w.Event('submit', { bubbles: true, cancelable: true }));
  assert.equal(submit(), true);
  t.mock.timers.tick(500);
  assert.equal(overlay.hidden, true);
  w.dispatchEvent(new w.PageTransitionEvent('pageshow'));
  assert.equal(submit(), false);
  w.dispatchEvent(new w.PageTransitionEvent('pageshow', { persisted: true }));
  assert.equal(submit(), true);
});
