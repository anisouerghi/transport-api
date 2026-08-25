package com.transport.reporting.dto;

/**
 * Résultat de création d'une réponse agent (réponse + statut d'envoi e-mail).
 */
public final class ReplyCreateResult {

    private final ReplyResponse reply;
    private final boolean replySaved;
    private final boolean success;
    private final String message;
    private final String errorCode;

    private ReplyCreateResult(Builder builder) {
        this.reply = builder.reply;
        this.replySaved = builder.replySaved;
        this.success = builder.success;
        this.message = builder.message;
        this.errorCode = builder.errorCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReplyResponse getReply() {
        return reply;
    }

    public boolean isReplySaved() {
        return replySaved;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static final class Builder {
        private ReplyResponse reply;
        private boolean replySaved;
        private boolean success;
        private String message;
        private String errorCode;

        public Builder reply(ReplyResponse reply) {
            this.reply = reply;
            return this;
        }

        public Builder replySaved(boolean replySaved) {
            this.replySaved = replySaved;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public ReplyCreateResult build() {
            return new ReplyCreateResult(this);
        }
    }
}
