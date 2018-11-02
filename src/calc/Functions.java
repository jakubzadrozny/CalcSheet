package calc;

import java.util.HashMap;
import java.util.Map;

public class Functions {

    public final Map<String, Field> Consts = new HashMap<>();
    public final Map<String, Field> ArgType = new HashMap<>();
    public final Map<String, Field> ValType = new HashMap<>();
    public final Map<String, Wrapper> Body = new HashMap<>();

    public Functions () {
        Consts.put("PI", new Field(Math.PI));
        Consts.put("E", new Field(Math.E));

        ArgType.put("sin", new Field(Field.Type.tNum));
        ArgType.put("cos", new Field(Field.Type.tNum));
        ArgType.put("exp", new Field(Field.Type.tNum));

        ValType.put("sin", new Field(Field.Type.tNum));
        ValType.put("cos", new Field(Field.Type.tNum));
        ValType.put("exp", new Field(Field.Type.tNum));

        Body.put("sin", new ApplySin());
        Body.put("cos", new ApplyCos());
        Body.put("exp", new ApplyExp());
    }

    abstract class Wrapper {
        public abstract double apply (double x);
    }

    class ApplySin extends Wrapper {
        @Override
        public double apply (double x) { return Math.sin(x); }
    }

    class ApplyCos extends Wrapper {
        @Override
        public double apply (double x) { return Math.cos(x); }
    }

    class ApplyExp extends Wrapper {
        @Override
        public double apply (double x) { return Math.exp(x); }
    }

}
