package br.com.oficinamecanica.cadastro.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarAtivoPorId(UUID id);

    Optional<Cliente> buscarAtivoParaInativacao(UUID id);

    Optional<Cliente> buscarAtivoPorDocumento(Documento documento);

    List<Cliente> listarAtivos();

    boolean documentoJaCadastradoPorOutro(UUID id, Documento documento);
}
