package br.com.fatec.agendabanda.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class DataHoraJsonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer datasComOffset() {
        return builder -> {
            builder.deserializerByType(LocalDateTime.class, new com.fasterxml.jackson.databind.JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws IOException {
                    String valor = parser.getValueAsString();
                    if (valor == null) throw com.fasterxml.jackson.databind.JsonMappingException.from(parser, "Data e hora inválidas.");
                    try {
                        if (valor.matches(".*(?:Z|[+-][0-9]{2}:[0-9]{2})$")) {
                            return java.time.OffsetDateTime.parse(valor).atZoneSameInstant(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
                        }
                        return LocalDateTime.parse(valor);
                    } catch (java.time.DateTimeException ex) {
                        throw com.fasterxml.jackson.databind.JsonMappingException.from(parser, "Data e hora inválidas.");
                    }
                }
            });
            builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider) throws IOException {
                    generator.writeString(value.atZone(ZoneId.of("America/Sao_Paulo"))
                            .toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }
            });
        };
    }
}
