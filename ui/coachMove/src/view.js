var m = require('mithril');

var Slider = require('coach').slider;

module.exports = function(ctrl) {
  if (!ctrl.nbPeriods) return m('div.content_box_top', [
    m('h1', [
      ctrl.user.name,
      ' moves: No data available'
    ]),
  ]);
  return m('div', {
    config: function() {
      $('body').trigger('lichess.content_loaded');
    }
  }, [
    m('div.content_box_top', {
      class: 'content_box_top' + (ctrl.vm.loading ? ' loading' : '')
    }, [
      ctrl.nbPeriods > 1 ? m.component(Slider, {
        max: ctrl.nbPeriods,
        range: ctrl.vm.range,
        dates: ctrl.data ? [ctrl.data.from, ctrl.data.to] : null,
        onChange: ctrl.selectPeriodRange
      }) : null,
      m('h1', [
        ctrl.user.name,
        ' moves',
        ctrl.data ? m('div.over', [
          ' over ',
          ctrl.data.perfs[0].results.nbGames,
          ' games'
        ]) : null
      ]),
    ]),
    ctrl.vm.preloading ? m('div.loader') : (!ctrl.data ? m('div.top.nodata', m('p', 'Empty period range!')) : [
      inspect(ctrl),
      table(ctrl)
    ])
  ]);
};
