export function app(wrap: HTMLElement, toggle: () => void) {
  const $wrap = $(wrap), $input = $wrap.find('input');
  window.lichess.userAutocomplete($input, {
    focus: 1,
    friend: true,
    onSelect(q: any) {
      execute(q.name || q);
      $input.val('');
    }
  }).done(function() {
    $input.on('blur', toggle);
  });
}

function execute(q: string) {
  if (!q) return;
  if (q[0] === '/') command(q.slice(1));
  else location.href = '/@/' + q;
}

function command(q: string) {
  var parts = q.split(' '), exec = parts[0];
  if (exec === 'tv' || exec === 'follow') location.href = '/@/' + parts[1] + '/tv';
  else if (exec === 'play' || exec === 'challenge' || exec === 'match') location.href = '/?user=' + parts[1] + '#friend';
  else alert('Unknown command: ' + q);
}
