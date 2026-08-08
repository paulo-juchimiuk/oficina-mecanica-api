package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.UUID;

public class Veiculo {

    private static final String CODIGO = "VEICULO_INVALIDO";

    private final UUID id;
    private Placa placa;
    private String marca;
    private String modelo;
    private int ano;
    private UUID clienteId;
    private boolean ativo;

    public Veiculo(UUID id, Placa placa, String marca, String modelo, int ano, UUID clienteId, boolean ativo) {
        if (id == null) {
            throw new DadosInvalidosException(CODIGO, "Veiculo exige identidade");
        }
        this.id = id;
        this.ativo = ativo;
        alterar(placa, marca, modelo, ano, clienteId);
    }

    public static Veiculo cadastrar(Placa placa, String marca, String modelo, int ano, UUID clienteId) {
        return new Veiculo(UUID.randomUUID(), placa, marca, modelo, ano, clienteId, true);
    }

    public final void alterar(Placa placa, String marca, String modelo, int ano, UUID clienteId) {
        if (placa == null) {
            throw new DadosInvalidosException(CODIGO, "Veiculo exige Placa");
        }
        if (marca == null || marca.isBlank()) {
            throw new DadosInvalidosException(CODIGO, "Veiculo exige marca");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new DadosInvalidosException(CODIGO, "Veiculo exige modelo");
        }
        if (clienteId == null) {
            throw new DadosInvalidosException(CODIGO, "Veiculo exige o Cliente proprietario");
        }
        this.placa = placa;
        this.marca = marca.trim();
        this.modelo = modelo.trim();
        this.ano = ano;
        this.clienteId = clienteId;
    }

    public void inativar() {
        this.ativo = false;
    }

    public UUID id() {
        return id;
    }

    public Placa placa() {
        return placa;
    }

    public String marca() {
        return marca;
    }

    public String modelo() {
        return modelo;
    }

    public int ano() {
        return ano;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public boolean ativo() {
        return ativo;
    }
}
