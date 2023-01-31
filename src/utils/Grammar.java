package utils;

import java.util.ArrayList;

public class Grammar {
    ArrayList<Symbol> nonterminals;
    ArrayList<Symbol> terminals;
    ArrayList<Rule> rules;
    Symbol startingSymbol;

    public Grammar(ArrayList<Symbol> nts, ArrayList<Symbol> ts, ArrayList<Rule> rs, Symbol stS) {
        nonterminals = nts;
        terminals = ts;
        rules = rs;
        startingSymbol = stS;
    }

    public Grammar() {
        nonterminals = new ArrayList<>();
        terminals = new ArrayList<>();
        rules = new ArrayList<>();
        startingSymbol = null;
    }


    public static String getString(Grammar g) {
        StringBuilder s = new StringBuilder("Grammar{nonterminals=");
        for (Symbol nt : g.nonterminals) s.append(nt.name).append(" ");
        s.append(", terminals=");
        for (Symbol t: g.terminals) s.append(t.name).append(" ");
        s.append("\nrules=");
        for (Rule rl : g.rules) {
            s.append("(");
            s.append(rl.leftPart.name);
            s.append(" -> ");
            for (Symbol rp : rl.rightPart) {
                s.append(rp.name).append(" ");
            }
            s.append(')');
        }
        s.append("\nstartingSymbol=");
        s.append(g.startingSymbol.name).append('}');
        return s.toString();
    }
}
