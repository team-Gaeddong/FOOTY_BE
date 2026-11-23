package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

    // 목록 조건은 GameSpecifications로 조립한다
    @Override
    @EntityGraph(attributePaths = "host")
    Page<Game> findAll(Specification<Game> spec, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Optional<Game> findWithHostById(Long id);
}
