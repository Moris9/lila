import { ChartElm, loadHighcharts, MovePoint } from './common';
import divisionLines from './division';

// we share these with analyse/roundTraining but cannot export or import them
export const Colors = {
  inaccuracy: '#1c9ae3',
  blunder: '#db3d3d',
  mistake: '#cc9b00',
  white: '#ffffff',
  black: '#333333',
};

const acpl: Window['LichessChartGame']['acpl'] = async (data: any, mainline: any[], trans: Trans, el: ChartElm) => {
  await loadHighcharts('highchart');
  acpl.update = (d: any, mainline: any[]) =>
    el.highcharts && el.highcharts.series[0].setData(makeSerieData(d, mainline));

  const lightTheme = window.Highcharts.theme.light;
  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();

  const makeSerieData = (d: any, mainline: any[]) => {
    const partial = !d.analysis || d.analysis.partial;
    return mainline.slice(1).map(node => {
      const color = node.ply & 1;

      let cp;
      if (node.eval && node.eval.mate) {
        cp = node.eval.mate > 0 ? Infinity : -Infinity;
      } else if (node.san.includes('#')) {
        cp = color === 1 ? Infinity : -Infinity;
        if (d.game.variant.key === 'antichess') cp = -cp;
      } else if (node.eval && typeof node.eval.cp !== 'undefined') {
        cp = node.eval.cp;
      } else return { y: null };

      const turn = Math.floor((node.ply - 1) / 2) + 1;
      const dots = color === 1 ? '.' : '...';
      const point: MovePoint = {
        name: turn + dots + ' ' + node.san,
        y: 2 / (1 + Math.exp(-0.004 * cp)) - 1,
      };
      let [annotation, lineColor] = glyphProperties(node.glyphs);
      let fillColor = (color ? Colors.white : Colors.black) + '00';
      if (!partial && blurs[color].shift() === '1') {
        annotation = 'blur';
        lineColor = fillColor = 'rgba(42, 122, 225, 0.25)';
      }
      if (annotation) {
        point.marker = {
          symbol: annotation == 'blur' ? 'square' : 'circle',
          radius: 4,
          lineWidth: '3px',
          lineColor: lineColor,
          fillColor: fillColor,
        };
        point.name += ` [${annotation}]`;
      }
      return point;
    });
  };

  const disabled = { enabled: false };
  const noText = { text: null };
  const serieData = makeSerieData(data, mainline);
  el.highcharts = window.Highcharts.chart(el, {
    credits: disabled,
    legend: disabled,
    series: [
      {
        name: trans('advantage'),
        data: serieData,
      },
    ],
    chart: {
      type: 'area',
      spacing: [3, 0, 3, 0],
      animation: false,
    },
    plotOptions: {
      series: {
        animation: false,
      },
      area: {
        fillColor: window.Highcharts.theme.lichess.area.white,
        negativeFillColor: window.Highcharts.theme.lichess.area.black,
        threshold: 0,
        lineWidth: 1,
        color: '#d85000',
        cursor: 'pointer',
        states: {
          hover: {
            lineWidth: 1,
          },
        },
        events: {
          click(event: any) {
            if (event.point) {
              event.point.select(true);
              lichess.pubsub.emit('analysis.chart.click', event.point.x);
            }
          },
        },
        marker: {
          radius: 1,
          states: {
            hover: {
              radius: 4,
              lineColor: '#d85000',
            },
            select: {
              radius: 4,
              lineWidth: '3px',
              lineColor: `rgba(127, 127, 127, ${lightTheme ? '0.3' : '0.75'})`,
              fillColor: lightTheme ? '#888' : '#bbb',
            },
          },
        },
      },
    },
    tooltip: {
      pointFormatter: function (this: any, format: string) {
        format = format.replace('{series.name}', trans('advantage'));
        const ev = mainline[this.x + 1].eval;
        if (!ev) return;
        else if (ev.mate) return format.replace('{point.y}', '#' + ev.mate);
        else if (typeof ev.cp !== 'undefined') {
          const e = Math.max(Math.min(Math.round(ev.cp / 10) / 10, 99), -99);
          return format.replace('{point.y}', e > 0 ? `+${e}` : `${e}`);
        }
        return;
      },
    },
    title: noText,
    xAxis: {
      title: noText,
      labels: disabled,
      lineWidth: 0,
      tickWidth: 0,
      plotLines: divisionLines(data.game.division, trans),
    },
    yAxis: {
      title: noText,
      min: -1.1,
      max: 1.1,
      startOnTick: false,
      endOnTick: false,
      labels: disabled,
      lineWidth: 1,
      gridLineWidth: 0,
      plotLines: [
        {
          color: window.Highcharts.theme.lichess.text.weak,
          width: 1,
          value: 0,
        },
      ],
    },
  });
  el.highcharts.selectPly = (ply: number) => {
    const point = el.highcharts.series[0].data[ply - 1 - data.game.startedAtTurn];
    if (point) point.select(true);
    else el.highcharts.getSelectedPoints().forEach((point: any) => point.select(false));
  };
  lichess.pubsub.emit('analysis.change.trigger');
};

const glyphProperties = (glyphs: Array<Tree.Glyph> | undefined) => {
  if (glyphs?.some(g => g.id == 4)) return ['blunder', Colors.blunder + '00'];
  else if (glyphs?.some(g => g.id == 2)) return ['mistake', Colors.mistake + '00'];
  else if (glyphs?.some(g => g.id == 6)) return ['inaccuracy', Colors.inaccuracy + '00'];
  else return [undefined, undefined];
};

const toBlurArray = (player: any) => (player.blurs && player.blurs.bits ? player.blurs.bits.split('') : []);

export default acpl;
