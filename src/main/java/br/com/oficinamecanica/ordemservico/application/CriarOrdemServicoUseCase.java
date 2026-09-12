package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.VeiculoDeOutroClienteException;
import br.com.oficinamecanica.ordemservico.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Veiculos;
import java.util.List;
import java.util.UUID;

public class CriarOrdemServicoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final Clientes clientes;
    private final Veiculos veiculos;
    private final PrecificadorDeItens precificador;
    private final NotificadorDoCliente notificador;

    public CriarOrdemServicoUseCase(OrdemServicoRepository ordensServico, Clientes clientes, Veiculos veiculos,
                                    PrecificadorDeItens precificador, NotificadorDoCliente notificador) {
        this.ordensServico = ordensServico;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.precificador = precificador;
        this.notificador = notificador;
    }

    public OrdemServico executar(String documentoCliente, UUID veiculoId, String relatoDoProblema,
                                 List<ItemDeServicoRequisitado> itensServico, List<ItemDePecaRequisitado> itensPeca) {
        UUID clienteId = clientes.identidadePorDocumento(documentoCliente)
                .orElseThrow(ClienteNaoEncontradoException::new);
        UUID proprietario = veiculos.proprietarioDe(veiculoId)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(veiculoId));
        if (!proprietario.equals(clienteId)) {
            throw new VeiculoDeOutroClienteException(veiculoId);
        }
        OrdemServico ordemServico = OrdemServico.abrir(clienteId, veiculoId, relatoDoProblema);
        incluirPedidoInicial(ordemServico, itensServico, itensPeca);
        OrdemServico gravada = ordensServico.salvar(ordemServico);
        notificador.notificarMudancaDeStatus(gravada);
        return gravada;
    }

    private void incluirPedidoInicial(OrdemServico ordemServico, List<ItemDeServicoRequisitado> itensServico,
                                      List<ItemDePecaRequisitado> itensPeca) {
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            return;
        }
        ordemServico.incluirItens(
                precificador.precificarServicos(itensServico),
                precificador.precificarPecas(itensPeca));
    }
}
