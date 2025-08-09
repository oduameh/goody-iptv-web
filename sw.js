const CACHE = 'goody-iptv-v1';
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './goody-iptv-web-starter/icon-192.png',
  './goody-iptv-web-starter/icon-512.png',
  'https://cdn.jsdelivr.net/npm/hls.js@1.5.8/dist/hls.min.js'
];
self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k!==CACHE).map(k => caches.delete(k)))));
});
self.addEventListener('fetch', e => {
  e.respondWith(caches.match(e.request).then(r => r || fetch(e.request)));
}); 