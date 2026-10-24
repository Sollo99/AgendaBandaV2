package br.com.fatec.agendabanda.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class LimitadorAutenticacao {
    public enum Tipo { LOGIN, CADASTRO, RECUPERACAO }
    @Value("${app.auth.login.limite:20}") private int limiteLogin = 20;
    @Value("${app.auth.login.janela-minutos:15}") private long minutosLogin = 15;
    @Value("${app.auth.cadastro.limite:5}") private int limiteCadastro = 5;
    @Value("${app.auth.cadastro.janela-minutos:60}") private long minutosCadastro = 60;
    @Value("${app.auth.recuperacao.limite:10}") private int limiteRecuperacao = 10;
    @Value("${app.auth.recuperacao.janela-minutos:15}") private long minutosRecuperacao = 15;
    private static final int MAX_IPS = 10000;
    private final Clock clock;
    private final Map<String, Tentativas> tentativas = new HashMap<>();
    private record Tentativas(Instant fim, int quantidade) {}

    public LimitadorAutenticacao(Clock clock) { this.clock = clock; }

    @PostConstruct
    public void validarConfiguracao() {
        if (limiteLogin <= 0 || limiteCadastro <= 0 || limiteRecuperacao <= 0
                || minutosLogin <= 0 || minutosCadastro <= 0 || minutosRecuperacao <= 0) {
            throw new IllegalArgumentException("Limites e janelas de autenticação devem ser positivos.");
        }
    }

    public synchronized long segundosDeEspera(Tipo tipo, String ip) {
        int limite = switch (tipo) { case LOGIN -> limiteLogin; case CADASTRO -> limiteCadastro; case RECUPERACAO -> limiteRecuperacao; };
        Duration janela = Duration.ofMinutes(switch (tipo) { case LOGIN -> minutosLogin; case CADASTRO -> minutosCadastro; case RECUPERACAO -> minutosRecuperacao; });
        String chave = tipo.name() + ":" + ip;
        Instant agora = clock.instant();
        tentativas.entrySet().removeIf(item -> !item.getValue().fim().isAfter(agora));
        var atual = tentativas.get(chave);
        if (atual == null) {
            if (tentativas.size() >= MAX_IPS) return janela.toSeconds();
            tentativas.put(chave, new Tentativas(agora.plus(janela), 1));
            return 0;
        }
        if (atual.quantidade() >= limite) return Math.max(1, Duration.between(agora, atual.fim()).toSeconds());
        tentativas.put(chave, new Tentativas(atual.fim(), atual.quantidade() + 1));
        return 0;
    }
}
