package com.example.javaprogect;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.Contract;
import java.util.ArrayList;
import java.util.List;

public class RecursiveParser {

    private enum LexemeType {
        LEFT_BRACKET, RIGHT_BRACKET,
        OP_PLUS, OP_MINUS, OP_MUL, OP_DIV,
        OP_POW, OP_FACT, OP_PERCENT, OP_SQRT,
        NUMBER, PI,
        EOF
    }

    private static class Lexeme {
        final LexemeType type;
        final String value;

        Lexeme(LexemeType type, String value) {
            this.type = type;
            this.value = value;
        }

        @NonNull
        @Contract(pure = true)
        @Override
        public String toString() {
            return "Lexeme{" + "type=" + type + ", value='" + value + '\'' + '}';
        }
    }

    private static class LexemeStream {
        private final List<Lexeme> lexemes;
        private int currentIndex;

        LexemeStream(List<Lexeme> lexemes) {
            this.lexemes = lexemes;
            this.currentIndex = 0;
        }

        Lexeme consume() {
            return currentIndex < lexemes.size() ? lexemes.get(currentIndex++) : new Lexeme(LexemeType.EOF, "");
        }

        Lexeme peek() {
            return currentIndex < lexemes.size() ? lexemes.get(currentIndex) : new Lexeme(LexemeType.EOF, "");
        }

        void goBack() {
            if (currentIndex > 0) currentIndex--;
        }

        int getCurrentPosition() {
            return currentIndex;
        }
    }

    private final LexemeStream lexemeStream;
    private final String originalExpression;

    public RecursiveParser(String expression) {
        this.originalExpression = expression;
        this.lexemeStream = new LexemeStream(tokenize(expression));
    }

    public double parse() {
        if (lexemeStream.lexemes.isEmpty() || (lexemeStream.lexemes.size() == 1 && lexemeStream.lexemes.get(0).type == LexemeType.EOF)) {
            throw new ParseException("Empty expression", originalExpression, 0);
        }
        double result = parseAdditionSubtraction();
        Lexeme lastToken = lexemeStream.peek();
        if (lastToken.type != LexemeType.EOF) {
            throw new ParseException("Unexpected token: '" + lastToken.value + "' after expression", originalExpression, lexemeStream.getCurrentPosition());
        }
        return result;
    }

