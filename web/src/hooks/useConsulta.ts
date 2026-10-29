import { useCallback, useEffect, useRef, useState } from "react";
import type { DependencyList } from "react";

/** Uma consulta ativa por componente; respostas antigas nao substituem filtros mais recentes. */
export function useConsulta<T>(consulta: (signal: AbortSignal) => Promise<T>, dependencias: DependencyList = []) {
  const consultaAtual = useRef(consulta);
  consultaAtual.current = consulta;
  const controle = useRef<AbortController | null>(null);
  const montado = useRef(false);
  const [dados, setDados] = useState<T | null>(null);
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);

  const recarregar = useCallback(async () => {
    if (!montado.current) return;
    controle.current?.abort();
    const atual = new AbortController();
    controle.current = atual;
    setCarregando(true);
    setErro("");
    try {
      const resultado = await consultaAtual.current(atual.signal);
      if (!atual.signal.aborted && montado.current) setDados(resultado);
    } catch (e) {
      if (!atual.signal.aborted && montado.current) setErro(e instanceof Error ? e.message : "Não foi possível carregar os dados.");
    } finally {
      if (!atual.signal.aborted && montado.current) setCarregando(false);
    }
  }, []);

  useEffect(() => {
    montado.current = true;
    setDados(null);
    void recarregar();
    return () => { montado.current = false; controle.current?.abort(); };
  }, [recarregar, ...dependencias]);

  return { dados, erro, carregando, recarregar };
}
