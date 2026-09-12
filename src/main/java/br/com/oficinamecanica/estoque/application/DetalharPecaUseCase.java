package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import java.util.UUID;

public class DetalharPecaUseCase {

    private final PecaRepository pecas;

    public DetalharPecaUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    public Peca executar(UUID id) {
        return pecas.buscarAtivaPorId(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
    }
}
