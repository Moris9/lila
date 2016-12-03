module.exports = function(element, cfg) {
  var lobby;
  var nbRoundSpread = $.spreadNumber(
    document.querySelector('#nb_games_in_play span'),
    8,
    function() {
      return lichess.socket.pingInterval();
    });
  var nbUserSpread = $.spreadNumber(
    document.querySelector('#nb_connected_players > strong'),
    10,
    function() {
      return lichess.socket.pingInterval();
    });
  var onFirstConnect = function() {
    var gameId = lichess.getParameterByName('hook_like');
    if (!gameId) return;
    $.post('/setup/hook/' + lichess.StrongSocket.sri + '/like/' + gameId);
    lobby.setTab('real_time');
    window.history.replaceState(null, null, '/');
  };
  lichess.socket = lichess.StrongSocket(
    '/lobby/socket/v1',
    cfg.data.version, {
      receive: function(t, d) {
        lobby.socketReceive(t, d);
      },
      events: {
        n: function(nbUsers, msg) {
          nbUserSpread(msg.d);
          setTimeout(function() {
            nbRoundSpread(msg.r);
          }, lichess.socket.pingInterval() / 2);
        },
        reload_timeline: function() {
          $.ajax({
            url: $("#timeline").data('href'),
            success: function(html) {
              $('#timeline').html(html);
              lichess.pubsub.emit('content_loaded')();
            }
          });
        },
        streams: function(html) {
          $('#streams_on_air').html(html);
        },
        featured: function(o) {
          $('#featured_game').html(o.html);
          lichess.pubsub.emit('content_loaded')();
        },
        redirect: function(e) {
          lobby.setRedirecting();
          $.redirect(e);
        },
        tournaments: function(data) {
          $("#enterable_tournaments").html(data);
          lichess.pubsub.emit('content_loaded')();
        },
        simuls: function(data) {
          $("#enterable_simuls").html(data).parent().toggle($('#enterable_simuls tr').length > 0);
          lichess.pubsub.emit('content_loaded')();
        },
        reload_forum: function() {
          var $newposts = $("div.new_posts");
          setTimeout(function() {
            $.ajax({
              url: $newposts.data('url'),
              success: function(data) {
                $newposts.find('ol').html(data).end().scrollTop(0);
                lichess.pubsub.emit('content_loaded')();
              }
            });
          }, Math.round(Math.random() * 5000));
        },
        fen: function(e) {
          lichess.StrongSocket.defaults.events.fen(e);
          lobby.gameActivity(e.id);
        }
      },
      options: {
        name: 'lobby',
        onFirstConnect: onFirstConnect
      }
    });

  cfg.socketSend = lichess.socket.send;
  lobby = LichessLobby.mithril(element, cfg);

  var $startButtons = $('#start_buttons');

  var sliderTimes = [
    0, 0.5, 0.75, 1, 1.5, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    16, 17, 18, 19, 20, 25, 30, 35, 40, 45, 60, 90, 120, 150, 180
  ];

  function sliderTime(v) {
    return v < sliderTimes.length ? sliderTimes[v] : 180;
  }

  function showTime(v) {
    if (v === 1 / 2) return '½';
    if (v === 3 / 4) return '¾';
    return v;
  }

  function sliderIncrement(v) {
    if (v <= 20) return v;
    switch (v) {
      case 21:
        return 25;
      case 22:
        return 30;
      case 23:
        return 35;
      case 24:
        return 40;
      case 25:
        return 45;
      case 26:
        return 60;
      case 27:
        return 90;
      case 28:
        return 120;
      case 29:
        return 150;
      default:
        return 180;
    }
  }

  function sliderDays(v) {
    if (v <= 3) return v;
    switch (v) {
      case 4:
        return 5;
      case 5:
        return 7;
      case 6:
        return 10;
      default:
        return 14;
    }
  }

  function sliderInitVal(v, f, max) {
    for (var i = 0; i < max; i++) {
      if (f(i) === v) return i;
    }
  }

  function hookToPoolMember(color, data, $ratings) {
    var find = function(key) {
      for (var i in data)
        if (data[i].name === key) return data[i].value;
    };
    var valid = color == 'random' &&
      find('variant') == 1 &&
      find('mode') == 1 &&
      find('timeMode') == 1;
    if (!valid) return false;
    var id = parseInt(find('time')) + '+' + parseInt(find('increment'));
    var exists = lichess.lobby.data.pools.filter(function(p) {
      return p.id === id;
    }).length;
    if (!exists) return;
    var rating = parseInt($ratings.find('strong:visible').text());
    var range = find('ratingRange').split('-');
    var ratingMin = parseInt(range[0]),
      ratingMax = parseInt(range[1]);
    var keepRange = (rating - ratingMin) < 200 || (ratingMax - rating) < 200;
    return {
      id: id,
      range: keepRange ? range.join('-') : null
    };
  }

  function prepareForm() {
    var $form = $('.lichess_overboard');
    var $timeModeSelect = $form.find('#timeMode');
    var $modeChoicesWrap = $form.find('.mode_choice');
    var $modeChoices = $modeChoicesWrap.find('input');
    var $casual = $modeChoices.eq(0),
      $rated = $modeChoices.eq(1);
    var $variantSelect = $form.find('#variant');
    var $fenPosition = $form.find(".fen_position");
    var $timeInput = $form.find('.time_choice input');
    var $incrementInput = $form.find('.increment_choice input');
    var $daysInput = $form.find('.days_choice input');
    var isHook = $form.hasClass('game_config_hook');
    var $ratings = $form.find('.ratings > div');
    var randomColorVariants = $form.data('random-color-variants').split(',');
    var toggleButtons = function() {
      var timeMode = $timeModeSelect.val();
      var rated = $rated.prop('checked');
      var timeOk = timeMode != '1' || $timeInput.val() > 0 || $incrementInput.val() > 0;
      var ratedOk = !isHook || !rated || timeMode != '0';
      if (timeOk && ratedOk) {
        $form.find('.color_submits button').toggleClass('nope', false);
        $form.find('.color_submits button:not(.random)').toggle(!rated || randomColorVariants.indexOf($variantSelect.val()) === -1);
      } else
        $form.find('.color_submits button').toggleClass('nope', true);
    };
    var showRating = function() {
      var timeMode = $timeModeSelect.val();
      var key;
      switch ($variantSelect.val()) {
        case '1':
          if (timeMode == '1') {
            var time = $timeInput.val() * 60 + $incrementInput.val() * 40;
            if (time < 180) key = 'bullet';
            else if (time < 480) key = 'blitz';
            else key = 'classical';
          } else key = 'correspondence';
          break;
        case '10':
          key = 'crazyhouse';
          break;
        case '2':
          key = 'chess960';
          break;
        case '4':
          key = 'kingOfTheHill';
          break;
        case '5':
          key = 'threeCheck';
          break;
        case '6':
          key = 'antichess'
          break;
        case '7':
          key = 'atomic'
          break;
        case '8':
          key = "horde"
          break;
        case '9':
          key = "racingKings"
          break;
      }
      $ratings.hide().filter('.' + key).show();
    };
    if (isHook) {
      var $formTag = $form.find('form');
      if ($form.data('anon')) {
        $timeModeSelect.val(1)
          .children('.timeMode_2, .timeMode_0')
          .prop('disabled', true)
          .attr('title', $.trans('You need an account to do that'));
      }
      var ajaxSubmit = function(color) {
        var poolMember = hookToPoolMember(color, $formTag.serializeArray(), $ratings);
        $form.find('a.close').click();
        if (poolMember) {
          lobby.enterPool(poolMember);
          return false;
        }
        $.ajax({
          url: $formTag.attr('action').replace(/uid-placeholder/, lichess.StrongSocket.sri),
          data: $formTag.serialize() + "&color=" + color,
          type: 'post'
        });
        lobby.setTab($timeModeSelect.val() === '1' ? 'real_time' : 'seeks');
        return false;
      };
      $formTag.find('.color_submits button').click(function() {
        return ajaxSubmit($(this).val());
      }).attr('disabled', false);
      $formTag.submit(function() {
        return ajaxSubmit('random');
      });
    } else
      $form.find('form').one('submit', function() {
        $(this).find('.color_submits').find('button').hide().end().append(lichess.spinnerHtml);
      });
    lichess.slider().done(function() {
      $timeInput.add($incrementInput).each(function() {
        var $input = $(this),
          $value = $input.siblings('span');
        var isTimeSlider = $input.parent().hasClass('time_choice');
        $input.hide().after($('<div>').slider({
          value: sliderInitVal(parseFloat($input.val()), isTimeSlider ? sliderTime : sliderIncrement, 100),
          min: 0,
          max: isTimeSlider ? 33 : 30,
          range: 'min',
          step: 1,
          slide: function(event, ui) {
            var time = (isTimeSlider ? sliderTime : sliderIncrement)(ui.value);
            $value.text(isTimeSlider ? showTime(time) : time);
            $input.attr('value', time);
            showRating();
            toggleButtons();
          }
        }));
      });
      $daysInput.each(function() {
        var $input = $(this),
          $value = $input.siblings('span');
        $input.hide().after($('<div>').slider({
          value: sliderInitVal(parseInt($input.val()), sliderDays, 20),
          min: 1,
          max: 7,
          range: 'min',
          step: 1,
          slide: function(event, ui) {
            var days = sliderDays(ui.value);
            $value.text(days);
            $input.attr('value', days);
          }
        }));
      });
      $form.find('.rating_range').each(function() {
        var $this = $(this);
        var $input = $this.find("input");
        var $span = $this.siblings("span.range");
        var min = $input.data("min");
        var max = $input.data("max");
        var values = $input.val() ? $input.val().split("-") : [min, max];

        $span.text(values.join(' - '));
        $this.slider({
          range: true,
          min: min,
          max: max,
          values: values,
          step: 50,
          slide: function(event, ui) {
            $input.val(ui.values[0] + "-" + ui.values[1]);
            $span.text(ui.values[0] + " - " + ui.values[1]);
          }
        });
      });
    });
    $modeChoices.add($form.find('.members_only input')).on('change', function() {
      var rated = $rated.prop('checked');
      var membersOnly = $form.find('.members_only input').prop('checked');
      $form.find('.rating_range_config').toggle(rated || membersOnly);
      $form.find('.members_only').toggle(!rated);
      toggleButtons();
    }).trigger('change');
    $timeModeSelect.on('change', function() {
      var timeMode = $(this).val();
      $form.find('.time_choice, .increment_choice').toggle(timeMode == '1');
      $form.find('.days_choice').toggle(timeMode == '2');
      toggleButtons();
      showRating();
    }).trigger('change');

    var $fenInput = $fenPosition.find('input');
    var validateFen = $.fp.debounce(function() {
      $fenInput.removeClass("success failure");
      var fen = $fenInput.val();
      if (fen) {
        $.ajax({
          url: $fenInput.parent().data('validate-url'),
          data: {
            fen: fen
          },
          success: function(data) {
            $fenInput.addClass("success");
            $fenPosition.find('.preview').html(data);
            $fenPosition.find('a.board_editor').each(function() {
              $(this).attr('href', $(this).attr('href').replace(/editor\/.+$/, "editor/" + fen));
            });
            $form.find('.color_submits button').removeClass('nope');
            lichess.pubsub.emit('content_loaded')();
          },
          error: function() {
            $fenInput.addClass("failure");
            $fenPosition.find('.preview').html("");
            $form.find('.color_submits button').addClass('nope');
          }
        });
      }
    }, 200);
    $fenInput.on('keyup', validateFen);

    $variantSelect.on('change', function() {
      var fen = $(this).val() == '3';
      $fenPosition.toggle(fen);
      $modeChoicesWrap.toggle(!fen);
      if (fen) {
        $casual.click();
        document.body.dispatchEvent(new Event('chessground.resize'));
      }
      showRating();
      toggleButtons();
    }).trigger('change');

    $form.find('div.level').each(function() {
      var $infos = $(this).find('.ai_info > div');
      $(this).find('label').mouseenter(function() {
        $infos.hide().filter('.' + $(this).attr('for')).show();
      });
      $(this).find('#config_level').mouseleave(function() {
        var level = $(this).find('input:checked').val();
        $infos.hide().filter('.level_' + level).show();
      }).trigger('mouseout');
    });

    $form.find('a.close.icon').click(function() {
      $form.remove();
      $startButtons.find('a.active').removeClass('active');
      return false;
    });
  }

  $startButtons.find('a').not('.disabled').on('mousedown', function() {
    $.ajax({
      url: $(this).attr('href'),
      success: function(html) {
        $('.lichess_overboard').remove();
        $('#hooks_wrap').prepend(html);
        prepareForm();
        lichess.pubsub.emit('content_loaded')();
      },
      error: function() {
        lichess.reload();
      }
    });
    $(this).addClass('active').siblings().removeClass('active');
    $('.lichess_overboard').remove();
    return false;
  });

  if (['#ai', '#friend', '#hook'].indexOf(location.hash) !== -1) {
    $startButtons
      .find('a.config_' + location.hash.replace('#', ''))
      .each(function() {
        $(this).attr("href", $(this).attr("href") + location.search);
      }).trigger('mousedown');

    if (location.hash === '#hook') {
      if (/time=realTime/.test(location.search))
        lobby.setTab('real_time');
      else if (/time=correspondence/.test(location.search))
        lobby.setTab('seeks');
    }

    window.history.replaceState(null, null, '/');
  }
};
