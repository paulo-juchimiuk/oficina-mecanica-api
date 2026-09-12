package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import java.util.List;

public class ListarServicosUseCase {

    private final ServicoRepository servicos;

    public ListarServicosUseCase(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    public List<Servico> executar() {
        return servicos.listarAtivos();
    }
}
