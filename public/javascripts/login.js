$(function() {
  load($('form.login'));
});

function load($f) {
  $f.submit(function() {
    $f.find('.submit').attr('disabled', true);
    console.log({
        username: $f.find('#form3-username').val(),
        password: $f.find('#form3-password').val(),
        token: $f.find('#form3-token').val()
      });
    $.ajax({
      url: $f.attr('action'),
      method: $f.attr('method'),
      data: {
        username: $f.find('#form3-username').val(),
        password: $f.find('#form3-password').val(),
        token: $f.find('#form3-token').val()
      },
      success: function(res) {
        if (res === 'MissingTotpToken' || res === 'InvalidTotpToken') {
          $f.find('.one-factor').hide();
          $f.find('.two-factor').show();
          $f.find('.token input').val('');
          $f.find('.submit').attr('disabled', false);
          if (res === 'InvalidTotpToken') $f.find('.two-factor .error').show();
        }
        else lichess.redirect(res.indexOf('ok:') === 0 ? res.substr(3) : '/');
      },
      error: function(err) {
        $f.replaceWith($(err.responseText).find('form.login'));
        load($('form.login'));
      }
    });
    return false;
  });
}
