package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Dinheiro;
import br.com.oficinamecanica.estoque.domain.OrdemServicoForaDeExecucaoException;
import br.com.oficinamecanica.estoque.domain.OrdemServicoNaoAceitaDevolucaoException;
import br.com.oficinamecanica.estoque.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.estoque.domain.PecaComReservaAtivaException;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import br.com.oficinamecanica.estoque.domain.ReservaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import br.com.oficinamecanica.estoque.domain.SituacaoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Casos de uso do Estoque")
class EstoqueUseCasesTest {

    private static final Dinheiro PRECO = new Dinheiro(new BigDecimal("189.90"), "BRL");

    @Mock
    private PecaRepository pecas;

    @Mock
    private OrdensServico ordensServico;

    @Mock
    private OrdensServicoEmAndamento ordensServicoEmAndamento;

    private Peca pecaComSaldo(int saldo, int reservada) {
        return new Peca(UUID.randomUUID(), "Pastilha de freio dianteira", "unidade", PRECO,
                saldo, reservada, 4, true);
    }

    @Test
    @DisplayName("deve cadastrar peca ativa com saldo zerado")
    void deveCadastrarPeca() {
        when(pecas.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Peca cadastrada = new CadastrarPecaUseCase(pecas).executar("Filtro de oleo", "unidade", PRECO, 10);

        assertThat(cadastrada.nome()).isEqualTo("Filtro de oleo");
        assertThat(cadastrada.saldoEmEstoque()).isZero();
        assertThat(cadastrada.ativo()).isTrue();
    }

    @Test
    @DisplayName("deve alterar a peca preservando a identidade")
    void deveAlterarPeca() {
        Peca existente = pecaComSaldo(24, 0);
        when(pecas.buscarAtivaComTrava(existente.id())).thenReturn(Optional.of(existente));
        when(pecas.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Peca alterada = new AlterarPecaUseCase(pecas)
                .executar(existente.id(), "Filtro de oleo", "unidade", PRECO, 8);

        assertThat(alterada.id()).isEqualTo(existente.id());
        assertThat(alterada.nome()).isEqualTo("Filtro de oleo");
        assertThat(alterada.estoqueMinimo()).isEqualTo(8);
    }

    @Test
    @DisplayName("deve recusar alteracao de peca inexistente ou removida logicamente")
    void deveRecusarAlteracaoDePecaInexistente() {
        UUID id = UUID.randomUUID();
        when(pecas.buscarAtivaComTrava(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AlterarPecaUseCase(pecas).executar(id, "Filtro", "unidade", PRECO, 1))
                .isInstanceOf(PecaNaoEncontradaException.class);
        verify(pecas, never()).salvar(any());
    }

    @Test
    @DisplayName("deve detalhar a peca ativa")
    void deveDetalharPeca() {
        Peca existente = pecaComSaldo(24, 0);
        when(pecas.buscarAtivaPorId(existente.id())).thenReturn(Optional.of(existente));

        assertThat(new DetalharPecaUseCase(pecas).executar(existente.id())).isEqualTo(existente);
    }

    @Test
    @DisplayName("deve recusar detalhamento de peca inexistente ou removida logicamente")
    void deveRecusarDetalhamentoDePecaInexistente() {
        UUID id = UUID.randomUUID();
        when(pecas.buscarAtivaPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DetalharPecaUseCase(pecas).executar(id))
                .isInstanceOf(PecaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve listar as pecas ativas")
    void deveListarAtivas() {
        Peca ativa = pecaComSaldo(24, 0);
        when(pecas.listarAtivas()).thenReturn(List.of(ativa));

        assertThat(new ListarPecasUseCase(pecas).executar(false)).containsExactly(ativa);
    }

    @Test
    @DisplayName("deve listar apenas as pecas abaixo do Estoque minimo quando o filtro e pedido")
    void deveListarAbaixoDoMinimo() {
        Peca faltando = pecaComSaldo(1, 0);
        when(pecas.listarAtivasAbaixoDoEstoqueMinimo()).thenReturn(List.of(faltando));

        assertThat(new ListarPecasUseCase(pecas).executar(true)).containsExactly(faltando);
    }

    @Test
    @DisplayName("deve inativar a peca sem apagar o registro")
    void deveInativarPeca() {
        Peca existente = pecaComSaldo(24, 0);
        when(pecas.buscarAtivaComTrava(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServicoEmAndamento.existemParaPeca(existente.id())).thenReturn(false);
        when(pecas.temReservaAtiva(existente.id())).thenReturn(false);

        new InativarPecaUseCase(pecas, ordensServicoEmAndamento).executar(existente.id());

        ArgumentCaptor<Peca> salva = ArgumentCaptor.forClass(Peca.class);
        verify(pecas).salvar(salva.capture());
        assertThat(salva.getValue().ativo()).isFalse();
    }

    @Test
    @DisplayName("deve recusar inativacao quando a peca consta em Ordem de Servico em andamento")
    void deveRecusarInativacaoComOrdemEmAndamento() {
        Peca existente = pecaComSaldo(24, 0);
        when(pecas.buscarAtivaComTrava(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServicoEmAndamento.existemParaPeca(existente.id())).thenReturn(true);

        assertThatThrownBy(() -> new InativarPecaUseCase(pecas, ordensServicoEmAndamento).executar(existente.id()))
                .isInstanceOf(PecaComOrdemServicoEmAndamentoException.class);
        verify(pecas, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar inativacao quando a peca tem Reserva de peca ATIVA")
    void deveRecusarInativacaoComReservaAtiva() {
        Peca existente = pecaComSaldo(24, 4);
        when(pecas.buscarAtivaComTrava(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServicoEmAndamento.existemParaPeca(existente.id())).thenReturn(false);
        when(pecas.temReservaAtiva(existente.id())).thenReturn(true);

        assertThatThrownBy(() -> new InativarPecaUseCase(pecas, ordensServicoEmAndamento).executar(existente.id()))
                .isInstanceOf(PecaComReservaAtivaException.class);
        verify(pecas, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar inativacao de peca inexistente ou removida logicamente")
    void deveRecusarInativacaoDePecaInexistente() {
        UUID id = UUID.randomUUID();
        when(pecas.buscarAtivaComTrava(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new InativarPecaUseCase(pecas, ordensServicoEmAndamento).executar(id))
                .isInstanceOf(PecaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve somar a Entrada de estoque ao saldo da peca")
    void deveRegistrarEntrada() {
        Peca existente = pecaComSaldo(2, 0);
        when(pecas.buscarAtivaComTrava(existente.id())).thenReturn(Optional.of(existente));
        when(pecas.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Peca comEntrada = new RegistrarEntradaEstoqueUseCase(pecas).executar(existente.id(), 10);

        assertThat(comEntrada.saldoEmEstoque()).isEqualTo(12);
    }

    @Test
    @DisplayName("deve recusar Entrada de estoque em peca inexistente ou removida logicamente")
    void deveRecusarEntradaEmPecaInexistente() {
        UUID id = UUID.randomUUID();
        when(pecas.buscarAtivaComTrava(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegistrarEntradaEstoqueUseCase(pecas).executar(id, 10))
                .isInstanceOf(PecaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve persistir a reserva do disponivel e a pendencia do que faltou")
    void deveReservarEGerarPendencia() {
        Peca existente = pecaComSaldo(1, 0);
        UUID ordemServicoId = UUID.randomUUID();
        when(pecas.buscarAtivaComTrava(existente.id())).thenReturn(Optional.of(existente));

        new ReservarPecasUseCase(pecas).executar(ordemServicoId, List.of(new ItemAReservar(existente.id(), 2)));

        ArgumentCaptor<ReservaPeca> reserva = ArgumentCaptor.forClass(ReservaPeca.class);
        ArgumentCaptor<PendenciaPeca> pendencia = ArgumentCaptor.forClass(PendenciaPeca.class);
        verify(pecas).salvarReserva(reserva.capture());
        verify(pecas).salvarPendencia(pendencia.capture());
        assertThat(reserva.getValue().quantidade()).isEqualTo(1);
        assertThat(reserva.getValue().estaAtiva()).isTrue();
        assertThat(pendencia.getValue().quantidadeFaltante()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve recusar reserva de peca inexistente ou removida logicamente")
    void deveRecusarReservaDePecaInexistente() {
        UUID id = UUID.randomUUID();
        when(pecas.buscarAtivaComTrava(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ReservarPecasUseCase(pecas)
                .executar(UUID.randomUUID(), List.of(new ItemAReservar(id, 1))))
                .isInstanceOf(PecaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve consultar as pecas reservadas da Ordem de Servico")
    void deveConsultarReservadas() {
        UUID ordemServicoId = UUID.randomUUID();
        Peca peca = pecaComSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(ordemServicoId, 2).reserva().orElseThrow();
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(pecas.listarReservasDaOrdemServico(ordemServicoId)).thenReturn(List.of(reserva));

        assertThat(new ConsultarPecasReservadasUseCase(pecas, ordensServico).executar(ordemServicoId))
                .containsExactly(reserva);
    }

    @Test
    @DisplayName("deve recusar consulta de reservas de Ordem de Servico inexistente")
    void deveRecusarConsultaDeOrdemInexistente() {
        UUID ordemServicoId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(false);

        assertThatThrownBy(() -> new ConsultarPecasReservadasUseCase(pecas, ordensServico).executar(ordemServicoId))
                .isInstanceOf(OrdemServicoNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve registrar a Baixa de estoque na retirada da reserva")
    void deveRetirarReserva() {
        UUID ordemServicoId = UUID.randomUUID();
        Peca peca = pecaComSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(ordemServicoId, 4).reserva().orElseThrow();
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.estaEmExecucao(ordemServicoId)).thenReturn(true);
        when(pecas.buscarReservaDaOrdemServico(ordemServicoId, reserva.id())).thenReturn(Optional.of(reserva));
        when(pecas.relerReservaDaOrdemServico(ordemServicoId, reserva.id())).thenReturn(Optional.of(reserva));
        when(pecas.buscarComTrava(peca.id())).thenReturn(Optional.of(peca));
        when(pecas.salvarReserva(any())).thenAnswer(chamada -> chamada.getArgument(0));

        List<ReservaPeca> retiradas = new RetirarPecasReservadasUseCase(pecas, ordensServico)
                .executar(ordemServicoId, List.of(reserva.id()));

        assertThat(retiradas).hasSize(1);
        assertThat(retiradas.getFirst().situacao()).isEqualTo(SituacaoReserva.CONSUMIDA);
        assertThat(peca.saldoEmEstoque()).isEqualTo(20);
    }

    @Test
    @DisplayName("deve recusar retirada em Ordem de Servico inexistente")
    void deveRecusarRetiradaDeOrdemInexistente() {
        UUID ordemServicoId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(false);

        assertThatThrownBy(() -> new RetirarPecasReservadasUseCase(pecas, ordensServico)
                .executar(ordemServicoId, List.of(UUID.randomUUID())))
                .isInstanceOf(OrdemServicoNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve recusar retirada com a Ordem de Servico fora de EM_EXECUCAO")
    void deveRecusarRetiradaForaDeExecucao() {
        UUID ordemServicoId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.estaEmExecucao(ordemServicoId)).thenReturn(false);

        assertThatThrownBy(() -> new RetirarPecasReservadasUseCase(pecas, ordensServico)
                .executar(ordemServicoId, List.of(UUID.randomUUID())))
                .isInstanceOf(OrdemServicoForaDeExecucaoException.class);
    }

    @Test
    @DisplayName("deve recusar retirada de reserva que nao e da Ordem de Servico informada")
    void deveRecusarRetiradaDeReservaDeOutraOrdem() {
        UUID ordemServicoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.estaEmExecucao(ordemServicoId)).thenReturn(true);
        when(pecas.buscarReservaDaOrdemServico(ordemServicoId, reservaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RetirarPecasReservadasUseCase(pecas, ordensServico)
                .executar(ordemServicoId, List.of(reservaId)))
                .isInstanceOf(ReservaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve repor o saldo na devolucao de reserva ja consumida")
    void deveDevolverReservaConsumida() {
        UUID ordemServicoId = UUID.randomUUID();
        Peca peca = pecaComSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(ordemServicoId, 4).reserva().orElseThrow();
        peca.retirar(reserva);
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.aceitaDevolucaoDePecas(ordemServicoId)).thenReturn(true);
        when(pecas.buscarReservaDaOrdemServico(ordemServicoId, reserva.id())).thenReturn(Optional.of(reserva));
        when(pecas.relerReservaDaOrdemServico(ordemServicoId, reserva.id())).thenReturn(Optional.of(reserva));
        when(pecas.buscarComTrava(peca.id())).thenReturn(Optional.of(peca));
        when(pecas.salvarReserva(any())).thenAnswer(chamada -> chamada.getArgument(0));

        List<ReservaPeca> devolvidas = new DevolverPecasNaoUtilizadasUseCase(pecas, ordensServico)
                .executar(ordemServicoId, List.of(reserva.id()));

        assertThat(devolvidas.getFirst().situacao()).isEqualTo(SituacaoReserva.DEVOLVIDA);
        assertThat(peca.saldoEmEstoque()).isEqualTo(24);
    }

    @Test
    @DisplayName("deve recusar devolucao em Ordem de Servico inexistente")
    void deveRecusarDevolucaoDeOrdemInexistente() {
        UUID ordemServicoId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(false);

        assertThatThrownBy(() -> new DevolverPecasNaoUtilizadasUseCase(pecas, ordensServico)
                .executar(ordemServicoId, List.of(UUID.randomUUID())))
                .isInstanceOf(OrdemServicoNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve recusar devolucao com a Ordem de Servico em status que nao a aceita")
    void deveRecusarDevolucaoEmStatusIncompativel() {
        UUID ordemServicoId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.aceitaDevolucaoDePecas(ordemServicoId)).thenReturn(false);

        assertThatThrownBy(() -> new DevolverPecasNaoUtilizadasUseCase(pecas, ordensServico)
                .executar(ordemServicoId, List.of(UUID.randomUUID())))
                .isInstanceOf(OrdemServicoNaoAceitaDevolucaoException.class);
    }

    @Test
    @DisplayName("deve registrar a falta de peca como pendencia aberta")
    void deveRegistrarFalta() {
        UUID ordemServicoId = UUID.randomUUID();
        Peca peca = pecaComSaldo(0, 0);
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.estaEmExecucao(ordemServicoId)).thenReturn(true);
        when(pecas.buscarAtivaComTrava(peca.id())).thenReturn(Optional.of(peca));
        when(pecas.salvarPendencia(any())).thenAnswer(chamada -> chamada.getArgument(0));

        PendenciaPeca pendencia = new RegistrarFaltaDePecaUseCase(pecas, ordensServico)
                .executar(ordemServicoId, peca.id(), 3);

        assertThat(pendencia.quantidadeFaltante()).isEqualTo(3);
        assertThat(pendencia.resolvidaEm()).isNull();
    }

    @Test
    @DisplayName("deve recusar registro de falta com a Ordem de Servico fora de EM_EXECUCAO")
    void deveRecusarFaltaForaDeExecucao() {
        UUID ordemServicoId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.estaEmExecucao(ordemServicoId)).thenReturn(false);

        assertThatThrownBy(() -> new RegistrarFaltaDePecaUseCase(pecas, ordensServico)
                .executar(ordemServicoId, UUID.randomUUID(), 1))
                .isInstanceOf(OrdemServicoForaDeExecucaoException.class);
    }

    @Test
    @DisplayName("deve recusar registro de falta em Ordem de Servico inexistente")
    void deveRecusarFaltaDeOrdemInexistente() {
        UUID ordemServicoId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(false);

        assertThatThrownBy(() -> new RegistrarFaltaDePecaUseCase(pecas, ordensServico)
                .executar(ordemServicoId, UUID.randomUUID(), 1))
                .isInstanceOf(OrdemServicoNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve recusar registro de falta de peca inexistente ou removida logicamente")
    void deveRecusarFaltaDePecaInexistente() {
        UUID ordemServicoId = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        when(ordensServico.existe(ordemServicoId)).thenReturn(true);
        when(ordensServico.estaEmExecucao(ordemServicoId)).thenReturn(true);
        when(pecas.buscarAtivaComTrava(pecaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegistrarFaltaDePecaUseCase(pecas, ordensServico)
                .executar(ordemServicoId, pecaId, 1))
                .isInstanceOf(PecaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve consultar todas as pendencias quando nenhuma Ordem de Servico e informada")
    void deveConsultarTodasAsPendencias() {
        Peca peca = pecaComSaldo(0, 0);
        PendenciaPeca pendencia = peca.registrarFalta(UUID.randomUUID(), 2);
        when(pecas.listarPendencias()).thenReturn(List.of(pendencia));

        assertThat(new ConsultarPendenciasDePecasUseCase(pecas).executar(null)).containsExactly(pendencia);
    }

    @Test
    @DisplayName("deve filtrar as pendencias pela Ordem de Servico informada")
    void deveFiltrarPendenciasPorOrdemServico() {
        UUID ordemServicoId = UUID.randomUUID();
        Peca peca = pecaComSaldo(0, 0);
        PendenciaPeca pendencia = peca.registrarFalta(ordemServicoId, 2);
        when(pecas.listarPendenciasDaOrdemServico(ordemServicoId)).thenReturn(List.of(pendencia));

        assertThat(new ConsultarPendenciasDePecasUseCase(pecas).executar(ordemServicoId))
                .containsExactly(pendencia);
    }
}
