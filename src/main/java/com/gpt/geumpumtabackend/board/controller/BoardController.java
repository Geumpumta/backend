package com.gpt.geumpumtabackend.board.controller;

import com.gpt.geumpumtabackend.board.api.BoardApi;
import com.gpt.geumpumtabackend.board.dto.BoardListResponse;
import com.gpt.geumpumtabackend.board.dto.BoardRequest;
import com.gpt.geumpumtabackend.board.dto.BoardResponse;
import com.gpt.geumpumtabackend.board.service.BoardService;
import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board")
public class BoardController implements BoardApi {

    private final BoardService boardService;

    @GetMapping("/list")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<List<BoardListResponse>>> getBoardList(
            Long userId
    ){
        List<BoardListResponse> response = boardService.getBoardList(userId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/{boardId}")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<BoardResponse>> getBoard(
            Long userId,
            @PathVariable Long boardId
    ){
        BoardResponse response = boardService.getBoard(userId, boardId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @PostMapping
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<BoardResponse>> createBoard(
            Long userId,
            @RequestBody BoardRequest request
    ){
        BoardResponse response = boardService.createBoard(userId, request);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @DeleteMapping("/{boardId}")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<Void>> deleteBoard(
            Long userId,
            @PathVariable Long boardId
    ){
        boardService.deleteBoard(userId, boardId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

}
