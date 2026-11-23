package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 상태·역할·날짜 조건이 실제로 걸리는지는 DB가 필요해 GameListApiTest에서 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class GameListServiceTest {

    private static final Long REQUESTER_ID = 1L;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Test
    void 페이지_조건과_정렬을_저장소에_전달한다() {
        givenGames(List.of());

        Page<GameSummary> result = service().list(REQUESTER_ID, GameListCondition.none(), 2, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(gameRepository).findAll(anyCondition(), pageable.capture());
        assertThat(result.getContent()).isEmpty();
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("playDate").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
        // 빈 쪽이면 참가 인원을 세지 않는다
        verifyNoInteractions(gamePlayerRepository);
    }

    @Test
    void 조회한_경기의_참가_인원을_한_번에_세고_선수가_없으면_0이다() {
        Game crowded = game(10L);
        Game empty = game(11L);
        givenGames(List.of(crowded, empty));
        when(gamePlayerRepository.countPlayersByGameIds(List.of(10L, 11L)))
                .thenReturn(List.of(playerCount(10L, 8L)));

        Page<GameSummary> result = service().list(REQUESTER_ID, GameListCondition.none(), 0, 20);

        assertThat(result.getContent()).containsExactly(new GameSummary(crowded, 8L), new GameSummary(empty, 0L));
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    void 잘못된_페이지_조건은_저장소를_조회하지_않는다(int page, int size) {
        assertThatThrownBy(() -> service().list(REQUESTER_ID, GameListCondition.none(), page, size))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(GlobalErrorCode.INVALID_INPUT);
        verifyNoInteractions(gameRepository, gamePlayerRepository);
    }

    @Test
    void 시작일이_종료일보다_늦으면_저장소를_조회하지_않는다() {
        GameListCondition reversed = new GameListCondition(
                null, null, LocalDate.of(2030, 6, 2), LocalDate.of(2030, 6, 1));

        assertThatThrownBy(() -> service().list(REQUESTER_ID, reversed, 0, 20))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(GlobalErrorCode.INVALID_INPUT);
        verifyNoInteractions(gameRepository, gamePlayerRepository);
    }

    private GameListService service() {
        return new GameListService(gameRepository, gamePlayerRepository);
    }

    private void givenGames(List<Game> games) {
        when(gameRepository.findAll(anyCondition(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(games));
    }

    private Specification<Game> anyCondition() {
        return any();
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
