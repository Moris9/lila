$(function() {

  var $searchForm = $('form.search_user_form');

  if ($searchForm.length) {
    $searchInput = $searchForm.find('input.search_user');
    $searchInput.on('autocompleteselect', function(e, ui) {
      setTimeout(function() {
        $searchForm.submit();
      }, 10);
    });
    $searchForm.submit(function() {
      location.href = $searchForm.attr('action') + '/' + $searchInput.val();
      return false;
    });
  }

  $("div.user_show .mod_zone_toggle").each(function() {
    $(this).click(function() {
      var $zone = $("div.user_show .mod_zone");
      if ($zone.is(':visible')) $zone.hide();
      else $zone.html("Loading...").show().load($(this).attr("href"), function() {
        $('body').trigger('lichess.content_loaded');
      });
      return false;
    });
    if (location.search.indexOf('mod') != -1) $(this).click();
  });

});

function str_repeat(input, multiplier) {
  return new Array(multiplier + 1).join(input);
}
