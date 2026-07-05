package com.esports.platform.domain.tournament.repository;

import static com.esports.platform.domain.tournament.entity.QTournament.tournament;

import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class TournamentRepositoryImpl implements TournamentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Tournament> search(TournamentSearchCondition condition, Pageable pageable) {
        List<Tournament> content = queryFactory
                .selectFrom(tournament)
                .where(
                        keywordContains(condition.keyword()),
                        gameTypeEq(condition.gameType()),
                        formatEq(condition.format()),
                        statusEq(condition.status())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(tournament.count())
                .from(tournament)
                .where(
                        keywordContains(condition.keyword()),
                        gameTypeEq(condition.gameType()),
                        formatEq(condition.format()),
                        statusEq(condition.status())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? tournament.title.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression gameTypeEq(String gameType) {
        return StringUtils.hasText(gameType) ? tournament.gameType.eq(gameType) : null;
    }

    private BooleanExpression formatEq(TournamentFormat format) {
        return format != null ? tournament.format.eq(format) : null;
    }

    private BooleanExpression statusEq(TournamentStatus status) {
        return status != null ? tournament.status.eq(status) : null;
    }
}
