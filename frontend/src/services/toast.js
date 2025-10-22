let container;
function ensureContainer() {
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.style.position = 'fixed';
    container.style.top = '20px';
    container.style.right = '20px';
    container.style.zIndex = '9999';
    document.body.appendChild(container);
  }
}
export function toast(message, type = 'info', duration = 3000) {
  ensureContainer();
  const el = document.createElement('div');
  el.textContent = message;
  el.style.marginTop = '8px';
  el.style.padding = '10px 14px';
  el.style.borderRadius = '6px';
  el.style.color = '#fff';
  el.style.fontSize = '14px';
  el.style.boxShadow = '0 2px 10px rgba(0,0,0,.15)';
  el.style.background = type === 'error' ? '#e53935' : type === 'success' ? '#43a047' : '#2196f3';
  container.appendChild(el);
  setTimeout(() => {
    el.style.transition = 'opacity .3s';
    el.style.opacity = '0';
    setTimeout(() => container.removeChild(el), 300);
  }, duration);
}

