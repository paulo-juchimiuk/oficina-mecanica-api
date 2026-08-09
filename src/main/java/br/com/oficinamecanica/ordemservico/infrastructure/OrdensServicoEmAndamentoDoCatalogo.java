package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.catalogo.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
class OrdensServicoEmAndamentoDoCatalogo implements OrdensServicoEmAndamento {

    private final OrdemServicoJpaRepository repository;

    OrdensServicoEmAndamentoDoCatalogo(OrdemServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existemParaServico(UUID servicoId) {
        return repository.existeComServico(servicoId, StatusOrdemServico.emAndamento());
    }
}
