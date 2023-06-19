package com.example.batch.entity;

import lombok.Getter;

@Getter
public class PopularProduct {
    private Long id;
    private String productName;
    private int price;

    public PopularProduct(){}

    public PopularProduct(Product product) {
        this.id = product.getId();
        this.productName = product.getProductName();
        this.price = product.getPrice();
    }
}
