package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.game.service.GameListRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 쿼리 값 role=host처럼 소문자로 온 값도 받는다. 모르는 값은 타입 불일치로 400이 된다.
 */
@Component
public class GameListRoleConverter implements Converter<String, GameListRole> {

    @Override
    public GameListRole convert(String source) {
        return GameListRole.valueOf(source.trim().toUpperCase(Locale.ROOT));
    }
}
