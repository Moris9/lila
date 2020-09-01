$(function() {
  lichess.refreshInsightForm = function() {
    $('form.insight-refresh:not(.armed)').addClass('armed').submit(function() {
      lichess.modal($(this).find('.crunching'));
      $.post($(this).attr('action'), function() {
        lichess.reload();
      });
      return false;
    });
  };
  lichess.refreshInsightForm();
});
