package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Dinheiro;
import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import java.util.UUID;

public class AlterarPecaUseCase {

    private final PecaRepository pecas;

    public AlterarPecaUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    public Peca executar(UUID id, String nome, String unidadeMedida, Dinheiro preco, int estoqueMinimo) {
        Peca peca = pecas.buscarAtivaComTrava(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
        peca.alterar(nome, unidadeMedida, preco, estoqueMinimo);
        return pecas.salvar(peca);
    }
}
