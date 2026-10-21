(() => {
  const rows = document.getElementById('rows');
  if (!rows) return;
  function update(row, changed = false) {
    const product = row.querySelector('select[name$=".productId"]');
    const units = row.querySelector('.unit-select');
    if (!product || !units) return;
    const previous = changed ? '' : units.value;
    const available = [];
    Array.from(units.options).forEach(option => {
      if (!option.value) return;
      const matches = option.dataset.productId === product.value;
      option.hidden = !matches;
      option.disabled = !matches;
      option.selected = false;
      if (matches) available.push(option);
    });
    units.disabled = !product.value;
    units.required = !!product.value;
    units.selectedIndex = 0;
    const selected = available.find(option => option.value === previous) || available[0];
    if (selected) selected.selected = true;
  }
  rows.querySelectorAll('.row-item').forEach(row => update(row));
  rows.addEventListener('change', event => {
    if (event.target.matches('select[name$=".productId"]')) update(event.target.closest('.row-item'), true);
  });
  // Dòng sao chép phải lọc lại đơn vị, không giữ đơn vị của sản phẩm ở dòng trước.
  new MutationObserver(() => rows.querySelectorAll('.row-item').forEach(row => update(row)))
    .observe(rows, { childList: true });
  rows.addEventListener('click', event => {
    if (event.target.closest('button')) queueMicrotask(() => rows.querySelectorAll('.row-item').forEach(row => update(row)));
  });
})();
