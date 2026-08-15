package br.com.oficinamecanica.catalogo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository {

    Servico salvar(Servico servico);

    Optional<Servico> buscarAtivoPorId(UUID id);

    Optional<Servico> buscarAtivoComTrava(UUID id);

    List<Servico> listarAtivos();
}
