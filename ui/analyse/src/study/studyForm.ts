import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as dialog from './dialog';
import { prop, Prop } from 'common';
import { bind, bindSubmit } from '../util';
import { StudyData } from './interfaces';
import { MaybeVNodes } from '../interfaces';
import RelayCtrl from './relay/relayCtrl';

export interface StudyFormCtrl {
  open: Prop<boolean>;
  openIfNew(): void;
  save(data: FormData, isNew: boolean): void;
  getData(): StudyData;
  isNew(): boolean;
  redraw(): void;
  relay?: RelayCtrl;
}

interface FormData {
  [key: string]: string;
}

interface Select {
  key: string;
  name: string;
  choices: Choice[];
  selected: string;
}
type Choice = [string, string];

const visibilityChoices: Choice[] = [
  ['public', 'Public'],
  ['unlisted', 'Unlisted'],
  ['private', 'Invite only']
];
const userSelectionChoices: Choice[] = [
  ['nobody', 'Nobody'],
  ['owner', 'Only me'],
  ['contributor', 'Contributors'],
  ['member', 'Members'],
  ['everyone', 'Everyone']
];

function select(s: Select): MaybeVNodes {
  return [
    h('select#study-' + s.key, s.choices.map(function(o) {
      return h('option', {
        attrs: {
          value: o[0],
          selected: s.selected === o[0]
        }
      }, o[1]);
    })),
    h('label.control-label', {
      attrs: { for: 'study-' + s.key }
    }, s.name),
    h('i.bar')
  ];
};

export function ctrl(save: (data: FormData, isNew: boolean) => void, getData: () => StudyData, redraw: () => void, relay?: RelayCtrl): StudyFormCtrl {

  const initAt = Date.now();

  function isNew(): boolean {
    const d = getData();
    return d.from === 'scratch' && !!d.isNew && Date.now() - initAt < 9000;
  }

  const open = prop(false);

  return {
    open,
    openIfNew() {
      if (isNew()) open(true);
    },
    save(data: FormData, isNew: boolean) {
      save(data, isNew);
      open(false);
    },
    getData,
    isNew,
    redraw,
    relay
  };
}

export function view(ctrl: StudyFormCtrl): VNode {
  const data = ctrl.getData();
  const isNew = ctrl.isNew();
  const updateName = function(vnode, isUpdate) {
    const el = vnode.elm as HTMLInputElement;
    if (!isUpdate && !el.value) {
      el.value = data.name;
      if (isNew) el.select();
      el.focus();
    }
  }
  return dialog.form({
    class: 'study-edit',
    onClose: function() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.relay ? 'Configure live broadcast' : (isNew ? 'Create' : 'Edit') + ' study'),
      h('form.material.form.align-left', {
        hook: bindSubmit(e => {
          const obj: FormData = {};
          'name visibility computer explorer cloneable chat sticky'.split(' ').forEach(n => {
            const el = ((e.target as HTMLElement).querySelector('#study-' + n) as HTMLInputElement);
            if (el) obj[n] = el.value;
          });
          ctrl.save(obj, isNew);
        }, ctrl.redraw)
      }, [
        h('div.form-group' + (ctrl.relay ? '.none' : ''), [
          h('input#study-name', {
            attrs: {
              minlength: 3,
              maxlength: 100
            },
            hook: {
              insert: vnode => updateName(vnode, false),
              postpatch: (_, vnode) => updateName(vnode, true)
            }
          }),
          h('label.control-label', { attrs: { 'for': 'study-name' } }, 'Name'),
          h('i.bar')
        ]),
        h('div', [
          h('div.form-group.half', select({
            key: 'visibility',
            name: 'Visibility',
            choices: visibilityChoices,
            selected: data.visibility
          })),
          h('div.form-group.half', select({
            key: 'cloneable',
            name: 'Allow cloning',
            choices: userSelectionChoices,
            selected: data.settings.cloneable
          })),
          h('div.form-group.half', select({
            key: 'computer',
            name: 'Computer analysis',
            choices: userSelectionChoices,
            selected: data.settings.computer
          })),
          h('div.form-group.half', select({
            key: 'explorer',
            name: 'Opening explorer',
            choices: userSelectionChoices,
            selected: data.settings.explorer
          })),
          h('div.form-group.half', select({
            key: 'chat',
            name: 'Chat',
            choices: userSelectionChoices,
            selected: data.settings.chat
          })),
          h('div.form-group.half', select({
            key: 'sticky',
            name: 'Enable sync',
            choices: [
              ['true', 'Yes: keep everyone on the same position'],
              ['false', 'No: let people browse freely']
            ],
            selected: '' + data.settings.sticky
          }))
        ]),
        dialog.button(isNew ? 'Start' : 'Save')
      ]),
      h('div.destructive', [
        isNew ? null : h('form', {
          attrs: {
            action: '/study/' + data.id + '/clear-chat',
            method: 'post'
          },
          hook: bind('submit', _ => {
            return confirm('Delete the study chat history? There is no going back!');
          })
        }, [
          h('button.button.frameless', 'Clear chat')
        ]),
        h('form', {
          attrs: {
            action: '/study/' + data.id + '/delete',
            method: 'post'
          },
          hook: bind('submit', _ => {
            return isNew || confirm('Delete the entire study? There is no going back!');
          })
        }, [
          h('button.button.frameless', isNew ? 'Cancel' : 'Delete study')
        ])
      ])
    ]
  });
}
