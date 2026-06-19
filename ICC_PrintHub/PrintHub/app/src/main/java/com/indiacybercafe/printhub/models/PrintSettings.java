package com.indiacybercafe.printhub.models;

import java.io.Serializable;

public class PrintSettings implements Serializable {
    private String colorMode; // Black & White, Color
    private String paperQuality; // Light, Medium, High
    private int copies = 1;
    private String binding; // Loose Paper, Stapled, Spiral
    private double price;

    public PrintSettings() {}

    // Getters and Setters
    public String getColorMode() { return colorMode; }
    public void setColorMode(String colorMode) { this.colorMode = colorMode; }
    public String getPaperQuality() { return paperQuality; }
    public void setPaperQuality(String paperQuality) { this.paperQuality = paperQuality; }
    public int getCopies() { return copies; }
    public void setCopies(int copies) { this.copies = copies; }
    public String getBinding() { return binding; }
    public void setBinding(String binding) { this.binding = binding; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
