export function skip(txt: string) {
  return analyse(txt) && isSpammer.get() != '1';
}
export function hasTeamUrl(txt: string) {
  return !!txt.match(teamUrlRegex);
}
export function report(txt: string) {
  if (analyse(txt)) {
    $.post('/jslog/____________?n=spam');
    isSpammer.set(1);
  }
}

const isSpammer = window.lichess.storage.make('spammer');

const spamRegex = new RegExp([
  'xcamweb.com',
  'chess-bot',
  'coolteenbitch',
  'goo.gl/',
  'letcafa.webcam',
  'tinyurl.com/',
  'wooga.info/',
  'bit.ly/',
  'wbt.link/',
  'eb.by/',
  '001.rs/',
  'shr.name/',
  'u.to/',
].map(url => {
  return url.replace(/\./g, '\\.').replace(/\//g, '\\/');
}).join('|'));

function analyse(txt: string) {
  return !!txt.match(spamRegex);
}

const teamUrlRegex = /lichess\.org\/team\//
