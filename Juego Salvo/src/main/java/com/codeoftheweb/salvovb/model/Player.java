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


    //dto para para administrar la info de Player
	//ACA TAMBIEN ESTA LA INFO PARA CUANDO GANA, PIERDE Y EMPATA
    public Map<String, Object> playerDTO(){
        Map<String, Object> dto = new LinkedHashMap<>();

        //int totalWon = this.getWons().size();
        Integer totalLose = this.getLoss().size();
        Integer totalTie = this.getTies().size();
        

        dto.put("id", this.getId_player());
        dto.put("username", this.getUserName());

        dto.put("won", this.getWons());
        dto.put("lose", totalLose);
        dto.put("tie", totalTie);
        dto.put("total", this.getTotalPoints());
        return dto;
    }

    // ESTOS OBTIENEN LOS PUNTOS POR PERDER, EMPATAR O GANAR, Y LUEGO LOS PUNTOS TOTALES
    // HACE UN FILTRO, PONE EN UNA VARIABLE EL VALOR, Y HACE UN SET EN DONDE PONE TODOS ESOS VALORES
    public Set<Score> getLoss() {
        return this.scores.stream()
                .filter(lossScore -> lossScore.getScore() == 0)
                .collect(Collectors.toSet());
    }

    public Set<Score> getTies() {
        return this.scores.stream()
                .filter(tieScore -> tieScore.getScore() == 0.5)
                .collect(Collectors.toSet());
    }

    public int getWons() {
        return this.scores.stream()
                .filter(wonScore -> wonScore.getScore() == 1)
                .collect(Collectors.toSet())
                .size();
    }





    public int getTotalPoints() {
        return this.getWons() * 3 + getTies().size();
    }



}
