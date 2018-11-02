package calc;

public class Field {

    public enum Type {
        tNum,
        tBool,
        tRange,
        tError,
        tPair,
        tLambda
    }

    public Type type;
    private double numVal;
    private boolean bVal;
    public String argName;
    private String message;
    private String from;
    private String to;

    public Field f1;
    public Field f2;
    public Type argType;
    public Type resType;
    public Expression expr;

    public Field () { }
    public Field (Type t) { type = t; }
    public Field (double x) { type = Type.tNum; numVal = x; }
    public Field (boolean x) { type = Type.tBool; bVal = x; }
    public Field (String message) {
        type = Type.tError;
        this.message = message;
    }
    public Field (String from, String to) {
        this.type = Type.tRange;
        this.from = from;
        this.to = to;
    }

    public Field (Type argType, Type resType) {
        this.type = Type.tLambda;
        this.argType = argType;
        this.resType = resType;
    }
    public Field (String argName, Expression e) {
        this.argName = argName;
        expr = e;
    }
    public Field (Field f1, Field f2) {
        this.type = Type.tPair;
        this.f1 = f1;
        this.f2 = f2;
    }

    public void setType (Field f) {
        type = f.type;
        if(type == Type.tPair) {
            this.f1.type = f.f1.type;
            this.f2.type = f.f2.type;
        }
    }
    public void setNum (double x) {
        numVal = x;
    }
    public void setBool (boolean x) {
        bVal = x;
    }
    public double getNum() {
        return numVal;
    }
    public boolean getBool () {
        return bVal;
    }
    public String getFrom () { return from; }
    public String getTo () { return to; }

    public static Type StringToType (String typ) {
        if(typ.equals("num")) return Type.tNum;
        else if(typ.equals("bool")) return Type.tBool;
        else return null;
    }

    @Override
    public String toString () {
        if(type == Type.tNum) {
            double r = (double) Math.round(numVal * 1000d) / 1000d;
            double f = Math.floor(r);
            if(Math.abs(r - f) < 0.0001) return Integer.toString((int) f);
            else return Double.toString(r);
        }
        if(type == Type.tBool)
            return Boolean.toString(bVal);
        if(type == Type.tPair)
            return "(" + f1.toString() + ", " + f2.toString() + ")";
        if(type == Type.tRange)
            return from + ":" + to;
        return message;
    }

}
