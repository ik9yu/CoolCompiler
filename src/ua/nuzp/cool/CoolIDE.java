package ua.nuzp.cool;

import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class CoolIDE extends JFrame {

    // UI компоненти
    private JTextArea inputArea;
    private JTextArea errorArea;
    private DefaultTableModel tokensModel;
    private JPanel treePanel;

    public CoolIDE() {
        super("Cool Compiler IDE");

        // Налаштування вікна
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setupUI();
    }

    // Побудова інтерфейсу
    private void setupUI() {

        // Панель кнопок
        JToolBar toolBar = new JToolBar();
        JButton lexerButton = new JButton("1. Run Lexer");
        JButton parserButton = new JButton("2. Run Parser (Build Tree)");

        lexerButton.addActionListener(e -> runLexer());
        parserButton.addActionListener(e -> runParser());

        toolBar.add(lexerButton);
        toolBar.add(parserButton);
        add(toolBar, BorderLayout.NORTH);

        // Редактор коду
        inputArea = new JTextArea();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        inputArea.setText("class Main inherits IO {\n  main() : Object {\n    out_string(\"Hello world!\\n\")\n  };\n};");

        JScrollPane codeScroll = new JScrollPane(inputArea);
        codeScroll.setBorder(BorderFactory.createTitledBorder("Source Code"));

        // Вкладки результатів
        JTabbedPane tabbedPane = new JTabbedPane();

        // Таблиця токенів
        String[] cols = {"Token", "Value", "Pos"};
        tokensModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tokensModel);
        tabbedPane.addTab("Tokens (Lab 1)", new JScrollPane(table));

        // Панель дерева розбору
        treePanel = new JPanel(new BorderLayout());
        tabbedPane.addTab("Parse Tree (Lab 2)", new JScrollPane(treePanel));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, codeScroll, tabbedPane);
        split.setResizeWeight(0.4);

        // Консоль помилок
        errorArea = new JTextArea(5, 20);
        errorArea.setForeground(Color.RED);
        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setBorder(BorderFactory.createTitledBorder("Console / Errors"));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, split, errorScroll);
        mainSplit.setResizeWeight(0.8);

        add(mainSplit, BorderLayout.CENTER);
    }

    // Лексичний аналіз
    private void runLexer() {
        tokensModel.setRowCount(0);
        errorArea.setText("Lexer started...\n");

        String code = inputArea.getText();
        CoolLexer lexer = new CoolLexer(CharStreams.fromString(code));

        lexer.removeErrorListeners();
        lexer.addErrorListener(createErrorListener());

        List<? extends Token> tokens = lexer.getAllTokens();
        for (Token t : tokens) {
            String rule = CoolLexer.VOCABULARY.getSymbolicName(t.getType());
            tokensModel.addRow(new Object[]{rule, t.getText(), t.getLine() + ":" + t.getCharPositionInLine()});
        }

        if (errorArea.getText().equals("Lexer started...\n"))
            errorArea.append("Lexer finished successfully.\n");
    }

    // Синтаксичний аналіз + побудова дерева
    private void runParser() {
        errorArea.setText("Parser started...\n");
        treePanel.removeAll();

        String code = inputArea.getText();

        CoolLexer lexer = new CoolLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(createErrorListener());

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        CoolParser parser = new CoolParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(createErrorListener());

        ParseTree tree = parser.program();

        // Відобразити дерево якщо без помилок
        if (parser.getNumberOfSyntaxErrors() == 0) {
            TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
            viewer.setScale(1.5);
            treePanel.add(viewer, BorderLayout.CENTER);
            errorArea.append("Parsing completed. Tree built.\n");
        } else {
            errorArea.append("Parsing failed. Fix errors to see the tree.\n");
        }

        treePanel.revalidate();
        treePanel.repaint();
    }

    // Listener для виводу помилок
    private BaseErrorListener createErrorListener() {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg, RecognitionException e) {
                errorArea.append("Error at " + line + ":" + charPositionInLine + " -> " + msg + "\n");
            }
        };
    }

    // Точка входу
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CoolIDE().setVisible(true));
    }
}