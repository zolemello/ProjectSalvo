package com.codeoftheweb.salvovb.rest.controller;

import com.codeoftheweb.salvovb.model.*;
import com.codeoftheweb.salvovb.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api")
public class  SalvoRestController {
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private SalvoRepository salvoRepository;

    @Autowired
    private ScoreRepository scoreRepository;

   /* public enum GameState {
        UNDEFINED,
        ENTER_SHIPS,
        WAIT_OPPONENT,
        WAIT_OPPONENT_SHIPS,
        FIRE,
        WAIT,
        WON,
        LOST,
        TIED
    }
*/ 

    @RequestMapping("/players")
    public List<Object> getAllPlayers() {

        return playerRepository.findAll().stream().map(player -> player.playerDTO()).collect(Collectors.toList());
    }

    
//CREAR USUARIO
    @RequestMapping(path = "/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> registrer(@RequestParam String username,  @RequestParam String password) {
        ResponseEntity<Map<String, Object>> response;
        Player player = playerRepository.findByUserName(username);
        if (username.isEmpty() || password.isEmpty()) {
            response = new ResponseEntity<>(makeMap("error", "No name"), HttpStatus.FORBIDDEN);
        } else if (player != null) {
            response = new ResponseEntity<>(makeMap("error", "Username already exists"), HttpStatus.FORBIDDEN);
        } else {
            Player newPlayer = playerRepository.save(new Player(username, passwordEncoder.encode(password)));
            response = new ResponseEntity<>(makeMap("id", newPlayer.getId_player()), HttpStatus.CREATED);
        }
        return response;
    }


    
    private Player getUserAuthenticated(Authentication authentication){
        Player player=new Player();
        
        if (authentication != null && authentication instanceof AnonymousAuthenticationToken != true) {
             player = playerRepository.findByUserName(authentication.getName());
        }else{
              player = null;
        }
        return player;
    }

