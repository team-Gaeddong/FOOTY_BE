package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository.PlayerCount;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.service.GameListService.GameSummary;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameListServiceTest {

    private static final Long REQUESTER_ID = 1L;
    private static final EnumSet<GameStatus> ALL_STATUSES = EnumSet.allOf(GameStatus.class);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Test
    void 조건이_없으면_모든_상태의_경기를_페이지_조건과_날짜순으로_조회한다() {
        when(gameRepository.findByDeletedFalseAndStatusIn(eq(ALL_STATUSES), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<GameSummary> result = service().list(REQUESTER_ID, null, null, 2, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(gameRepository).findByDeletedFalseAndStatusIn(eq(ALL_STATUSES), pageable.capture());
        assertThat(result.getContent()).isEmpty();
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("playDate").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
        // 빈 쪽이면 참가 인원을 세지 않는다
        verifyNoInteractions(gamePlayerRepository);
    }

    @Test
    void 상태를_주면_그_상태의_경기만_조회한다() {
        EnumSet<GameStatus> upcoming = EnumSet.of(GameStatus.RECRUITING, GameStatus.CONFIRMED);
        when(gameRepository.findByDeletedFalseAndStatusIn(eq(upcoming), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service().list(REQUESTER_ID, List.of(GameStatus.CONFIRMED, GameStatus.RECRUITING, GameStatus.CONFIRMED),
                null, 0, 20);

        verify(gameRepository).findByDeletedFalseAndStatusIn(eq(upcoming), any(Pageable.class));
    }

    @Test
    void 역할이_HOST면_요청자가_파티장인_경기만_조회한다() {
        when(gameRepository.findByDeletedFalseAndStatusInAndHostId(eq(ALL_STATUSES), eq(REQUESTER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service().list(REQUESTER_ID, List.of(), GameListRole.HOST, 0, 20);

        verify(gameRepository)
                .findByDeletedFalseAndStatusInAndHostId(eq(ALL_STATUSES), eq(REQUESTER_ID), any(Pageable.class));
    }

    @Test
    void 역할이_JOINED면_요청자가_참가한_경기만_조회한다() {
        EnumSet<GameStatus> finished = EnumSet.of(GameStatus.FINISHED);
        when(gameRepository.findJoinedBy(eq(REQUESTER_ID), eq(finished), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service().list(REQUESTER_ID, List.of(GameStatus.FINISHED), GameListRole.JOINED, 0, 20);

        verify(gameRepository).findJoinedBy(eq(REQUESTER_ID), eq(finished), any(Pageable.class));
    }

    @Test
    void 조회한_경기의_참가_인원을_한_번에_세고_선수가_없으면_0이다() {
        Game crowded = game(10L);
        Game empty = game(11L);
        when(gameRepository.findByDeletedFalseAndStatusIn(eq(ALL_STATUSES), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(crowded, empty)));
        when(gamePlayerRepository.countPlayersByGameIds(List.of(10L, 11L)))
                .thenReturn(List.of(playerCount(10L, 8L)));

        Page<GameSummary> result = service().list(REQUESTER_ID, null, null, 0, 20);

        assertThat(result.getContent()).containsExactly(new GameSummary(crowded, 8L), new GameSummary(empty, 0L));
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    void 잘못된_페이지_조건은_저장소를_조회하지_않는다(int page, int size) {
        assertThatThrownBy(() -> service().list(REQUESTER_ID, null, null, page, size))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(GlobalErrorCode.INVALID_INPUT);
        verifyNoInteractions(gameRepository, gamePlayerRepository);
    }

    private GameListService service() {
        return new GameListService(gameRepository, gamePlayerRepository);
    }

    private Game game(Long id) {
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(id);
        return game;
    }

    private PlayerCount playerCount(Long gameId, long count) {
        return new PlayerCount() {
            @Override
            public Long getGameId() {
                return gameId;
            }

            @Override
            public long getPlayerCount() {
                return count;
            }
        };
    }
}
