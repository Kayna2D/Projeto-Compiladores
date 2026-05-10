/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package lexico;

/**
 *
 * @author Usuario
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;
    
public class Lexer {
    private BufferedReader reader;
    private char caractereAtual;
    private int linha;
    private int coluna;
    private boolean eof;
    private List<Token> tokens;
    private List<String> erros;
    
    // Buffer de pushback
    private boolean hasPushback;
    private char pushbackChar;
    private int pushbackLinha;
    private int pushbackColuna;
    
    public Lexer(String arquivoFonte) throws IOException {
        this.reader = new BufferedReader(new FileReader(arquivoFonte));
        this.linha = 1;
        this.coluna = 0;
        this.eof = false;
        this.tokens = new ArrayList<>();
        this.erros = new ArrayList<>();
        this.hasPushback = false;
        avancarCaractere();
    }
    
    private void avancarCaractere() throws IOException {
        if (hasPushback) {
            // Usa o caractere do buffer
            caractereAtual = pushbackChar;
            linha = pushbackLinha;
            coluna = pushbackColuna;
            hasPushback = false;
        } else {
            int next = reader.read();
            if (next == -1) {
                eof = true;
                caractereAtual = '\0';
            } else {
                caractereAtual = (char) next;
                coluna++;
                if (caractereAtual == '\n') {
                    linha++;
                    coluna = 0;
                }
            }
        }
    }
    
    private void pushback(char c, int lin, int col) {
        hasPushback = true;
        pushbackChar = c;
        pushbackLinha = lin;
        pushbackColuna = col;
    }
    
    public void skipWhiteSpace() throws IOException {
        while (!eof && Character.isWhitespace(caractereAtual)) {
            avancarCaractere();
        }
    }
    
    private void skipCommentLine() throws IOException {
        while (!eof && caractereAtual != '\n') {
            avancarCaractere();
        }
    }
    
    private Token lerNumero() throws IOException {
        StringBuilder lexema = new StringBuilder();
        int linhaToken = linha;
        int colunaToken = coluna;
        boolean isDecimal = false;
        
        // Lê dígitos antes do ponto
        while (!eof && Character.isDigit(caractereAtual)) {
            lexema.append(caractereAtual);
            avancarCaractere();
        }
        
        // Ponto decimal?
        if (!eof && caractereAtual == '.') {
            // Guarda posição do ponto
            int linhaPonto = linha;
            int colunaPonto = coluna;
            char pontoChar = caractereAtual;

            // Avança o ponto
            avancarCaractere();
            
            // Verifica se o próximo caractere é um dígito
            if (!eof && Character.isDigit(caractereAtual)) {
                // É número decimal! Adiciona o ponto e continua lendo dígitos
                lexema.append('.');
                isDecimal = true;

                while (!eof && Character.isDigit(caractereAtual)) {
                    lexema.append(caractereAtual);
                    avancarCaractere();
                }
            } else {
                pushback(pontoChar, linhaPonto, colunaPonto);
            }
        }
        
        // Validador de ID
        if (!eof && Character.isLetter(caractereAtual)) {
            while (!eof && (Character.isLetterOrDigit(caractereAtual) || caractereAtual == '_')) {
                lexema.append(caractereAtual);
                avancarCaractere();
            }
            erros.add(String.format("Erro léxico: identificador inválido '%s' "
                    + "na linha %d, coluna %d", 
                    lexema.toString(), linhaToken, colunaToken));
            return new Token(Token.TipoToken.ERRO, lexema.toString(), linhaToken, colunaToken);
        }
        
        return new Token(
                isDecimal ? Token.TipoToken.NUM_DECIMAL : Token.TipoToken.NUM_INT,
                lexema.toString(),
                linhaToken,
                colunaToken
        );
    }
    
    private Token lerIdOuPalavraReservada() throws IOException {
        StringBuilder lexema = new StringBuilder();
        int linhaToken = linha;
        int colunaToken = coluna;
        
        while (!eof && (Character.isLetterOrDigit(caractereAtual) || caractereAtual == '_')) {
            lexema.append(caractereAtual);
            avancarCaractere();
        }
        
        String lexemaStr = lexema.toString();
        Token.TipoToken tipo = TabelaSimbolos.getToken(lexemaStr);
        
        return new Token(tipo, lexemaStr, linhaToken, colunaToken);
    }
    
    private Token lerTexto() throws IOException {
        StringBuilder lexema = new StringBuilder();
        int linhaToken = linha;
        int colunaToken = coluna;
        
        // Trata aspa inicial
        avancarCaractere();
        
        while (!eof && caractereAtual != '"') {
            if (caractereAtual == '\n') {
                erros.add(String.format("Erro léxico: string não fechada na linha %d, coluna %d", 
                          linhaToken, colunaToken));
                return new Token(Token.TipoToken.ERRO, lexema.toString(), linhaToken, colunaToken);
            }
            lexema.append(caractereAtual);
            avancarCaractere();
        }
        if (eof) {
            erros.add(String.format("Erro léxico: string não fechada no fim do arquivo, linha %d", 
                      linhaToken));
            return new Token(Token.TipoToken.ERRO, lexema.toString(), linhaToken, colunaToken);
        }
        
        avancarCaractere();
        
        return new Token(Token.TipoToken.TEXTO_LITERAL, lexema.toString(), linhaToken, colunaToken);
    }
    
    private Token lerOperadorRelacional() throws IOException {
        int linhaToken = linha;
        int colunaToken = coluna;
        char primeiro = caractereAtual;
        avancarCaractere();
        
        if (!eof && caractereAtual == '=') {
            avancarCaractere();
            switch (primeiro) {
                case '>': return new Token(Token.TipoToken.OP_REL_MAIOR_IGUAL, ">=", 
                        linhaToken, colunaToken);
                case '<': return new Token(Token.TipoToken.OP_REL_MENOR_IGUAL, "<=", 
                        linhaToken, colunaToken);
                case '!': return new Token(Token.TipoToken.OP_REL_DIFERENTE, "!=", 
                        linhaToken, colunaToken);
                case '=': return new Token(Token.TipoToken.OP_REL_IGUAL, "==", 
                        linhaToken, colunaToken);
            }
        }
        
        // Operação de 1 caractere
        switch (primeiro) {
            case '>': return new Token(Token.TipoToken.OP_REL_MAIOR, ">", 
                    linhaToken, colunaToken);
            case '<': return new Token(Token.TipoToken.OP_REL_MENOR, "<", 
                    linhaToken, colunaToken);
            case '!': 
                erros.add(String.format("Erro léxico: operador inválido '!' na linha %d, coluna %d", 
                          linhaToken, colunaToken));
                return new Token(Token.TipoToken.ERRO, "!", linhaToken, colunaToken);
        }
        
        return null;
    }
    
    private Token lerOperadorLogico () throws IOException {
        int linhaToken = linha;
        int colunaToken = coluna;
        char primeiro = caractereAtual;
        avancarCaractere();
        
        if (!eof && caractereAtual == primeiro) {
            avancarCaractere();
            String lexema = String.valueOf(primeiro) + primeiro;
            if (primeiro == '&') {
                return new Token(Token.TipoToken.OP_AND, lexema, linhaToken, colunaToken);
            } else { // '|'
                return new Token(Token.TipoToken.OP_OR, lexema, linhaToken, colunaToken);
            }
        } else {
            erros.add(String.format("Erro léxico: operador lógico inválido na linha %d, coluna %d", 
                      linhaToken, colunaToken));
            return new Token(Token.TipoToken.ERRO, String.valueOf(primeiro), linhaToken, colunaToken);
        }
    }
    
    public Token nextToken() throws IOException {
        skipWhiteSpace();
        
        // Verifica comentários
        if (!eof && caractereAtual == '/' ) {
            avancarCaractere();
            if (!eof && caractereAtual == '/') {
                skipCommentLine();
                return nextToken();
            } else {
                // Se for apenas '/', é operador de divisão
                return new Token(Token.TipoToken.OP_DIV, "/", linha, coluna - 1);
            }
        }
        
        if (eof) {
            return new Token(Token.TipoToken.EOF, "EOF", linha, coluna);
        }
        
        int linhaToken = linha;
        int colunaToken = coluna;
        
        // Números
        if (Character.isDigit(caractereAtual)) {
            return lerNumero();
        }
        
        // Identificadores e palavras reservadas
        if (Character.isLetter(caractereAtual) || caractereAtual == '_') {
            return lerIdOuPalavraReservada();
        }
        
        // Strings
        if (caractereAtual == '"') {
            return lerTexto();
        }
        
        // Operadores e delimitadores
        switch (caractereAtual) {
            case '=':
                avancarCaractere();
                if (!eof && caractereAtual == '=') {
                    avancarCaractere();
                    return new Token(Token.TipoToken.OP_REL_IGUAL, "==", linhaToken, colunaToken);
                }
                return new Token(Token.TipoToken.OP_ATRIB, "=", linhaToken, colunaToken);
            case '>': case '<':
                return lerOperadorRelacional();
                
            case '&': case '|':
                return lerOperadorLogico();
                
            case '+':
                avancarCaractere();
                return new Token(Token.TipoToken.OP_SOMA, "+", linhaToken, colunaToken);
                
            case '-':
                avancarCaractere();
                return new Token(Token.TipoToken.OP_SUB, "-", linhaToken, colunaToken);
                
            case '*':
                avancarCaractere();
                return new Token(Token.TipoToken.OP_MULT, "*", linhaToken, colunaToken);
                
            case '/':
                avancarCaractere();
                if (!eof && caractereAtual == '/') {
                    skipCommentLine();
                    return nextToken();
                }
                return new Token(Token.TipoToken.OP_DIV, "/", linhaToken, colunaToken);
                
            case '(':
                avancarCaractere();
                return new Token(Token.TipoToken.ABRE_PAREN, "(", linhaToken, colunaToken);
                
            case ')':
                avancarCaractere();
                return new Token(Token.TipoToken.FECHA_PAREN, ")", linhaToken, colunaToken);
                
            case '{':
                avancarCaractere();
                return new Token(Token.TipoToken.ABRE_CHAVE, "{", linhaToken, colunaToken);
                
            case '}':
                avancarCaractere();
                return new Token(Token.TipoToken.FECHA_CHAVE, "}", linhaToken, colunaToken);
                
            case ';':
                avancarCaractere();
                return new Token(Token.TipoToken.PONTO_VIRGULA, ";", linhaToken, colunaToken);
                
            case '.':
                avancarCaractere();
                return new Token(Token.TipoToken.PONTO_FINAL, ".", linhaToken, colunaToken);
                
            default:
                char erroChar = caractereAtual;
                avancarCaractere();
                erros.add(String.format("Erro léxico: caractere não reconhecido '%c' na linha %d, coluna %d", 
                          erroChar, linhaToken, colunaToken));
                return new Token(Token.TipoToken.ERRO, String.valueOf(erroChar), linhaToken, colunaToken);
        }
    }
    
    public List<Token> analisar() throws IOException {
        Token t;
        do {
            t = nextToken();
            tokens.add(t);
        } while (t.getTipo() != Token.TipoToken.EOF);   
        return tokens;
    }
    
    public void imprimirTokens() {
        System.out.println("\n=== LISTA DE TOKENS ===");
        System.out.println("Total de tokens: " + tokens.size());
        System.out.println("-".repeat(60));
        
        for (Token t : tokens) {
            if (t.getTipo() != Token.TipoToken.EOF) {
                System.out.println(t);
            }
        }
        
        if (!erros.isEmpty()) {
            System.out.println("\n=== ERROS LÉXICOS ===");
            for (String erro : erros) {
                System.out.println(erro);
            }
        }
    }
    
    public List<Token> getTokens() {
        return tokens;
    }
    
    public List<String> getErros() {
        return erros;
    }
    
    public boolean temErros() {
        return !erros.isEmpty();
    }
}
