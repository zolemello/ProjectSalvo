package com.codeoftheweb.salvovb.repository;

import com.codeoftheweb.salvovb.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;
import java.util.Optional;


@RepositoryRestResource
public interface GameRepository extends JpaRepository<Game,Long> {
}
