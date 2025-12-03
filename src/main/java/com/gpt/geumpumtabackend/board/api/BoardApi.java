package com.gpt.geumpumtabackend.board.api;

import com.gpt.geumpumtabackend.board.dto.BoardListResponse;
import com.gpt.geumpumtabackend.board.dto.BoardRequest;
import com.gpt.geumpumtabackend.board.dto.BoardResponse;
import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface BoardApi {

    @Operation(
            summary =  "게시글 목록 조회를 위한 api",
            description = "USER 이상의 권한을 가진 사용자는 게시글 목록(제목, 작성 날짜)을 조회할 수 있습니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoardListResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = BoardListResponse.class,
                    description = "게시글 목록 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/list")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<List<BoardListResponse>>> getBoardList(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "게시글 상세 조회를 위한 api",
            description = "USER 이상의 권한을 가진 사용자는 게시글을 상세 조회할 수 있습니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoardResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = BoardResponse.class,
                    description = "게시글 목록 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.BOARD_NOT_FOUND)
            }
    )
    @GetMapping("/{boardId}")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<BoardResponse>> getBoard(
            @Parameter(hidden = true) Long userId,
            @PathVariable Long boardId
    );

    @Operation(
            summary =  "게시글 작성을 위한 api",
            description = "ADMIN 권한을 가진 사용자만이 게시글을 작성할 수 있습니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoardResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = BoardResponse.class,
                    description = "게시글 작성 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @PostMapping
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<BoardResponse>> createBoard(
            @Parameter(hidden = true) Long userId,
            @RequestBody BoardRequest request
    );


    @Operation(
            summary =  "게시글 삭제를 위한 api",
            description = "ADMIN 권한을 가진 사용자만이 게시글을 삭제할 수 있습니다."

    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    description = "게시글 목록 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @DeleteMapping("/{boardId}")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<Void>> deleteBoard(
            @Parameter(hidden = true) Long userId,
            @PathVariable Long boardId
    );

}
