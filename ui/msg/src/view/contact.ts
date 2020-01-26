import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Contact, LastMsg } from '../interfaces'
import MsgCtrl from '../ctrl';
import { userName, userIcon, bindMobileMousedown } from './util';

export default function renderContact(ctrl: MsgCtrl, contact: Contact, active?: string): VNode {
  const user = contact.user, msg = contact.lastMsg;
  return h('div.msg-app__side__contact', {
    key: user.id,
    class: {
      active: active == user.id,
      new: !!msg && !msg.read && msg.user != ctrl.data.me.id
    },
    hook: bindMobileMousedown(_ => ctrl.openConvo(user.id)),
  }, [
    userIcon(user, 'msg-app__side__contact__icon'),
    h('div.msg-app__side__contact__user', [
      h('div.msg-app__side__contact__head', [
        h('div.msg-app__side__contact__name', userName(user)),
        msg ? h('div.msg-app__side__contact__date', renderDate(msg)) : null
      ]),
      msg ? h('div.msg-app__side__contact__msg', msg.text) : null
    ])
  ]);
}

function renderDate(msg: LastMsg): VNode {
  return h('time.timeago', {
    key: msg.date.getTime(),
    attrs: {
      title: msg.date.toLocaleString(),
      datetime: msg.date.getTime()
    }
  }, window.lichess.timeago.format(msg.date));
}
