package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.UUID;

public class ItemServico {

    private static final String CODIGO = "ITEM_SERVICO_INVALIDO";

    private final UUID id;
    private final UUID servicoId;
    private final Dinheiro valorMaoDeObraSnapshot;
    private final int versaoOrigem;

    public ItemServico(UUID id, UUID servicoId, Dinheiro valorMaoDeObraSnapshot, int versaoOrigem) {
        if (id == null || servicoId == null) {
            throw new DadosInvalidosException(CODIGO, "Item de servico exige identidade e Servico");
        }
        if (valorMaoDeObraSnapshot == null) {
            throw new DadosInvalidosException(CODIGO, "Item de servico exige o Valor de mao de obra copiado");
        }
        if (versaoOrigem < 1) {
            throw new DadosInvalidosException(CODIGO, "Item de servico exige a Versao do orcamento que o introduziu");
        }
        this.id = id;
        this.servicoId = servicoId;
        this.valorMaoDeObraSnapshot = valorMaoDeObraSnapshot;
        this.versaoOrigem = versaoOrigem;
    }

    static ItemServico incluir(UUID servicoId, Dinheiro valorMaoDeObra, int versaoOrigem) {
        return new ItemServico(UUID.randomUUID(), servicoId, valorMaoDeObra, versaoOrigem);
    }

    Dinheiro subtotal() {
        return valorMaoDeObraSnapshot;
    }

    public UUID id() {
        return id;
    }

    public UUID servicoId() {
        return servicoId;
    }

    public Dinheiro valorMaoDeObraSnapshot() {
        return valorMaoDeObraSnapshot;
    }

    public int versaoOrigem() {
        return versaoOrigem;
    }
}
