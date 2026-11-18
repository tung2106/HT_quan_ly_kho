(() => {
  const key = 'vatlieu-theme';
  let theme = 'light';
  try {
    if (localStorage.getItem(key) === 'dark') theme = 'dark';
  } catch (_) {
    /* Vẫn đổi được giao diện khi trình duyệt không cho lưu lựa chọn. */
  }
  function apply(value) {
    theme = value === 'dark' ? 'dark' : 'light';
    document.documentElement.dataset.bsTheme = theme;
    document
      .querySelectorAll('[data-theme]')
      .forEach((button) =>
        button.setAttribute('aria-pressed', String(button.dataset.theme === theme))
      );
    const status = document.querySelector('[data-theme-status]');
    if (status)
      status.textContent =
        theme === 'dark' ? 'Đang dùng giao diện tối.' : 'Đang dùng giao diện sáng.';
  }
  // Áp dụng trước khi hiển thị trang để không nháy nền sáng khi đã chọn tối.
  apply(theme);
  document.addEventListener('DOMContentLoaded', () => {
    apply(theme);
    document.querySelectorAll('[data-theme]').forEach((button) =>
      button.addEventListener('click', () => {
        apply(button.dataset.theme);
        try {
          localStorage.setItem(key, theme);
        } catch (_) {
          /* Lựa chọn vẫn có hiệu lực trong trang hiện tại. */
        }
      })
    );
  });
  window.addEventListener('storage', (event) => {
    if (event.key === key || event.key === null) apply(event.newValue);
  });
})();
