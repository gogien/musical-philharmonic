package org.app.musical_philharmonic.dto;

import org.app.musical_philharmonic.entity.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class TicketResponse {
    private Integer id;
    private Integer concertId;
    private UUID buyerId;
    private String seatNumber;
    private LocalDateTime purchaseTimestamp;
    private TicketStatus status;
    private LocalDateTime reservationExpiration;
    private String paymentMethod;
    private String returnReason;
    private LocalDateTime returnTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public LocalDateTime getPurchaseTimestamp() {
        return purchaseTimestamp;
    }

    public void setPurchaseTimestamp(LocalDateTime purchaseTimestamp) {
        this.purchaseTimestamp = purchaseTimestamp;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public LocalDateTime getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(LocalDateTime returnTime) {
        this.returnTime = returnTime;
    }
}

