package br.com.fatec.agendabanda.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class SenhaValidator implements ConstraintValidator<SenhaValida, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // Campo opcional na edição; @NotBlank torna a senha obrigatória nos demais fluxos.
        return valor == null || valor.isEmpty()
                || (!valor.isBlank() && valor.length() >= 6 && valor.getBytes(StandardCharsets.UTF_8).length <= 72);
    }
}
