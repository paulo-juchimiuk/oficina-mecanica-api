package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.VeiculoDeOutroClienteException;
import br.com.oficinamecanica.ordemservico.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Veiculos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CriarOrdemServicoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final Clientes clientes;
    private final Veiculos veiculos;

    public CriarOrdemServicoUseCase(OrdemServicoRepository ordensServico, Clientes clientes, Veiculos veiculos) {
        this.ordensServico = ordensServico;
        this.clientes = clientes;
        this.veiculos = veiculos;
    }

    @Transactional
    public OrdemServico executar(String documentoCliente, UUID veiculoId, String relatoDoProblema) {
        UUID clienteId = clientes.identidadePorDocumento(documentoCliente)
                .orElseThrow(ClienteNaoEncontradoException::new);
        UUID proprietario = veiculos.proprietarioDe(veiculoId)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(veiculoId));
        if (!proprietario.equals(clienteId)) {
            throw new VeiculoDeOutroClienteException(veiculoId);
        }
        return ordensServico.salvar(OrdemServico.abrir(clienteId, veiculoId, relatoDoProblema));
    }
}
