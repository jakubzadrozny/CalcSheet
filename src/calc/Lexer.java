package calc;

import java.util.*;

public class Lexer {

    public class ParseException extends Exception {
        public ParseException () { }
        public ParseException (String message) { super(message); }
        public ParseException (Throwable cause) { super(cause); }
        public ParseException (String message, Throwable cause) { super(message, cause); }
    }

    String input;
    List<Token> tokens;

    private String[] singleOperators = { "(", ")", "=", "<", ">", "&", "|", "+", "-", "^", "*", "/", "%", "~", ":", ",", "$" };
    private String[] doubleOperators = { "<>", "<=", ">=", "->", "::" };

    public Lexer () {
        tokens = new LinkedList<Token>();
    }

    boolean isSingleOperator (String operator) {
        return Arrays.asList(singleOperators).contains(operator);
    }

    boolean isDoubleOperator (String operator) {
        return Arrays.asList(doubleOperators).contains(operator);
    }

    boolean readNumber () {
        String number = "";
        boolean dotRead = false;
        while(!input.isEmpty()) {
            char c = input.charAt(0);
            if(Character.isDigit(c)) { }
            else if(!dotRead && c == '.') dotRead = true;
            else break;
            number += c;
            input = input.substring(1);
        }

        if(!Objects.equals(number, "")) {
            tokens.add(new Token(Token.Type.tokNum, number));
            return true;
        }

        return false;
    }

    boolean readKeyword () {
        if(input.startsWith("true")) {
            input = input.replaceFirst("true", "");
            tokens.add(new Token(Token.Type.tokTrue, null));
            return true;
        }

        if(input.startsWith("false")) {
            input = input.replaceFirst("false", "");
            tokens.add(new Token(Token.Type.tokFalse, null));
            return true;
        }

        if(input.startsWith("if")) {
            input = input.replaceFirst("if", "");
            tokens.add(new Token(Token.Type.tokIf, null));
            return true;
        }

        if(input.startsWith("then")) {
            input = input.replaceFirst("then", "");
            tokens.add(new Token(Token.Type.tokThen, null));
            return true;
        }

        if(input.startsWith("else")) {
            input = input.replaceFirst("else", "");
            tokens.add(new Token(Token.Type.tokElse, null));
            return true;
        }

        return false;
    }

    boolean readOperator () {

        String operator;

        if(input.length() >= 2) {
            operator = input.substring(0, 2);
            if(isDoubleOperator(operator)) {
                tokens.add(new Token(Token.Type.tokOp, operator));
                input = input.substring(2);
                return true;
            }
        }

        if(input.isEmpty()) return false;

        operator = input.substring(0, 1);
        if(isSingleOperator(operator)) {
            tokens.add(new Token(Token.Type.tokOp, operator));
            input = input.substring(1);
            return true;
        }

        return false;
    }

    boolean readIdentifier () {
        if(input.isEmpty() || !Character.isJavaIdentifierStart(input.charAt(0))) return false;

        String identifier = "";
        while(!input.isEmpty() && Character.isJavaIdentifierPart(input.charAt(0))) {
            identifier += input.charAt(0);
            input = input.substring(1);
        }

        if(!Objects.equals(identifier, "")) {
            tokens.add(new Token(Token.Type.tokIdent, identifier));
            return true;
        }

        return false;
    }

    void run () throws ParseException {
        while(!input.isEmpty()) {

            if(Character.isWhitespace(input.charAt(0))) {
                input = input.substring(1);
                continue;
            }

            boolean succ = readOperator();
            if(succ) continue;

            succ = readNumber();
            if(succ) continue;

            succ = readKeyword();
            if(succ) continue;

            succ = readIdentifier();
            if(succ) continue;

            throw new ParseException("Unexpected character at input");
        }
    }

}
