// Usage:
// mongo --eval 'mainDb="localhost/lichess";oauthDb="localhost/lichess";studyDb="localhost/lichess";puzzleDb="localhost/puzzler"' bin/mongodb/user-gdpr-scrub.js
// Run it as a cron script every 10m or so

mainDb = connect(mainDb);
oauthDb = connect(oauthDb);
studyDb = connect(studyDb);
puzzleDb = connect(puzzleDb);
allGames = typeof allGames == 'undefined' ? false : true; // set to true to scrub username from all games
allPms = typeof allPms == 'undefined' ? false : true; // set to true to erase all PM conversations

const user = mainDb.user4.findOne({ eraseAt: { $lt: new Date() } });
if (!user) quit();
if (user.enabled || user.erasedAt) {
  mainDb.update({ _id: user._id }, { $unset: { eraseAt: 1 } });
  quit();
}

print(`\n\n Delete user ${user.username} and all references to their username!\n\n`);

const userId = user._id;
const tosViolation = user.marks && user.marks.length;

const ghostId = 'ghost';
const newGhostId = () => {
  const idChars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const idLength = 8;
  let id = '';
  for (let i = idLength; i > 0; --i) id += idChars[Math.floor(Math.random() * idChars.length)];
  return `!${id}`;
};

const scrub = (collName, inDb) => f => {
  print(`- ${collName}`);
  sleep(200);
  return f((inDb || mainDb)[collName]);
};

const byId = doc => ({ _id: doc._id });
const deleteAllIn = (db, collName, field, value) => scrub(collName, db)(c => c.remove({ [field]: value || userId }));
const deleteAll = (collName, field, value) => deleteAllIn(mainDb, collName, field, value);
const replaceWithGhostId = (collName, field, inDb) =>
  scrub(collName, inDb)(c => c.updateMany({ [field]: userId }, { $set: { [field]: ghostId } }));

// Let us scrub.

deleteAll('activity', '_id', new RegExp(`^${userId}:`));

deleteAll('analysis_requester', '_id');

deleteAll('bookmark', 'u');

deleteAll('challenge', 'challenger.id');
deleteAll('challenge', 'destUser.id');

replaceWithGhostId('clas_clas', 'created.by');
scrub('clas_clas')(c => c.updateMany({ teachers: userId }, { $pull: { teachers: userId } }));

deleteAll('clas_student', 'userId');

deleteAll('coach', '_id');

deleteAll('coach_review', 'userId');
deleteAll('coach_review', 'coachId');

deleteAll('config', '_id');

deleteAll('coordinate_score', '_id');

if (allGames) deleteAll('crosstable2', '_id', new RegExp(`^${userId}/`));

scrub('f_post')(c => c.updateMany({ userId: userId }, { $set: { userId: ghostId, text: '', erasedAt: new Date() } }));

const deleteGamesByQuery = query => {
  const ids = scrub('game5')(c => c.distinct('_id', query));
  if (!ids.length) return;
  scrub('game5')(c => c.remove({ _id: { $in: ids } }));
  scrub('analysis2')(c => c.remove({ _id: { $in: ids } }));
  scrub('bookmark')(c => c.remove({ g: { $in: ids } }));
};

deleteGamesByQuery({ us: userId, $or: [{ 'p0.ai': { $exists: 1 } }, { 'p1.ai': { $exists: 1 } }] });
deleteGamesByQuery({ 'pgni.user': userId });

if (allGames)
  scrub('game5')(c =>
    c.find({ us: userId }, { us: 1, wid: 1 }).forEach(doc => {
      c.update(byId(doc), {
        $set: {
          [`us.${doc.us.indexOf(userId)}`]: ghostId, // replace in player usernames
        },
        ...(doc.wid == userId ? { $unset: { wid: 1 } } : {}), // remove winner username
      });
    })
  );

deleteAll('history3', '_id');

deleteAll('image', 'createdBy');

deleteAll('irwin_report', '_id');

deleteAll('learn_progress', '_id');

deleteAll('matchup', '_id', new RegExp(`^${userId}/`));

/*
We decided not to delete PMs out of legit interest of the correspondents
and also to be able to comply with data requests from law enforcement
*/
if (allPms) {
  const msgThreadIds = scrub('msg_thread')(c => {
    const ids = c.distinct('_id', { users: userId });
    c.remove({ users: userId });
    return ids;
  });
  scrub('msg_msg')(c => msgThreadIds.length && c.remove({ tid: { $in: msgThreadIds } }));
}

scrub('note')(c => {
  c.remove({ from: userId, mod: { $ne: true } });
  c.remove({ to: userId, mod: { $ne: true } });
});

deleteAll('notify', 'notifies');

replaceWithGhostId('oauth_client', 'author', oauthDb);

deleteAllIn(oauthDb, 'oauth_access_token', 'user_id');

deleteAll('perf_stat', '_id', new RegExp(`^${userId}/`));

replaceWithGhostId('plan_charge', 'userId');