    @RequestMapping("/games")
    public Map<String, Object> getAllGames(Authentication authentication) {
        Map<String, Object> dto=new LinkedHashMap<>();
        
        Player player = this.getUserAuthenticated(authentication);
        if (player != null) {
                        dto.put("players", player.playerDTO());
                        
        }else{
            dto.put("players", null);
        }
        dto.put( "games", gameRepository.findAll().stream().map(game -> makeGameDTO(game)).collect(Collectors.toList()));
        return dto;

    }
// CREAR JUEGO
    @RequestMapping(path = "/games", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createGame(Authentication authentication) {
        Player player=getUserAuthenticated(authentication);
        if (player==null) {
            return new ResponseEntity<>(makeMap("error","NO AUTORIZADO"), HttpStatus.UNAUTHORIZED);
        }

        Game game = new Game();
        gameRepository.save(game);
        GamePlayer gamePlayer = new GamePlayer(game, playerRepository.findByUserName(player.getUserName()));
        gamePlayerRepository.save(gamePlayer);
        return new ResponseEntity<>(makeMap("gpId",gamePlayer.getId_gameplayer()),HttpStatus.CREATED);
    }

    @RequestMapping("/game_view/{gameplayerid}")
    public Map<String, Object> getGameViewByGamePlayerID(@PathVariable Long gameplayerid){
        GamePlayer gamePlayer = gamePlayerRepository.findById(gameplayerid).get();

        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("id", gamePlayer.getGame().getId_game());
        dto.put("created", gamePlayer.getGame().getCreation_date());
        dto.put( "gamePlayers", gamePlayer.getGame().getGame_players()
                .stream().map(gamePlayer1-> makeGamePlayerDTO(gamePlayer1)).collect(Collectors.toList()));
        dto.put("ships", gamePlayer.getShips()
                .stream()
                .map(ship -> makeShipDto(ship))
                .collect(Collectors.toList()));
        dto.put("salvos",gamePlayer.getGame().getGame_players()
                .stream()
                .flatMap(gamePlayer1 -> gamePlayer1.getSalvos()
                        .stream()
                        .map(salvo -> makeSalvoDto(salvo,gamePlayer1)))
                .collect(Collectors.toList()));
        return dto;
    }


    @RequestMapping("/game/{nn}/players")
    public Map<String, Object> getPlayersViewByGameID(@PathVariable Long nn){
        Game game = gameRepository.findById(nn).get();
        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("players:", game.getGame_players().stream().map(gameplayer1 -> makeGamePlayerDTO(gameplayer1)).collect(Collectors.toList()));
        return dto;
    }

    //UNIRSE A UN JUEGO 
    @RequestMapping(path="/game/{gameId}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long gameId, Authentication authentication) {
        ResponseEntity<Map<String, Object>> response;
        Player player=getUserAuthenticated(authentication);
        if (player == null) {
            return new ResponseEntity<>(makeMap("error","Necesita loguearse para entrar a esta partida"), HttpStatus.UNAUTHORIZED);
        }
        Optional<Game> game = gameRepository.findById(gameId);
        if (game == null) {
            response = new ResponseEntity<>(makeMap("error", "Game doesnt exist"), HttpStatus.NOT_FOUND);
        }    
        if ( game.get().getGame_players().size() > 1 ) {
            return new ResponseEntity<>(makeMap("error","Partida completa"), HttpStatus.FORBIDDEN);
        }
        GamePlayer gamePlayer = new GamePlayer(game.get(), playerRepository.findByUserName(authentication.getName()));
        gamePlayerRepository.save(gamePlayer);
        return new ResponseEntity<>(makeMap("gpId",gamePlayer.getId_gameplayer()), HttpStatus.CREATED);
    }


    //ACOMODAR SHIPS 
    @PostMapping(value="/games/players/{gamePlayerId}/ships")
    public ResponseEntity<Map<String, Object>> placeShips(@PathVariable Long gamePlayerId, Authentication authentication, @RequestBody Set<Ship> ships) {
        Optional<GamePlayer> gamePlayer = gamePlayerRepository.findById(gamePlayerId);
        Player player = getUserAuthenticated(authentication);
        if (player == null || (!gamePlayer.isPresent()) || (!gamePlayer.get().getPlayer().getUserName().equals(authentication.getName()))) {
            return new ResponseEntity<>(makeMap("error", "NO AUTORIZADO"), HttpStatus.UNAUTHORIZED);
        }
        if (gamePlayer.get().getShips().size() > 0 || ships.size() != 5) {
            return new ResponseEntity<>(makeMap("error", ""), HttpStatus.FORBIDDEN);
        }


        ships.forEach(ship -> {
            ship.setGamePlayer(gamePlayer.get());
            shipRepository.save(ship);
        });

        gamePlayer.get().setShips(ships);
        gamePlayerRepository.save(gamePlayer.get());
        return new ResponseEntity<>(makeMap("exito", "Ships colocados"), HttpStatus.CREATED);
    }


    @PostMapping(value="/games/players/{gamePlayerId}/salvos")
    public ResponseEntity<Map<String, Object>> fireSalvos(@PathVariable Long gamePlayerId, Authentication authentication, @RequestBody Salvo salvo) {

        Optional <GamePlayer> gamePlayer = gamePlayerRepository.findById(gamePlayerId);
        if ( getUserAuthenticated(authentication)==null || (!gamePlayer.isPresent()) || (!gamePlayer.get().getPlayer().getUserName().equals(authentication.getName()))) {
            return new ResponseEntity<>(makeMap("error", "Usuario NO AUTORIZADO"), HttpStatus.UNAUTHORIZED);
        }


        long maxTurn = gamePlayer.get().getSalvos().stream().count();
        System.out.println(maxTurn);
        if ( maxTurn+1 != salvo.getTurn() ) {
            return new ResponseEntity<>(makeMap("error", "Turno equivocado"), HttpStatus.FORBIDDEN);
        }

        Optional <GamePlayer> opponentGamePlayer = gamePlayer.get().getGame().getGame_players().stream().filter(gamePlayer1 -> gamePlayer1.getId_gameplayer() != gamePlayer.get().getId_gameplayer()).findFirst();
        if ( !(opponentGamePlayer.isPresent()) || opponentGamePlayer.get().getSalvos().size() < salvo.getTurn()-1) {
            return new ResponseEntity<>(makeMap("error", "Esperando oponente"), HttpStatus.FORBIDDEN);
        }

        if (opponentGamePlayer.get().getShips().size()==0){
            return new ResponseEntity<>(makeMap("error", "Esperando Oponente"), HttpStatus.FORBIDDEN);
        }
        


        gamePlayer.get().addSalvo(salvo);

       if ( gamePlayer != null ) {
           

            if (gamePlayer.get().getEstadoJuego() == GamePlayer.GameState.WON) {
                scoreRepository.save(new Score(gamePlayer.get().getGame(), gamePlayer.get().getPlayer(), 1.0, new Date()));
                scoreRepository.save(new Score(gamePlayer.get().getGame(), opponentGamePlayer.get().getPlayer(), 0.0, new Date()));
            } else if (gamePlayer.get().getEstadoJuego() == GamePlayer.GameState.LOST) {
                scoreRepository.save(new Score(gamePlayer.get().getGame(), gamePlayer.get().getPlayer(), 0.0, new Date()));
                scoreRepository.save(new Score(gamePlayer.get().getGame(), opponentGamePlayer.get().getPlayer(), 1.0, new Date()));
            } else if (gamePlayer.get().getEstadoJuego() == GamePlayer.GameState.TIED) {
                scoreRepository.save(new Score(gamePlayer.get().getGame(), gamePlayer.get().getPlayer(), 0.5, new Date()));
                scoreRepository.save(new Score(gamePlayer.get().getGame(), opponentGamePlayer.get().getPlayer(), 0.5, new Date()));
            }
        }

        salvo.setGameplayer(gamePlayer.get());
        salvoRepository.save(salvo);
        return new ResponseEntity<>(makeMap("exito", "Salvos cargados"), HttpStatus.CREATED);
    }




    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

   //GAME DTO
   private Map<String, Object> makeGameDTO(Game game) {
    Map<String, Object> dto = new LinkedHashMap<String, Object>();
    dto.put("id", game.getId_game());
    dto.put("created", game.getCreation_date().getTime());
    dto.put( "players", game.getGame_players().stream().map(gamePlayer1-> makeGamePlayerDTO(gamePlayer1)).collect(Collectors.toList()));
    dto.put("scores",game.getGame_players().stream().map(gamePlayer ->
        {
            if (gamePlayer.getScore().isPresent()) {
            
                return makeScoreDto(gamePlayer.getScore().get());
            } else {
             
                return null;
            }
        }));
    return dto;
}

       //GAME PLAYER DTO
    private Map<String, Object> makeGamePlayerDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("gpId", gamePlayer.getId_gameplayer());
       dto.put("id",gamePlayer.getPlayer().getId_player());
        dto.put("email",gamePlayer.getPlayer().getUserName());
        dto.put("gameState", gamePlayer.getEstadoJuego());
        return dto;
    }

