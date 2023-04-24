import debounce from 'common/debounce';
import { bind, MaybeVNodes } from 'common/snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import { h, VNode } from 'snabbdom';
import { multiBoard as xhrLoad } from './studyXhr';
import { opposite } from 'chessground/util';
import { StudyCtrl, ChapterPreview, ChapterPreviewPlayer, Position } from './interfaces';

export class MultiBoardCtrl {
  loading = false;
  page = 1;
  pager?: Paginator<ChapterPreview>;
  playing = false;

  constructor(readonly studyId: string, readonly redraw: () => void, readonly trans: Trans) {}

  addNode = (pos: Position, node: Tree.Node) => {
    const cp = this.pager && this.pager.currentPageResults.find(cp => cp.id == pos.chapterId);
    if (cp?.playing) {
      cp.fen = node.fen;
      cp.lastMove = node.uci;
      this.redraw();
    }
  };

  reload = (onInsert?: boolean) => {
    if (this.pager && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    xhrLoad(this.studyId, this.page, this.playing).then(p => {
      this.pager = p;
      if (p.nbPages < this.page) {
        if (!p.nbPages) this.page = 1;
        else this.setPage(p.nbPages);
      }
      this.loading = false;
      this.redraw();
    });
  };

  reloadEventually = debounce(this.reload, 1000);

  setPage = (page: number) => {
    if (this.page != page) {
      this.page = page;
      this.reload();
    }
  };
  nextPage = () => this.setPage(this.page + 1);
  prevPage = () => this.setPage(this.page - 1);
  lastPage = () => {
    if (this.pager) this.setPage(this.pager.nbPages);
  };

  setPlaying = (v: boolean) => {
    this.playing = v;
    this.reload();
  };
}

export function view(ctrl: MultiBoardCtrl, study: StudyCtrl): VNode | undefined {
  const chapterIds = study.chapters
    .list()
    .map(c => c.id)
    .join('');
  return h(
    'div.study__multiboard',
    {
      class: { loading: ctrl.loading, nopager: !ctrl.pager },
      hook: {
        insert(vnode: VNode) {
          ctrl.reload(true);
          vnode.data!.chapterIds = chapterIds;
        },
        postpatch(old: VNode, vnode: VNode) {
          if (old.data!.chapterIds !== chapterIds) ctrl.reloadEventually();
          vnode.data!.chapterIds = chapterIds;
        },
      },
    },
    ctrl.pager ? renderPager(ctrl.pager, study) : [spinner()]
  );
}

function renderPager(pager: Paginator<ChapterPreview>, study: StudyCtrl): MaybeVNodes {
  const ctrl = study.multiBoard;
  return [
    h('div.top', [renderPagerNav(pager, ctrl), renderPlayingToggle(ctrl)]),
    h('div.now-playing', pager.currentPageResults.map(makePreview(study))),
  ];
}

function renderPlayingToggle(ctrl: MultiBoardCtrl): VNode {
  return h('label.playing', [
    h('input', {
      attrs: { type: 'checkbox', checked: ctrl.playing },
      hook: bind('change', e => {
        ctrl.setPlaying((e.target as HTMLInputElement).checked);
      }),
    }),
    ctrl.trans.noarg('playing'),
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
    from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
    to = Math.min(pager.nbResults, page * pager.maxPerPage);
  return h('div.pager', [
    pagerButton(ctrl.trans.noarg('first'), '', () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton(ctrl.trans.noarg('previous'), '', ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton(ctrl.trans.noarg('next'), '', ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton(ctrl.trans.noarg('last'), '', ctrl.lastPage, page < pager.nbPages, ctrl),
    h('button.fbt', {
      attrs: {
        'data-icon': '',
        title: 'Search',
      },
      hook: bind('click', () => lichess.pubsub.emit('study.search.open')),
    }),
  ]);
}

function pagerButton(text: string, icon: string, click: () => void, enable: boolean, ctrl: MultiBoardCtrl): VNode {
  return h('button.fbt', {
    attrs: {
      'data-icon': icon,
      disabled: !enable,
      title: text,
    },
    hook: bind('mousedown', click, ctrl.redraw),
  });
}

const makePreview = (study: StudyCtrl) => (preview: ChapterPreview) =>
  h(
    `a.mini-game.mini-game-${preview.id}.mini-game--init.is2d`,
    {
      attrs: {
        'data-state': `${preview.fen},${preview.orientation},${preview.lastMove}`,
      },
      class: {
        active: !study.multiBoard.loading && study.vm.chapterId == preview.id && !study.relay?.tourShow.active,
      },
      hook: {
        insert(vnode) {
          const el = vnode.elm as HTMLElement;
          lichess.miniGame.init(el);
          vnode.data!.fen = preview.fen;
          el.addEventListener('mousedown', _ => study.setChapter(preview.id));
        },
        postpatch(old, vnode) {
          if (old.data!.fen !== preview.fen) {
            lichess.miniGame.update(vnode.elm as HTMLElement, {
              lm: preview.lastMove!,
              fen: preview.fen,
            });
          }
          vnode.data!.fen = preview.fen;
        },
      },
    },
    [boardPlayer(preview, opposite(preview.orientation)), h('span.cg-wrap'), boardPlayer(preview, preview.orientation)]
  );

const userName = (u: ChapterPreviewPlayer) => (u.title ? [h('span.utitle', u.title), ' ' + u.name] : [u.name]);

function renderPlayer(player: ChapterPreviewPlayer | undefined): VNode | undefined {
  return (
    player &&
    h('span.mini-game__player', [
      h('span.mini-game__user', [
        h('span.name', userName(player)),
        player.rating && h('span.rating', ' ' + player.rating),
      ]),
    ])
  );
}

const boardPlayer = (preview: ChapterPreview, color: Color) => {
  const player = preview.players && preview.players[color];
  const result = preview.outcome && preview.outcome.split('-')[color === 'white' ? 0 : 1];
  return h('span.mini-game__player', [
    h('span.mini-game__user', [renderPlayer(player)]),
    result && h('span.mini-game__result', result.replace('1/2', '½')),
  ]);
};
