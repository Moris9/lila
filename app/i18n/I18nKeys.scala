// Generated with bin/trans-dump
package lila.i18n

import lila.http.Context

import play.api.templates.Html
import play.api.i18n.Lang

final class I18nKeys(translator: Translator) {

  final class Key(val key: String) extends I18nKey {

    def apply(args: Any*)(implicit ctx: Context): Html =
      translator.html(key, args.toList)(ctx.req)

    def str(args: Any*)(implicit ctx: Context): String =
      translator.str(key, args.toList)(ctx.req)

    def to(lang: Lang)(args: Any*): String =
      translator.transTo(key, args.toList)(lang)
  }

  def untranslated(message: String) = Untranslated(message)

  val playWithAFriend = new Key("playWithAFriend")
  val inviteAFriendToPlayWithYou = new Key("inviteAFriendToPlayWithYou")
  val playWithTheMachine = new Key("playWithTheMachine")
  val challengeTheArtificialIntelligence = new Key("challengeTheArtificialIntelligence")
  val toInviteSomeoneToPlayGiveThisUrl = new Key("toInviteSomeoneToPlayGiveThisUrl")
  val gameOver = new Key("gameOver")
  val waitingForOpponent = new Key("waitingForOpponent")
  val waiting = new Key("waiting")
  val yourTurn = new Key("yourTurn")
  val aiNameLevelAiLevel = new Key("aiNameLevelAiLevel")
  val level = new Key("level")
  val toggleTheChat = new Key("toggleTheChat")
  val toggleSound = new Key("toggleSound")
  val chat = new Key("chat")
  val resign = new Key("resign")
  val checkmate = new Key("checkmate")
  val stalemate = new Key("stalemate")
  val white = new Key("white")
  val black = new Key("black")
  val createAGame = new Key("createAGame")
  val noGameAvailableRightNowCreateOne = new Key("noGameAvailableRightNowCreateOne")
  val whiteIsVictorious = new Key("whiteIsVictorious")
  val blackIsVictorious = new Key("blackIsVictorious")
  val playWithTheSameOpponentAgain = new Key("playWithTheSameOpponentAgain")
  val newOpponent = new Key("newOpponent")
  val playWithAnotherOpponent = new Key("playWithAnotherOpponent")
  val yourOpponentWantsToPlayANewGameWithYou = new Key("yourOpponentWantsToPlayANewGameWithYou")
  val joinTheGame = new Key("joinTheGame")
  val whitePlays = new Key("whitePlays")
  val blackPlays = new Key("blackPlays")
  val theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim = new Key("theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim")
  val makeYourOpponentResign = new Key("makeYourOpponentResign")
  val forceResignation = new Key("forceResignation")
  val talkInChat = new Key("talkInChat")
  val theFirstPersonToComeOnThisUrlWillPlayWithYou = new Key("theFirstPersonToComeOnThisUrlWillPlayWithYou")
  val whiteCreatesTheGame = new Key("whiteCreatesTheGame")
  val blackCreatesTheGame = new Key("blackCreatesTheGame")
  val whiteJoinsTheGame = new Key("whiteJoinsTheGame")
  val blackJoinsTheGame = new Key("blackJoinsTheGame")
  val whiteResigned = new Key("whiteResigned")
  val blackResigned = new Key("blackResigned")
  val whiteLeftTheGame = new Key("whiteLeftTheGame")
  val blackLeftTheGame = new Key("blackLeftTheGame")
  val shareThisUrlToLetSpectatorsSeeTheGame = new Key("shareThisUrlToLetSpectatorsSeeTheGame")
  val youAreViewingThisGameAsASpectator = new Key("youAreViewingThisGameAsASpectator")
  val replayAndAnalyse = new Key("replayAndAnalyse")
  val viewGameStats = new Key("viewGameStats")
  val flipBoard = new Key("flipBoard")
  val threefoldRepetition = new Key("threefoldRepetition")
  val claimADraw = new Key("claimADraw")
  val offerDraw = new Key("offerDraw")
  val draw = new Key("draw")
  val nbConnectedPlayers = new Key("nbConnectedPlayers")
  val talkAboutChessAndDiscussLichessFeaturesInTheForum = new Key("talkAboutChessAndDiscussLichessFeaturesInTheForum")
  val seeTheGamesBeingPlayedInRealTime = new Key("seeTheGamesBeingPlayedInRealTime")
  val gamesBeingPlayedRightNow = new Key("gamesBeingPlayedRightNow")
  val viewAllNbGames = new Key("viewAllNbGames")
  val viewNbCheckmates = new Key("viewNbCheckmates")
  val nbBookmarks = new Key("nbBookmarks")
  val nbPopularGames = new Key("nbPopularGames")
  val nbAnalysedGames = new Key("nbAnalysedGames")
  val bookmarkedByNbPlayers = new Key("bookmarkedByNbPlayers")
  val viewInFullSize = new Key("viewInFullSize")
  val logOut = new Key("logOut")
  val signIn = new Key("signIn")
  val signUp = new Key("signUp")
  val people = new Key("people")
  val games = new Key("games")
  val forum = new Key("forum")
  val chessPlayers = new Key("chessPlayers")
  val minutesPerSide = new Key("minutesPerSide")
  val variant = new Key("variant")
  val timeControl = new Key("timeControl")
  val start = new Key("start")
  val username = new Key("username")
  val password = new Key("password")
  val haveAnAccount = new Key("haveAnAccount")
  val allYouNeedIsAUsernameAndAPassword = new Key("allYouNeedIsAUsernameAndAPassword")
  val learnMoreAboutLichess = new Key("learnMoreAboutLichess")
  val rank = new Key("rank")
  val gamesPlayed = new Key("gamesPlayed")
  val declineInvitation = new Key("declineInvitation")
  val cancel = new Key("cancel")
  val timeOut = new Key("timeOut")
  val drawOfferSent = new Key("drawOfferSent")
  val drawOfferDeclined = new Key("drawOfferDeclined")
  val drawOfferAccepted = new Key("drawOfferAccepted")
  val drawOfferCanceled = new Key("drawOfferCanceled")
  val yourOpponentOffersADraw = new Key("yourOpponentOffersADraw")
  val accept = new Key("accept")
  val decline = new Key("decline")
  val playingRightNow = new Key("playingRightNow")
  val abortGame = new Key("abortGame")
  val gameAborted = new Key("gameAborted")
  val standard = new Key("standard")
  val unlimited = new Key("unlimited")
  val mode = new Key("mode")
  val casual = new Key("casual")
  val rated = new Key("rated")
  val thisGameIsRated = new Key("thisGameIsRated")
  val rematch = new Key("rematch")
  val rematchOfferSent = new Key("rematchOfferSent")
  val rematchOfferAccepted = new Key("rematchOfferAccepted")
  val rematchOfferCanceled = new Key("rematchOfferCanceled")
  val rematchOfferDeclined = new Key("rematchOfferDeclined")
  val cancelRematchOffer = new Key("cancelRematchOffer")
  val viewRematch = new Key("viewRematch")
  val play = new Key("play")
  val inbox = new Key("inbox")
  val chatRoom = new Key("chatRoom")
  val spectatorRoom = new Key("spectatorRoom")
  val composeMessage = new Key("composeMessage")
  val sentMessages = new Key("sentMessages")
  val incrementInSeconds = new Key("incrementInSeconds")
  val freeOnlineChess = new Key("freeOnlineChess")
  val spectators = new Key("spectators")
  val nbWins = new Key("nbWins")
  val nbLosses = new Key("nbLosses")
  val nbDraws = new Key("nbDraws")
  val exportGames = new Key("exportGames")
  val color = new Key("color")
  val eloRange = new Key("eloRange")
  val giveNbSeconds = new Key("giveNbSeconds")
  val searchAPlayer = new Key("searchAPlayer")
  val whoIsOnline = new Key("whoIsOnline")
  val allPlayers = new Key("allPlayers")
  val namedPlayers = new Key("namedPlayers")
  val premoveEnabledClickAnywhereToCancel = new Key("premoveEnabledClickAnywhereToCancel")
  val thisPlayerUsesChessComputerAssistance = new Key("thisPlayerUsesChessComputerAssistance")
  val opening = new Key("opening")
  val takeback = new Key("takeback")
  val proposeATakeback = new Key("proposeATakeback")
  val takebackPropositionSent = new Key("takebackPropositionSent")
  val takebackPropositionDeclined = new Key("takebackPropositionDeclined")
  val takebackPropositionAccepted = new Key("takebackPropositionAccepted")
  val takebackPropositionCanceled = new Key("takebackPropositionCanceled")
  val yourOpponentProposesATakeback = new Key("yourOpponentProposesATakeback")
  val bookmarkThisGame = new Key("bookmarkThisGame")
  val toggleBackground = new Key("toggleBackground")
  val advancedSearch = new Key("advancedSearch")
  val tournament = new Key("tournament")
  val freeOnlineChessGamePlayChessNowInACleanInterfaceNoRegistrationNoAdsNoPluginRequiredPlayChessWithComputerFriendsOrRandomOpponents = new Key("freeOnlineChessGamePlayChessNowInACleanInterfaceNoRegistrationNoAdsNoPluginRequiredPlayChessWithComputerFriendsOrRandomOpponents")
  val teams = new Key("teams")
  val nbMembers = new Key("nbMembers")
  val allTeams = new Key("allTeams")
  val newTeam = new Key("newTeam")
  val myTeams = new Key("myTeams")
  val noTeamFound = new Key("noTeamFound")
  val joinTeam = new Key("joinTeam")
  val quitTeam = new Key("quitTeam")
  val anyoneCanJoin = new Key("anyoneCanJoin")
  val aConfirmationIsRequiredToJoin = new Key("aConfirmationIsRequiredToJoin")
  val joiningPolicy = new Key("joiningPolicy")
  val teamLeader = new Key("teamLeader")
  val teamBestPlayers = new Key("teamBestPlayers")
  val teamRecentMembers = new Key("teamRecentMembers")
  val averageElo = new Key("averageElo")
  val location = new Key("location")
  val settings = new Key("settings")

