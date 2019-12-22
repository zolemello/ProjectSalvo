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
    //el mappedBy es el nombre del campo en el ManytoOne del otro lado
    @OneToMany(mappedBy="gameplayer", fetch=FetchType.EAGER)
    Set<Salvo> salvoes=new HashSet<>();

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

    public Set<Salvo> getSalvoes() {
        return salvoes;
    }

    public void setSalvoes(Set<Salvo> salvoes) {
        this.salvoes = salvoes;
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

    //tarea 6
    public Enum<GamePlayerState> getGameState() {
        Optional <GamePlayer> opponentGamePlayer = this.getGame().getGame_players().stream().filter(gamePlayer1 -> gamePlayer1.getId_gameplayer() != this.getId_gameplayer()).findFirst();
        long lastTurn = this.getSalvoes().stream().mapToLong(Salvo::getTurn).max().orElse(0);
        int totalShips = 5;

        Enum<GamePlayerState> response = GamePlayerState.ERROR;

        if (!opponentGamePlayer.isPresent()) {
            response = GamePlayerState.WAIT_OPPONENT;
        } else {
            if (this.getShips().isEmpty()) {
                response = GamePlayerState.PLACE_SHIPS;
            } else if (opponentGamePlayer.get().getShips().isEmpty()) {
                response = GamePlayerState.WAIT_OPPONENT_SHIPS;
            } else {
                if ((this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size()) && (this.getSinks(lastTurn, this.getSalvoes(), opponentGamePlayer.get().getShips()).size() == totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getSalvoes(), this.getShips()).size() < totalShips)) {
                    response = GamePlayerState.WIN;
                } else if ((this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size()) && (this.getSinks(lastTurn, this.getSalvoes(), opponentGamePlayer.get().getShips()).size() == totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getSalvoes(), this.getShips()).size() == totalShips)) {
                    response = GamePlayerState.DRAW;
                } else if ((this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size()) && (this.getSinks(lastTurn, this.getSalvoes(), opponentGamePlayer.get().getShips()).size() < totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getSalvoes(), this.getShips()).size() == totalShips)) {
                    response = GamePlayerState.LOSE;
                } else if ((this.getId_gameplayer() < opponentGamePlayer.get().getId_gameplayer()) && (this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.ENTER_SALVO;
                } else if ((this.getId_gameplayer() < opponentGamePlayer.get().getId_gameplayer()) && (this.getSalvoes().size() > opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.WAIT_OPPONENT_SALVO;
                } else if ((this.getId_gameplayer() > opponentGamePlayer.get().getId_gameplayer()) && (this.getSalvoes().size() < opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.ENTER_SALVO;
                } else if ((this.getId_gameplayer() > opponentGamePlayer.get().getId_gameplayer()) && (this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.WAIT_OPPONENT_SALVO;
                }
            }
        }
        return response;
    }
    //tarea 6
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
        salvoes.add(salvo);
    }
}
