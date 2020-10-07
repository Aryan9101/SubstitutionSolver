import utils.Util;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.*;

public class SubSolver {
    public static Scanner input = new Scanner(System.in);
    public static DBConnector db;
    public static String alphabets = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static String mostCommon = "ETAOINSRHLDCUMFPGWYBVKXJQZ";
    public static String ciphertext;
    public static List<String> ciphertextWords;
    public static HashMap<Character, Character> key = new HashMap<>();

    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException { 
        db = new DBConnector();
        ciphertext = input.nextLine().toUpperCase();
        ciphertextWords = Arrays.asList(ciphertext.split("\\W+"));
        createEmptyKey();
        HashMap<String, Integer> bigramFrequencies = getNGramFrequencies(2);
        HashMap<String, Integer> trigramFrequencies = getNGramFrequencies(3);
        HashMap<String, Integer> twoLetterWords = getNLetterWordFrequencies(2);
        HashMap<String, Integer> threeLetterWords = getNLetterWordFrequencies(3);
        HashMap<String, Integer> fourLetterWords = getNLetterWordFrequencies(4);
        chooseLongerKey();
        System.out.println(key.toString());
        System.out.println(decryptMessage(ciphertext));
        db.disconnect();
    }

    public static HashMap<Character, Character> createEmptyKey(){
        HashMap<Character, Character> key = new HashMap<>();
        for (char alphabet: alphabets.toCharArray()) {
            key.put(alphabet, null);
        }
        return key;
    }

    public static String decryptWithKey(String word){
        StringBuilder decrypted = new StringBuilder();
        for (char letter: word.toCharArray()) {
            if (key.get(letter) != null) {
                decrypted.append(key.get(letter));
            } else {
                decrypted.append(letter);
            }
        }
        return decrypted.toString();
    }

    public static String decryptMessage(String ciphertext) {
        String[] words = ciphertext.split("\\s+");
        StringBuilder encrypted = new StringBuilder();
        for (String word: words) {
            if (Util.nonLetters.indexOf(word.charAt(0)) != -1) {
                encrypted.append(word);
            } else {
                encrypted.append(decryptWithKey(word));
            }
            encrypted.append(" ");
        }
        return encrypted.toString();
    }

    public static HashMap<Character, Integer> getNLetterFrequencies(){
        HashMap<Character, Integer> freqMap = new HashMap<>();
        for (char letter: ciphertext.toCharArray()) {
            if (Util.nonLetters.indexOf(letter) == -1)
                freqMap.put(letter, freqMap.getOrDefault(letter, 0) + 1);
        }
        return freqMap;
    }

    public static char getNthMostFrequentChar(int N){
        HashMap<Character, Integer> frequencies = getNLetterFrequencies();
        List<Map.Entry<Character, Integer>> sortedByFreq = frequencies.entrySet().stream()
                                                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                                    .collect(Collectors.toList());
        return sortedByFreq.get(N-1).getKey();
    }

    public static HashMap<String, Integer> getNGramFrequencies(int N){
        HashMap<String, Integer> nGramFrequencies = new HashMap<>();
        for (int i = 0; i < ciphertext.length() - N; i++) {
            String nGram = ciphertext.substring(i, i + N);
            if (Util.isAlphabetic(nGram)) {
                nGramFrequencies.put(nGram, nGramFrequencies.getOrDefault(nGram, 0) + 1);
            }
        }
        return nGramFrequencies;
    }

    public static HashMap<String, Integer> getNLetterWordFrequencies(int N){
        HashMap<String, Integer> nLetterWordFrequencies = new HashMap<>();
        for (String word: ciphertextWords) {
            if (word.length() == N){
                nLetterWordFrequencies.put(word, nLetterWordFrequencies.getOrDefault(word, 0) + 1);
            }
        }
        return nLetterWordFrequencies;
    }

    public static HashSet<Character> getDoubles(){
        HashSet<Character> doubles = new HashSet<>();
        for (int i = 0; i < ciphertext.length() - 1; i++) {
            if (ciphertext.charAt(i) == ciphertext.charAt(i + 1)){
                doubles.add(ciphertext.charAt(i));
            }
        }
        return doubles;
    }