  def keys = List(playWithAFriend, inviteAFriendToPlayWithYou, playWithTheMachine, challengeTheArtificialIntelligence, toInviteSomeoneToPlayGiveThisUrl, gameOver, waitingForOpponent, waiting, yourTurn, aiNameLevelAiLevel, level, toggleTheChat, toggleSound, chat, resign, checkmate, stalemate, white, black, createAGame, noGameAvailableRightNowCreateOne, whiteIsVictorious, blackIsVictorious, playWithTheSameOpponentAgain, newOpponent, playWithAnotherOpponent, yourOpponentWantsToPlayANewGameWithYou, joinTheGame, whitePlays, blackPlays, theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim, makeYourOpponentResign, forceResignation, talkInChat, theFirstPersonToComeOnThisUrlWillPlayWithYou, whiteCreatesTheGame, blackCreatesTheGame, whiteJoinsTheGame, blackJoinsTheGame, whiteResigned, blackResigned, whiteLeftTheGame, blackLeftTheGame, shareThisUrlToLetSpectatorsSeeTheGame, youAreViewingThisGameAsASpectator, replayAndAnalyse, viewGameStats, flipBoard, threefoldRepetition, claimADraw, offerDraw, draw, nbConnectedPlayers, talkAboutChessAndDiscussLichessFeaturesInTheForum, seeTheGamesBeingPlayedInRealTime, gamesBeingPlayedRightNow, viewAllNbGames, viewNbCheckmates, nbBookmarks, nbPopularGames, nbAnalysedGames, bookmarkedByNbPlayers, viewInFullSize, logOut, signIn, signUp, people, games, forum, chessPlayers, minutesPerSide, variant, timeControl, start, username, password, haveAnAccount, allYouNeedIsAUsernameAndAPassword, learnMoreAboutLichess, rank, gamesPlayed, declineInvitation, cancel, timeOut, drawOfferSent, drawOfferDeclined, drawOfferAccepted, drawOfferCanceled, yourOpponentOffersADraw, accept, decline, playingRightNow, abortGame, gameAborted, standard, unlimited, mode, casual, rated, thisGameIsRated, rematch, rematchOfferSent, rematchOfferAccepted, rematchOfferCanceled, rematchOfferDeclined, cancelRematchOffer, viewRematch, play, inbox, chatRoom, spectatorRoom, composeMessage, sentMessages, incrementInSeconds, freeOnlineChess, spectators, nbWins, nbLosses, nbDraws, exportGames, color, eloRange, giveNbSeconds, searchAPlayer, whoIsOnline, allPlayers, namedPlayers, premoveEnabledClickAnywhereToCancel, thisPlayerUsesChessComputerAssistance, opening, takeback, proposeATakeback, takebackPropositionSent, takebackPropositionDeclined, takebackPropositionAccepted, takebackPropositionCanceled, yourOpponentProposesATakeback, bookmarkThisGame, toggleBackground, advancedSearch, tournament, freeOnlineChessGamePlayChessNowInACleanInterfaceNoRegistrationNoAdsNoPluginRequiredPlayChessWithComputerFriendsOrRandomOpponents, teams, nbMembers, allTeams, newTeam, myTeams, noTeamFound, joinTeam, quitTeam, anyoneCanJoin, aConfirmationIsRequiredToJoin, joiningPolicy, teamLeader, teamBestPlayers, teamRecentMembers, averageElo, location, settings)
}
