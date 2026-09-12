package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import java.util.List;
import java.util.UUID;

public class ConsultarPendenciasDePecasUseCase {

    private final PecaRepository pecas;

    public ConsultarPendenciasDePecasUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    public List<PendenciaPeca> executar(UUID ordemServicoId) {
        if (ordemServicoId == null) {
            return pecas.listarPendencias();
        }
        return pecas.listarPendenciasDaOrdemServico(ordemServicoId);
    }
}
