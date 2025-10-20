import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvExporter {

    public static void exportToCsv(JTable table, Component parent) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent, "The table is empty. Nothing to export.", "Export Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save as CSV");
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".csv") || f.isDirectory();
            }
            @Override
            public String getDescription() {
                return "CSV Files (*.csv)";
            }
        });
        fileChooser.setAcceptAllFileFilterUsed(false);

        int userSelection = fileChooser.showSaveDialog(parent);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (FileWriter csvWriter = new FileWriter(fileToSave)) {
                // Write headers
                for (int i = 0; i < table.getColumnCount(); i++) {
                    csvWriter.append(escapeCsv(table.getColumnName(i), false)); // Headers are always text
                    if (i < table.getColumnCount() - 1) {
                        csvWriter.append(",");
                    }
                }
                csvWriter.append("\n");

                // Write data rows
                for (int row = 0; row < table.getRowCount(); row++) {
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        String columnName = table.getColumnName(col);
                        Object cellData = table.getValueAt(row, col);
                        boolean isChoiceCode = columnName.equalsIgnoreCase("ChoiceCode") || columnName.equalsIgnoreCase("Choice_Code") || columnName.equalsIgnoreCase("Choice Code");

                        csvWriter.append(escapeCsv(cellData == null ? "" : cellData.toString(), isChoiceCode));

                        if (col < table.getColumnCount() - 1) {
                            csvWriter.append(",");
                        }
                    }
                    csvWriter.append("\n");
                }

                JOptionPane.showMessageDialog(parent, "✅ Data exported successfully to:\n" + fileToSave.getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "❌ Error exporting data: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Escapes special characters for CSV format.
     * @param data The string data to escape.
     * @param isIdentifier A flag to indicate if this is a special identifier column.
     * @return The CSV-safe string.
     */
    private static String escapeCsv(String data, boolean isIdentifier) {
        // --- THIS IS THE NEW LOGIC ---
        // If it's an identifier column like ChoiceCode, wrap it to force Excel to treat it as text.
        if (isIdentifier) {
            return "=\"" + data + "\"";
        }
        // --- END OF NEW LOGIC ---

        // General handling for all other columns
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }
}