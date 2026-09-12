package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import java.util.List;

public class ListarPecasUseCase {

    private final PecaRepository pecas;

    public ListarPecasUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    public List<Peca> executar(boolean abaixoDoEstoqueMinimo) {
        if (abaixoDoEstoqueMinimo) {
            return pecas.listarAtivasAbaixoDoEstoqueMinimo();
        }
        return pecas.listarAtivas();
    }
}
