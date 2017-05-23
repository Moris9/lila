var tablesort = require('tablesort');

var $zone = $("div.user_show .mod_zone");

$zone.find('form.fide_title select').on('change', function() {
  $(this).parent('form').submit();
});

lichess.pubsub.emit('content_loaded')();

var $modLog = $zone.find('.mod_log ul').children();

if ($modLog.length > 20) {
  var list = $modLog.slice(20);
  list.addClass('modlog-hidden').hide()
    .first().before('<a id="modlog-show">Show all ' + $modLog.length + ' mod log entries...</a>');
    $zone.find('#modlog-show').click(function() {
      $zone.find('.modlog-hidden').show();
      $(this).remove();
    });
}

$zone.find('li.ip').slice(0, 2).each(function() {
  var $li = $(this);
  $(this).one('mouseover', function() {
    $.ajax({
      url: '/mod/ip-intel?ip=' + $(this).find('.address').text(),
      success: function(res) {
        $li.append($('<span class="intel">' + res + '% proxy</span>'));
      }
    });
  });
});

(function(){
  var cleanNumber = function(i) {
    return i.replace(/[^\-?0-9.]/g, '');
  },

  compareNumber = function(a, b) {
    a = parseFloat(a);
    b = parseFloat(b);

    a = isNaN(a) ? 0 : a;
    b = isNaN(b) ? 0 : b;

    return a - b;
  };

  tablesort.extend('number', function(item) {
    return item.match(/^-?[£\x24Û¢´€]?\d+\s*([,\.]\d{0,2})/) || // Prefixed currency
      item.match(/^-?\d+\s*([,\.]\d{0,2})?[£\x24Û¢´€]/) || // Suffixed currency
      item.match(/^-?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/); // Number
  }, function(a, b) {
    a = cleanNumber(a);
    b = cleanNumber(b);

    return compareNumber(b, a);
  });
}());

$zone.find('table.others').each(function() {
  tablesort(this);
});
