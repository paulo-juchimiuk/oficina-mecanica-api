package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.estoque.application.ItemAReservar;
import br.com.oficinamecanica.estoque.application.ReservarPecasUseCase;
import br.com.oficinamecanica.ordemservico.application.ReservaDePecas;
import br.com.oficinamecanica.ordemservico.domain.ItemPeca;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
class ReservaDePecasNoEstoque implements ReservaDePecas {

    private final ReservarPecasUseCase reservarPecas;

    ReservaDePecasNoEstoque(ReservarPecasUseCase reservarPecas) {
        this.reservarPecas = reservarPecas;
    }

    @Override
    public void reservar(UUID ordemServicoId, List<ItemPeca> itens) {
        if (itens.isEmpty()) {
            return;
        }
        reservarPecas.executar(ordemServicoId, itens.stream()
                .map(item -> new ItemAReservar(item.pecaId(), item.quantidade()))
                .toList());
    }
}
