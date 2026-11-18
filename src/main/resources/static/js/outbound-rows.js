(() => {
  const rows = document.getElementById('rows');
  function renumber() {
    rows.querySelectorAll('.row-item').forEach((row, index) =>
      row.querySelectorAll('[name]').forEach((field) => {
        field.name = field.name.replace(/\[\d+\]/, `[${index}]`);
      })
    );
  }
  document.getElementById('add-outbound-row').addEventListener('click', () => {
    if (rows.children.length >= 100) return;
    const row = rows.querySelector('.row-item').cloneNode(true);
    row.querySelectorAll('input,select').forEach((field) => {
      field.value = '';
    });
    row.querySelector('.batch-options')?.removeAttribute('open');
    rows.appendChild(row);
    renumber();
    row.querySelector('select').focus();
  });
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('.remove-outbound-row');
    if (!button) return;
    const row = button.closest('.row-item');
    const next = row.nextElementSibling || row.previousElementSibling;
    if (!next) return;
    row.remove();
    next.querySelector('select').focus();
    renumber();
  });
})();
