import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Ctrl, Challenge, ChallengeData, ChallengeDirection, ChallengeUser, TimeControl } from './interfaces'

export function loaded(ctrl: Ctrl): VNode {
  return h('div#challenge_app.links.dropdown.rendered', renderContent(ctrl));
}

export function loading(): VNode {
  return h('div#challenge_app.links.dropdown.rendered', [
    h('div.empty.loading', '-'),
    create()
  ]);
}

function renderContent(ctrl: Ctrl): VNode[] {
  let d = ctrl.data();
  const nb = d.in.length + d.out.length;
  return nb ? [allChallenges(ctrl, d, nb)] : [
    empty(),
    create()
  ];
}

function userPowertips(vnode: VNode) {
  window.lichess.powertip.manualUserIn(vnode.elm);
}

function allChallenges(ctrl: Ctrl, d: ChallengeData, nb: number): VNode {
  return h('div.challenges', {
    class: { many: nb > 3 },
    hook: {
      insert: userPowertips,
      postpatch: userPowertips
    }
  }, d.in.map(challenge(ctrl, 'in')).concat(d.out.map(challenge(ctrl, 'out'))));
}

function challenge(ctrl: Ctrl, dir: ChallengeDirection) {
  return (c: Challenge) => {
    return h('div.challenge.' + dir + '.c-' + c.id, {
      class: {
        declined: !!c.declined
      }
    }, [
      h('div.content', [
        h('span.title', renderUser(dir === 'in' ? c.challenger : c.destUser)),
        h('span.desc', [
          window.lichess.globalTrans(c.rated ? 'Rated' : 'Casual'),
          timeControl(c.timeControl),
          c.variant.name
        ].join(' • '))
      ]),
      h('i', {
        attrs: {'data-icon': c.perf.icon}
      }),
      h('div.buttons', (dir === 'in' ? inButtons : outButtons)(ctrl, c))
    ]);
  };
}

function inButtons(ctrl: Ctrl, c: Challenge): VNode[] {
  return [
    h('form', {
      attrs: {
        method: 'post',
        action: `/challenge/${c.id}/accept`
      }
    }, [
      h('button.button.accept', {
        attrs: {
          'type': 'submit',
          'data-icon': 'E'
        }
      })]),
    h('button.submit.button.decline', {
      attrs: {
        'type': 'submit',
        'data-icon': 'L'
      },
      hook: {
        insert: (vnode: VNode) => {
          (vnode.elm as HTMLElement).addEventListener('click', () => ctrl.decline(c.id));
        }
      }
    })
  ];
}

function outButtons(ctrl: Ctrl, c: Challenge) {
  return [
    h('div.owner', [
      h('span.waiting', ctrl.trans()('waiting')),
      h('a.view', {
        attrs: {
          'data-icon': 'v',
          href: '/' + c.id
        }
      })
    ]),
    h('button.button.decline', {
      attrs: { 'data-icon': 'L' },
      hook: {
        insert: (vnode: VNode) => {
          (vnode.elm as HTMLElement).addEventListener('click', () => ctrl.cancel(c.id));
        }
      }
    })
  ];
}

function timeControl(c: TimeControl): string {
  switch (c.type) {
    case 'unlimited':
      return 'Unlimited';
    case 'correspondence':
      return c.daysPerTurn + ' days';
    case 'clock':
      return c.show || '-';
  }
}

function renderUser(u?: ChallengeUser): VNode {
  if (!u) return h('span', 'Open challenge');
  const rating = u.rating + (u.provisional ? '?' : '');
  return h('a.ulpt.user_link', {
    attrs: { href: `/@/${u.name}`},
    class: { online: !!u.online }
  }, [
    h('i.line' + (u.patron ? '.patron' : '')),
    h('name', (u.title ? u.title + ' ' : '') + u.name + ' (' + rating + ')')
  ]);
}

function create(): VNode {
  return h('a.create', {
    attrs: {
      href: '/?any#friend',
      'data-icon': 'O'
    },
    title: 'Challenge someone'
  });
}

function empty(): VNode {
  return h('div.empty.text', {
    attrs: {
      'data-icon': '',
    }
  }, 'No challenges.');
}
