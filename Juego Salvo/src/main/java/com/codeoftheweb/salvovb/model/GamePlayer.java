package com.codeoftheweb.salvovb.model;

import com.codeoftheweb.salvovb.rest.controller.SalvoRestController;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;



@Entity
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id_gameplayer;

    private Date creationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="id_game")
    private Game game;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="id_player")
    private Player player;

    @OneToMany(mappedBy="gamePlayer", fetch=FetchType.EAGER)
    Set<Ship> ships=new HashSet<>();
    @OneToMany(mappedBy="gameplayer", fetch=FetchType.EAGER)
    Set<Salvo> salvos=new HashSet<>();

    private String 
        WAIT_OPPONENT,
        PLACE_SHIPS,
        WAIT_OPPONENT_SHIPS,
        ENTER_SALVO,
        WAIT_OPPONENT_SALVO,
        WIN,
        DRAW,
        LOSE,
        ERROR; 
    

    public GamePlayer() {
        this.creationDate=new Date();
    }

    public GamePlayer(Game game, Player player) {
        this.creationDate=new Date();
        this.game = game;
        this.player = player;
    }

    public long getId_gameplayer() {
        return id_gameplayer;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Set<Ship> getShips() {
        return ships;
    }

    public void setShips(Set<Ship> ships) {
        this.ships = ships;
    }

    public Set<Salvo> getsalvos() {
        return salvos;
    }

    public void setsalvos(Set<Salvo> salvos) {
        this.salvos = salvos;
    }

    public Optional<Score> getScore(){
        return player.getScore(game);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

   
    public String getEstadoJuego() {
        Optional <GamePlayer> opponentGamePlayer = this.getGame().getGame_players().stream().filter(gamePlayer1 -> gamePlayer1.getId_gameplayer() != this.getId_gameplayer()).findFirst();
        long lastTurn = this.getsalvos().stream().mapToLong(Salvo::getTurn).max().orElse(0);
        int totalShips = 5;

          String response = ERROR; 
        
        if (!opponentGamePlayer.isPresent()) {
            response = WAIT_OPPONENT;
        } else {
            if (this.getShips().isEmpty()) {
                response = PLACE_SHIPS;
            } else if (opponentGamePlayer.get().getShips().isEmpty()) {
                response = WAIT_OPPONENT_SHIPS;
            } else {
                if ((this.getsalvos().size() == opponentGamePlayer.get().getsalvos().size()) && (this.getSinks(lastTurn, this.getsalvos(), opponentGamePlayer.get().getShips()).size() == totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getsalvos(), this.getShips()).size() < totalShips)) {
                    response = WIN;
                } else if ((this.getsalvos().size() == opponentGamePlayer.get().getsalvos().size()) && (this.getSinks(lastTurn, this.getsalvos(), opponentGamePlayer.get().getShips()).size() == totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getsalvos(), this.getShips()).size() == totalShips)) {
                    response = DRAW;
                } else if ((this.getsalvos().size() == opponentGamePlayer.get().getsalvos().size()) && (this.getSinks(lastTurn, this.getsalvos(), opponentGamePlayer.get().getShips()).size() < totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getsalvos(), this.getShips()).size() == totalShips)) {
                    response = LOSE;
                } else if ((this.getId_gameplayer() < opponentGamePlayer.get().getId_gameplayer()) && (this.getsalvos().size() == opponentGamePlayer.get().getsalvos().size())) {
                    response = ENTER_SALVO;
                } else if ((this.getId_gameplayer() < opponentGamePlayer.get().getId_gameplayer()) && (this.getsalvos().size() > opponentGamePlayer.get().getsalvos().size())) {
                    response = WAIT_OPPONENT_SALVO;
                } else if ((this.getId_gameplayer() > opponentGamePlayer.get().getId_gameplayer()) && (this.getsalvos().size() < opponentGamePlayer.get().getsalvos().size())) {
                    response = ENTER_SALVO;
                } else if ((this.getId_gameplayer() > opponentGamePlayer.get().getId_gameplayer()) && (this.getsalvos().size() == opponentGamePlayer.get().getsalvos().size())) {
                    response = WAIT_OPPONENT_SALVO;
                }
            }
        }
        return response;
    }
   

    public List<Map<String, Object>> getSinks(long turn, Set <Salvo> salvos, Set<Ship> ships) {
        List<String> allShots = new ArrayList<>();
        salvos
                .stream()
                .filter(salvo -> salvo.getTurn() <= turn)
                .forEach(salvo -> allShots.addAll(salvo.getLocations()));
        return ships
                .stream()
                .filter(ship -> allShots.containsAll(ship.getShipLocations()))
                .map(ship -> new SalvoRestController().makeShipDto(ship))
                .collect(Collectors.toList());
    }

    public void addSalvo(Salvo salvo) {
        salvo.setGameplayer(this);
        salvos.add(salvo);
    }

	
}
