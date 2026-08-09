package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Dinheiro;
import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AlterarServicoUseCase {

    private final ServicoRepository servicos;

    public AlterarServicoUseCase(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    @Transactional
    public Servico executar(UUID id, String nome, String descricao, Dinheiro valorMaoDeObra) {
        Servico servico = servicos.buscarAtivoPorId(id).orElseThrow(() -> new ServicoNaoEncontradoException(id));
        servico.alterar(nome, descricao, valorMaoDeObra);
        return servicos.salvar(servico);
    }
}
