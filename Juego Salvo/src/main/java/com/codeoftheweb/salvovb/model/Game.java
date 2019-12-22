package com.codeoftheweb.salvovb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id_game;

    private Date creation_date;

    @OneToMany(mappedBy="game", fetch=FetchType.EAGER)
    Set<GamePlayer> game_players;

    @OneToMany(mappedBy="game", fetch=FetchType.EAGER)
    Set<Score> scores;

    public Game() {
        this.creation_date = new Date();
    }

    public long getId_game() {
        return id_game;
    }

    public Date getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(Date creation_date) {
        this.creation_date = creation_date;
    }

    public Set<GamePlayer> getGame_players() {
        return game_players;
    }

    public Set<Score> getScores() {
        return scores;
    }

    public void setGame_players(Set<GamePlayer> game_players) {
        this.game_players = game_players;
    }

    public void setScores(Set<Score> scores) {
        this.scores = scores;
    }

    public void addGamePlayer(GamePlayer gameplayer) {
        gameplayer.setGame(this);
        game_players.add(gameplayer);
    }

    public boolean getEndDate(Set<GamePlayer> gamePlayers) {

        GamePlayer gamePlayer = gamePlayers.stream().findFirst().orElse(null);
        if ( (null != gamePlayer) && (gamePlayer.getEstadoJuego() == GamePlayerState.WIN || gamePlayer.getEstadoJuego() == GamePlayerState.LOSE || gamePlayer.getEstadoJuego() == GamePlayerState.DRAW)) {
            return true;
        } else {
            return false;
        }
    }

}
