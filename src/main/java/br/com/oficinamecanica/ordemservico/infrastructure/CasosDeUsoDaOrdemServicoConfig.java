package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.application.AprovarOrcamentoUseCase;
import br.com.oficinamecanica.ordemservico.application.ConcluirDiagnosticoUseCase;
import br.com.oficinamecanica.ordemservico.application.ConcluirExecucaoUseCase;
import br.com.oficinamecanica.ordemservico.application.ConsultarAcompanhamentoUseCase;
import br.com.oficinamecanica.ordemservico.application.ConsultarTempoMedioExecucaoUseCase;
import br.com.oficinamecanica.ordemservico.application.CriarOrdemServicoUseCase;
import br.com.oficinamecanica.ordemservico.application.DetalharOrdemServicoUseCase;
import br.com.oficinamecanica.ordemservico.application.IncluirItensUseCase;
import br.com.oficinamecanica.ordemservico.application.IniciarDiagnosticoUseCase;
import br.com.oficinamecanica.ordemservico.application.ListarOrdensServicoUseCase;
import br.com.oficinamecanica.ordemservico.application.NotificacaoAoCliente;
import br.com.oficinamecanica.ordemservico.application.NotificadorDoCliente;
import br.com.oficinamecanica.ordemservico.application.PrecificadorDeItens;
import br.com.oficinamecanica.ordemservico.application.RegistrarEntregaUseCase;
import br.com.oficinamecanica.ordemservico.application.RegistrarReparoAdicionalUseCase;
import br.com.oficinamecanica.ordemservico.application.ReprovarOrcamentoUseCase;
import br.com.oficinamecanica.ordemservico.application.ReservaDePecas;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.Pecas;
import br.com.oficinamecanica.ordemservico.domain.Servicos;
import br.com.oficinamecanica.ordemservico.domain.Veiculos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CasosDeUsoDaOrdemServicoConfig {

    @Bean
    PrecificadorDeItens precificadorDeItens(Servicos servicos, Pecas pecas) {
        return new PrecificadorDeItens(servicos, pecas);
    }

    @Bean
    NotificadorDoCliente notificadorDoCliente(Clientes clientes, NotificacaoAoCliente notificacao) {
        return new NotificadorDoCliente(clientes, notificacao);
    }

    @Bean
    AprovarOrcamentoUseCase aprovarOrcamentoUseCase(OrdemServicoRepository ordensServico, ReservaDePecas reservaDePecas, ConsultarAcompanhamentoUseCase acompanhamento, NotificadorDoCliente notificador) {
        return new AprovarOrcamentoUseCase(ordensServico, reservaDePecas, acompanhamento, notificador);
    }

    @Bean
    ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase(OrdemServicoRepository ordensServico, NotificadorDoCliente notificador) {
        return new ConcluirDiagnosticoUseCase(ordensServico, notificador);
    }

    @Bean
    ConcluirExecucaoUseCase concluirExecucaoUseCase(OrdemServicoRepository ordensServico, NotificadorDoCliente notificador) {
        return new ConcluirExecucaoUseCase(ordensServico, notificador);
    }

    @Bean
    ConsultarAcompanhamentoUseCase consultarAcompanhamentoUseCase(OrdemServicoRepository ordensServico, Veiculos veiculos, Servicos servicos, Pecas pecas) {
        return new ConsultarAcompanhamentoUseCase(ordensServico, veiculos, servicos, pecas);
    }

    @Bean
    ConsultarTempoMedioExecucaoUseCase consultarTempoMedioExecucaoUseCase(OrdemServicoRepository ordensServico) {
        return new ConsultarTempoMedioExecucaoUseCase(ordensServico);
    }

    @Bean
    CriarOrdemServicoUseCase criarOrdemServicoUseCase(OrdemServicoRepository ordensServico, Clientes clientes, Veiculos veiculos, PrecificadorDeItens precificador, NotificadorDoCliente notificador) {
        return new CriarOrdemServicoUseCase(ordensServico, clientes, veiculos, precificador, notificador);
    }

    @Bean
    DetalharOrdemServicoUseCase detalharOrdemServicoUseCase(OrdemServicoRepository ordensServico) {
        return new DetalharOrdemServicoUseCase(ordensServico);
    }

    @Bean
    IncluirItensUseCase incluirItensUseCase(OrdemServicoRepository ordensServico, PrecificadorDeItens precificador) {
        return new IncluirItensUseCase(ordensServico, precificador);
    }

    @Bean
    IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase(OrdemServicoRepository ordensServico, NotificadorDoCliente notificador) {
        return new IniciarDiagnosticoUseCase(ordensServico, notificador);
    }

    @Bean
    ListarOrdensServicoUseCase listarOrdensServicoUseCase(OrdemServicoRepository ordensServico) {
        return new ListarOrdensServicoUseCase(ordensServico);
    }

    @Bean
    RegistrarEntregaUseCase registrarEntregaUseCase(OrdemServicoRepository ordensServico, NotificadorDoCliente notificador) {
        return new RegistrarEntregaUseCase(ordensServico, notificador);
    }

    @Bean
    RegistrarReparoAdicionalUseCase registrarReparoAdicionalUseCase(OrdemServicoRepository ordensServico, PrecificadorDeItens precificador, NotificadorDoCliente notificador) {
        return new RegistrarReparoAdicionalUseCase(ordensServico, precificador, notificador);
    }

    @Bean
    ReprovarOrcamentoUseCase reprovarOrcamentoUseCase(OrdemServicoRepository ordensServico, ConsultarAcompanhamentoUseCase acompanhamento, NotificadorDoCliente notificador) {
        return new ReprovarOrcamentoUseCase(ordensServico, acompanhamento, notificador);
    }
}
