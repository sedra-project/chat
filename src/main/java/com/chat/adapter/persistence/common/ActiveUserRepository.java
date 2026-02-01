package com.chat.adapter.persistence.common;

import com.chat.application.port.out.ActiveUserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveUserRepository implements ActiveUserRepositoryPort {
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    @Override
    public boolean add(String username) {
        return users.add(username);
    }

    @Override
    public boolean remove(String username) {
        return users.remove(username);
    }

    @Override
    public List<String> list() {
        return new ArrayList<>(users);
    }

    @Override
    public boolean contains(String username) {
        return users.contains(username);
    }
}