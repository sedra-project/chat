package com.chat.application.port.out;

import java.util.List;

public interface ActiveUserRepositoryPort {
    boolean add(String username);
    boolean remove(String username);
    List<String> list();
    boolean contains(String username);
}