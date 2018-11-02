package calc;

import java.util.*;

public class Evaluator extends Parser {

    private Map<String, Field> env;
    private Functions fs = new Functions();

    private static String[] arithmeticOps = { "+", "-", "*", "/", "%", "^" };

    public Evaluator (Map<String, Field>env) { this.env = env; }

    public class TypecheckingException extends Exception {
        public TypecheckingException () { }
        public TypecheckingException (String message) { super(message); }
        public TypecheckingException (Throwable cause) { super(cause); }
        public TypecheckingException (String message, Throwable cause) { super(message, cause); }
    }

    public class RuntimeException extends Exception {
        public RuntimeException () { }
        public RuntimeException (String message) { super(message); }
        public RuntimeException (Throwable cause) { super(cause); }
        public RuntimeException (String message, Throwable cause) { super(message, cause); }
    }

    void checkForRange (String from, String to, Field.Type t) throws TypecheckingException {
        int startH = from.charAt(0) - 64;
        int endH = to.charAt(0) - 64;
        int startV = Integer.parseInt(from.substring(1));
        int endV = Integer.parseInt(to.substring(1));

        for (int i = startH; i <= endH; i++) {
            for (int j = startV; j <= endV; j++) {
                String name = Controller.GenerateName(i, j);
                Field f = env.get(name);
                if (f == null || f.type != t)
                    throw new TypecheckingException("variable from range not set");
            }
        }
    }

    Field appInferType (Expression e) throws TypecheckingException {
        if(e.name.equals("apply")) {
            Field t = inferType(e.child1);
            if(t.type != Field.Type.tPair || t.f1.type != Field.Type.tRange ||
                    t.f2.type != Field.Type.tLambda) throw new TypecheckingException("invalid lambda application");
            checkForRange(t.f1.getFrom(), t.f1.getTo(), t.f2.argType);
            return new Field (t.f2.resType);
        }

        if(e.child1 == null) return fs.ValType.get(e.name);

        Field t1 = inferType (e.child1);
        Field t2 = fs.ArgType.get(e.name);
        if(t2 == null)
            throw new TypecheckingException ("no such function");
        if(t1.type != t2.type)
            throw new TypecheckingException ("invalid application");
        return fs.ValType.get(e.name);
    }

    Field binaryInferType (Expression e) throws TypecheckingException {
        Field t1 = inferType (e.child1);
        Field t2 = inferType (e.child2);
        if(Arrays.asList(comparativeOps).contains(e.operator)) {
            if(t1.type != Field.Type.tNum || t2.type != Field.Type.tNum)
                throw new TypecheckingException ("Comparison only possible on integers");
            else return new Field (Field.Type.tBool);
        }
        else if(Arrays.asList(arithmeticOps).contains(e.operator)) {
            if(t1.type != Field.Type.tNum || t2.type != Field.Type.tNum)
                throw new TypecheckingException ("Arithmetic only possible on integers");
            else return new Field (Field.Type.tNum);
        }
        else {
            if(t1.type != Field.Type.tBool || t2.type != Field.Type.tBool)
                throw new TypecheckingException ("Boolean operations only possible on booleans");
            else return new Field (Field.Type.tBool);
        }
    }

    Field unaryInferType (Expression e) throws TypecheckingException {
        Field t1 = inferType (e.child1);
        if(e.operator.equals("~")) {
            if(t1.type != Field.Type.tBool)
                throw new TypecheckingException ("Not only applicable to boolean type");
            else return new Field (Field.Type.tBool);
        }
        else {
            if(t1.type != Field.Type.tNum)
                throw new TypecheckingException ("Minus only applicable to integer type");
            else return new Field (Field.Type.tNum);
        }
    }

    Field inferType (Expression e) throws TypecheckingException {
        Field curr = new Field ();

        switch (e.type) {
            case eNumber:
                curr.type = Field.Type.tNum;
                break;

            case eBoolean:
                curr.type = Field.Type.tBool;
                break;

            case eRange:
                curr = new Field (e.from, e.to);
                break;

            case ePair:
                Field t4 = inferType (e.child1);
                Field t5 = inferType (e.child2);
                curr = new Field (t4, t5);
                break;

            case eUnaryOp:
                curr = unaryInferType (e);
                break;

            case eBinaryOp:
                curr = binaryInferType (e);
                break;

            case eVariable:
                curr = fs.Consts.get(e.name);
                if(curr == null) curr = env.get(e.name);
                if(curr == null)
                    throw new TypecheckingException ("Undefined variable found");
                break;

            case eIf:
                Field t1 = inferType (e.child1);
                if(t1.type != Field.Type.tBool)
                    throw new TypecheckingException ("Non-bool expression in if statement found");
                Field t2 = inferType (e.child2);
                Field t3 = inferType (e.child3);
                if(t2.type != t3.type)
                    throw new TypecheckingException ("Both options of if statement should be of same type");
                curr.type = t2.type;
                break;

            case eApp:
                curr = appInferType (e);
                break;

            case eLambda:
                env.put(e.argName, new Field(e.argType));
                Field t6 = inferType (e.child1);
                env.remove(e.argName);
                curr = new Field (e.argType, t6.type);
        }

        return curr;
    }

