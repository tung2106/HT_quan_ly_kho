(() => {
  const initialized = new WeakSet();
  let sequence = 0;
  const names = { productId: 'Sản phẩm', name: 'Tên', phone: 'Số điện thoại', note: 'Ghi chú' };

  function button(text, label) {
    const node = document.createElement('button');
    node.type = 'button';
    node.className = 'btn btn-outline-secondary';
    node.textContent = text;
    node.setAttribute('aria-label', label);
    return node;
  }

  function setup(field) {
    if (field.closest('.g-recaptcha')) return;
    if (initialized.has(field) || ['hidden', 'submit', 'button'].includes(field.type)) return;
    initialized.add(field);
    // Dòng nhân bản cần ID riêng để nhãn và lỗi không trỏ về dòng đầu tiên.
    const oldId = field.id;
    const duplicate = oldId && document.getElementById(oldId) !== field;
    if (!oldId || duplicate) {
      do {
        field.id = `ui-field-${++sequence}`;
      } while (document.getElementById(field.id) !== field);
    }
    const wrap = field.closest('.money-input-wrap') || field;
    if (duplicate) {
      // Không sao chép trạng thái lỗi của dòng cũ sang dòng mới.
      if (wrap.nextElementSibling?.id === `${oldId}-error`) wrap.nextElementSibling.remove();
      field.classList.remove('is-invalid');
      field.removeAttribute('aria-invalid');
      const describedBy = (field.getAttribute('aria-describedby') || '')
        .split(/\s+/)
        .filter((id) => id && id !== `${oldId}-error`)
        .join(' ');
      if (describedBy) field.setAttribute('aria-describedby', describedBy);
      else field.removeAttribute('aria-describedby');
    }
    let label = wrap.previousElementSibling;
    if (label?.tagName === 'LABEL' && (!label.htmlFor || label.htmlFor === oldId))
      label.htmlFor = field.id;
    if (!field.labels?.length && !field.getAttribute('aria-label')) {
      label = document.createElement('label');
      label.className = 'form-label';
      label.htmlFor = field.id;
      label.textContent = names[field.name?.split('.').at(-1)] || field.placeholder || 'Thông tin';
      wrap.before(label);
    }
    if (field.type === 'search' || field.type === 'password') {
      const secret = field.type === 'password';
      const group = document.createElement('div');
      group.className = secret ? 'ui-secret mb-2' : 'ui-search mb-2';
      field.before(group);
      group.append(field);
      field.classList.remove('mb-2');
      const action = button(
        secret ? 'Hiện' : '×',
        secret ? 'Hiện mật khẩu' : 'Xóa nội dung tìm kiếm'
      );
      group.append(action);
      const update = () => {
        if (!secret) action.hidden = !field.value;
      };
      field.addEventListener('input', update);
      action.addEventListener('click', () => {
        if (secret) {
          const show = field.type === 'password';
          field.type = show ? 'text' : 'password';
          action.textContent = show ? 'Ẩn' : 'Hiện';
          action.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
          action.setAttribute('aria-pressed', String(show));
        } else {
          field.value = '';
          field.dispatchEvent(new Event('input', { bubbles: true }));
        }
        field.focus();
        update();
      });
      update();
    }
    if (field.tagName === 'TEXTAREA') {
      const resize = () => {
        field.style.height = 'auto';
        field.style.height = `${Math.max(100, field.scrollHeight)}px`;
      };
      field.addEventListener('input', resize);
      resize();
    }
  }

  function scan(root) {
    if (root.matches?.('input, select, textarea')) setup(root);
    root.querySelectorAll?.('input, select, textarea').forEach(setup);
  }
  scan(document);
  // Cửa sổ tiện ích dùng cùng dialog native: Escape đóng và trả focus về nút mở.
  document.querySelectorAll('[data-dialog-open]').forEach((trigger) => {
    const panel = document.getElementById(trigger.dataset.dialogOpen);
    if (!panel) return;
    trigger.addEventListener('click', () => {
      panel.showModal();
      document.body.classList.add('ui-dialog-open');
      trigger.setAttribute('aria-expanded', 'true');
    });
    panel
      .querySelectorAll('[data-dialog-close]')
      .forEach((close) => close.addEventListener('click', () => panel.close()));
    const outside = (event) => {
      const bounds = panel.getBoundingClientRect();
      return (
        event.target === panel &&
        (event.clientX < bounds.left ||
          event.clientX > bounds.right ||
          event.clientY < bounds.top ||
          event.clientY > bounds.bottom)
      );
    };
    let startedOutside = false;
    panel.addEventListener('pointerdown', (event) => {
      startedOutside = outside(event);
    });
    panel.addEventListener('click', (event) => {
      if (startedOutside && outside(event)) panel.close();
      startedOutside = false;
    });
    panel.addEventListener('close', () => {
      document.body.classList.remove('ui-dialog-open');
      trigger.setAttribute('aria-expanded', 'false');
      trigger.focus();
    });
  });
  new MutationObserver((records) =>
    records.forEach((record) => record.addedNodes.forEach(scan))
  ).observe(document.body, { subtree: true, childList: true });

  function showError(field) {
    let error = document.getElementById(`${field.id}-error`);
    if (!error) {
      error = document.createElement('p');
      error.id = `${field.id}-error`;
      error.className = 'ui-field-error';
      (field.closest('.money-input-wrap, .ui-secret, .ui-search') || field).after(error);
      field.setAttribute(
        'aria-describedby',
        [field.getAttribute('aria-describedby'), error.id].filter(Boolean).join(' ')
      );
    }
    const valid = !field.willValidate || field.validity.valid;
    error.hidden = valid;
    field.classList.toggle('is-invalid', !valid);
    field.setAttribute('aria-invalid', String(!valid));
    if (!valid) {
      error.textContent = field.validity.customError
        ? field.validationMessage
        : field.validity.valueMissing
          ? 'Vui lòng nhập hoặc chọn thông tin này.'
          : field.validity.rangeUnderflow
            ? `Giá trị phải từ ${field.min} trở lên.`
            : field.validity.rangeOverflow
              ? `Giá trị không được lớn hơn ${field.max}.`
              : 'Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.';
    }
    return valid;
  }

  // Một hộp xác nhận dùng chung, không gửi dữ liệu khi người dùng bấm Hủy hoặc Escape.
  const dialog = document.createElement('dialog');
  dialog.className = 'ui-dialog';
  dialog.setAttribute('aria-labelledby', 'ui-confirm-title');
  dialog.setAttribute('aria-describedby', 'ui-confirm-description');
  dialog.innerHTML =
    '<h2 id="ui-confirm-title"></h2><p id="ui-confirm-description"></p><div class="ui-dialog-actions"></div>';
  const cancel = button('Hủy', 'Hủy');
  const accept = button('Xác nhận', 'Xác nhận');
  accept.className = 'btn btn-danger';
  dialog.querySelector('.ui-dialog-actions').append(cancel, accept);
  document.body.append(dialog);
  let pending = null;
  const approved = new WeakSet();
  cancel.addEventListener('click', () => dialog.close());
  dialog.addEventListener('close', () => {
    document.body.classList.remove('ui-dialog-open');
    pending?.submitter?.focus();
    pending = null;
  });
  accept.addEventListener('click', () => {
    const current = pending;
    if (!current) return;
    dialog.close();
    approved.add(current.form);
    current.form.requestSubmit(current.submitter || undefined);
  });
  document.addEventListener(
    'submit',
    (event) => {
      const form = event.target;
      if (event.defaultPrevented) return;
      const fields = [...form.elements].filter(
        (field) => field.matches?.('input, select, textarea') && field.willValidate
      );
      const invalid = fields.filter((field) => !showError(field));
      if (invalid.length) {
        event.preventDefault();
        invalid[0].closest('details')?.setAttribute('open', '');
        invalid[0].focus();
        return;
      }
      if (approved.delete(form) || !form.dataset.confirm) return;
      event.preventDefault();
      pending = { form, submitter: event.submitter };
      const title = form.dataset.confirmTitle || 'Xác nhận thao tác';
      dialog.querySelector('h2').textContent = title;
      dialog.querySelector('p').textContent = form.dataset.confirm;
      accept.textContent = form.dataset.confirmAction || 'Xác nhận';
      accept.setAttribute('aria-label', accept.textContent);
      dialog.showModal();
      document.body.classList.add('ui-dialog-open');
      cancel.focus();
    },
    true
  );
  document.querySelectorAll('form').forEach((form) => {
    form.noValidate = true;
  });
  document.addEventListener('input', (event) => {
    // Chờ bộ định dạng tiền kiểm tra customValidity trước khi cập nhật lỗi.
    if (event.target.getAttribute?.('aria-invalid') === 'true')
      queueMicrotask(() => showError(event.target));
  });
})();
