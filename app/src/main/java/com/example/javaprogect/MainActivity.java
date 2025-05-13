package com.example.javaprogect;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AutoResizeTextView textInputDisplay;
    private AutoResizeTextView textExpressionPreviewDisplay;

    private boolean isResultCurrentlyDisplayed = false;
    private ArrayList<String> calculationHistoryList = new ArrayList<>();

    private static final int MAX_HISTORY_ITEMS = 20;
    private static final String GENERIC_ERROR_MESSAGE = "Error";
    private static final String INCOMPLETE_EXPRESSION_MSG = "Incomplete expression";
    private static final String UNBALANCED_BRACKETS_MSG = "Unbalanced parentheses";
    private static final String TEXT_TOO_LONG_MSG = "Expression too long";
    private static final int MAX_EXPRESSION_LENGTH = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        textInputDisplay = findViewById(R.id.text_input);
        textExpressionPreviewDisplay = findViewById(R.id.text_expression);

        textInputDisplay.setMinTextSizeInSp(24);
        textInputDisplay.setMaxTextSizeInSp(70);

        textExpressionPreviewDisplay.setMinTextSizeInSp(18);
        textExpressionPreviewDisplay.setMaxTextSizeInSp(50);

        initializeButtonListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_history) {
            showHistoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeButtonListeners() {
        View.OnClickListener digitClickListener = v -> {
            Button b = (Button) v;
            onDigitPressed(b.getText().toString());
        };
        findViewById(R.id.btn_0).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_1).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_2).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_3).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_4).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_5).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_6).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_7).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_8).setOnClickListener(digitClickListener);
        findViewById(R.id.btn_9).setOnClickListener(digitClickListener);

        View.OnClickListener operatorClickListener = v -> {
            Button b = (Button) v;
            onOperatorPressed(b.getText().toString());
        };
        findViewById(R.id.btn_plus).setOnClickListener(operatorClickListener);
        findViewById(R.id.btn_minus).setOnClickListener(operatorClickListener);
        findViewById(R.id.btn_mul).setOnClickListener(operatorClickListener);
        findViewById(R.id.btn_div).setOnClickListener(operatorClickListener);
        findViewById(R.id.btn_degree).setOnClickListener(operatorClickListener);

        findViewById(R.id.btn_sqrt).setOnClickListener(v -> onFunctionPressed("√"));
        findViewById(R.id.btn_pi).setOnClickListener(v -> onFunctionPressed("π"));
        findViewById(R.id.btn_factorial).setOnClickListener(v -> onFunctionPressed("!"));
        findViewById(R.id.btn_percent).setOnClickListener(v -> onFunctionPressed("%"));

        findViewById(R.id.btn_ac).setOnClickListener(v -> onClearAllPressed());
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackspacePressed());
        findViewById(R.id.btn_brackets).setOnClickListener(v -> onBracketsPressed());
        findViewById(R.id.btn_comma).setOnClickListener(v -> onDecimalPressed());
        findViewById(R.id.btn_equal).setOnClickListener(v -> onEqualsPressed());
    }

    private void onDigitPressed(String digit) {
        clearErrorFromPreview();
        if (isResultCurrentlyDisplayed) {
            textInputDisplay.setText(digit);
            isResultCurrentlyDisplayed = false;
        } else {
            String currentInput = textInputDisplay.getText().toString();
            if (currentInput.equals("0") && digit.equals("0")) {
                return;
            }
            if (currentInput.equals("0") && !digit.equals("0") && !digit.equals(".")) {
                textInputDisplay.setText(digit);
            } else {
                if (!currentInput.isEmpty()) {
                    char lastChar = currentInput.charAt(currentInput.length() - 1);
                    if (lastChar == ')' || lastChar == 'π' || lastChar == '%' || lastChar == '!') {
                        textInputDisplay.append("×");
                    }
                }
                textInputDisplay.append(digit);
            }
        }
        updateLivePreview();
    }

    private void onOperatorPressed(String operator) {
        clearErrorFromPreview();
        isResultCurrentlyDisplayed = false;
        String currentInput = textInputDisplay.getText().toString();

        if (currentInput.isEmpty()) {
            if (operator.equals("-") || operator.equals("+")) {
                textInputDisplay.append(operator);
            }
            updateLivePreview();
            return;
        }

        char lastChar = currentInput.charAt(currentInput.length() - 1);
        boolean isLastCharOperator = "+-×÷^".indexOf(lastChar) != -1;
        boolean isLastCharOpenBracket = lastChar == '(';
        if (isLastCharOperator) {
            if ( (operator.equals("-") || operator.equals("+")) && (lastChar == '×' || lastChar == '÷' || lastChar == '^') ){
                textInputDisplay.append(operator);
            } else {
                textInputDisplay.setText(currentInput.substring(0, currentInput.length() - 1) + operator);
            }
        } else if (isLastCharOpenBracket && (operator.equals("-") || operator.equals("+"))) {
            textInputDisplay.append(operator);
        } else if (!isLastCharOpenBracket) {
            textInputDisplay.append(operator);
        }
        updateLivePreview();
    }

    private void onFunctionPressed(String functionSymbol) {
        clearErrorFromPreview();
        if (isResultCurrentlyDisplayed) {
            textInputDisplay.setText("");
            isResultCurrentlyDisplayed = false;
        }

        String currentInput = textInputDisplay.getText().toString();
        char lastChar = currentInput.isEmpty() ? ' ' : currentInput.charAt(currentInput.length() - 1);
        boolean isLastCharFactor = Character.isDigit(lastChar) || lastChar == ')' || lastChar == 'π' || lastChar == '!' || lastChar == '%';

        if (functionSymbol.equals("√")) {
            if (isLastCharFactor) textInputDisplay.append("×");
            textInputDisplay.append("√");
        } else if (functionSymbol.equals("π")) {
            if (isLastCharFactor || Character.isDigit(lastChar)) textInputDisplay.append("×");
            textInputDisplay.append("π");
        } else if (functionSymbol.equals("!") || functionSymbol.equals("%")) {
            if (!currentInput.isEmpty() && (Character.isDigit(lastChar) || lastChar == ')' || lastChar == 'π')) {
                textInputDisplay.append(functionSymbol);
            }
        }
        updateLivePreview();
    }

    private void onDecimalPressed() {
        clearErrorFromPreview();
        String decimalSeparator = ".";

        if (isResultCurrentlyDisplayed) {
            textInputDisplay.setText("0" + decimalSeparator);
            isResultCurrentlyDisplayed = false;
            updateLivePreview();
            return;
        }

        String currentInput = textInputDisplay.getText().toString();
        if (currentInput.isEmpty()) {
            textInputDisplay.append("0" + decimalSeparator);
            updateLivePreview();
            return;
        }

        int i = currentInput.length() - 1;
        boolean segmentHasDecimal = false;
        while (i >= 0) {
            char c = currentInput.charAt(i);
            if (c == '.') {
                segmentHasDecimal = true;
                break;
            }
            if ("+-×÷^√()π!%".indexOf(c) != -1) {
                break;
            }
            i--;
        }

        if (!segmentHasDecimal) {
            char lastChar = currentInput.charAt(currentInput.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == 'π' || lastChar == ')') {
                textInputDisplay.append(decimalSeparator);
            } else if ("+-×÷^√(".indexOf(lastChar) != -1) {
                textInputDisplay.append("0" + decimalSeparator);
            }
        }
        updateLivePreview();
    }

    private void onBracketsPressed() {
        clearErrorFromPreview();
        if (isResultCurrentlyDisplayed) {
            textInputDisplay.setText("(");
            isResultCurrentlyDisplayed = false;
            updateLivePreview();
            return;
        }

        String currentInput = textInputDisplay.getText().toString();
        int openBracketsCount = 0;
        int closeBracketsCount = 0;
        for (char c : currentInput.toCharArray()) {
            if (c == '(') openBracketsCount++;
            else if (c == ')') closeBracketsCount++;
        }

        char lastChar = currentInput.isEmpty() ? ' ' : currentInput.charAt(currentInput.length() - 1);

        boolean isLastCharFactor = Character.isDigit(lastChar) || lastChar == ')' || lastChar == 'π' || lastChar == '!' || lastChar == '%';

        if (isLastCharFactor) {
            if (openBracketsCount > closeBracketsCount) {
                textInputDisplay.append(")");
            } else {
                textInputDisplay.append("×(");
            }
        } else if (lastChar == '(' || currentInput.isEmpty() || "+-×÷^√".indexOf(lastChar) != -1) {
            textInputDisplay.append("(");
        } else {
            textInputDisplay.append("(");
        }
        updateLivePreview();
    }

    private void onBackspacePressed() {
        clearErrorFromPreview();
        String currentInput = textInputDisplay.getText().toString();
        if (!currentInput.isEmpty()) {
            textInputDisplay.setText(currentInput.substring(0, currentInput.length() - 1));
            if (isResultCurrentlyDisplayed) {
                isResultCurrentlyDisplayed = false;
            }

        }
        updateLivePreview();
    }

    private void onClearAllPressed() {
        textInputDisplay.setText("");
        textExpressionPreviewDisplay.setText("");
        isResultCurrentlyDisplayed = false;
    }

    @SuppressLint({"SetTextI18n"})
    private void onEqualsPressed() {
        String expression = textInputDisplay.getText().toString();
        if (expression.isEmpty() || isResultCurrentlyDisplayed) {
            return;
        }

        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            displayErrorInPreview(TEXT_TOO_LONG_MSG);
            return;
        }

        char lastChar = expression.charAt(expression.length() - 1);
        if ("+-×÷^√(".indexOf(lastChar) != -1) {
            displayErrorInPreview(INCOMPLETE_EXPRESSION_MSG);
            return;
        }

        int openBrackets = 0;
        int closeBrackets = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') openBrackets++;
            else if (c == ')') closeBrackets++;
        }
        if (openBrackets != closeBrackets) {
            displayErrorInPreview(UNBALANCED_BRACKETS_MSG);
            return;
        }

        try {
            RecursiveParser parser = new RecursiveParser(expression);
            double resultValue = parser.parse();

            if (Double.isNaN(resultValue) || Double.isInfinite(resultValue)) {
                throw new ArithmeticException("Result is NaN or Infinite");
            }

            String resultString = formatResultNumber(resultValue);
            addToCalculationHistory(expression, resultString);

            textInputDisplay.setText(resultString);
            isResultCurrentlyDisplayed = true;

        } catch (RecursiveParser.ParseException e) {
            displayErrorInPreview(GENERIC_ERROR_MESSAGE);
            android.util.Log.e("Calculator", "Parse Error: " + e.getMessage());
        } catch (ArithmeticException e) {
            String message = e.getMessage();
            if (message != null && (message.equals("Division by zero") || message.contains("Factorial") || message.contains("Square root"))) {
                displayErrorInPreview(message);
            } else {
                displayErrorInPreview(GENERIC_ERROR_MESSAGE);
            }
            android.util.Log.e("Calculator", "Arithmetic Error: " + e.getMessage());
        } catch (Exception e) {
            displayErrorInPreview(GENERIC_ERROR_MESSAGE);
            android.util.Log.e("Calculator", "Unexpected calculation error", e);
        }
    }

    @SuppressLint({"SetTextI18n"})
    private void updateLivePreview() {
        if (isResultCurrentlyDisplayed) {
            return;
        }

        String expression = textInputDisplay.getText().toString();

        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            displayErrorInPreview(TEXT_TOO_LONG_MSG);
            return;
        } else {
            if (TEXT_TOO_LONG_MSG.equals(textExpressionPreviewDisplay.getText().toString())) {
                textExpressionPreviewDisplay.setText("");
            }
        }


        if (expression.isEmpty()) {
            textExpressionPreviewDisplay.setText("");
            return;
        }

        char lastChar = expression.charAt(expression.length() - 1);
        if ("+-×÷^√(".indexOf(lastChar) != -1) {
            textExpressionPreviewDisplay.setText("");
            return;
        }
        int openBrackets = 0;
        int closeBrackets = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') openBrackets++;
            else if (c == ')') closeBrackets++;
        }
        if (openBrackets != closeBrackets) {
            textExpressionPreviewDisplay.setText("");
            return;
        }

        try {
            RecursiveParser previewParser = new RecursiveParser(expression);
            double resultValue = previewParser.parse();

            if (Double.isNaN(resultValue) || Double.isInfinite(resultValue)) {
                textExpressionPreviewDisplay.setText("");
                return;
            }
            textExpressionPreviewDisplay.setText(formatResultNumber(resultValue));
        } catch (Exception e) {
            textExpressionPreviewDisplay.setText("");
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatResultNumber(double result) {
        if (Math.abs(result) >= 1e15 || (Math.abs(result) < 1e-6 && result != 0) ) {
            return String.format(Locale.US, "%.6e", result);
        } else if (Math.abs(result - Math.round(result)) < 1e-9) {
            return String.format(Locale.US, "%d", (long) Math.round(result));
        } else {
            String formatted = String.format(Locale.US, "%.9f", result).replaceAll("\\.?0+$", "");
            if (formatted.endsWith(".")) {
                formatted = formatted.substring(0, formatted.length() -1);
            }
            return formatted;
        }
    }

    private void addToCalculationHistory(String expression, String result) {
        String historyEntry = expression + " = " + result;
        if (calculationHistoryList.isEmpty() || !calculationHistoryList.get(calculationHistoryList.size() - 1).equals(historyEntry)) {
            calculationHistoryList.add(historyEntry);
            if (calculationHistoryList.size() > MAX_HISTORY_ITEMS) {
                calculationHistoryList.remove(0);
            }
        }
    }

    private void showHistoryDialog() {
        if (calculationHistoryList.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.history)
                    .setMessage("Calculation history is empty.")
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }

        List<String> reversedHistory = new ArrayList<>(calculationHistoryList);
        Collections.reverse(reversedHistory);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                reversedHistory);

        new AlertDialog.Builder(this)
                .setTitle(R.string.history)
                .setAdapter(adapter, (dialog, which) -> {
                    String selectedEntry = reversedHistory.get(which);
                    try {
                        String expressionToInsert = selectedEntry.substring(0, selectedEntry.indexOf(" ="));
                        textInputDisplay.setText(expressionToInsert);
                        isResultCurrentlyDisplayed = false;
                        updateLivePreview();
                        dialog.dismiss();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to insert entry", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("Close", null)
                .setNegativeButton("Clear", (dialog, which) -> {
                    promptToClearHistory();
                })
                .show();
    }

    private void promptToClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (d, w) -> {
                    calculationHistoryList.clear();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void displayErrorInPreview(String message) {
        textExpressionPreviewDisplay.setText(message);
        isResultCurrentlyDisplayed = false;
    }

    private void clearErrorFromPreview() {
        String currentPreviewText = textExpressionPreviewDisplay.getText().toString();
        if (GENERIC_ERROR_MESSAGE.equals(currentPreviewText) ||
                INCOMPLETE_EXPRESSION_MSG.equals(currentPreviewText) ||
                UNBALANCED_BRACKETS_MSG.equals(currentPreviewText) ||
                TEXT_TOO_LONG_MSG.equals(currentPreviewText)) {
            textExpressionPreviewDisplay.setText("");
        }
    }
}