    Field countUnary (Expression e) throws RuntimeException {
        Field v = count (e.child1);
        if(e.operator.equals("~"))
            return new Field (!v.getBool());
        else
            return new Field (-v.getNum());
    }

    Field countBinary (Expression e) throws RuntimeException {
        Field v1 = count (e.child1);
        Field v2 = count (e.child2);
        switch(e.operator) {
            case "=":
                return new Field (v1.getNum() == v2.getNum());
            case "<":
                return new Field (v1.getNum() < v2.getNum());
            case ">":
                return new Field (v1.getNum() > v2.getNum());
            case "<>":
                return new Field (v1.getNum() != v2.getNum());
            case "<=":
                return new Field (v1.getNum() <= v2.getNum());
            case ">=":
                return new Field (v1.getNum() >= v2.getNum());

            case "+":
                return new Field (v1.getNum() + v2.getNum());
            case "-":
                return new Field (v1.getNum() - v2.getNum());
            case "*":
                return new Field (v1.getNum() * v2.getNum());
            case "^":
                return new Field (Math.pow(v1.getNum(), v2.getNum()));

            case "/":
                if(v2.getNum() == 0)
                    throw new RuntimeException ("Division by zero");
                else
                    return new Field (v1.getNum() / v2.getNum());
            case "%":
                if(v2.getNum() == 0)
                    throw new RuntimeException ("Division by zero");
                else
                    return new Field (v1.getNum() % v2.getNum());

            case "&":
                return new Field (v1.getBool() & v2.getBool());
            case "|":
                return new Field (v1.getBool() | v2.getBool());
        }
        return null;
    }

    Field countApply (Expression e) throws RuntimeException {
        Field v = count (e.child1);
        String from = v.f1.getFrom();
        String to = v.f1.getTo();
        Expression body = v.f2.expr;
        String arg = v.f2.argName;
        Double res = 0.0;

        int startH = from.charAt(0) - 64;
        int endH = to.charAt(0) - 64;
        int startV = Integer.parseInt(from.substring(1));
        int endV = Integer.parseInt(to.substring(1));

        for(int i = startH; i <= endH; i++) {
            for(int j = startV; j <= endV; j++) {
                String name = Controller.GenerateName(i, j);
                env.put(arg, env.get(name));
                Field v1 = count (body);
                env.remove(arg);
                res += v1.getNum();
            }
        }

        return new Field (res);
    }

    Field countApp (Expression e) throws RuntimeException {
        if(e.name.equals("apply")) return countApply(e);

        Field v2 = count (e.child1);
        Functions.Wrapper f = fs.Body.get(e.name);
        return new Field (f.apply(v2.getNum()));
    }

    Field count (Expression e) throws RuntimeException {
        Field curr = new Field ();

        switch (e.type) {
            case eNumber:
                curr.setNum(e.getNum());
                break;

            case eBoolean:
                curr.setBool(e.getBool());
                break;

            case eRange:
                curr = new Field (e.from, e.to);
                break;

            case ePair:
                Field v4 = count (e.child1);
                Field v5 = count (e.child2);
                return new Field (v4, v5);

            case eUnaryOp:
                curr = countUnary (e);
                break;

            case eBinaryOp:
                curr = countBinary (e);
                break;

            case eVariable:
                curr = fs.Consts.get(e.name);
                if(curr == null) curr = env.get(e.name);
                break;

            case eIf:
                Field v1 = count (e.child1);
                if(v1.getBool())
                    return count (e.child2);
                else
                    return count (e.child3);

            case eApp:
                curr = countApp (e);
                break;

            case eLambda:
                return new Field (e.argName, e.child1);
        }

        return curr;
    }

    public Set<String> getDependencies (String input) {
        Set<String> res = new HashSet<>();
        try {
            Expression e = parse(input);
            res.addAll(e.getDependencies());
        } catch (Exception e) { }
        return res;
    }

    public Field eval (String input) throws Exception {
        Expression e = parse (input);
        Field t = inferType (e);
        Field v = count (e);
        v.setType(t);
        return v;
    }

}
