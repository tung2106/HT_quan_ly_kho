(() => {
  const moneyName = /(?:^|\.)(unitPrice|lotAmount|retailPrice|shippingCost|paidAmount|amount)$/;
  function canonical(value) { return value.replace(/\./g, ''); }
  function format(value) {
    return value.replace(/\./g, '').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  }
  function valid(value) {
    return /^\d+$/.test(canonical(value));
  }
  function fromServer(value) {
    if (!value || !/^\d+(\.\d+)?$/.test(value)) return value;
    const [whole, fraction = ''] = value.split('.');
    return (BigInt(whole) + (fraction[0] >= '5' ? 1n : 0n)).toString();
  }
  function cents(value) {
    const [whole, fraction = ''] = value.split('.');
    return BigInt(whole || '0') * 100n + BigInt((fraction + '00').slice(0, 2));
  }
  if (typeof module !== 'undefined') module.exports = { canonical, format, valid, cents, fromServer };
  if (typeof document === 'undefined') return;
  const initialized = new WeakSet();
  function validate(input) {
    input.setCustomValidity('');
    if (!input.value) return;
    if (!valid(input.value)) {
      input.setCustomValidity('Nhập số tiền nguyên đồng, ví dụ 1.500.000; không nhập dấu phẩy.');
      return;
    }
    const value = cents(canonical(input.value));
    const min = input.getAttribute('min');
    const max = input.getAttribute('max');
    if (min && value < cents(min)) input.setCustomValidity('Số tiền nhỏ hơn mức cho phép.');
    if (max && value > cents(max)) input.setCustomValidity('Số tiền vượt quá số tiền còn nợ.');
  }
  function setup(input) {
    if (!moneyName.test(input.name) || initialized.has(input)) return;
    initialized.add(input);
    // Giá trị từ máy chủ dùng dấu chấm thập phân; dòng sao chép đã dùng định dạng Việt Nam.
    const initial = input.dataset.moneyFormatted ? input.value : fromServer(input.value);
    if (!input.dataset.moneyFormatted) input.dataset.moneyOriginal = input.value;
    else if (input.value !== input.dataset.moneyDisplay) delete input.dataset.moneyOriginal;
    input.type = 'text';
    input.inputMode = 'numeric';
    input.dataset.moneyFormatted = 'true';
    input.value = format(initial);
    input.dataset.moneyDisplay = input.value;
    if (!input.parentElement.classList.contains('money-input-wrap')) {
      const wrap = document.createElement('div');
      wrap.className = 'input-group money-input-wrap mb-2';
      input.before(wrap);
      wrap.append(input);
      input.classList.remove('mb-2');
      const suffix = document.createElement('span');
      suffix.className = 'input-group-text';
      suffix.textContent = 'VNĐ';
      wrap.append(suffix);
    }
    input.addEventListener('input', () => {
      delete input.dataset.moneyOriginal;
      const before = input.value;
      const pos = input.selectionStart ?? before.length;
      const count = before.slice(0, pos).replace(/\./g, '').length;
      // Không bỏ ký tự sai (ví dụ dấu âm), tránh vô tình biến số tiền sai thành số hợp lệ.
      if (/^[\d.]*$/.test(before)) {
        input.value = format(before);
        let cursor = 0, seen = 0;
        while (cursor < input.value.length && seen < count) {
          if (input.value[cursor] !== '.') seen++;
          cursor++;
        }
        input.setSelectionRange(cursor, cursor);
      }
      validate(input);
    });
    input.addEventListener('change', () => validate(input));
    validate(input);
  }
  function scan(root) {
    if (root.matches?.('input')) setup(root);
    root.querySelectorAll?.('input').forEach(setup);
  }
  scan(document);
  new MutationObserver(records => records.forEach(r => r.addedNodes.forEach(scan)))
    .observe(document.body, { childList: true, subtree: true });
  document.querySelectorAll('form').forEach(form => {
    // Chỉ chuẩn hóa dữ liệu gửi đi; giữ dấu phân cách trên màn hình, kể cả khi quay lại.
    form.addEventListener('formdata', event => {
      form.querySelectorAll('input[data-money-formatted]').forEach(input => {
        if (!input.disabled && input.name) {
          // Sửa tên sản phẩm không được vô tình làm tròn giá cũ nếu người dùng chưa sửa ô tiền.
          const unchanged = input.dataset.moneyOriginal !== undefined && input.value === input.dataset.moneyDisplay;
          event.formData.set(input.name, unchanged ? input.dataset.moneyOriginal : canonical(input.value));
        }
      });
    });
    form.addEventListener('reset', () => setTimeout(() => {
      form.querySelectorAll('input[data-money-formatted]').forEach(input => {
        input.value = format(fromServer(input.defaultValue));
        input.dataset.moneyOriginal = input.defaultValue;
        input.dataset.moneyDisplay = input.value;
        validate(input);
      });
    }, 0));
  });
})();
