package semantic;

import java.util.HashMap;
import java.util.Map;

public class Symbol {
    public String name;
    public Map<String, String> attribute;

    public Symbol(String name) {
        this.name = name;
        attribute = new HashMap<>();
    }
}