    private List<Lexeme> tokenize(String text) {
        List<Lexeme> lexemes = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            char c = text.charAt(pos);
            switch (c) {
                case '(': lexemes.add(new Lexeme(LexemeType.LEFT_BRACKET, "(")); pos++; break;
                case ')': lexemes.add(new Lexeme(LexemeType.RIGHT_BRACKET, ")")); pos++; break;
                case '+': lexemes.add(new Lexeme(LexemeType.OP_PLUS, "+")); pos++; break;
                case '-': lexemes.add(new Lexeme(LexemeType.OP_MINUS, "-")); pos++; break;
                case '×': lexemes.add(new Lexeme(LexemeType.OP_MUL, "×")); pos++; break;
                case '÷': lexemes.add(new Lexeme(LexemeType.OP_DIV, "÷")); pos++; break;
                case 'π': lexemes.add(new Lexeme(LexemeType.PI, "π")); pos++; break;
                case '!': lexemes.add(new Lexeme(LexemeType.OP_FACT, "!")); pos++; break;
                case '%': lexemes.add(new Lexeme(LexemeType.OP_PERCENT, "%")); pos++; break;
                case '√': lexemes.add(new Lexeme(LexemeType.OP_SQRT, "√")); pos++; break;
                case '^': lexemes.add(new Lexeme(LexemeType.OP_POW, "^")); pos++; break;
                case '.':
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9': {
                    StringBuilder numBuilder = new StringBuilder();
                    boolean decimalFound = (c == '.');
                    numBuilder.append(c);
                    pos++;

                    while (pos < text.length()) {
                        char current = text.charAt(pos);
                        if (Character.isDigit(current)) {
                            numBuilder.append(current);
                            pos++;
                        } else if (current == '.' && !decimalFound) {
                            numBuilder.append('.');
                            decimalFound = true;
                            pos++;
                        } else {
                            break;
                        }
                    }
                    String numStr = numBuilder.toString();
                    if (numStr.equals(".")) {
                        throw new ParseException("Invalid number format: '" + numStr + "'", text, pos - numStr.length());
                    }
                    lexemes.add(new Lexeme(LexemeType.NUMBER, numStr));
                    break;
                }
                default:
                    if (Character.isWhitespace(c)) {
                        pos++;
                    } else {
                        throw new ParseException("Unexpected character: '" + c + "'", text, pos);
                    }
                    break;
            }
        }
        lexemes.add(new Lexeme(LexemeType.EOF, ""));
        return lexemes;
    }

    private double parseAdditionSubtraction() {
        double value = parseMultiplicationDivision();
        while (true) {
            Lexeme lexeme = lexemeStream.peek();
            if (lexeme.type == LexemeType.OP_PLUS) {
                lexemeStream.consume();
                value += parseMultiplicationDivision();
            } else if (lexeme.type == LexemeType.OP_MINUS) {
                lexemeStream.consume();
                value -= parseMultiplicationDivision();
            } else {
                return value;
            }
        }
    }

    private double parseMultiplicationDivision() {
        double value = parseExponentiation();
        while (true) {
            Lexeme lexeme = lexemeStream.peek();
            if (lexeme.type == LexemeType.OP_MUL) {
                lexemeStream.consume();
                value *= parseExponentiation();
            } else if (lexeme.type == LexemeType.OP_DIV) {
                lexemeStream.consume();
                double divisor = parseExponentiation();
                if (Math.abs(divisor) < 1e-15) {
                    throw new ArithmeticException("Division by zero");
                }
                value /= divisor;
            } else {
                return value;
            }
        }
    }

    private double parseExponentiation() {
        double value = parseUnaryOperations();
        if (lexemeStream.peek().type == LexemeType.OP_POW) {
            lexemeStream.consume();
            double exponent = parseExponentiation();
            value = Math.pow(value, exponent);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new ArithmeticException("Result of power operation is invalid");
            }
        }
        return value;
    }

    private double parseUnaryOperations() {
        Lexeme lexeme = lexemeStream.peek();
        if (lexeme.type == LexemeType.OP_MINUS) {
            lexemeStream.consume();
            Lexeme nextLexeme = lexemeStream.peek();
            if (nextLexeme.type == LexemeType.OP_MINUS || nextLexeme.type == LexemeType.OP_PLUS ||
                    nextLexeme.type == LexemeType.OP_MUL || nextLexeme.type == LexemeType.OP_DIV ||
                    nextLexeme.type == LexemeType.OP_POW) {
                throw new ParseException("Unexpected operator after unary minus", originalExpression, lexemeStream.getCurrentPosition());
            }
            return -parsePostfixOperations();
        } else if (lexeme.type == LexemeType.OP_SQRT) {
            lexemeStream.consume();
            double value = parsePostfixOperations();
            if (value < 0) {
                throw new ArithmeticException("Square root of a negative number");
            }
            return Math.sqrt(value);
        } else if (lexeme.type == LexemeType.OP_PLUS) {
            lexemeStream.consume();
            return parsePostfixOperations();
        } else {
            return parsePostfixOperations();
        }
    }

    private double parsePostfixOperations() {
        double value = parseFactorAtom();
        while (true) {
            Lexeme lexeme = lexemeStream.peek();
            if (lexeme.type == LexemeType.OP_FACT) {
                lexemeStream.consume();
                if (value < 0 || value != Math.floor(value)) {
                    throw new ArithmeticException("Factorial is defined only for non-negative integers");
                }
                if (value > 20) {
                    throw new ArithmeticException("Factorial input too large (max 20 supported)");
                }
                value = computeFactorial((long) value);
            } else if (lexeme.type == LexemeType.OP_PERCENT) {
                lexemeStream.consume();
                value = value / 100.0;
            } else {
                break;
            }
        }
        return value;
    }

    private double parseFactorAtom() {
        Lexeme lexeme = lexemeStream.consume();
        switch (lexeme.type) {
            case NUMBER:
                try {
                    String numStr = lexeme.value;
                    if (numStr.endsWith(".") && numStr.length() > 1) {
                        numStr = numStr.substring(0, numStr.length() - 1);
                    } else if (numStr.startsWith(".")) {
                        numStr = "0" + numStr;
                    }
                    return Double.parseDouble(numStr);
                } catch (NumberFormatException e) {
                    throw new ParseException("Invalid number format: '" + lexeme.value + "'", originalExpression, lexemeStream.getCurrentPosition() - 1);
                }
            case PI:
                return Math.PI;
            case LEFT_BRACKET:
                double value = parseAdditionSubtraction();
                Lexeme closingBracket = lexemeStream.consume();
                if (closingBracket.type != LexemeType.RIGHT_BRACKET) {
                    String errorMsg = closingBracket.type == LexemeType.EOF ?
                            "Missing closing parenthesis ')'" :
                            "Expected ')' but found '" + closingBracket.value + "'";
                    throw new ParseException(errorMsg, originalExpression, lexemeStream.getCurrentPosition() - 1);
                }
                return value;
            default:
                lexemeStream.goBack();
                throw new ParseException("Unexpected token: '" + lexeme.value + "'. Expected number, PI, or '('", originalExpression, lexemeStream.getCurrentPosition());
        }
    }

    private long computeFactorial(long n) {
        if (n < 0) throw new IllegalArgumentException("Factorial cannot be computed for negative numbers.");
        if (n > 20) throw new ArithmeticException("Factorial input too large, causes overflow.");
        long result = 1;
        for (long i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public static class ParseException extends RuntimeException {
        public ParseException(String message, String expressionText, int position) {
            super(buildFormattedMessage(message, expressionText, position));
        }

        private static String buildFormattedMessage(String baseMessage, String expression, int positionInExpression) {
            StringBuilder pointer = new StringBuilder();
            if (positionInExpression >= 0 && positionInExpression <= expression.length()) {
                for (int i = 0; i < positionInExpression; i++) {
                    pointer.append(' ');
                }
                pointer.append('^');
            }
            return baseMessage + "\n" + expression + "\n" + pointer.toString();
        }
    }
}