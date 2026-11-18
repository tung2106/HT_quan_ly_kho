const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { JSDOM } = require('jsdom');

async function page(t, saved, blocked = false) {
  const dom = new JSDOM(readFileSync('src/main/resources/templates/fragments.html', 'utf8'), {
    url: 'http://localhost/',
    runScripts: 'outside-only',
  });
  t.after(() => dom.window.close());
  const w = dom.window;
  if (saved) w.localStorage.setItem('vatlieu-theme', saved);
  if (blocked)
    Object.defineProperty(w, 'localStorage', {
      get() {
        throw new Error('Storage blocked');
      },
    });
  w.eval(readFileSync('src/main/resources/static/js/theme.js', 'utf8'));
  // Giao diện được áp dụng ngay cả trước khi các nút được khởi tạo.
  assert.equal(
    w.document.documentElement.dataset.bsTheme,
    saved === 'dark' && !blocked ? 'dark' : 'light'
  );
  await new Promise((resolve) =>
    w.document.addEventListener('DOMContentLoaded', resolve, { once: true })
  );
  return w;
}

test('Nhớ giao diện tối, đổi sang sáng và lưu lựa chọn', async (t) => {
  const w = await page(t, 'dark');
  assert.equal(
    w.document.querySelector('[data-theme="dark"]').getAttribute('aria-pressed'),
    'true'
  );
  w.document.querySelector('[data-theme="light"]').click();
  assert.equal(w.document.documentElement.dataset.bsTheme, 'light');
  assert.equal(w.localStorage.getItem('vatlieu-theme'), 'light');
  assert.equal(
    w.document.querySelector('[data-theme="dark"]').getAttribute('aria-pressed'),
    'false'
  );
});

test('Trình duyệt chặn lưu trữ vẫn đổi được giao diện', async (t) => {
  const w = await page(t, null, true);
  w.document.querySelector('[data-theme="dark"]').click();
  assert.equal(w.document.documentElement.dataset.bsTheme, 'dark');
  assert.match(w.document.querySelector('[data-theme-status]').textContent, /tối/);
});

test('Lựa chọn giữa các tab được đồng bộ và giá trị lạ về sáng', async (t) => {
  const w = await page(t, 'unknown');
  w.dispatchEvent(new w.StorageEvent('storage', { key: 'vatlieu-theme', newValue: 'dark' }));
  assert.equal(w.document.documentElement.dataset.bsTheme, 'dark');
  w.dispatchEvent(new w.StorageEvent('storage', { key: 'vatlieu-theme', newValue: 'unknown' }));
  assert.equal(w.document.documentElement.dataset.bsTheme, 'light');
});
