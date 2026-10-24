(() => {
  function update(row) {
    const product = row.querySelector('select[name$=".productId"]');
    const batch = row.querySelector('.batch-select');
    const hint = row.querySelector('.agriculture-conversion');
    if (hint) hint.hidden = product?.selectedOptions[0]?.dataset.group !== 'AGRICULTURE';
    if (!product || !batch) return;
    for (const option of batch.options) {
      const matches = !option.value || option.dataset.productId === product.value;
      option.hidden = !matches;
      option.disabled = !matches;
    }
    if (batch.selectedOptions[0]?.disabled) batch.value = '';
    batch.disabled = !product.value;
  }
  const rows = document.getElementById('rows');
  if (!rows) return;
  rows.addEventListener('change', event => {
    if (event.target.matches('select[name$=".productId"]')) update(event.target.closest('.row-item'));
  });
  function scan() { rows.querySelectorAll('.row-item').forEach(update); }
  new MutationObserver(scan).observe(rows, { childList: true });
  scan();
})();
