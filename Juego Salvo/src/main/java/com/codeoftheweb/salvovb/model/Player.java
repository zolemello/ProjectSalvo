package com.codeoftheweb.salvovb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id_player;

    private String userName;

    private String password;

    @OneToMany(mappedBy="player", fetch=FetchType.EAGER)
    Set<GamePlayer> game_players;
    //mappedBy vincula con el nombre del atributo del lado de la clase Score relacionado con player en este caso.
    @OneToMany(mappedBy="player", fetch=FetchType.EAGER)
    Set<Score> scores;

    public Player() {
    }

    public Player(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public long getId_player() {
        return id_player;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<GamePlayer> getGame_players() {
        return game_players;
    }

    public void setGame_players(Set<GamePlayer> game_players) {
        this.game_players = game_players;
    }

    public Set<Score> getScores() {
        return scores;
    }

    public void setScores(Set<Score> scores) {
        this.scores = scores;
    }

    public void addGamePlayer(GamePlayer gameplayer) {
        gameplayer.setPlayer(this);
        game_players.add(gameplayer);
    }



    public Optional<Score> getScore(Game game){
        Optional<Score> maybeScore = this.getScores().stream().filter(sc -> sc.getGame().getId_game() == game.getId_game()).findFirst();
        return maybeScore;
    }

}
