import * as xhr from 'common/xhr';
import throttle from 'common/throttle';
import { Pool } from './interfaces';

export const seeks = throttle(2000, () => xhr.json('/lobby/seeks'));

export const nowPlaying = () => xhr.json('/account/now-playing').then(o => o.nowPlaying);

export const anonPoolSeek = (pool: Pool) =>
  xhr.json('/setup/hook/' + lichess.sri, {
    method: 'POST',
    body: xhr.form({
      variant: 1,
      timeMode: 1,
      time: pool.lim,
      increment: pool.inc,
      days: 1,
      color: 'random'
    })
  });
