package com.gaeddong.footy.domain.poll.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.service.MemberReader;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.domain.PollAnswer;
import com.gaeddong.footy.domain.poll.domain.PollOption;
import com.gaeddong.footy.domain.poll.domain.repository.PollAnswerRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollOptionRepository;
import com.gaeddong.footy.domain.poll.exception.PollErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollAnswerService {

    private final MemberReader memberReader;
    private final PollReader pollReader;
    private final PollOptionRepository pollOptionRepository;
    private final PollAnswerRepository pollAnswerRepository;
    private final Clock clock;

    /**
     * 참가 회원이 투표한다. 마감 전에는 다시 보내 응답을 전부 바꿀 수 있고, 빈 목록이면 투표를 취소한다(D-34).
     * 하나만 고를 수 있는 투표면 선택지는 0~1개다.
     */
    @Transactional
    public void answer(Long memberId, Long gameId, Long pollId, List<Long> optionIds) {
        if (new HashSet<>(optionIds).size() != optionIds.size()) {
            throw new FootyException(PollErrorCode.DUPLICATE_OPTION);
        }
        Member member = memberReader.getActiveMember(memberId);
        pollReader.getGameAsParticipant(gameId, memberId);
        Poll poll = pollReader.getPoll(gameId, pollId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (poll.isClosedAt(now)) {
            throw new FootyException(PollErrorCode.POLL_CLOSED);
        }
        if (!poll.isAllowMultiple() && optionIds.size() > 1) {
            throw new FootyException(PollErrorCode.MULTIPLE_NOT_ALLOWED);
        }
        List<PollOption> chosen = findOptions(poll, optionIds);

        pollAnswerRepository.deleteAllByPollIdAndMemberId(poll.getId(), memberId);
        saveAnswers(chosen.stream()
                .map(option -> PollAnswer.builder().pollOption(option).member(member).createdAt(now).build())
                .toList());
    }

    // 같은 회원이 동시에 두 번 보내면 지우고 다시 넣는 사이에 uk_poll_answer에 걸릴 수 있다
    private void saveAnswers(List<PollAnswer> answers) {
        try {
            pollAnswerRepository.saveAllAndFlush(answers);
        } catch (DataIntegrityViolationException e) {
            throw new FootyException(PollErrorCode.CONCURRENT_ANSWER);
        }
    }

    // 하나라도 이 투표의 선택지가 아니면 아무것도 반영하지 않는다
    private List<PollOption> findOptions(Poll poll, List<Long> optionIds) {
        Map<Long, PollOption> optionsById = pollOptionRepository.findAllByPollIdOrderByIdAsc(poll.getId()).stream()
                .collect(Collectors.toMap(PollOption::getId, Function.identity()));
        if (!optionsById.keySet().containsAll(optionIds)) {
            throw new FootyException(PollErrorCode.OPTION_NOT_FOUND);
        }
        return optionIds.stream().map(optionsById::get).toList();
    }
}
