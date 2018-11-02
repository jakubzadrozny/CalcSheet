package calc;

import java.io.*;
import java.util.*;

public class Environment {

    private Evaluator eval;
    private Map<String, Field> env = new HashMap<>();
    private Map<String, String> inp = new HashMap<>();
    private Map<String, Set<String> > fwd = new HashMap<>();
    private Map<String, Set<String> > bwd = new HashMap<>();

    public Environment () { eval = new Evaluator (env); }

    public Field getValue (String name) { return env.get(name); }
    public String getInput (String name) { return inp.get(name); }

    private void updateField (String name)  {
        String input = inp.get(name);
        Field f;
        try {
            f = eval.eval(input);
        } catch (Exception e) {
            f = new Field(e.getMessage());
        }
        env.put(name, f);
    }

    private void removeDependencies (String from) {
        Set<String> deps = fwd.get(from);
        if(deps == null) return;
        for(String name : deps)
            bwd.get(name).remove(from);
        deps.clear();
    }

    private void addDependencies (String from) {
        Set<String> deps = fwd.get(from);
        if(deps == null) return;
        for(String name : deps) {
            Set<String> tmp = bwd.get(name);
            if(tmp == null) bwd.put(name, new HashSet<>());
            bwd.get(name).add(from);
        }

    }

    private Map<String, Integer> vis = new HashMap<>();
    private Map<String, Integer> arrows = new HashMap<>();
    private Set<String> changed = new HashSet<>();

    private void cntArrows (String name) {
        vis.put(name, 1);

        Set<String> edges = bwd.get(name);
        if(edges == null) {
            vis.put(name, 2);
            return;
        }

        for(String next : edges) {
            Integer cnt = arrows.get(next);
            if (cnt == null) cnt = 0;
            arrows.put(next,  cnt + 1);

            Integer st = vis.get(next);
            if(st == null) cntArrows(next);
            else if(st == 1)
                throw new RuntimeException ("cyclic dependency");
        }

        vis.put(name, 2);
    }

    private void topSort (String name) {
        updateField(name);
        changed.add(name);

        Set<String> edges = bwd.get(name);
        if(edges == null) return;

        for(String next : edges) {
            int arrs = arrows.get(next);
            arrows.put(next, arrs - 1);
            if(arrs == 1) topSort(next);
        }
    }

    public String copyInput (String from, String to, int xOffset, int yOffset) {
        try {
            String input = inp.get(from);
            Expression e = eval.parse(input);
            e.replaceVars(xOffset, yOffset);
            return e.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public List<String> evaluate (String name, String input) {
        removeDependencies(name);
        fwd.put(name, eval.getDependencies(input));
        addDependencies(name);

        vis.clear();
        arrows.clear();
        changed.clear();
        inp.put(name, input);

        try {
            cntArrows(name);
            topSort(name);
        } catch (Exception e) {
            Field f = new Field (e.getMessage());
            env.put(name, f);
            changed.add(name);
        }

        List<String> res = new ArrayList<>();
        res.addAll(changed);
        return res;
    }

    public void clearAll () {
        inp.clear();
        env.clear();
        fwd.clear();
        bwd.clear();
    }

    public void saveAll (File f) {
        try {
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(inp);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> loadAll (File f) {
        List<String> added = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            inp = (HashMap) ois.readObject();

            for (Map.Entry<String, String> entry : inp.entrySet())
                added.addAll(evaluate(entry.getKey(), entry.getValue()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return added;
    }

}
