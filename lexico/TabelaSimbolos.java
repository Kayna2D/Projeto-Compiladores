/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package lexico;

import java.util.HashMap;
import java.util.Map;

public class TabelaSimbolos {
    private static final Map<String, Token.TipoToken> palavrasReservadas;
    
    static {
        palavrasReservadas = new HashMap<>();
        palavrasReservadas.put("inicio", Token.TipoToken.INICIO);
        palavrasReservadas.put("fim", Token.TipoToken.FIM);
        palavrasReservadas.put("inteiro", Token.TipoToken.INTEIRO);
        palavrasReservadas.put("decimal", Token.TipoToken.DECIMAL);
        palavrasReservadas.put("texto", Token.TipoToken.TEXTO);
        palavrasReservadas.put("ler", Token.TipoToken.LER);
        palavrasReservadas.put("printar", Token.TipoToken.PRINTAR);
        palavrasReservadas.put("se", Token.TipoToken.SE);
        palavrasReservadas.put("senao", Token.TipoToken.SENAO);
        palavrasReservadas.put("senaose", Token.TipoToken.SENAO_SE);
        palavrasReservadas.put("enquanto", Token.TipoToken.ENQUANTO);
        palavrasReservadas.put("para", Token.TipoToken.PARA);
    }

    public static Token.TipoToken getToken (String lexema) {
        Token.TipoToken tipo = palavrasReservadas.get(lexema.toLowerCase());
        return tipo != null ? tipo : Token.TipoToken.ID;
    }
    
    
}
