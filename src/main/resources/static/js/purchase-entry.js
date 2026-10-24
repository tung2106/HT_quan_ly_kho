(() => {
  const rows = document.getElementById('rows');
  if (!rows) return;
  function show(row, selector, visible) {
    const field = row.querySelector(selector);
    field.hidden = !visible;
    field.querySelectorAll('input,textarea,select').forEach(input => input.disabled = !visible);
  }
  function update(row, initialize = false) {
    const product = row.querySelector('select[name$=".productId"]');
    const group = row.querySelector('.purchase-group');
    const mode = row.querySelector('.entry-mode');
    if (initialize) group.value = product.selectedOptions[0]?.dataset.group || 'MATERIAL';
    const agriculture = group.value === 'AGRICULTURE';
    Array.from(product.options).forEach(option => {
      if (!option.value) return;
      option.hidden = option.dataset.group !== group.value;
      option.disabled = option.hidden;
    });
    if (product.selectedOptions[0]?.disabled) {
      product.value = '';
      product.dispatchEvent(new Event('change', { bubbles: true }));
    }
    show(row, '.agriculture-fields', agriculture);
    if (!agriculture) mode.value = 'WEIGHT';
    if (!mode.value) mode.value = 'WEIGHT';
    show(row, '.bags-field', agriculture && mode.value === 'BAGS');
    show(row, '.lot-field', agriculture && mode.value === 'LOT');
    show(row, '.qty-field', !agriculture || mode.value !== 'BAGS');
    show(row, '.price-field', !agriculture || mode.value !== 'LOT');
    row.querySelector('.qty-label').textContent = agriculture ? 'Tổng kg thực tế' : 'Số lượng';
    row.querySelector('.price-label').textContent = agriculture ? 'Giá mua / kg' : 'Đơn giá / đơn vị';
  }
  rows.querySelectorAll('.row-item').forEach(row => update(row, true));
  rows.addEventListener('change', event => {
    if (event.target.matches('.purchase-group,.entry-mode,select[name$=".productId"]'))
      update(event.target.closest('.row-item'));
  });
  new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(node => {
    if (node.nodeType === 1 && node.matches('.row-item')) update(node, true);
  }))).observe(rows, { childList: true });
})();
