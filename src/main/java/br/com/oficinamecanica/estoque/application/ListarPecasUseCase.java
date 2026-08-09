package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ListarPecasUseCase {

    private final PecaRepository pecas;

    public ListarPecasUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    @Transactional(readOnly = true)
    public List<Peca> executar(boolean abaixoDoEstoqueMinimo) {
        if (abaixoDoEstoqueMinimo) {
            return pecas.listarAtivasAbaixoDoEstoqueMinimo();
        }
        return pecas.listarAtivas();
    }
}
