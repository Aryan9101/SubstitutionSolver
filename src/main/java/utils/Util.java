package utils;

import java.util.HashMap;

public class Util {
    public static String nonLetters = ".,:;<>'?()[]{}| /\\\t\n+=_-*&^%$#@!~`";

    public static boolean isAlphabetic(String word){
        boolean flag = true;
        for (char letter: word.toCharArray()) {
            if (letter < 'A' || letter > 'Z') {
                flag = false;
                break;
            }
        }
        return flag;
    }

    public static String getPattern(String word){
        int index = 0;
        HashMap<Character, String> lettersSeen = new HashMap<>();
        StringBuilder pattern = new StringBuilder();
        for (char letter: word.toCharArray()) {
            if (!lettersSeen.containsKey(letter)) {
                lettersSeen.put(letter, Integer.toString(index));
                index++;
            }
            pattern.append(lettersSeen.get(letter)).append("|");
        }
        return pattern.substring(0, pattern.length()-1);
    }
}
