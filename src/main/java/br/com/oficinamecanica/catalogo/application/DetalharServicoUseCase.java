package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import java.util.UUID;

public class DetalharServicoUseCase {

    private final ServicoRepository servicos;

    public DetalharServicoUseCase(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    public Servico executar(UUID id) {
        return servicos.buscarAtivoPorId(id).orElseThrow(() -> new ServicoNaoEncontradoException(id));
    }
}
