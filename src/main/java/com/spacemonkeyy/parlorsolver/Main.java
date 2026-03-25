package com.spacemonkeyy.parlorsolver;

import com.spacemonkeyy.parlorsolver.parsing.Parser;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter statement:");
            String statement = scanner.nextLine();
            System.out.println(Parser.parse(statement));
        }
    }
}