    //SHIP DTO
    public Map<String, Object> makeShipDto(Ship ship){
        Map<String,Object> dto= new LinkedHashMap<>();
        dto.put("type", ship.getType());
        dto.put("locations", ship.getShipLocations());
        return dto;
    }

//SALVO DTO
    public Map<String, Object> makeSalvoDto(Salvo salvo, GamePlayer gamePlayer){
        Map<String,Object> dto= new LinkedHashMap<>();
        dto.put("turn", salvo.getTurn());
        dto.put("locations", salvo.getLocations());
        dto.put("player",salvo.getGameplayer().getPlayer().getId_player());
        //tarea 5
        Optional<GamePlayer> opponentGamePlayer = gamePlayer.getGame().getGame_players().stream().filter(gamePlayer2 -> gamePlayer2.getId_gameplayer() != gamePlayer.getId_gameplayer()).findFirst();
        if (opponentGamePlayer.isPresent()) {
            Set<Ship> opponentShips = opponentGamePlayer.get().getShips();
            dto.put("hits", salvo.getHits(salvo.getLocations(), opponentShips));
            dto.put("sinks", salvo.getSinks(salvo.getTurn(), salvo.getGameplayer().getSalvos(), opponentShips));
        }
        return dto;
    }

    //SCORE DTO
    public Map<String, Object> makeScoreDto(Score score){
        Map<String,Object> dto= new LinkedHashMap<>();
        dto.put("game",score.getGame().getId_game());
        dto.put("player",score.getPlayer().getId_player());
        dto.put("score",score.getScore());
        dto.put("finish_date",score.getFinishDate());
        return dto;
    }

}
