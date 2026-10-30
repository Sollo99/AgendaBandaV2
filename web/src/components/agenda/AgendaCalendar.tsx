import { useMemo } from "react";
import { Box, Button, Flex, Grid, Heading, HStack, Stack, Text } from "@chakra-ui/react";
import type { Agenda } from "../../types";
import { diasDoCalendario, deslocarMes, nomeMes, formatarHorario, inputDataHora, instanteSaoPaulo } from "../../datas";
import { tiposEvento } from "./tiposEvento";
const diasSemana = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"];

export default function AgendaCalendar({ eventos, mesAtual, hoje, onMes, onSelecionar }: {
  eventos: Agenda[]; mesAtual: string; hoje: string; onMes: (mes: string) => void; onSelecionar: (evento: Agenda) => void;
}) {
  const diasCalendario = useMemo(() => diasDoCalendario(mesAtual), [mesAtual]);
  const eventosPorDia = useMemo(() => {
    const grupos = new Map<string, Agenda[]>();
    for (const evento of eventos) {
      const chave = inputDataHora(evento.dataHoraInicio).slice(0, 10);
      const lista = grupos.get(chave) ?? [];
      lista.push(evento);
      grupos.set(chave, lista);
    }
    for (const lista of grupos.values()) lista.sort((a, b) => instanteSaoPaulo(a.dataHoraInicio) - instanteSaoPaulo(b.dataHoraInicio));
    return grupos;
  }, [eventos]);
  return (
      <Box bg="bg.panel" borderWidth="1px" borderRadius="lg" overflow="hidden">
        <Flex justify="space-between" align="center" gap="3" p="4" borderBottomWidth="1px" wrap="wrap">
          <HStack>
            <Button size="sm" variant="outline" onClick={() => onMes(deslocarMes(mesAtual, -1))}>Anterior</Button>
            <Button size="sm" variant="outline" onClick={() => onMes(hoje.slice(0, 7))}>Hoje</Button>
            <Button size="sm" variant="outline" onClick={() => onMes(deslocarMes(mesAtual, 1))}>Próximo</Button>
          </HStack>
          <Heading size="md">{nomeMes(mesAtual)}</Heading>
        </Flex>

        <Grid templateColumns="repeat(7, minmax(0, 1fr))" borderBottomWidth="1px">
          {diasSemana.map((dia) => (
            <Box key={dia} px="2" py="2" textAlign="center" fontSize="sm" fontWeight="semibold" color="fg.muted">
              {dia}
            </Box>
          ))}
        </Grid>

        <Grid templateColumns="repeat(7, minmax(0, 1fr))">
          {diasCalendario.map((dia) => {
            const chave = dia;
            const eventosDia = eventosPorDia.get(chave) ?? [];
            const foraDoMes = dia.slice(0, 7) !== mesAtual;
            const diaAtual = chave === hoje;

            return (
              <Box
                key={chave}
                minH={{ base: "104px", md: "132px" }}
                p="2"
                borderRightWidth="1px"
                borderBottomWidth="1px"
                bg={foraDoMes ? "bg.subtle" : "bg.panel"}
                opacity={foraDoMes ? 0.58 : 1}
              >
                <Flex justify="space-between" align="center" mb="2">
                  <Text
                    minW="7"
                    h="7"
                    px="1"
                    display="inline-flex"
                    alignItems="center"
                    justifyContent="center"
                    borderRadius="full"
                    bg={diaAtual ? "blue.500" : "transparent"}
                    color={diaAtual ? "white" : "fg"}
                    fontWeight={diaAtual ? "bold" : "medium"}
                  >
                    {Number(dia.slice(-2))}
                  </Text>
                </Flex>

                <Stack gap="1">
                  {eventosDia.map((evento) => {
                    const tipo = tiposEvento[evento.tipoEspecifico] ?? tiposEvento.EVENTO;
                    return (
                      <Button
                        key={evento.codigoAgenda}
                        size="xs"
                        h="auto"
                        minH="28px"
                        justifyContent="flex-start"
                        variant="ghost"
                        borderLeftWidth="4px"
                        borderLeftColor={tipo.cor}
                        bg="bg.subtle"
                        px="2"
                        py="1"
                        whiteSpace="normal"
                        textAlign="left"
                        lineHeight="1.2"
                        onClick={() => onSelecionar(evento)}
                      >
                        <Box overflow="hidden">
                          <Text fontSize="xs" fontWeight="bold" truncate>{formatarHorario(evento.dataHoraInicio)} {evento.titulo}</Text>
                          <Text fontSize="2xs" color="fg.muted" truncate>{evento.tipoEvento.nome}</Text>
                        </Box>
                      </Button>
                    );
                  })}
                </Stack>
              </Box>
            );
          })}
        </Grid>
      </Box>

  );
}
