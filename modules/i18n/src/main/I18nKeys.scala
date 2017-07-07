// Generated with bin/trans-dump.js
package lila.i18n

import I18nDb.{ Site, Arena }

// format: OFF
object I18nKeys {

def untranslated(message: String) = new Untranslated(message)

val `playWithAFriend` = new Translated("playWithAFriend", Site)
val `playWithTheMachine` = new Translated("playWithTheMachine", Site)
val `toInviteSomeoneToPlayGiveThisUrl` = new Translated("toInviteSomeoneToPlayGiveThisUrl", Site)
val `gameOver` = new Translated("gameOver", Site)
val `waitingForOpponent` = new Translated("waitingForOpponent", Site)
val `waiting` = new Translated("waiting", Site)
val `yourTurn` = new Translated("yourTurn", Site)
val `aiNameLevelAiLevel` = new Translated("aiNameLevelAiLevel", Site)
val `level` = new Translated("level", Site)
val `toggleTheChat` = new Translated("toggleTheChat", Site)
val `toggleSound` = new Translated("toggleSound", Site)
val `chat` = new Translated("chat", Site)
val `resign` = new Translated("resign", Site)
val `checkmate` = new Translated("checkmate", Site)
val `stalemate` = new Translated("stalemate", Site)
val `white` = new Translated("white", Site)
val `black` = new Translated("black", Site)
val `randomColor` = new Translated("randomColor", Site)
val `createAGame` = new Translated("createAGame", Site)
val `whiteIsVictorious` = new Translated("whiteIsVictorious", Site)
val `blackIsVictorious` = new Translated("blackIsVictorious", Site)
val `kingInTheCenter` = new Translated("kingInTheCenter", Site)
val `threeChecks` = new Translated("threeChecks", Site)
val `raceFinished` = new Translated("raceFinished", Site)
val `variantEnding` = new Translated("variantEnding", Site)
val `newOpponent` = new Translated("newOpponent", Site)
val `yourOpponentWantsToPlayANewGameWithYou` = new Translated("yourOpponentWantsToPlayANewGameWithYou", Site)
val `joinTheGame` = new Translated("joinTheGame", Site)
val `whitePlays` = new Translated("whitePlays", Site)
val `blackPlays` = new Translated("blackPlays", Site)
val `opponentLeftChoices` = new Translated("opponentLeftChoices", Site)
val `makeYourOpponentResign` = new Translated("makeYourOpponentResign", Site)
val `forceResignation` = new Translated("forceResignation", Site)
val `forceDraw` = new Translated("forceDraw", Site)
val `talkInChat` = new Translated("talkInChat", Site)
val `theFirstPersonToComeOnThisUrlWillPlayWithYou` = new Translated("theFirstPersonToComeOnThisUrlWillPlayWithYou", Site)
val `whiteResigned` = new Translated("whiteResigned", Site)
val `blackResigned` = new Translated("blackResigned", Site)
val `whiteLeftTheGame` = new Translated("whiteLeftTheGame", Site)
val `blackLeftTheGame` = new Translated("blackLeftTheGame", Site)
val `shareThisUrlToLetSpectatorsSeeTheGame` = new Translated("shareThisUrlToLetSpectatorsSeeTheGame", Site)
val `theComputerAnalysisHasFailed` = new Translated("theComputerAnalysisHasFailed", Site)
val `viewTheComputerAnalysis` = new Translated("viewTheComputerAnalysis", Site)
val `requestAComputerAnalysis` = new Translated("requestAComputerAnalysis", Site)
val `computerAnalysis` = new Translated("computerAnalysis", Site)
val `computerAnalysisAvailable` = new Translated("computerAnalysisAvailable", Site)
val `analysis` = new Translated("analysis", Site)
val `depthX` = new Translated("depthX", Site)
val `usingServerAnalysis` = new Translated("usingServerAnalysis", Site)
val `loadingEngine` = new Translated("loadingEngine", Site)
val `cloudAnalysis` = new Translated("cloudAnalysis", Site)
val `cloud` = new Translated("cloud", Site)
val `goDeeper` = new Translated("goDeeper", Site)
val `showThreat` = new Translated("showThreat", Site)
val `inLocalBrowser` = new Translated("inLocalBrowser", Site)
val `toggleLocalEvaluation` = new Translated("toggleLocalEvaluation", Site)
val `move` = new Translated("move", Site)
val `variantLoss` = new Translated("variantLoss", Site)
val `variantWin` = new Translated("variantWin", Site)
val `insufficientMaterial` = new Translated("insufficientMaterial", Site)
val `pawnMove` = new Translated("pawnMove", Site)
val `capture` = new Translated("capture", Site)
val `close` = new Translated("close", Site)
val `winning` = new Translated("winning", Site)
val `losing` = new Translated("losing", Site)
val `drawn` = new Translated("drawn", Site)
val `unknown` = new Translated("unknown", Site)
val `database` = new Translated("database", Site)
val `gameSpeed` = new Translated("gameSpeed", Site)
val `antichessWin` = new Translated("antichessWin", Site)
val `whiteDrawBlack` = new Translated("whiteDrawBlack", Site)
val `averageRatingX` = new Translated("averageRatingX", Site)
val `watkinsAntichessSolution` = new Translated("watkinsAntichessSolution", Site)
val `watkinsAntichessSolutionExplanation` = new Translated("watkinsAntichessSolutionExplanation", Site)
val `masterDbExplanation` = new Translated("masterDbExplanation", Site)
val `proofTreeSize` = new Translated("proofTreeSize", Site)
val `noGameFound` = new Translated("noGameFound", Site)
val `alreadySearchingThroughAllAvailableGames` = new Translated("alreadySearchingThroughAllAvailableGames", Site)
val `maybeIncludeMoreGamesFromThePreferencesMenu` = new Translated("maybeIncludeMoreGamesFromThePreferencesMenu", Site)
val `openingExplorer` = new Translated("openingExplorer", Site)
val `xOpeningExplorer` = new Translated("xOpeningExplorer", Site)
val `winPreventedBy50MoveRule` = new Translated("winPreventedBy50MoveRule", Site)
val `lossSavedBy50MoveRule` = new Translated("lossSavedBy50MoveRule", Site)
val `allSet` = new Translated("allSet", Site)
val `blunders` = new Translated("blunders", Site)
val `mistakes` = new Translated("mistakes", Site)
val `inaccuracies` = new Translated("inaccuracies", Site)
val `moveTimes` = new Translated("moveTimes", Site)
val `flipBoard` = new Translated("flipBoard", Site)
val `threefoldRepetition` = new Translated("threefoldRepetition", Site)
val `claimADraw` = new Translated("claimADraw", Site)
val `offerDraw` = new Translated("offerDraw", Site)
val `draw` = new Translated("draw", Site)
val `currentGames` = new Translated("currentGames", Site)
val `viewInFullSize` = new Translated("viewInFullSize", Site)
val `logOut` = new Translated("logOut", Site)
val `signIn` = new Translated("signIn", Site)
val `newToLichess` = new Translated("newToLichess", Site)
val `youNeedAnAccountToDoThat` = new Translated("youNeedAnAccountToDoThat", Site)
val `signUp` = new Translated("signUp", Site)
val `computersAreNotAllowedToPlay` = new Translated("computersAreNotAllowedToPlay", Site)
val `games` = new Translated("games", Site)
val `forum` = new Translated("forum", Site)
val `xPostedInForumY` = new Translated("xPostedInForumY", Site)
val `latestForumPosts` = new Translated("latestForumPosts", Site)
val `players` = new Translated("players", Site)
val `minutesPerSide` = new Translated("minutesPerSide", Site)
val `variant` = new Translated("variant", Site)
val `variants` = new Translated("variants", Site)
val `timeControl` = new Translated("timeControl", Site)
val `realTime` = new Translated("realTime", Site)
val `correspondence` = new Translated("correspondence", Site)
val `daysPerTurn` = new Translated("daysPerTurn", Site)
val `oneDay` = new Translated("oneDay", Site)
val `time` = new Translated("time", Site)
val `rating` = new Translated("rating", Site)
val `ratingStats` = new Translated("ratingStats", Site)
val `username` = new Translated("username", Site)
val `usernameOrEmail` = new Translated("usernameOrEmail", Site)
val `password` = new Translated("password", Site)
val `haveAnAccount` = new Translated("haveAnAccount", Site)
val `changePassword` = new Translated("changePassword", Site)
val `changeEmail` = new Translated("changeEmail", Site)
val `email` = new Translated("email", Site)
val `emailIsOptional` = new Translated("emailIsOptional", Site)
val `passwordReset` = new Translated("passwordReset", Site)
val `forgotPassword` = new Translated("forgotPassword", Site)
val `rank` = new Translated("rank", Site)
val `gamesPlayed` = new Translated("gamesPlayed", Site)
val `cancel` = new Translated("cancel", Site)
val `timeOut` = new Translated("timeOut", Site)
val `drawOfferSent` = new Translated("drawOfferSent", Site)
val `drawOfferDeclined` = new Translated("drawOfferDeclined", Site)
val `drawOfferAccepted` = new Translated("drawOfferAccepted", Site)
val `drawOfferCanceled` = new Translated("drawOfferCanceled", Site)
val `whiteOffersDraw` = new Translated("whiteOffersDraw", Site)
val `blackOffersDraw` = new Translated("blackOffersDraw", Site)
val `whiteDeclinesDraw` = new Translated("whiteDeclinesDraw", Site)
val `blackDeclinesDraw` = new Translated("blackDeclinesDraw", Site)
val `yourOpponentOffersADraw` = new Translated("yourOpponentOffersADraw", Site)
val `accept` = new Translated("accept", Site)
val `decline` = new Translated("decline", Site)
val `playingRightNow` = new Translated("playingRightNow", Site)
val `finished` = new Translated("finished", Site)
val `abortGame` = new Translated("abortGame", Site)
val `gameAborted` = new Translated("gameAborted", Site)
val `standard` = new Translated("standard", Site)
val `unlimited` = new Translated("unlimited", Site)
val `mode` = new Translated("mode", Site)
val `casual` = new Translated("casual", Site)
val `rated` = new Translated("rated", Site)
val `thisGameIsRated` = new Translated("thisGameIsRated", Site)
val `rematch` = new Translated("rematch", Site)
val `rematchOfferSent` = new Translated("rematchOfferSent", Site)
val `rematchOfferAccepted` = new Translated("rematchOfferAccepted", Site)
val `rematchOfferCanceled` = new Translated("rematchOfferCanceled", Site)
val `rematchOfferDeclined` = new Translated("rematchOfferDeclined", Site)
val `cancelRematchOffer` = new Translated("cancelRematchOffer", Site)
val `viewRematch` = new Translated("viewRematch", Site)
val `play` = new Translated("play", Site)
val `inbox` = new Translated("inbox", Site)
val `chatRoom` = new Translated("chatRoom", Site)
val `loginToChat` = new Translated("loginToChat", Site)
val `youHaveBeenTimedOut` = new Translated("youHaveBeenTimedOut", Site)
val `spectatorRoom` = new Translated("spectatorRoom", Site)
val `composeMessage` = new Translated("composeMessage", Site)
val `noNewMessages` = new Translated("noNewMessages", Site)
val `subject` = new Translated("subject", Site)
val `recipient` = new Translated("recipient", Site)
val `send` = new Translated("send", Site)
val `incrementInSeconds` = new Translated("incrementInSeconds", Site)
val `freeOnlineChess` = new Translated("freeOnlineChess", Site)
val `spectators` = new Translated("spectators", Site)
val `exportGames` = new Translated("exportGames", Site)
val `ratingRange` = new Translated("ratingRange", Site)
val `thisPlayerUsesChessComputerAssistance` = new Translated("thisPlayerUsesChessComputerAssistance", Site)
val `thisPlayerArtificiallyIncreasesTheirRating` = new Translated("thisPlayerArtificiallyIncreasesTheirRating", Site)
val `openingExplorerAndTablebase` = new Translated("openingExplorerAndTablebase", Site)
val `takeback` = new Translated("takeback", Site)
val `proposeATakeback` = new Translated("proposeATakeback", Site)
val `takebackPropositionSent` = new Translated("takebackPropositionSent", Site)
val `takebackPropositionDeclined` = new Translated("takebackPropositionDeclined", Site)
val `takebackPropositionAccepted` = new Translated("takebackPropositionAccepted", Site)
val `takebackPropositionCanceled` = new Translated("takebackPropositionCanceled", Site)
val `yourOpponentProposesATakeback` = new Translated("yourOpponentProposesATakeback", Site)
val `bookmarkThisGame` = new Translated("bookmarkThisGame", Site)
val `search` = new Translated("search", Site)
val `advancedSearch` = new Translated("advancedSearch", Site)
val `tournament` = new Translated("tournament", Site)
val `tournaments` = new Translated("tournaments", Site)
val `tournamentPoints` = new Translated("tournamentPoints", Site)
val `viewTournament` = new Translated("viewTournament", Site)
val `backToTournament` = new Translated("backToTournament", Site)
val `backToGame` = new Translated("backToGame", Site)
val `siteDescription` = new Translated("siteDescription", Site)
val `teams` = new Translated("teams", Site)
val `allTeams` = new Translated("allTeams", Site)
val `newTeam` = new Translated("newTeam", Site)
val `myTeams` = new Translated("myTeams", Site)
val `noTeamFound` = new Translated("noTeamFound", Site)
val `joinTeam` = new Translated("joinTeam", Site)
val `quitTeam` = new Translated("quitTeam", Site)
val `anyoneCanJoin` = new Translated("anyoneCanJoin", Site)
val `aConfirmationIsRequiredToJoin` = new Translated("aConfirmationIsRequiredToJoin", Site)
val `joiningPolicy` = new Translated("joiningPolicy", Site)
val `teamLeader` = new Translated("teamLeader", Site)
val `teamBestPlayers` = new Translated("teamBestPlayers", Site)
val `teamRecentMembers` = new Translated("teamRecentMembers", Site)
val `xJoinedTeamY` = new Translated("xJoinedTeamY", Site)
val `xCreatedTeamY` = new Translated("xCreatedTeamY", Site)
val `averageElo` = new Translated("averageElo", Site)
val `location` = new Translated("location", Site)
val `settings` = new Translated("settings", Site)
val `filterGames` = new Translated("filterGames", Site)
val `reset` = new Translated("reset", Site)
val `apply` = new Translated("apply", Site)
val `leaderboard` = new Translated("leaderboard", Site)
val `pasteTheFenStringHere` = new Translated("pasteTheFenStringHere", Site)
val `pasteThePgnStringHere` = new Translated("pasteThePgnStringHere", Site)
val `fromPosition` = new Translated("fromPosition", Site)
val `continueFromHere` = new Translated("continueFromHere", Site)
val `importGame` = new Translated("importGame", Site)
val `thisIsAChessCaptcha` = new Translated("thisIsAChessCaptcha", Site)
val `clickOnTheBoardToMakeYourMove` = new Translated("clickOnTheBoardToMakeYourMove", Site)
val `captcha.fail` = new Translated("captcha.fail", Site)
val `notACheckmate` = new Translated("notACheckmate", Site)
val `colorPlaysCheckmateInOne` = new Translated("colorPlaysCheckmateInOne", Site)
val `retry` = new Translated("retry", Site)
val `reconnecting` = new Translated("reconnecting", Site)
val `onlineFriends` = new Translated("onlineFriends", Site)
val `noFriendsOnline` = new Translated("noFriendsOnline", Site)
val `findFriends` = new Translated("findFriends", Site)
val `favoriteOpponents` = new Translated("favoriteOpponents", Site)
val `follow` = new Translated("follow", Site)
val `following` = new Translated("following", Site)
val `unfollow` = new Translated("unfollow", Site)
val `block` = new Translated("block", Site)
val `blocked` = new Translated("blocked", Site)
val `unblock` = new Translated("unblock", Site)
val `followsYou` = new Translated("followsYou", Site)
val `xStartedFollowingY` = new Translated("xStartedFollowingY", Site)
val `more` = new Translated("more", Site)
val `memberSince` = new Translated("memberSince", Site)
val `lastSeenActive` = new Translated("lastSeenActive", Site)
val `challengeToPlay` = new Translated("challengeToPlay", Site)
val `player` = new Translated("player", Site)
val `list` = new Translated("list", Site)
val `graph` = new Translated("graph", Site)
val `required` = new Translated("required", Site)
val `openTournaments` = new Translated("openTournaments", Site)
val `duration` = new Translated("duration", Site)
val `winner` = new Translated("winner", Site)
val `standing` = new Translated("standing", Site)
val `createANewTournament` = new Translated("createANewTournament", Site)
val `join` = new Translated("join", Site)
val `withdraw` = new Translated("withdraw", Site)
val `points` = new Translated("points", Site)
val `wins` = new Translated("wins", Site)
val `losses` = new Translated("losses", Site)
val `winStreak` = new Translated("winStreak", Site)
val `createdBy` = new Translated("createdBy", Site)
val `tournamentIsStarting` = new Translated("tournamentIsStarting", Site)
val `tournamentPairingsAreNowClosed` = new Translated("tournamentPairingsAreNowClosed", Site)
val `standByX` = new Translated("standByX", Site)
val `youArePlaying` = new Translated("youArePlaying", Site)
val `winRate` = new Translated("winRate", Site)
val `berserkRate` = new Translated("berserkRate", Site)
val `performance` = new Translated("performance", Site)
val `tournamentComplete` = new Translated("tournamentComplete", Site)
val `movesPlayed` = new Translated("movesPlayed", Site)
val `whiteWins` = new Translated("whiteWins", Site)
val `blackWins` = new Translated("blackWins", Site)
val `draws` = new Translated("draws", Site)
val `nextXTournament` = new Translated("nextXTournament", Site)
val `viewMoreTournaments` = new Translated("viewMoreTournaments", Site)
val `averageOpponent` = new Translated("averageOpponent", Site)
val `membersOnly` = new Translated("membersOnly", Site)
val `boardEditor` = new Translated("boardEditor", Site)
val `startPosition` = new Translated("startPosition", Site)
val `clearBoard` = new Translated("clearBoard", Site)
val `savePosition` = new Translated("savePosition", Site)
val `loadPosition` = new Translated("loadPosition", Site)
val `isPrivate` = new Translated("isPrivate", Site)
val `reportXToModerators` = new Translated("reportXToModerators", Site)
val `profileCompletion` = new Translated("profileCompletion", Site)
val `xRating` = new Translated("xRating", Site)
val `ifNoneLeaveEmpty` = new Translated("ifNoneLeaveEmpty", Site)
val `gameCompletionRate` = new Translated("gameCompletionRate", Site)
val `profile` = new Translated("profile", Site)
val `editProfile` = new Translated("editProfile", Site)
val `firstName` = new Translated("firstName", Site)
val `lastName` = new Translated("lastName", Site)
val `biography` = new Translated("biography", Site)
val `country` = new Translated("country", Site)
val `preferences` = new Translated("preferences", Site)
val `watchLichessTV` = new Translated("watchLichessTV", Site)
val `previouslyOnLichessTV` = new Translated("previouslyOnLichessTV", Site)
val `onlinePlayers` = new Translated("onlinePlayers", Site)
val `activeToday` = new Translated("activeToday", Site)
val `activePlayers` = new Translated("activePlayers", Site)
val `bewareTheGameIsRatedButHasNoClock` = new Translated("bewareTheGameIsRatedButHasNoClock", Site)
val `training` = new Translated("training", Site)
val `yourPuzzleRatingX` = new Translated("yourPuzzleRatingX", Site)
val `findTheBestMoveForWhite` = new Translated("findTheBestMoveForWhite", Site)
val `findTheBestMoveForBlack` = new Translated("findTheBestMoveForBlack", Site)
val `toTrackYourProgress` = new Translated("toTrackYourProgress", Site)
val `trainingSignupExplanation` = new Translated("trainingSignupExplanation", Site)
val `puzzleId` = new Translated("puzzleId", Site)
val `puzzleOfTheDay` = new Translated("puzzleOfTheDay", Site)
val `clickToSolve` = new Translated("clickToSolve", Site)
val `goodMove` = new Translated("goodMove", Site)
val `butYouCanDoBetter` = new Translated("butYouCanDoBetter", Site)
val `bestMove` = new Translated("bestMove", Site)
val `keepGoing` = new Translated("keepGoing", Site)
val `puzzleFailed` = new Translated("puzzleFailed", Site)
val `butYouCanKeepTrying` = new Translated("butYouCanKeepTrying", Site)
val `victory` = new Translated("victory", Site)
val `wasThisPuzzleAnyGood` = new Translated("wasThisPuzzleAnyGood", Site)
val `pleaseVotePuzzle` = new Translated("pleaseVotePuzzle", Site)
val `thankYou` = new Translated("thankYou", Site)
val `ratingX` = new Translated("ratingX", Site)
val `fromGameLink` = new Translated("fromGameLink", Site)
val `startTraining` = new Translated("startTraining", Site)
val `continueTraining` = new Translated("continueTraining", Site)
val `retryThisPuzzle` = new Translated("retryThisPuzzle", Site)
val `thisPuzzleIsCorrect` = new Translated("thisPuzzleIsCorrect", Site)
val `thisPuzzleIsWrong` = new Translated("thisPuzzleIsWrong", Site)
val `automaticallyProceedToNextGameAfterMoving` = new Translated("automaticallyProceedToNextGameAfterMoving", Site)
val `autoSwitch` = new Translated("autoSwitch", Site)
val `puzzles` = new Translated("puzzles", Site)
val `coordinates` = new Translated("coordinates", Site)
val `latestUpdates` = new Translated("latestUpdates", Site)
val `tournamentWinners` = new Translated("tournamentWinners", Site)
val `name` = new Translated("name", Site)
val `description` = new Translated("description", Site)
val `no` = new Translated("no", Site)
val `yes` = new Translated("yes", Site)
val `help` = new Translated("help", Site)
val `createANewTopic` = new Translated("createANewTopic", Site)
val `topics` = new Translated("topics", Site)
val `posts` = new Translated("posts", Site)
val `lastPost` = new Translated("lastPost", Site)
val `views` = new Translated("views", Site)
val `replies` = new Translated("replies", Site)
val `replyToThisTopic` = new Translated("replyToThisTopic", Site)
val `reply` = new Translated("reply", Site)
val `message` = new Translated("message", Site)
val `createTheTopic` = new Translated("createTheTopic", Site)
val `reportAUser` = new Translated("reportAUser", Site)
val `user` = new Translated("user", Site)
val `reason` = new Translated("reason", Site)
val `whatIsIheMatter` = new Translated("whatIsIheMatter", Site)
val `cheat` = new Translated("cheat", Site)
val `insult` = new Translated("insult", Site)
val `troll` = new Translated("troll", Site)
val `other` = new Translated("other", Site)
val `reportDescriptionHelp` = new Translated("reportDescriptionHelp", Site)
val `by` = new Translated("by", Site)
val `thisTopicIsNowClosed` = new Translated("thisTopicIsNowClosed", Site)
val `theming` = new Translated("theming", Site)
val `donate` = new Translated("donate", Site)
val `blog` = new Translated("blog", Site)
val `questionsAndAnswers` = new Translated("questionsAndAnswers", Site)
val `notes` = new Translated("notes", Site)
val `typePrivateNotesHere` = new Translated("typePrivateNotesHere", Site)
val `gameDisplay` = new Translated("gameDisplay", Site)
val `pieceAnimation` = new Translated("pieceAnimation", Site)
val `materialDifference` = new Translated("materialDifference", Site)
val `closeAccount` = new Translated("closeAccount", Site)
val `changedMindDoNotCloseAccount` = new Translated("changedMindDoNotCloseAccount", Site)
val `closeAccountExplanation` = new Translated("closeAccountExplanation", Site)
val `thisAccountIsClosed` = new Translated("thisAccountIsClosed", Site)
val `invalidUsernameOrPassword` = new Translated("invalidUsernameOrPassword", Site)
val `emailMeALink` = new Translated("emailMeALink", Site)
val `currentPassword` = new Translated("currentPassword", Site)
val `newPassword` = new Translated("newPassword", Site)
val `newPasswordAgain` = new Translated("newPasswordAgain", Site)
val `boardHighlights` = new Translated("boardHighlights", Site)
val `pieceDestinations` = new Translated("pieceDestinations", Site)
val `boardCoordinates` = new Translated("boardCoordinates", Site)
val `moveListWhilePlaying` = new Translated("moveListWhilePlaying", Site)
val `pgnPieceNotation` = new Translated("pgnPieceNotation", Site)
val `chessPieceSymbol` = new Translated("chessPieceSymbol", Site)
val `pgnLetter` = new Translated("pgnLetter", Site)
val `chessClock` = new Translated("chessClock", Site)
val `tenthsOfSeconds` = new Translated("tenthsOfSeconds", Site)
val `never` = new Translated("never", Site)
val `whenTimeRemainingLessThanTenSeconds` = new Translated("whenTimeRemainingLessThanTenSeconds", Site)
val `horizontalGreenProgressBars` = new Translated("horizontalGreenProgressBars", Site)
val `soundWhenTimeGetsCritical` = new Translated("soundWhenTimeGetsCritical", Site)
val `gameBehavior` = new Translated("gameBehavior", Site)
val `howDoYouMovePieces` = new Translated("howDoYouMovePieces", Site)
val `clickTwoSquares` = new Translated("clickTwoSquares", Site)
val `dragPiece` = new Translated("dragPiece", Site)
val `bothClicksAndDrag` = new Translated("bothClicksAndDrag", Site)
val `premovesPlayingDuringOpponentTurn` = new Translated("premovesPlayingDuringOpponentTurn", Site)
val `takebacksWithOpponentApproval` = new Translated("takebacksWithOpponentApproval", Site)
val `promoteToQueenAutomatically` = new Translated("promoteToQueenAutomatically", Site)
val `claimDrawOnThreefoldRepetitionAutomatically` = new Translated("claimDrawOnThreefoldRepetitionAutomatically", Site)
val `privacy` = new Translated("privacy", Site)
val `letOtherPlayersFollowYou` = new Translated("letOtherPlayersFollowYou", Site)
val `letOtherPlayersChallengeYou` = new Translated("letOtherPlayersChallengeYou", Site)
val `letOtherPlayersInviteYouToStudy` = new Translated("letOtherPlayersInviteYouToStudy", Site)
val `sound` = new Translated("sound", Site)
val `yourPreferencesHaveBeenSaved` = new Translated("yourPreferencesHaveBeenSaved", Site)
val `none` = new Translated("none", Site)
val `fast` = new Translated("fast", Site)
val `normal` = new Translated("normal", Site)
val `slow` = new Translated("slow", Site)
val `insideTheBoard` = new Translated("insideTheBoard", Site)
val `outsideTheBoard` = new Translated("outsideTheBoard", Site)
val `onSlowGames` = new Translated("onSlowGames", Site)
val `always` = new Translated("always", Site)
val `inCasualGamesOnly` = new Translated("inCasualGamesOnly", Site)
val `whenPremoving` = new Translated("whenPremoving", Site)
val `whenTimeRemainingLessThanThirtySeconds` = new Translated("whenTimeRemainingLessThanThirtySeconds", Site)
val `difficultyEasy` = new Translated("difficultyEasy", Site)
val `difficultyNormal` = new Translated("difficultyNormal", Site)
val `difficultyHard` = new Translated("difficultyHard", Site)
val `xLeftANoteOnY` = new Translated("xLeftANoteOnY", Site)
val `xCompetesInY` = new Translated("xCompetesInY", Site)
val `xAskedY` = new Translated("xAskedY", Site)
val `xAnsweredY` = new Translated("xAnsweredY", Site)
val `xCommentedY` = new Translated("xCommentedY", Site)
val `timeline` = new Translated("timeline", Site)
val `seeAllTournaments` = new Translated("seeAllTournaments", Site)
val `starting` = new Translated("starting", Site)
val `allInformationIsPublicAndOptional` = new Translated("allInformationIsPublicAndOptional", Site)
val `yourCityRegionOrDepartment` = new Translated("yourCityRegionOrDepartment", Site)
val `biographyDescription` = new Translated("biographyDescription", Site)
val `listBlockedPlayers` = new Translated("listBlockedPlayers", Site)
val `human` = new Translated("human", Site)
val `computer` = new Translated("computer", Site)
val `side` = new Translated("side", Site)
val `clock` = new Translated("clock", Site)
val `unauthorizedError` = new Translated("unauthorizedError", Site)
val `noInternetConnection` = new Translated("noInternetConnection", Site)
val `connectedToLichess` = new Translated("connectedToLichess", Site)
val `signedOut` = new Translated("signedOut", Site)
val `loginSuccessful` = new Translated("loginSuccessful", Site)
val `playOnTheBoardOffline` = new Translated("playOnTheBoardOffline", Site)
val `playOfflineComputer` = new Translated("playOfflineComputer", Site)
val `opponent` = new Translated("opponent", Site)
val `learn` = new Translated("learn", Site)
val `community` = new Translated("community", Site)
val `tools` = new Translated("tools", Site)
val `increment` = new Translated("increment", Site)
val `sharePGN` = new Translated("sharePGN", Site)
val `playOnline` = new Translated("playOnline", Site)
val `playOffline` = new Translated("playOffline", Site)
val `allowAnalytics` = new Translated("allowAnalytics", Site)
val `shareGameURL` = new Translated("shareGameURL", Site)
val `error.required` = new Translated("error.required", Site)
val `error.email` = new Translated("error.email", Site)
val `error.email_acceptable` = new Translated("error.email_acceptable", Site)
val `error.email_unique` = new Translated("error.email_unique", Site)
val `blindfoldChess` = new Translated("blindfoldChess", Site)
val `moveConfirmation` = new Translated("moveConfirmation", Site)
val `inCorrespondenceGames` = new Translated("inCorrespondenceGames", Site)
val `ifRatingIsPlusMinusX` = new Translated("ifRatingIsPlusMinusX", Site)
val `onlyFriends` = new Translated("onlyFriends", Site)
val `menu` = new Translated("menu", Site)
val `castling` = new Translated("castling", Site)
val `whiteCastlingKingside` = new Translated("whiteCastlingKingside", Site)
val `whiteCastlingQueenside` = new Translated("whiteCastlingQueenside", Site)
val `blackCastlingKingside` = new Translated("blackCastlingKingside", Site)
val `blackCastlingQueenside` = new Translated("blackCastlingQueenside", Site)
val `tpTimeSpentPlaying` = new Translated("tpTimeSpentPlaying", Site)
val `watchGames` = new Translated("watchGames", Site)
val `tpTimeSpentOnTV` = new Translated("tpTimeSpentOnTV", Site)
val `watch` = new Translated("watch", Site)
val `internationalEvents` = new Translated("internationalEvents", Site)
val `videoLibrary` = new Translated("videoLibrary", Site)
val `mobileApp` = new Translated("mobileApp", Site)
val `webmasters` = new Translated("webmasters", Site)
val `contribute` = new Translated("contribute", Site)
val `contact` = new Translated("contact", Site)
val `termsOfService` = new Translated("termsOfService", Site)
val `sourceCode` = new Translated("sourceCode", Site)
val `simultaneousExhibitions` = new Translated("simultaneousExhibitions", Site)
val `host` = new Translated("host", Site)
val `createdSimuls` = new Translated("createdSimuls", Site)
val `hostANewSimul` = new Translated("hostANewSimul", Site)
val `noSimulFound` = new Translated("noSimulFound", Site)
val `noSimulExplanation` = new Translated("noSimulExplanation", Site)
val `returnToSimulHomepage` = new Translated("returnToSimulHomepage", Site)
val `aboutSimul` = new Translated("aboutSimul", Site)
val `aboutSimulImage` = new Translated("aboutSimulImage", Site)
val `aboutSimulRealLife` = new Translated("aboutSimulRealLife", Site)
val `aboutSimulRules` = new Translated("aboutSimulRules", Site)
val `aboutSimulSettings` = new Translated("aboutSimulSettings", Site)
val `create` = new Translated("create", Site)
val `whenCreateSimul` = new Translated("whenCreateSimul", Site)
val `simulVariantsHint` = new Translated("simulVariantsHint", Site)
val `simulClockHint` = new Translated("simulClockHint", Site)
val `simulAddExtraTime` = new Translated("simulAddExtraTime", Site)
val `simulHostExtraTime` = new Translated("simulHostExtraTime", Site)
val `lichessTournaments` = new Translated("lichessTournaments", Site)
val `tournamentFAQ` = new Translated("tournamentFAQ", Site)
val `tournamentOfficial` = new Translated("tournamentOfficial", Site)
val `timeBeforeTournamentStarts` = new Translated("timeBeforeTournamentStarts", Site)
val `averageCentipawnLoss` = new Translated("averageCentipawnLoss", Site)
val `keyboardShortcuts` = new Translated("keyboardShortcuts", Site)
val `keyMoveBackwardOrForward` = new Translated("keyMoveBackwardOrForward", Site)
val `keyGoToStartOrEnd` = new Translated("keyGoToStartOrEnd", Site)
val `keyShowOrHideComments` = new Translated("keyShowOrHideComments", Site)
val `keyEnterOrExitVariation` = new Translated("keyEnterOrExitVariation", Site)
val `newTournament` = new Translated("newTournament", Site)
val `tournamentHomeTitle` = new Translated("tournamentHomeTitle", Site)
val `tournamentHomeDescription` = new Translated("tournamentHomeDescription", Site)
val `tournamentNotFound` = new Translated("tournamentNotFound", Site)
val `tournamentDoesNotExist` = new Translated("tournamentDoesNotExist", Site)
val `tournamentMayHaveBeenCanceled` = new Translated("tournamentMayHaveBeenCanceled", Site)
val `returnToTournamentsHomepage` = new Translated("returnToTournamentsHomepage", Site)
val `weeklyPerfTypeRatingDistribution` = new Translated("weeklyPerfTypeRatingDistribution", Site)
val `yourPerfTypeRatingIsRating` = new Translated("yourPerfTypeRatingIsRating", Site)
val `youAreBetterThanPercentOfPerfTypePlayers` = new Translated("youAreBetterThanPercentOfPerfTypePlayers", Site)
val `youDoNotHaveAnEstablishedPerfTypeRating` = new Translated("youDoNotHaveAnEstablishedPerfTypeRating", Site)
val `checkYourEmail` = new Translated("checkYourEmail", Site)
val `weHaveSentYouAnEmailClickTheLink` = new Translated("weHaveSentYouAnEmailClickTheLink", Site)
val `ifYouDoNotSeeTheEmailCheckOtherPlaces` = new Translated("ifYouDoNotSeeTheEmailCheckOtherPlaces", Site)
val `areYouSureYouEvenRegisteredYourEmailOnLichess` = new Translated("areYouSureYouEvenRegisteredYourEmailOnLichess", Site)
val `itWasNotRequiredForYourRegistration` = new Translated("itWasNotRequiredForYourRegistration", Site)
val `weHaveSentYouAnEmailTo` = new Translated("weHaveSentYouAnEmailTo", Site)
val `byRegisteringYouAgreeToBeBoundByOur` = new Translated("byRegisteringYouAgreeToBeBoundByOur", Site)
val `networkLagBetweenYouAndLichess` = new Translated("networkLagBetweenYouAndLichess", Site)
val `timeToProcessAMoveOnLichessServer` = new Translated("timeToProcessAMoveOnLichessServer", Site)
val `downloadAnnotated` = new Translated("downloadAnnotated", Site)
val `downloadRaw` = new Translated("downloadRaw", Site)
val `downloadImported` = new Translated("downloadImported", Site)
val `printFriendlyPDF` = new Translated("printFriendlyPDF", Site)
val `crosstable` = new Translated("crosstable", Site)
val `youCanAlsoScrollOverTheBoardToMoveInTheGame` = new Translated("youCanAlsoScrollOverTheBoardToMoveInTheGame", Site)
val `analysisShapesHowTo` = new Translated("analysisShapesHowTo", Site)
val `confirmResignation` = new Translated("confirmResignation", Site)
val `inputMovesWithTheKeyboard` = new Translated("inputMovesWithTheKeyboard", Site)
val `letOtherPlayersMessageYou` = new Translated("letOtherPlayersMessageYou", Site)
val `castleByMovingTheKingTwoSquaresOrOntoTheRook` = new Translated("castleByMovingTheKingTwoSquaresOrOntoTheRook", Site)
val `castleByMovingTwoSquares` = new Translated("castleByMovingTwoSquares", Site)
val `castleByMovingOntoTheRook` = new Translated("castleByMovingOntoTheRook", Site)
val `shareYourInsightsData` = new Translated("shareYourInsightsData", Site)
val `withNobody` = new Translated("withNobody", Site)
val `withFriends` = new Translated("withFriends", Site)
val `withEverybody` = new Translated("withEverybody", Site)
val `youHaveAlreadyRegisteredTheEmail` = new Translated("youHaveAlreadyRegisteredTheEmail", Site)
val `kidMode` = new Translated("kidMode", Site)
val `kidModeExplanation` = new Translated("kidModeExplanation", Site)
val `inKidModeTheLichessLogoGetsIconX` = new Translated("inKidModeTheLichessLogoGetsIconX", Site)
val `enableKidMode` = new Translated("enableKidMode", Site)
val `disableKidMode` = new Translated("disableKidMode", Site)
val `security` = new Translated("security", Site)
val `thisIsAListOfDevicesThatHaveLoggedIntoYourAccount` = new Translated("thisIsAListOfDevicesThatHaveLoggedIntoYourAccount", Site)
val `alternativelyYouCanX` = new Translated("alternativelyYouCanX", Site)
val `revokeAllSessions` = new Translated("revokeAllSessions", Site)
val `playChessEverywhere` = new Translated("playChessEverywhere", Site)
val `asFreeAsLichess` = new Translated("asFreeAsLichess", Site)
val `builtForTheLoveOfChessNotMoney` = new Translated("builtForTheLoveOfChessNotMoney", Site)
val `everybodyGetsAllFeaturesForFree` = new Translated("everybodyGetsAllFeaturesForFree", Site)
val `zeroAdvertisement` = new Translated("zeroAdvertisement", Site)
val `fullFeatured` = new Translated("fullFeatured", Site)
val `phoneAndTablet` = new Translated("phoneAndTablet", Site)
val `bulletBlitzClassical` = new Translated("bulletBlitzClassical", Site)
val `correspondenceChess` = new Translated("correspondenceChess", Site)
val `onlineAndOfflinePlay` = new Translated("onlineAndOfflinePlay", Site)
val `correspondenceAndUnlimited` = new Translated("correspondenceAndUnlimited", Site)
val `viewTheSolution` = new Translated("viewTheSolution", Site)
val `followAndChallengeFriends` = new Translated("followAndChallengeFriends", Site)
val `gameAnalysis` = new Translated("gameAnalysis", Site)
val `xHostsY` = new Translated("xHostsY", Site)
val `xJoinsY` = new Translated("xJoinsY", Site)
val `xLikesY` = new Translated("xLikesY", Site)
val `quickPairing` = new Translated("quickPairing", Site)
val `lobby` = new Translated("lobby", Site)
val `yourScore` = new Translated("yourScore", Site)
val `language` = new Translated("language", Site)
val `background` = new Translated("background", Site)
val `boardGeometry` = new Translated("boardGeometry", Site)
val `boardTheme` = new Translated("boardTheme", Site)
val `boardSize` = new Translated("boardSize", Site)
val `pieceSet` = new Translated("pieceSet", Site)
val `embedInYourWebsite` = new Translated("embedInYourWebsite", Site)
val `usernameAlreadyUsed` = new Translated("usernameAlreadyUsed", Site)
val `usernameInvalid` = new Translated("usernameInvalid", Site)
val `usernameStartNoNumber` = new Translated("usernameStartNoNumber", Site)
val `usernameUnacceptable` = new Translated("usernameUnacceptable", Site)
val `directlySupportLichess` = new Translated("directlySupportLichess", Site)
val `playChessInStyle` = new Translated("playChessInStyle", Site)
val `chessBasics` = new Translated("chessBasics", Site)
val `coaches` = new Translated("coaches", Site)
val `invalidPgn` = new Translated("invalidPgn", Site)
val `invalidFen` = new Translated("invalidFen", Site)
val `error.minLength` = new Translated("error.minLength", Site)
val `error.maxLength` = new Translated("error.maxLength", Site)
val `error.min` = new Translated("error.min", Site)
val `error.max` = new Translated("error.max", Site)
val `error.unknown` = new Translated("error.unknown", Site)
val `custom` = new Translated("custom", Site)
val `notifications` = new Translated("notifications", Site)
val `challenges` = new Translated("challenges", Site)
val `perfRatingX` = new Translated("perfRatingX", Site)
val `nbNodes` = new Translated("nbNodes", Site)
val `mateInXHalfMoves` = new Translated("mateInXHalfMoves", Site)
val `nextCaptureOrPawnMoveInXHalfMoves` = new Translated("nextCaptureOrPawnMoveInXHalfMoves", Site)
val `nbPlayers` = new Translated("nbPlayers", Site)
val `nbGames` = new Translated("nbGames", Site)
val `nbBookmarks` = new Translated("nbBookmarks", Site)
val `nbDays` = new Translated("nbDays", Site)
val `nbHours` = new Translated("nbHours", Site)
val `nbGamesWithYou` = new Translated("nbGamesWithYou", Site)
val `nbGamesPlayed` = new Translated("nbGamesPlayed", Site)
val `nbRated` = new Translated("nbRated", Site)
val `nbWins` = new Translated("nbWins", Site)
val `nbLosses` = new Translated("nbLosses", Site)
val `nbDraws` = new Translated("nbDraws", Site)
val `giveNbSeconds` = new Translated("giveNbSeconds", Site)
val `nbMembers` = new Translated("nbMembers", Site)
val `nbImportedGames` = new Translated("nbImportedGames", Site)
val `nbFollowers` = new Translated("nbFollowers", Site)
val `nbFollowing` = new Translated("nbFollowing", Site)
val `lessThanNbMinutes` = new Translated("lessThanNbMinutes", Site)
val `playedXTimes` = new Translated("playedXTimes", Site)
val `youHaveNbSecondsToMakeYourFirstMove` = new Translated("youHaveNbSecondsToMakeYourFirstMove", Site)
val `nbGamesInPlay` = new Translated("nbGamesInPlay", Site)
val `maximumNbCharacters` = new Translated("maximumNbCharacters", Site)
val `blocks` = new Translated("blocks", Site)
val `nbForumPosts` = new Translated("nbForumPosts", Site)
val `nbPerfTypePlayersThisWeek` = new Translated("nbPerfTypePlayersThisWeek", Site)
val `availableInNbLanguages` = new Translated("availableInNbLanguages", Site)

object arena {
val `isItRated` = new Translated("isItRated", Arena)
val `willBeNotified` = new Translated("willBeNotified", Arena)
val `isRated` = new Translated("isRated", Arena)
val `isNotRated` = new Translated("isNotRated", Arena)
val `someRated` = new Translated("someRated", Arena)
val `howAreScoresCalculated` = new Translated("howAreScoresCalculated", Arena)
}

}