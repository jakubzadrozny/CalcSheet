package calc;

import java.util.*;

public class Parser extends Lexer {

    final String[] comparativeOps = { "=", "<", ">", "<>", "<=", ">=" };
    final String[] additiveOps = { "|", "+", "-" };
    final String[] multOps = { "&", "*", "/", "%" };
    final String[] unaryOps = { "-", "~" };
    final String[] powerOps = { "^" };

    boolean expectToken (Token.Type type) {
        if(tokens.isEmpty() || tokens.get(0).type != type) return false;
        tokens.remove(0);
        return true;
    }

    boolean expectToken (Token.Type type, String content) {
        if(tokens.isEmpty() || tokens.get(0).type != type || !tokens.get(0).content.equals(content)) return false;
        tokens.remove(0);
        return true;
    }

    String readOperator (String[] list) {
        if(tokens.isEmpty() || tokens.get(0).type != Token.Type.tokOp) return null;

        String operator = tokens.get(0).content;
        if(Arrays.asList(list).contains(operator)) {
            tokens.remove(0);
            return operator;
        }
        else return null;
    }

    Expression buildLockedExpression () {
        if(!expectToken(Token.Type.tokOp, "$")) return null;
        if(tokens.isEmpty()) return null;

        Token fst = tokens.get(0);
        if(fst.type == Token.Type.tokIdent) {
            tokens.remove(0);
            return new Expression(fst.content, true);
        }
        else if(fst.type == Token.Type.tokNum) {
            tokens.remove(0);
            return new Expression(Double.parseDouble(fst.content), true);
        }
        else return null;
    }

    Expression buildSimpleExpression () {
        Expression e = buildLockedExpression();
        if(e != null) return e;

        if(tokens.isEmpty()) return null;
        Token first = tokens.get(0);

        if(first.type == Token.Type.tokNum) {
            tokens.remove(0);
            return new Expression (Double.parseDouble(first.content));
        }

        if(first.type == Token.Type.tokTrue) {
            tokens.remove(0);
            return new Expression (true);
        }

        if(first.type == Token.Type.tokFalse) {
            tokens.remove(0);
            return new Expression (false);
        }

        if(!expectToken(Token.Type.tokOp, "(")) return null;
        Expression expr = buildExpression();
        if(expr == null) return null;
        if(!expectToken(Token.Type.tokOp, ")")) return null;
        return expr;
    }

    Expression buildApplicativeExpression () {
        if(tokens.isEmpty()) return null;
        Token first = tokens.get(0);
        String name = first.content;

        if(first.type != Token.Type.tokIdent) return buildSimpleExpression();
        tokens.remove(0);

        if(expectToken(Token.Type.tokOp, "(")) {
            if(expectToken(Token.Type.tokOp, ")"))
                return new Expression((Expression) null, name);
            else {
                Expression expr = buildExpression();
                if(expr == null) return null;
                if(!expectToken(Token.Type.tokOp, ")")) return null;
                return new Expression(expr, name);
            }
        }
        else {
            Expression expr = buildApplicativeExpression();
            if(expr == null) return new Expression(name);
            return new Expression(expr, name);
        }
    }

    Expression buildUnaryExpression () {
        String operator = readOperator(unaryOps);
        if(operator != null) {
            Expression expr = buildUnaryExpression();
            if(expr == null) return null;
            else return new Expression (Expression.Type.eUnaryOp, expr, operator);
        }

        return buildApplicativeExpression();
    }

    Expression buildPowerExpression () {
        Expression e1 = buildUnaryExpression();

        String operator = readOperator(powerOps);
        if(operator != null) {
            Expression e2 = buildPowerExpression();
            if(e2 == null) return null;
            return new Expression(Expression.Type.eBinaryOp, e1, e2, operator);
        }

        return e1;
    }

    Expression buildMultExpression () {
        Expression e1 = buildPowerExpression();
        if(e1 == null) return null;

        String operator = readOperator(multOps);
        while(operator != null) {
            Expression e2 = buildPowerExpression();
            if(e2 == null) return null;

            e1 = new Expression (Expression.Type.eBinaryOp, e1, e2, operator);
            operator = readOperator(multOps);
        }

        return e1;
    }

