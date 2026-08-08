package br.com.oficinamecanica.cadastro.api;

import br.com.oficinamecanica.cadastro.domain.Veiculo;
import java.util.UUID;

public record VeiculoResponse(UUID id, String placa, String marca, String modelo, int ano, UUID clienteId) {

    static VeiculoResponse de(Veiculo veiculo) {
        return new VeiculoResponse(
                veiculo.id(),
                veiculo.placa().valor(),
                veiculo.marca(),
                veiculo.modelo(),
                veiculo.ano(),
                veiculo.clienteId());
    }
}
