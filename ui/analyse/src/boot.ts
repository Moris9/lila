import { AnalyseApi, AnalyseOpts } from './interfaces';
import { start } from './main';

export default function(cfg: AnalyseOpts) {
  const li = window.lichess,
    element = document.getElementById('main-wrap') as HTMLElement,
    data = cfg.data;
  let analyse: AnalyseApi;

  li.socket = li.StrongSocket(
    data.url.socket,
    data.player.version, {
      options: {
        name: 'analyse'
      },
      params: {
        userTv: data.userTv && data.userTv.id
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      },
      events: {
      }
    });
  cfg.$side = $('.analyse__side').clone();
  cfg.trans = li.trans(cfg.i18n);
  cfg.initialPly = 'url';
  cfg.element = element.querySelector('main.analyse') as HTMLElement;
  cfg.socketSend = li.socket.send;
  analyse = start(cfg);
};
