package br.com.oficinamecanica.ordemservico.domain;

import java.util.UUID;

public record ServicoAIncluir(UUID servicoId, Dinheiro valorMaoDeObra) {
}
