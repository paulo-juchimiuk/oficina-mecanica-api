package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.application.ConsultarPecasReservadasUseCase;
import br.com.oficinamecanica.estoque.application.DevolverPecasNaoUtilizadasUseCase;
import br.com.oficinamecanica.estoque.application.RegistrarFaltaDePecaUseCase;
import br.com.oficinamecanica.estoque.application.RetirarPecasReservadasUseCase;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@Tag(name = "Pecas")
@RestController
@RequestMapping("/ordens-servico/{id}")
public class EstoqueDaOrdemServicoController {

    private final ConsultarPecasReservadasUseCase consultarPecasReservadas;
    private final RetirarPecasReservadasUseCase retirarPecasReservadas;
    private final DevolverPecasNaoUtilizadasUseCase devolverPecasNaoUtilizadas;
    private final RegistrarFaltaDePecaUseCase registrarFaltaDePeca;

    public EstoqueDaOrdemServicoController(ConsultarPecasReservadasUseCase consultarPecasReservadas,
                                           RetirarPecasReservadasUseCase retirarPecasReservadas,
                                           DevolverPecasNaoUtilizadasUseCase devolverPecasNaoUtilizadas,
                                           RegistrarFaltaDePecaUseCase registrarFaltaDePeca) {
        this.consultarPecasReservadas = consultarPecasReservadas;
        this.retirarPecasReservadas = retirarPecasReservadas;
        this.devolverPecasNaoUtilizadas = devolverPecasNaoUtilizadas;
        this.registrarFaltaDePeca = registrarFaltaDePeca;
    }

    @GetMapping("/pecas-reservadas")
    @Operation(summary = "Consultar pecas reservadas da OS")
    public List<ReservaPecaResponse> consultarReservadas(@PathVariable("id") UUID ordemServicoId) {
        return consultarPecasReservadas.executar(ordemServicoId).stream().map(ReservaPecaResponse::de).toList();
    }

    @PostMapping("/pecas/retirada")
    @Operation(summary = "Retirar pecas reservadas")
    public List<ReservaPecaResponse> retirar(@PathVariable("id") UUID ordemServicoId,
                                             @Valid @RequestBody ReservasRequest requisicao) {
        return retirarPecasReservadas.executar(ordemServicoId, requisicao.identificadores()).stream()
                .map(ReservaPecaResponse::de)
                .toList();
    }

    @PostMapping("/pecas/devolucao")
    @Operation(summary = "Devolver peca nao utilizada")
    public List<ReservaPecaResponse> devolver(@PathVariable("id") UUID ordemServicoId,
                                              @Valid @RequestBody ReservasRequest requisicao) {
        return devolverPecasNaoUtilizadas.executar(ordemServicoId, requisicao.identificadores()).stream()
                .map(ReservaPecaResponse::de)
                .toList();
    }

    @PostMapping("/pecas/faltas")
    @Operation(summary = "Registrar falta de peca")
    @ResponseStatus(HttpStatus.CREATED)
    public PendenciaPecaResponse registrarFalta(@PathVariable("id") UUID ordemServicoId,
                                                @Valid @RequestBody FaltaPecaRequest requisicao) {
        return PendenciaPecaResponse.de(registrarFaltaDePeca.executar(
                ordemServicoId, requisicao.pecaId(), requisicao.quantidadeFaltante()));
    }
}
