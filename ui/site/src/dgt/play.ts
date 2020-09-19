import { Chess } from 'chessops/chess';
import { INITIAL_FEN, makeFen, parseFen } from 'chessops/fen';
import { makeSan, makeSanAndPlay, parseSan } from 'chessops/san';
import { Move, NormalMove } from 'chessops/types';
import { board } from 'chessops/debug';
import { defaultSetup, makeUci, parseUci } from 'chessops';

export default function (token: string) {

  const root = document.getElementById('dgt-play-zone') as HTMLDivElement;
  const consoleOutput = document.getElementById('dgt-play-zone-log') as HTMLPreElement;

  console.log(parseFen('rnbqkbnr/pppp1ppp/8/8/3pP3/2P5/PP3PPP/RNBQKBNR b KQkq - 1 3'));

  // and your code in here.

  /**
   * CONFIGURATION VALUES
   */
  const liveChessURL = localStorage.getItem('dgt-livechess-url');
  const announceAllMoves = (localStorage.getItem('dgt-speech-announce-all-moves') == "true");
  const verbose = (localStorage.getItem('dgt-verbose') == "true");
  const announceMoveFormat = localStorage.getItem('dgt-speech-announce-move-format') ? localStorage.getItem('dgt-speech-announce-move-format') : "san";
  const speechSynthesisOn = (localStorage.getItem('dgt-speech-synthesis') == "true");
  const voice = localStorage.getItem('dgt-speech-voice');
  var keywords = {
    "K": "King",
    "Q": "Queen",
    "R": "Rook",
    "B": "Bishop",
    "N": "Knight",
    "P": "Pawn",
    "x": "Takes",
    "+": "Check",
    "#": "Checkmate",
    "(=)": "Game ends in draw",
    "O-O-O": "Castles queenside",
    "O-O": "Castles kingside",
    "white": "White",
    "black": "Black",
    "wins by": "wins by",
    "timeout": "timeout",
    "resignation": "resignation",
    "illegal": "illegal",
    "move": "move"
  }
  try {
    var tempString = localStorage.getItem('dgt-speech-keywords');
    keywords = tempString && JSON.parse(tempString);
  } catch (error) {
    console.error("Invalid JSON Object for Speech Keywords. Using English default.");
  }

  //Lichess Integration with Board API

  /**
   * GLOBAL VATIABLES - Lichess Connectivity
   */
  var time = new Date(); //A Global time object
  var currentGameId = ''; //Track which is the current Game, in case there are several open games
  var currentGameColor = ''; //Track which color is being currently played by the player. 'white' or 'black'
  var me: { id: string; username: string }; //Track my information
  var gameInfoMap = new Map(); //A collection of key values to store game inmutable information of all open games
  var gameStateMap = new Map(); //A collection of key values to store the changing state of all open games
  var gameConnectionMap = new Map<string, { connected: boolean, lastEvent: number }>(); //A collection of key values to store the network status of a game
  var gameChessBoardMap = new Map<string, Chess>(); //A collection of chessops Boads representing the current board of the games
  var eventSteamStatus = { connected: false, lastEvent: time.getTime() }; //An object to store network status of the main eventStream
  const keywordsBase = ["K", "Q", "R", "B", "N", "P", "x", "+", "#", "(=)", "O-O-O", "O-O", "white", "black", "wins by", "timeout", "resignation"]
  var lastSanMove: { player: string, move: string, by: string }; //Track last move in SAN format . This is because there is no easy way to keep history of san moves
  /**
   * Global Variables for DGT Board Connection (JACM)
   */
  var localBoard: Chess = startingPosition(); //Board with valid moves played on Lichess and DGT Board. May be half move behind Lichess or half move in advance
  var DGTgameId = ''; //Used to track if DGT board was setup already with the lichess currentGameId
  var boards = Array<{ serialnr: string, state: string }>(); //An array to store all the board recognized by DGT LiveChess 
  var liveChessConnection: WebSocket; //Connection Object to LiveChess through websocket  
  var isLiveChessConnected = false; //Used to track if a board there is a connection to DGT Live Chess
  var currentSerialnr = '0'; //Public property to store the current serial number of the DGT Board in case there is more than one
  //subscription stores the information about the board being connected, most importantly the serialnr
  var subscription = { "id": 2, "call": "subscribe", "param": { "feed": "eboardevent", "id": 1, "param": { "serialnr": "" } } };
  /***
   * Bind console output to HTML pre Element
   */
  rewireLoggingToElement(consoleOutput, root, true);
  function rewireLoggingToElement(eleLocator: HTMLPreElement, eleOverflowLocator: HTMLDivElement, autoScroll: boolean) {
    //Clear the console
    eleLocator.innerHTML = "";
    //Bind to all types of console messages
    fixLoggingFunc('log');
    fixLoggingFunc('debug');
    fixLoggingFunc('warn');
    fixLoggingFunc('error');
    fixLoggingFunc('info');
    fixLoggingFunc('table');

    function fixLoggingFunc(name: string) {
      console['old' + name] = console[name];
      //Rewire function
      console[name] = function () {
        var output = "";
        for (let i = 0; i < arguments.length; i++) {
          let arg = arguments[i];
          output += "<span class=\"log-" + (typeof arg) + " log-" + name + "\">";
          if (
            typeof arg === "object" &&
            typeof JSON === "object" &&
            typeof JSON.stringify === "function"
          ) {
            output += JSON.stringify(arg);
          } else {
            output += arg;
          }
          output += "</span>&nbsp;";
        }
        if (output != "*" && output != ":")
          output += "<br>";
        if (autoScroll) {
          const isScrolledToBottom = eleOverflowLocator.scrollHeight - eleOverflowLocator.clientHeight <= eleOverflowLocator.scrollTop + 1;
          eleLocator.innerHTML += output;
          if (isScrolledToBottom) {
            eleOverflowLocator.scrollTop = eleOverflowLocator.scrollHeight - eleOverflowLocator.clientHeight;
          }
        } else {
          eleLocator.innerHTML += output;
        }
        //Call original function
        try {
          console['old' + name].apply(undefined, arguments);
        } catch {
          console['olderror'].apply(undefined, ['Error when loggin']);
        }
      };
    }
  }

  /**
   * Wait some time without blocking other code
   *
   * @param {number} ms - The number of milliseconds to sleep
   */
  function sleep(ms: number = 0) {
    return new Promise(r => setTimeout(r, ms));
  }


  /**
   * GET /api/account
   * 
   * Get my profile
   * 
   * Shows Public informations about the logged in user.
   * 
   * Example
   * {"id":"andrescavallin","username":"andrescavallin","online":true,"perfs":{"blitz":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"bullet":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"correspondence":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"classical":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"rapid":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true}},"createdAt":1599930231644,"seenAt":1599932744930,"playTime":{"total":0,"tv":0},"language":"en-US","url":"http://localhost:9663/@/andrescavallin","nbFollowing":0,"nbFollowers":0,"count":{"all":0,"rated":0,"ai":0,"draw":0,"drawH":0,"loss":0,"lossH":0,"win":0,"winH":0,"bookmark":0,"playing":0,"import":0,"me":0},"followable":true,"following":false,"blocking":false,"followsYou":false}
   * */
  function getProfile() {
    //Log intention
    if (verbose) console.log('getProfile - About to call /api/account');
    fetch('/api/account', {
      headers: { 'Authorization': 'Bearer ' + token }
    }).then(r => r.json()).then(
      data => {
        //Log raw data received
        if (verbose) console.log('/api/account Response:' + JSON.stringify(data));
        //Diplay Title + UserName . Title may be undefined
        console.log("\n");
        console.log("┌─────────────────────────────────────────────────────┐");
        console.log("│ " + (typeof (data.title) == "undefined" ? '' : data.title) + ' ' + data.username);
        //Display performance ratings
        console.table(data.perfs);
        //Store my profile
        me = data;
      })
      .catch(
        err => { console.error('getProfile - Error. ' + err.message) }
      );
  }

  /** 
    GET /api/stream/event
    Stream incoming events

    Stream the events reaching a lichess user in real time as ndjson.

    Each line is a JSON object containing a type field. Possible values are:

    challenge Incoming challenge
    gameStart Start of a game
    When the stream opens, all current challenges and games are sent.

    Examples:
    {"type":"gameStart","game":{"id":"kjKzl2MO"}}
    {"type":"challenge","challenge":{"id":"WTr3JNcm","status":"created","challenger":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1362,"provisional":true,"online":true,"lag":3},"destUser":{"id":"godking666","name":"Godking666","title":null,"rating":1910,"online":true,"lag":3},"variant":{"key":"standard","name":"Standard","short":"Std"},"rated":false,"speed":"rapid","timeControl":{"type":"clock","limit":900,"increment":10,"show":"15+10"},"color":"white","perf":{"icon":"#","name":"Rapid"}}}
 */
  async function connectToEventStream() {
    //Log intention
    if (verbose) console.log('connectToEventStream - About to call /api/stream/event');
    const response = await fetch('/api/stream/event', {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      if (verbose) console.log('connectToEventStream - Chunk received', value);
      //Update connection status
      eventSteamStatus = { connected: true, lastEvent: time.getTime() };
      //Response may contain several JSON objects on the same chunk separated by \n . This may create an empty element at the end.
      var JSONArray = value.split('\n');
      for (let i = 0; i < JSONArray.length; i++) {
        //Skip empty elements that may have happened witht the .split('\n')
        if (JSONArray[i].length > 2) {
          try {
            var data = JSON.parse(JSONArray[i]);
            //JSON data found, let's check if this is a game that started. field type is mandatory except on http 4xx
            if (data.type == "gameStart") {
              if (verbose) console.log('connectToEventStream - gameStart event arrived. GameId: ' + data.game.id);
              try {
                //Connect to that game's stream
                connectToGameStream(data.game.id);
              }
              catch (error) {
                //This will trigger if connectToGameStream fails
                console.error('connectToEventStream - Failed to connect to game stream. ' + Error(error).message);
              }
            }
            else if (data.type == "challenge") {
              //Challenge received
              //TODO
            }
            else if (response.status >= 400) {
              console.warn('connectToEventStream - ' + data.error);
            }
          }
          catch (error) {
            console.error('connectToEventStream - Unable to parse JSON or Unexpected error. ' + Error(error).message);
          }
        }
        else {
          //Signal that some empty message arrived. This is normal to keep the connection alive.
          if (verbose) console.log("*"); //process.stdout.write("*"); Replace to support browser
        }
      }
    }

    console.warn('connectToEventStream - Event Stream ended by server');
    //Update connection status
    eventSteamStatus = { connected: false, lastEvent: time.getTime() };
  }

  /**
  Stream Board game state
   
  GET /api/board/game/stream/{gameId}
   
  Stream the state of a game being played with the Board API, as ndjson.
  Use this endpoint to get updates about the game in real-time, with a single request.
  Each line is a JSON object containing a type field. Possible values are:
   
  gameFull Full game data. All values are immutable, except for the state field.
  gameState Current state of the game. Immutable values not included. Sent when a move is played, a draw is offered, or when the game ends.
  chatLine Chat message sent by a user in the room "player" or "spectator".
  The first line is always of type gameFull.
   
  Examples:
   
  New Game
  {"id":"972RKuuq","variant":{"key":"standard","name":"Standard","short":"Std"},"clock":{"initial":900000,"increment":10000},"speed":"rapid","perf":{"name":"Rapid"},"rated":false,"createdAt":1586647003562,"white":{"id":"godking666","name":"Godking666","title":null,"rating":1761},"black":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1362,"provisional":true},"initialFen":"startpos","type":"gameFull","state":{"type":"gameState","moves":"e2e4","wtime":900000,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}}
  First Move
  {"type":"gameState","moves":"e2e4","wtime":900000,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}
  Middle Game
  {"type":"gameState","moves":"e2e4 c7c6 g1f3 d7d5 e4e5 c8f5 d2d4 e7e6 h2h3 f5e4 b1d2 f8b4 c2c3 b4a5 d2e4 d5e4 f3d2 d8h4 g2g3 h4e7 d2e4 e7d7 e4d6 e8f8 d1f3 g8h6 c1h6 h8g8 h6g5 a5c7 e1c1 c7d6 e5d6 d7d6 g5f4 d6d5 f3d5 c6d5 f4d6 f8e8 d6b8 a8b8 f1b5 e8f8 h1e1 f8e7 d1d3 a7a6 b5a4 g8c8 a4b3 b7b5 b3d5 e7f8","wtime":903960,"btime":847860,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}
  After reconnect
  {"id":"ZQDjy4sa","variant":{"key":"standard","name":"Standard","short":"Std"},"clock":{"initial":900000,"increment":10000},"speed":"rapid","perf":{"name":"Rapid"},"rated":true,"createdAt":1586643869056,"white":{"id":"gg60","name":"gg60","title":null,"rating":1509},"black":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1433,"provisional":true},"initialFen":"startpos","type":"gameFull","state":{"type":"gameState","moves":"e2e4 c7c6 g1f3 d7d5 e4e5 c8f5 d2d4 e7e6 h2h3 f5e4 b1d2 f8b4 c2c3 b4a5 d2e4 d5e4 f3d2 d8h4 g2g3 h4e7 d2e4 e7d7 e4d6 e8f8 d1f3 g8h6 c1h6 h8g8 h6g5 a5c7 e1c1 c7d6 e5d6 d7d6 g5f4 d6d5 f3d5 c6d5 f4d6 f8e8 d6b8 a8b8 f1b5 e8f8 h1e1 f8e7 d1d3 a7a6 b5a4 g8c8 a4b3 b7b5 b3d5 e7f8 d5b3 a6a5 a2a3 a5a4 b3a2 f7f6 e1e6 f8f7 e6b6","wtime":912940,"btime":821720,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"resign","winner":"white"}}
  Draw Offered
  {"type":"gameState","moves":"e2e4 c7c6","wtime":880580,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":true,"status":"started"}
  After draw accepted
  {"type":"gameState","moves":"e2e4 c7c6","wtime":865460,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"draw"}
  Out of Time
  {"type":"gameState","moves":"e2e3 e7e5","wtime":0,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"outoftime","winner":"black"}
  Mate
  {"type":"gameState","moves":"e2e4 e7e5 f1c4 d7d6 d1f3 b8c6 f3f7","wtime":900480,"btime":907720,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"mate"}
  Promotion
  {"type":"gameState","moves":"e2e4 b8c6 g1f3 c6d4 f1c4 e7e5 d2d3 d7d5 f3d4 f7f6 c4d5 f6f5 f2f3 g7g6 e1g1 c7c6 d5b3 d8d5 e4d5 a8b8 d4e6 f8b4 e6c7 e8e7 d5d6 e7f6 d6d7 b4f8 d7d8q","wtime":2147483647,"btime":2147483647,"winc":0,"binc":0,"wdraw":false,"bdraw":false,"status":"started"}
  @param {string} gameId - The alphanumeric identifier of the game to be tracked
   */

  async function connectToGameStream(gameId: string) {
    //Log intention
    if (verbose) console.log('connectToGameStream - About to call /api/board/game/stream/' + gameId);
    const response = await fetch('/api/board/game/stream/' + gameId, {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();
    while (reader) { //while (true)
      const { value, done } = await reader.read();
      if (done) break;
      //Log raw data received
      if (verbose) console.log('connectToGameStream - board game stream recevied:', value);
      //Update connection status
      gameConnectionMap.set(gameId, { connected: true, lastEvent: time.getTime() });
      //Response may contain several JSON objects on the same chunk separated by \n . This may create an empty element at the end.
      var JSONArray = value.split('\n');
      for (let i = 0; i < JSONArray.length; i++) {
        //Skip empty elements that may have happened witht the .split('\n')
        if (JSONArray[i].length > 2) {
          try {
            var data = JSON.parse(JSONArray[i]);
            //The first line is always of type gameFull.
            if (data.type == "gameFull") {
              if (!verbose) console.clear();
              //Log game Summary 
              //logGameSummary(data);
              //Store game inmutable information on the gameInfoMap dictionary collection 
              gameInfoMap.set(gameId, data);
              //Store game state on the gameStateMap dictionary collection
              gameStateMap.set(gameId, data.state);
              //Update the ChessBoard to the ChessBoard Map
              initializeChessBoard(gameId, data);
              //Log the state. Note that we are doing this after storing the state and initializing the chessops board
              logGameState(gameId);
              //Call chooseCurrentGame to determine if this stream will be the new current game
              chooseCurrentGame();
            }
            else if (data.type == "gameState") {
              if (!verbose) console.clear();
              //Update the ChessBoard Map
              updateChessBoard(gameId, gameStateMap.get(gameId), data);
              //Update game state with most recent state
              gameStateMap.set(gameId, data);
              //Log the state. Note that we are doing this after storing the state and updating the chessops board
              logGameState(gameId);
            }
            else if (data.type == "chatLine") {
              //Received chat line
              //TODO
            }
            else if (response.status >= 400) {
              console.log('connectToGameStream - ' + data.error);
            }
          }
          catch (error) {
            console.error('connectToGameStream - No valid game data or Unexpected error. ' + Error(error).message);
          }
        }
        else {
          //Signal that some empty message arrived
          if (verbose) console.log(":"); //process.stdout.write(":"); Changed to support browser
        }
      }
    }
    //End Stream output.end();
    console.warn('connectToGameStream - Game ' + gameId + ' Stream ended.');
    //Update connection state
    gameConnectionMap.set(gameId, { connected: false, lastEvent: time.getTime() });
  }

  /**
   * Return a string representation of the remaining time on the clock
   * 
   * @param {number} timer - Numeric representation of remaining time
   * 
   * @returns {String} - String representation of numeric time
   */
  function formattedTimer(timer: number): string {
    // Pad function to pad with 0 to 2 or 3 digits, default is 2
    var pad = (n: number, z = 2) => (`00${n}`).slice(-z);
    return pad(timer / 3.6e6 | 0) + ':' + pad((timer % 3.6e6) / 6e4 | 0) + ':' + pad((timer % 6e4) / 1000 | 0) //+ '.' + pad(timer % 1000, 3);
  }

  /**
  * mainLoop() is a function that tries to keep the streams connected at all times, up to a maximum of 20 retries
  */
  async function lichessConnectionLoop() {
    //Program ends after 20 re-connection attempts
    for (let attempts = 0; attempts < 20; attempts++) {
      //Connect to main event stream
      connectToEventStream();
      //On the first time, if there are no games, it may take several seconds to receive data so lets wait a bit. Also give some time to connect to started games
      await sleep(5000);
      //Now enter a loop to monitor the connection
      do {
        //sleep 5 seconds and just listen to events
        await sleep(5000);
        //Check if any started games are disconnected
        for (let [gameId, networkState] of gameConnectionMap) {
          if (!networkState.connected && gameStateMap.get(gameId).status == "started") {
            //Game is not conencted and has not finished, reconnect
            if (verbose) console.log(`Started game is disconnected. Attempting reconnection for gameId: ${gameId}`);
            connectToGameStream(gameId);
          }
        }
      }
      while (eventSteamStatus.connected)
      //This means event stream is not connected
      console.warn("No connection to event stream. Attempting re-connection. Attempt: " + attempts);
    }
    console.error("No connection to event stream after maximum number of attempts 20. Reload page to start again.");

  }

  /**
   * This function will update the currentGameId with a valid active game
   * and then will attach this game to the DGT Board
   * It requires that all maps are up to date: gameInfoMap, gameStateMap, gameConnectionMap and gameChessBoardMap
   */
  async function chooseCurrentGame() {
    //Determine new value for currentGameId. First create an array with only the started games
    //So then there is none or more than one started game
    var playableGames = playableGamesArray();
    //If there is only one started game, then its easy
    if (playableGames.length == 1) {
      currentGameId = playableGames[0].gameId;
      attachCurrentGameIdToDGTBoard(); //Let the board know which color the player is actually playing and setup the position
      console.log('Active game updated. currentGameId: ' + currentGameId);
    }
    else if (playableGames.length == 0) {
      console.log('No started playable games, challenges or games are disconnected. Please start a new game or fix connection.');
      //TODO What happens if the games reconnect and this move is not sent?
    }
    else {
      console.table(playableGames);
      //TODO
      //If more than one game is playable player must pick one.
      //for now alert user
      console.warn('Currently only one game is supported. Ability to select game coming soon. Defaulting to latest.')
      let index = playableGames.length - 1;
      currentGameId = playableGames[Number(index)].gameId;
      //Command looks good for currentGameId 
      attachCurrentGameIdToDGTBoard(); //Let the board know which color the player is actually playing and setup the position
      console.log('Active game updated. currentGameId: ' + currentGameId);
    }
  }


  /**
   * Initialize a ChessBoard when connecting or re-connecting to a game
   * 
   * @param {string} gameId - The gameId of the game to store on the board
   * @param {Object} data - The gameFull event from lichess.org
   */
  function initializeChessBoard(gameId: string, data: { initialFen: string; state: { moves: string; }; }) {
    try {
      var initialFen: string = INITIAL_FEN;
      if (data.initialFen != "startpos")
        initialFen = data.initialFen;
      var setup = parseFen(initialFen).unwrap();
      var chess: Chess = Chess.fromSetup(setup).unwrap();
      var moves = data.state.moves.split(' ');
      for (let i = 0; i < moves.length; i++) {
        if (moves[i] != '') {
          //Make any move that may have been already played on the ChessBoard. Useful when reconnecting
          var uciMove = parseUci(moves[i]);
          if (uciMove && chess.isLegal(uciMove))
            chess.play(uciMove);
        }
      }
      //Store the ChessBoard on the ChessBoardMap
      gameChessBoardMap.set(gameId, chess);
      if (verbose) console.log(`initializeChessBoard - New Board for gameId: ${gameId}`);
      if (verbose) console.log(board(chess.board));
      if (verbose) console.log(chess.turn + "'s turn");
    }
    catch (error) {
      console.error(`initializeChessBoard - Error: ${error.message}`);
    }
  }

  /**
  * Update the ChessBoard for the specified gameId with the new moves on newState since the last stored state
  * 
  * @param {string} gameId - The gameId of the game to store on the board
  * @param {Object} currentState - The state stored on the gameStateMap
  * @param {Object} newState - The new state not yet stored
  */
  function updateChessBoard(gameId: string, currentState: { moves: string }, newState: { moves: string; }) {
    try {
      var chess = gameChessBoardMap.get(gameId);
      if (chess) {
        var pendingMoves: string
        if (!currentState.moves) {
          //No prior moves. Use the new moves
          pendingMoves = newState.moves;
        }
        else {
          //Get all the moves on the newState that are not present on the currentState
          pendingMoves = newState.moves.substring(currentState.moves.length, newState.moves.length);
        }
        var moves = pendingMoves.split(' ');
        for (let i = 0; i < moves.length; i++) {
          if (moves[i] != '') {
            //Make the new move
            var uciMove = parseUci(moves[i]);
            if (uciMove && chess.isLegal(uciMove)) {
              //This is a good chance to get the move in SAN format
              if (chess.turn == 'black')
                lastSanMove = { player: 'black', move: makeSan(chess, uciMove), by: gameInfoMap.get(currentGameId).black.id };
              else
                lastSanMove = { player: 'white', move: makeSan(chess, uciMove), by: gameInfoMap.get(currentGameId).white.id };
              chess.play(uciMove);
            }
          }
        }
        //Store the ChessBoard on the ChessBoardMap
        if (verbose) console.log(`updateChessBoard - Updated Board for gameId: ${gameId}`);
        if (verbose) console.log(board(chess.board));
        if (verbose) console.log(chess.turn + "'s turn");
      }
    }
    catch (error) {
      console.error(`updateChessBoard - Error: ${error.message}`);
    }
  }

  /**
  * Utility function to update which color is being played with the board
  */
  function attachCurrentGameIdToDGTBoard() {
    //Every times a new game is connected clear the console
    consoleOutput.innerHTML = "";
    //
    if (me.id == gameInfoMap.get(currentGameId).white.id)
      currentGameColor = 'white';
    else
      currentGameColor = 'black';
    //Send the position to LiveChess for synchronization
    sendBoardToLiveChess(gameChessBoardMap.get(currentGameId));
  }

  /**
   * Iterate the gameConnectionMap dictionary and return an arrays containing only the games that can be played with the board
   * @returns {Array} - Array containing a summary of playable games
   */
  function playableGamesArray(): Array<{
    'gameId': string,
    'versus': string,
    'vs rating': string,
    'game rating': string,
    'Timer': string,
    'Last Move': string
  }> {
    var playableGames: Array<{
      'gameId': string,
      'versus': string,
      'vs rating': string,
      'game rating': string,
      'Timer': string,
      'Last Move': string
    }> = [];
    var keys = Array.from(gameConnectionMap.keys());
    //The for each iterator is not used since we don't want to continue execution. We want a syncrhonous result
    //for (let [gameId, networkState] of gameConnectionMap) {
    //    if (gameConnectionMap.get(gameId).connected && gameStateMap.get(gameId).status == "started") {    
    for (var i = 0; i < keys.length; i++) {
      if (gameConnectionMap.get(keys[i]).connected && gameStateMap.get(keys[i]).status == "started") {
        //Game is good for commands
        var gameInfo = gameInfoMap.get(keys[i]);
        //var gameState = gameStateMap.get(keys[i]);
        var lastMove = getLastUCIMove(keys[i]);
        var versus = (gameInfo.black.id == me.id) ? ((gameInfo.white.title !== null) ? gameInfo.white.title : '@') + ' ' + gameInfo.white.name : ((gameInfo.black.title !== null) ? gameInfo.black.title : '@') + ' ' + gameInfo.black.name;
        playableGames.push({
          'gameId': gameInfo.id,
          'versus': versus,
          'vs rating': (gameInfo.black.id == me.id) ? gameInfo.white.rating : gameInfo.black.rating,
          'game rating': gameInfo.variant.short + ' ' + (gameInfo.rated ? 'rated' : 'unrated'),
          'Timer': gameInfo.speed + ' ' + ((gameInfo.clock !== null) ? (String(gameInfo.clock.initial / 60000) + "'+" + String(gameInfo.clock.increment / 1000) + "''") : '∞'),
          'Last Move': lastMove.player + ' ' + lastMove.move + ' by ' + lastMove.by
        })
      }
    }
    return playableGames;
  }

  /**
   * Display the state as stored in the Dictionary collection
   *
   * @param {string} gameId - The alphanumeric identifier of the game for which state is going to be shown
   */
  function logGameState(gameId: string) {
    if (gameStateMap.has(gameId) && gameInfoMap.has(gameId)) {
      var gameInfo = gameInfoMap.get(gameId);
      var gameState = gameStateMap.get(gameId);
      var lastMove = getLastUCIMove(gameId);
      console.log(""); //process.stdout.write("\n"); Changed to support browser
      console.table({
        'Title': { white: ((gameInfo.white.title !== null) ? gameInfo.white.title : '@'), black: ((gameInfo.black.title !== null) ? gameInfo.black.title : '@'), game: 'Id: ' + gameInfo.id },
        'Username': { white: gameInfo.white.name, black: gameInfo.black.name, game: 'Status: ' + gameState.status },
        'Rating': { white: gameInfo.white.rating, black: gameInfo.black.rating, game: gameInfo.variant.short + ' ' + (gameInfo.rated ? 'rated' : 'unrated') },
        'Timer': { white: formattedTimer(gameState.wtime), black: formattedTimer(gameState.btime), game: gameInfo.speed + ' ' + ((gameInfo.clock !== null) ? (String(gameInfo.clock.initial / 60000) + "'+" + String(gameInfo.clock.increment / 1000) + "''") : '∞') },
        'Last Move': { white: (lastMove.player == 'white' ? lastMove.move : '?'), black: (lastMove.player == 'black' ? lastMove.move : '?'), game: lastMove.player },
      });
      switch (gameState.status) {
        case "started":
          //Announce the last move
          if (me.id !== lastMove.by || announceAllMoves) {
            announcePlay(lastMove, gameState.wtime, gameState.btime);
          }
          break;
        case "outoftime":
          announceWinner(keywords[gameState.winner], 'flag', keywords[gameState.winner] + ' ' + keywords['wins by'] + ' ' + keywords['timeout']);
          break;
        case "resign":
          announceWinner(keywords[gameState.winner], 'resign', keywords[gameState.winner] + ' ' + keywords['wins by'] + ' ' + keywords['resignation']);
          break;
        case "mate":
          announceWinner(keywords[lastMove.player], 'mate', keywords[lastMove.player] + ' ' + keywords['wins by'] + ' ' + keywords['#']);
          break;
        case "draw":
          announceWinner('draw', 'draw', keywords['(=)']);
          break;
        default:
          console.log(`Unknown status received: ${gameState.status}`);
      }
    }
  }



  /**
   * Peeks a game state and calculates who played the last move and what move it was
   * 
   * @param {string} gameId - The alphanumeric identifier of the game where the last move is going to be calculated
   * 
   * @return {Object} - The move in JSON
   */
  function getLastUCIMove(gameId: string): { player: string, move: string, by: string } {
    if (gameStateMap.has(gameId) && gameInfoMap.has(gameId)) {
      var gameInfo = gameInfoMap.get(gameId);
      var gameState = gameStateMap.get(gameId);
      //This is the original code that does not used chessops objects and can be used to get the UCI move but not SAN.
      if (String(gameState.moves).length > 1) {
        var moves = gameState.moves.split(' ');
        if (verbose) console.log(`getLastUCIMove - ${moves.length} moves detected. Last one: ${moves[moves.length - 1]}`);
        if (moves.length % 2 == 0)
          return { player: 'black', move: moves[moves.length - 1], by: gameInfo.black.id };
        else
          return { player: 'white', move: moves[moves.length - 1], by: gameInfo.white.id };
      }
    }
    if (verbose) console.log('getLastUCIMove - No moves.');
    return { player: 'none', move: 'none', by: 'none' };
  }

  /**
   * Fedback the user about the detected moved
   * 
   * @param lastMove JSON object with the move information
   * @param wtime Remaining time for white
   * @param btime Remaining time for black
   */
  function announcePlay(lastMove: { player: string, move: string, by: string }, wtime: number, btime: number) {
    //ttsSay(lastMove.player);
    //Now play it using text to speech library
    var moveText: string;
    if (announceMoveFormat && announceMoveFormat.toLowerCase() == "san" && lastSanMove) {
      moveText = lastSanMove.move;
      ttsSay(raplaceKeywords(lastSanMove.move));
    }
    else {
      moveText = lastMove.move;
      ttsSay(lastMove.move);
    }
    if (lastMove.player == 'white') {
      console.log('<span class="dgt-white-move">' + moveText + ' by White' + '</span>');
    }
    else {
      console.log('<span class="dgt-black-move">' + moveText + ' by Black' + '</span>');
    }    
    //TODO
    //Give feedback on running out of time
  }

  function announceWinner(winner, status, message) {
    if (winner == 'white') {
      console.log('  ' + status + '  -  ' + message);
    }
    else {
      console.log('  ' + status + '  -  ' + message);
    }
    //Now play message using text to speech library
    ttsSay(message);
  }

  function announceInvalidMove() {
    if (currentGameColor == 'white') {
      console.warn('  [ X X ]  - Illegal move by white.')
    }
    else {
      console.warn('  [ X X ]  - Illegal move by black.')
    }
    //Now play it using text to speech library
    ttsSay('Illegal Move');
  }


  async function connectToLiveChess() {
    var SANMove: string; //a move in san format returned by liveChess
    //Open the WebSocket
    liveChessConnection = new WebSocket((liveChessURL) ? liveChessURL : "ws://localhost:1982/api/v1.0");

    //Attach Events
    liveChessConnection.onopen = () => {
      isLiveChessConnected = true;
      if (verbose) console.info("Websocket onopen: Connection to LiveChess was sucessful")
      liveChessConnection.send('{"id":1,"call":"eboards"}');
    }

    liveChessConnection.onerror = () => {
      console.error("Websocket ERROR: ");
    }

    liveChessConnection.onclose = () => {
      console.error('Websocket to LiveChess disconnected');
      //Clear the value of current serial number this serves as a diconnected status
      currentSerialnr = '0';
      //Set connection state to false
      isLiveChessConnected = false;
    };

    liveChessConnection.onmessage = async e => {
      if (verbose) console.info('Websocket onmessage with data:' + e.data);
      var message = JSON.parse(e.data);
      if (message.response == 'call' && message.id == '1') {
        //Get the list of availble boards on LiveChess
        boards = message.param;
        console.table(boards)
        if (verbose) console.info(boards[0].serialnr)
        //TODO 
        //we need to be able to handle more than one board
        //for now using the first board found
        //Update the base subscription message with the serial number
        currentSerialnr = boards[0].serialnr;
        subscription.param.param.serialnr = currentSerialnr;
        if (verbose) console.info('Websocket onmessage[call]: board serial number updated to: ' + currentSerialnr);
        if (verbose) console.info('Webscoket - about to send the following message \n' + JSON.stringify(subscription));
        liveChessConnection.send(JSON.stringify(subscription))
        //Check if the board is properly connected
        if (boards[0].state != "ACTIVE" && boards[0].state != "INACTIVE") // "NOTRESPONDING" || "DELAYED"
          console.error(`Board with serial ${currentSerialnr} is not properly connected. Please fix`);
        //Send setup with stating position

        if (gameStateMap.has(currentGameId) && gameConnectionMap.get(currentGameId).connected && gameStateMap.get(currentGameId).status == "started") {
          //There is a game in progress, setup the board as per lichess board
          if (currentGameId != DGTgameId) {
            //We know we have not synchronized yet
            if (verbose) console.info('There is a game in progress, calling liveChessBoardSetUp...');
            sendBoardToLiveChess(gameChessBoardMap.get(currentGameId));
          }
        }
      }
      else if (message.response == 'feed' && !(!message.param.san)) {
        //Received move from board
        if (verbose) console.info('onmessage - san: ' + message.param.san)
        //get last move known to lichess and avoid calling multiple times this function
        var lastMove = getLastUCIMove(currentGameId);
        if (message.param.san.length == 0) {
          if (verbose) console.info('onmessage - san is empty')
        }
        else {
          //A move was received
          SANMove = String(message.param.san[message.param.san.length - 1]).trim();
          if (verbose) console.info('onmessage - SANMove = ' + SANMove);
          var moveObject: NormalMove; //a move in chessops format
          moveObject = parseSan(localBoard, SANMove); //get move from DGT LiveChess
          //if valid move on local chessops
          if (localBoard.isLegal(moveObject)) {
            if (verbose) console.info('onmessage - Move is legal');
            //if received move.color == this.currentGameColor
            if (localBoard.turn == currentGameColor) {
              //This is a valid new move send it to lichess
              if (verbose) console.info('onmessage - Valid Move played: ' + SANMove)
              await validateAndSendBoardMove(moveObject);
              //Update the lastSanMove
              lastSanMove = { player: localBoard.turn , move: SANMove, by: me.id }              
              //Play the move on local board to keep it in sync
              localBoard.play(moveObject);
            }
            else if (lastMove.move == makeUci(moveObject)) {
              //This is a valid adjustment - Just making the move from Lichess
              if (verbose) console.info('onmessage - Valid Adjustment: ' + SANMove);
              //no need to send anything to Lichess moveObject required
              //Play the move on local board to keep it in sync
              localBoard.play(moveObject);
            }
            else {
              //Invalid Adjustment. Move was legal but does not match last move received from Lichess
              console.error('onmessage - Invalid Adjustment was made');
              if (lastMove.move == makeUci(moveObject)) {
                console.error('onmessage - Played move has not been received by Lichess.');
              } else {
                console.error('onmessage - Expected:' + lastMove.move + ' by ' + lastMove.player);
                console.error('onmessage - Detected:' + makeUci(moveObject) + ' by ' + localBoard.turn);
              }
              announceInvalidMove();
              await sleep(1000);
              //Repeat last game state announcement
              var gameState = gameStateMap.get(currentGameId);
              announcePlay(lastMove, gameState.wtime, gameState.btime);
            }
          }
          else {
            //Move was valid on DGT Board but not ilegal on localBoard
            if (verbose) console.info('onmessage - Move is NOT legal');
            if (lastMove.move == makeUci(moveObject)) {
              //This is fine, the same last move was received again and seems ilegal
              if (verbose) console.warn('onmessage - Move received is the same as the last moved played: ' + SANMove)
            }
            else {
              //Receiving a legal move on DGT Board but invalid move on localBoard signals a de-sycnhronization
              if (verbose) console.error('onmessage - invalidMove - Position Mismatch between DGT Board and internal in memory Board . SAN: ' + SANMove);
              announceInvalidMove();
              console.info(board(localBoard.board));
            }
          }
        }
      }
      else if (message.response == 'feed') {
        //feed received but not san
        //No moves received, this may be an out of snc problem or just the starting position
        if (verbose) console.info('onmessage - No move received on feed event.');
        //TODO THIS MAY REQUIRE RE-SYNCHRONIZATION BETWEEN LICHESS AND DGT BOARD
      }
    }
  }

  async function DGTliveChessConnectionLoop() {
    //Attempt connection right away
    connectToLiveChess();
    //Program ends after 20 re-connection attempts
    for (let attempts = 0; attempts < 20; attempts++) {
      do {
        //Just sleep five seconds while there is a valid currentSerialnr
        await sleep(5000);
      }
      while (currentSerialnr != '0' && isLiveChessConnected)
      //currentSerialnr is 0 so still no connection to board. Retry
      if (!isLiveChessConnected) {
        console.warn("No connection to DGT Live Chess. Attempting re-connection. Attempt: " + attempts);
        connectToLiveChess();
      }
      else {
        //Websocket is fine but still no board detected
        console.warn("Connection to DGT Live Chess is Fine but no board is detected. Attempting re-connection. Attempt: " + attempts);
        liveChessConnection.send('{"id":1,"call":"eboards"}');
      }
    }
    console.error("No connection to DGT Live Chess after maximum number of attempts 20. Reload page to start again.");
  }

  /**
   * Syncrhonizes the position on Lichess with the position on the board
   * If the position does not match, no moves will be received from LiveChess
   * @param chess - The chessops Chess object with the position on Lichess
   */
  async function sendBoardToLiveChess(chess: Chess) {
    var fen = makeFen(chess.toSetup());
    var setupMessage = {
      "id": 3,
      "call": "call",
      "param": {
        "id": 1,
        "method": "setup",
        "param": {
          "fen": fen
        }
      }
    }
    if (verbose) console.log("setUp -: " + JSON.stringify(setupMessage))
    if (isLiveChessConnected) {
      liveChessConnection.send(JSON.stringify(setupMessage))
      //Store the gameId so we now we already synchronized
      DGTgameId = currentGameId;
      //Initialize localBoard too so it matched what was sent to LiveChess
      localBoard = gameChessBoardMap.get(currentGameId).clone();
    }
    else {
      console.error("WebSocket is not open - cannot send setup command.");
    }
  }

  /**
  * This functions hanldes the sending the move to the right lichess game. 
  * If more than one game is being played, it will ask which game to connect to,
  * thus waiting for user input and so, it becomes async
  * 
  * @param {Object} boardMove - The move in chessops format or string if in lichess format
  */
  async function validateAndSendBoardMove(boardMove: NormalMove) {
    //While there is not and active game, keep trying to find one so the move is not lost
    while (!(gameStateMap.has(currentGameId) && gameConnectionMap.get(currentGameId).connected && gameStateMap.get(currentGameId).status == "started")) {
      //Wait a few seconds to see if the games reconnects or starts and give some space to other code to run
      console.warn('validateAndSendBoardMove - Cannot send move while disconnected. Re-Trying in 2 seconds...')
      await sleep(2000);
      //Now attempt to select for which game is this command intented
      await chooseCurrentGame();
    }
    //Now send the move
    var command = makeUci(boardMove);
    sendMove(currentGameId, command);
  }

  /**
   * Make a Board move
   * 
   * /api/board/game/{gameId}/move/{move}
   * 
   * Make a move in a game being played with the Board API.
   * The move can also contain a draw offer/agreement.
   * 
   * @param {string} gameId - The gameId for the active game
   * @param {string} uciMove - The move un UCI format 
   */
  function sendMove(gameId: string, uciMove: string) {
    //prevent sending empty moves
    if (uciMove.length > 1) {
      //Log intention
      //Automatically decline draws when making a move
      var url = `/api/board/game/${gameId}/move/${uciMove}?offeringDraw=false`;
      if (verbose) console.log('sendMove - About to call ' + url);
      fetch(url, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token }
      }).then(response => {
        try {
          if (response.status == 200 || response.status == 201) {
            //Move sucessfully sent
            if (verbose) console.log('sendMove - Move sucessfully sent.');
          }
          else {
            response.json().then((errorJson) => {
              console.error('sendMove - Failed to send move. ' + errorJson.error);
            })
          }
        }
        catch (error) {
          console.error('sendMove - Unexpected error. ' + error);
        }

      })
        .catch(
          err => { console.error('sendMove - Error. ' + err.message) }
        );
    }
  }

  /**
   * Replaces letters with full name of the pieces or move name
   * @param sanMove The move in san format
   */
  function raplaceKeywords(sanMove) {
    var extendedSanMove = sanMove;
    for (let i = 0; i < keywordsBase.length; i++) {
      try {
        extendedSanMove = extendedSanMove.replace(keywordsBase[i], ' ' + keywords[keywordsBase[i]] + ' ');
      } catch (error) {
        console.error(`raplaceKeywords - Error replacing keyword. ${keywordsBase[i]} . ${Error(error).message}`);
      }
    }
    return extendedSanMove;
  }


  /**
   * GLOBAL VATIABLES
   */
  async function ttsSay(text: string) {
    //Check Voice is disables
    if (!speechSynthesisOn) return;
    if (verbose) console.log('TTS - for text: ' + text);
    var utterThis = new SpeechSynthesisUtterance(text);
    var selectedOption = voice;
    var availableVoices = speechSynthesis.getVoices();
    for (let i = 0; i < availableVoices.length; i++) {
      if (availableVoices[i].name === selectedOption) {
        utterThis.voice = availableVoices[i];
        break;
      }
    }
    //utterThis.pitch = pitch.value;
    utterThis.rate = .60;
    speechSynthesis.speak(utterThis);
  }

  function start() {
      console.log("");
      console.log("      ,....,                      ▄████▄   ██░ ██ ▓█████   ██████   ██████     ");
      console.log("     ,::::::<                    ▒██▀ ▀█  ▓██░ ██▒▓█   ▀ ▒██    ▒ ▒██    ▒     ");
      console.log("    ,::/^\\\"``.                   ▒▓█    ▄ ▒██▀▀██░▒███   ░ ▓██▄   ░ ▓██▄       ");
      console.log("   ,::/, `   e`.                 ▒▓▓▄ ▄██▒░▓█ ░██ ▒▓█  ▄   ▒   ██▒  ▒   ██▒    ");
      console.log("  ,::; |        '.               ▒ ▓███▀ ░░▓█▒░██▓░▒████▒▒██████▒▒▒██████▒▒    ");
      console.log("  ,::|  \___,-.  c)               ░ ░▒ ▒  ░ ▒ ░░▒░▒░░ ▒░ ░▒ ▒▓▒ ▒ ░▒ ▒▓▒ ▒ ░    ");
      console.log("  ;::|     \\   '-'               ░  ▒    ▒ ░▒░ ░ ░ ░  ░░ ░▒  ░ ░░ ░▒  ░ ░      ");
      console.log("  ;::|      \\                    ░         ░  ░░ ░   ░   ░  ░  ░  ░  ░  ░      ");
      console.log("  ;::|   _.=`\\                   ░ ░       ░  ░  ░   ░  ░      ░        ░      ");
      console.log("  `;:|.=` _.=`\\                  ░                                             ");
      console.log("    '|_.=`   __\\                                                               ");
      console.log("    `\\_..==`` /                 Lichess.org - DGT Electronic Board Connector   ");
      console.log("     .'.___.-'.                Developed by Andres Cavallin and Juan Cavallin  ");
      console.log("    /          \\                                                               ");
      console.log("jgs('--......--')                                                             ");
      console.log("   /'--......--'\\                                                              ");
      console.log("   `\"--......--\"`                                                             ");
  }

  /**
   * Show the profile and then
   * Start the Main Loop
   */
  start();
  getProfile();
  lichessConnectionLoop();
  DGTliveChessConnectionLoop();


  function startingPosition(): Chess {
    return Chess.fromSetup(defaultSetup()).unwrap();
    //const setup = parseFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").unwrap();
    //return Chess.fromSetup(setup).unwrap();
  }

}