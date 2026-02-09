package ua.nuzp.cool;

import org.antlr.v4.runtime.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CoolIDE extends JFrame {

    // UI компоненти
    private JTextArea inputArea;
    private JTextArea errorArea;
    private DefaultTableModel tableModel;

    public CoolIDE() {
        super("Cool Compiler IDE");

        // Налаштування вікна
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Кнопка запуску лексера
        JButton runButton = new JButton("▶ Run Lexer");
        runButton.addActionListener(e -> runAnalysis());

        JToolBar toolBar = new JToolBar();
        toolBar.add(runButton);
        add(toolBar, BorderLayout.NORTH);

        // Поле введення коду
        inputArea = new JTextArea();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(inputArea);

        // Таблиця токенів
        String[] cols = {"Token", "Value", "Pos", "Rule"};
        tableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScroll, tableScroll);
        split.setResizeWeight(0.5);

        // Поле помилок
        errorArea = new JTextArea(5, 20);
        errorArea.setForeground(Color.RED);
        JScrollPane errorScroll = new JScrollPane(errorArea);

        add(split, BorderLayout.CENTER);
        add(errorScroll, BorderLayout.SOUTH);
    }

    // Запуск лексичного аналізу
    private void runAnalysis() {
        tableModel.setRowCount(0);
        errorArea.setText("");

        String code = inputArea.getText();
        if (code.isEmpty()) return;

        // Створення лексера
        CoolLexer lexer = new CoolLexer(CharStreams.fromString(code));

        // Обробка помилок
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg, RecognitionException e) {
                errorArea.append("Error (Line " + line + ":" + charPositionInLine + "): " + msg + "\n");
            }
        });

        // Заповнення таблиці токенами
        List<? extends Token> tokens = lexer.getAllTokens();
        for (Token t : tokens) {
            String rule = CoolLexer.VOCABULARY.getSymbolicName(t.getType());
            tableModel.addRow(new Object[]{rule, t.getText(),
                    t.getLine() + ":" + t.getCharPositionInLine(), t.getType()});
        }
    }

    // Точка входу
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CoolIDE().setVisible(true));
    }
}