import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { bind } from '../util';
import { renderClock } from '../clock/clockView';
import { renderInner as tableInner } from '../view/table';
import { render as renderGround } from '../ground';
import renderCorresClock from '../corresClock/corresClockView';
import * as renderUser from '../view/user';
import { renderResult } from '../view/replay';
import { plyStep } from '../round';
import { Step, DecodedDests, Position, Redraw } from '../interfaces';
import { Player } from 'game';
import { files } from 'chessground/types';
import { invRanks } from 'chessground/util';

type Sans = {
  [key: string]: Uci;
}

type Notification = {
  text: string;
  date: Date;
}

window.lichess.RoundNVUI = function() {

  let notification: Notification | undefined;

  const settings = {
    moveNotation: {
      choices: [
        ['san', 'SAN: Nxf3'],
        ['literate', 'literate: knight takes f 3'],
        ['anna', 'Anna: knight takes felix 3'],
        ['full', 'Full: gustav 1 knight takes on felix 3']
      ],
      default: 'san',
      value: window.lichess.storage.make('nvui.moveNotation')
    }
  };

  return {
    render(ctrl: RoundController) {
      const d = ctrl.data,
        step = plyStep(d, ctrl.ply);
      return ctrl.chessground ? h('div.nvui', [
        h('h1', 'Textual representation'),
        h('div', [
          ...(ctrl.isPlaying() ? [
            h('h2', 'Your color: ' + d.player.color),
            h('h2', ['Opponent: ', renderPlayer(ctrl, d.opponent)])
          ] : ['white', 'black'].map((color: Color) => h('h2', [
            color + ' player: ',
            renderPlayer(ctrl, ctrl.playerByColor(color))
          ]))
          ),
          h('h2', 'Moves'),
          h('p.pgn', {
            attrs: {
              role : 'log',
              'aria-live': 'off'
            }
          }, movesHtml(d.steps.slice(1), settings)),
          h('h2', 'Pieces'),
          h('div.pieces', piecesHtml(ctrl)),
          // h('h2', 'FEN'),
          // h('p.fen', step.fen),
          h('h2', 'Game status'),
          h('div.status', {
            attrs: {
              role : 'status',
              'aria-live' : 'assertive',
              'aria-atomic' : true
            }
          }, [ctrl.data.game.status.name === 'started' ? 'Playing' : renderResult(ctrl)]),
          h('h2', 'Last move'),
          h('p.lastMove', {
            attrs: {
              'aria-live' : 'assertive',
              'aria-atomic' : true
            }
          }, readSan(step, settings)),
          ...(ctrl.isPlaying() ? [
            h('h2', 'Move form'),
            h('form', {
              hook: {
                insert(vnode) {
                  const el = vnode.elm as HTMLFormElement;
                  const d = ctrl.data;
                  const $form = $(el).submit(function() {
                    const input = $form.find('.move').val();
                    const legalUcis = destsToUcis(ctrl.chessground.state.movable.dests!);
                    const sans: Sans = sanWriter(plyStep(d, ctrl.ply).fen, legalUcis) as Sans;
                    const uci = sanToUci(input, sans) || input;
                    if (legalUcis.indexOf(uci.toLowerCase()) >= 0) ctrl.socket.send("move", {
                      from: uci.substr(0, 2),
                      to: uci.substr(2, 2),
                      promotion: uci.substr(4, 1)
                    }, { ackable: true });
                    else {
                      notification = {
                        text: d.player.color === d.game.player ? `Invalid move: ${input}` : 'Not your turn',
                        date: new Date()
                      };
                      ctrl.redraw();
                    }
                    $form.find('.move').val('');
                    return false;
                  });
                  $form.find('.move').val('').focus();
                }
              }
            }, [
              h('label', [
                d.player.color === d.game.player ? 'Your move' : 'Waiting',
                h('input.move', {
                  attrs: {
                    name: 'move',
                    'type': 'text',
                    autocomplete: 'off',
                    autofocus: true
                  }
                })
              ])
            ])
          ]: []),
          h('h2', 'Your clock'),
          h('div.botc', anyClock(ctrl, 'bottom')),
          h('h2', 'Opponent clock'),
          h('div.topc', anyClock(ctrl, 'top')),
          h('h2', 'Actions'),
          h('div.actions', tableInner(ctrl)),
          h('h2', 'Board table'),
          h('pre.board', tableBoard(ctrl)),
          h('h2', 'Settings'),
          h('label', [
            'Move notation',
            renderSetting(settings.moveNotation, ctrl.redraw)
          ]),
          h('div.notify', {
            attrs: {
              'aria-live': 'assertive',
              'aria-atomic' : true
            }
          }, (notification && notification.date.getTime() > (Date.now() - 1000 * 3)) ? notification.text : '')
        ])
      ]) : renderGround(ctrl);
    }
  };
}

