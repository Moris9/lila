$(function() {

  var $searchForm = $('form.search');

  if ($searchForm.length) {
    $searchInput = $searchForm.find('input.search_user');
    $searchInput.on('autocompleteselect', function(e, ui) {
      setTimeout(function() {
        $searchForm.submit();
      }, 10);
    });
    $searchForm.submit(function() {
      location.href = $searchForm.attr('action') + $searchInput.val();
      return false;
    });
  }

  $("div.user_show .mod_zone_toggle").each(function() {
    $(this).click(function() {
      var $zone = $("div.user_show .mod_zone");
      if ($zone.is(':visible')) $zone.hide();
      else $zone.html("Loading...").show().load($(this).attr("href"), function() {
        $(this).find('form.fide_title select').on('change', function() {
          $(this).parent('form').submit();
        });
        $('body').trigger('lichess.content_loaded');
        $(this).find('li.ip').slice(0, 3).each(function() {
          var $li = $(this);
          $.ajax({
            url: '/mod/ip-intel?ip=' + $(this).find('.address').text(),
            success: function(res) {
              var p = Math.round(parseFloat(res) * 100);
              $li.append($('<span class="intel">' + p + '% proxy</span>'));
            }
          });
        });
      });
      return false;
    });
    if (location.search.indexOf('mod') === 1) $(this).click();
  });

  $("div.user_show .note_zone_toggle").each(function() {
    $(this).click(function() {
      $("div.user_show .note_zone").toggle();
    });
    if (location.search.indexOf('note') != -1) $(this).click();
  });

  $('.buttonset').buttonset().disableSelection();

  $('form.autosubmit').each(function() {
    var $form = $(this);
    $form.find('input').change(function() {
      $.ajax({
        url: $form.attr('action'),
        method: $form.attr('method'),
        data: $form.serialize(),
        success: function() {
          $form.find('.saved').fadeIn();
        }
      });
    });
  });

  $("div.user_show .claim_title_zone").each(function() {
    var $zone = $(this);
    $zone.find('.actions a').click(function() {
      $.post($(this).attr('href'));
      $zone.remove();
      return false;
    });
  });
});

function str_repeat(input, multiplier) {
  return new Array(multiplier + 1).join(input);
}
