import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Pieces } from 'chessground/types';
import { Rank, File } from 'chessground/types';
import { invRanks, allKeys } from 'chessground/util';
import { Setting, makeSetting } from './setting';
import { files } from 'chessground/types';

export type Style = 'uci' | 'san' | 'literate' | 'nato' | 'anna';
export type PieceStyle = 'letter' | 'white uppercase letter' | 'name' | 'white uppercase name';
export type PrefixStyle = 'letter' | 'name' | 'none';
export type PositionStyle = 'before' | 'after' | 'none';
export type BoardStyle = 'plain' | 'table'

const nato: { [letter: string]: string } = { a: 'alpha', b: 'bravo', c: 'charlie', d: 'delta', e: 'echo', f: 'foxtrot', g: 'golf', h: 'hotel' };
const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };
const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

const letterPiece: { [letter: string]: string} = { p: 'p', r: 'r', n: 'n', b: 'b', q: 'q', k: 'k',
                                                   P: 'p', R: 'r', N: 'n', B: 'b', Q: 'q', K: 'k'};
const whiteUpperLetterPiece: { [letter: string]: string} = { p: 'p', r: 'r', n: 'n', b: 'b', q: 'q', k: 'k',
                                                   P: 'P', R: 'R', N: 'N', B: 'B', Q: 'Q', K: 'K'};
const namePiece: { [letter: string]: string} = { p: 'pawn', r: 'rook', n: 'knight', b: 'bishop', q: 'queen', k: 'king',
                                                   P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king'};
const whiteUpperNamePiece: { [letter: string]: string} = { p: 'pawn', r: 'rook', n: 'knight', b: 'bishop', q: 'queen', k: 'king',
                                                   P: 'Pawn', R: 'Rook', N: 'Knight', B: 'Bishop', Q: 'Queen', K: 'King'};
const skipToFile: { [letter: string]: string} = {'!': 'a', '@': 'b', '#': 'c', '$': 'd', '%': 'e', '^': 'f', '&': 'g', '*': 'h'};

export function symbolToFile(char: string)  {
  return skipToFile[char] ?? "";
}

export function supportedVariant(key: string) {
  return [
    'standard', 'chess960', 'kingOfTheHill', 'threeCheck', 'fromPosition'
  ].includes(key);
}

export function boardSetting(): Setting<BoardStyle> {
  return makeSetting<BoardStyle>({
    choices: [
      ['plain', 'plain: layout with no semantic rows or columns'],
      ['table', 'table: layout using a table with rank and file columns and row headers']
    ],
    default: 'plain',
    storage: lichess.storage.make('nvui.boardLayout')
  });
}

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['san', 'SAN: Nxf3'],
      ['uci', 'UCI: g1f3'],
      ['literate', 'Literate: knight takes f 3'],
      ['anna', 'Anna: knight takes felix 3'],
      ['nato', 'Nato: knight takes foxtrot 3'],
    ],
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: lichess.storage.make('nvui.moveNotation')
  });
}

export function pieceSetting(): Setting<PieceStyle> {
  return makeSetting<PieceStyle>({
    choices: [
      ['letter', 'Letter: p, p'],
      ['white uppercase letter', 'White uppecase letter: P, p'],
      ['name', 'Name: pawn, pawn'],
      ['white uppercase name', 'White uppercase name: Pawn, pawn']
    ],
    default: 'letter',
    storage: lichess.storage.make('nvui.pieceStyle')
  });
}

export function prefixSetting(): Setting<PrefixStyle> {
  return makeSetting<PrefixStyle>({
    choices: [
      ['letter', 'Letter: w/b'],
      ['name', 'Name: white/black'],
      ['none', 'None']
    ],
    default: 'letter',
    storage: lichess.storage.make('nvui.prefixStyle')
  });
}

export function positionSetting(): Setting<PositionStyle> {
  return makeSetting<PositionStyle>({
    choices: [
      ['before', 'before: c2: wp'],
      ['after', 'after: wp: c2'],
      ['none', 'none']
    ],
    default: 'before',
    storage: lichess.storage.make('nvui.positionStyle')
  });
}
const renderPieceStyle = (piece: string, pieceStyle: PieceStyle) => {
  switch(pieceStyle) {
    case 'letter':
      return letterPiece[piece];
    case 'white uppercase letter':
      return whiteUpperLetterPiece[piece];
    case 'name':
      return namePiece[piece];
    case 'white uppercase name':
      return whiteUpperNamePiece[piece];
  }
}
const renderPrefixStyle = (color: Color, prefixStyle: PrefixStyle) => {
  switch(prefixStyle) {
    case 'letter':
      return color.charAt(0);
    case 'name':
      return color + " ";
    case 'none':
      return '';
  }
}


