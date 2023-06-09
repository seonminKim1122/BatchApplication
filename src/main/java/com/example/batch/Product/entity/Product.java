package com.example.batch.Product.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "amount")
    private int amount;

    @Column(name = "categorya")
    private String categoryA;

    @Column(name = "categoryb")
    private String categoryB;

    @Column(name = "price")
    private int price;

    @Column(name = "product_name")
    private String productName;
}
