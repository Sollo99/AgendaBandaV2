package br.com.fatec.agendabanda.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import br.com.fatec.agendabanda.exception.LimiteAutenticacaoException;

@Component
public class LimitadorAutenticacao {
    public enum Tipo { LOGIN, CADASTRO, RECUPERACAO }
    @Value("${app.auth.login.limite:20}") private int limiteLogin = 20;
    @Value("${app.auth.login.janela-minutos:15}") private long minutosLogin = 15;
    @Value("${app.auth.cadastro.limite:5}") private int limiteCadastro = 5;
    @Value("${app.auth.cadastro.janela-minutos:60}") private long minutosCadastro = 60;
    @Value("${app.auth.recuperacao.limite:10}") private int limiteRecuperacao = 10;
    @Value("${app.auth.recuperacao.janela-minutos:15}") private long minutosRecuperacao = 15;
    @Value("${app.auth.login.limite-global:200}") private int globalLogin = 200;
    @Value("${app.auth.cadastro.limite-global:50}") private int globalCadastro = 50;
    @Value("${app.auth.recuperacao.limite-global:100}") private int globalRecuperacao = 100;
    private static final int MAX_CHAVES = 10000;
    private final Clock clock;
    private final Map<String, Tentativas> tentativas = new HashMap<>();
    private record Tentativas(Instant fim, int quantidade) {}

    public LimitadorAutenticacao(Clock clock) { this.clock = clock; }

    @PostConstruct
    public void validarConfiguracao() {
        if (limiteLogin <= 0 || limiteCadastro <= 0 || limiteRecuperacao <= 0
                || minutosLogin <= 0 || minutosCadastro <= 0 || minutosRecuperacao <= 0
                || globalLogin <= 0 || globalCadastro <= 0 || globalRecuperacao <= 0) {
            throw new IllegalArgumentException("Limites e janelas de autenticação devem ser positivos.");
        }
    }

    public synchronized long segundosDeEspera(Tipo tipo, String ip) {
        long espera = contar(tipo, "ip:" + ip, limite(tipo));
        if (espera > 0) return espera;
        int global = switch (tipo) { case LOGIN -> globalLogin; case CADASTRO -> globalCadastro; case RECUPERACAO -> globalRecuperacao; };
        return contar(tipo, "global", global);
    }

    // O alvo é o mesmo para IPs diferentes; aliases de recuperação usam o código do membro.
    public synchronized void verificarIdentificador(Tipo tipo, String identificador) {
        long espera = contar(tipo, "alvo:" + hash(identificador.trim().toLowerCase(Locale.ROOT)), limite(tipo));
        if (espera > 0) throw new LimiteAutenticacaoException(espera);
    }

    private int limite(Tipo tipo) {
        return switch (tipo) { case LOGIN -> limiteLogin; case CADASTRO -> limiteCadastro; case RECUPERACAO -> limiteRecuperacao; };
    }

    private long contar(Tipo tipo, String escopo, int limite) {
        Duration janela = Duration.ofMinutes(switch (tipo) { case LOGIN -> minutosLogin; case CADASTRO -> minutosCadastro; case RECUPERACAO -> minutosRecuperacao; });
        String chave = tipo.name() + ":" + escopo;
        Instant agora = clock.instant();
        tentativas.entrySet().removeIf(item -> !item.getValue().fim().isAfter(agora));
        var atual = tentativas.get(chave);
        if (atual == null) {
            if (tentativas.size() >= MAX_CHAVES) return janela.toSeconds();
            tentativas.put(chave, new Tentativas(agora.plus(janela), 1));
            return 0;
        }
        if (atual.quantidade() >= limite) return Math.max(1, Duration.between(agora, atual.fim()).toSeconds());
        tentativas.put(chave, new Tentativas(atual.fim(), atual.quantidade() + 1));
        return 0;
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", ex);
        }
    }
}
