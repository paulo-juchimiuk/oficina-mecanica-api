package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import br.com.oficinamecanica.ordemservico.domain.VeiculoDeOutroClienteException;
import br.com.oficinamecanica.ordemservico.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Veiculos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Casos de uso da Ordem de Servico")
class OrdemServicoUseCasesTest {

    private static final String DOCUMENTO = "10433218100";
    private static final String RELATO = "Barulho ao frear";

    private OrdemServicoRepository ordensServico;
    private Clientes clientes;
    private Veiculos veiculos;
    private CriarOrdemServicoUseCase criarOrdemServico;
    private IniciarDiagnosticoUseCase iniciarDiagnostico;

    private final UUID clienteId = UUID.randomUUID();
    private final UUID veiculoId = UUID.randomUUID();

    @BeforeEach
    void prepararDublesDeTeste() {
        ordensServico = mock(OrdemServicoRepository.class);
        clientes = mock(Clientes.class);
        veiculos = mock(Veiculos.class);
        criarOrdemServico = new CriarOrdemServicoUseCase(ordensServico, clientes, veiculos);
        iniciarDiagnostico = new IniciarDiagnosticoUseCase(ordensServico);
        when(ordensServico.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    @DisplayName("deve abrir a OS quando o Documento identifica o Cliente e o Veiculo e dele")
    void deveAbrirAOrdemServico() {
        when(clientes.identidadePorDocumento(DOCUMENTO)).thenReturn(Optional.of(clienteId));
        when(veiculos.proprietarioDe(veiculoId)).thenReturn(Optional.of(clienteId));

        OrdemServico ordem = criarOrdemServico.executar(DOCUMENTO, veiculoId, RELATO);

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(ordem.clienteId()).isEqualTo(clienteId);
        assertThat(ordem.veiculoId()).isEqualTo(veiculoId);
        verify(ordensServico).salvar(ordem);
    }

    @Test
    @DisplayName("deve recusar a abertura quando nenhum Cliente ativo tem o Documento informado")
    void deveRecusarAberturaSemCliente() {
        when(clientes.identidadePorDocumento(DOCUMENTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criarOrdemServico.executar(DOCUMENTO, veiculoId, RELATO))
                .isInstanceOf(ClienteNaoEncontradoException.class);
        verify(ordensServico, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar a abertura quando o Veiculo nao existe")
    void deveRecusarAberturaSemVeiculo() {
        when(clientes.identidadePorDocumento(DOCUMENTO)).thenReturn(Optional.of(clienteId));
        when(veiculos.proprietarioDe(veiculoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criarOrdemServico.executar(DOCUMENTO, veiculoId, RELATO))
                .isInstanceOf(VeiculoNaoEncontradoException.class);
        verify(ordensServico, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar a abertura quando o Veiculo pertence a outro Cliente")
    void deveRecusarAberturaComVeiculoDeOutroCliente() {
        when(clientes.identidadePorDocumento(DOCUMENTO)).thenReturn(Optional.of(clienteId));
        when(veiculos.proprietarioDe(veiculoId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> criarOrdemServico.executar(DOCUMENTO, veiculoId, RELATO))
                .isInstanceOf(VeiculoDeOutroClienteException.class);
        verify(ordensServico, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar iniciar o diagnostico de uma Ordem de Servico inexistente")
    void deveRecusarDiagnosticoDeOrdemInexistente() {
        UUID inexistente = UUID.randomUUID();
        when(ordensServico.buscarPorId(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> iniciarDiagnostico.executar(inexistente))
                .isInstanceOf(OrdemServicoNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve levar a OS a Em diagnostico e gravar o agregado")
    void deveIniciarDiagnosticoEGravar() {
        OrdemServico ordem = OrdemServico.abrir(clienteId, veiculoId, RELATO);
        when(ordensServico.buscarPorId(ordem.id())).thenReturn(Optional.of(ordem));

        OrdemServico atualizada = iniciarDiagnostico.executar(ordem.id());

        assertThat(atualizada.status()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        verify(ordensServico).salvar(ordem);
    }
}
