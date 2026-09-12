package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import java.util.UUID;

public class RegistrarEntradaEstoqueUseCase {

    private final PecaRepository pecas;

    public RegistrarEntradaEstoqueUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    public Peca executar(UUID id, int quantidade) {
        Peca peca = pecas.buscarAtivaComTrava(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
        peca.registrarEntrada(quantidade);
        return pecas.salvar(peca);
    }
}
