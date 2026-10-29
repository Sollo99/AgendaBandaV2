package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.dto.*;
import br.com.fatec.agendabanda.model.Membro;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.MusicoRepository;
import br.com.fatec.agendabanda.exception.RecursoNaoEncontradoException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilPessoalService {
    private final MembroRepository membroRepository;
    private final MusicoRepository musicoRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MapperService mapperService;
    private final AuditoriaContexto auditoria;
    private final ValidadorMembro validador;
    private final SenhaMembroService senhas;

    public PerfilPessoalService(MembroRepository membroRepository, MusicoRepository musicoRepository,
            BCryptPasswordEncoder passwordEncoder, MapperService mapperService, AuditoriaContexto auditoria,
            ValidadorMembro validador, SenhaMembroService senhas) {
        this.membroRepository=membroRepository; this.musicoRepository=musicoRepository;
        this.passwordEncoder=passwordEncoder; this.mapperService=mapperService; this.auditoria=auditoria;
        this.validador=validador; this.senhas=senhas;
    }

    @Transactional(readOnly = true)
    public MembroDto obterProprio(Integer codigoMembro) {
        return mapperService.membroDto(buscarEntidade(codigoMembro));
    }

    @Transactional
    public MembroDto atualizarProprio(Integer codigoMembro, PerfilRequest request) {
        auditoria.prepararDoRequest();
        var membro = buscarParaAtualizar(codigoMembro);
        validador.validarUnicidade(request.cpf(), request.email(), request.login(), codigoMembro);
        membro.setNome(request.nome().trim());
        membro.setCpf(ValidadorMembro.normalizarCpf(request.cpf()));
        membro.setEmail(request.email().trim());
        membro.setLogin(request.login().trim());
        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            if (request.senhaAtual() == null || !passwordEncoder.matches(request.senhaAtual(), membro.getSenha())) {
                throw new RegraNegocioException("A senha atual informada é inválida.");
            }
            senhas.alterar(membro, request.novaSenha());
        }
        var salvo = membroRepository.save(membro);
        musicoRepository.findById(codigoMembro).ifPresent(musico -> {
            if (request.instrumento() != null) musico.setInstrumento(request.instrumento().trim());
            if (request.biografia() != null) musico.setBiografia(request.biografia().trim());
            musicoRepository.save(musico);
        });
        return mapperService.membroDto(salvo);
    }


    private Membro buscarEntidade(Integer id) {
        return membroRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
    }

    private Membro buscarParaAtualizar(Integer id) {
        return membroRepository.bloquearPorCodigo(id).orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
    }
}
