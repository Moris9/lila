self.addEventListener('push', event => {
  const data = event.data!.json();
  return self.registration.showNotification(data.title, {
    badge: 'https://lichess1.org/assets/logo/lichess-favicon-256.png',
    icon: 'https://lichess1.org/assets/logo/lichess-favicon-256.png',
    body: data.body,
    tag: data.tag,
    data: data.payload,
    requireInteraction: true,
  });
});

async function handleNotificationClick(event: NotificationEvent) {
  const notifications = await self.registration.getNotifications();
  notifications.forEach(notification => notification.close());

  const windowClients = await self.clients.matchAll({
    type: 'window',
    includeUncontrolled: true,
  }) as ReadonlyArray<WindowClient>;

  // determine url
  const data = event.notification.data.userData;
  let url = '/';
  if (data.fullId) url = '/' + data.fullId;
  else if (data.threadId) url = '/inbox/' + data.threadId + '#bottom';
  else if (data.challengeId) url = '/' + data.challengeId;

  // focus open window with same url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, self.location.href);
    if (clientUrl.pathname === url && 'focus' in client) return await client.focus();
  }

  // navigate from open homepage to url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, self.location.href);
    if (clientUrl.pathname === '/') return await client.navigate(url);
  }

  // open new window
  return await self.clients.openWindow(url);
}

self.addEventListener('notificationclick', e => e.waitUntil(handleNotificationClick(e)));
