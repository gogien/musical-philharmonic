package org.app.musical_philharmonic.dto;

public class TicketReturnRequest {
    private String reason = "customer request";
    private String actorEmail;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }
}

