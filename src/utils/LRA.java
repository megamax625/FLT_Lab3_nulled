package utils;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.stream.Collectors;

public class LRA {
    public ArrayList<State> states;
    public ArrayList<State> finalStates;
    public ArrayList<Symbol> alphabet;
    public State startState;
    public ArrayList<Transition> transitions;
    public static int maximumStateNumber;

    public LRA(ArrayList<State> sts, ArrayList<State> fSts, ArrayList<Symbol> alph, State stState, ArrayList<Transition> tr) {
        this.states = sts;
        this.finalStates = fSts;
        this.alphabet = alph;
        this.startState = stState;
        this.transitions = tr;
    }

    public static class Transition {
        public State src;
        public Symbol symbol;
        public State dest;

        public Transition(State left, Symbol sym, State right) {
            this.src = left;
            this.symbol = sym;
            this.dest = right;
        }


        @Override
        public String toString() {
            return "{" + src.num +
                    ", " + symbol.name +
                    ", " + dest.num +
                    '}';
        }
    }

    public static class State {
        int num;
        int finality; // 1 - финальное, 2 - не финальное, но свёртка, 0 - сдвиг по символу сент. формы/стартовое, 3 - новое при построении пда
        ArrayList<Rule> associatedRules;
        ArrayList<Symbol> nextSymbolsInRules;

        public State(int n, int f, ArrayList<Rule> rules) {
            this.num = n;
            this.finality = f;
            this.associatedRules = rules;
            nextSymbolsInRules = ScrapeNextSymbols(associatedRules);
        }

        @Override
        public String toString() {
            return "State{" +
                    "num=" + num +
                    ", finality=" + finality +
                    "\nassociatedRules:" + associatedRules.stream().map((r) -> (r.leftPart.name + " -> " + r.rightPart.toString())).collect(Collectors.joining("\n\t\t")) +
                    "\nnext symbols: [" + nextSymbolsInRules.stream().map((s) -> (s.name)).collect(Collectors.joining(" ")) +
                    "]}";
        }
    }

    public static LRA CFGtoAutomaton(Grammar CFG, boolean debug) {
        DebugPrint("Starting LR(0)-automaton construction", debug);
        ArrayList<Symbol> tempRP = new ArrayList<>();
        tempRP.add(CFG.startingSymbol);
        CFG.rules.add(0, new Rule(new Symbol("[S']", "nonterm"), tempRP));
        for (Rule r: CFG.rules) {
            r = r.InsertDot();
            DebugPrint("Rule with inserted dot: " + r, debug);
        }
        ArrayList<Rule> originalRules = new ArrayList<>(CFG.rules);
        State startState = new State(0, 0, originalRules);
        DebugPrint("StartState: " + startState.toString(), debug);
        ArrayList<State> states = new ArrayList<>();
        ArrayList<Transition> transitions = new ArrayList<>();
        states.add(startState);
        DebugPrint("Added new state1: " + startState, debug);
        ArrayList<Symbol> alphabet = CFG.terminals;
        DebugPrint("ORIG RULES: " + originalRules.stream()
                .map((r) -> (r.leftPart.name + " -> " + r.rightPart.toString())).collect(Collectors.joining("\n")), debug);
        MakeNewStates1(states, transitions, startState, originalRules, CFG, debug);
        states.sort(Comparator.comparingInt(s -> s.num));
        return new LRA(states, states.stream().filter((s) -> (s.finality == 1)).collect(Collectors.toCollection(ArrayList::new)), alphabet, startState, transitions);
    }

    public static void MakeNewStates1(ArrayList<State> states, ArrayList<Transition> transitions, State state,
                                       ArrayList<Rule> originalRules, Grammar CFG, boolean debug) {
        ArrayDeque<State> nextStatesToBuildFrom = new ArrayDeque<>();
        for (Symbol ns : state.nextSymbolsInRules) {
            ArrayList<Rule> applicableRulesList = state.associatedRules.stream().filter((r) -> (r.nextSymbol.name.equals(ns.name)))
                    .collect(Collectors.toCollection(ArrayList::new));
            DebugPrint("Making transition by symbol " + ns + " for rules:\n" +
                    applicableRulesList.stream().map((r) -> (r.leftPart.name + " -> " + r.rightPart.toString())).collect(Collectors.joining("\n")), debug);
            ArrayList<Rule> newRules = new ArrayList<>();
            for (Rule ar : applicableRulesList) {
                ArrayList<Symbol> rightPart = new ArrayList<>();
                for (Symbol s : ar.rightPart) {
                    rightPart.add(new Symbol(s.name, s.type));
                }
                Rule newRule = new Rule(new Symbol(ar.leftPart.name, ar.leftPart.type), rightPart);
                newRules.add(newRule);
            }
            for (Rule nr : newRules) {
                nr = nr.shiftDot();
                DebugPrint("Shifted dot for rule: " + nr, debug);
            }
            if (newRules.stream().anyMatch(Rule::isDotAtEnd)) {
                Rule nr = newRules.get(0);
                int finality;
                if (nr.leftPart.name.equals("[S']")) finality = 1;
                else finality = 2;
                State newState = new State(states.size(), finality, newRules);
                Transition newTransition = new Transition(state, ns, newState);
                states.add(newState);
                transitions.add(newTransition);
                if (!newRules.stream().allMatch(Rule::isDotAtEnd)) nextStatesToBuildFrom.add(newState);
                DebugPrint("Made new state1: " + newState + "\nwith transition: " + newTransition, debug);
            } else {
                State newState = new State(states.size(), 0, newRules);
                transitions.add(new Transition(state, ns, newState));
                nextStatesToBuildFrom.add(newState);
            }
        }
        while (!nextStatesToBuildFrom.isEmpty()) {
            State newState = nextStatesToBuildFrom.peek();
            nextStatesToBuildFrom.remove();
            MakeNewStates2(nextStatesToBuildFrom, states, transitions, newState, originalRules, CFG, debug);
        }
    }

