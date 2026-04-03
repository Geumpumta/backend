package com.gpt.geumpumtabackend.board.dto;

import com.gpt.geumpumtabackend.board.domain.Board;

import java.time.LocalDateTime;

public record BoardListResponse(
        Long id,
        String title,
        LocalDateTime createdAt
) {
    public static BoardListResponse from(Board board){
        return new BoardListResponse(board.getId(), board.getTitle(), board.getCreatedAt());
    }
}
