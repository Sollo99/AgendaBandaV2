package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.BackupDtos.*;
import br.com.fatec.agendabanda.dto.PaginaDto;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@RestController
@RequestMapping("/api/backups")
public class BackupController {
    private final BackupService backups;
    private final BackupAutorizacaoService autorizacao;
    private final TokenService tokens;
    public BackupController(BackupService backups,BackupAutorizacaoService autorizacao,TokenService tokens) {
        this.backups=backups;this.autorizacao=autorizacao;this.tokens=tokens;
    }
    private Ator admin(HttpServletRequest http,String senha) { return autorizacao.verificar(tokens.sessaoObrigatoria(http),senha,http.getRemoteAddr()); }
    @GetMapping("/configuracao")
    public Painel configuracao(HttpServletRequest http) { admin(http,null);return backups.painel(); }
    @PutMapping("/configuracao")
    public Painel configurar(@Valid @RequestBody ConfiguracaoRequest request,HttpServletRequest http) {
        admin(http,null);return backups.configurar(request.preferencias());
    }
    @GetMapping
    public PaginaDto<Registro> listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest http) {
        admin(http,null);return backups.listar(page,size);
    }
    @PostMapping
    public ResponseEntity<OperacaoAceita> criar(HttpServletRequest http) {
        return ResponseEntity.accepted().body(backups.backup(admin(http,null)));
    }
    @PostMapping("/{id}/restaurar")
    public ResponseEntity<OperacaoAceita> restaurar(@PathVariable UUID id,@Valid @RequestBody RestaurarRequest request,HttpServletRequest http) {
        Ator ator=admin(http,request.senhaAtual());
        return ResponseEntity.accepted().body(backups.restaurar(id,ator,request.confirmacao()));
    }
    @GetMapping("/operacoes/{id}")
    public Progresso operacao(@PathVariable UUID id,HttpServletRequest http) {
        admin(http,null);return backups.progresso(id);
    }
    @GetMapping("/{id}/arquivo")
    public ResponseEntity<FileSystemResource> arquivo(@PathVariable UUID id,HttpServletRequest http) throws IOException {
        admin(http,null);
        var arquivo=backups.arquivoDownload(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(arquivo))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(arquivo.getFileName().toString()).build().toString())
                .body(new FileSystemResource(arquivo));
    }
}
