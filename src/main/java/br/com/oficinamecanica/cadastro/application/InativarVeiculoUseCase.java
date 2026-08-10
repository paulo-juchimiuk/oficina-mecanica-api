package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.cadastro.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class InativarVeiculoUseCase {

    private final VeiculoRepository veiculos;
    private final OrdensServicoEmAndamento ordensServico;

    public InativarVeiculoUseCase(VeiculoRepository veiculos, OrdensServicoEmAndamento ordensServico) {
        this.veiculos = veiculos;
        this.ordensServico = ordensServico;
    }

    @Transactional
    public void executar(UUID id) {
        Veiculo veiculo = veiculos.buscarAtivoParaInativacao(id).orElseThrow(() -> new VeiculoNaoEncontradoException(id));
        if (ordensServico.existemParaVeiculo(id)) {
            throw new VeiculoComOrdemServicoEmAndamentoException();
        }
        veiculo.inativar();
        veiculos.salvar(veiculo);
    }
}
