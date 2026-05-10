/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sintatico;

/**
 *
 * @author Usuario
 */

import lexico.Token;
import lexico.Token.TipoToken;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int indice;
    private Token tokenAtual;
    private List<String> erros;
    private NoSintatico raiz;

    public Parser(List<Token> tokens) {
        this.tokens = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getTipo() != TipoToken.ERRO) {
                this.tokens.add(t);
            }
        }
        this.indice = 0;
        this.erros = new ArrayList<>();
        if (!this.tokens.isEmpty()) {
            this.tokenAtual = this.tokens.get(0);
        }
    }
    
    private void next() {
        indice++;
        if (indice < tokens.size()) {
            tokenAtual = tokens.get(indice);
        }
    }
    
    // Valida tipo do token e avança
    private boolean validarTipo(TipoToken tipoEsperado) {
        if (tokenAtual.getTipo() == tipoEsperado) {
            next();
            return true;
        }
        return false;
    }
    
    private Token consumir(TipoToken tipoEsperado, String msgErro) {
        if (tokenAtual.getTipo() == tipoEsperado) {
            Token token = tokenAtual;
            next();
            return token;
        } else {
            addErro(msgErro);
            // Skip tokens until we find the expected token or a statement boundary
            while (indice < tokens.size() &&
                   tokenAtual != null &&
                   tokenAtual.getTipo() != tipoEsperado &&
                   tokenAtual.getTipo() != TipoToken.PONTO_FINAL &&
                   !isTokenSincronizacao()) {
                next();
            }

            // If we stopped at the expected token, consume it to recover
            if (tokenAtual != null && tokenAtual.getTipo() == tipoEsperado) {
                next();
            } else if (tokenAtual != null && tokenAtual.getTipo() == TipoToken.PONTO_FINAL
                       && tipoEsperado == TipoToken.PONTO_FINAL) {
                // only consume the statement terminator if the caller expected it
                next();
            }

            return null;
        }
    }
    
    private boolean isInicioComando(TipoToken tipo) {
        return tipo == TipoToken.INTEIRO ||
               tipo == TipoToken.DECIMAL ||
               tipo == TipoToken.TEXTO ||
               tipo == TipoToken.LER ||
               tipo == TipoToken.PRINTAR ||
               tipo == TipoToken.ID ||
               tipo == TipoToken.SE ||
               tipo == TipoToken.ENQUANTO ||
               tipo == TipoToken.PARA;
    }

    private boolean isTokenSincronizacao() {
        TipoToken tipo = tokenAtual.getTipo();
        return tipo == TipoToken.PONTO_FINAL ||
               tipo == TipoToken.PONTO_VIRGULA ||
               tipo == TipoToken.FECHA_PAREN ||
               tipo == TipoToken.ABRE_CHAVE ||
               tipo == TipoToken.FECHA_CHAVE ||
               tipo == TipoToken.INICIO ||
               tipo == TipoToken.FIM ||
               tipo == TipoToken.SENAO ||
               tipo == TipoToken.SENAO_SE ||
               tipo == TipoToken.EOF ||
               isInicioComando(tipo);
    }

    private void sincronizarComando() {
        // Avanca pelo menos um token para evitar loop infinito.
        if (indice < tokens.size()) {
            next();
        }
        while (indice < tokens.size() && tokenAtual != null &&
               !isInicioComando(tokenAtual.getTipo()) &&
               !isFimBloco()) {
            if (tokenAtual.getTipo() == TipoToken.PONTO_FINAL) {
                next();
                break;
            }
            next();
        }
    }

    private void addErro(String msg) {
        String infoToken = tokenAtual != null ?
                String.format(" {encontrado: '%s' linha %d, coluna %d}", 
                        tokenAtual.getLexema(), tokenAtual.getLinha(), tokenAtual.getColuna())
                : "";
        erros.add("Erro sintatico: " + msg + infoToken);
    }
    
    // Análise sintática
    public NoSintatico analisar() {
        raiz = S();
        
        if (indice < tokens.size() - 1) {
            addErro("Tokens extras apos o fim do programa.");
        }
        return raiz;
    }
    
    // Regras de gramática 
    // S --> 'inicio' bloco 'fim'
    private NoSintatico S() {        
        NoSintatico no = new NoSintatico("S");
        
        if (tokenAtual.getTipo() == TipoToken.INICIO) {
            no.adicionarFilho(new NoSintatico("INICIO", "inicio"));
            next();
            
            NoSintatico noBloco = bloco();
            if (noBloco != null) {
                no.adicionarFilho(noBloco);
            }
            
            if (tokenAtual.getTipo() == TipoToken.FIM) {
                no.adicionarFilho(new NoSintatico("FIM", "fim"));
                next();
            } else {
                addErro("Esperado 'fim' para fechar o programa");
            }
        } else {
            addErro("Programa deve começar com 'inicio'");
        }
        
        return no;
    }
    
    // bloco -> cmd bloco | ε
    private NoSintatico bloco() {
        NoSintatico no = new NoSintatico("bloco");
        boolean temComando = false;

        while (!isFimBloco()) {
            int indiceAntes = indice;
            NoSintatico noCmd = cmd();
            if (noCmd != null) {
                no.adicionarFilho(noCmd);
                temComando = true;
            }

            if (indice == indiceAntes) {
                next();
            }
        }

        if (!temComando) {
            no.adicionarFilho(new NoSintatico("ε"));
        }

        return no;
    }
    
    private boolean isFimBloco() {
        return tokenAtual.getTipo() == TipoToken.FIM ||
                tokenAtual.getTipo() == TipoToken.FECHA_CHAVE ||
                tokenAtual.getTipo() == TipoToken.SENAO ||
                tokenAtual.getTipo() == TipoToken.SENAO_SE ||
                tokenAtual.getTipo() == TipoToken.EOF;
    }
    
    // cmd  declaracao | leitura | escrita | atribuicao | se | enquanto | para
    private NoSintatico cmd() {
        NoSintatico no = new NoSintatico("cmd");
        
        switch (tokenAtual.getTipo()) {
            case INTEIRO:
            case DECIMAL:
            case TEXTO:
                no.adicionarFilho(declaracao());
                break;
                
            case LER:
                no.adicionarFilho(leitura());
                break;
                
            case PRINTAR:
                no.adicionarFilho(escrita());
                break;
                
            case ID:
                no.adicionarFilho(atribuicao());
                break;
                
            case SE:
                no.adicionarFilho(se());
                break;
                
            case ENQUANTO:
                no.adicionarFilho(enquanto());
                break;
                
            case PARA:
                no.adicionarFilho(para());
                break;
                
            default:
                addErro("Comando inesperado: " + tokenAtual.getLexema());
                sincronizarComando();
                return null;
        }
        return no;
    }
    
    // declaracao  tipo ID ‘.’
    private NoSintatico declaracao() {
        NoSintatico no = new NoSintatico("declaracao");
        
        NoSintatico noTipo = tipo();
        if (noTipo != null) {
            no.adicionarFilho(noTipo);
        }
        
        Token id = consumir(TipoToken.ID, "Esperado um identificador após o tipo");
        if (id != null) {
            no.adicionarFilho(new NoSintatico("ID", id.getLexema(), id.getLinha(), id.getColuna()));
        }
        
        consumir(TipoToken.PONTO_FINAL, "Esperado '.' após declaração de variável");
        
        return no;
    }
    
    // tipo  ‘inteiro’ | ‘decimal’ | ‘texto’
    private NoSintatico tipo() {
        NoSintatico no = new NoSintatico("tipo");
        
        if (validarTipo(TipoToken.INTEIRO)) {
            no.adicionarFilho(new NoSintatico("INTEIRO", "inteiro"));
        } else if (validarTipo(TipoToken.DECIMAL)) {
            no.adicionarFilho(new NoSintatico("DECIMAL", "decimal"));
        } else if (validarTipo(TipoToken.TEXTO)) {
            no.adicionarFilho(new NoSintatico("TEXTO", "texto"));
        } else {
            addErro("Tipo de variavel esperado (inteiro, decimal ou texto)");
            return null;
        }
     
        return no;
    }
    
    // leitura  ‘ler’ ‘(’ ID ‘)’ ‘.’
    private NoSintatico leitura() {
        NoSintatico no = new NoSintatico("leitura");
        
        if (validarTipo(TipoToken.LER)) {
            no.adicionarFilho(new NoSintatico("LER", "ler"));
            
            consumir(TipoToken.ABRE_PAREN, "Esperado '(' após 'ler'");
            
            Token id = consumir(TipoToken.ID, "Esperado um identificador em 'ler()'");
            if (id != null) {
                no.adicionarFilho(new NoSintatico("ID", id.getLexema(), id.getLinha(), id.getColuna()));
            }
            
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' após identificador em 'ler()'");
            consumir(TipoToken.PONTO_FINAL, "Esperado '.' após 'ler()'");
        }
        
        return no;
    }
    
    // escrita  ‘printar’ ‘(’ conteudo ‘)’ ‘.’
    private NoSintatico escrita() {
        NoSintatico no = new NoSintatico("escrita");
        
        if (validarTipo(TipoToken.PRINTAR)) {
            no.adicionarFilho(new NoSintatico("PRINTAR", "printar"));
            
            consumir(TipoToken.ABRE_PAREN, "Esperado '(' após 'printar'");
            
            NoSintatico noConteudo = conteudo();
            if (noConteudo != null) {
                no.adicionarFilho(noConteudo);
            }
            
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' após conteúdo em 'printar()'");
            consumir(TipoToken.PONTO_FINAL, "Esperado '.' após 'printar()'");
        }
        
        return no;
    }
    
    // conteudo  TEXTO | ID
    private NoSintatico conteudo() {
        NoSintatico no = new NoSintatico("conteudo");
        
        if (tokenAtual.getTipo() == TipoToken.TEXTO_LITERAL) {
            Token texto = tokenAtual;
            no.adicionarFilho(new NoSintatico("TEXTO", texto.getLexema(), texto.getLinha(), texto.getColuna()));
            next();
        } else if (tokenAtual.getTipo() == TipoToken.ID) {
            Token id = tokenAtual;
            no.adicionarFilho(new NoSintatico("ID", id.getLexema(), id.getLinha(), id.getColuna()));
            next();
        } else {
            addErro("Esperado um texto ou identificador em 'printar()'");
        }
        
        return no;
    }
    
    //atribuicao  ID ‘=’ expr ‘.’
    private NoSintatico atribuicao() {
        NoSintatico no = new NoSintatico("atribuicao");
        
        Token id = consumir(TipoToken.ID, "Esperado um identificador para atribuição");
        if (id != null) {
            no.adicionarFilho(new NoSintatico("ID", id.getLexema(), id.getLinha(), id.getColuna()));
        }
        
        consumir(TipoToken.OP_ATRIB, "Esperado '=' para atribuição");
        
        NoSintatico noExpr = expr();
        if (noExpr != null) {
            no.adicionarFilho(noExpr);
        }
        
        consumir(TipoToken.PONTO_FINAL, "Esperado '.' apos atribuicao");
        
        return no;
    }
    
    // se  ‘se’ ‘(‘ expr_logica ‘)’ ‘{’ bloco ‘}’ sns
    private NoSintatico se(){
        NoSintatico no = new NoSintatico("se");
        
        if (validarTipo(TipoToken.SE)) {
            no.adicionarFilho(new NoSintatico("SE", "se"));
            consumir(TipoToken.ABRE_PAREN, "Esperado '(' apos se");
            
            NoSintatico noCondicao = expr_logica();
            if (noCondicao != null) {
                no.adicionarFilho(noCondicao);
            }
            
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' após condição do 'se'");
            consumir(TipoToken.ABRE_CHAVE, "Esperado '{' para abrir bloco do 'se'");
            
            NoSintatico noBloco = bloco();
            if (noBloco != null) {
                no.adicionarFilho(noBloco);
            }
            
            consumir(TipoToken.FECHA_CHAVE, "Esperado '}' para fechar bloco do 'se'");
            
            NoSintatico noSns = sns();
            if (noSns != null) {
                no.adicionarFilho(noSns);
            }
        }
        return no;
    }
    
    // sns  ‘senaose’ ‘(‘ expr_logica ‘)’  ‘{’ bloco ‘}’  sns | 
    // ‘senao’ ‘{’ bloco ‘}’ | 
    // ε
    private NoSintatico sns() {
        NoSintatico no = new NoSintatico("sns");
        
        if (validarTipo(TipoToken.SENAO_SE)) {
            no.adicionarFilho(new NoSintatico("SENAO_SE", "senaose"));
            
            consumir(TipoToken.ABRE_PAREN, "Esperado '(' após 'senaose'");
            
            NoSintatico noCondicao = expr_logica();
            if (noCondicao != null) {
                no.adicionarFilho(noCondicao);
            }
            
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' apps condicao do 'senaose'");
            consumir(TipoToken.ABRE_CHAVE, "Esperado '{' para abrir bloco do 'senaose'");
            
            NoSintatico noBloco = bloco();
            if (noBloco != null) {
                no.adicionarFilho(noBloco);
            }
            
            consumir(TipoToken.FECHA_CHAVE, "Esperado '}' para fechar bloco do 'senaose'");
            
            NoSintatico noSns = sns();
            if (noSns != null) {
                no.adicionarFilho(noSns);
            }
        } else if (validarTipo(TipoToken.SENAO)) {
            no.adicionarFilho(new NoSintatico("SENAO", "senao"));
            
            consumir(TipoToken.ABRE_CHAVE, "Esperado '{' para abrir bloco do 'senao'");
            
            NoSintatico noBloco = bloco();
            if (noBloco != null) {
                no.adicionarFilho(noBloco);
            }
            
            consumir(TipoToken.FECHA_CHAVE, "Esperado '}' para fechar bloco do 'senao'");
        } else {
            no.adicionarFilho(new NoSintatico("ε"));
        }
        
        return no;
    }
    
    // enquanto  ‘enquanto’ ‘(‘ expr_logica ‘)’ ‘{’ bloco ‘}’
    private NoSintatico enquanto() {
        NoSintatico no = new NoSintatico("enquanto");
        
        if (validarTipo(TipoToken.ENQUANTO)) {
            no.adicionarFilho(new NoSintatico("ENQUANTO", "enquanto"));
            
            consumir(TipoToken.ABRE_PAREN, "Esperado '(' apos 'enquanto'");
            
            NoSintatico noCondicao = expr_logica();
            if (noCondicao != null) {
                no.adicionarFilho(noCondicao);
            }
            
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' após condição do 'enquanto'");
            consumir(TipoToken.ABRE_CHAVE, "Esperado '{' para abrir bloco do 'enquanto'");
            
            NoSintatico noBloco = bloco();
            if (noBloco != null) {
                no.adicionarFilho(noBloco);   
            }
            
            consumir(TipoToken.FECHA_CHAVE, "Esperado '}' para fechar bloco do 'enquanto'");
        }
        
        return no;
    }
    
    // para  ‘para’ 
    //‘(’ (declaracao_para | atribuicao_interna) ‘;’ expr_logica ‘;’ atribuicao_interna ‘)’ 
    //‘{’ bloco ‘}’
    private NoSintatico para() {
        NoSintatico no = new NoSintatico("para");
        
        if (validarTipo(TipoToken.PARA)) {
            no.adicionarFilho(new NoSintatico("PARA", "para"));
            
            consumir(TipoToken.ABRE_PAREN, "Esperado '(' após 'para'");
            
            // Declaração ou atribuição
            if (tokenAtual.getTipo() == TipoToken.INTEIRO ||
                tokenAtual.getTipo() == TipoToken.DECIMAL ||
                tokenAtual.getTipo() == TipoToken.TEXTO) {
                no.adicionarFilho(declaracao_para());
            } else {
                no.adicionarFilho(atribuicao_interna());
            }
            
            consumir(TipoToken.PONTO_VIRGULA, "Esperado ';' apos inicializacao do 'para'");
            
            NoSintatico noCondicao = expr_logica();
            if (noCondicao != null) {
                no.adicionarFilho(noCondicao);
            }
            
            consumir(TipoToken.PONTO_VIRGULA, "Esperado ';' apos condicao do 'para'");
            
            no.adicionarFilho(atribuicao_interna());
            
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' para fechar cabecalho do 'para'");
            consumir(TipoToken.ABRE_CHAVE, "Esperado '{' para abrir bloco do 'para'");
            
            NoSintatico noBloco = bloco();
            if (noBloco != null) {
                no.adicionarFilho(noBloco);
            }
            
            consumir(TipoToken.FECHA_CHAVE, "Esperado '}' para fechar bloco do 'para'");

        }
        
        return no;
    }
    
    // declaracao_para  tipo ID ‘= ’ expr
    private NoSintatico declaracao_para() {
        NoSintatico no = new NoSintatico("declaracao_para");
        
        NoSintatico noTipo = tipo();
        if (noTipo != null) {
            no.adicionarFilho(noTipo);
        }
        
        Token id = consumir(TipoToken.ID, "Esperado um identificador no 'para'");
        if (id != null) {
            no.adicionarFilho(new NoSintatico("ID", id.getLexema(), id.getLinha(), id.getColuna()));
        }
        
        consumir(TipoToken.OP_ATRIB, "Esperado '=' na inicializacao do 'para'");
        
        NoSintatico noExpr = expr();
        if (noExpr != null) {
            no.adicionarFilho(noExpr);
        }
        
        return no;
    }
    
    // atribuição_interna  ID ‘=’ expr
    private NoSintatico atribuicao_interna() {
        NoSintatico no = new NoSintatico("atribuicao_interna");
        
        Token id = consumir(TipoToken.ID, "Esperado um identificador no 'para'");
        if (id != null) {
            no.adicionarFilho(new NoSintatico("ID", id.getLexema(), id.getLinha(), id.getColuna()));
        }
        
        consumir(TipoToken.OP_ATRIB, "Esperado '=' na atribuição do 'para'");
        
        NoSintatico noExpr = expr();
        if (noExpr != null) {
            no.adicionarFilho(noExpr);
        }
        
        return no;
    }
    
    /* expr_logica  expr_logica '&&' expr_logica | 
    expr_logica '||' expr_logica | 
    expr op_rel expr | 
    ‘(’ expr_logica ‘)’ */
    private NoSintatico expr_logica(){
        return expr_logica_or();
    }
    
    private NoSintatico expr_logica_or() {
        NoSintatico no = expr_logica_and();
        
        while (tokenAtual.getTipo() == TipoToken.OP_OR) {
            NoSintatico noOp = new NoSintatico("||");
            next();
            
            NoSintatico noDir = expr_logica_and();
            if (noDir != null) {
                noOp.adicionarFilho(no);
                noOp.adicionarFilho(noDir);
                no = noOp;
            }
        }
        
        return no;
    }
    
    private NoSintatico expr_logica_and() {
        NoSintatico no = expr_relacional();
        
        while (tokenAtual.getTipo() == TipoToken.OP_AND) {
            NoSintatico noOp = new NoSintatico("&&");
            next();
            
            NoSintatico noDir = expr_relacional();
            if (noDir != null) {
                noOp.adicionarFilho(no);
                noOp.adicionarFilho(noDir);
                no = noOp;
            }
        }
        
        return no;
    }
    
    private NoSintatico expr_relacional() {
        if (tokenAtual.getTipo() == TipoToken.ABRE_PAREN) {
            next();
            NoSintatico no = expr_logica();
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' para fechar expressão");
            return no;
        }
        
        NoSintatico noEsq = expr();
        if (isOperadorRelacional()) {
            String op = tokenAtual.getLexema();
            next();
            
            NoSintatico noDir = expr();
            
            NoSintatico noOp = new NoSintatico("op_rel", op);
            noOp.adicionarFilho(noEsq);
            noOp.adicionarFilho(noDir);
            return noOp;
        }
        
        return noEsq;
    }
    
    private boolean isOperadorRelacional() {
        return tokenAtual.getTipo() == TipoToken.OP_REL_MAIOR ||
               tokenAtual.getTipo() == TipoToken.OP_REL_MENOR ||
               tokenAtual.getTipo() == TipoToken.OP_REL_MAIOR_IGUAL ||
               tokenAtual.getTipo() == TipoToken.OP_REL_MENOR_IGUAL ||
               tokenAtual.getTipo() == TipoToken.OP_REL_DIFERENTE ||
               tokenAtual.getTipo() == TipoToken.OP_REL_IGUAL;
    }
    
    // expr  termo resto_expr
    private NoSintatico expr() {
        NoSintatico no = termo();
        return resto_expr(no);
    }
    
    // resto_expr  ‘+’ termo resto_expr | ‘-’ termo resto_expr | ε
    private NoSintatico resto_expr(NoSintatico esquerdo) {
        if (tokenAtual.getTipo() == TipoToken.OP_SOMA) {
            next();
            NoSintatico noDir = termo();
            NoSintatico noOp = new NoSintatico("+");
            noOp.adicionarFilho(esquerdo);
            noOp.adicionarFilho(noDir);
            return resto_expr(noOp);
            
        } else if (tokenAtual.getTipo() == TipoToken.OP_SUB) {
            next();
            NoSintatico noDir = termo();
            NoSintatico noOp = new NoSintatico("-");
            noOp.adicionarFilho(esquerdo);
            noOp.adicionarFilho(noDir);
            return resto_expr(noOp);   
        } else {
            return esquerdo;
        }
    }
    
    // termo  fator resto_termo
    private NoSintatico termo() {
        NoSintatico no = fator();
        return resto_termo(no);
    }
    
    // resto_termo  ‘*’ fator resto_termo | ‘/’ fator resto_termo | ε
    private NoSintatico resto_termo(NoSintatico esquerdo) {
        if (tokenAtual.getTipo() == TipoToken.OP_MULT) {
            next();
            NoSintatico noDir = fator();
            NoSintatico noOp = new NoSintatico("*");
            noOp.adicionarFilho(esquerdo);
            noOp.adicionarFilho(noDir);
            return resto_termo(noOp);
            
        } else if (tokenAtual.getTipo() == TipoToken.OP_DIV) {
            next();
            NoSintatico noDir = fator();
            NoSintatico noOp = new NoSintatico("/");
            noOp.adicionarFilho(esquerdo);
            noOp.adicionarFilho(noDir);
            return resto_termo(noOp);
        
        } else {
            return esquerdo;
        }
    }
    
    // fator  NUM | ID | ‘(’ expr ‘)’ | TEXTO
    private NoSintatico fator() {
         NoSintatico no = new NoSintatico("fator");
        
        if (tokenAtual.getTipo() == TipoToken.NUM_INT) {
            Token num = tokenAtual;
            no.adicionarFilho(new NoSintatico("NUM_INT", num.getLexema(), num.getLinha(), num.getColuna()));
            next();
            
        } else if (tokenAtual.getTipo() == TipoToken.NUM_DECIMAL) {
            Token num = tokenAtual;
            no.adicionarFilho(new NoSintatico("NUM_DECIMAL", num.getLexema(), num.getLinha(), num.getColuna()));
            next();
            
        } else if (tokenAtual.getTipo() == TipoToken.ID) {
            Token id = tokenAtual;
            no.adicionarFilho(new NoSintatico("ID", id.getLexema(), id.getLinha(), id.getColuna()));
            next();
            
        } else if (tokenAtual.getTipo() == TipoToken.TEXTO_LITERAL) {
            Token texto = tokenAtual;
            no.adicionarFilho(new NoSintatico("TEXTO", texto.getLexema(), texto.getLinha(), texto.getColuna()));
            next();
            
        } else if (validarTipo(TipoToken.ABRE_PAREN)) {
            NoSintatico noExpr = expr();
            if (noExpr != null) {
                no.adicionarFilho(noExpr);
            }
            consumir(TipoToken.FECHA_PAREN, "Esperado ')' para fechar expressao");
            
        } else {
            addErro("Esperado numero, identificador, texto ou '('");
            // try to skip to the end of the statement to avoid cascading errors
            while (indice < tokens.size() && tokenAtual != null &&
                   tokenAtual.getTipo() != TipoToken.PONTO_FINAL &&
                   !isTokenSincronizacao()) {
                next();
            }
            return null;
        }
        
        return no;
    }
    
    // Métodos de acesso

    public List<String> getErros() {
        return erros;
    }
    
    public boolean temErros() {
        return !erros.isEmpty();
    }   
    
    public void imprimirArvore() {
        System.out.println("\n=== ÁRVORE DE DERIVAÇÃO ===");
        if (raiz != null) {
            raiz.imprimirArvore("", true);
        }
    }
    
    public void imprimirErros() {
        if (!erros.isEmpty()) {
            System.out.println("\n=== ERROS SINTATICOS ===");
            for (String erro : erros) {
                System.out.println(erro);
            }
        }
    }
        
}

    