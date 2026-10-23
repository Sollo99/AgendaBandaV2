package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CidadeRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Pattern(regexp = "(?i)AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO", message = "Informe uma UF brasileira válida.") String uf
) {
}
