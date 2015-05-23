// Generated with bin/trans-dump
package lila.i18n

import play.twirl.api.Html
import play.api.i18n.Lang

import lila.user.UserContext

final class I18nKeys(translator: Translator) {

  final class Key(val key: String) extends I18nKey {

    def apply(args: Any*)(implicit ctx: UserContext): Html =
      translator.html(key, args.toList)(ctx.req)

    def str(args: Any*)(implicit ctx: UserContext): String =
      translator.str(key, args.toList)(ctx.req)

    def to(lang: Lang)(args: Any*): String =
      translator.transTo(key, args.toList)(lang)
  }

  def untranslated(message: String) = Untranslated(message)

  val `playWithAFriend` = new Key("playWithAFriend")
  val `playWithTheMachine` = new Key("playWithTheMachine")
  val `toInviteSomeoneToPlayGiveThisUrl` = new Key("toInviteSomeoneToPlayGiveThisUrl")
  val `gameOver` = new Key("gameOver")
  val `waitingForOpponent` = new Key("waitingForOpponent")
  val `waiting` = new Key("waiting")
  val `yourTurn` = new Key("yourTurn")
  val `aiNameLevelAiLevel` = new Key("aiNameLevelAiLevel")
  val `level` = new Key("level")
  val `toggleTheChat` = new Key("toggleTheChat")
  val `toggleSound` = new Key("toggleSound")
  val `chat` = new Key("chat")
  val `resign` = new Key("resign")
  val `checkmate` = new Key("checkmate")
  val `stalemate` = new Key("stalemate")
  val `white` = new Key("white")
  val `black` = new Key("black")
  val `randomColor` = new Key("randomColor")
  val `createAGame` = new Key("createAGame")
  val `whiteIsVictorious` = new Key("whiteIsVictorious")
  val `blackIsVictorious` = new Key("blackIsVictorious")
  val `kingInTheCenter` = new Key("kingInTheCenter")
  val `threeChecks` = new Key("threeChecks")
  val `variantEnding` = new Key("variantEnding")
  val `playWithTheSameOpponentAgain` = new Key("playWithTheSameOpponentAgain")
  val `newOpponent` = new Key("newOpponent")
  val `playWithAnotherOpponent` = new Key("playWithAnotherOpponent")
  val `yourOpponentWantsToPlayANewGameWithYou` = new Key("yourOpponentWantsToPlayANewGameWithYou")
  val `joinTheGame` = new Key("joinTheGame")
  val `whitePlays` = new Key("whitePlays")
  val `blackPlays` = new Key("blackPlays")
  val `theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim` = new Key("theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim")
  val `makeYourOpponentResign` = new Key("makeYourOpponentResign")
  val `forceResignation` = new Key("forceResignation")
  val `forceDraw` = new Key("forceDraw")
  val `talkInChat` = new Key("talkInChat")
  val `theFirstPersonToComeOnThisUrlWillPlayWithYou` = new Key("theFirstPersonToComeOnThisUrlWillPlayWithYou")
  val `whiteCreatesTheGame` = new Key("whiteCreatesTheGame")
  val `blackCreatesTheGame` = new Key("blackCreatesTheGame")
  val `whiteJoinsTheGame` = new Key("whiteJoinsTheGame")
  val `blackJoinsTheGame` = new Key("blackJoinsTheGame")
  val `whiteResigned` = new Key("whiteResigned")
  val `blackResigned` = new Key("blackResigned")
  val `whiteLeftTheGame` = new Key("whiteLeftTheGame")
  val `blackLeftTheGame` = new Key("blackLeftTheGame")
  val `shareThisUrlToLetSpectatorsSeeTheGame` = new Key("shareThisUrlToLetSpectatorsSeeTheGame")
  val `replayAndAnalyse` = new Key("replayAndAnalyse")
  val `computerAnalysisInProgress` = new Key("computerAnalysisInProgress")
  val `theComputerAnalysisHasFailed` = new Key("theComputerAnalysisHasFailed")
  val `viewTheComputerAnalysis` = new Key("viewTheComputerAnalysis")
  val `requestAComputerAnalysis` = new Key("requestAComputerAnalysis")
  val `computerAnalysis` = new Key("computerAnalysis")
  val `analysis` = new Key("analysis")
  val `blunders` = new Key("blunders")
  val `mistakes` = new Key("mistakes")
  val `inaccuracies` = new Key("inaccuracies")
  val `moveTimes` = new Key("moveTimes")
  val `flipBoard` = new Key("flipBoard")
  val `threefoldRepetition` = new Key("threefoldRepetition")
  val `claimADraw` = new Key("claimADraw")
  val `offerDraw` = new Key("offerDraw")
  val `draw` = new Key("draw")
  val `nbConnectedPlayers` = new Key("nbConnectedPlayers")
  val `gamesBeingPlayedRightNow` = new Key("gamesBeingPlayedRightNow")
  val `viewAllNbGames` = new Key("viewAllNbGames")
  val `viewNbCheckmates` = new Key("viewNbCheckmates")
  val `nbBookmarks` = new Key("nbBookmarks")
  val `nbPopularGames` = new Key("nbPopularGames")
  val `nbAnalysedGames` = new Key("nbAnalysedGames")
  val `bookmarkedByNbPlayers` = new Key("bookmarkedByNbPlayers")
  val `viewInFullSize` = new Key("viewInFullSize")
  val `logOut` = new Key("logOut")
  val `signIn` = new Key("signIn")
  val `newToLichess` = new Key("newToLichess")
  val `youNeedAnAccountToDoThat` = new Key("youNeedAnAccountToDoThat")
  val `signUp` = new Key("signUp")
  val `computersAreNotAllowedToPlay` = new Key("computersAreNotAllowedToPlay")
  val `games` = new Key("games")
  val `forum` = new Key("forum")
  val `xPostedInForumY` = new Key("xPostedInForumY")
  val `latestForumPosts` = new Key("latestForumPosts")
  val `players` = new Key("players")
  val `minutesPerSide` = new Key("minutesPerSide")
  val `variant` = new Key("variant")
  val `timeControl` = new Key("timeControl")
  val `realTime` = new Key("realTime")
  val `correspondence` = new Key("correspondence")
  val `daysPerTurn` = new Key("daysPerTurn")
  val `oneDay` = new Key("oneDay")
  val `nbDays` = new Key("nbDays")
  val `nbHours` = new Key("nbHours")
  val `time` = new Key("time")
  val `rating` = new Key("rating")
  val `username` = new Key("username")
  val `password` = new Key("password")
  val `haveAnAccount` = new Key("haveAnAccount")
  val `allYouNeedIsAUsernameAndAPassword` = new Key("allYouNeedIsAUsernameAndAPassword")
  val `changePassword` = new Key("changePassword")
  val `changeEmail` = new Key("changeEmail")
  val `email` = new Key("email")
  val `emailIsOptional` = new Key("emailIsOptional")
  val `passwordReset` = new Key("passwordReset")
  val `forgotPassword` = new Key("forgotPassword")
  val `learnMoreAboutLichess` = new Key("learnMoreAboutLichess")
  val `rank` = new Key("rank")
  val `gamesPlayed` = new Key("gamesPlayed")
  val `nbGamesWithYou` = new Key("nbGamesWithYou")
  val `declineInvitation` = new Key("declineInvitation")
  val `cancel` = new Key("cancel")
  val `timeOut` = new Key("timeOut")
  val `drawOfferSent` = new Key("drawOfferSent")
  val `drawOfferDeclined` = new Key("drawOfferDeclined")
  val `drawOfferAccepted` = new Key("drawOfferAccepted")
  val `drawOfferCanceled` = new Key("drawOfferCanceled")
  val `whiteOffersDraw` = new Key("whiteOffersDraw")
  val `blackOffersDraw` = new Key("blackOffersDraw")
  val `whiteDeclinesDraw` = new Key("whiteDeclinesDraw")
  val `blackDeclinesDraw` = new Key("blackDeclinesDraw")
  val `yourOpponentOffersADraw` = new Key("yourOpponentOffersADraw")
  val `accept` = new Key("accept")
  val `decline` = new Key("decline")
  val `playingRightNow` = new Key("playingRightNow")
  val `finished` = new Key("finished")
  val `abortGame` = new Key("abortGame")
  val `gameAborted` = new Key("gameAborted")
  val `standard` = new Key("standard")
  val `unlimited` = new Key("unlimited")
  val `mode` = new Key("mode")
  val `casual` = new Key("casual")
  val `rated` = new Key("rated")
  val `thisGameIsRated` = new Key("thisGameIsRated")
  val `rematch` = new Key("rematch")
  val `rematchOfferSent` = new Key("rematchOfferSent")
  val `rematchOfferAccepted` = new Key("rematchOfferAccepted")
  val `rematchOfferCanceled` = new Key("rematchOfferCanceled")
  val `rematchOfferDeclined` = new Key("rematchOfferDeclined")
  val `cancelRematchOffer` = new Key("cancelRematchOffer")
  val `viewRematch` = new Key("viewRematch")
  val `play` = new Key("play")
  val `inbox` = new Key("inbox")
  val `chatRoom` = new Key("chatRoom")
  val `spectatorRoom` = new Key("spectatorRoom")
  val `composeMessage` = new Key("composeMessage")
  val `noNewMessages` = new Key("noNewMessages")
  val `subject` = new Key("subject")
  val `recipient` = new Key("recipient")
  val `send` = new Key("send")
  val `incrementInSeconds` = new Key("incrementInSeconds")
  val `freeOnlineChess` = new Key("freeOnlineChess")
  val `spectators` = new Key("spectators")
  val `nbWins` = new Key("nbWins")
  val `nbLosses` = new Key("nbLosses")
  val `nbDraws` = new Key("nbDraws")
  val `exportGames` = new Key("exportGames")
  val `ratingRange` = new Key("ratingRange")
  val `giveNbSeconds` = new Key("giveNbSeconds")
  val `premoveEnabledClickAnywhereToCancel` = new Key("premoveEnabledClickAnywhereToCancel")
  val `thisPlayerUsesChessComputerAssistance` = new Key("thisPlayerUsesChessComputerAssistance")
  val `thisPlayerArtificiallyIncreasesTheirRating` = new Key("thisPlayerArtificiallyIncreasesTheirRating")
  val `opening` = new Key("opening")
  val `takeback` = new Key("takeback")
  val `proposeATakeback` = new Key("proposeATakeback")
  val `takebackPropositionSent` = new Key("takebackPropositionSent")
  val `takebackPropositionDeclined` = new Key("takebackPropositionDeclined")
  val `takebackPropositionAccepted` = new Key("takebackPropositionAccepted")
  val `takebackPropositionCanceled` = new Key("takebackPropositionCanceled")
  val `yourOpponentProposesATakeback` = new Key("yourOpponentProposesATakeback")
  val `bookmarkThisGame` = new Key("bookmarkThisGame")
  val `search` = new Key("search")
  val `advancedSearch` = new Key("advancedSearch")
  val `tournament` = new Key("tournament")
  val `tournaments` = new Key("tournaments")
  val `tournamentPoints` = new Key("tournamentPoints")
  val `viewTournament` = new Key("viewTournament")
  val `backToTournament` = new Key("backToTournament")
  val `backToGame` = new Key("backToGame")
  val `freeOnlineChessGamePlayChessNowInACleanInterfaceNoRegistrationNoAdsNoPluginRequiredPlayChessWithComputerFriendsOrRandomOpponents` = new Key("freeOnlineChessGamePlayChessNowInACleanInterfaceNoRegistrationNoAdsNoPluginRequiredPlayChessWithComputerFriendsOrRandomOpponents")
  val `teams` = new Key("teams")
  val `nbMembers` = new Key("nbMembers")
  val `allTeams` = new Key("allTeams")
  val `newTeam` = new Key("newTeam")
  val `myTeams` = new Key("myTeams")
  val `noTeamFound` = new Key("noTeamFound")
  val `joinTeam` = new Key("joinTeam")
  val `quitTeam` = new Key("quitTeam")
  val `anyoneCanJoin` = new Key("anyoneCanJoin")
  val `aConfirmationIsRequiredToJoin` = new Key("aConfirmationIsRequiredToJoin")
  val `joiningPolicy` = new Key("joiningPolicy")
  val `teamLeader` = new Key("teamLeader")
  val `teamBestPlayers` = new Key("teamBestPlayers")
  val `teamRecentMembers` = new Key("teamRecentMembers")
  val `xJoinedTeamY` = new Key("xJoinedTeamY")
  val `xCreatedTeamY` = new Key("xCreatedTeamY")
  val `averageElo` = new Key("averageElo")
  val `location` = new Key("location")
  val `settings` = new Key("settings")
  val `filterGames` = new Key("filterGames")
  val `reset` = new Key("reset")
  val `apply` = new Key("apply")
  val `leaderboard` = new Key("leaderboard")
  val `pasteTheFenStringHere` = new Key("pasteTheFenStringHere")
  val `pasteThePgnStringHere` = new Key("pasteThePgnStringHere")
  val `fromPosition` = new Key("fromPosition")
  val `continueFromHere` = new Key("continueFromHere")
  val `importGame` = new Key("importGame")
  val `nbImportedGames` = new Key("nbImportedGames")
  val `thisIsAChessCaptcha` = new Key("thisIsAChessCaptcha")
  val `clickOnTheBoardToMakeYourMove` = new Key("clickOnTheBoardToMakeYourMove")
  val `notACheckmate` = new Key("notACheckmate")
  val `colorPlaysCheckmateInOne` = new Key("colorPlaysCheckmateInOne")
  val `retry` = new Key("retry")
  val `reconnecting` = new Key("reconnecting")
  val `onlineFriends` = new Key("onlineFriends")
  val `noFriendsOnline` = new Key("noFriendsOnline")
  val `findFriends` = new Key("findFriends")
  val `favoriteOpponents` = new Key("favoriteOpponents")
  val `follow` = new Key("follow")
  val `following` = new Key("following")
  val `unfollow` = new Key("unfollow")
  val `block` = new Key("block")
  val `blocked` = new Key("blocked")
  val `unblock` = new Key("unblock")
  val `followsYou` = new Key("followsYou")
  val `xStartedFollowingY` = new Key("xStartedFollowingY")
  val `nbFollowers` = new Key("nbFollowers")
  val `nbFollowing` = new Key("nbFollowing")
  val `more` = new Key("more")
  val `memberSince` = new Key("memberSince")
  val `lastLogin` = new Key("lastLogin")
  val `challengeToPlay` = new Key("challengeToPlay")
  val `player` = new Key("player")
  val `list` = new Key("list")
  val `graph` = new Key("graph")
  val `lessThanNbMinutes` = new Key("lessThanNbMinutes")
  val `xToYMinutes` = new Key("xToYMinutes")
  val `textIsTooShort` = new Key("textIsTooShort")
  val `textIsTooLong` = new Key("textIsTooLong")
  val `required` = new Key("required")
  val `openTournaments` = new Key("openTournaments")
  val `duration` = new Key("duration")
  val `winner` = new Key("winner")
  val `standing` = new Key("standing")
  val `createANewTournament` = new Key("createANewTournament")
  val `join` = new Key("join")
  val `withdraw` = new Key("withdraw")
  val `points` = new Key("points")
  val `wins` = new Key("wins")
  val `losses` = new Key("losses")
  val `winStreak` = new Key("winStreak")
  val `createdBy` = new Key("createdBy")
  val `tournamentIsStarting` = new Key("tournamentIsStarting")
  val `membersOnly` = new Key("membersOnly")
  val `boardEditor` = new Key("boardEditor")
  val `startPosition` = new Key("startPosition")
  val `clearBoard` = new Key("clearBoard")
  val `savePosition` = new Key("savePosition")
  val `loadPosition` = new Key("loadPosition")
  val `isPrivate` = new Key("isPrivate")
  val `reportXToModerators` = new Key("reportXToModerators")
  val `profile` = new Key("profile")
  val `editProfile` = new Key("editProfile")
  val `firstName` = new Key("firstName")
  val `lastName` = new Key("lastName")
  val `biography` = new Key("biography")
  val `country` = new Key("country")
  val `preferences` = new Key("preferences")
  val `watchLichessTV` = new Key("watchLichessTV")
  val `previouslyOnLichessTV` = new Key("previouslyOnLichessTV")
  val `todaysLeaders` = new Key("todaysLeaders")
  val `onlinePlayers` = new Key("onlinePlayers")
  val `progressToday` = new Key("progressToday")
  val `progressThisWeek` = new Key("progressThisWeek")
  val `progressThisMonth` = new Key("progressThisMonth")
  val `leaderboardThisWeek` = new Key("leaderboardThisWeek")
  val `leaderboardThisMonth` = new Key("leaderboardThisMonth")
  val `activeToday` = new Key("activeToday")
  val `activeThisWeek` = new Key("activeThisWeek")
  val `activePlayers` = new Key("activePlayers")
  val `bewareTheGameIsRatedButHasNoClock` = new Key("bewareTheGameIsRatedButHasNoClock")
  val `training` = new Key("training")
  val `yourPuzzleRatingX` = new Key("yourPuzzleRatingX")
  val `findTheBestMoveForWhite` = new Key("findTheBestMoveForWhite")
  val `findTheBestMoveForBlack` = new Key("findTheBestMoveForBlack")
  val `toTrackYourProgress` = new Key("toTrackYourProgress")
  val `trainingSignupExplanation` = new Key("trainingSignupExplanation")
  val `recentlyPlayedPuzzles` = new Key("recentlyPlayedPuzzles")
  val `puzzleId` = new Key("puzzleId")
  val `puzzleOfTheDay` = new Key("puzzleOfTheDay")
  val `clickToSolve` = new Key("clickToSolve")
  val `goodMove` = new Key("goodMove")
  val `butYouCanDoBetter` = new Key("butYouCanDoBetter")
  val `bestMove` = new Key("bestMove")
  val `keepGoing` = new Key("keepGoing")
  val `puzzleFailed` = new Key("puzzleFailed")
  val `butYouCanKeepTrying` = new Key("butYouCanKeepTrying")
  val `victory` = new Key("victory")
  val `giveUp` = new Key("giveUp")
  val `puzzleSolvedInXSeconds` = new Key("puzzleSolvedInXSeconds")
  val `wasThisPuzzleAnyGood` = new Key("wasThisPuzzleAnyGood")
  val `pleaseVotePuzzle` = new Key("pleaseVotePuzzle")
  val `thankYou` = new Key("thankYou")
  val `ratingX` = new Key("ratingX")
  val `playedXTimes` = new Key("playedXTimes")
  val `fromGameLink` = new Key("fromGameLink")
  val `startTraining` = new Key("startTraining")
  val `continueTraining` = new Key("continueTraining")
  val `retryThisPuzzle` = new Key("retryThisPuzzle")
  val `thisPuzzleIsCorrect` = new Key("thisPuzzleIsCorrect")
  val `thisPuzzleIsWrong` = new Key("thisPuzzleIsWrong")
  val `youHaveNbSecondsToMakeYourFirstMove` = new Key("youHaveNbSecondsToMakeYourFirstMove")
  val `nbGamesInPlay` = new Key("nbGamesInPlay")
  val `automaticallyProceedToNextGameAfterMoving` = new Key("automaticallyProceedToNextGameAfterMoving")
  val `autoSwitch` = new Key("autoSwitch")
  val `openingId` = new Key("openingId")
  val `yourOpeningRatingX` = new Key("yourOpeningRatingX")
  val `findNbStrongMoves` = new Key("findNbStrongMoves")
  val `thisMoveGivesYourOpponentTheAdvantage` = new Key("thisMoveGivesYourOpponentTheAdvantage")
  val `openingFailed` = new Key("openingFailed")
  val `openingSolved` = new Key("openingSolved")
  val `recentlyPlayedOpenings` = new Key("recentlyPlayedOpenings")
  val `puzzles` = new Key("puzzles")
  val `coordinates` = new Key("coordinates")
  val `openings` = new Key("openings")
  val `latestUpdates` = new Key("latestUpdates")
  val `tournamentWinners` = new Key("tournamentWinners")
  val `name` = new Key("name")
  val `description` = new Key("description")
  val `no` = new Key("no")
  val `yes` = new Key("yes")
  val `help` = new Key("help")
  val `createANewTopic` = new Key("createANewTopic")
  val `topics` = new Key("topics")
  val `posts` = new Key("posts")
  val `lastPost` = new Key("lastPost")
  val `views` = new Key("views")
  val `replies` = new Key("replies")
  val `replyToThisTopic` = new Key("replyToThisTopic")
  val `reply` = new Key("reply")
  val `message` = new Key("message")
  val `createTheTopic` = new Key("createTheTopic")
  val `reportAUser` = new Key("reportAUser")
  val `user` = new Key("user")
  val `reason` = new Key("reason")
  val `whatIsIheMatter` = new Key("whatIsIheMatter")
  val `cheat` = new Key("cheat")
  val `insult` = new Key("insult")
  val `troll` = new Key("troll")
  val `other` = new Key("other")
  val `reportDescriptionHelp` = new Key("reportDescriptionHelp")
  val `by` = new Key("by")
  val `thisTopicIsNowClosed` = new Key("thisTopicIsNowClosed")
  val `theming` = new Key("theming")
  val `donate` = new Key("donate")
  val `blog` = new Key("blog")
  val `map` = new Key("map")
  val `realTimeWorldMapOfChessMoves` = new Key("realTimeWorldMapOfChessMoves")
  val `questionsAndAnswers` = new Key("questionsAndAnswers")
  val `notes` = new Key("notes")
  val `typePrivateNotesHere` = new Key("typePrivateNotesHere")
  val `gameDisplay` = new Key("gameDisplay")
  val `pieceAnimation` = new Key("pieceAnimation")
  val `materialDifference` = new Key("materialDifference")
  val `closeAccount` = new Key("closeAccount")
  val `closeYourAccount` = new Key("closeYourAccount")
  val `changedMindDoNotCloseAccount` = new Key("changedMindDoNotCloseAccount")
  val `closeAccountExplanation` = new Key("closeAccountExplanation")
  val `thisAccountIsClosed` = new Key("thisAccountIsClosed")
  val `invalidUsernameOrPassword` = new Key("invalidUsernameOrPassword")
  val `emailMeALink` = new Key("emailMeALink")
  val `currentPassword` = new Key("currentPassword")
  val `newPassword` = new Key("newPassword")
  val `newPasswordAgain` = new Key("newPasswordAgain")
  val `boardHighlights` = new Key("boardHighlights")
  val `pieceDestinations` = new Key("pieceDestinations")
  val `boardCoordinates` = new Key("boardCoordinates")
  val `moveListWhilePlaying` = new Key("moveListWhilePlaying")
  val `chessClock` = new Key("chessClock")
  val `tenthsOfSeconds` = new Key("tenthsOfSeconds")
  val `never` = new Key("never")
  val `whenTimeRemainingLessThanTenSeconds` = new Key("whenTimeRemainingLessThanTenSeconds")
  val `horizontalGreenProgressBars` = new Key("horizontalGreenProgressBars")
  val `soundWhenTimeGetsCritical` = new Key("soundWhenTimeGetsCritical")
  val `gameBehavior` = new Key("gameBehavior")
  val `premovesPlayingDuringOpponentTurn` = new Key("premovesPlayingDuringOpponentTurn")
  val `takebacksWithOpponentApproval` = new Key("takebacksWithOpponentApproval")
  val `promoteToQueenAutomatically` = new Key("promoteToQueenAutomatically")
  val `claimDrawOnThreefoldRepetitionAutomatically` = new Key("claimDrawOnThreefoldRepetitionAutomatically")
  val `privacy` = new Key("privacy")
  val `letOtherPlayersFollowYou` = new Key("letOtherPlayersFollowYou")
  val `letOtherPlayersChallengeYou` = new Key("letOtherPlayersChallengeYou")
  val `sound` = new Key("sound")
  val `soundControlInTheTopBarOfEveryPage` = new Key("soundControlInTheTopBarOfEveryPage")
  val `yourPreferencesHaveBeenSaved` = new Key("yourPreferencesHaveBeenSaved")
  val `none` = new Key("none")
  val `fast` = new Key("fast")
  val `normal` = new Key("normal")
  val `slow` = new Key("slow")
  val `insideTheBoard` = new Key("insideTheBoard")
  val `outsideTheBoard` = new Key("outsideTheBoard")
  val `onSlowGames` = new Key("onSlowGames")
  val `always` = new Key("always")
  val `inCasualGamesOnly` = new Key("inCasualGamesOnly")
  val `whenPremoving` = new Key("whenPremoving")
  val `whenTimeRemainingLessThanThirtySeconds` = new Key("whenTimeRemainingLessThanThirtySeconds")
  val `difficultyEasy` = new Key("difficultyEasy")
  val `difficultyNormal` = new Key("difficultyNormal")
  val `difficultyHard` = new Key("difficultyHard")
  val `xLeftANoteOnY` = new Key("xLeftANoteOnY")
  val `xCompetesInY` = new Key("xCompetesInY")
  val `xAskedY` = new Key("xAskedY")
  val `xAnsweredY` = new Key("xAnsweredY")
  val `xCommentedY` = new Key("xCommentedY")
  val `timeline` = new Key("timeline")
  val `seeAllTournaments` = new Key("seeAllTournaments")
  val `starting` = new Key("starting")
  val `allInformationIsPublicAndOptional` = new Key("allInformationIsPublicAndOptional")
  val `yourCityRegionOrDepartment` = new Key("yourCityRegionOrDepartment")
  val `biographyDescription` = new Key("biographyDescription")
  val `maximumNbCharacters` = new Key("maximumNbCharacters")
  val `blocks` = new Key("blocks")
  val `listBlockedPlayers` = new Key("listBlockedPlayers")
  val `human` = new Key("human")
  val `computer` = new Key("computer")
  val `side` = new Key("side")
  val `clock` = new Key("clock")
  val `unauthorizedError` = new Key("unauthorizedError")
  val `noInternetConnection` = new Key("noInternetConnection")
  val `connectedToLichess` = new Key("connectedToLichess")
  val `signedOut` = new Key("signedOut")
  val `loginSuccessful` = new Key("loginSuccessful")
  val `playOnTheBoardOffline` = new Key("playOnTheBoardOffline")
  val `playOfflineComputer` = new Key("playOfflineComputer")
  val `opponent` = new Key("opponent")
  val `learn` = new Key("learn")
  val `community` = new Key("community")
  val `tools` = new Key("tools")
  val `increment` = new Key("increment")
  val `board` = new Key("board")
  val `pieces` = new Key("pieces")
  val `sharePGN` = new Key("sharePGN")
  val `playOnline` = new Key("playOnline")
  val `playOffline` = new Key("playOffline")
  val `allowAnalytics` = new Key("allowAnalytics")
  val `shareGameURL` = new Key("shareGameURL")

