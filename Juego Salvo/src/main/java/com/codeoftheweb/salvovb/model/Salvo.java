package com.codeoftheweb.salvovb.model;

import com.codeoftheweb.salvovb.rest.controller.SalvoRestController;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Salvo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id_salvo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="gamePlayerID")
    private GamePlayer gameplayer;

    private long turn;

    @ElementCollection
    @Column(name="salvoLocations")
    private List<String> locations;

    public Salvo() {
    }

    public Salvo(GamePlayer gameplayer, long turn, List<String> locations) {
        this.gameplayer = gameplayer;
        this.turn = turn;
        this.locations = locations;
    }

    public GamePlayer getGameplayer() {
        return gameplayer;
    }

    public void setGameplayer(GamePlayer gameplayer) {
        this.gameplayer = gameplayer;
    }

    public long getTurn() {
        return turn;
    }

    public void setTurn(long turn) {
        this.turn = turn;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getHits(List <String> currentSalvoLocations, Set<Ship> opponentShips) {
        return currentSalvoLocations
                .stream()
                .filter(location -> opponentShips
                        .stream()
                        .anyMatch(ship -> ship.getShipLocations().contains(location)))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getSinks(long turn, Set <Salvo> mySalvos, Set<Ship> opponentShips) {
        List<String> allShots = new ArrayList<>();
        //voy a recorrer el set de mis salvos y voy a filtar aquellos cuyo turno es menor o igual al turno actual
        //luego voy a agregar a allShots las ubicaciones de esos salvos
        mySalvos.stream()
                .filter(salvo -> salvo.getTurn() <= turn)
                .forEach(salvo -> allShots.addAll(salvo.getLocations()));
        //aqui retornamos la coleccion de mapas de los barcos del oponente que hayan sido tocados en todas sus locations.
        return opponentShips.stream()
                .filter(ship -> allShots.containsAll(ship.getShipLocations()))
                .map(ship -> new SalvoRestController().makeShipDto(ship))//dudoso
                .collect(Collectors.toList());
    }
}
