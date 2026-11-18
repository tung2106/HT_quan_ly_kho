(() => {
  const rows = document.getElementById('rows');
  if (!rows) return;
  function show(row, selector, visible) {
    const field = row.querySelector(selector);
    field.hidden = !visible;
    field.querySelectorAll('input,textarea,select').forEach((input) => (input.disabled = !visible));
  }
  function update(row) {
    const group = row.querySelector('.purchase-group');
    const mode = row.querySelector('.entry-mode');
    const agriculture = group.value === 'AGRICULTURE';
    const unit = row.querySelector('[name="baseUnit"]');
    if (agriculture) unit.value = 'kg';
    Array.from(unit.options).forEach((option) => {
      option.disabled = agriculture && option.value !== 'kg';
    });
    show(row, '.agriculture-fields', agriculture);
    if (!agriculture) mode.value = 'WEIGHT';
    if (!mode.value) mode.value = 'WEIGHT';
    show(row, '.bags-field', agriculture && mode.value === 'BAGS');
    show(row, '.lot-field', agriculture && mode.value === 'LOT');
    show(row, '.qty-field', !agriculture || mode.value !== 'BAGS');
    show(row, '.price-field', !agriculture || mode.value !== 'LOT');
    row.querySelector('.qty-label').textContent = agriculture ? 'Tổng kg thực tế' : 'Số lượng';
    row.querySelector('.price-label').textContent = agriculture
      ? 'Giá mua / kg'
      : 'Đơn giá / đơn vị';
  }
  rows.querySelectorAll('.row-item').forEach(update);
  function handleChange(event) {
    if (event.isComposing) return;
    const row = event.target.closest('.row-item');
    if (!row) return;
    if (event.target.name === 'productName') {
      const key = (value) => value.trim().normalize('NFC').toLocaleLowerCase('vi');
      const matches = [...row.querySelectorAll('datalist option')].filter(
        (option) => key(option.value) === key(event.target.value)
      );
      const sameGroup = matches.filter(
        (option) => option.dataset.group === row.querySelector('.purchase-group').value
      );
      const selected =
        matches.length === 1 ? matches[0] : sameGroup.length === 1 ? sameGroup[0] : null;
      if (selected) {
        row.querySelector('.purchase-group').value = selected.dataset.group;
        row.querySelector('[name="baseUnit"]').value = selected.dataset.unit;
      }
    }
    update(row);
  }
  rows.addEventListener('change', handleChange);
  rows.addEventListener('input', (event) => {
    if (event.target.name === 'productName') handleChange(event);
  });
})();
