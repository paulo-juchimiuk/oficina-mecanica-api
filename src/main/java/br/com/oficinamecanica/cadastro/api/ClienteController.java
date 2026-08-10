package br.com.oficinamecanica.cadastro.api;

import br.com.oficinamecanica.cadastro.application.AlterarClienteUseCase;
import br.com.oficinamecanica.cadastro.application.CadastrarClienteUseCase;
import br.com.oficinamecanica.cadastro.application.DetalharClienteUseCase;
import br.com.oficinamecanica.cadastro.application.InativarClienteUseCase;
import br.com.oficinamecanica.cadastro.application.ListarClientesUseCase;
import br.com.oficinamecanica.cadastro.domain.Contato;
import br.com.oficinamecanica.cadastro.domain.Documento;
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

@Tag(name = "Clientes")
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final CadastrarClienteUseCase cadastrarCliente;
    private final AlterarClienteUseCase alterarCliente;
    private final InativarClienteUseCase inativarCliente;
    private final DetalharClienteUseCase detalharCliente;
    private final ListarClientesUseCase listarClientes;

    public ClienteController(CadastrarClienteUseCase cadastrarCliente,
                             AlterarClienteUseCase alterarCliente,
                             InativarClienteUseCase inativarCliente,
                             DetalharClienteUseCase detalharCliente,
                             ListarClientesUseCase listarClientes) {
        this.cadastrarCliente = cadastrarCliente;
        this.alterarCliente = alterarCliente;
        this.inativarCliente = inativarCliente;
        this.detalharCliente = detalharCliente;
        this.listarClientes = listarClientes;
    }

    @PostMapping
    @Operation(summary = "Cadastrar cliente")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse cadastrar(@Valid @RequestBody ClienteRequest requisicao) {
        return ClienteResponse.de(cadastrarCliente.executar(
                requisicao.nome(), new Documento(requisicao.documento()), contatoDe(requisicao)));
    }

    @GetMapping
    @Operation(summary = "Listar clientes")
    public List<ClienteResponse> listar(@RequestParam(required = false) String documento) {
        Optional<Documento> filtro = Optional.ofNullable(documento).map(Documento::new);
        return listarClientes.executar(filtro).stream().map(ClienteResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar cliente")
    public ClienteResponse detalhar(@PathVariable UUID id) {
        return ClienteResponse.de(detalharCliente.executar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar cliente")
    public ClienteResponse alterar(@PathVariable UUID id, @Valid @RequestBody ClienteRequest requisicao) {
        return ClienteResponse.de(alterarCliente.executar(
                id, requisicao.nome(), new Documento(requisicao.documento()), contatoDe(requisicao)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover cliente")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        inativarCliente.executar(id);
    }

    private Contato contatoDe(ClienteRequest requisicao) {
        return new Contato(requisicao.email(), requisicao.telefone());
    }
}