    public static void MakeNewStates2(ArrayDeque<State> stateQ, ArrayList<State> states, ArrayList<Transition> transitions, State state,
                                       ArrayList<Rule> originalRules, Grammar CFG, boolean debug) {
        ArrayList<Rule> rules = new ArrayList<>(state.associatedRules);
        ArrayList<Symbol> nextSymbols = state.nextSymbolsInRules;
        rules = rules.stream().filter((r) -> (!r.isDotAtEnd())).collect(Collectors.toCollection(ArrayList::new));
        for (Symbol next : nextSymbols) {
            ArrayList<Rule> finalNewRules = new ArrayList<>(rules);
            rules.addAll(originalRules.stream().filter((r) -> ((r.leftPart.name.equals(next.name)) &&
                    (!finalNewRules.contains(r)))).collect(Collectors.toCollection(ArrayList::new)));
        }
        DebugPrint("Rules for next new state: " + rules.stream()
                .map((r) -> (r.leftPart.name + " -> " + r.rightPart.toString())).collect(Collectors.joining("\n")), debug);
        ArrayList<Symbol> newNextSymbols = ScrapeNextSymbols(rules);
        DebugPrint("Next symbols for new state: " +
                newNextSymbols.stream().map((newS) -> (newS.name)).collect(Collectors.joining(" ")), debug);
        int stateIndex = states.size();
        ArrayList<Symbol> notProcessedSymbols = new ArrayList<>();
        for (Symbol ns : newNextSymbols) {
            notProcessedSymbols.add(new Symbol(ns.name, ns.type));
        }
        State newState;
        if (state.finality == 1) {
            newState = new State(states.size(), state.finality, new ArrayList<>(state.associatedRules));
            newState.associatedRules = rules;
            newState.nextSymbolsInRules = newNextSymbols;
        } else {
            newState = new State(state.num, 0, rules);
        }
        for (Symbol ns : new ArrayList<>(notProcessedSymbols)) {
            if (CFG.nonterminals.stream().anyMatch((nt) -> ns.name.equals(nt.name))) {
                DebugPrint("Checking nextSymbol " + ns + " for cycle in state " + newState, debug);
                stateIndex = findCycle(states, rules, ns);
                if (stateIndex != -1) {
                    transitions.add(new Transition(states.get(stateIndex), ns, states.get(stateIndex)));
                    DebugPrint("Made cycle from " + states.get(stateIndex) + " by symbol " + ns, debug);
                    notProcessedSymbols.remove(ns);
                }
            }
        }
        if (!notProcessedSymbols.isEmpty()) {
            if (newState.finality != 1) {
                states.add(newState);
            }
            DebugPrint("Next symbols in rules:" + newState.nextSymbolsInRules.toString(), debug);
            int i = 0;
            for (Symbol ns : notProcessedSymbols) {
                ArrayList<Rule> applicableRulesList = newState.associatedRules.stream().filter((r) -> (r.nextSymbol.name.equals(ns.name)))
                        .collect(Collectors.toCollection(ArrayList::new));
                DebugPrint("Making transition from state " + state.num + " by symbol " + ns + " for rules:\n" +
                        applicableRulesList.stream().map((r) -> (r.leftPart.name + " -> " + r.rightPart.toString())).collect(Collectors.joining("\n")), debug);
                ArrayList<Rule> newRules = new ArrayList<>();
                for (Rule ar : applicableRulesList) {
                    ArrayList<Symbol> rightPart = new ArrayList<>();
                    for (Symbol s : ar.rightPart) {
                        rightPart.add(new Symbol(s.name, s.type));
                    }
                    Rule newRule = new Rule(new Symbol(ar.leftPart.name, ar.leftPart.type), new ArrayList<>(rightPart));
                    newRule.nextSymbol = newRule.scrapeSymbolAfterDot();
                    newRules.add(newRule);
                }
                for (Rule nr : newRules) {
                    nr = nr.shiftDot();
                    DebugPrint("Shifted dot for rule: " + nr, debug);
                }
                for (Rule or : originalRules) {
                    if (newRules.stream().anyMatch((nr) -> ((!nr.isDotAtEnd()) && (nr.nextSymbol.name.equals(or.leftPart.name))))) {
                        Rule rule = new Rule(or.leftPart, new ArrayList<>(or.rightPart));
                        rule.nextSymbol = rule.scrapeSymbolAfterDot();
                        newRules.add(rule);
                    }
                }
                Symbol oneSymbol = newRules.get(0).nextSymbol;
                if (newRules.stream().allMatch((nr) -> (nr.nextSymbol.name.equals(oneSymbol.name)))) {
                    DebugPrint("New rules: " + newRules.stream().map(Rule::toString).collect(Collectors.joining(" ")), debug);
                    int index = -1;
                    for (State s : states) {
                        boolean fits = true;
                        for (Rule r : newRules) {
                            DebugPrint("Checking rule : " + r, debug);
                            if (s.associatedRules.stream().noneMatch((ar) -> (ar.toString().equals(r.toString())))) {
                                fits = false;
                                break;
                            }
                        }
                        if (fits) {
                            index = s.num;
                            break;
                        }
                    }
                    if (index != -1) {
                        transitions.add(new Transition(newState, ns, states.get(index)));
                        DebugPrint("Made transition from state " + newState.toString() + " by symbol " + ns + " to state " + states.get(index).toString(), debug);
                    } else {
                        State newestState = new State(states.size() + stateQ.size(), 0, newRules);
                        Transition tr = new Transition(state, ns, newestState);
                        transitions.add(tr);
                        DebugPrint("Made new state2: " + newestState + "\nwith transition: " + tr, debug);
                        if (newestState.associatedRules.stream().allMatch(Rule::isDotAtEnd)) {
                            newestState.finality = 2;
                            states.add(newestState);
                        } else {
                            stateQ.add(newestState);
                        }
                    }
                } else {
                    State newestState = new State(states.size() + stateQ.size() + i, 0, newRules.stream().filter((r) -> (r.nextSymbol.name.equals(ns.name)))
                            .collect(Collectors.toCollection(ArrayList::new)));
                    i++;
                    Transition tr = new Transition(state, ns, newestState);
                    transitions.add(tr);
                    DebugPrint("Made new state3: " + newestState + "\nwith transition: " + tr, debug);
                    stateQ.add(newestState);
                }
            }
        } if ((newState.finality != 1) && (states.stream().noneMatch((st) -> (st.num == newState.num)))) {
            states.add(newState);
        }
    }


