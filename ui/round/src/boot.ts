import { RoundOpts, RoundData } from './interfaces';
import { RoundApi, RoundMain } from './main';
import { ChatCtrl } from 'chat';
import { tourStandingCtrl, TourStandingCtrl, TourPlayer } from './tourStanding';

const li = window.lichess;

export default function(opts: RoundOpts, element: HTMLElement): void {
  const data: RoundData = opts.data;
  let round: RoundApi, chat: ChatCtrl | undefined;
  if (data.tournament) $('body').data('tournament-id', data.tournament.id);
  li.socket = li.StrongSocket(
    data.url.socket,
    data.player.version, {
      options: { name: 'round' },
      params: { userTv: data.userTv && data.userTv.id },
      receive(t: string, d: any) { round.socketReceive(t, d); },
      events: {
        crowd(e: { watchers: number }) {
          $watchers.watchers("set", e.watchers);
        },
        tvSelect(o: any) {
          if (data.tv && data.tv.channel == o.channel) li.reload();
          else $('#tv_channels a.' + o.channel + ' span').html(
            o.player ? [
              o.player.title,
              o.player.name,
              '(' + o.player.rating + ')'
            ].filter(x => x).join('&nbsp') : 'Anonymous');
        },
        end() {
          $.ajax({
            url: [(data.tv ? '/tv/'  + data.tv.channel : ''), data.game.id, data.player.color, 'sides'].join('/'),
            success: function(html) {
              const $html = $(html);
              $('#site_header div.side').replaceWith($html.find('.side'));
              $('#lichess div.crosstable').replaceWith($html.find('.crosstable'));
              li.pubsub.emit('content_loaded')();
              startTournamentClock();
            }
          });
        },
        tourStanding(s: TourPlayer[]) {
          if (opts.chat && opts.chat.plugin && chat) {
            (opts.chat.plugin as TourStandingCtrl).set(s);
            chat.redraw();
          }
        }
      }
    });

  function startTournamentClock() {
    $("div.game_tournament div.clock").each(function(this: HTMLElement) {
      $(this).clock({
        time: parseFloat($(this).data("time"))
      });
    });
  };
  function getPresetGroup(d: RoundData) {
    if (d.player.spectator) return;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return;
  };
  opts.element = element.querySelector('.round') as HTMLElement;
  opts.socketSend = li.socket.send;
  if (!opts.tour && !data.simul) opts.onChange = (d: RoundData) => {
    if (chat) chat.preset.setGroup(getPresetGroup(d));
  };
  opts.crosstableEl = element.querySelector('.crosstable') as HTMLElement;

  let $watchers: JQuery;
  function letsGo() {
    round = (window['LichessRound'] as RoundMain).app(opts);
    if (opts.chat) {
      if (opts.tour) {
        opts.chat.plugin = tourStandingCtrl(opts.tour, opts.i18n.standing);
        opts.chat.alwaysEnabled = true;
      } else if (!data.simul) {
        opts.chat.preset = getPresetGroup(opts.data);
        opts.chat.parseMoves = true;
      }
      li.makeChat('chat', opts.chat, function(c) {
        chat = c;
      });
    }
    $watchers = $('#site_header div.watchers').watchers();
    startTournamentClock();
    $('#now_playing').find('.move_on input').change(function() {
      round.moveOn.toggle();
    }).prop('checked', round.moveOn.get()).on('click', 'a', function() {
      li.hasToReload = true;
      return true;
    });
    if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
      history.replaceState(null, '', '/' + data.game.id);
    if (!data.player.spectator && data.game.status.id < 25) li.topMenuIntent();
    $('#zentog').click(round.toggleZen);
  };
  if (li.isTrident) setTimeout(letsGo, 150);
  else letsGo();
}
