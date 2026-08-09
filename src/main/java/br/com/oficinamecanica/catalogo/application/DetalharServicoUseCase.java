package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DetalharServicoUseCase {

    private final ServicoRepository servicos;

    public DetalharServicoUseCase(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    @Transactional(readOnly = true)
    public Servico executar(UUID id) {
        return servicos.buscarAtivoPorId(id).orElseThrow(() -> new ServicoNaoEncontradoException(id));
    }
}
