import StrongSocket from './socket';

const li: any = window.lichess;

export default function moduleLaunchers() {
  if (li.userAnalysis) startUserAnalysis(li.userAnalysis);
  else if (li.study) startAnalyse(li.study);
  else if (li.practice) startAnalyse(li.practice);
  else if (li.relay) startAnalyse(li.relay);
}

function startUserAnalysis(cfg) {
  cfg.$side = $('.analyse__side').clone();
  startAnalyse(cfg);
}

function startAnalyse(cfg) {
  let analyse;
  cfg.socketSend = li.socket.send;
  window.lichess.socket = new StrongSocket(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d)
  });
  analyse = window.LichessAnalyse.start(cfg);
}
