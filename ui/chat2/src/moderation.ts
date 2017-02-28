import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ModerationCtrl, ModerationOpts, ModerationData, ModerationReason } from './interfaces'
import { userModInfo } from './xhr'
import { userLink, spinner } from './util';

function isToday(timestamp: number) {
  return window.moment(timestamp).isSame(new Date(), 'day');
}

export function moderationCtrl(opts: ModerationOpts): ModerationCtrl {

  let data: ModerationData | undefined;
  let loading = false;

  const open = (username: string) => {
    loading = true;
    userModInfo(username).then(d => {
      data = d;
      loading = false;
      opts.redraw();
    });
    opts.redraw();
  };

  const close = () => {
    data = undefined;
    loading = false;
    opts.redraw();
  };

  return {
    loading: () => loading,
    data: () => data,
    reasons: opts.reasons,
    permissions: opts.permissions,
    open: open,
    close: close,
    timeout(reason: ModerationReason) {
      data && opts.send('timeout', {
        userId: data.id,
        reason: reason.key
      });
      close();
      opts.redraw();
    },
    shadowban() {
      loading = true;
      data && $.post('/mod/' + data.id + '/troll?set=1').then(() => data && open(data.username));
      opts.redraw();
    }
  };
}

export function lineAction(onClick: (e: Event) => void) {
  return h('i.mod', {
    on: {
      click: onClick
    },
    attrs: {
      'data-icon': '',
      title: 'Moderation'
    }
  });
}

export function moderationView(ctrl?: ModerationCtrl): VNode[] | undefined {
  if (!ctrl) return;
  if (ctrl.loading()) return [h('div.loading', spinner())];
  var data = ctrl.data();
  if (!data) return;
  return [
    h('div.top', [
      h('span.text', {
        attrs: {'data-icon': '' },
      }, [userLink(data.username)]),
      h('span.toggle_chat', {
        attrs: {'data-icon': 'L'},
        on: {click: ctrl.close}
      })
    ]),
    h('div.content.moderation', [
      h('div.infos.block', [
        window.lichess.numberFormat(data.games) + ' games',
        data.troll ? 'TROLL' : undefined,
        data.engine ? 'ENGINE' : undefined,
        data.booster ? 'BOOSTER' : undefined
      ].map(t => t && h('span', t)).concat([
        h('a', {
          attrs: {
            href: '/@/' + data.username + '?mod'
          }
        }, 'profile')
      ]).concat(
        ctrl.permissions.shadowban ? [
          h('a', {
            attrs: {
              href: '/mod/' + data.username + '/communication'
            }
          }, 'coms')
        ] : [])),
      h('div.timeout.block', [
        h('h2', 'Timeout 10 minutes for'),
        ...ctrl.reasons.map(r => {
          return h('a.text', {
            attrs: { 'data-icon': 'p' },
            on: { click: [ctrl.timeout, r] }
          }, r.name);
        }),
        ...(
          (data.troll || !ctrl.permissions.shadowban) ? [] : [h('div.shadowban', [
            'Or ',
            h('button.button', {
              on: { click: [ctrl.shadowban, data.username] }
            }, 'shadowban')
          ])])
      ]),
      h('div.history.block', [
        h('h2', 'Timeout history'),
        h('table', h('tbody.slist', {
          hook: {
            insert: () => window.lichess.pubsub.emit('content_loaded')()
          }
        }, data.history.map(function(e) {
          return h('tr', [
            h('td.reason', e.reason),
            h('td.mod', e.mod),
            h('td', h('time.moment', {
              attrs: {
                'data-format': isToday(e.date) ? 'LT' : 'DD/MM/YY',
                datetime: new Date(e.date).toISOString()
              }
            }))
          ]);
        })))
      ])
    ])
  ];
};
