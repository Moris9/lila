// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// @externs_url http://closure-compiler.googlecode.com/svn/trunk/contrib/externs/jquery-2.0.js
// ==/ClosureCompiler==
//
$(function() {

  var disabled = {
    enabled: false
  };
  var noText = {
    text: null
  };
  var noAnimation = {
    animation: disabled
  };
  var theme = Highcharts.theme;
  var defaults = {
    title: {
      floating: true
    },
    yAxis: {
      title: noText
    },
    credits: disabled,
    legend: disabled
  };

  function mergeDefaults(config) {
    return $.extend(true, defaults, config);
  }

  $('div.elo_history').each(function() {
    var $this = $(this);
    var rows = $this.data('rows');
    var size = rows.date.length;

    function points(series) {
      var ps = [];
      for (var i = 0; i < size; i++) {
        ps.push({
          name: rows.date[i],
          y: rows[series][i]
        });
      }
      return ps;
    }
    var marker = {
      enabled: false,
      symbol: 'circle',
      states: {
        hover: {
          radius: 4
        }
      }
    };
    $(this).highcharts(mergeDefaults({
      chart: {},
      colors: theme.light ? ['#0000ff', theme.colors[3], '#909090'] : ['#4444ff', theme.colors[3], '#707070'],
      title: noText,
      xAxis: {
        labels: disabled,
        lineWidth: 0,
        tickWidth: 0
      },
      plotOptions: {
        line: {
          marker: marker
        },
        area: {
          marker: marker,
          lineWidth: 0,
          fillOpacity: 0.3
        }
      },
      series: [{
          name: 'Precise ELO',
          type: 'area',
          data: points('elo'),
          threshold: null
        }, {
          name: 'Average ELO',
          type: 'line',
          data: points('avg')
        }, {
          name: 'Opponent ELO',
          type: 'line',
          data: points('op')
        }
      ]
    }));
  });

  $('div.adv_chart').each(function() {
    var $this = $(this);
    var cpMax = parseInt($this.data('max'), 10) / 100;
    $(this).highcharts(mergeDefaults({
      series: [{
          name: 'Advantage',
          data: _.map($this.data('rows'), function(row) {
            row.y = row.y / 100;
            return row;
          })
        }
      ],
      chart: {
        type: 'area',
        margin: 2,
        spacing: [2, 2, 2, 2]
      },
      plotOptions: {
        area: {
          color: theme.colors[7],
          negativeColor: theme.colors[1],
          threshold: 0,
          lineWidth: 1,
          allowPointSelect: true,
          column: noAnimation,
          cursor: 'pointer',
          events: {
            click: function(event) {
              if (event.point) {
                event.point.select();
                GoToMove(event.point.x + 1);
              }
            }
          },
          marker: {
            radius: 2,
            enabled: true,
            states: {
              select: {
                radius: 4,
                lineColor: '#b57600',
                fillColor: '#ffffff'
              }
            }
          }
        }
      },
      title: {
        text: $this.attr('title'),
        align: 'left',
        y: 12
      },
      xAxis: {
        title: noText,
        labels: disabled,
        lineWidth: 0,
        tickWidth: 0
      },
      yAxis: {
        min: -cpMax,
        max: cpMax,
        labels: disabled,
        gridLineWidth: 0
      }
    }));
  });
});

Highcharts.theme = (function() {

  var light = document.body.className.indexOf('light') != -1;
  var text = {
    weak: light ? '#a0a0a0' : '#707070',
    strong: light ? '#707070' : '#a0a0a0'
  };
  var line = {
    weak: light ? '#ccc' : '#404040',
    strong: light ? '#a0a0a0' : '#606060'
  };

  function font(size) {
    return size + 'px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif';
  }

  return {
    light: light,
    colors: ["#DDDF0D", "#7798BF", "#55BF3B", "#DF5353", "#aaeeee", "#ff0066", "#eeaaee",
        "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"
    ],
    chart: {
      backgroundColor: null,
      borderWidth: 0,
      borderRadius: 0,
      plotBackgroundColor: null,
      plotShadow: false,
      plotBorderWidth: 0
    },
    title: {
      style: {
        font: font(13),
        color: text.strong
      }
    },
    xAxis: {
      gridLineWidth: 0,
      gridLineColor: line.weak,
      lineColor: line.strong,
      tickColor: line.strong,
      labels: {
        style: {
          color: text.weak,
          fontWeight: 'bold'
        }
      },
      title: {
        style: {
          color: text.weak,
          font: font(12)
        }
      }
    },
    yAxis: {
      alternateGridColor: null,
      minorTickInterval: null,
      gridLineColor: line.weak,
      minorGridLineColor: null,
      lineWidth: 0,
      tickWidth: 0,
      labels: {
        style: {
          color: text.weak,
          fontSize: '10px'
        }
      },
      title: {
        style: {
          color: text.weak,
          font: font(12)
        }
      }
    },
    legend: {
      itemStyle: {
        color: text.strong
      },
      itemHiddenStyle: {
        color: text.weak
      }
    },
    labels: {
      style: {
        color: text.strong
      }
    },
    tooltip: {
      backgroundColor: {
        linearGradient: {
          x1: 0,
          y1: 0,
          x2: 0,
          y2: 1
        },
        stops: light ? [
          [0, 'rgba(200, 200, 200, .8)'],
          [1, 'rgba(250, 250, 250, .8)']
        ] : [
          [0, 'rgba(56, 56, 56, .8)'],
          [1, 'rgba(16, 16, 16, .8)']
        ]
      },
      borderWidth: 0,
      style: {
        fontWeight: 'bold',
        color: text.strong
      }
    },
    plotOptions: {
      series: {
        shadow: false
      },
      line: {
        dataLabels: {
          color: text.strong
        },
        marker: {
          lineColor: text.weak
        }
      },
      spline: {
        marker: {
          lineColor: text.weak
        }
      },
      scatter: {
        marker: {
          lineColor: text.weak
        }
      },
      candlestick: {
        lineColor: text.strong
      }
    }
  };
})();
Highcharts.setOptions(Highcharts.theme);
