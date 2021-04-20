import { scroller } from './scroller';
export { isMoreThanText, enhanceAll as enhance } from 'common/richText';
/* Enhance with iframe expansion */

interface Expandable {
  element: HTMLElement;
  link: Link;
}
interface Link {
  type: LinkType;
  src: string;
}
type LinkType = 'game';

const domain = window.location.host;
const gameRegex = new RegExp(`(?:https?://)${domain}/(?:embed/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?$`);
const notGames = ['training', 'analysis', 'insights', 'practice', 'features', 'password', 'streamer', 'timeline'];

export function expandIFrames(el: HTMLElement) {
  const expandables: Expandable[] = [];

  el.querySelectorAll('a:not(.text)').forEach((a: HTMLAnchorElement) => {
    const link = parseLink(a);
    if (link)
      expandables.push({
        element: a,
        link: link,
      });
  });

  expandGames(expandables.filter(e => e.link.type == 'game'));
}

function expandGames(games: Expandable[]): void {
  if (games.length < 3) games.forEach(expand);
  else
    games.forEach(game => {
      game.element.title = 'Click to expand';
      game.element.classList.add('text');
      game.element.setAttribute('data-icon', '=');
      game.element.addEventListener('click', e => {
        if (e.button === 0) {
          e.preventDefault();
          expand(game);
        }
      });
    });
}

function expand(exp: Expandable): void {
  const $iframe: any = $('<iframe>').attr('src', exp.link.src);
  $(exp.element).parent().parent().addClass('has-embed');
  $(exp.element).replaceWith($('<div class="embed">').prepend($iframe));
  return $iframe
    .on('load', function (this: HTMLIFrameElement) {
      if (this.contentDocument?.title.startsWith('404')) (this.parentNode as HTMLElement).classList.add('not-found');
      scroller.auto();
    })
    .on('mouseenter', function (this: HTMLIFrameElement) {
      this.focus();
    });
}

function parseLink(a: HTMLAnchorElement): Link | undefined {
  const [id, pov, ply] = Array.from(a.href.match(gameRegex) || []).slice(1);
  if (id && !notGames.includes(id))
    return {
      type: 'game',
      src: configureSrc(`/embed/${id}${pov ? `/${pov}` : ''}${ply || ''}`),
    };
  return undefined;
}

const themes = [
  'blue',
  'blue2',
  'blue3',
  'blue-marble',
  'canvas',
  'wood',
  'wood2',
  'wood3',
  'wood4',
  'maple',
  'maple2',
  'brown',
  'leather',
  'green',
  'marble',
  'green-plastic',
  'grey',
  'metal',
  'olive',
  'newspaper',
  'purple',
  'purple-diag',
  'pink',
  'ic',
  'horsey',
];

function configureSrc(url: string): string {
  if (url.includes('://')) return url;
  const parsed = new URL(url, window.location.href);
  parsed.searchParams.append('theme', themes.find(theme => document.body.classList.contains(theme))!);
  parsed.searchParams.append('bg', document.body.getAttribute('data-theme')!);
  return parsed.href;
}