export function lastCaptured(movesGenerator: () => string[], pieceStyle: PieceStyle, prefixStyle: PrefixStyle): string {
  const moves = movesGenerator();
  const oldFen = moves[moves.length-2];
  const newFen = moves[moves.length-1];
  if (!oldFen || !newFen) {
    return 'none';
  }
  const oldSplitFen = oldFen.split(' ')[0];
  const newSplitFen = newFen.split(' ')[0];
  for (var p of 'kKqQrRbBnNpP') {
    const diff = (oldSplitFen.split(p).length - 1) - (newSplitFen.split(p).length -1);
    const pcolor = p.toUpperCase() === p ? 'white' : 'black';
    if (diff === 1) {
      const prefix = renderPrefixStyle(pcolor, prefixStyle);
      const piece = renderPieceStyle(p, pieceStyle);
      return prefix + piece;
    } 
  }
  return 'none';
}

export function renderSan(san: San, uci: Uci | undefined, style: Style) {
  if (!san) return '';
  let move: string;
  if (san.includes('O-O-O')) move = 'long castling';
  else if (san.includes('O-O')) move = 'short castling';
  else if (style === 'san') move = san.replace(/[\+#]/, '');
  else if (style === 'uci') move = uci || san;
  else {
    move = san.replace(/[\+#]/, '').split('').map(c => {
      if (c == 'x') return 'takes';
      if (c == '+') return 'check';
      if (c == '#') return 'checkmate';
      if (c == '=') return 'promotion';
      const code = c.charCodeAt(0);
      if (code > 48 && code < 58) return c; // 1-8
      if (code > 96 && code < 105) return renderFile(c, style); // a-g
      return roles[c] || c;
    }).join(' ');
  }
  if (san.includes('+')) move += ' check';
  if (san.includes('#')) move += ' checkmate';
  return move;
}

export function renderPieces(pieces: Pieces, style: Style): VNode {
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
      const keys = [];
      for (const [key, piece] of pieces) {
        if (piece.color === color && piece.role === role) keys.push(key);
      }
      if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).map((k: string) => renderKey(k, style)).join(', ')}`
      ).join(', ')
    ]);
  }));
}

export function renderPieceKeys(pieces: Pieces, p: string, style: Style): string {
  const name = `${p === p.toUpperCase() ? 'white' : 'black'} ${roles[p.toUpperCase()]}`;
  const res: Key[] = [];
  for (const [k, piece] of pieces) {
    if (piece && `${piece.color} ${piece.role}` === name) res.push(k as Key);
  }
  return `${name}: ${res.length ? res.map(k => renderKey(k, style)).join(', ') : 'none'}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFile: string, style: Style): string {
  const res: string[] = [];
  for (const k of allKeys) {
    if (k.includes(rankOrFile)) {
      const piece = pieces.get(k);
      if (piece) res.push(`${renderKey(k, style)} ${piece.color} ${piece.role}`);
    }
  }
  return res.length ? res.join(', ') : 'blank';
}

