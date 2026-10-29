package br.com.fatec.agendabanda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private ResponseEntity<Map<String, String>> resposta(HttpStatus status, String codigo, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem, "codigo", codigo));
    }

    @ExceptionHandler(LimiteAutenticacaoException.class)
    public ResponseEntity<Map<String, String>> limiteAutenticacao(LimiteAutenticacaoException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", Long.toString(ex.getSegundos()))
                .body(Map.of("erro", ex.getMessage(), "codigo", "LIMITE_REQUISICOES"));
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, SessaoExpiradaException.class})
    public ResponseEntity<Map<String, String>> credenciaisInvalidas(IllegalArgumentException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", ex.getMessage());
    }

    @ExceptionHandler(RecuperacaoExpiradaException.class)
    public ResponseEntity<Map<String, String>> recuperacaoExpirada(RecuperacaoExpiradaException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "RECUPERACAO_EXPIRADA", ex.getMessage());
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, String>> naoEncontrado(Exception ex) {
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO",
                ex instanceof RecursoNaoEncontradoException ? ex.getMessage() : "Recurso não encontrado.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> erroIntegridade(DataIntegrityViolationException ex) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO", "Não foi possível salvar ou excluir: há dados duplicados, inválidos ou vínculos com outros registros.");
    }

    @ExceptionHandler({ConcurrencyFailureException.class, ConflitoException.class})
    public ResponseEntity<Map<String, String>> erroConcorrencia(Exception ex) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO", ex instanceof ConflitoException ? ex.getMessage()
                : "Outro usuário está alterando esses dados. Aguarde e tente novamente.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> erroRegra(IllegalArgumentException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "REGRA_INVALIDA", ex instanceof RegraNegocioException ? ex.getMessage() : "Dados inválidos para esta operação.");
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> erroPermissao(SecurityException ex) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Você não tem permissão para executar esta ação.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> erroValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream().findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("Dados inválidos.");
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", mensagem);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> parametrosInvalidos(Exception ex) {
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", "Confira o formato e os valores dos campos informados.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> metodoInvalido(HttpRequestMethodNotSupportedException ex) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_INVALIDO", "Método não permitido para este recurso.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> erroInterno(Exception ex) {
        LoggerFactory.getLogger(ApiExceptionHandler.class).error("Falha interna ao processar a requisicao.", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Não foi possível concluir a operação. Tente novamente mais tarde.");
    }
}
