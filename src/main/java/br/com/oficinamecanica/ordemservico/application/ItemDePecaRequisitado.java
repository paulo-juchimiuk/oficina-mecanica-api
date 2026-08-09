package br.com.oficinamecanica.ordemservico.application;

import java.util.UUID;

public record ItemDePecaRequisitado(UUID pecaId, int quantidade) {
}
