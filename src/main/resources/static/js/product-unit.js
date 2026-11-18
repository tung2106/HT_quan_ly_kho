(() => {
  const group = document.getElementById('productGroup');
  const baseUnit = document.getElementById('baseUnit');
  function syncProductGroup() {
    const agriculture = group.value === 'AGRICULTURE';
    if (agriculture) baseUnit.value = 'kg';
    for (const option of baseUnit.options) option.disabled = agriculture && option.value !== 'kg';
    document.getElementById('agriculture-unit-note').hidden = !agriculture;
  }
  group.addEventListener('change', syncProductGroup);
  syncProductGroup();
})();
