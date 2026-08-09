package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.catalogo.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class InativarServicoUseCase {

    private final ServicoRepository servicos;
    private final OrdensServicoEmAndamento ordensServico;

    public InativarServicoUseCase(ServicoRepository servicos, OrdensServicoEmAndamento ordensServico) {
        this.servicos = servicos;
        this.ordensServico = ordensServico;
    }

    @Transactional
    public void executar(UUID id) {
        Servico servico = servicos.buscarAtivoPorId(id).orElseThrow(() -> new ServicoNaoEncontradoException(id));
        if (ordensServico.existemParaServico(id)) {
            throw new ServicoComOrdemServicoEmAndamentoException();
        }
        servico.inativar();
        servicos.salvar(servico);
    }
}
