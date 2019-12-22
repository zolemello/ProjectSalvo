package com.codeoftheweb.salvovb.service.impl;

import com.codeoftheweb.salvovb.model.Game;
import com.codeoftheweb.salvovb.repository.GameRepository;
import com.codeoftheweb.salvovb.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class GameServiceImpl implements GameService {

@Autowired
private GameRepository gameRepository;


public List <Game> findAll(){
    return gameRepository.findAll();
    }

}
