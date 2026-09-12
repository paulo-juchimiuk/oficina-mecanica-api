package br.com.oficinamecanica.cadastro.infrastructure;

import br.com.oficinamecanica.cadastro.application.AlterarClienteUseCase;
import br.com.oficinamecanica.cadastro.application.AlterarVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.CadastrarClienteUseCase;
import br.com.oficinamecanica.cadastro.application.CadastrarVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.DetalharClienteUseCase;
import br.com.oficinamecanica.cadastro.application.DetalharVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.InativarClienteUseCase;
import br.com.oficinamecanica.cadastro.application.InativarVeiculoUseCase;
import br.com.oficinamecanica.cadastro.application.ListarClientesUseCase;
import br.com.oficinamecanica.cadastro.application.ListarVeiculosUseCase;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CasosDeUsoDoCadastroConfig {

    @Bean
    AlterarClienteUseCase alterarClienteUseCase(ClienteRepository clientes) {
        return new AlterarClienteUseCase(clientes);
    }

    @Bean
    AlterarVeiculoUseCase alterarVeiculoUseCase(VeiculoRepository veiculos, ClienteRepository clientes) {
        return new AlterarVeiculoUseCase(veiculos, clientes);
    }

    @Bean
    CadastrarClienteUseCase cadastrarClienteUseCase(ClienteRepository clientes) {
        return new CadastrarClienteUseCase(clientes);
    }

    @Bean
    CadastrarVeiculoUseCase cadastrarVeiculoUseCase(VeiculoRepository veiculos, ClienteRepository clientes) {
        return new CadastrarVeiculoUseCase(veiculos, clientes);
    }

    @Bean
    DetalharClienteUseCase detalharClienteUseCase(ClienteRepository clientes) {
        return new DetalharClienteUseCase(clientes);
    }

    @Bean
    DetalharVeiculoUseCase detalharVeiculoUseCase(VeiculoRepository veiculos) {
        return new DetalharVeiculoUseCase(veiculos);
    }

    @Bean
    InativarClienteUseCase inativarClienteUseCase(ClienteRepository clientes, OrdensServicoEmAndamento ordensServico) {
        return new InativarClienteUseCase(clientes, ordensServico);
    }

    @Bean
    InativarVeiculoUseCase inativarVeiculoUseCase(VeiculoRepository veiculos, OrdensServicoEmAndamento ordensServico) {
        return new InativarVeiculoUseCase(veiculos, ordensServico);
    }

    @Bean
    ListarClientesUseCase listarClientesUseCase(ClienteRepository clientes) {
        return new ListarClientesUseCase(clientes);
    }

    @Bean
    ListarVeiculosUseCase listarVeiculosUseCase(VeiculoRepository veiculos) {
        return new ListarVeiculosUseCase(veiculos);
    }
}
