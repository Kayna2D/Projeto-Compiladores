/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package lexico;

/**
 *
 * @author Usuario
 */
public class Token {
    public enum TipoToken {
        // Palavras reservadas
        INICIO, FIM, INTEIRO, DECIMAL, TEXTO,
        LER, PRINTAR, SE, SENAO, SENAO_SE,
        ENQUANTO, PARA,
        
        // Identificadores e literais
        ID, NUM_INT, NUM_DECIMAL, TEXTO_LITERAL,
        
        // Operadores relacionais
        OP_REL_MENOR, OP_REL_MAIOR, OP_REL_MENOR_IGUAL,
        OP_REL_MAIOR_IGUAL, OP_REL_DIFERENTE, OP_REL_IGUAL,
        
        // Operadores aritméticos
        OP_SOMA, OP_SUB, OP_MULT, OP_DIV,
        
        // Operadores lógicos
        OP_AND, OP_OR,
        
        // Atribuição
        OP_ATRIB,
        
        // Delimitadores
        ABRE_PAREN, FECHA_PAREN, ABRE_CHAVE, FECHA_CHAVE,
        PONTO_VIRGULA, PONTO_FINAL,
        
        // Especiais
        EOF, ERRO
    }
    
    private TipoToken tipo;
    private String lexema;
    private int linha;
    private int coluna;

    public Token(TipoToken tipo, String lexema, int linha, int coluna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    @Override
    public String toString() {
        return "<" + tipo + ", " + lexema + ">";
    }
    
    
    
}