    private static int findCycle(ArrayList<State> states, ArrayList<Rule> rules, Symbol ns) {
        int index = -1;
        for (State st : states) {
            boolean same = true;
            for (Rule r : rules) {
                if (st.associatedRules.stream().filter((sr) -> (!(sr.isDotAtEnd())))
                        .noneMatch((srr) -> ((srr.equals(r))))) {
                    same = false;
                    break;
                }
            }
            if (same) return st.num;
        }
        return index;
    }


    public static ArrayList<Symbol> ScrapeNextSymbols(ArrayList<Rule> rules) {
        ArrayList<Symbol> nextSymbols = new ArrayList<>();
        for (Rule r : rules) {
            Symbol s = r.scrapeSymbolAfterDot();
            if (!((s == null) || (nextSymbols.stream().anyMatch((sym) -> (s.name.equals(sym.name)))))) nextSymbols.add(s);
        }
        return nextSymbols;
    }

    public State getDestination(State src, Symbol sym) {
        for (Transition tr : transitions) {
            if ((src.num == tr.src.num) && (sym.name.equals(tr.symbol.name))) return tr.dest;
        }
        return null;
    }

    public static void DebugPrint(String str, boolean debug) {
        if (debug) System.out.println(str);
    }

    public static String LRAtoDOT(LRA auto) {
        StringBuilder str = new StringBuilder("digraph LR0_automaton {\n");
        for (State st : auto.states) {
            str.append("\t").append("q").append(st.num);
            if (auto.finalStates.contains(st)) str.append(" [peripheries=2]");
            str.append(";\n");
        }
        for (Transition tr : auto.transitions) {
            str.append("\t").append("q").append(tr.src.num).append(" -> q").append(tr.dest.num).append(" [label=");
            if (tr.symbol.name.matches("\\[[A-z]+\\]")) str.append(tr.symbol.name, 1, tr.symbol.name.length()-1);
            else str.append(tr.symbol.name);
            str.append("];\n");
        }
        str.append("}\n");
        return str.toString();
    }

    public String getRulesByStates() {
        StringBuilder str = new StringBuilder();
        for (State s : states) {
            str.append("Rules for state ").append(s.num).append(":");
            for (Rule r : s.associatedRules) {
                str.append("\n\t").append(r.toString());
                if (r.isDotAtEnd()) str.append(" | ").append(r.rightPart.
                        stream().filter((sym) -> (!sym.type.equals("dot"))).map((symbol) -> (symbol.name)).collect(Collectors.joining("")))
                        .append(" -> ").append(r.leftPart.name);
            }
            str.append("\n");
        }
        return str.toString();
    }
}