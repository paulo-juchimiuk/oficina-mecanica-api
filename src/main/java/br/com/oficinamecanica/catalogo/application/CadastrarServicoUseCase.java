package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Dinheiro;
import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarServicoUseCase {

    private final ServicoRepository servicos;

    public CadastrarServicoUseCase(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    @Transactional
    public Servico executar(String nome, String descricao, Dinheiro valorMaoDeObra) {
        return servicos.salvar(Servico.cadastrar(nome, descricao, valorMaoDeObra));
    }
}
