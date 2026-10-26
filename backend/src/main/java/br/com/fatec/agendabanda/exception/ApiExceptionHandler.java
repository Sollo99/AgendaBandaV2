package br.com.fatec.agendabanda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(LimiteAutenticacaoException.class)
    public ResponseEntity<Map<String, String>> limiteAutenticacao(LimiteAutenticacaoException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(ex.getSegundos())).body(Map.of("erro", ex.getMessage()));
    }
    @ExceptionHandler({CredenciaisInvalidasException.class, SessaoExpiradaException.class})
    public ResponseEntity<Map<String, String>> credenciaisInvalidas(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(RecuperacaoExpiradaException.class)
    public ResponseEntity<Map<String, String>> recuperacaoExpirada(RecuperacaoExpiradaException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage(), "codigo", "RECUPERACAO_EXPIRADA"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> erroIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro",
                "Não foi possível salvar ou excluir: há dados duplicados, inválidos ou vínculos com outros registros."));
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Map<String, String>> erroConcorrencia(ConcurrencyFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro",
                "Outro usuário está alterando esses dados. Aguarde e tente novamente."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> erroRegra(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> erroPermissao(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> erroValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Dados inválidos.");
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem));
    }
}