  def keys = List(`playWithAFriend`, `playWithTheMachine`, `toInviteSomeoneToPlayGiveThisUrl`, `gameOver`, `waitingForOpponent`, `waiting`, `yourTurn`, `aiNameLevelAiLevel`, `level`, `toggleTheChat`, `toggleSound`, `chat`, `resign`, `checkmate`, `stalemate`, `white`, `black`, `randomColor`, `createAGame`, `whiteIsVictorious`, `blackIsVictorious`, `kingInTheCenter`, `threeChecks`, `variantEnding`, `playWithTheSameOpponentAgain`, `newOpponent`, `playWithAnotherOpponent`, `yourOpponentWantsToPlayANewGameWithYou`, `joinTheGame`, `whitePlays`, `blackPlays`, `theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim`, `makeYourOpponentResign`, `forceResignation`, `forceDraw`, `talkInChat`, `theFirstPersonToComeOnThisUrlWillPlayWithYou`, `whiteCreatesTheGame`, `blackCreatesTheGame`, `whiteJoinsTheGame`, `blackJoinsTheGame`, `whiteResigned`, `blackResigned`, `whiteLeftTheGame`, `blackLeftTheGame`, `shareThisUrlToLetSpectatorsSeeTheGame`, `replayAndAnalyse`, `computerAnalysisInProgress`, `theComputerAnalysisHasFailed`, `viewTheComputerAnalysis`, `requestAComputerAnalysis`, `computerAnalysis`, `analysis`, `blunders`, `mistakes`, `inaccuracies`, `moveTimes`, `flipBoard`, `threefoldRepetition`, `claimADraw`, `offerDraw`, `draw`, `nbConnectedPlayers`, `gamesBeingPlayedRightNow`, `viewAllNbGames`, `viewNbCheckmates`, `nbBookmarks`, `nbPopularGames`, `nbAnalysedGames`, `bookmarkedByNbPlayers`, `viewInFullSize`, `logOut`, `signIn`, `newToLichess`, `youNeedAnAccountToDoThat`, `signUp`, `computersAreNotAllowedToPlay`, `games`, `forum`, `xPostedInForumY`, `latestForumPosts`, `players`, `minutesPerSide`, `variant`, `timeControl`, `realTime`, `correspondence`, `daysPerTurn`, `oneDay`, `nbDays`, `nbHours`, `time`, `rating`, `username`, `password`, `haveAnAccount`, `allYouNeedIsAUsernameAndAPassword`, `changePassword`, `changeEmail`, `email`, `emailIsOptional`, `passwordReset`, `forgotPassword`, `learnMoreAboutLichess`, `rank`, `gamesPlayed`, `nbGamesWithYou`, `declineInvitation`, `cancel`, `timeOut`, `drawOfferSent`, `drawOfferDeclined`, `drawOfferAccepted`, `drawOfferCanceled`, `whiteOffersDraw`, `blackOffersDraw`, `whiteDeclinesDraw`, `blackDeclinesDraw`, `yourOpponentOffersADraw`, `accept`, `decline`, `playingRightNow`, `finished`, `abortGame`, `gameAborted`, `standard`, `unlimited`, `mode`, `casual`, `rated`, `thisGameIsRated`, `rematch`, `rematchOfferSent`, `rematchOfferAccepted`, `rematchOfferCanceled`, `rematchOfferDeclined`, `cancelRematchOffer`, `viewRematch`, `play`, `inbox`, `chatRoom`, `spectatorRoom`, `composeMessage`, `noNewMessages`, `subject`, `recipient`, `send`, `incrementInSeconds`, `freeOnlineChess`, `spectators`, `nbWins`, `nbLosses`, `nbDraws`, `exportGames`, `ratingRange`, `giveNbSeconds`, `premoveEnabledClickAnywhereToCancel`, `thisPlayerUsesChessComputerAssistance`, `thisPlayerArtificiallyIncreasesTheirRating`, `opening`, `takeback`, `proposeATakeback`, `takebackPropositionSent`, `takebackPropositionDeclined`, `takebackPropositionAccepted`, `takebackPropositionCanceled`, `yourOpponentProposesATakeback`, `bookmarkThisGame`, `search`, `advancedSearch`, `tournament`, `tournaments`, `tournamentPoints`, `viewTournament`, `backToTournament`, `backToGame`, `freeOnlineChessGamePlayChessNowInACleanInterfaceNoRegistrationNoAdsNoPluginRequiredPlayChessWithComputerFriendsOrRandomOpponents`, `teams`, `nbMembers`, `allTeams`, `newTeam`, `myTeams`, `noTeamFound`, `joinTeam`, `quitTeam`, `anyoneCanJoin`, `aConfirmationIsRequiredToJoin`, `joiningPolicy`, `teamLeader`, `teamBestPlayers`, `teamRecentMembers`, `xJoinedTeamY`, `xCreatedTeamY`, `averageElo`, `location`, `settings`, `filterGames`, `reset`, `apply`, `leaderboard`, `pasteTheFenStringHere`, `pasteThePgnStringHere`, `fromPosition`, `continueFromHere`, `importGame`, `nbImportedGames`, `thisIsAChessCaptcha`, `clickOnTheBoardToMakeYourMove`, `notACheckmate`, `colorPlaysCheckmateInOne`, `retry`, `reconnecting`, `onlineFriends`, `noFriendsOnline`, `findFriends`, `favoriteOpponents`, `follow`, `following`, `unfollow`, `block`, `blocked`, `unblock`, `followsYou`, `xStartedFollowingY`, `nbFollowers`, `nbFollowing`, `more`, `memberSince`, `lastLogin`, `challengeToPlay`, `player`, `list`, `graph`, `lessThanNbMinutes`, `xToYMinutes`, `textIsTooShort`, `textIsTooLong`, `required`, `openTournaments`, `duration`, `winner`, `standing`, `createANewTournament`, `join`, `withdraw`, `points`, `wins`, `losses`, `winStreak`, `createdBy`, `tournamentIsStarting`, `membersOnly`, `boardEditor`, `startPosition`, `clearBoard`, `savePosition`, `loadPosition`, `isPrivate`, `reportXToModerators`, `profile`, `editProfile`, `firstName`, `lastName`, `biography`, `country`, `preferences`, `watchLichessTV`, `previouslyOnLichessTV`, `todaysLeaders`, `onlinePlayers`, `progressToday`, `progressThisWeek`, `progressThisMonth`, `leaderboardThisWeek`, `leaderboardThisMonth`, `activeToday`, `activeThisWeek`, `activePlayers`, `bewareTheGameIsRatedButHasNoClock`, `training`, `yourPuzzleRatingX`, `findTheBestMoveForWhite`, `findTheBestMoveForBlack`, `toTrackYourProgress`, `trainingSignupExplanation`, `recentlyPlayedPuzzles`, `puzzleId`, `puzzleOfTheDay`, `clickToSolve`, `goodMove`, `butYouCanDoBetter`, `bestMove`, `keepGoing`, `puzzleFailed`, `butYouCanKeepTrying`, `victory`, `giveUp`, `puzzleSolvedInXSeconds`, `wasThisPuzzleAnyGood`, `pleaseVotePuzzle`, `thankYou`, `ratingX`, `playedXTimes`, `fromGameLink`, `startTraining`, `continueTraining`, `retryThisPuzzle`, `thisPuzzleIsCorrect`, `thisPuzzleIsWrong`, `youHaveNbSecondsToMakeYourFirstMove`, `nbGamesInPlay`, `automaticallyProceedToNextGameAfterMoving`, `autoSwitch`, `openingId`, `yourOpeningRatingX`, `findNbStrongMoves`, `thisMoveGivesYourOpponentTheAdvantage`, `openingFailed`, `openingSolved`, `recentlyPlayedOpenings`, `puzzles`, `coordinates`, `openings`, `latestUpdates`, `tournamentWinners`, `name`, `description`, `no`, `yes`, `help`, `createANewTopic`, `topics`, `posts`, `lastPost`, `views`, `replies`, `replyToThisTopic`, `reply`, `message`, `createTheTopic`, `reportAUser`, `user`, `reason`, `whatIsIheMatter`, `cheat`, `insult`, `troll`, `other`, `reportDescriptionHelp`, `by`, `thisTopicIsNowClosed`, `theming`, `donate`, `blog`, `map`, `realTimeWorldMapOfChessMoves`, `questionsAndAnswers`, `notes`, `typePrivateNotesHere`, `gameDisplay`, `pieceAnimation`, `materialDifference`, `closeAccount`, `closeYourAccount`, `changedMindDoNotCloseAccount`, `closeAccountExplanation`, `thisAccountIsClosed`, `invalidUsernameOrPassword`, `emailMeALink`, `currentPassword`, `newPassword`, `newPasswordAgain`, `boardHighlights`, `pieceDestinations`, `boardCoordinates`, `moveListWhilePlaying`, `chessClock`, `tenthsOfSeconds`, `never`, `whenTimeRemainingLessThanTenSeconds`, `horizontalGreenProgressBars`, `soundWhenTimeGetsCritical`, `gameBehavior`, `premovesPlayingDuringOpponentTurn`, `takebacksWithOpponentApproval`, `promoteToQueenAutomatically`, `claimDrawOnThreefoldRepetitionAutomatically`, `privacy`, `letOtherPlayersFollowYou`, `letOtherPlayersChallengeYou`, `sound`, `soundControlInTheTopBarOfEveryPage`, `yourPreferencesHaveBeenSaved`, `none`, `fast`, `normal`, `slow`, `insideTheBoard`, `outsideTheBoard`, `onSlowGames`, `always`, `inCasualGamesOnly`, `whenPremoving`, `whenTimeRemainingLessThanThirtySeconds`, `difficultyEasy`, `difficultyNormal`, `difficultyHard`, `xLeftANoteOnY`, `xCompetesInY`, `xAskedY`, `xAnsweredY`, `xCommentedY`, `timeline`, `seeAllTournaments`, `starting`, `allInformationIsPublicAndOptional`, `yourCityRegionOrDepartment`, `biographyDescription`, `maximumNbCharacters`, `blocks`, `listBlockedPlayers`, `human`, `computer`, `side`, `clock`, `unauthorizedError`, `noInternetConnection`, `connectedToLichess`, `signedOut`, `loginSuccessful`, `playOnTheBoardOffline`, `playOfflineComputer`, `opponent`, `learn`, `community`, `tools`, `increment`, `board`, `pieces`, `sharePGN`, `playOnline`, `playOffline`, `allowAnalytics`, `shareGameURL`)

  lazy val count = keys.size
}