export function renderBoard(pieces: Pieces, pov: Color, pieceStyle: PieceStyle, prefixStyle: PrefixStyle, positionStyle: PositionStyle, boardStyle: BoardStyle): VNode {
  const doRankHeader = (rank: Rank): VNode => {
    return h('th', {attrs: {scope: 'row'}}, rank);
  }
  const doFileHeaders = (): VNode => {
    let ths = files.map(file => h('th', {attrs: {scope: 'col'}}, file));
    if (pov === 'black') ths.reverse();
    return h('tr', [
      h('td'),
      ...ths,
      h('td')
    ]);
  }
  const renderPositionStyle = (rank: Rank, file: File, orig: string) => {
    switch(positionStyle) {
      case 'before':
        return file.toUpperCase() + rank + ' ' + orig;
      case 'after':
        return orig + ' ' + file.toUpperCase() + rank;
      case 'none':
        return orig;
    }
  }
  const doPieceButton = (rank: Rank, file: File, letter: string, color: Color | 'none', text: string): VNode => {
    return h('button', {
      attrs: { rank: rank, file: file, piece: letter.toLowerCase(), color: color}
    }, text);
  }
  const doPiece = (rank: Rank, file: File): VNode => {
    const key = file + rank as Key;
    const piece = pieces.get(key);
    const pieceWrapper = boardStyle === 'table' ? 'td' : 'span';
    if (piece) {        
      const role = letters[piece.role];
      const pieceText = renderPieceStyle(piece.color === 'white' ? role.toUpperCase() : role, pieceStyle);
      const prefix = renderPrefixStyle(piece.color, prefixStyle);
      const text = renderPositionStyle(rank, file, prefix + pieceText);
      return h(pieceWrapper, doPieceButton(rank, file, role, piece.color, text));
    } else {
      const letter = (key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+';
      const text = renderPositionStyle(rank, file, letter);
      return h(pieceWrapper, doPieceButton(rank, file, letter, 'none', text));
    }
  }
  const doRank = (pov: Color, rank: Rank): VNode => {      
    let rankElements = [];
    if (boardStyle === 'table') rankElements.push(doRankHeader(rank));
    rankElements.push(...files.map(file => doPiece(rank, file)));
    if (boardStyle === 'table') rankElements.push(doRankHeader(rank));
    if (pov === 'black') rankElements.reverse();
    return h((boardStyle === 'table' ? 'tr' : 'div'), rankElements);
  }
  let ranks: VNode[] = [];
  if (boardStyle === 'table') ranks.push(doFileHeaders());
  ranks.push(...invRanks.map(rank => doRank(pov, rank)));
  if (boardStyle === 'table') ranks.push(doFileHeaders());
  if (pov === 'black') ranks.reverse();
  return h((boardStyle === 'table' ? 'table.board-wrapper' : 'div.board-wrapper'), ranks);
}

export function renderFile(f: string, style: Style): string {
  return style === 'nato' ? nato[f] : (style === 'anna' ? anna[f] : f);
}

export function renderKey(key: string, style: Style): string {
  return `${renderFile(key[0], style)} ${key[1]}`;
}

export function castlingFlavours(input: string): string {
  switch(input.toLowerCase().replace(/[-\s]+/g, '')) {
    case 'oo': case '00': return 'o-o';
    case 'ooo': case '000': return 'o-o-o';
  }
  return input;
}

/* Listen to interactions on the chessboard */
function positionJumpHandler() {
  return (ev: KeyboardEvent) => {
    const $btn = $(ev.target as HTMLElement);
    const $file = $btn.attr('file') ?? "";
    const $rank = $btn.attr('rank') ?? "";
    let $newRank = "";
    let $newFile = "";
    if (ev.key.match(/^[1-8]$/)) {
      $newRank = ev.key;
      $newFile = $file;
    } else if (ev.key.match(/^[!@#$%^&*]$/)) {
      $newRank = $rank;
      $newFile = symbolToFile(ev.key);
    // if not a valid key for jumping
    } else {
      return true;
    }
    const newBtn = document.querySelector('.board-wrapper button[rank="' + $newRank + '"][file="' + $newFile + '"]') as HTMLElement;
    if (newBtn) {
      newBtn.focus();
      return false;
    }
    return true;
  }
}

function pieceJumpingHandler(wrapSound: () => void, errorSound: () => void) {
  return (ev: KeyboardEvent) => {
    if (!ev.key.match(/^[kqrbnp]$/i)) return true;
    const $currBtn = $(ev.target as HTMLElement);
    const $myBtnAttrs = '.board-wrapper [rank="' + $currBtn.attr('rank') + '"][file="' + $currBtn.attr('file') + '"]';
    const $allPieces = $('.board-wrapper [piece="' + ev.key.toLowerCase() + '"], ' + $myBtnAttrs);
    const $myPieceIndex = $allPieces.index($myBtnAttrs);
    const $next = ev.key.toLowerCase() === ev.key;
    const $prevNextPieces = $next ? $allPieces.slice($myPieceIndex+1) : $allPieces.slice(0, $myPieceIndex);
    const $piece = $next ? $prevNextPieces.get(0) : $prevNextPieces.get($prevNextPieces.length-1);
    if ($piece) {
      $piece.focus();
    // if detected any matching piece; one is the pice being clicked on,
    } else if ($allPieces.length >= 2) {
      const $wrapPiece = $next ? $allPieces.get(0): $allPieces.get($allPieces.length-1);
      $wrapPiece?.focus();
      wrapSound();
    } else {
      errorSound();
    }
    return false;
  };
}

function arrowKeyHandler(pov: Color, borderSound: () => void) {
  return (ev: KeyboardEvent) => {
    const $currBtn = $(ev.target as HTMLElement);
    const $isWhite = pov === 'white';
    let $file = $currBtn.attr('file') ?? " ";
    let $rank = Number($currBtn.attr('rank'));
    if (ev.key === 'ArrowUp') {
      $rank = $isWhite ? $rank += 1 : $rank -= 1;
    } else if (ev.key === 'ArrowDown') {
      $rank = $isWhite ? $rank -= 1 : $rank += 1;
    } else if (ev.key === 'ArrowLeft') {
      $file = String.fromCharCode($isWhite ? $file.charCodeAt(0) - 1 : $file.charCodeAt(0) + 1);
    } else if (ev.key === 'ArrowRight') {
      $file = String.fromCharCode($isWhite ? $file.charCodeAt(0) + 1 : $file.charCodeAt(0) - 1);
    } else {
      return true;
    }
    const $newSq = document.querySelector('.board-wrapper [file="' + $file + '"][rank="' + $rank + '"]') as HTMLElement;
    if ($newSq) {
      $newSq.focus();
    } else {
      borderSound();
    }
    ev.preventDefault();
    return false;
  };
}

function selectionHandler(opponentColor: Color, selectSound: () => void) {
  return (ev: MouseEvent) => {
    // this depends on the current document structure. This may not be advisable in case the structure wil change.
    const $evBtn = $(ev.target as HTMLElement);
    const $pos = ($evBtn.attr('file') ?? "") + $evBtn.attr('rank');
    const $moveBox = $(document.querySelector('input.move') as HTMLInputElement);
    if (!$moveBox) return false;

    // if no move in box yet
    if ($moveBox.val() === '') {
      // if user selects anothers' piece first
      if ($evBtn.attr('color') === opponentColor) return false;
      // as long as the user is selecting a piece and not a blank tile
      if ($evBtn.text().match(/^[^\-+]+/g)) {
        $moveBox.val($pos);
        selectSound();
      }
    } else {
      // if user selects their own piece second
      if ($evBtn.attr('color') === (opponentColor === 'black' ? 'white' : 'black')) return false;

      $moveBox.val($moveBox.val() + $pos);
      // this section depends on the form being the granparent of the input.move box.
      const $form = $moveBox.parent().parent();
      const $event = new Event('submit', {
        cancelable: true,
        bubbles: true
      })
      $form.trigger($event);
    }
    return false;
  };
}

function boardCommandsHandler() {
  return (ev: KeyboardEvent) => {
    const $currBtn = $(ev.target as HTMLElement);
    const $boardLive = $('.boardstatus');
    const $position = ($currBtn.attr('file') ?? "") + ($currBtn.attr('rank') ?? "")
    if (ev.key === 'o') {
      $boardLive.text()
      $boardLive.text($position);
      return false;
    } else if (ev.key === 'l') {
      const $lastMove = $('p.lastMove').text();
      $boardLive.text();
      $boardLive.text($lastMove);
      return false;
    } else if (ev.key === 't') {
      $boardLive.text();
      $boardLive.text($('.nvui .botc').text() + ', ' + $('.nvui .topc').text());
      return false;
    }
    return true;
  };
}
function lastCapturedCommandHandler(steps: () => string[], pieceStyle: PieceStyle, prefixStyle: PrefixStyle) {
  return (ev: KeyboardEvent) => {
    const $boardLive = $('.boardstatus');
    if (ev.key === 'c') {
      $boardLive.text();
      $boardLive.text(lastCaptured(steps, pieceStyle, prefixStyle));
      return false;
    }
    return true;
  }
}

export function analysisBoardListenersSetup(color: Color, ocolor: Color, selectSound: () => void, wrapSound: () => void, borderSound: () => void, errorSound: () => void): (vnode: HTMLElement) => void {
  return (el: HTMLElement) => {
    const $board = $(el as HTMLElement);
    $board.on('keypress', boardCommandsHandler());
    const $buttons = $board.find('button');
    $buttons.on('click', selectionHandler(ocolor, selectSound));
    $buttons.on('keydown', arrowKeyHandler(color, borderSound));
    $buttons.on('keypress', positionJumpHandler());
    $buttons.on('keypress', pieceJumpingHandler(wrapSound, errorSound));
  };
}

export function roundBoardListenersSetup(color: Color, ocolor: Color, steps: () => string[], pieceStyle: PieceStyle, prefixStyle: PrefixStyle, selectSound: () => void, wrapSound: () => void, borderSound: () => void, errorSound: () => void) {
  return (el: HTMLElement) => {
    console.log(steps);
    console.log(steps());
    const $board = $(el);
    $board.on('keypress', boardCommandsHandler());
    // NOTE: This is the only line different from analysisBoardListenerSetup
    $board.on('keypress', lastCapturedCommandHandler(steps, pieceStyle, prefixStyle));
    const $buttons = $board.find('button');
    $buttons.on('click', selectionHandler(ocolor, selectSound));
    $buttons.on('keydown', arrowKeyHandler(color, borderSound));
    $buttons.on('keypress', positionJumpHandler());
    $buttons.on('keypress', pieceJumpingHandler(wrapSound, errorSound));
  };
}