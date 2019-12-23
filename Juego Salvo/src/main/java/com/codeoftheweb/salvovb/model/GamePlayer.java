package com.codeoftheweb.salvovb.model;

import com.codeoftheweb.salvovb.repository.GamePlayerRepository;
import com.codeoftheweb.salvovb.rest.controller.SalvoRestController;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;



@Entity
public class GamePlayer {

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    
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

    public Set<Salvo> getSalvos() {
        return salvos;
    }

    public void setSalvos(Set<Salvo> salvos) {
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

    public enum GameState {
        UNDEFINED,
        ENTER_SHIPS,
        WAIT_OPPONENT,
        WAIT_OPPONENT_SHIPS,
        FIRE,
        WAIT,
        WON,
        LOST,
        TIED,
        ERROR
    }
   

    // VER COMO HACER ESTE PUNTO 
    //LO DEJO ASI PARA QUE NO ROMPA EN LOS OTROS LUGARES
    public Enum<GameState> getEstadoJuego(){
        

        Optional <GamePlayer> opponentGamePlayer = this.getGame().getGame_players().stream().filter(gamePlayer1 -> gamePlayer1.getId_gameplayer() != this.getId_gameplayer()).findFirst();

       return null ;
    }

    public void addSalvo(Salvo salvo) {
        salvo.setGameplayer(this);
        salvos.add(salvo);
    }
   
}
