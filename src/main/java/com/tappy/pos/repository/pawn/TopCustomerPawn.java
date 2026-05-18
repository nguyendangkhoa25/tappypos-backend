package com.tappy.pos.repository.pawn;

public interface TopCustomerPawn {
    Long getCustomerId();
    String getPhone();
    String getLastName();
    Integer getTotalCount();
    Double getTotalAmount();
    Double getInterestAmount();
}
