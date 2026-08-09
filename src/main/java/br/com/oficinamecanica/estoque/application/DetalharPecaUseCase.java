package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DetalharPecaUseCase {

    private final PecaRepository pecas;

    public DetalharPecaUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    @Transactional(readOnly = true)
    public Peca executar(UUID id) {
        return pecas.buscarAtivaPorId(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
    }
}
