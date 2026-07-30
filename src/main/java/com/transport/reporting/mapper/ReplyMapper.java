package com.transport.reporting.mapper;

import com.transport.reporting.dto.ReplyResponse;
import com.transport.reporting.entity.Reply;
import org.springframework.stereotype.Component;

/**
 * Mapper Reply : conversion Entity -> DTO.
 */
@Component
public class ReplyMapper {

    public ReplyResponse toResponse(Reply reply) {
        return ReplyResponse.builder()
                .replyId(reply.getReplyId())
                .message(reply.getMessage())
                .replyDate(reply.getReplyDate())
                .emailSent(reply.isEmailSent())
                .reportId(reply.getReport().getReportId())
                .userId(reply.getAppUser() != null ? reply.getAppUser().getUserId() : null)
                .build();
    }
}
