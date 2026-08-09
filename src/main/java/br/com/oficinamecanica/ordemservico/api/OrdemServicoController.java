package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.application.ConcluirDiagnosticoUseCase;
import br.com.oficinamecanica.ordemservico.application.ConcluirExecucaoUseCase;
import br.com.oficinamecanica.ordemservico.application.CriarOrdemServicoUseCase;
import br.com.oficinamecanica.ordemservico.application.DetalharOrdemServicoUseCase;
import br.com.oficinamecanica.ordemservico.application.IncluirItensUseCase;
import br.com.oficinamecanica.ordemservico.application.IniciarDiagnosticoUseCase;
import br.com.oficinamecanica.ordemservico.application.ItemDePecaRequisitado;
import br.com.oficinamecanica.ordemservico.application.ItemDeServicoRequisitado;
import br.com.oficinamecanica.ordemservico.application.ListarOrdensServicoUseCase;
import br.com.oficinamecanica.ordemservico.application.RegistrarEntregaUseCase;
import br.com.oficinamecanica.ordemservico.application.RegistrarReparoAdicionalUseCase;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemServicoController {

    private final CriarOrdemServicoUseCase criarOrdemServico;
    private final ListarOrdensServicoUseCase listarOrdensServico;
    private final DetalharOrdemServicoUseCase detalharOrdemServico;
    private final IniciarDiagnosticoUseCase iniciarDiagnostico;
    private final IncluirItensUseCase incluirItens;
    private final ConcluirDiagnosticoUseCase concluirDiagnostico;
    private final RegistrarReparoAdicionalUseCase registrarReparoAdicional;
    private final ConcluirExecucaoUseCase concluirExecucao;
    private final RegistrarEntregaUseCase registrarEntrega;

    public OrdemServicoController(CriarOrdemServicoUseCase criarOrdemServico,
                                  ListarOrdensServicoUseCase listarOrdensServico,
                                  DetalharOrdemServicoUseCase detalharOrdemServico,
                                  IniciarDiagnosticoUseCase iniciarDiagnostico,
                                  IncluirItensUseCase incluirItens,
                                  ConcluirDiagnosticoUseCase concluirDiagnostico,
                                  RegistrarReparoAdicionalUseCase registrarReparoAdicional,
                                  ConcluirExecucaoUseCase concluirExecucao,
                                  RegistrarEntregaUseCase registrarEntrega) {
        this.criarOrdemServico = criarOrdemServico;
        this.listarOrdensServico = listarOrdensServico;
        this.detalharOrdemServico = detalharOrdemServico;
        this.iniciarDiagnostico = iniciarDiagnostico;
        this.incluirItens = incluirItens;
        this.concluirDiagnostico = concluirDiagnostico;
        this.registrarReparoAdicional = registrarReparoAdicional;
        this.concluirExecucao = concluirExecucao;
        this.registrarEntrega = registrarEntrega;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemServicoResponse criar(@Valid @RequestBody CriarOrdemServicoRequest requisicao) {
        return OrdemServicoResponse.de(criarOrdemServico.executar(
                requisicao.documentoCliente(), requisicao.veiculoId(), requisicao.relatoDoProblema()));
    }

    @GetMapping
    public List<OrdemServicoSummaryResponse> listar(@RequestParam(required = false) StatusOrdemServico status) {
        return listarOrdensServico.executar(Optional.ofNullable(status)).stream()
                .map(OrdemServicoSummaryResponse::de)
                .toList();
    }

    @GetMapping("/{id}")
    public OrdemServicoResponse detalhar(@PathVariable UUID id) {
        return OrdemServicoResponse.de(detalharOrdemServico.executar(id));
    }

    @PostMapping("/{id}/diagnostico/inicio")
    public OrdemServicoResponse iniciarDiagnostico(@PathVariable UUID id) {
        return OrdemServicoResponse.de(iniciarDiagnostico.executar(id));
    }

    @PostMapping("/{id}/itens")
    public OrdemServicoResponse incluirItens(@PathVariable UUID id,
                                             @Valid @RequestBody IncluirItensRequest requisicao) {
        return OrdemServicoResponse.de(incluirItens.executar(id,
                servicosDe(requisicao.itensServico()), pecasDe(requisicao.itensPeca())));
    }

    @PostMapping("/{id}/diagnostico/conclusao")
    public OrdemServicoResponse concluirDiagnostico(@PathVariable UUID id) {
        return OrdemServicoResponse.de(concluirDiagnostico.executar(id));
    }



    @PostMapping("/{id}/reparos-adicionais")
    public OrdemServicoResponse registrarReparoAdicional(@PathVariable UUID id,
                                                         @Valid @RequestBody ReparoAdicionalRequest requisicao) {
        return OrdemServicoResponse.de(registrarReparoAdicional.executar(id, requisicao.descricao(),
                servicosDe(requisicao.itensServico()), pecasDe(requisicao.itensPeca())));
    }

    @PostMapping("/{id}/execucao/conclusao")
    public OrdemServicoResponse concluirExecucao(@PathVariable UUID id) {
        return OrdemServicoResponse.de(concluirExecucao.executar(id));
    }

    @PostMapping("/{id}/entrega")
    public OrdemServicoResponse registrarEntrega(@PathVariable UUID id) {
        return OrdemServicoResponse.de(registrarEntrega.executar(id));
    }

    private List<ItemDeServicoRequisitado> servicosDe(List<IncluirItensRequest.ItemDeServico> itens) {
        return Optional.ofNullable(itens).orElseGet(List::of).stream()
                .map(item -> new ItemDeServicoRequisitado(item.servicoId()))
                .toList();
    }

    private List<ItemDePecaRequisitado> pecasDe(List<IncluirItensRequest.ItemDePeca> itens) {
        return Optional.ofNullable(itens).orElseGet(List::of).stream()
                .map(item -> new ItemDePecaRequisitado(item.pecaId(), item.quantidade()))
                .toList();
    }
}
