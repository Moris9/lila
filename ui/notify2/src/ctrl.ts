import { Ctrl, NotifyOpts, NotifyData, Redraw } from './interfaces'

import { asText } from './view'

export default function ctrl(opts: NotifyOpts, redraw: Redraw): Ctrl {

  let data: NotifyData | undefined
  let initiating = true;
  let scrolling = false;

  const readAllStorage = window.lichess.storage.make('notify-read-all');

  readAllStorage.listen(() => {
    if (data) {
      data.unread = 0;
      opts.setCount(0);
      redraw();
    }
  });

  function update(d: NotifyData, incoming: boolean) {
    data = d;
    if (data.pager.currentPage === 1 && data.unread && opts.isVisible()) {
      opts.setNotified();
      data.unread = 0;
      readAllStorage.set(1); // tell other tabs
    }
    initiating = false;
    scrolling = false;
    opts.setCount(data.unread);
    if (incoming) notifyNew();
    redraw();
  }

  function notifyNew() {
    if (!data || data.pager.currentPage !== 1) return;
    var notif = data.pager.currentPageResults.find(n => !n.read);
    if (!notif) return;
    opts.pulse();
    if (!window.lichess.quietMode) window.lichess.sound.newPM();
    var text = asText(notif);
    if (text) window.lichess.desktopNotification(text);
  }

  function loadPage(page: number) {
    return $.get('/notify', {page: page || 1}).then(d => update(d, false));
  }

  function nextPage() {
    if (!data || !data.pager.nextPage) return;
    scrolling = true;
    loadPage(data.pager.nextPage);
    redraw();
  }

  function previousPage() {
    if (!data || !data.pager.previousPage) return;
    scrolling = true;
    loadPage(data.pager.previousPage);
    redraw();
  }

  function setVisible() {
    if (!data || data.pager.currentPage === 1) loadPage(1);
  }

  return {
    data: () => data,
    initiating: () => initiating,
    scrolling: () => scrolling,
    update,
    nextPage,
    previousPage,
    loadPage,
    setVisible
  };
}
