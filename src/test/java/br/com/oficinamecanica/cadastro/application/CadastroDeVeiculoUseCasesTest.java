package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Contato;
import br.com.oficinamecanica.cadastro.domain.Documento;
import br.com.oficinamecanica.cadastro.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.PlacaJaCadastradaException;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.cadastro.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@DisplayName("Casos de uso de Veiculo")
class CadastroDeVeiculoUseCasesTest {

    private static final Placa PLACA = new Placa("ABC1234");

    @Mock
    private VeiculoRepository veiculos;

    @Mock
    private ClienteRepository clientes;

    @Mock
    private OrdensServicoEmAndamento ordensServico;

    private Cliente proprietario() {
        return Cliente.cadastrar("Ana", new Documento("10433218100"), new Contato("cliente@example.com", null));
    }

    @Test
    @DisplayName("deve cadastrar veiculo vinculado a um Cliente ativo")
    void deveCadastrarVeiculoComClienteAtivo() {
        Cliente dono = proprietario();
        when(clientes.buscarAtivoPorId(dono.id())).thenReturn(Optional.of(dono));
        when(veiculos.placaJaCadastradaPorOutro(any(), any())).thenReturn(false);
        when(veiculos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Veiculo cadastrado = new CadastrarVeiculoUseCase(veiculos, clientes)
                .executar(PLACA, "Fiat", "Uno", 2015, dono.id());

        assertThat(cadastrado.clienteId()).isEqualTo(dono.id());
        assertThat(cadastrado.ativo()).isTrue();
    }

    @Test
    @DisplayName("deve recusar cadastro quando o Cliente proprietario nao existe ou esta inativo")
    void deveRecusarCadastroSemClienteAtivo() {
        UUID clienteId = UUID.randomUUID();
        when(clientes.buscarAtivoPorId(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CadastrarVeiculoUseCase(veiculos, clientes)
                .executar(PLACA, "Fiat", "Uno", 2015, clienteId))
                .isInstanceOf(ClienteNaoEncontradoException.class);
        verify(veiculos, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar cadastro quando a Placa pertence a outro veiculo, inclusive inativo")
    void deveRecusarPlacaJaCadastrada() {
        Cliente dono = proprietario();
        when(clientes.buscarAtivoPorId(dono.id())).thenReturn(Optional.of(dono));
        when(veiculos.placaJaCadastradaPorOutro(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> new CadastrarVeiculoUseCase(veiculos, clientes)
                .executar(PLACA, "Fiat", "Uno", 2015, dono.id()))
                .isInstanceOf(PlacaJaCadastradaException.class);
    }

    @Test
    @DisplayName("deve recusar alteracao de veiculo inexistente ou removido logicamente")
    void deveRecusarAlteracaoDeVeiculoInexistente() {
        UUID id = UUID.randomUUID();
        when(veiculos.buscarAtivoParaModificacao(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AlterarVeiculoUseCase(veiculos, clientes)
                .executar(id, PLACA, "Fiat", "Uno", 2015, UUID.randomUUID()))
                .isInstanceOf(VeiculoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("deve transferir o veiculo para outro Cliente ativo")
    void deveTransferirProprietario() {
        Cliente dono = proprietario();
        Veiculo existente = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, UUID.randomUUID());
        when(veiculos.buscarAtivoParaModificacao(existente.id())).thenReturn(Optional.of(existente));
        when(clientes.buscarAtivoPorId(dono.id())).thenReturn(Optional.of(dono));
        when(veiculos.placaJaCadastradaPorOutro(existente.id(), PLACA)).thenReturn(false);
        when(veiculos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Veiculo alterado = new AlterarVeiculoUseCase(veiculos, clientes)
                .executar(existente.id(), PLACA, "Fiat", "Uno", 2015, dono.id());

        assertThat(alterado.clienteId()).isEqualTo(dono.id());
    }

    @Test
    @DisplayName("deve recusar inativacao quando o veiculo tem Ordem de Servico em andamento")
    void deveRecusarInativacaoComOrdemServicoEmAndamento() {
        Veiculo existente = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, UUID.randomUUID());
        when(veiculos.buscarAtivoParaModificacao(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServico.existemParaVeiculo(existente.id())).thenReturn(true);

        assertThatThrownBy(() -> new InativarVeiculoUseCase(veiculos, ordensServico).executar(existente.id()))
                .isInstanceOf(VeiculoComOrdemServicoEmAndamentoException.class);
        verify(veiculos, never()).salvar(any());
    }

    @Test
    @DisplayName("deve inativar o veiculo sem apagar o registro")
    void deveInativarVeiculo() {
        Veiculo existente = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, UUID.randomUUID());
        when(veiculos.buscarAtivoParaModificacao(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServico.existemParaVeiculo(existente.id())).thenReturn(false);

        new InativarVeiculoUseCase(veiculos, ordensServico).executar(existente.id());

        ArgumentCaptor<Veiculo> salvo = ArgumentCaptor.forClass(Veiculo.class);
        verify(veiculos).salvar(salvo.capture());
        assertThat(salvo.getValue().ativo()).isFalse();
    }

    @Test
    @DisplayName("deve recusar detalhamento de veiculo inexistente ou removido logicamente")
    void deveRecusarDetalhamentoDeVeiculoInexistente() {
        UUID id = UUID.randomUUID();
        when(veiculos.buscarAtivoPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DetalharVeiculoUseCase(veiculos).executar(id))
                .isInstanceOf(VeiculoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("deve listar apenas os ativos quando nao ha filtro por Placa")
    void deveListarAtivosSemFiltro() {
        Veiculo ativo = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, UUID.randomUUID());
        when(veiculos.listarAtivos()).thenReturn(List.of(ativo));

        assertThat(new ListarVeiculosUseCase(veiculos).executar(Optional.empty())).containsExactly(ativo);
    }

    @Test
    @DisplayName("deve devolver lista vazia quando o filtro por Placa nao acha veiculo ativo")
    void deveDevolverListaVaziaQuandoFiltroNaoAcha() {
        when(veiculos.buscarAtivoPorPlaca(PLACA)).thenReturn(Optional.empty());

        assertThat(new ListarVeiculosUseCase(veiculos).executar(Optional.of(PLACA))).isEmpty();
    }

    @Test
    @DisplayName("deve devolver o veiculo do filtro por Placa")
    void deveDevolverVeiculoDoFiltro() {
        Veiculo ativo = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, UUID.randomUUID());
        when(veiculos.buscarAtivoPorPlaca(PLACA)).thenReturn(Optional.of(ativo));

        assertThat(new ListarVeiculosUseCase(veiculos).executar(Optional.of(PLACA))).containsExactly(ativo);
    }
}
