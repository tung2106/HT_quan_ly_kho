(() => {
  const script = document.querySelector('script[data-cat-src]');
  const overlay = document.createElement('div');
  overlay.className = 'sky-loading';
  overlay.hidden = true;
  overlay.innerHTML = `<div class="sky-loading-card">
    <img alt="Mèo may mắn ôm tiền vàng" width="152" height="220">
    <div role="status" aria-live="polite">
      <h2>Mèo đang trông kho…</h2>
      <p class="sky-loading-message">Đang tải trang, bạn chờ một chút nhé.</p>
    </div>
    <div class="sky-loading-dots" aria-hidden="true"><span></span><span></span><span></span></div>
    <button type="button" class="btn btn-outline-primary" hidden>Ẩn màn hình chờ</button>
  </div>`;
  overlay.querySelector('img').src = script.dataset.catSrc;
  document.body.appendChild(overlay);
  const message = overlay.querySelector('.sky-loading-message');
  const dismiss = overlay.querySelector('button');
  let timer;
  let submitting = false;
  const hide = () => { overlay.hidden = true; clearTimeout(timer); };
  const reset = () => { hide(); submitting = false; };
  const show = (text) => {
    message.textContent = text;
    dismiss.hidden = true;
    overlay.hidden = false;
    clearTimeout(timer);
    timer = setTimeout(() => {
      message.textContent = 'Trang phản hồi hơi lâu. Bạn có thể ẩn màn hình chờ để xem lại, không cần gửi lại phiếu.';
      dismiss.hidden = false;
    }, 12000);
  };
  dismiss.addEventListener('click', hide);
  window.addEventListener('pageshow', reset);
  window.addEventListener('load', hide, { once: true });
  document.addEventListener('click', (event) => {
    const link = event.target.closest('a[href]');
    if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || link.hasAttribute('download') || (link.target && link.target !== '_self')) return;
    const url = new URL(link.href, location.href);
    if (url.origin !== location.origin || (url.pathname === location.pathname && url.search === location.search && url.hash)) return;
    show('Đang mở trang, bạn chờ một chút nhé.');
  });
  document.addEventListener('submit', (event) => {
    if (event.defaultPrevented || (event.target.target && event.target.target !== '_self')) return;
    if (submitting) { event.preventDefault(); return; }
    // Không đổi giá trị hoặc vô hiệu hóa trường form khi trình duyệt gửi dữ liệu.
    submitting = true;
    show('Đang gửi dữ liệu, bạn chờ một chút nhé.');
  });
  document.querySelectorAll('.navbar a[href]').forEach(link => {
    const path = new URL(link.href).pathname;
    if (location.pathname === path || location.pathname.startsWith(path + '/')) link.setAttribute('aria-current', 'page');
  });
  if (document.readyState !== 'complete') show('Đang tải trang, bạn chờ một chút nhé.');
})();
