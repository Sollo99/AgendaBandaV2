package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.model.Membro;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.exception.RecursoNaoEncontradoException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SenhaMembroService {
    private final MembroRepository membros;
    private final BCryptPasswordEncoder encoder;
    private final CicloRecuperacaoService ciclo;
    private final TokenService tokens;
    private final AuditoriaContexto auditoria;

    public SenhaMembroService(MembroRepository membros, BCryptPasswordEncoder encoder, CicloRecuperacaoService ciclo,
            TokenService tokens, AuditoriaContexto auditoria) {
        this.membros=membros; this.encoder=encoder; this.ciclo=ciclo; this.tokens=tokens; this.auditoria=auditoria;
    }

    // Chamadores administrativos/de perfil ja identificaram o ator e bloquearam o membro.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void alterar(Membro membro, String novaSenha) {
        membro.setSenha(encoder.encode(novaSenha));
        ciclo.cancelarAbertas(membro.getCodigo(), "Senha alterada por outro caminho.");
        tokens.removerSessoesDoMembro(membro.getCodigo());
    }

    @Transactional
    public void redefinirSenha(Membro membro, String novaSenha) {
        auditoria.preparar(membro.getCodigo());
        var bloqueado = membros.bloquearPorCodigo(membro.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
        alterar(bloqueado, novaSenha);
        membros.save(bloqueado);
    }
}
