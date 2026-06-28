package com.indiacybercafe.printhub.models;

import java.io.Serializable;
import java.util.List;

public class OrderModel implements Serializable {
    private String orderId;
    private String uid;
    private List<PrintSet> printSets;
    private AddressModel address;
    private String paymentId;
    private String paymentStatus;
    private String paymentMethod; // "razorpay", "cod"
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private boolean codOrder;
    private String status; // Pending, Accepted, Printing, Packed, Shipped, Delivered, Cancelled, Completed
    private double totalAmount;
    private double deliveryCharge;
    private double printingCharges;
    private String gstType;
    private long createdAt;
    private long updatedAt;

    public OrderModel() {}

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public List<PrintSet> getPrintSets() { return printSets; }
    public void setPrintSets(List<PrintSet> printSets) { this.printSets = printSets; }
    public AddressModel getAddress() { return address; }
    public void setAddress(AddressModel address) { this.address = address; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public boolean isCodOrder() { return codOrder; }
    public void setCodOrder(boolean codOrder) { this.codOrder = codOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(double deliveryCharge) { this.deliveryCharge = deliveryCharge; }
    public double getPrintingCharges() { return printingCharges; }
    public void setPrintingCharges(double printingCharges) { this.printingCharges = printingCharges; }
    public String getGstType() { return gstType; }
    public void setGstType(String gstType) { this.gstType = gstType; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
