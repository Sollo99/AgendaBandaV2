package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.ConflitoException;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class BackupManutencao {
    private final ReentrantReadWriteLock lock=new ReentrantReadWriteLock(true);
    private volatile boolean bloqueada;
    public boolean ativa() { return bloqueada; }
    public void bloquear() { bloqueada=true; }
    public void liberar() { bloqueada=false; }
    public Permissao tentarLeitura() {
        if (bloqueada || !lock.readLock().tryLock()) return null;
        if (bloqueada) { lock.readLock().unlock(); return null; }
        return new Permissao(() -> lock.readLock().unlock());
    }
    public Permissao iniciarRestauracao() {
        bloqueada=true;
        try {
            if (!lock.writeLock().tryLock(60,TimeUnit.SECONDS))
                throw new ConflitoException("Existem operações em andamento. Aguarde e tente restaurar novamente.");
            return new Permissao(() -> lock.writeLock().unlock());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ConflitoException("A espera pela manutenção foi interrompida.");
        }
    }
    public static final class Permissao implements AutoCloseable {
        private final Runnable fechar;
        private boolean fechada;
        private Permissao(Runnable fechar) { this.fechar=fechar; }
        @Override public void close() { if (!fechada) { fechada=true; fechar.run(); } }
    }
}
