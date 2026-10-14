package com.junia.portfolio.model;

public interface Taxable {
    double calculateTaxableGain(double sellingPrice, double unitsSold);
    double getTaxRate();
}
