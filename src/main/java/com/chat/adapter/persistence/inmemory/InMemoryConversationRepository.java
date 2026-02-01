package com.chat.adapter.persistence.inmemory;

import com.chat.application.dto.ConversationSummary;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.domain.model.ConversationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@Profile("inmem")
public class InMemoryConversationRepository implements ConversationRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryConversationRepository.class);

    private final AtomicLong seq = new AtomicLong(1000);

    // convId -> membres
    private final Map<String, Set<String>> membersByConv = new ConcurrentHashMap<>();
    // clé canonique DM -> convId
    private final Map<String, String> directKeyToId = new ConcurrentHashMap<>();
    // convId -> type
    private final Map<String, ConversationType> typeByConv = new ConcurrentHashMap<>();
    // convId -> nom (pour GROUP)
    private final Map<String, String> nameByConv = new ConcurrentHashMap<>();
    // user -> set convIds
    private final Map<String, Set<String>> convsByUser = new ConcurrentHashMap<>();

    @Override
    public String getOrCreateDirect(String userA, String userB) {
        String key = directKey(userA, userB);
        String id = directKeyToId.get(key);
        if (id != null) return id;

        synchronized (this) {
            id = directKeyToId.get(key);
            if (id == null) {
                id = String.valueOf(seq.incrementAndGet());
                directKeyToId.put(key, id);
                typeByConv.put(id, ConversationType.DIRECT);

                // membres
                Set<String> members = ConcurrentHashMap.newKeySet();
                members.add(userA);
                members.add(userB);
                membersByConv.put(id, members);

                // index utilisateurs
                convsByUser.computeIfAbsent(userA, k -> ConcurrentHashMap.newKeySet()).add(id);
                convsByUser.computeIfAbsent(userB, k -> ConcurrentHashMap.newKeySet()).add(id);
                log.info("Created DIRECT conversation id={} users=[{},{}]", id, userA, userB);
            }
        }
        return id;
    }

    @Override
    public boolean isMember(String conversationId, String username) {
        Set<String> m = membersByConv.get(conversationId);
        boolean res = (m != null && m.contains(username));
        log.info("isMember? conv={} user={} -> {} members={}", conversationId, username, res, membersByConv.get(conversationId));
        return res;
    }

    @Override
    public void addMember(String conversationId, String username) {
        membersByConv.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(username);
        convsByUser.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(conversationId);
    }

    @Override
    public List<String> membersOf(String conversationId) {
        Set<String> m = membersByConv.get(conversationId);
        return m == null ? Collections.emptyList() : new ArrayList<>(m);
    }

    @Override
    public List<ConversationSummary> listForUser(String username) {
        Set<String> ids = convsByUser.getOrDefault(username, Collections.emptySet());
        return ids.stream().map(id -> {
                    ConversationType t = typeByConv.get(id);
                    String name = nameByConv.get(id);
                    List<String> members = membersOf(id);
                    if (t == ConversationType.DIRECT) {
                        String peer = members.stream().filter(u -> !u.equals(username)).findFirst().orElse(null);
                        return new ConversationSummary(id, t, peer, members);
                    }
                    return new ConversationSummary(id, t, name, members);
                }).sorted(Comparator
                        .comparing((ConversationSummary s) -> s.getType() != ConversationType.DIRECT)
                        .thenComparing(s -> Optional.ofNullable(s.getName()).orElse("")))
                .collect(Collectors.toList());
    }

    private String directKey(String a, String b) {
        boolean aFirst = a.compareToIgnoreCase(b) <= 0;
        String x = aFirst ? a : b;
        String y = aFirst ? b : a;
        return x + "|" + y;
    }
}