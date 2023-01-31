package utils;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class PDA {
    public ArrayList<LRA.State> states;
    public ArrayList<LRA.State> finalStates;
    public ArrayList<Symbol> alphabet;
    public LRA.State startState;
    public ArrayList<Transition> transitions;
    public ArrayList<Integer> stackSymbolAlphabet;
    public static int maximumStateNumber;

    public PDA(ArrayList<LRA.State> states, ArrayList<LRA.State> finalStates, ArrayList<Symbol> alphabet, LRA.State startState,
               ArrayList<LRA.Transition> transitions) {
        this.states = states;
        this.finalStates = finalStates;
        this.alphabet = alphabet;
        this.startState = startState;
        this.transitions = new ArrayList<>();
        for (LRA.Transition tr : transitions) {
            Transition tran = new Transition(tr.src, tr.symbol, tr.dest);
            tran.stackChange = new Transition.StackChange();
            if (tr.dest.finality != 1) {
                tran.stackChange.left = new Symbol("x");
                tran.stackChange.right.add(new Symbol(String.valueOf(tr.dest.num), "stateNum"));
            } else {
                tran.stackChange.left = new Symbol("0", "stateNum");
            }
            tran.stackChange.right.add(new Symbol("x"));
            this.transitions.add(tran);
        }
        this.stackSymbolAlphabet = new ArrayList<>();
        for (LRA.State st : states) {
            stackSymbolAlphabet.add(st.num);
        }
        maximumStateNumber = states.get(states.size()-1).num + 1;
    }

    public static class Transition {
        public LRA.State src;
        public Symbol symbol;
        public StackChange stackChange;
        public LRA.State dest;

        public static class StackChange {
            public Symbol left;
            public ArrayList<Symbol> right;

            public StackChange() {
                this.left = null;
                this.right = new ArrayList<>();
            }

            @Override
            public String toString() {
                return left + "/" + right.stream().map((s) -> (s.name)).collect(Collectors.joining(" "));
            }
        }

        public Transition(LRA.State left, Symbol sym, LRA.State right) {
            this.src = left;
            this.symbol = sym;
            this.dest = right;
            this.stackChange = new StackChange();
        }

        public Transition(LRA.State left, Symbol sym, LRA.State right, StackChange stackChange) {
            this.src = left;
            this.symbol = sym;
            this.dest = right;
            this.stackChange = stackChange;
        }

        @Override
        public String toString() {
            return "{" + src.num +
                    ", <" + symbol.name + ", " + stackChange.toString() +
                    ">, " + dest.num +
                    '}';
        }
    }

    public static PDA LR0toPDA(LRA auto, Grammar cfg, boolean debug) {
        PDA pda = new PDA(auto.states, auto.finalStates, auto.alphabet, auto.startState, auto.transitions);
        DebugPrint(PDAtoDOT(pda), debug);
        AddEpsTransitions(pda, debug);
        DebugPrint(PDAtoDOT(pda), debug);
        RemoveNontermTransitions(pda, cfg, debug);
        return pda;
    }

    public static void AddEpsTransitions(PDA pda, boolean debug) {
        ArrayList<LRA.State> svertStates = pda.states.stream().filter((st) -> (st.finality) == 2).collect(Collectors.toCollection(ArrayList::new));
        DebugPrint(pda.transitions.stream().map(Transition::toString).collect(Collectors.joining(";")), debug);
        for (LRA.State svSt : svertStates) {
            int backtrackDepth = svSt.associatedRules.get(0).rightPart.size() - 1; // точка тоже сюда входит, но считать её не надо
            DebugPrint("Backtrack depth: " + backtrackDepth + " for state " + svSt, debug);
            RecBacktrack(pda.states, pda.transitions, svSt, svSt, svSt, backtrackDepth, debug);
        }
    }

    public static void RecBacktrack(ArrayList<LRA.State> states, ArrayList<Transition> transitions, LRA.State st, LRA.State chainState, LRA.State origState, int depth, boolean debug) {
        if (depth > 1) {
            ArrayList<Transition> associatedTransitions = transitions.stream()
                    .filter((tr) -> ((tr.dest.num == st.num) && (tr.src.finality != 3))).collect(Collectors.toCollection(ArrayList::new));
            for (Transition tr : associatedTransitions) {
                LRA.State newState = new LRA.State(maximumStateNumber, 3, new ArrayList<>());
                maximumStateNumber++;
                Transition.StackChange stackChange = new Transition.StackChange();
                stackChange.left = new Symbol(String.valueOf(st.num), "stateNum");
                stackChange.right = new ArrayList<>();
                Transition newTr = new Transition(chainState, new Symbol("ε"), newState, stackChange);
                DebugPrint("Made a new state with num=" + newState.num + " and a transition " + newTr.toString(), debug);
                states.add(newState);
                transitions.add(newTr);
                RecBacktrack(states, transitions, tr.src, newState, origState, depth - 1, debug);
            }
        } else {
            Symbol ns = origState.associatedRules.get(0).leftPart;
            ArrayList<LRA.State> associatedStates = states.stream()
                    .filter((state) -> (state.associatedRules.stream()
                            .anyMatch((ar) -> ((Rule.getPreviousSymbol(ar) != null) && Rule.getPreviousSymbol(ar).name.equals(ns.name)))))
                    .collect(Collectors.toCollection(ArrayList::new));
            LRA.State newState = new LRA.State(maximumStateNumber, 3, new ArrayList<>());
            states.add(newState);
            DebugPrint("Made a new state with num=" + newState.num, debug);
            maximumStateNumber++;
            Transition.StackChange stackChange1 = new Transition.StackChange();
            stackChange1.left = new Symbol(String.valueOf(st.num), "stateNum");
            stackChange1.right = new ArrayList<>();
            Transition newTr1 = new Transition(chainState, new Symbol("ε"), newState, stackChange1);
            DebugPrint("Made a new state with num=" + newState.num + " and a transition " + newTr1.toString(), debug);
            transitions.add(newTr1);
            DebugPrint("Associated states: " + associatedStates.stream().map((ast) -> (String.valueOf(ast.num))).collect(Collectors.joining(";")), debug);
            for (LRA.State aSt : associatedStates) {
                ArrayList<LRA.State> srcStates = states.stream().filter((s) -> (transitions.stream()
                        .anyMatch((tr) -> ((s.num == tr.src.num) && (ns.name.equals(tr.symbol.name)) && (tr.dest.num == aSt.num)) && (s.finality != 3))))
                        .collect(Collectors.toCollection(ArrayList::new));
                DebugPrint("Source states for state " + aSt.num + ": " + srcStates.stream().map((ast) -> (String.valueOf(ast.num))).collect(Collectors.joining(";")), debug);
                for (LRA.State src : srcStates) {
                    Transition.StackChange stackChange = new Transition.StackChange();
                    stackChange.right = new ArrayList<>();
                    if (aSt.finality == 1) {
                        stackChange.left = new Symbol(String.valueOf(src.num), "stateNum");
                    } else {
                        stackChange.left = new Symbol(String.valueOf(src.num), "stateNum");
                        stackChange.right.add(new Symbol(String.valueOf(aSt.num), "stateNum"));
                        stackChange.right.add(new Symbol(String.valueOf(src.num), "stateNum"));
                    }
                    Transition newTr = new Transition(newState, ns, aSt, stackChange);
                    DebugPrint("Made a new transition " + newTr, debug);
                    transitions.add(newTr);
                    if (aSt.finality == 1) {
                        ArrayList<Transition> changeTransitions = transitions.stream()
                                .filter((tran) -> ((tran.stackChange.right.size() > 0) && (tran.dest.num == newTr.dest.num) && (tran.src.num != newTr.src.num) &&
                                        (tran.stackChange.right.get(tran.stackChange.right.size() - 1).name.equals("x")))).collect(Collectors.toCollection(ArrayList::new));
                        for (Transition tran : changeTransitions) {
                            DebugPrint("Checking tran " + tran.toString(), debug);
                            if (tran.stackChange.right.get(tran.stackChange.right.size() - 1).name.equals("x"))
                                tran.stackChange.right.remove(tran.stackChange.right.size() - 1);
                        }
                    }
                }
            }
        }
    }

    public static void RemoveNontermTransitions(PDA pda, Grammar cfg, boolean debug) {
        for (Transition tr : pda.transitions) {
            if (tr.dest.finality == 2) {
                tr.stackChange.right = new ArrayList<>();
                tr.stackChange.right.add(new Symbol("x"));
            }
        }
        for (LRA.State s : new ArrayList<>(pda.states)) {
            if ((s.finality == 3)) {
                int oldNum = s.num;
                int newNum = -1;
                Symbol nonterm = new Symbol("nope", "nope");
                for (Transition tr : pda.transitions) {
                    if ((tr.dest == s) && (tr.src.finality == 2)) {
                        newNum = tr.src.num;
                        nonterm = tr.symbol;
                        pda.transitions.remove(tr);
                        break;
                    }
                }
                if (newNum != -1) {
                    for (Transition tr : new ArrayList<>(pda.transitions)) {
                        if (tr.src.num == oldNum) {
                            tr.src.num = newNum;
                            if (tr.symbol.name.equals(nonterm.name)) {
                                tr.symbol = new Symbol("ε");
                            }
                        }
                        pda.states.remove(s);
                    }
                }
            }
            for (Transition tr : pda.transitions) {
                if ((tr.src.finality == 3) && (cfg.nonterminals.stream().anyMatch((nt) -> (nt.name.equals(tr.symbol.name)))))
                    tr.symbol = new Symbol("ε");
            }
            pda.transitions.removeIf((tr) -> (cfg.nonterminals.stream().anyMatch((nt) -> (nt.name.equals(tr.symbol.name)))));
        }
    }

    public static String PDAtoDOT(PDA auto) {
        StringBuilder str = new StringBuilder("digraph PDA {\n");
        for (LRA.State st : auto.states) {
            str.append("\t").append("q").append(st.num);
            if (auto.finalStates.contains(st)) str.append(" [peripheries=2]");
            str.append(";\n");
        }
        for (Transition tr : auto.transitions) {
            str.append("\t").append("q").append(tr.src.num).append(" -> q").append(tr.dest.num).append(" [label=\"") ;
            if (tr.symbol.name.matches("\\[[A-z]\\]")) str.append(tr.symbol.name.substring(1, tr.symbol.name.length()-1));
            else str.append(tr.symbol.name);
            str.append(", ").append(tr.stackChange.toString());
            str.append("\"];\n");
        }
        str.append("}\n");
        return str.toString();
    }

    public static void DebugPrint(String str, boolean debug) {
        if (debug) System.out.println(str);
    }
}
