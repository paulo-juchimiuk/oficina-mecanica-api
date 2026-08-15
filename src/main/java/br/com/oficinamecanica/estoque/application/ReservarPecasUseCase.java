package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.ResultadoDaReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import static java.util.Comparator.comparing;

@Service
public class ReservarPecasUseCase {

    private final PecaRepository pecas;

    public ReservarPecasUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    @Transactional
    public List<ResultadoDaReserva> executar(UUID ordemServicoId, List<ItemAReservar> itens) {
        return itens.stream()
                .sorted(comparing(ItemAReservar::pecaId))
                .map(item -> reservar(ordemServicoId, item))
                .toList();
    }

    private ResultadoDaReserva reservar(UUID ordemServicoId, ItemAReservar item) {
        Peca peca = pecas.buscarAtivaComTrava(item.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(item.pecaId()));
        ResultadoDaReserva resultado = peca.reservar(ordemServicoId, item.quantidade());
        pecas.salvar(peca);
        resultado.reserva().ifPresent(pecas::salvarReserva);
        resultado.pendencia().ifPresent(pecas::salvarPendencia);
        return resultado;
    }
}
