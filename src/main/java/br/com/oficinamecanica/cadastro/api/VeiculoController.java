package br.com.oficinamecanica.cadastro.api;

import br.com.oficinamecanica.cadastro.application.AlterarVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.CadastrarVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.DetalharVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.InativarVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.ListarVeiculosUseCase;
import br.com.oficinamecanica.cadastro.domain.Placa;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Veiculos")
@RestController
@RequestMapping("/veiculos")
public class VeiculoController {

    private final CadastrarVeiculoUseCase cadastrarVeiculo;
    private final AlterarVeiculoUseCase alterarVeiculo;
    private final InativarVeiculoUseCase inativarVeiculo;
    private final DetalharVeiculoUseCase detalharVeiculo;
    private final ListarVeiculosUseCase listarVeiculos;

    public VeiculoController(CadastrarVeiculoUseCase cadastrarVeiculo,
                             AlterarVeiculoUseCase alterarVeiculo,
                             InativarVeiculoUseCase inativarVeiculo,
                             DetalharVeiculoUseCase detalharVeiculo,
                             ListarVeiculosUseCase listarVeiculos) {
        this.cadastrarVeiculo = cadastrarVeiculo;
        this.alterarVeiculo = alterarVeiculo;
        this.inativarVeiculo = inativarVeiculo;
        this.detalharVeiculo = detalharVeiculo;
        this.listarVeiculos = listarVeiculos;
    }

    @PostMapping
    @Operation(summary = "Cadastrar veiculo")
    @ResponseStatus(HttpStatus.CREATED)
    public VeiculoResponse cadastrar(@Valid @RequestBody VeiculoRequest requisicao) {
        return VeiculoResponse.de(cadastrarVeiculo.executar(
                new Placa(requisicao.placa()), requisicao.marca(), requisicao.modelo(),
                requisicao.ano(), requisicao.clienteId()));
    }

    @GetMapping
    @Operation(summary = "Listar veiculos")
    public List<VeiculoResponse> listar(@RequestParam(required = false) String placa) {
        Optional<Placa> filtro = Optional.ofNullable(placa).map(Placa::new);
        return listarVeiculos.executar(filtro).stream().map(VeiculoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar veiculo")
    public VeiculoResponse detalhar(@PathVariable UUID id) {
        return VeiculoResponse.de(detalharVeiculo.executar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar veiculo")
    public VeiculoResponse alterar(@PathVariable UUID id, @Valid @RequestBody VeiculoRequest requisicao) {
        return VeiculoResponse.de(alterarVeiculo.executar(
                id, new Placa(requisicao.placa()), requisicao.marca(), requisicao.modelo(),
                requisicao.ano(), requisicao.clienteId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover veiculo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        inativarVeiculo.executar(id);
    }
}
