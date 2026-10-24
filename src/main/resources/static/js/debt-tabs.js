(() => {
  const tabs = [...document.querySelectorAll('[role="tab"][aria-controls$="-debts"]')];
  if (!tabs.length) return;
  // Chỉ tìm theo tên đối tác, không dùng nội dung số tiền hay ghi chú trong phiếu.
  function normalize(value) {
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase().trim().replace(/\s+/g, ' ');
  }
  document.querySelectorAll('.debt-search').forEach(input => {
    const panel = input.closest('[role="tabpanel"]');
    const cards = [...panel.querySelectorAll('.debt-card')];
    input.addEventListener('input', () => {
      const query = normalize(input.value);
      let count = 0;
      cards.forEach(card => {
        const matches = normalize(card.dataset.partner || '').includes(query);
        card.classList.toggle('d-none', !matches);
        if (matches) count++;
      });
      panel.querySelector('.debt-search-empty').hidden = !query || count > 0;
    });
  });
  const key = 'vatlieu-debt-tab';
  function activate(id, remember = true) {
    tabs.forEach(tab => {
      const active = tab.getAttribute('aria-controls') === id;
      const color = tab.id.startsWith('supplier') ? 'danger' : 'success';
      tab.setAttribute('aria-selected', String(active));
      tab.tabIndex = active ? 0 : -1;
      tab.classList.toggle('btn-' + color, active);
      tab.classList.toggle('btn-outline-' + color, !active);
      document.getElementById(tab.getAttribute('aria-controls')).hidden = !active;
    });
    if (remember) {
      try { sessionStorage.setItem(key, id); } catch (_) { /* Vẫn đổi tab khi không lưu được. */ }
      history.replaceState(null, '', location.pathname + location.search + '#' + id);
    }
  }
  let initial = location.hash.slice(1);
  if (!tabs.some(t => t.getAttribute('aria-controls') === initial)) {
    try { initial = sessionStorage.getItem(key); } catch (_) { /* Mặc định tab trả nợ. */ }
  }
  activate(tabs.some(t => t.getAttribute('aria-controls') === initial) ? initial : 'supplier-debts', false);
  tabs.forEach((tab, index) => {
    tab.addEventListener('click', () => activate(tab.getAttribute('aria-controls')));
    tab.addEventListener('keydown', event => {
      if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
      event.preventDefault();
      const next = event.key === 'Home' ? tabs[0] : event.key === 'End' ? tabs[tabs.length - 1] : tabs[(index + 1) % tabs.length];
      next.focus(); next.click();
    });
  });
})();
