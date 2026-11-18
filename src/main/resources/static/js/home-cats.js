(() => {
  const gallery = document.querySelector('.home-cat-gallery');
  if (!gallery) return;

  const frame = gallery.querySelector('.home-cat-frame');
  const photos = [frame.firstElementChild, ...gallery.querySelector('template').content.children];
  let index = 0;
  let start = null;
  let skipClick = false;

  function step(by) {
    index = (index + by + photos.length) % photos.length;
    gallery.dataset.step = String(by);
    frame.replaceChildren(photos[index]);
  }

  for (const button of gallery.querySelectorAll('button')) {
    button.hidden = false;
    button.addEventListener('click', (event) => {
      if (!skipClick || event.detail === 0) step(Number(button.dataset.step));
    });
  }

  gallery.addEventListener('keydown', (event) => {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    step(event.key === 'ArrowLeft' ? -1 : 1);
  });

  gallery.addEventListener('pointerdown', (event) => {
    if (!event.isPrimary || event.button !== 0) return;
    skipClick = false;
    start = { id: event.pointerId, x: event.clientX, y: event.clientY };
    (event.target.closest('button') || gallery).setPointerCapture(event.pointerId);
  });

  gallery.addEventListener('pointerup', (event) => {
    if (start?.id !== event.pointerId) return;
    const dx = event.clientX - start.x;
    const dy = event.clientY - start.y;
    start = null;
    // Một lần vuốt chỉ đổi một ảnh, kể cả khi trình duyệt phát thêm click.
    skipClick = Math.max(Math.abs(dx), Math.abs(dy)) >= 24;
    if (skipClick && Math.abs(dx) > Math.abs(dy)) step(dx < 0 ? 1 : -1);
  });

  gallery.addEventListener('lostpointercapture', (event) => {
    if (start?.id === event.pointerId) start = null;
  });
})();