deleteAll('plan_patron', '_id');

deleteAll('playban', '_id');

deleteAll('player_assessment', 'userId');

deleteAll('practice_progress', '_id');

deleteAll('pref', '_id');

deleteAll('push_device', 'userId');

scrub('puzzle2_puzzle', puzzleDb)(c => c.updateMany({ users: userId }, { $set: { 'users.$': ghostId } }));

deleteAllIn(puzzleDb, 'puzzle2_round', '_id', new RegExp(`^${userId}:`));

deleteAll('ranking', '_id', new RegExp(`^${userId}:`));

deleteAll('relation', 'u1');
deleteAll('relation', 'u2');

scrub('report2')(c => {
  c.find({ 'atoms.by': userId }, { atoms: 1 }).forEach(doc => {
    const newAtoms = doc.atoms.map(a => ({
      ...a,
      by: a.by == userId ? ghostId : a.by,
    }));
    c.update(byId(doc), { $set: { atoms: newAtoms } });
  });
  !tosViolation && c.updateMany({ user: userId }, { $set: { user: ghostId } });
});

if (!tosViolation) deleteAll('security', 'user');

deleteAll('seek', 'user.id');

deleteAll('shutup', '_id');

replaceWithGhostId('simul', 'hostId');
scrub('simul')(c => c.updateMany({ 'pairings.player.user': userId }, { $set: { 'pairings.$.player.user': ghostId } }));

deleteAll('storm_day', '_id', new RegExp(`^${userId}:`));

deleteAll('streamer', '_id');

const studyIds = scrub(
  'study',
  studyDb
)(c => {
  const ids = c.distinct('_id', { ownerId: userId });
  c.remove({ ownerId: userId });
  c.updateMany({ likers: userId }, { $pull: { likers: userId } });
  c.updateMany({ uids: userId }, { $pull: { uids: userId }, $unset: { [`members.${userId}`]: true } });
  return ids;
});

if (studyIds.length) scrub('study_chapter_flat', studyDb)(c => c.remove({ studyId: { $in: studyIds } }));

deleteAllIn(studyDb, 'study_user_topic', '_id');

replaceWithGhostId('swiss', 'winnerId');

const swissIds = scrub('swiss_player')(c => c.distinct('s', { u: userId }));

if (swissIds.length) {
  // here we use a single ghost ID for all swiss players and pairings,
  // because the mapping of swiss player to swiss pairings must be preserved
  const swissGhostId = newGhostId();
  scrub('swiss_player')(c => {
    c.find({ _id: { $in: swissIds.map(s => `${s}:${userId}`) } }).forEach(p => {
      c.remove({ _id: p._id });
      p._id = `${p.s}:${swissGhostId}`;
      p.u = swissGhostId;
      c.insert(p);
    });
  });
  scrub('swiss_pairing')(c => c.updateMany({ s: { $in: swissIds }, p: userId }, { $set: { 'p.$': swissGhostId } }));
}

replaceWithGhostId('team', 'createdBy');
scrub('team')(c => c.updateMany({ leaders: userId }, { $pull: { leaders: userId } }));

deleteAll('team_request', 'user');

deleteAll('team_member', 'user');

replaceWithGhostId('tournament2', 'createdBy');
replaceWithGhostId('tournament2', 'winnerId');

const arenaIds = scrub('tournament_leaderboard')(c => c.distinct('t', { u: userId }));
if (arenaIds.length) {
  // here we use a single ghost ID for all arena players and pairings,
  // because the mapping of arena player to arena pairings must be preserved
  const arenaGhostId = newGhostId();
  scrub('tournament_player')(c =>
    c.updateMany({ tid: { $in: arenaIds }, uid: userId }, { $set: { uid: arenaGhostId } })
  );
  scrub('tournament_pairing')(c =>
    c.updateMany({ tid: { $in: arenaIds }, u: userId }, { $set: { 'u.$': arenaGhostId } })
  );
  deleteAll('tournament_leaderboard', 'u');
}

deleteAll('trophy', 'user');

scrub('user4')(c =>
  c.update(
    { _id: userId },
    // If the user was banned for TOS violations, delete the following fields:
    tosViolation
      ? {
          $unset: {
            profile: 1,
            roles: 1,
            toints: 1,
            time: 1,
            kid: 1,
            lang: 1,
            title: 1,
            plan: 1,
            totp: 1,
            changedCase: 1,
            blind: 1,
            salt: 1,
            bpass: 1,
            mustConfirmEmail: 1,
            colorIt: 1,
          },
          $set: {
            erasedAt: new Date(),
          },
        }
      : // Else, delete everything from the user document, with the following exceptions:
        // - _id, as to prevent signing up with the same username again. Usernames must NOT be reused.
        // - prevEmail and createdAt, to prevent mass-creation of accounts reusing the same email address.
        {
          prevEmail: user.prevEmail,
          createdAt: user.createdAt,
          erasedAt: new Date(),
        }
  )
);

deleteAll('video_view', 'u');
