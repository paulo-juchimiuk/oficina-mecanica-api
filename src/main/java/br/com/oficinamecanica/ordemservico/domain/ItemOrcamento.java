package br.com.oficinamecanica.ordemservico.domain;

public record ItemOrcamento(TipoItemOrcamento tipo, String nome, Integer quantidade, Dinheiro valor) {

    public static ItemOrcamento deServico(String nome, Dinheiro valor) {
        return new ItemOrcamento(TipoItemOrcamento.SERVICO, nome, null, valor);
    }

    public static ItemOrcamento dePeca(String nome, int quantidade, Dinheiro valor) {
        return new ItemOrcamento(TipoItemOrcamento.PECA, nome, quantidade, valor);
    }
}
