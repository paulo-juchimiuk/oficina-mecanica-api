package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.cadastro.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
class OrdensServicoEmAndamentoDoCadastro implements OrdensServicoEmAndamento {

    private final OrdemServicoJpaRepository repository;

    OrdensServicoEmAndamentoDoCadastro(OrdemServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existemParaCliente(UUID clienteId) {
        return repository.existsByClienteIdAndStatusIn(clienteId, StatusOrdemServico.emAndamento());
    }

    @Override
    public boolean existemParaVeiculo(UUID veiculoId) {
        return repository.existsByVeiculoIdAndStatusIn(veiculoId, StatusOrdemServico.emAndamento());
    }
}
