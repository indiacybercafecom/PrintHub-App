package com.indiacybercafe.printhub.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PrintSet implements Serializable {
    private String id;
    private String name;
    private List<FileModel> files = new ArrayList<>();
    private PrintSettings settings;
    private double totalPrice;

    public PrintSet() {}

    public PrintSet(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<FileModel> getFiles() { return files; }
    public void setFiles(List<FileModel> files) { this.files = files; }
    public PrintSettings getSettings() { return settings; }
    public void setSettings(PrintSettings settings) { this.settings = settings; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
