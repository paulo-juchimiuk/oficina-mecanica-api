package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ItemPeca;
import java.util.List;
import java.util.UUID;

public interface ReservaDePecas {

    void reservar(UUID ordemServicoId, List<ItemPeca> itens);
}