    Expression buildAddExpression () {
        Expression e1 = buildMultExpression();
        if(e1 == null) return null;

        String operator = readOperator(additiveOps);
        while(operator != null) {
            Expression e2 = buildMultExpression();
            if(e2 == null) return null;

            e1 = new Expression (Expression.Type.eBinaryOp, e1, e2, operator);
            operator = readOperator(additiveOps);
        }

        return e1;
    }

    Expression buildOpExpression () {
        Expression e1 = buildAddExpression();
        if(e1 == null) return null;

        String operator = readOperator(comparativeOps);
        if(operator != null) {
            Expression e2 = buildAddExpression();
            if(e2 == null) return null;
            else return new Expression (Expression.Type.eBinaryOp, e1, e2, operator);
        }
        else return e1;
    }

    Expression buildRangeExpression () {
        if(tokens.size() < 2) return null;

        boolean fstLocked = false;
        boolean sndLocked = false;
        Token dollar = tokens.get(0);
        Token fst, mid, snd;

        if(dollar.type == Token.Type.tokOp && dollar.content.equals("$")) {
            fstLocked = true;
            if(tokens.size() < 3) return null;
            fst = tokens.get(1);
            mid = tokens.get(2);
        }
        else {
            fst = dollar;
            mid = tokens.get(1);
        }

        if(fst.type != Token.Type.tokIdent || mid.type != Token.Type.tokOp
                || !mid.content.equals(":")) return null;

        if(fstLocked) tokens.remove(0);
        tokens.remove(0);
        tokens.remove(0);

        if(expectToken(Token.Type.tokOp, "$")) sndLocked = true;

        snd = tokens.get(0);
        if(snd.type != Token.Type.tokIdent) return null;
        tokens.remove(0);

        return new Expression(fst.content, snd.content, fstLocked, sndLocked);
    }

    Expression buildLambdaExpression () {
        if(tokens.size() < 2) return null;

        Token fst = tokens.get(0);
        if(fst.type != Token.Type.tokIdent) return null;

        String typ;
        Token nxt = tokens.get(1);
        if(nxt.type != Token.Type.tokOp || !nxt.content.equals("::")) {
            if(nxt.type != Token.Type.tokOp || !nxt.content.equals("->")) return null;
            tokens.remove(0);
            tokens.remove(0);
            typ = "num";
        }
        else {
            tokens.remove(0);
            tokens.remove(0);
            Token typTok = tokens.get(0);
            if(typTok.type != Token.Type.tokIdent) return null;
            tokens.remove(0);
            typ = typTok.content;
            if(!expectToken(Token.Type.tokOp, "->")) return null;
        }

        Expression e = buildExpression();
        if(e == null) return null;

        return new Expression(fst.content, typ, e);
    }

    Expression buildPairExpression () {
        Expression e1 = buildRangeExpression();
        if(e1 == null) e1 = buildLambdaExpression();
        if(e1 == null) e1 = buildOpExpression();
        if(e1 == null) return null;

        if(expectToken(Token.Type.tokOp, ",")) {
            Expression e2 = buildPairExpression();
            if(e2 == null) return null;
            else return new Expression (Expression.Type.ePair, e1, e2);
        }
        else return e1;
    }

    Expression buildIfExpression () {
        if(!expectToken(Token.Type.tokIf)) return null;

        Expression e1 = buildExpression();
        if(e1 == null) return null;

        if(!expectToken(Token.Type.tokThen)) return null;

        Expression e2 = buildExpression();
        if(e2 == null) return null;

        if(!expectToken(Token.Type.tokElse)) return null;

        Expression e3 = buildExpression();
        if(e3 == null) return null;

        return new Expression(Expression.Type.eIf, e1, e2, e3);
    }

    Expression buildExpression () {
        Expression expr = buildIfExpression();
        if(expr != null) return expr;
        else return buildPairExpression();
    }

    Expression parse (String input) throws ParseException {
        this.input = input;
        tokens.clear();
        run();

        Expression expr = buildExpression();

        if(expr == null) throw new ParseException ("No legit expression found");
        else if(!tokens.isEmpty()) throw new ParseException ("Unexpected tokens found");
        else return expr;
    }

}
