package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.OrdemServicoForaDeExecucaoException;
import br.com.oficinamecanica.estoque.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class RegistrarFaltaDePecaUseCase {

    private final PecaRepository pecas;
    private final OrdensServico ordensServico;

    public RegistrarFaltaDePecaUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        this.pecas = pecas;
        this.ordensServico = ordensServico;
    }

    @Transactional
    public PendenciaPeca executar(UUID ordemServicoId, UUID pecaId, int quantidadeFaltante) {
        if (!ordensServico.existe(ordemServicoId)) {
            throw new OrdemServicoNaoEncontradaException(ordemServicoId);
        }
        if (!ordensServico.estaEmExecucao(ordemServicoId)) {
            throw new OrdemServicoForaDeExecucaoException(ordemServicoId);
        }
        Peca peca = pecas.buscarAtivaComTrava(pecaId).orElseThrow(() -> new PecaNaoEncontradaException(pecaId));
        return pecas.salvarPendencia(peca.registrarFalta(ordemServicoId, quantidadeFaltante));
    }
}
