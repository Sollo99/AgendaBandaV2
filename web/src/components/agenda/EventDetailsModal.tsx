import { Badge, Box, Button, Flex, Heading, HStack, Stack, Text } from "@chakra-ui/react";
import type { Agenda, Membro } from "../../types";
import { formatarDataHora } from "../../datas";
import ModalShell from "./ModalShell";
import { tiposEvento } from "./tiposEvento";
import ParticipationList from "./ParticipationList";

export default function EventDetailsModal({ evento, usuario, onClose, onEdit, onDelete, onAtualizar }: {
  evento: Agenda; usuario: Membro; onClose: () => void; onEdit: () => void; onDelete: () => void; onAtualizar: (evento: Agenda) => void;
}) {
  return (
        <ModalShell onClose={onClose}>
          <Box bg="bg.panel" borderWidth="1px" borderRadius="lg" boxShadow="xl" p="5">
            <Stack gap="4">
              <Flex justify="space-between" align="flex-start" gap="4">
                <Box>
                  <Badge style={{ background: (tiposEvento[evento.tipoEspecifico] ?? tiposEvento.EVENTO).cor, color: "white" }}>
                    {tiposEvento[evento.tipoEspecifico]?.nome ?? evento.tipoEvento.nome}
                  </Badge>
                  <Heading size="md" mt="3">{evento.titulo}</Heading>
                  <Text color="fg.muted">{formatarDataHora(evento.dataHoraInicio)} até {formatarDataHora(evento.dataHoraFim)}</Text>
                </Box>
                <Button size="sm" variant="ghost" onClick={onClose}>Fechar</Button>
              </Flex>

              <Stack gap="2">
                <Text>{evento.descricao || "Sem descrição."}</Text>
                <Text><b>Local:</b> {evento.local.nome}, {evento.local.endereco} - {evento.local.cidadeNome}/{evento.local.uf}</Text>
                <Text><b>Criado por:</b> {evento.criador.nome}</Text>
                {evento.repertorio && <Text><b>Repertório:</b> {evento.repertorio}</Text>}
                {evento.observacao && <Text><b>Observação:</b> {evento.observacao}</Text>}
                {evento.pauta && <Text><b>Pauta:</b> {evento.pauta}</Text>}
                {evento.cidadeShow && <Text><b>Cidade do show:</b> {evento.cidadeShow.nome}/{evento.cidadeShow.uf}</Text>}
                <Text><b>Status:</b> {evento.status.charAt(0) + evento.status.slice(1).toLowerCase()}</Text>
                <ParticipationList evento={evento} usuario={usuario} onAtualizar={onAtualizar} />
              </Stack>

              {(usuario.administrador || evento.criador.codigo === usuario.codigo) && (
                <HStack>
                  <Button size="sm" onClick={onEdit}>Editar</Button>
                  {evento.status !== "REALIZADO" && !evento.participacoes.some(p => p.presente != null) && <Button size="sm" variant="outline" onClick={onDelete}>Excluir</Button>}
                </HStack>
              )}
            </Stack>
          </Box>
        </ModalShell>
  );
}
