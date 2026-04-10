self.addEventListener('install', (e) => {
    console.log('[Service Worker] Instalado');
});

self.addEventListener('fetch', (e) => {
    // Para já, apenas passa os pedidos normalmente sem cache complexa
    e.respondWith(fetch(e.request));
});