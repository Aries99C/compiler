package lexical;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CharacterReader {

    private final List<Character> characters = new ArrayList<>();
    private String filename;

    public CharacterReader(String filename) {
        this.filename = filename;
        readByCharacter();
    }

    private void readByCharacter() {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filename))) {
            int c;
            while ((c = reader.read()) != -1) {
                characters.add((char) c);
            }
        } catch (FileNotFoundException exception) {
            System.out.println("cannot find file " + filename);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public List<Character> getBuffer() {
        return characters;
    }

    public static void main(String[] args) {
        CharacterReader reader = new CharacterReader("src/main/java/lexical/test.c");
        for (char c : reader.getBuffer()) {
            System.out.print(c);
        }
    }
}