    public static HashMap<Character, HashSet<Character>> getPossibilities(String decryptedWord, String table) throws SQLException {
        HashMap<Character, HashSet<Character>> possibilities = new HashMap<>();
        String pattern = Util.getPattern(decryptedWord);
        ArrayList<String> matchedPatterns = db.getWordsFromPattern(pattern, table);
        for (String possibleMatch: matchedPatterns) {
            for (int i = 0; i < decryptedWord.length(); i++) {
                char current = decryptedWord.charAt(i);
                if (!possibilities.containsKey(current)){
                    possibilities.put(current, new HashSet<>());
                }
                possibilities.get(current).add(possibleMatch.charAt(i));
            }
        }
        return possibilities;
    }

    public static void filterUniqueChars(HashMap<Character, HashSet<Character>> map1,
                                         HashMap<Character, HashSet<Character>> map2){
        for (char letter: map2.keySet()) {
            if (map1.containsKey(letter)) {
                map1.get(letter).retainAll(map2.get(letter));
            } else {
                map1.put(letter, map2.get(letter));
            }
        }
    }

    public static HashMap<Character, HashSet<Character>> getPossibilitiesForLongestWord(String table) throws SQLException {
        int maxLength = ciphertextWords.stream().map(String::length).max(Integer::compareTo).orElse(-1);
        ListIterator<String> longestWords = ciphertextWords.stream().filter(s -> s.length() == maxLength)
                                                                        .collect(Collectors.toList()).listIterator();
        ciphertextWords = ciphertextWords.stream().filter(s -> s.length() != maxLength).collect(Collectors.toList());
        HashMap<Character, HashSet<Character>> possibilities = getPossibilities(longestWords.next(), table);
        while (longestWords.hasNext()){
            HashMap<Character, HashSet<Character>> possibilitiesForWord = getPossibilities(longestWords.next(), table);
            filterUniqueChars(possibilities, possibilitiesForWord);
        }
        return possibilities;
    }

    public static HashMap<Character, HashSet<Character>> getKeyPossibilities(String table) throws SQLException {
        HashMap<Character, HashSet<Character>> possibleKey = getPossibilitiesForLongestWord(table);
        while (!ciphertextWords.isEmpty()) {
            filterUniqueChars(possibleKey, getPossibilitiesForLongestWord(table));
        }
        return possibleKey;
    }

    public static void filterKeyFromPossibilities(String table, HashMap<Character, Character> key) throws SQLException {
        HashMap<Character, HashSet<Character>> keyPossibilities = getKeyPossibilities(table);
        for (Map.Entry<Character, HashSet<Character>> letterChoices: keyPossibilities.entrySet()) {
            if (letterChoices.getValue().size() == 1) {
                char letter = letterChoices.getValue().iterator().next();
                key.put(letterChoices.getKey(), letter);
                for (Map.Entry<Character, HashSet<Character>> entry: keyPossibilities.entrySet()) {
                    entry.getValue().remove(letter);
                }
            }
        }
        if (checkIfSingleChoicesRemain(keyPossibilities)){
            filterKeyFromPossibilities(table, key);
        }
    }

    public static boolean checkIfSingleChoicesRemain(HashMap<Character, HashSet<Character>> keyPossibilities) {
        boolean flag = false;
        for (Map.Entry<Character, HashSet<Character>> entry: keyPossibilities.entrySet()) {
            if (entry.getValue().size() == 1) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static void chooseLongerKey() throws SQLException {
        HashMap<Character, Character> keyFromRegularTable = createEmptyKey();
        HashMap<Character, Character> keyFromMiniTable = createEmptyKey();
        filterKeyFromPossibilities("word_patterns", keyFromRegularTable);
        ciphertextWords = Arrays.asList(ciphertext.split("\\W+")); // Reset the words list
        filterKeyFromPossibilities("word_patterns_mini", keyFromMiniTable);
        int nullCount1 = 0;
        int nullCount2 = 0;
        for (char alphabet: alphabets.toCharArray()) {
            if (keyFromRegularTable.get(alphabet) == null) {
                nullCount1++;
            }
            if (keyFromMiniTable.get(alphabet) == null) {
                nullCount2++;
            }
        }
        key = nullCount1 <= nullCount2 ? keyFromRegularTable : keyFromMiniTable;
    }
}