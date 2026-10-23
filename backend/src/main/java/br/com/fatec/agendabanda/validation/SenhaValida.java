package br.com.fatec.agendabanda.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SenhaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaValida {
    String message() default "A senha deve ter pelo menos 6 caracteres e no máximo 72 bytes em UTF-8.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
