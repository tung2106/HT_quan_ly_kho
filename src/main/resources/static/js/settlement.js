(() => {
  document.querySelectorAll('.settle-all').forEach(checkbox => {
    const amount = checkbox.form.querySelector('input[name="paidAmount"]');
    function update() {
      amount.disabled = checkbox.checked;
      amount.closest('.money-input-wrap')?.classList.toggle('opacity-50', checkbox.checked);
    }
    checkbox.addEventListener('change', update);
    update();
  });
})();
