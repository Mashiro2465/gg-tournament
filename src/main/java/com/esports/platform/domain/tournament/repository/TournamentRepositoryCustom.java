package com.esports.platform.domain.tournament.repository;

import com.esports.platform.domain.tournament.entity.Tournament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TournamentRepositoryCustom {

    Page<Tournament> search(TournamentSearchCondition condition, Pageable pageable);
}
