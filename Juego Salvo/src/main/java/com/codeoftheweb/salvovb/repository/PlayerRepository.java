package com.codeoftheweb.salvovb.repository;

import com.codeoftheweb.salvovb.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource

public interface PlayerRepository extends JpaRepository<Player,Long> {
    Player findByUserName(@Param("name") String name);
}
