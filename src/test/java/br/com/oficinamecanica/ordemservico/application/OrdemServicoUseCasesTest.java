package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.Pecas;
import br.com.oficinamecanica.ordemservico.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Servicos;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import br.com.oficinamecanica.ordemservico.domain.VeiculoDeOutroClienteException;
import br.com.oficinamecanica.ordemservico.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Veiculos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    private Servicos servicos;
    private Pecas pecas;
    private CriarOrdemServicoUseCase criarOrdemServico;
    private IniciarDiagnosticoUseCase iniciarDiagnostico;

    private final UUID clienteId = UUID.randomUUID();
    private final UUID veiculoId = UUID.randomUUID();

    @BeforeEach
    void prepararDublesDeTeste() {
        ordensServico = mock(OrdemServicoRepository.class);
        clientes = mock(Clientes.class);
        veiculos = mock(Veiculos.class);
        servicos = mock(Servicos.class);
        pecas = mock(Pecas.class);
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
        when(ordensServico.buscarParaMovimentacao(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> iniciarDiagnostico.executar(inexistente))
                .isInstanceOf(OrdemServicoNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve levar a OS a Em diagnostico e gravar o agregado")
    void deveIniciarDiagnosticoEGravar() {
        OrdemServico ordem = OrdemServico.abrir(clienteId, veiculoId, RELATO);
        when(ordensServico.buscarParaMovimentacao(ordem.id())).thenReturn(Optional.of(ordem));

        OrdemServico atualizada = iniciarDiagnostico.executar(ordem.id());

        assertThat(atualizada.status()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        verify(ordensServico).salvar(ordem);
    }

    private Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor), "BRL");
    }

    private OrdemServico ordemEmDiagnostico() {
        OrdemServico ordem = OrdemServico.abrir(clienteId, veiculoId, RELATO);
        ordem.iniciarDiagnostico();
        when(ordensServico.buscarParaMovimentacao(ordem.id())).thenReturn(Optional.of(ordem));
        return ordem;
    }

    @Test
    @DisplayName("deve copiar do catalogo o Valor de mao de obra e o preco no momento da inclusao")
    void deveCopiarOsValoresDoCatalogoNaInclusao() {
        IncluirItensUseCase incluirItens = new IncluirItensUseCase(ordensServico, servicos, pecas);
        OrdemServico ordem = ordemEmDiagnostico();
        UUID servicoId = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        when(servicos.valorMaoDeObraDe(servicoId)).thenReturn(Optional.of(reais("120.00")));
        when(pecas.precoDe(pecaId)).thenReturn(Optional.of(reais("50.00")));

        OrdemServico atualizada = incluirItens.executar(ordem.id(),
                List.of(new ItemDeServicoRequisitado(servicoId)),
                List.of(new ItemDePecaRequisitado(pecaId, 2)));

        assertThat(atualizada.itensServico()).singleElement()
                .satisfies(item -> assertThat(item.valorMaoDeObraSnapshot()).isEqualTo(reais("120.00")));
        assertThat(atualizada.versaoMaisRecente().orElseThrow().total()).isEqualTo(reais("220.00"));
        verify(ordensServico).salvar(ordem);
    }

    @Test
    @DisplayName("deve recusar a inclusao quando o Servico nao esta ativo no catalogo")
    void deveRecusarInclusaoComServicoInexistente() {
        IncluirItensUseCase incluirItens = new IncluirItensUseCase(ordensServico, servicos, pecas);
        OrdemServico ordem = ordemEmDiagnostico();
        UUID servicoId = UUID.randomUUID();
        when(servicos.valorMaoDeObraDe(servicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incluirItens.executar(ordem.id(),
                List.of(new ItemDeServicoRequisitado(servicoId)), List.of()))
                .isInstanceOf(ServicoNaoEncontradoException.class);
        verify(ordensServico, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar a inclusao quando a Peca nao esta ativa no catalogo")
    void deveRecusarInclusaoComPecaInexistente() {
        IncluirItensUseCase incluirItens = new IncluirItensUseCase(ordensServico, servicos, pecas);
        OrdemServico ordem = ordemEmDiagnostico();
        UUID pecaId = UUID.randomUUID();
        when(pecas.precoDe(pecaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incluirItens.executar(ordem.id(),
                List.of(), List.of(new ItemDePecaRequisitado(pecaId, 1))))
                .isInstanceOf(PecaNaoEncontradaException.class);
        verify(ordensServico, never()).salvar(any());
    }

    @Test
    @DisplayName("deve enviar o Orcamento ao e-mail do Cliente ao concluir o diagnostico")
    void deveEnviarOOrcamentoAoConcluirODiagnostico() {
        EnvioDeOrcamento envio = mock(EnvioDeOrcamento.class);
        ConcluirDiagnosticoUseCase concluirDiagnostico =
                new ConcluirDiagnosticoUseCase(ordensServico, clientes, envio);
        IncluirItensUseCase incluirItens = new IncluirItensUseCase(ordensServico, servicos, pecas);
        OrdemServico ordem = ordemEmDiagnostico();
        UUID servicoId = UUID.randomUUID();
        when(servicos.valorMaoDeObraDe(servicoId)).thenReturn(Optional.of(reais("120.00")));
        when(clientes.emailDe(clienteId)).thenReturn(Optional.of("ana@example.com"));
        incluirItens.executar(ordem.id(), List.of(new ItemDeServicoRequisitado(servicoId)), List.of());

        OrdemServico atualizada = concluirDiagnostico.executar(ordem.id());

        assertThat(atualizada.status()).isEqualTo(StatusOrdemServico.AGUARDANDO_APROVACAO);
        verify(envio).enviar(eq("ana@example.com"), eq(ordem.codigoAcompanhamento()),
                argThat(versao -> versao.versao() == 1 && versao.enviado()));
    }

    @Test
    @DisplayName("deve recusar concluir o diagnostico de uma Ordem de Servico inexistente")
    void deveRecusarConclusaoDeOrdemInexistente() {
        ConcluirDiagnosticoUseCase concluirDiagnostico =
                new ConcluirDiagnosticoUseCase(ordensServico, clientes, mock(EnvioDeOrcamento.class));
        UUID inexistente = UUID.randomUUID();
        when(ordensServico.buscarParaMovimentacao(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> concluirDiagnostico.executar(inexistente))
                .isInstanceOf(OrdemServicoNaoEncontradaException.class);
    }
}
