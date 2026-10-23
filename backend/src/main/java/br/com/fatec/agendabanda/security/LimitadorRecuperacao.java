package br.com.fatec.agendabanda.security;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class LimitadorRecuperacao {
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final int LIMITE = 10;
    private static final int MAX_IPS = 10000;
    private final Clock clock;
    private final Map<String, Tentativas> tentativas = new HashMap<>();
    private record Tentativas(Instant fim, int quantidade) {}

    public LimitadorRecuperacao() { this(Clock.systemUTC()); }
    LimitadorRecuperacao(Clock clock) { this.clock = clock; }

    public synchronized long segundosDeEspera(String ip) {
        Instant agora = clock.instant();
        tentativas.entrySet().removeIf(item -> !item.getValue().fim().isAfter(agora));
        var atual = tentativas.get(ip);
        if (atual == null) {
            if (tentativas.size() >= MAX_IPS) return JANELA.toSeconds();
            tentativas.put(ip, new Tentativas(agora.plus(JANELA), 1));
            return 0;
        }
        if (atual.quantidade() >= LIMITE) return Math.max(1, Duration.between(agora, atual.fim()).toSeconds());
        tentativas.put(ip, new Tentativas(atual.fim(), atual.quantidade() + 1));
        return 0;
    }
}
