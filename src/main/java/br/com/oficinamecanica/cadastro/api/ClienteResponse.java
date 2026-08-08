package br.com.oficinamecanica.cadastro.api;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import java.util.UUID;

public record ClienteResponse(UUID id, String nome, String documento, String email, String telefone) {

    static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(
                cliente.id(),
                cliente.nome(),
                cliente.documento().numero(),
                cliente.contato().email(),
                cliente.contato().telefone());
    }
}
