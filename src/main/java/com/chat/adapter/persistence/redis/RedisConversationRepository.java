package com.chat.adapter.persistence.redis;

import com.chat.application.dto.ConversationSummary;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.domain.model.ConversationType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("redis")
public class RedisConversationRepository implements ConversationRepositoryPort {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<String> createDirectScript;

    public RedisConversationRepository(StringRedisTemplate redis) {
        this.redis = redis;
        this.createDirectScript = buildCreateDirectScript();
    }

    @Override
    public String getOrCreateDirect(String userA, String userB) {
        // clé canonique pour la conv directe (ordre stable, insensible à la casse)
        String directKey = directKey(userA, userB);
        // exécution du script Lua atomique
        String convId = redis.execute(
                createDirectScript,
                Arrays.asList(directKey, "conv:seq"),
                userA, userB
        );
        if (convId == null) {
            // fallback très rare: relire la clé (sécurité)
            convId = redis.opsForValue().get(directKey);
        }
        return convId;
    }

    @Override
    public boolean isMember(String conversationId, String username) {
        String key = "conv:" + conversationId + ":members";
        Boolean member = redis.opsForSet().isMember(key, username);
        return Boolean.TRUE.equals(member);
    }

    @Override
    public void addMember(String conversationId, String username) {
        String membersKey = "conv:" + conversationId + ":members";
        String userConvsKey = "user:" + username + ":convs";
        redis.opsForSet().add(membersKey, username);
        redis.opsForSet().add(userConvsKey, conversationId);
    }

    @Override
    public List<String> membersOf(String conversationId) {
        String key = "conv:" + conversationId + ":members";
        Set<String> m = redis.opsForSet().members(key);
        if (m == null) return Collections.emptyList();
        return new ArrayList<>(m);
    }

    @Override
    public List<ConversationSummary> listForUser(String username) {
        String userConvsKey = "user:" + username + ":convs";
        Set<String> ids = redis.opsForSet().members(userConvsKey);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        List<ConversationSummary> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            String base = "conv:" + id + ":";
            String typeStr = redis.opsForValue().get(base + "type");
            if (typeStr == null) continue;
            ConversationType type = ConversationType.valueOf(typeStr);
            List<String> members = membersOf(id);
            String name = redis.opsForValue().get(base + "name");
            if (type == ConversationType.DIRECT) {
                // pour DIRECT, on affiche le “peer” comme nom
                final String me = username;
                String peer = members.stream().filter(u -> !u.equals(me)).findFirst().orElse(null);
                out.add(new ConversationSummary(id, type, peer, members));
            } else {
                out.add(new ConversationSummary(id, type, name, members));
            }
        }
        // tri optionnel: DIRECT d’abord, puis GROUP, puis par nom
        return out.stream()
                .sorted(Comparator
                        .comparing((ConversationSummary s) -> s.getType() != ConversationType.DIRECT)
                        .thenComparing(s -> Optional.ofNullable(s.getName()).orElse(""))
                )
                .collect(Collectors.toList());
    }

    private String directKey(String a, String b) {
        boolean aFirst = a.compareToIgnoreCase(b) <= 0;
        String x = aFirst ? a : b;
        String y = aFirst ? b : a;
        return "conv:direct:" + x + "|" + y;
    }

    private DefaultRedisScript<String> buildCreateDirectScript() {
        // Lua: crée une conv DIRECT atomiquement si absente, sinon renvoie l’existante.
        // KEYS[1] = directKey (conv:direct:{a}|{b})
        // KEYS[2] = conv:seq
        // ARGV[1] = userA, ARGV[2] = userB
        String lua =
                "local directKey = KEYS[1]\n" +
                        "local seqKey = KEYS[2]\n" +
                        "local userA = ARGV[1]\n" +
                        "local userB = ARGV[2]\n" +
                        "local existing = redis.call('GET', directKey)\n" +
                        "if existing then return existing end\n" +
                        "local id = redis.call('INCR', seqKey)\n" +
                        "local convId = tostring(id)\n" +
                        "local base = 'conv:' .. convId .. ':'\n" +
                        "redis.call('SET', directKey, convId)\n" +
                        "redis.call('SET', base .. 'type', 'DIRECT')\n" +
                        "redis.call('SET', base .. 'name', '')\n" +
                        "redis.call('SADD', base .. 'members', userA, userB)\n" +
                        "redis.call('SADD', 'user:' .. userA .. ':convs', convId)\n" +
                        "redis.call('SADD', 'user:' .. userB .. ':convs', convId)\n" +
                        "return convId\n";
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(String.class);
        return script;
    }
}