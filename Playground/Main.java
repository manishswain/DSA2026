package Playground;

import java.util.List;

class ReportExporter {
    public String exportCsv(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            sb.append(String.join("##", rows.get(i)));
            if (i < rows.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}

// Test
public class Main {
    public static void main(String[] args) {
        ReportExporter exporter = new ReportExporter();
        List<String[]> data = List.of(
                new String[] { "Name", "Age", "City" },
                new String[] { "Alice", "30", "New York" },
                new String[] { "Bob", "25", "London" });
        System.out.println(exporter.exportCsv(data));
    }
}