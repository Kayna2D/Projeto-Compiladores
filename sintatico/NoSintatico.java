/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sintatico;

import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author Usuario
 */
public class NoSintatico {
    private String rotulo;
    private String valor;
    private int linha;
    private int coluna;
    private List<NoSintatico> filhos;
    
    public NoSintatico(String rotulo) {
        this.rotulo = rotulo;
        this.valor = null;
        this.linha = -1;
        this.coluna = -1;
        this.filhos = new ArrayList<>();
    }
    
    public NoSintatico(String rotulo, String valor) {
        this.rotulo = rotulo;
        this.valor = valor;
        this.linha = -1;
        this.coluna = -1;
        this.filhos = new ArrayList<>();
    }

    public NoSintatico(String rotulo, int linha, int coluna) {
        this(rotulo, null, linha, coluna);
    }

    public NoSintatico(String rotulo, String valor, int linha, int coluna) {
        this.rotulo = rotulo;
        this.valor = valor;
        this.linha = linha;
        this.coluna = coluna;
        this.filhos = new ArrayList<>();
    }
    
    public void adicionarFilho(NoSintatico filho) {
        if (filho != null) {
            filhos.add(filho);
        }
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getValor() {
        return valor;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    public List<NoSintatico> getFilhos() {
        return filhos;
    }
    
    public void imprimirArvore(String prefixo, boolean isLast) {
        System.out.print(prefixo);
        
        if (isLast) {
            System.out.print("└── ");
            prefixo += "    ";
        } else {
            System.out.print("├── ");
            prefixo += "│   ";
        }
        
        // Imprime o nó atual
        if (valor != null && !valor.isEmpty()) {
            System.out.println(rotulo + ": '" + valor + "'");
        } else {
            System.out.println(rotulo);
        }

        for (int i = 0; i < filhos.size(); i++) {
            filhos.get(i).imprimirArvore(prefixo, i == filhos.size() - 1);
        }
    }
}
