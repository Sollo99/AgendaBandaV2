package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;
import br.com.fatec.agendabanda.repository.MembroRepository;
import org.springframework.stereotype.Component;

@Component
public class ValidadorMembro {
    private final MembroRepository membroRepository;
    public ValidadorMembro(MembroRepository membroRepository) { this.membroRepository = membroRepository; }
    public void validarUnicidade(String cpf, String email, String login, Integer codigoAtual) {
        membroRepository.findByLogin(login.trim())
                .filter(membro -> !membro.getCodigo().equals(codigoAtual))
                .ifPresent(membro -> { throw new RegraNegocioException("Login já cadastrado."); });
        membroRepository.findByEmailIgnoreCase(email.trim())
                .filter(membro -> !membro.getCodigo().equals(codigoAtual))
                .ifPresent(membro -> { throw new RegraNegocioException("E-mail já cadastrado."); });
        membroRepository.findByCpf(normalizarCpf(cpf))
                .filter(membro -> !membro.getCodigo().equals(codigoAtual))
                .ifPresent(membro -> { throw new RegraNegocioException("CPF já cadastrado."); });
    }


    public static String normalizarCpf(String cpf) { return cpf.replaceAll("[^0-9]", ""); }
}
