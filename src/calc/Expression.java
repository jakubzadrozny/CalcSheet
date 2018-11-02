package calc;

import java.util.ArrayList;
import java.util.List;

public class Expression {

    public enum Type {
        eNumber,
        eBoolean,
        eBinaryOp,
        eUnaryOp,
        eVariable,
        eIf,
        eApp,
        eRange,
        ePair,
        eLambda
    }

    public Type type;

    public boolean locked;
    public boolean sndLocked;
    public double numVal;
    public boolean bVal;
    public String name;
    public String operator;
    public String argName;
    public String from;
    public String to;

    public Field.Type argType;

    public Expression child1;
    public Expression child2;
    public Expression child3;

    public Expression(Type t, Expression e, String op) {
        type = t;
        child1 = e;
        operator = op;
    }

    public Expression(Type t, Expression e1, Expression e2) {
        type = t;
        child1 = e1;
        child2 = e2;
    }

    public Expression(Type t, Expression e1, Expression e2, String op) {
        type = t;
        child1 = e1;
        child2 = e2;
        operator = op;
    }

    public Expression(Type t, Expression e1, Expression e2, Expression e3) {
        type = t;
        child1 = e1;
        child2 = e2;
        child3 = e3;
    }

    public Expression(String argName, String argType, Expression e) {
        this.type = Type.eLambda;
        this.argName = argName;
        this.argType = Field.StringToType(argType);
        this.child1 = e;
    }

    public Expression(Expression e, String name) {
        type = Type.eApp;
        child1 = e;
        this.name = name;
    }

    public Expression(String from, String to, boolean fstLocked, boolean sndLocked) {
        type = Type.eRange;
        this.from = from;
        this.to = to;
        this.locked = fstLocked;
        this.sndLocked = sndLocked;
    }

    public Expression(double v) {
        type = Type.eNumber;
        numVal = v;
        locked = false;
    }

    public Expression(double v, boolean locked) {
        type = Type.eNumber;
        numVal = v;
        this.locked = locked;
    }

    public Expression(boolean b) {
        type = Type.eBoolean;
        bVal = b;
    }

    public Expression(String n) {
        type = Type.eVariable;
        name = n;
        locked = false;
    }

    public Expression(String n, boolean locked) {
        type = Type.eVariable;
        name = n;
        this.locked = locked;
    }

    public double getNum() {
        return numVal;
    }

    public boolean getBool() {
        return bVal;
    }

    public static String ReplaceName(String name, int xOffset, int yOffset) {
        try {
            int x = name.charAt(0) - 64;
            int y = Integer.parseInt(name.substring(1));
            return Controller.GenerateName(x + xOffset, y + yOffset);
        } catch (Exception e) {
            return name;
        }
    }

    public void replaceVars(int xOffset, int yOffset) { replaceVars(xOffset, yOffset, true); }

    public void replaceVars(int xOffset, int yOffset, boolean first) {
        if (this.type == Type.eRange) {
            if (!locked) this.from = ReplaceName(this.from, xOffset, yOffset);
            if (!sndLocked) this.to = ReplaceName(this.to, xOffset, yOffset);
        } else if (this.type == Type.eVariable) {
            if (!locked) this.name = ReplaceName(this.name, xOffset, yOffset);
        } else if (first && this.type == Expression.Type.eNumber) {
            Double v = this.getNum();
            if (!locked) if (v - Math.floor(v) < 0.001) this.numVal = v + (double) yOffset;
        }

        if (this.child1 != null) this.child1.replaceVars(xOffset, yOffset, false);
        if (this.child2 != null) this.child2.replaceVars(xOffset, yOffset, false);
        if (this.child3 != null) this.child3.replaceVars(xOffset, yOffset, false);
    }

    public List<String> getDependencies () { return getDependencies(""); }
    public List<String> getDependencies(String argName) {
        List<String> deps = new ArrayList<>();
        if (this.type == Type.eVariable && !this.name.equals(argName))
            deps.add(this.name);
        if (this.type == Type.eRange) {
            String from = this.from;
            String to = this.to;

            int startH = from.charAt(0) - 64;
            int endH = to.charAt(0) - 64;
            int startV = Integer.parseInt(from.substring(1));
            int endV = Integer.parseInt(to.substring(1));

            for (int i = startH; i <= endH; i++)
                for (int j = startV; j <= endV; j++)
                    deps.add(Controller.GenerateName(i, j));
        }

        if (this.child1 != null) {
            if(this.type == Type.eLambda) deps.addAll(this.child1.getDependencies(this.argName));
            else deps.addAll(this.child1.getDependencies());
        }
        if (this.child2 != null) deps.addAll(this.child2.getDependencies());
        if (this.child3 != null) deps.addAll(this.child3.getDependencies());

        return deps;
    }

    @Override
    public String toString() {
        if (type == Type.eUnaryOp)
            return operator + "(" + child1.toString() + ")";
        if (type == Type.eBinaryOp)
            return "(" + child1.toString() + ") " + operator + " (" + child2.toString() + ")";
        if (type == Type.eIf)
            return "if " + child1.toString() + " then " + child2.toString() + " else " + child3.toString();
        if (type == Type.eNumber) {
            if (locked) return "$" + String.valueOf(numVal);
            else return String.valueOf(numVal);
        }
        if (type == Type.eBoolean)
            return String.valueOf(bVal);
        if (type == Type.eVariable) {
            if (locked) return "$" + name;
            else return name;
        }
        if (type == Type.ePair)
            return "(" + child1.toString() + ", " + child2.toString() + ")";
        if (type == Type.eRange) {
            String res = "";
            if (locked) res += "$";
            res += from + ":";
            if (sndLocked) res += "$";
            return res + to;
        }
        if (type == Type.eLambda)
            return argName + " -> " + child1.toString();
        if (type == Type.eApp)
            return name + " " + child1.toString();
        return null;
    }
}
