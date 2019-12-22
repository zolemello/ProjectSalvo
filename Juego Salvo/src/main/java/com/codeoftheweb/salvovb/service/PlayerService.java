package com.codeoftheweb.salvovb.service;

import com.codeoftheweb.salvovb.model.Player;
import com.codeoftheweb.salvovb.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {
    @Autowired
    private PlayerRepository playerRepository;

    public Optional<Player> findbyId(Long id) {
        return playerRepository.findById(id);
    }

    public List<Player> findall() {
        return playerRepository.findAll();
    }

    public Player createPlayer(Player player) {
        return playerRepository.save(player);
    }



}
