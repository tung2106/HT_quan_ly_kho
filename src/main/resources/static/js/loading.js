(() => {
  const script = document.querySelector('script[data-cat-src]');
  const revealDelay = 1500;
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
  document.body.appendChild(overlay);
  const cat = overlay.querySelector('img');
  const message = overlay.querySelector('.sky-loading-message');
  const dismiss = overlay.querySelector('button');
  let timer;
  let reveal;
  let submitting = false;
  const hide = () => {
    overlay.hidden = true;
    clearTimeout(timer);
    clearTimeout(reveal);
  };
  const reset = () => {
    hide();
    submitting = false;
  };
  const show = (text) => {
    message.textContent = text;
    dismiss.hidden = true;
    clearTimeout(reveal);
    reveal = setTimeout(() => {
      if (!cat.hasAttribute('src')) cat.src = script.dataset.catSrc;
      overlay.hidden = false;
    }, revealDelay);
    clearTimeout(timer);
    timer = setTimeout(() => {
      message.textContent =
        'Trang phản hồi hơi lâu. Bạn có thể ẩn màn hình chờ để xem lại, không cần gửi lại phiếu.';
      dismiss.hidden = false;
    }, 12000);
  };
  dismiss.addEventListener('click', hide);
  window.addEventListener('pagehide', hide);
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) reset();
  });
  document.addEventListener('click', (event) => {
    const link = event.target.closest('a[href]');
    if (
      !link ||
      event.defaultPrevented ||
      event.button !== 0 ||
      event.ctrlKey ||
      event.metaKey ||
      event.shiftKey ||
      event.altKey ||
      link.hasAttribute('download') ||
      (link.target && link.target !== '_self')
    )
      return;
    const url = new URL(link.href, location.href);
    if (
      url.origin !== location.origin ||
      (url.pathname === location.pathname && url.search === location.search && url.hash)
    )
      return;
    show('Đang mở trang, bạn chờ một chút nhé.');
  });
  document.addEventListener('submit', (event) => {
    if (event.defaultPrevented || (event.target.target && event.target.target !== '_self')) return;
    if (submitting) {
      event.preventDefault();
      return;
    }
    // Không đổi giá trị hoặc vô hiệu hóa trường form khi trình duyệt gửi dữ liệu.
    submitting = true;
    show('Đang gửi dữ liệu, bạn chờ một chút nhé.');
  });
  document.querySelectorAll('.navbar a[data-nav]').forEach((link) => {
    const path = new URL(link.href).pathname;
    const stockPath = /\/(stock|products|suppliers|categories)(\/|$)/.test(location.pathname);
    const active =
      link.dataset.nav === 'stock'
        ? stockPath
        : location.pathname === path ||
          (link.dataset.nav !== 'home' && location.pathname.startsWith(path + '/'));
    if (active) link.setAttribute('aria-current', 'page');
  });
})();
