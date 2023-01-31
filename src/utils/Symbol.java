package utils;

public class Symbol {
    String name;
    String type; // "term", "nonterm", "alternative", "arrow", "empty", "dot", "stateNum"

    public Symbol(String name) {
        this.name = name;
        if (name.length() < 1) {
            System.out.println("Trying to make symbol out of empty string!");
            System.exit(2);
        }
        if (name.length() == 1) {
            if (name.matches("[a-z]")) {
                this.type = "term";
            } else if (name.equals("ε")) {
                this.type = "empty";
            } else if (name.equals("|")) {
                this.type = "alternative";
            } else if (name.equals("•")) {
                this.type = "dot";
            } else {
                System.out.println("Found 1-letter symbol which is not term, alternative or empty: " + name);
                System.exit(2);
            }
        } else {
            if (name.matches("\\[[a-zA-z]+\\]")) {
                this.type = "nonterm";
            } else if (name.equals("->")) {
                this.type = "arrow";
            } else {
                System.out.println("Found >1-letter symbol which is not nonterm or arrow: " + name);
                System.exit(2);
            }
        }
    }

    public Symbol(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Symbol(Symbol s) {
        this.name = s.name;
        this.type = s.type;
    }

    public Symbol() {
        this.name = null;
        this.type = null;
    }

    @Override
    public String toString() {
        return name;
    }
}
