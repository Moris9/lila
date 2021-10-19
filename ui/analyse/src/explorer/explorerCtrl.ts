import { Prop, prop } from 'common';
import { storedProp } from 'common/storage';
import debounce from 'common/debounce';
import { sync, Sync } from 'common/sync';
import { opposite } from 'chessground/util';
import * as xhr from './explorerXhr';
import { winnerOf, colorOf } from './explorerUtil';
import * as gameUtil from 'game';
import AnalyseCtrl from '../ctrl';
import { Hovering, ExplorerData, ExplorerDb, OpeningData, SimpleTablebaseHit, ExplorerOpts } from './interfaces';
import { CancellableStream } from 'common/ndjson';
import { ExplorerConfigCtrl } from './explorerConfig';

function pieceCount(fen: Fen) {
  const parts = fen.split(/\s/);
  return parts[0].split(/[nbrqkp]/i).length - 1;
}

function tablebasePieces(variant: VariantKey) {
  switch (variant) {
    case 'standard':
    case 'fromPosition':
    case 'chess960':
      return 7;
    case 'atomic':
    case 'antichess':
      return 6;
    default:
      return 0;
  }
}

export const tablebaseGuaranteed = (variant: VariantKey, fen: Fen) => pieceCount(fen) <= tablebasePieces(variant);

export default class ExplorerCtrl {
  allowed: Prop<boolean>;
  enabled: Prop<boolean>;
  withGames: boolean;
  effectiveVariant: VariantKey;
  config: ExplorerConfigCtrl;

  loading = prop(true);
  failing = prop<Error | null>(null);
  hovering = prop<Hovering | null>(null);
  movesAway = prop(0);
  gameMenu = prop<string | null>(null);
  lastStream = prop<Sync<CancellableStream> | null>(null);
  cache: Dictionary<ExplorerData> = {};

  constructor(readonly root: AnalyseCtrl, readonly opts: ExplorerOpts, allow: boolean) {
    this.allowed = prop(allow);
    this.enabled = root.embed ? prop(false) : storedProp('explorer.enabled', false);
    this.withGames = root.synthetic || gameUtil.replayable(root.data) || !!root.data.opponent.ai;
    this.effectiveVariant = root.data.game.variant.key === 'fromPosition' ? 'standard' : root.data.game.variant.key;
    this.config = new ExplorerConfigCtrl(
      root.data.game,
      this.effectiveVariant,
      this.onConfigClose,
      root.trans,
      root.redraw
    );
    window.addEventListener('hashchange', this.checkHash, false);
    this.checkHash();
  }

  checkHash = (e?: HashChangeEvent) => {
    if ((location.hash === '#explorer' || location.hash === '#opening') && !this.root.embed) {
      this.enabled(true);
      if (e) this.root.redraw();
    }
  };

  onConfigClose = () => {
    this.cache = {};
    this.setNode();
    this.root.redraw();
  };

  fetch = debounce(
    () => {
      const fen = this.root.node.fen;
      const processData = (res: ExplorerData) => {
        this.cache[fen] = res;
        this.movesAway(res.moves.length ? 0 : this.movesAway() + 1);
        this.loading(false);
        this.failing(null);
        this.root.redraw();
      };
      const onError = (err: Error) => {
        this.loading(false);
        this.failing(err);
        this.root.redraw();
      };
      const prev = this.lastStream();
      if (prev) prev.promise.then(stream => stream.cancel());
      if (this.withGames && this.tablebaseRelevant(this.effectiveVariant, fen))
        xhr.tablebase(this.opts.tablebaseEndpoint, this.effectiveVariant, fen).then(processData, onError);
      else
        this.lastStream(
          sync(
            xhr
              .opening(
                {
                  endpoint: this.opts.endpoint,
                  endpoint3: this.opts.endpoint3,
                  db: this.db() as ExplorerDb,
                  personal: {
                    player: this.config.data.playerName.value(),
                    color: this.root.getOrientation(),
                    mode: this.config.data.mode.selected(),
                  },
                  variant: this.effectiveVariant,
                  rootFen: this.root.nodeList[0].fen,
                  play: this.root.nodeList.slice(1).map(s => s.uci!),
                  fen,
                  speeds: this.config.data.speed.selected(),
                  ratings: this.config.data.rating.selected(),
                  withGames: this.withGames,
                },
                processData
              )
              .then(stream => {
                stream.end.promise.then(this.root.redraw);
                return stream;
              })
          )
        );
    },
    250,
    true
  );

  empty: OpeningData = {
    isOpening: true,
    moves: [],
    fen: '',
    opening: this.root.data.game.opening,
  };

  tablebaseRelevant = (variant: VariantKey, fen: Fen) =>
    pieceCount(fen) - 1 <= tablebasePieces(variant) && this.root.ceval.possible;

  setNode = () => {
    if (!this.enabled()) return;
    this.gameMenu(null);
    const node = this.root.node;
    if (node.ply > 50 && !this.tablebaseRelevant(this.effectiveVariant, node.fen)) {
      this.cache[node.fen] = this.empty;
    }
    const cached = this.cache[node.fen];
    if (cached) {
      this.movesAway(cached.moves.length ? 0 : this.movesAway() + 1);
      this.loading(false);
      this.failing(null);
    } else {
      this.loading(true);
      this.fetch();
    }
  };

  db = () => this.config.db();
  current = () => this.cache[this.root.node.fen];
  toggle = () => {
    this.movesAway(0);
    this.enabled(!this.enabled());
    this.setNode();
    this.root.autoScroll();
  };
  disable = () => {
    if (this.enabled()) {
      this.enabled(false);
      this.gameMenu(null);
      this.root.autoScroll();
    }
  };
  setHovering = (fen: Fen, uci: Uci | null) => {
    this.hovering(uci ? { fen, uci } : null);
    this.root.setAutoShapes();
  };
  onFlip = () => {
    if (this.db() == 'player') {
      this.cache = {};
      this.setNode();
    }
  };
  isIndexing = () => {
    const stream = this.lastStream();
    return !!stream && (!stream.sync || !stream.sync.end.sync);
  };
  fetchMasterOpening = (() => {
    const masterCache: Dictionary<OpeningData> = {};
    return (fen: Fen): Promise<OpeningData> => {
      const val = masterCache[fen];
      if (val) return Promise.resolve(val);
      return new Promise(resolve =>
        xhr.opening(
          {
            endpoint: this.opts.endpoint,
            endpoint3: this.opts.endpoint3,
            db: 'masters',
            rootFen: fen,
            play: [],
            fen,
          },
          (res: OpeningData) => {
            masterCache[fen] = res;
            resolve(res);
          }
        )
      );
    };
  })();
  fetchTablebaseHit = async (fen: Fen): Promise<SimpleTablebaseHit> => {
    const res = await xhr.tablebase(this.opts.tablebaseEndpoint, this.effectiveVariant, fen);
    const move = res.moves[0];
    if (move && move.dtz == null) throw 'unknown tablebase position';
    return {
      fen,
      best: move && move.uci,
      winner: res.checkmate ? opposite(colorOf(fen)) : res.stalemate ? undefined : winnerOf(fen, move!),
    } as SimpleTablebaseHit;
  };
}