function renderSetting(setting: any, redraw: Redraw) {
  const v = setting.value.get() || setting.default;
  return h('select', {
    hook: bind('change', e => {
      setting.value.set((e.target as HTMLSelectElement).value);
      console.log(setting.value.get());
      redraw();
    })
  }, setting.choices.map((choice: [string, string]) => {
    const [key, name] = choice;
    return h('option', {
      attrs: {
        value: key,
        selected: key === v
      }
    }, name)
  }));
}

function renderPlayer(ctrl: RoundController, player: Player) {
  return player.ai ? renderUser.aiName(ctrl, player.ai) : renderUser.userHtml(ctrl, player);
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data, player = ctrl.playerAt(position);
  return (ctrl.clock && renderClock(ctrl, player, position)) || (
    d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)
  ) || undefined;
}

function destsToUcis(dests: DecodedDests) {
  const ucis: string[] = [];
  Object.keys(dests).forEach(function(orig) {
    dests[orig].forEach(function(dest) {
      ucis.push(orig + dest);
    });
  });
  return ucis;
}

function sanToUci(san: string, sans: Sans): Uci | undefined {
  if (san in sans) return sans[san];
  const lowered = san.toLowerCase();
  for (let i in sans)
    if (i.toLowerCase() === lowered) return sans[i];
  return;
}

function movesHtml(steps: Step[], settings: any) {
  const res: Array<string | VNode> = [];
  steps.forEach(s => {
    res.push(readSan(s, settings) + ', ');
    if (s.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

function piecesHtml(ctrl: RoundController): VNode {
  const pieces = ctrl.chessground.state.pieces;
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
      const keys = [];
      for (let key in pieces) {
        if (pieces[key]!.color === color && pieces[key]!.role === role) keys.push(key);
      }
      if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
    });
    const tags: VNode[] = [];
    lists.forEach((l: any) => {
      tags.push(h('h4', l[0]));
      tags.push(h('p', l.slice(1).map(annaKey).join(', ')));
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...tags
    ]);
  }));
}

const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

function tableBoard(ctrl: RoundController): string {
  const pieces = ctrl.chessground.state.pieces;
  const board = [[' ', ...files, ' ']];
  for(let rank of invRanks) {
    let line = [];
    for(let file of files) {
      let key = file + rank;
      const piece = pieces[key];
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'white' ? letter.toUpperCase() : letter);
      } else line.push((file.charCodeAt(0) + rank) % 2 ? '-' : '+');
    }
    board.push(['' + rank, ...line, '' + rank]);
  }
  board.push([' ', ...files, ' ']);
  if (ctrl.data.player.color === 'black') {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return board.map(line => line.join(' ')).join('\n');
}

const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };

function readSan(s: Step, settings: any) {
  if (!s.san) return '';
  const style = settings.moveNotation.value.get();
  const has = window.lichess.fp.contains;
  let move: string;
  if (has(s.san, 'O-O')) move = 'short castling';
  else if (has(s.san, 'O-O-O')) move = 'long castling';
  else if (style === 'san') move = s.san.replace(/[\+#]/, '');
  else {
    if (style === 'literate' || style == 'anna') move = s.san.replace(/[\+#]/, '').split('').map(c => {
      const code = c.charCodeAt(0);
      if (code > 48 && code < 58) return c;
      if (c == 'x') return 'takes';
      if (c == '+') return 'check';
      if (c == '#') return 'checkmate';
      if (c == '=') return 'promotion';
      if (c.toUpperCase() === c) return roles[c];
      if (style === 'anna' && anna[c]) return anna[c];
      return c;
    }).join(' ');
    else {
      const role = roles[s.san[0]] || 'pawn';
      const orig = annaKey(s.uci.slice(0, 2));
      const dest = annaKey(s.uci.slice(2, 4));
      const goes = has(s.san, 'x') ? 'takes on' : 'moves to';
      move = `${orig} ${role} ${goes} ${dest}`
      const prom = s.uci[4];
      if (prom) move += ' promotes to ' + roles[prom.toUpperCase()];
    }
  }
  if (has(s.san, '+')) move += ' check';
  if (has(s.san, '#')) move += ' checkmate';
  return move;
}

const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
function annaKey(key: string): string {
  return `${anna[key[0]]} ${key[1]}`;
}
