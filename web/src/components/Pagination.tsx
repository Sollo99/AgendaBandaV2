import { Button, HStack, Text } from "@chakra-ui/react";

export default function Pagination({ page, totalPages, totalElements, carregando, onChange }: {
  page: number; totalPages: number; totalElements: number; carregando: boolean; onChange: (page: number) => void;
}) {
  return <HStack wrap="wrap" justify="space-between">
    <Text color="fg.muted">{totalElements} registros. Página {Math.min(page + 1, Math.max(1, totalPages))} de {Math.max(1, totalPages)}.</Text>
    <HStack>
      <Button size="sm" variant="outline" disabled={carregando || page <= 0} onClick={() => onChange(page - 1)}>Página anterior</Button>
      <Button size="sm" variant="outline" disabled={carregando || page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Próxima página</Button>
    </HStack>
  </HStack>;
}
