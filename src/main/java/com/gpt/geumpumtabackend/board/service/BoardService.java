package com.gpt.geumpumtabackend.board.service;

import com.gpt.geumpumtabackend.board.domain.Board;
import com.gpt.geumpumtabackend.board.dto.BoardListResponse;
import com.gpt.geumpumtabackend.board.dto.BoardRequest;
import com.gpt.geumpumtabackend.board.dto.BoardResponse;
import com.gpt.geumpumtabackend.board.repository.BoardRepository;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    public List<BoardListResponse> getBoardList(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        List<Board> response = boardRepository.findTop10ByOrderByCreatedAtDesc();

        return response.stream()
                .map(BoardListResponse::from)
                .toList();
    }

    public BoardResponse getBoard(Long userId, Long boardId) {
        userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        Board response = boardRepository.findById(boardId)
                .orElseThrow(()->new BusinessException(ExceptionType.BOARD_NOT_FOUND));

        return BoardResponse.from(response);
    }

    @Transactional
    public BoardResponse createBoard(Long userId, BoardRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        return BoardResponse.from(
                boardRepository.save(
                        Board.builder()
                                .title(request.title())
                                .content(request.content())
                                .build()
        ));
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        boardRepository.deleteById(boardId);
    }
}
