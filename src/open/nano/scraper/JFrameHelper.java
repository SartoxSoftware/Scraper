package open.nano.scraper;

import open.java.toolkit.console.Console;
import open.java.toolkit.console.LogType;
import open.java.toolkit.console.ansi.Foreground;
import open.java.toolkit.files.Files;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.IOException;

public class JFrameHelper
{
    public static JButton addButton(JFrame frame, String text, int x, int y, int width, int height)
    {
        JButton btn = new JButton();
        btn.setText(text);
        btn.setBounds(x, y, width, height);
        btn.setVisible(true);
        frame.add(btn);
        return btn;
    }

    public static JTextArea addTextBox(JFrame frame, String text, boolean editable, int x, int y, int width, int height)
    {
        JTextArea box = new JTextArea();
        box.setText(text);
        box.setBounds(x, y, width, height);
        box.setVisible(true);
        box.setEditable(editable);
        frame.add(box);
        return box;
    }

    public static JLabel addLabel(JFrame frame, String text, int x, int y, int width, int height)
    {
        JLabel label = new JLabel();
        label.setText(text);
        label.setBounds(x, y, width, height);
        label.setVisible(true);
        frame.add(label);
        return label;
    }

    public static JButton addButton(JDialog frame, String text, int x, int y, int width, int height)
    {
        JButton btn = new JButton();
        btn.setText(text);
        btn.setBounds(x, y, width, height);
        btn.setVisible(true);
        frame.add(btn);
        return btn;
    }

    public static JTextField addTextBox(JDialog frame, String text, boolean editable, int x, int y, int width, int height)
    {
        JTextField box = new JTextField();
        box.setText(text);
        box.setBounds(x, y, width, height);
        box.setVisible(true);
        box.setEditable(editable);
        frame.add(box);
        return box;
    }

    public static JTextArea addTextArea(JDialog frame, String text, boolean editable, int x, int y, int width, int height)
    {
        JTextArea box = new JTextArea();
        box.setText(text);
        box.setBounds(x, y, width, height);
        box.setVisible(true);
        box.setEditable(editable);
        frame.add(box);
        return box;
    }

    public static JLabel addLabel(JDialog frame, String text, int x, int y, int width, int height)
    {
        JLabel label = new JLabel();
        label.setText(text);
        label.setBounds(x, y, width, height);
        label.setVisible(true);
        frame.add(label);
        return label;
    }

    public static JComboBox<Boolean> addComboBox(JDialog frame, boolean selectedItem, int x, int y, int width, int height)
    {
        JComboBox<Boolean> box = new JComboBox<>();
        box.addItem(true);
        box.addItem(false);
        box.setBounds(x, y, width, height);
        box.setSelectedItem(selectedItem);
        box.setVisible(true);
        frame.add(box);
        return box;
    }

    public static JComboBox<String> addThemeComboBox(JDialog frame, String selectedItem, int x, int y, int width, int height)
    {
        JComboBox<String> box = new JComboBox<>();
        box.addItem("default");
        box.addItem("dark");
        box.setBounds(x, y, width, height);
        box.setSelectedItem(selectedItem);
        box.setVisible(true);
        frame.add(box);
        return box;
    }

    public static Component addScrollPane(JFrame frame, Component c)
    {
        JScrollPane scroll = new JScrollPane(c);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setBounds(c.getBounds());
        scroll.setVisible(true);
        frame.add(scroll);
        return c;
    }

    public static Component addScrollPane(JDialog frame, Component c)
    {
        JScrollPane scroll = new JScrollPane(c);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setBounds(c.getBounds());
        scroll.setVisible(true);
        frame.add(scroll);
        return c;
    }

    public static JDialog createNewDialog(Dimension dimension, String title) throws IOException
    {
        JDialog dialog = new JDialog();
        dialog.setMinimumSize(dimension);
        dialog.setMaximumSize(dimension);
        dialog.setPreferredSize(dimension);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setLayout(null);
        dialog.setTitle(title);

        String text = Files.readFile("settings/" + title.toLowerCase() + ".txt");
        JTextArea area = (JTextArea) addScrollPane(dialog, addTextArea(dialog, text, true, 0, 0, dimension.width, dimension.height - 100));
        area.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                try
                {
                    Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
                } catch (IOException ignored) {}
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                try
                {
                    Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
                } catch (IOException ignored) {}
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                try
                {
                    Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
                } catch (IOException ignored) {}
            }
        });
        JButton load = addButton(dialog, "Load from file", 0, dimension.height - 100, dimension.width, 63);

        load.addActionListener(e ->
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Load your " + title.toLowerCase());

            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION)
            {
                String path = chooser.getSelectedFile().getAbsolutePath();

                try
                {
                    area.setText(Files.readFile(path));
                    Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
                } catch (IOException ignored) {}
            }
        });

        return dialog;
    }

    public static void configureDarkTheme(JFrame frame)
    {
        UIManager.put("control", new Color(60, 60, 60));
        frame.getContentPane().setBackground((Color) UIManager.get("control"));
        UIManager.put("info", UIManager.get("control"));
        UIManager.put("nimbusBase", new Color(18, 30, 49));
        UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
        UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
        UIManager.put("nimbusFocus", new Color(115,164,209));
        UIManager.put("nimbusGreen", new Color(176,179,50));
        UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
        UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
        UIManager.put("nimbusOrange", new Color(191,98,4));
        UIManager.put("nimbusRed", new Color(169,46,34));
        UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
        UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
        UIManager.put("text", new Color(230, 230, 230));
    }
}
