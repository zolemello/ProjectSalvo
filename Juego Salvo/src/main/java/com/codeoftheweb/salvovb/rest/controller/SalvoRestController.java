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
    private PlayerRepository pRepo;

    @Autowired
    private GameRepository gRepo;

    @Autowired
    private GamePlayerRepository gpRepo;

    @Autowired
    private ShipRepository shRepo;

    @Autowired
    private SalvoRepository salRepo;

    @Autowired
    private ScoreRepository scRepo;


    @RequestMapping("/players")
    public List<Object> getAllPlayers() {

        return pRepo.findAll().stream().map(player -> makePlayerDTO(player)).collect(Collectors.toList());
    }

    @RequestMapping(path = "/players", method = RequestMethod.POST)
    public ResponseEntity<Object> register(@RequestParam String username, @RequestParam String password) {

        if (username.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>("Missing data", HttpStatus.FORBIDDEN);
        }

        if (pRepo.findByUserName(username) !=  null) {
            return new ResponseEntity<>("Name already in use", HttpStatus.FORBIDDEN);
        }

        pRepo.save(new Player(username, passwordEncoder.encode(password)));
        return new ResponseEntity<>("Usuario creado",HttpStatus.CREATED);
    }


    //Metodo para retornar si hay usuario autenticado
    private Player getUserAuthenticated(Authentication authentication){
        Player player=new Player();
        //chequeamos que si hay algun usuario logueado
        if (authentication != null && authentication instanceof AnonymousAuthenticationToken != true) {
            //caso afirmativo lo incluimos al dto
            player = pRepo.findByUserName(authentication.getName());
        }else{
            //caso negativo ingresamos null
            player = null;
        }
        return player;
    }

    @RequestMapping("/games")
    public Map<String, Object> getAllGames(Authentication authentication) {
        Map<String, Object> dto=new LinkedHashMap<>();
        //chequeamos que si hay algun usuario logueado
        Player player = this.getUserAuthenticated(authentication);
        if (player != null) {
            //caso afirmativo lo incluimos al dto
            dto.put("players", makePlayerDTO(player));
        }else{
            //caso negativo ingresamos null
            dto.put("players", null);
        }
        //se muestran todos los juegos
        dto.put( "games", gRepo.findAll().stream().map(game -> makeGameDTO(game)).collect(Collectors.toList()));
        return dto;

    }

    @RequestMapping(path = "/games", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createGame(Authentication authentication) {
        Player player=getUserAuthenticated(authentication);
        if (player==null) {
            return new ResponseEntity<>(makeMap("error","NO AUTORIZADO"), HttpStatus.UNAUTHORIZED);
        }

        Game game = new Game();
        gRepo.save(game);
        GamePlayer gamePlayer = new GamePlayer(game, pRepo.findByUserName(player.getUserName()));
        gpRepo.save(gamePlayer);
        return new ResponseEntity<>(makeMap("gpid",gamePlayer.getId_gameplayer()),HttpStatus.CREATED);
    }

    @RequestMapping("/game_view/{nn}")
    public Map<String, Object> getGameViewByGamePlayerID(@PathVariable Long nn){
        GamePlayer gamePlayer = gpRepo.findById(nn).get();

        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("id", gamePlayer.getGame().getId_game());
        dto.put("created", gamePlayer.getGame().getCreation_date());
        dto.put( "gamePlayers", gamePlayer.getGame().getGame_players()
                .stream().map(gamePlayer1-> makeGamePlayerDTO(gamePlayer1)).collect(Collectors.toList()));
        dto.put("ships", gamePlayer.getShips()
                .stream()
                .map(ship -> makeShipDto(ship))
                .collect(Collectors.toList()));
        dto.put("salvoes",gamePlayer.getGame().getGame_players()
                .stream()
                .flatMap(gamePlayer1 -> gamePlayer1.getSalvoes()
                        .stream()
                        .map(salvo -> makeSalvoDto(salvo,gamePlayer1)))
                .collect(Collectors.toList()));
        return dto;
    }


    @RequestMapping("/game/{nn}/players")
    public Map<String, Object> getPlayersViewByGameID(@PathVariable Long nn){
        Game game = gRepo.findById(nn).get();
        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("players:", game.getGame_players().stream().map(gameplayer1 -> makeGamePlayerDTO(gameplayer1)).collect(Collectors.toList()));
        return dto;
    }

    @RequestMapping(path="/game/{gameId}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long gameId, Authentication authentication) {
        Player player=getUserAuthenticated(authentication);
        if (player == null) {
            return new ResponseEntity<>(makeMap("error","NO AUTORIZADO"), HttpStatus.UNAUTHORIZED);
        }
        Optional<Game> game = gRepo.findById(gameId);
        if (!game.isPresent()) {
            return new ResponseEntity<>(makeMap("error","CONFLICTO"), HttpStatus.CONFLICT);
        }
        if ( game.get().getGame_players().size() > 1 ) {
            return new ResponseEntity<>(makeMap("error","lleno"), HttpStatus.FORBIDDEN);
        }
        GamePlayer gamePlayer = new GamePlayer(game.get(), pRepo.findByUserName(authentication.getName()));
        gpRepo.save(gamePlayer);
        return new ResponseEntity<>(makeMap("gpid",gamePlayer.getId_gameplayer()), HttpStatus.CREATED);
    }

    @PostMapping(value="/games/players/{gamePlayerId}/ships")
    public ResponseEntity<Map<String, Object>> placeShips(@PathVariable Long gamePlayerId, Authentication authentication, @RequestBody Set<Ship> ships) {
        Optional<GamePlayer> gamePlayer = gpRepo.findById(gamePlayerId);
        Player player = getUserAuthenticated(authentication);
        if (player == null || (!gamePlayer.isPresent()) || (!gamePlayer.get().getPlayer().getUserName().equals(authentication.getName()))) {
            return new ResponseEntity<>(makeMap("error", "NO AUTORIZADO"), HttpStatus.UNAUTHORIZED);
        }
        if (gamePlayer.get().getShips().size() > 0 || ships.size() != 5) {
            return new ResponseEntity<>(makeMap("error", "ya fueron cargados o no se completo la cantidad de 5 barcos"), HttpStatus.FORBIDDEN);
        }


        ships.forEach(ship -> {
            ship.setGamePlayer(gamePlayer.get());
            shRepo.save(ship);
        });

        gamePlayer.get().setShips(ships);
        gpRepo.save(gamePlayer.get());
        return new ResponseEntity<>(makeMap("exito", "cargados"), HttpStatus.CREATED);
    }


    @PostMapping(value="/games/players/{gamePlayerId}/salvos")
    public ResponseEntity<Map<String, Object>> fireSalvos(@PathVariable Long gamePlayerId, Authentication authentication, @RequestBody Salvo salvo) {

        Optional <GamePlayer> gamePlayer = gpRepo.findById(gamePlayerId);
        if ( getUserAuthenticated(authentication)==null || (!gamePlayer.isPresent()) || (!gamePlayer.get().getPlayer().getUserName().equals(authentication.getName()))) {
            return new ResponseEntity<>(makeMap("error", "Usuario NO AUTORIZADO"), HttpStatus.UNAUTHORIZED);
        }


        long maxTurn = gamePlayer.get().getSalvoes().stream().count();
        System.out.println(maxTurn);
        if ( maxTurn+1 != salvo.getTurn() ) {
            return new ResponseEntity<>(makeMap("error", "numero de turno equivocado"), HttpStatus.FORBIDDEN);
        }

        Optional <GamePlayer> opponentGamePlayer = gamePlayer.get().getGame().getGame_players().stream().filter(gamePlayer1 -> gamePlayer1.getId_gameplayer() != gamePlayer.get().getId_gameplayer()).findFirst();
        if ( !(opponentGamePlayer.isPresent()) || opponentGamePlayer.get().getSalvoes().size() < salvo.getTurn()-1) {
            return new ResponseEntity<>(makeMap("error", "oponente ausente/coordinacion de turnos"), HttpStatus.FORBIDDEN);
        }

        if (opponentGamePlayer.get().getShips().size()==0){
            return new ResponseEntity<>(makeMap("error", "oponente aun no posicionó las naves"), HttpStatus.FORBIDDEN);
        }
        //tarea 6
        gamePlayer.get().addSalvo(salvo);

        if ( gamePlayer != null ) {
            if (gamePlayer.get().getGameState() == GamePlayerState.WIN) {
                scRepo.save(new Score(gamePlayer.get().getGame(), gamePlayer.get().getPlayer(), 1.0, new Date()));
                scRepo.save(new Score(gamePlayer.get().getGame(), opponentGamePlayer.get().getPlayer(), 0.0, new Date()));
            } else if (gamePlayer.get().getGameState() == GamePlayerState.LOSE) {
                scRepo.save(new Score(gamePlayer.get().getGame(), gamePlayer.get().getPlayer(), 0.0, new Date()));
                scRepo.save(new Score(gamePlayer.get().getGame(), opponentGamePlayer.get().getPlayer(), 1.0, new Date()));
            } else if (gamePlayer.get().getGameState() == GamePlayerState.DRAW) {
                scRepo.save(new Score(gamePlayer.get().getGame(), gamePlayer.get().getPlayer(), 0.5, new Date()));
                scRepo.save(new Score(gamePlayer.get().getGame(), opponentGamePlayer.get().getPlayer(), 0.5, new Date()));
            }
        }

        salvo.setGameplayer(gamePlayer.get());
        salRepo.save(salvo);
        return new ResponseEntity<>(makeMap("exito", "cargados"), HttpStatus.CREATED);
    }




    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    //DTO
    private Map<String, Object> makeGameDTO(Game game) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", game.getId_game());
        dto.put("created", game.getCreation_date().getTime());
        dto.put("ended", game.getEndDate(game.getGame_players()));
        dto.put( "players", game.getGame_players().stream().map(gamePlayer1-> makeGamePlayerDTO(gamePlayer1)).collect(Collectors.toList()));
        dto.put("scores",game.getGame_players().stream().map(gamePlayer ->
        {
            if (gamePlayer.getScore().isPresent()) {
                //si hay algo en el optional
                return makeScoreDto(gamePlayer.getScore().get());
            } else {
                //si optional esta vacio o empty
                System.out.println(gamePlayer.getScore());
                return null;//dudoso
            }
        }));
        return dto;
    }

    private Map<String, Object> makePlayerDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId_player());
        dto.put("email", player.getUserName());
        return dto;
    }

    private Map<String, Object> makeGamePlayerDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("gpid", gamePlayer.getId_gameplayer());
        //dto.put("player", makePlayerDTO(gamePlayer.getPlayer()));
        dto.put("id",gamePlayer.getPlayer().getId_player());
        dto.put("email",gamePlayer.getPlayer().getUserName());
        dto.put("gameState", gamePlayer.getGameState());
        return dto;
    }

    public Map<String, Object> makeShipDto(Ship ship){
        Map<String,Object> dto= new LinkedHashMap<>();
        dto.put("type", ship.getType());
        dto.put("locations", ship.getShipLocations());
        return dto;
    }


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
            dto.put("sinks", salvo.getSinks(salvo.getTurn(), salvo.getGameplayer().getSalvoes(), opponentShips));
        }
        return dto;
    }

    public Map<String, Object> makeScoreDto(Score score){
        Map<String,Object> dto= new LinkedHashMap<>();
        dto.put("game",score.getGame().getId_game());
        dto.put("player",score.getPlayer().getId_player());
        dto.put("score",score.getScore());
        dto.put("finish_date",score.getFinishDate());
        return dto;
    }

}
