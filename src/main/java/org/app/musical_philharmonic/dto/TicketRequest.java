package org.app.musical_philharmonic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.app.musical_philharmonic.entity.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class TicketRequest {
    @NotNull
    private Integer concertId;

    private UUID buyerId;

    @NotBlank
    @Size(max = 10)
    private String seatNumber;

    private TicketStatus status;

    private LocalDateTime reservationExpiration;

    public Integer getConcertId() {
        return concertId;
    }

    public void setConcertId(Integer concertId) {
        this.concertId = concertId;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID buyerId) {
        this.buyerId = buyerId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getReservationExpiration() {
        return reservationExpiration;
    }

    public void setReservationExpiration(LocalDateTime reservationExpiration) {
        this.reservationExpiration = reservationExpiration;
    }
}

