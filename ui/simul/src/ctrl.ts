import { makeSocket, SimulSocket } from './socket';
import xhr from './xhr';
import * as status from 'game/status';
import {
  SimulData,
  SimulOpts
} from './interfaces';

export default class SimulCtrl {

  data: SimulData;
  trans: Trans;
  socket: SimulSocket;

  constructor(readonly opts: SimulOpts, readonly redraw: () => void) {
    this.data = opts.data;
    this.trans = window.lichess.trans(opts.i18n);
    this.socket = makeSocket(opts.socketSend, this);
    if (this.createdByMe() && this.data.isCreated) this.setupCreatedHost();
  }

  private setupCreatedHost = () => {
    window.lichess.storage.set('lichess.move_on', '1'); // hideous hack :D
    let hostIsAround = true;
    window.lichess.idleTimer(
      15 * 60 * 1000,
      () => { hostIsAround = false; },
      () => { hostIsAround = true; });
    setInterval(() => {
      if (this.data.isCreated && hostIsAround) xhr.ping(this.data.id);
    }, 10 * 1000);
  };

  reload = (data: SimulData) => {
    this.data = {
      ...data,
      team: this.data.team // reload data does not contain the team anymore
    }
  };

  teamBlock = () => this.data.team && !this.data.team.isIn;

  createdByMe = () => this.opts.userId === this.data.host.id;
  candidates = () => this.data.applicants.filter(a => !a.accepted);
  accepted = () => this.data.applicants.filter(a => a.accepted);
  myCurrentPairing = () =>
    this.opts.userId ? this.data.pairings.find(p =>
      p.game.status < status.ids.mate && p.player.id === this.opts.userId
    ) : null;
  acceptedContainsMe = () => this.accepted().some(a => a.player.id === this.opts.userId);
  applicantsContainsMe = () => this.candidates().some(a => a.player.id === this.opts.userId);
  containsMe = () => this.opts.userId && (this.applicantsContainsMe() || this.pairingsContainMe());
  pairingsContainMe = () => this.data.pairings.some(a => a.player.id === this.opts.userId);

}
