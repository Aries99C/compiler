package grammatical;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LR {
    public final List<Production> productions = new ArrayList<>();
    public final List<String> terminalList = new ArrayList<>();

    private void init() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("src/main/java/grammar/grammar.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] leftRights = line.split("::=");
                String[] multiRights = leftRights[1].split(" \\| ");
                for (String rights : multiRights) {
                    productions.add(new Production(leftRights[0].trim(), rights.trim().split("\\s+")));
                }
            }
        } catch (Exception ignored) {}
    }

    public void showProductions() {
        System.out.println("****************************************");
        System.out.println("Productions:");
        for (Production production : productions) {
            String left = production.left;
            String[] rights = production.rights;
            StringBuilder builder = new StringBuilder();
            builder.append(left).append(" ::= ");
            for (String right : rights) {
                builder.append(right).append(" ");
            }
            System.out.println(builder.toString());
        }
        System.out.println("****************************************");
    }

    public LR() {
        init();
        showProductions();
    }

    public static void main(String[] args) {
        LR lr = new LR();
    }
}
