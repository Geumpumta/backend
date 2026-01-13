package com.gpt.geumpumtabackend.board.dto;

import com.gpt.geumpumtabackend.board.domain.Board;

import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static BoardResponse from(Board board){
        return new BoardResponse(board.getId(), board.getTitle(), board.getContent(), board.getCreatedAt());
    }
}
