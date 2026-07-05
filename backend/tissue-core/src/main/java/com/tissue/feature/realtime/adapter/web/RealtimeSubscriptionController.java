package com.tissue.feature.realtime.adapter.web;

import com.tissue.feature.realtime.application.SseEmitterRegistry;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Opens the caller's realtime SSE stream.
 *
 * <p>One long-lived connection per session carries updates for every project the member belongs to.
 */
@Tag(name = "Realtime")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RealtimeSubscriptionController {

    private final SseEmitterRegistry registry;

    @Operation(operationId = "subscribeToEvents", summary = "Subscribe to the realtime event stream", description = """
                    Opens a long-lived Server Sent Events (SSE) stream that pushes changes
                    (issue and sprint updates) for every project the caller belongs to, so a
                    client can update in place instead of polling.

                    Each event's `data` is a JSON body describing what changed. The stream stays open
                    until the client disconnects or the server times it out, after which the client
                    reconnects.

                    **Requirements:**
                    - Requires authentication

                    **Client note:** this is a streaming endpoint. Consume the response
                    incrementally as an SSE stream (read it line by line as events arrive). Do not
                    call the generated client method for this operation. It buffers the whole
                    response and blocks until the stream closes.""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Event stream opened")})
    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentMember MemberDetails memberDetails) {
        return registry.subscribe(memberDetails.getMemberId());
    }
}
