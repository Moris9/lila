import { h, thunk } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Ctrl, Line } from './interfaces'
import * as spam from './spam'
import enhance from './enhance';
import { presetView } from './preset';
import { lineAction } from './moderation';
import { userLink } from './util';

const whisperRegex = /^\/w(?:hisper)?\s/;

export default function(ctrl: Ctrl): VNode[] {
  if (!ctrl.vm.enabled) return [];
  const scrollCb = (vnode: VNode) => {
    const el = vnode.elm as HTMLElement
      if (ctrl.data.lines.length > 5) {
        const autoScroll = (el.scrollTop === 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 100)));
        if (autoScroll) {
          el.scrollTop = 999999;
          setTimeout(_ => el.scrollTop = 999999, 300)
        }
      }
  }
  const vnodes = [
    h('ol.messages.content.scroll-shadow-soft', {
      hook: {
        insert(vnode) {
          $(vnode.elm as HTMLElement).on('click', 'a.jump', (e: Event) => {
            window.lichess.pubsub.emit('jump')((e.target as HTMLElement).getAttribute('data-ply'));
          });
          scrollCb(vnode);
        },
        postpatch: (_, vnode) => scrollCb(vnode)
      }
    }, selectLines(ctrl).map(line => renderLine(ctrl, line))),
    renderInput(ctrl)
  ];
  const presets = presetView(ctrl.preset);
  if (presets) vnodes.push(presets)
  return vnodes
}

function renderInput(ctrl: Ctrl) {
  if ((ctrl.data.loginRequired && !ctrl.data.userId) || ctrl.data.restricted)
  return h('input.lichess_say', {
    attrs: {
      placeholder: 'Login to chat',
      disabled: true
    }
  });
  let placeholder: string;
  if (ctrl.vm.timeout) placeholder = 'You have been timed out.';
  else if (!ctrl.vm.writeable) placeholder = 'Invited members only.';
  else placeholder = ctrl.trans(ctrl.vm.placeholderKey);
  return h('input.lichess_say', {
    attrs: {
      placeholder: placeholder,
      autocomplete: 'off',
      maxlength: 140,
      disabled: ctrl.vm.timeout || !ctrl.vm.writeable
    },
    on: {
      keyup(e: KeyboardEvent) {
        const el = e.target as HTMLInputElement;
        const txt = el.value;
        if (e.which == 10 || e.which == 13) {
          if (txt === '') {
            const kbm = document.querySelector('.keyboard-move input') as HTMLElement;
            if (kbm) kbm.focus();
          } else {
            spam.report(txt);
            if (ctrl.opts.public && spam.hasTeamUrl(txt)) alert("Please don't advertise teams in the chat.");
            else ctrl.post(txt);
            el.value = '';
            el.classList.remove('whisper');
          }
        } else el.classList.toggle('whisper', !!txt.match(whisperRegex));
      }
    }
  })
}

function sameLines(l1: Line, l2: Line) {
  return l1.d && l2.d && l1.u === l2.u;
}

function selectLines(ctrl: Ctrl): Array<Line> {
  let prev: Line, ls: Array<Line> = [];
  ctrl.data.lines.forEach(line => {
    if (!line.d &&
      (!prev || !sameLines(prev, line)) &&
      (!line.r || ctrl.opts.kobold) &&
      !spam.skip(line.t)
    ) ls.push(line);
    prev = line;
  });
  return ls;
}

function renderText(t: string, parseMoves: boolean) {
  return h('t', {
    props: {
      innerHTML: enhance(t, parseMoves)
    }
  });
}

function renderLine(ctrl: Ctrl, line: Line) {

  const textNode = thunk('t', line.t, renderText, [line.t, ctrl.opts.parseMoves]);

  if (line.u === 'lichess') return h('li.system', textNode);

  if (line.c) return h('li', [
    h('span', '[' + line.c + ']'),
    textNode
  ]);
  var userNode = thunk('a', line.u, userLink, [line.u]);

  return ctrl.moderation ? h('li', [
    lineAction(() => ctrl.moderation && line.u && ctrl.moderation.open(line.u.split(' ')[0])),
    userNode,
    textNode
  ]) : h('li', [userNode, textNode]);
}
