package br.com.oficinamecanica.ordemservico.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrdemServicoRepository {

    OrdemServico salvar(OrdemServico ordemServico);

    Optional<OrdemServico> buscarPorId(UUID id);

    Optional<OrdemServico> buscarPorCodigoAcompanhamento(CodigoAcompanhamento codigoAcompanhamento);

    Optional<OrdemServico> buscarComTrava(UUID id);

    Optional<OrdemServico> buscarComTravaPorCodigo(CodigoAcompanhamento codigoAcompanhamento);

    List<OrdemServico> listarComStatus(StatusOrdemServico status);

    List<OrdemServico> listarExceto(Set<StatusOrdemServico> status);

    List<OrdemServico> listarComExecucaoConcluida(Optional<UUID> servicoId);
}
