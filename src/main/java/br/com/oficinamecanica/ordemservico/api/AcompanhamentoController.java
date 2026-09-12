package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.application.AprovarOrcamentoUseCase;
import br.com.oficinamecanica.ordemservico.application.ConsultarAcompanhamentoUseCase;
import br.com.oficinamecanica.ordemservico.application.ReprovarOrcamentoUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Acompanhamento")
@RestController
@SecurityRequirements
@RequestMapping(AcompanhamentoController.CAMINHO)
public class AcompanhamentoController {

    static final String CAMINHO = "/acompanhamento";

    private final ConsultarAcompanhamentoUseCase consultarAcompanhamento;
    private final AprovarOrcamentoUseCase aprovarOrcamento;
    private final ReprovarOrcamentoUseCase reprovarOrcamento;

    public AcompanhamentoController(ConsultarAcompanhamentoUseCase consultarAcompanhamento,
                                    AprovarOrcamentoUseCase aprovarOrcamento,
                                    ReprovarOrcamentoUseCase reprovarOrcamento) {
        this.consultarAcompanhamento = consultarAcompanhamento;
        this.aprovarOrcamento = aprovarOrcamento;
        this.reprovarOrcamento = reprovarOrcamento;
    }

    @GetMapping("/{codigoAcompanhamento}")
    @Operation(summary = "Consultar progresso da OS (Cliente, sem JWT)")
    public AcompanhamentoResponse consultar(@PathVariable String codigoAcompanhamento) {
        return AcompanhamentoResponse.de(consultarAcompanhamento.executar(codigoAcompanhamento));
    }

    @PostMapping("/{codigoAcompanhamento}/orcamento/aprovacao")
    @Operation(summary = "Notificacao externa de aprovacao do orcamento (Cliente, sem JWT)")
    public AcompanhamentoResponse aprovar(@PathVariable String codigoAcompanhamento) {
        return AcompanhamentoResponse.de(aprovarOrcamento.executar(codigoAcompanhamento));
    }

    @PostMapping("/{codigoAcompanhamento}/orcamento/reprovacao")
    @Operation(summary = "Notificacao externa de recusa do orcamento (Cliente, sem JWT)")
    public AcompanhamentoResponse reprovar(@PathVariable String codigoAcompanhamento) {
        return AcompanhamentoResponse.de(reprovarOrcamento.executar(codigoAcompanhamento));
    }
}
