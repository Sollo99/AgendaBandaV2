package br.com.fatec.agendabanda.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public record ErroApi(String erro, String codigo) {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void escrever(HttpServletResponse response, int status, String codigo, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        JSON.writeValue(response.getWriter(), new ErroApi(mensagem, codigo));
    }
}
