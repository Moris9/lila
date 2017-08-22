import { h, thunk } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { throttle } from 'common';
import { option } from '../util';
import AnalyseCtrl from '../ctrl';
import { StudyCtrl, StudyChapter } from './interfaces';

function editable(value: string, submit: (v: string, el: HTMLInputElement) => void): VNode {
  return h('input', {
    attrs: {
      spellcheck: false,
      value
    },
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLInputElement;
        el.onblur = function() {
          submit(el.value, el);
        };
        el.onkeypress = function(e) {
          if ((e.keyCode || e.which) == 13) el.blur();
        }
      }
    }
  });
}

function fixed(text) {
  return h('span', text);
}

let selectedType: string;

type TagRow = (string | VNode)[];

function renderPgnTags(chapter: StudyChapter, submit, types: string[]): VNode {
  let rows: TagRow[] = [];
  if (chapter.setup.variant.key !== 'standard')
  rows.push(['Variant', fixed(chapter.setup.variant.name)]);
  rows = rows.concat(chapter.tags.map(tag => [
    tag[0],
    submit ? editable(tag[1], submit(tag[0])) : fixed(tag[1])
  ]));
  if (submit) {
    const existingTypes = chapter.tags.map(t => t[0]);
    rows.push([
      h('select', {
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLInputElement;
            selectedType = el.value;
            el.addEventListener('change', _ => {
              selectedType = el.value;
              $(el).parents('tr').find('input').focus();
            });
          },
          postpatch: (_, vnode) => {
            selectedType = (vnode.elm as HTMLInputElement).value;
          }
        }
      }, [
        h('option', 'New tag'),
        ...types.map(t => {
          if (!window.lichess.fp.contains(existingTypes, t)) return option(t, '', t);
        })
      ]),
      editable('', (value, el) => {
        if (selectedType) {
          submit(selectedType)(value);
          el.value = '';
        }
      })
    ]);
  }

  return h('table.tags.slist', h('tbody', rows.map(function(r) {
    return h('tr', {
      key: '' + r[0]
    }, [
      h('th', [r[0]]),
      h('td', [r[1]])
    ]);
  })));
}

export function ctrl(root: AnalyseCtrl, getChapter: () => StudyChapter, types) {

  const submit = throttle(500, false, function(name, value) {
    root.study!.makeChange('setTag', {
      chapterId: getChapter().id,
      name,
      value: value.substr(0, 140)
    });
  });

  return {
    submit(name) {
      return value => submit(name, value);
    },
    getChapter,
    types
  }
}
function doRender(root: StudyCtrl): VNode {
  return h('div.undertable_inner', renderPgnTags(
    root.tags.getChapter(),
    root.vm.mode.write && root.tags.submit,
    root.tags.types))
}

export function view(root: StudyCtrl): VNode {
  const chapter = root.tags.getChapter(),
  key = chapter.id + root.data.name + chapter.name + root.data.likes + chapter.tags + root.vm.mode.write;
  return thunk('div.undertable_inner.' + chapter.id, doRender, [root, key]);
}
