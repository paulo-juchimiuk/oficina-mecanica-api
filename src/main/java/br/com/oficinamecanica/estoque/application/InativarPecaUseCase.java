package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.estoque.domain.PecaComReservaAtivaException;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class InativarPecaUseCase {

    private final PecaRepository pecas;
    private final OrdensServicoEmAndamento ordensServicoEmAndamento;

    public InativarPecaUseCase(PecaRepository pecas, OrdensServicoEmAndamento ordensServicoEmAndamento) {
        this.pecas = pecas;
        this.ordensServicoEmAndamento = ordensServicoEmAndamento;
    }

    @Transactional
    public void executar(UUID id) {
        Peca peca = pecas.buscarAtivaComTrava(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
        if (ordensServicoEmAndamento.existemParaPeca(id)) {
            throw new PecaComOrdemServicoEmAndamentoException();
        }
        if (pecas.temReservaAtiva(id)) {
            throw new PecaComReservaAtivaException();
        }
        peca.inativar();
        pecas.salvar(peca);
    }
}
