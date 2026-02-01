package com.chat.application.port.out;

public interface UsernameReservationPort {
    boolean tryReserve(String username);  // true si réservation OK, false si déjà réservé
    void release(String username);        // utile S2, pas utilisé en S1
}