import type { ReactNode } from "react";
import { useEffect } from "react";
import { Box } from "@chakra-ui/react";

export default function ModalShell({ children, onClose }: { children: ReactNode; onClose: () => void }) {
  useEffect(() => {
    const tecla = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", tecla);
    return () => window.removeEventListener("keydown", tecla);
  }, [onClose]);
  return <Box position="fixed" inset="0" bg="blackAlpha.600" zIndex="modal" display="flex"
    alignItems="center" justifyContent="center" p="4" onClick={onClose}>
    <Box role="dialog" aria-modal="true" aria-label="Evento" w="100%" maxW="760px" maxH="calc(100vh - 32px)"
      overflowY="auto" onClick={e => e.stopPropagation()}>{children}</Box>
  </Box>;
}
