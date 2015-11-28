var m = require('mithril');

function dataTypeFormat(dt) {
  if (dt === 'seconds') return '{point.y:.1f}';
  if (dt === 'average') return '{point.y:,.1f}';
  if (dt === 'percent') return '{point.percentage:.0f}%';
  return '{point.y:,.0f}';
}

function yAxisTypeFormat(dt) {
  if (dt === 'seconds') return '{value:.1f}';
  if (dt === 'average') return '{value:,.1f}';
  if (dt === 'percent') return '{value:.0f}%';
  return '{value:,.0f}';
}

var colors = {
  green: '#759900',
  red: '#dc322f',
  orange: '#d59120',
  blue: '#007599'
};
var resultColors = {
  Victory: colors.green,
  Draw: colors.blue,
  Defeat: colors.red
};

function makeChart(el, data) {
  var sizeSerie = {
    name: data.sizeSerie.name,
    data: data.sizeSerie.data,
    yAxis: 1,
    type: 'column',
    stack: 'size',
    animation: {
      duration: 300
    },
    color: 'rgba(80,80,80,0.2)'
  };
  var valueSeries = data.series.map(function(s) {
    var c = {
      name: s.name,
      data: s.data,
      yAxis: 0,
      type: 'column',
      stack: s.stack,
      // animation: {
      //   duration: 300
      // },
      dataLabels: {
        enabled: true,
        format: dataTypeFormat(s.dataType)
      },
      tooltip: {
        // headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
        pointFormat: '<span style="color:{point.color}">\u25CF</span> {series.name}: <b>' + dataTypeFormat(s.dataType) + '</b><br/>',
        shared: true
      }
    };
    if (data.valueYaxis.name === 'Result') c.color = resultColors[s.name];
    return c;
  });
  var chartConf = {
    chart: {
      type: 'column',
      alignTicks: true,
      spacing: [20, 0, 20, 10],
      backgroundColor: null,
      borderWidth: 0,
      borderRadius: 0,
      plotBackgroundColor: null,
      plotShadow: false,
      plotBorderWidth: 0,
      style: {
        font: "12px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif"
      }
    },
    title: {
      text: null
    },
    xAxis: {
      categories: data.xAxis.categories,
      crosshair: true,
      labels: {
        rotation: -30
      }
    },
    yAxis: [data.valueYaxis, data.sizeYaxis].map(function(a, i) {
      return {
        title: {
          text: i === 1 ? a.name : false
        },
        labels: {
          format: yAxisTypeFormat(a.dataType)
        },
        opposite: i % 2 === 1,
        min: a.dataType === 'percent' ? 0 : undefined,
        max: a.dataType === 'percent' ? 100 : undefined
      };
    }),
    plotOptions: {
      column: {
        animation: {
          duration: 300
        },
        stacking: 'normal'
      }
    },
    series: valueSeries.concat(sizeSerie),
    credits: {
      enabled: false
    },
    legend: {
      enabled: true
    }
  };
  $(el).highcharts(chartConf);
}

module.exports = function(ctrl) {
  if (!ctrl.validCombinationCurrent()) return m('div', 'Invalid dimension/metric combination');
  if (!ctrl.vm.answer) return m('div.square-wrap', m('div.square-in', m('div.square-spin')));
  return m('div.chart', {
    config: function(el) {
      makeChart(el, ctrl.vm.answer);
    }
  })
};
