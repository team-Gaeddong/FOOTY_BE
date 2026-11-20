package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameListServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Test
    void 페이지_조회_조건과_정렬을_저장소에_전달한다() {
        Page<Game> expected = new PageImpl<>(List.of());
        when(gameRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(expected);
        GameListService service = new GameListService(gameRepository);

        Page<Game> result = service.list(2, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(gameRepository).findByDeletedFalse(pageable.capture());
        assertThat(result).isSameAs(expected);
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("playDate").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    void 잘못된_페이지_조건은_저장소를_조회하지_않는다(int page, int size) {
        GameListService service = new GameListService(gameRepository);

        assertThatThrownBy(() -> service.list(page, size))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(GlobalErrorCode.INVALID_INPUT);
        verify(gameRepository, never()).findByDeletedFalse(any(Pageable.class));
    }
}
