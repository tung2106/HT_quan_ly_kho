(() => {
  document.querySelectorAll('.settle-all').forEach((control) => {
    const amount = control.form.querySelector('input[name="paidAmount"]');
    const button = control.form.querySelector('[data-settle-toggle]');
    const status = control.form.querySelector('[data-settle-status]');
    function update() {
      const selected = control.type === 'checkbox' ? control.checked : control.value === 'true';
      amount.disabled = selected;
      amount.closest('.money-input-wrap')?.classList.toggle('opacity-50', selected);
      button?.setAttribute('aria-pressed', String(selected));
      if (status)
        status.textContent = selected
          ? 'Khi lưu phiếu sẽ ghi thu đủ. Bấm lại để bỏ chọn.'
          : 'Chọn Tất toán nếu khách đã trả đủ.';
    }
    button?.addEventListener('click', () => {
      control.value = String(control.value !== 'true');
      update();
    });
    control.addEventListener('change', update);
    update();
  });
})();
