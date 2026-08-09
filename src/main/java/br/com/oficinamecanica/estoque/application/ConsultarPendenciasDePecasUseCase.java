package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ConsultarPendenciasDePecasUseCase {

    private final PecaRepository pecas;

    public ConsultarPendenciasDePecasUseCase(PecaRepository pecas) {
        this.pecas = pecas;
    }

    @Transactional(readOnly = true)
    public List<PendenciaPeca> executar(UUID ordemServicoId) {
        if (ordemServicoId == null) {
            return pecas.listarPendencias();
        }
        return pecas.listarPendenciasDaOrdemServico(ordemServicoId);
    }
}
