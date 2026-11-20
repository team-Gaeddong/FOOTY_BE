package com.gaeddong.footy.domain.poll.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.domain.PollAnswer;
import com.gaeddong.footy.domain.poll.domain.PollOption;
import com.gaeddong.footy.domain.poll.domain.repository.PollAnswerRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollOptionRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollQueryService {

    private final PollReader pollReader;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollAnswerRepository pollAnswerRepository;
    private final Clock clock;

    /**
     * 경기의 투표를 최신순으로 조회한다. 파티장과 참가 회원만 볼 수 있다(D-35).
     */
    @Transactional(readOnly = true)
    public List<PollSummary> list(Long requesterId, Long gameId) {
        pollReader.getGameAsParticipant(gameId, requesterId);
        List<Poll> polls = pollRepository.findAllByGameIdOrderByCreatedAtDescIdDesc(gameId);
        if (polls.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> voterCounts = pollAnswerRepository.countVotersByGameId(gameId).stream()
                .collect(Collectors.toMap(PollAnswerRepository.VoterCount::getPollId,
                        PollAnswerRepository.VoterCount::getVoterCount));
        Set<Long> votedPollIds = new HashSet<>(pollAnswerRepository.findVotedPollIds(gameId, requesterId));
        LocalDateTime now = LocalDateTime.now(clock);

        return polls.stream()
                .map(poll -> new PollSummary(
                        poll,
                        voterCounts.getOrDefault(poll.getId(), 0L),
                        votedPollIds.contains(poll.getId()),
                        poll.isClosedAt(now)))
                .toList();
    }

    /**
     * 선택지별 득표 수와 투표자(닉네임 공개, D-35), 요청자가 고른 선택지를 함께 돌려준다.
     */
    @Transactional(readOnly = true)
    public PollDetail get(Long requesterId, Long gameId, Long pollId) {
        pollReader.getGameAsParticipant(gameId, requesterId);
        Poll poll = pollReader.getPoll(gameId, pollId);
        return toDetail(poll, requesterId);
    }

    private PollDetail toDetail(Poll poll, Long requesterId) {
        List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByIdAsc(poll.getId());
        List<PollAnswer> answers = pollAnswerRepository.findAllByPollOptionPollIdOrderByIdAsc(poll.getId());
        // 응답한 순서대로 투표자를 보여준다
        Map<Long, List<Member>> votersByOptionId = answers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getPollOption().getId(),
                        Collectors.mapping(PollAnswer::getMember, Collectors.toList())));
        long voterCount = answers.stream().map(answer -> answer.getMember().getId()).distinct().count();

        List<OptionResult> results = options.stream()
                .map(option -> {
                    List<Member> voters = votersByOptionId.getOrDefault(option.getId(), List.of());
                    boolean selected = voters.stream().anyMatch(voter -> voter.getId().equals(requesterId));
                    return new OptionResult(option, voters, selected);
                })
                .toList();
        return new PollDetail(poll, poll.isClosedAt(LocalDateTime.now(clock)), voterCount, results);
    }

    public record PollSummary(Poll poll, long voterCount, boolean voted, boolean closed) {
    }

    public record PollDetail(Poll poll, boolean closed, long voterCount, List<OptionResult> options) {
    }

    public record OptionResult(PollOption option, List<Member> voters, boolean selected) {
    }
}
