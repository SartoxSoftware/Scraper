package open.nano.scraper;

import open.java.toolkit.files.Files;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

import static open.java.toolkit.swing.SwingHelper.*;

public class JFrameHelper
{
    public static JComboBox<Boolean> addSettingsComboBox(JDialog frame, boolean selectedItem, int x, int y, int width, int height)
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

    public static JDialog createNewDialog(Dimension dimension, String title)
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
                Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
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
                area.setText(Files.readFile(path));

                Files.writeLines("settings/" + title.toLowerCase() + ".txt", area.getText().split("\\r?\\n"), false);
            }
        });

        return dialog;
    }
}
