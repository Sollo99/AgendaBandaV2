package br.com.fatec.agendabanda.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    @Value("${app.auth.cadastro.limite-global:50}") private int globalCadastro = 50;
    @Value("${app.auth.recuperacao.limite-global:100}") private int globalRecuperacao = 100;
    private static final int MAX_CHAVES = 10000;
    private final Clock clock;
    private final Map<String, Tentativas> tentativas = new LinkedHashMap<>(128, 0.75f, true);
    private record Tentativas(Instant fim, int quantidade) {}

    public LimitadorAutenticacao(Clock clock) { this.clock = clock; }

    @PostConstruct
    public void validarConfiguracao() {
        if (limiteLogin <= 0 || limiteCadastro <= 0 || limiteRecuperacao <= 0
                || minutosLogin <= 0 || minutosCadastro <= 0 || minutosRecuperacao <= 0
                || globalCadastro <= 0 || globalRecuperacao <= 0) {
            throw new IllegalArgumentException("Limites e janelas de autenticação devem ser positivos.");
        }
    }

    public synchronized long segundosDeEspera(Tipo tipo, String ip) {
        long espera = contar(tipo, "ip:" + ip, limite(tipo));
        if (espera > 0) return espera;
        if (tipo == Tipo.LOGIN) return 0; // Nenhuma origem bloqueia todos os logins por um teto compartilhado.
        int global = tipo == Tipo.CADASTRO ? globalCadastro : globalRecuperacao;
        return contar(tipo, "global", global);
    }

    // Um solicitante nao pode esgotar o limite do mesmo alvo para outras origens.
    public synchronized void verificarIdentificador(Tipo tipo, String identificador, String ip) {
        if (tipo == Tipo.LOGIN) throw new IllegalArgumentException("Use o limitador de falhas de login por conta e IP.");
        long espera = contar(tipo, "alvo:" + hash(identificador.trim().toLowerCase(Locale.ROOT)) + ":ip:" + hash(ip), limite(tipo));
        if (espera > 0) throw new LimiteAutenticacaoException(espera);
    }

    public synchronized void verificarLogin(String identificador, String ip) {
        Instant agora = clock.instant();
        removerExpiradas(agora);
        var atual = tentativas.get(chaveLogin(identificador, ip));
        if (atual != null && atual.quantidade() >= limiteLogin) {
            throw new LimiteAutenticacaoException(Math.max(1, Duration.between(agora, atual.fim()).toSeconds()));
        }
    }

    public synchronized void registrarFalhaLogin(String identificador, String ip) {
        long espera = contar(Tipo.LOGIN, escopoLogin(identificador, ip), limiteLogin);
        if (espera > 0) throw new LimiteAutenticacaoException(espera);
    }

    public synchronized void limparFalhasLogin(String identificador, String ip) {
        tentativas.remove(chaveLogin(identificador, ip));
    }

    private String chaveLogin(String identificador, String ip) { return Tipo.LOGIN.name() + ":" + escopoLogin(identificador, ip); }

    private String escopoLogin(String identificador, String ip) {
        return "falha:" + hash(identificador.trim().toLowerCase(Locale.ROOT)) + ":" + hash(ip);
    }

    private void removerExpiradas(Instant agora) {
        tentativas.entrySet().removeIf(item -> !item.getValue().fim().isAfter(agora));
    }

    private int limite(Tipo tipo) {
        return switch (tipo) { case LOGIN -> limiteLogin; case CADASTRO -> limiteCadastro; case RECUPERACAO -> limiteRecuperacao; };
    }

    private long contar(Tipo tipo, String escopo, int limite) {
        Duration janela = Duration.ofMinutes(switch (tipo) { case LOGIN -> minutosLogin; case CADASTRO -> minutosCadastro; case RECUPERACAO -> minutosRecuperacao; });
        String chave = tipo.name() + ":" + escopo;
        Instant agora = clock.instant();
        removerExpiradas(agora);
        var atual = tentativas.get(chave);
        if (atual == null) {
            // Saturacao da memoria nao vira um bloqueio global para novos usuarios.
            if (tentativas.size() >= MAX_CHAVES) tentativas.remove(tentativas.keySet().iterator().next());
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
