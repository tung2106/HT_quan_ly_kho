(() => {
  const script=document.querySelector('script[data-home]');
  const home=script.dataset.home;
  const storageKey='vatlieu-pages:'+home;
  if(location.pathname.endsWith('/login')) {
    try { sessionStorage.removeItem(storageKey); } catch (_) { /* Vẫn dùng được khi trình duyệt chặn lưu trữ. */ }
    return;
  }
  // Chỉ lưu các trang khác nhau, không lưu lần gửi form, tải lại hoặc thông báo kết quả.
  const page=location.pathname+(new URLSearchParams(location.search).get('kind')==='WASTE'?'?kind=WASTE':'');
  let pages=[];
  function save() {
    try { sessionStorage.setItem(storageKey,JSON.stringify(pages)); } catch (_) { /* Quay về trang chủ khi không lưu được. */ }
  }
  function remember() {
    try {
      const stored=JSON.parse(sessionStorage.getItem(storageKey)||'[]');
      pages=Array.isArray(stored)?stored.filter(path => {
        if(typeof path!=='string' || !path.startsWith('/') || path.startsWith('//')) return false;
        const url=new URL(path,location.origin);
        return url.origin===location.origin && url.pathname.startsWith(home) && !url.pathname.endsWith('/login');
      }).slice(-30):[];
    } catch (_) { pages=[]; }
    const index=pages.lastIndexOf(page);
    if(index>=0) pages=pages.slice(0,index+1);
    else pages.push(page);
    save();
  }
  remember();
  window.addEventListener('pageshow',remember);
  if(location.pathname===home) return;
  const toolbar=document.createElement('div');toolbar.className='container pt-3';
  const button=document.createElement('button');button.type='button';button.className='btn btn-outline-primary';
  button.textContent='← Quay lại';
  button.addEventListener('click',() => {
    remember();
    pages.pop();
    const previous=pages.at(-1)||home;
    save();
    // Mở trang bằng GET, không quay lại POST hoặc gửi lại thao tác vừa lưu.
    location.assign(previous);
  });
  toolbar.appendChild(button);document.body.prepend(toolbar);
})();
