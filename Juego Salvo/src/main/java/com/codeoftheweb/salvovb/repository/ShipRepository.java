package com.codeoftheweb.salvovb.repository;

import com.codeoftheweb.salvovb.model.Ship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface ShipRepository extends JpaRepository <Ship,Long>{
}
