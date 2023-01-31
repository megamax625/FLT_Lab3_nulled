package utils;
import java.util.ArrayList;

public class Parser {
    public static Grammar ParseGrammar(String testInput, boolean debug) {
        testInput = testInput.replaceAll("\\s", "");
        if (testInput.isEmpty()) {
            System.out.println("Test input file is empty!");
            System.exit(4);
        }
        ArrayList<Symbol> CFGtokens = Tokenize(testInput, debug);
        Grammar CFG = ParseGrammar1(CFGtokens, debug);

        return CFG;
    }

    private static Grammar ParseGrammar1(ArrayList<Symbol> tokens, boolean debug) {
        ArrayList<Symbol> nonterminals = new ArrayList<>();
        ArrayList<Symbol> terminals = new ArrayList<>();
        ArrayList<Rule> rules = new ArrayList<>();
        Symbol startingSymbol = null;

        Symbol currLeftPart = null;
        Symbol lastSymbol = null;
        ArrayList<Symbol> sBuf = new ArrayList<>();
        Symbol token;
        boolean first_iter = true;
        while (!tokens.isEmpty()) {
            token = tokens.get(0);
            if (token.type.equals("arrow")) {
                if (lastSymbol == null) {
                    System.out.println("Arrow found with no preceding nonterminal");
                    System.exit(7);
                }
                if (!(currLeftPart == null)) { // уже разбирается какое-нибудь правило
                    if (sBuf.isEmpty()) {
                        DebugPrint("Trying to make a rule with no right part; first-arrow guaranteed", debug);
                        if (!first_iter) {
                            System.exit(7);
                        }
                        first_iter = false;
                    }
                    if (!currLeftPart.type.equals("nonterm")) {
                        System.out.println("Trying to make a rule with not a nonterm as a left part: " + currLeftPart);
                        System.exit(7);
                    }
                    rules.add(new Rule(currLeftPart, sBuf));
                }
                currLeftPart = lastSymbol;
                sBuf = new ArrayList<>();
                lastSymbol = token;
                tokens.remove(0);
                continue;
            }
            if (token.type.equals("alternative")) {
                if (lastSymbol == null) {
                    System.out.println("Alternative found with no preceding nonterminal");
                    System.exit(7);
                }
                if (lastSymbol.type.equals("nonterm") || lastSymbol.type.equals("term") || lastSymbol.type.equals("empty"))
                    sBuf.add(lastSymbol);
                if (currLeftPart != null) { // уже разбирается какое-нибудь правило
                    if (sBuf.isEmpty()) {
                        System.out.println("Trying to make a rule with no right part");
                        DebugPrint("CurrleftPart: " + currLeftPart.name, debug);
                        System.exit(7);
                    }
                    rules.add(new Rule(currLeftPart, sBuf));
                }
                if (!currLeftPart.type.equals("nonterm")) {
                    System.out.println("Trying to make a rule with not a nonterm as a left part: " + currLeftPart);
                    System.exit(7);
                }
                sBuf = new ArrayList<>();
                lastSymbol = token;
                tokens.remove(0);
                continue;
            }
            if (token.type.equals("term")) {
                boolean contained = false;
                for (Symbol term : terminals) {
                    if (term.name.equals(token.name)) {
                        contained = true;
                        break;
                    }
                }
                if (!contained) terminals.add(token);
                if (currLeftPart == null) {
                    System.out.println("Encountered a terminal before any nonterminal could be used as a left part: " + token.name);
                    System.exit(7);
                }
                if (!(lastSymbol == null) && ((lastSymbol.type.equals("nonterm")) || (lastSymbol.type.equals("term")) || (lastSymbol.type.equals("empty")))) sBuf.add(lastSymbol);
                lastSymbol = token;
                tokens.remove(0);
            }
            if (token.type.equals("nonterm")) {
                boolean contained = false;
                for (Symbol nterm : nonterminals) {
                    if (nterm.name.equals(token.name)) {
                        contained = true;
                        break;
                    }
                }
                if (!contained) nonterminals.add(token);
                if (!(lastSymbol == null) && ((lastSymbol.type.equals("nonterm")) || (lastSymbol.type.equals("term")) || (lastSymbol.type.equals("empty")))) sBuf.add(lastSymbol);
                lastSymbol = token;
                tokens.remove(0);
            }
            if (token.type.equals("empty")) {
                if (!(lastSymbol == null) && ((lastSymbol.type.equals("nonterm")) || (lastSymbol.type.equals("term")) || (lastSymbol.type.equals("empty")))) sBuf.add(lastSymbol);
                lastSymbol = token;
                tokens.remove(0);
            }
        }
        if (lastSymbol.type.equals("nonterm") || (lastSymbol.type.equals("term")) || (lastSymbol.type.equals("empty")))
            sBuf.add(lastSymbol);
        if (!sBuf.isEmpty()) {
            rules.add(new Rule(currLeftPart, sBuf));
        }
        for (Symbol s : nonterminals) {
            if (s.name.equals("[S]")) {
                startingSymbol = s;
                break;
            }
        }
        if (startingSymbol == null) {
            System.out.println("Found no starting symbol [S] in Right Linear Grammar");
            System.exit(6);
        }
        Grammar grammar = new Grammar(nonterminals, terminals, rules, startingSymbol);
        DebugPrint("Parsed Grammar:\n" + Grammar.getString(grammar) + "\n", debug);
        return new Grammar(nonterminals, terminals, rules, startingSymbol);
    }

    private static ArrayList<Symbol> Tokenize(String str, boolean debug) {
        ArrayList<String> separatedStrings = new ArrayList<>();
        String newStr = new String(str);
        StringBuilder buf = new StringBuilder();
        boolean insideNonterm = false;
        char s;
        while (!newStr.isEmpty()) {
            s = newStr.charAt(0);
            if (s == '[') {
                int nontermFinish = newStr.indexOf("]");
                if (nontermFinish == -1) {
                    System.out.println("Nonterminal parentheses do not close in " + newStr);
                    System.exit(5);
                } else {
                    buf.append(newStr.substring(0, nontermFinish + 1));
                    separatedStrings.add(buf.toString());
                    buf = new StringBuilder();
                    newStr = newStr.substring(nontermFinish+1);
                }
            }
            else if (s == '|') {
                separatedStrings.add("|");
                newStr = newStr.substring(1);
            }
            else if ((s == '-') && (newStr.charAt(1) == '>')) {
                separatedStrings.add("->");
                newStr = newStr.substring(2);
            }
            else if (s == 'ε') {
                separatedStrings.add("ε");
                newStr = newStr.substring(1);
            }
            else if ((s >= 'a') && (s <= 'z')) {
                separatedStrings.add(String.valueOf(s));
                newStr = newStr.substring(1);
            }
            else {
                System.out.println("Got incorrect symbol during tokenization: " + s + " \n leftover string: " + newStr);
                System.exit(5);
            }
        }
        ArrayList<Symbol> symbols = new ArrayList<>();
        DebugPrint("Received separated strings:",debug);
        DebugPrint("\n", debug);
        separatedStrings.forEach((x) -> DebugPrint(x, debug));
        DebugPrint("\n", debug);
        separatedStrings.forEach((x) -> symbols.add(new Symbol(x)));
        DebugPrint("Made Array of symbols:", debug);
        DebugPrint("\n", debug);
        symbols.forEach((x) -> DebugPrint(x.name + " type: " + x.type + ";", debug));
        DebugPrint("\n", debug);
        return symbols;
    }

    public static void DebugPrint(String str, boolean debug) {
        if (debug) System.out.print(str + " ");
    }
}
