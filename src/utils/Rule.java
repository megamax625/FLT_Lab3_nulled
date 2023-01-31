package utils;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Rule {
    Symbol leftPart;
    ArrayList<Symbol> rightPart;
    Symbol nextSymbol;

    public Rule(Symbol left, ArrayList<Symbol> right) {
        this.leftPart = left;
        if (right.size() < 1) {
            System.out.println("Trying to make a rule with no right part!");
            System.exit(3);
        } else {
            this.rightPart = right;
        }
    }


    public Rule(ArrayList<Symbol> right) {
        this.rightPart = right;
    }

    public Rule() {
        this.leftPart = null;
        this.rightPart = null;
        this.nextSymbol = null;
    }

    @Override
    public String toString() {
        return "Rule: {" + leftPart.name + " -> " + rightPart.stream().map((s) -> s.name).collect(Collectors.joining(" ")) +
                "]}";
    }

    public static boolean isToEmpty(Rule r) {
        return ((r.rightPart.size() == 1) && (r.rightPart.get(0).type.equals("empty")));
    }

    public static boolean isToTerm(Rule r) {
        return ((r.rightPart.size() == 1) && (r.rightPart.get(0).type.equals("term")));
    }

    public static boolean isToTermNterm(Rule r) {
        return ((r.rightPart.size() == 2) && (r.rightPart.get(0).type.equals("term")) && (r.rightPart.get(1).type.equals("nonterm")));
    }

    public static boolean isToNtermNterm(Rule r) {
        return ((r.rightPart.size() == 2) && (r.rightPart.get(0).type.equals("nonterm")) && (r.rightPart.get(1).type.equals("nonterm")));
    }

    public static boolean hasTermAndNotToTerm(Rule r) {
        return ((r.rightPart.size() > 1) && r.rightPart.stream().anyMatch(rp -> rp.type.equals("term")));
    }

    public static boolean isToNterms(Rule r) {
        return ((r.rightPart.size() > 1) && r.rightPart.stream().allMatch(s -> s.type.equals("nonterm")));
    }

    public static ArrayList<String> getTerminalNames(Rule r) {
        ArrayList<String> names = new ArrayList<>();
        for (Symbol s : r.rightPart) {
            if ((s.type.equals("term")) && (!names.contains(s.name))) names.add(s.name);
        }
        return names;
    }

    public static void DebugPrint(String str, boolean debug) {
        if (debug) System.out.println(str);
    }

    public Rule InsertDot() {
        this.rightPart.add(0, new Symbol("•"));
        return this;
    }

    public boolean isDotAtEnd() {
        return this.rightPart.get(this.rightPart.size() - 1).type.equals("dot");
    }

    public Rule shiftDot() {
        if (!isDotAtEnd()) {
            Symbol dot = new Symbol("•");
            int pos = getDotPosition();
            this.rightPart.remove(pos);
            this.rightPart.add(pos + 1, dot);
            return new Rule(this.leftPart, this.rightPart);
        }
        return null;
    }

    public int getDotPosition() {
        int i;
        for (i = 0; i < this.rightPart.size(); i++) {
            if (this.rightPart.get(i).type.equals("dot")) break;
        }
        return i;
    }

    public static Symbol getPreviousSymbol(Rule r) {
        int pos = r.getDotPosition();
        if ((pos != 0) && (pos < r.rightPart.size())) return r.rightPart.get(pos-1);
        else return null;
    }

    public Symbol scrapeSymbolAfterDot() {
        if (!isDotAtEnd()) {
            int i = getDotPosition();
            this.nextSymbol = this.rightPart.get(i+1);
            return this.nextSymbol;
        }
        else return null;
    }

}