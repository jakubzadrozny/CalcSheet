package calc;

public class Token {

    public enum Type {
        tokNum("number"),
        tokIdent("identifier"),
        tokOp("operator"),
        tokTrue("true"),
        tokFalse("false"),
        tokIf("if"),
        tokThen("then"),
        tokElse("else");

        private String text;

        Type (String _text) { text = _text; }

        @Override
        public String toString () { return text; }
    }

    public Type type;
    public String content;

    public Token (Type _type, String _content) { type = _type; content = _content; }

    @Override
    public String toString () {
        String res = type.toString();
        if(content != null) res += "(" + content + ")";
        return res;
    }
}
