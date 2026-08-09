package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ListarServicosUseCase {

    private final ServicoRepository servicos;

    public ListarServicosUseCase(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    @Transactional(readOnly = true)
    public List<Servico> executar() {
        return servicos.listarAtivos();
    }
}
