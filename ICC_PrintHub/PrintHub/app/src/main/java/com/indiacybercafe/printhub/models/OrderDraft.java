package com.indiacybercafe.printhub.models;

import java.io.Serializable;
import java.util.List;

public class OrderDraft implements Serializable {
    private String uid;
    private List<PrintSet> printSets;
    private AddressModel address;
    private String paymentId;
    private double totalAmount;
    private double deliveryCharge;
    private double printingCharges;
    private String gstType;

    public OrderDraft() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public List<PrintSet> getPrintSets() { return printSets; }
    public void setPrintSets(List<PrintSet> printSets) { this.printSets = printSets; }
    public AddressModel getAddress() { return address; }
    public void setAddress(AddressModel address) { this.address = address; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(double deliveryCharge) { this.deliveryCharge = deliveryCharge; }
    public double getPrintingCharges() { return printingCharges; }
    public void setPrintingCharges(double printingCharges) { this.printingCharges = printingCharges; }
    public String getGstType() { return gstType; }
    public void setGstType(String gstType) { this.gstType = gstType; }
}
