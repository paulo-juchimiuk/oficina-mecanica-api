package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Dinheiro;
import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;

public class CadastrarServicoUseCase {

    private final ServicoRepository servicos;

    public CadastrarServicoUseCase(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    public Servico executar(String nome, String descricao, Dinheiro valorMaoDeObra) {
        return servicos.salvar(Servico.cadastrar(nome, descricao, valorMaoDeObra));
    }
}
