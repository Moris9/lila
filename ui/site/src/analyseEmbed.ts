import exportLichessGlobals from './site.lichess.globals';
import LichessReplay from 'replay';

exportLichessGlobals();

export default function embed(opts: any) {
  window.LichessAnalyse.start({
    ...opts,
    socketSend: () => {},
  });

  window.addEventListener('resize', () => document.body.dispatchEvent(new Event('chessground.resize')));

  if (opts.study?.chapter.gamebook)
    $('.main-board').append(
      $(
        `<a href="/study/${opts.study.id}/${opts.study.chapter.id}" target="_blank" rel="noopener" class="button gamebook-embed">Start</a>`
      )
    );
}

export function replayEmbed(opts: any) {
  LichessReplay(document.querySelector('.replay')!, opts);
}
