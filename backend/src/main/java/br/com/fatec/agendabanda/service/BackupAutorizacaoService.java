package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.BackupDtos.Ator;
import br.com.fatec.agendabanda.exception.RegraNegocioException;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import br.com.fatec.agendabanda.security.LimitadorAutenticacao;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupAutorizacaoService {
    private final MembroRepository membros;
    private final BCryptPasswordEncoder senhas;
    private final LimitadorAutenticacao limitador;
    public BackupAutorizacaoService(MembroRepository membros,BCryptPasswordEncoder senhas,LimitadorAutenticacao limitador) {
        this.membros=membros;this.senhas=senhas;this.limitador=limitador;
    }
    @Transactional(readOnly=true)
    public Ator verificar(SessaoUsuario sessao,String senhaAtual,String ip) {
        var membro=membros.findById(sessao.codigo()).orElseThrow(() -> new SecurityException("Administrador não encontrado."));
        if (!Boolean.TRUE.equals(membro.getAdministrador()) || !Boolean.TRUE.equals(membro.getAtivo()) || !"APROVADO".equals(membro.getStatusCadastro()))
            throw new SecurityException("Apenas administradores ativos podem gerenciar backups.");
        if (senhaAtual!=null) {
            String alvo="backup:"+membro.getCodigo();
            limitador.verificarLogin(alvo,ip);
            if (!senhas.matches(senhaAtual,membro.getSenha())) {
                limitador.registrarFalhaLogin(alvo,ip);
                throw new RegraNegocioException("A senha atual informada é inválida.");
            }
            limitador.limparFalhasLogin(alvo,ip);
        }
        return new Ator(membro.getCodigo(),membro.getNome());
    }
}
