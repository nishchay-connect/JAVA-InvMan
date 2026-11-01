package ui;

import exception.InvalidProductDataException;

import java.util.Scanner;
import java.util.function.Function;


public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) {
                    return v;
                }
            } catch (NumberFormatException e) {
            }
            System.out.println("  Invalid choice. Please enter a number from " + min + " to " + max + ".");
        }
    }

   
    public <T> T readValid(String prompt, Function<String, T> parser) {
        while (true) {
            String s = readLine(prompt);
            try {
                return parser.apply(s);
            } catch (InvalidProductDataException e) {
                System.out.println("  Invalid: " + e.getMessage());
            }
        }
    }

    public <T> T readValidOrBlank(String prompt, Function<String, T> parser) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                return null;
            }
            try {
                return parser.apply(s);
            } catch (InvalidProductDataException e) {
                System.out.println("  Invalid: " + e.getMessage());
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String s = readLine(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                return true;
            }
            if (s.equals("n") || s.equals("no")) {
                return false;
            }
            System.out.println("  Please answer y or n.");
        }
    }
}
