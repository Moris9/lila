import { initWith as initMiniBoard } from 'common/mini-board';
import * as xhr from 'common/xhr';
import { requestIdleCallback } from './functions';

const inCrosstable = (el: HTMLElement) => document.querySelector('.crosstable')?.contains(el);

function onPowertipPreRender(id: string, preload?: (url: string) => void) {
  return (el: HTMLAnchorElement) => {
    const url = ($(el).data('href') || el.href).replace(/\?.+$/, '');
    if (preload) preload(url);
    xhr.text(url + '/mini').then(html => {
      const el = document.getElementById(id) as HTMLElement;
      el.innerHTML = html;
      lichess.contentLoaded(el);
    });
  };
}

const uptA = (url: string, icon: string) => `<a class="btn-rack__btn" href="${url}" data-icon="${icon}"></a>`;

const userPowertip = (el: HTMLElement, pos?: PowerTip.Placement) =>
  $(el)
    .removeClass('ulpt')
    .powerTip({
      preRender: onPowertipPreRender('powerTip', (url: string) => {
        const u = url.slice(3);
        const name = $(el).data('name') || $(el).html();
        $('#powerTip').html(
          '<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' +
            name +
            '</span></div></div><div class="upt__actions btn-rack">' +
            uptA('/@/' + u + '/tv', '') +
            uptA('/inbox/new?user=' + u, '') +
            uptA('/?user=' + u + '#friend', '') +
            '<a class="btn-rack__btn relation-button" disabled></a></div>'
        );
      }),
      placement: pos || (el.getAttribute('data-pt-pos') as PowerTip.Placement) || (inCrosstable(el) ? 'n' : 's'),
    });

const gamePowertip = (el: HTMLElement) =>
  $(el)
    .removeClass('glpt')
    .powerTip({
      preRender: onPowertipPreRender('miniGame', () => lichess.spinnerHtml),
      placement: inCrosstable(el) ? 'n' : 'w',
      popupId: 'miniGame',
    });

const boardPowertip = (el: HTMLElement) =>
  $(el)
    .removeClass('blpt')
    .powerTip({
      popupId: 'miniBoard',
      preRender(el) {
        const tipEl = document.getElementById('miniBoard') as HTMLDivElement;
        tipEl.innerHTML = `<div class="mini-board mini-board--init cg-wrap standard is2d"/>`;
        initMiniBoard(tipEl.querySelector('.cg-wrap')!, el.dataset['fen']!, 'white');
      },
    });

function powerTipWith(el: HTMLElement, ev: Event, f: (el: HTMLElement) => void) {
  if (!('ontouchstart' in window)) {
    f(el);
    $.powerTip.show(el, ev);
  }
}

function onIdleForAll(par: HTMLElement, sel: string, f: (el: HTMLElement) => void) {
  requestIdleCallback(
    () => Array.prototype.forEach.call(par.querySelectorAll(sel), (el: HTMLElement) => f(el)), // do not codegolf to `f`
    800
  );
}

const powertip = {
  watchMouse() {
    document.body.addEventListener('mouseover', e => {
      const t = e.target as HTMLElement;
      if (t.classList.contains('ulpt')) powerTipWith(t, e, userPowertip);
      else if (t.classList.contains('glpt')) powerTipWith(t, e, gamePowertip);
    });
  },
  watchMouseIn(el: HTMLElement, cls: string, handler: (el: HTMLElement) => void) {
    el.addEventListener('mouseover', e => {
      const t = e.target as HTMLElement;
      if (t.classList.contains(cls)) powerTipWith(t, e, handler);
    });
  },
  manualGameIn(parent: HTMLElement) {
    onIdleForAll(parent, '.glpt', gamePowertip);
  },
  manualGame: gamePowertip,
  manualUser: userPowertip,
  manualUserIn(parent: HTMLElement) {
    onIdleForAll(parent, '.ulpt', userPowertip);
  },
};

export default powertip;
