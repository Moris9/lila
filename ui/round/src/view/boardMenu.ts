import { h } from 'snabbdom';
import { snabModal } from 'common/modal';
import * as licon from 'common/licon';
import RoundController from '../ctrl';
import { bind } from '../util';
import { game as gameRoute } from 'game/router';
import { toggle, ToggleSettings } from 'common/controls';
import { dataIcon } from 'common/snabbdom';

export const boardMenu = (ctrl: RoundController) =>
  snabModal({
    class: 'board-menu',
    onClose: () => ctrl.menu(false),
    content: [
      h(
        'button.button.text',
        {
          class: { active: ctrl.flip },
          attrs: dataIcon(''),
          hook: bind('click', () => {
            const d = ctrl.data;
            if (d.tv) location.href = '/tv/' + d.tv.channel + (d.tv.flip ? '' : '?flip=1');
            else if (d.player.spectator) location.href = gameRoute(d, d.opponent.color);
            else ctrl.flipNow();
          }),
        },
        ctrl.noarg('flipBoard')
      ),
      ctrlToggle(
        {
          name: 'Enable voice input',
          title: 'Enable voice input',
          id: 'voice',
          checked: ctrl.voiceMoveEnabled(),
          disabled: false,
          change: ctrl.voiceMoveEnabled,
        },
        ctrl
      ),
      ctrlToggle(
        {
          name: 'Enable keyboard input',
          title: 'Enable keyboard input',
          id: 'keyboard',
          checked: ctrl.keyboardMoveEnabled(),
          disabled: false,
          change: ctrl.keyboardMoveEnabled,
        },
        ctrl
      ),
    ],
  });

const ctrlToggle = (t: ToggleSettings, ctrl: RoundController) => toggle(t, ctrl.trans, ctrl.redraw);
