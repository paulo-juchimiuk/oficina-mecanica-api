package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Dinheiro;
import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarPecaUseCase {

    private final PecaRepository pecas;

    public CadastrarPecaUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    @Transactional
    public Peca executar(String nome, String unidadeMedida, Dinheiro preco, int estoqueMinimo) {
        return pecas.salvar(Peca.cadastrar(nome, unidadeMedida, preco, estoqueMinimo));
    }
}
