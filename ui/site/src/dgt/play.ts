export default function() {
  // here's the new setting keys
  [
    'dgt-livechess-url',
    'dgt-speech-keywords',
    'dgt-speech-synthesis',
    'dgt-speech-announce-all-moves',
    'dgt-speech-announce-move-format',
    'dgt-verbose'
  ].forEach(k => {
    console.log(k, localStorage.getItem(k));
  });

  // put your UI in there
  const root = document.getElementById('dgt-play-zone');

  // and your code in here.
}
