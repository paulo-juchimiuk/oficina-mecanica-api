package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class RegistrarEntradaEstoqueUseCase {

    private final PecaRepository pecas;

    public RegistrarEntradaEstoqueUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    @Transactional
    public Peca executar(UUID id, int quantidade) {
        Peca peca = pecas.buscarAtivaParaMovimentacao(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
        peca.registrarEntrada(quantidade);
        return pecas.salvar(peca);
    }
}
