import { bind, onInsert } from 'common/snabbdom';
import { titleNameToId } from '../util';
import { h, VNode } from 'snabbdom';
import { snabModal } from 'common/modal';
import { prop, Prop } from 'common';
import { StudyMemberMap } from './interfaces';
import { AnalyseSocketSend } from '../socket';
import { storedSet, StoredSet } from 'common/storage';

export interface StudyInviteFormCtrl {
  open: Prop<boolean>;
  members: Prop<StudyMemberMap>;
  spectators: Prop<string[]>;
  toggle(): void;
  invite(titleName: string): void;
  redraw(): void;
  trans: Trans;
  previouslyInvited: StoredSet<string>;
}

export function makeCtrl(
  send: AnalyseSocketSend,
  members: Prop<StudyMemberMap>,
  setTab: () => void,
  redraw: () => void,
  trans: Trans
): StudyInviteFormCtrl {
  const open = prop(false),
    spectators = prop<string[]>([]);

  const toggle = () => {
    if (!open()) lichess.pubsub.emit('analyse.close-all');
    open(!open());
  };

  lichess.pubsub.on('analyse.close-all', () => open(false));

  const previouslyInvited = storedSet<string>('study.previouslyInvited', 10);
  return {
    open,
    members,
    spectators,
    toggle,
    invite(titleName: string) {
      const userId = titleNameToId(titleName);
      send('invite', userId);
      setTimeout(() => previouslyInvited(userId), 1000);
      setTab();
    },
    redraw,
    trans,
    previouslyInvited,
  };
}

export function view(ctrl: ReturnType<typeof makeCtrl>): VNode {
  const candidates = [...new Set([...ctrl.spectators(), ...ctrl.previouslyInvited()])]
    .filter(s => !ctrl.members()[titleNameToId(s)]) // remove existing members
    .sort();
  return snabModal({
    class: 'study__invite',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.trans.noarg('inviteToTheStudy')),
      h('p.info', { attrs: { 'data-icon': '' } }, ctrl.trans.noarg('pleaseOnlyInvitePeopleYouKnow')),
      h('div.input-wrapper', [
        // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: ctrl.trans.noarg('searchByUsername') },
          hook: onInsert<HTMLInputElement>(input =>
            lichess.userComplete().then(uac => {
              uac({
                input,
                tag: 'span',
                onSelect(v) {
                  input.value = '';
                  ctrl.invite(v.name);
                  ctrl.redraw();
                },
              });
              input.focus();
            })
          ),
        }),
      ]),
      candidates.length
        ? h(
            'div.users',
            candidates.map(function (username: string) {
              return h(
                'span.button.button-metal',
                {
                  key: username,
                  hook: bind('click', _ => {
                    ctrl.invite(username);
                  }),
                },
                username
              );
            })
          )
        : undefined,
    ],
  });
}
