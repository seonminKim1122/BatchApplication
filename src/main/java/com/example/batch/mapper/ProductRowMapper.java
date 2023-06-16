package com.example.batch.mapper;

import com.example.batch.entity.Product;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRowMapper implements RowMapper<Product> {
    @Override
    public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("product_id"));
        product.setAmount(rs.getInt("amount"));
        product.setCategoryA(rs.getString("categorya"));
        product.setCategoryB(rs.getString("categoryb"));
        product.setPrice(rs.getInt("price"));
        product.setProductName(rs.getString("product_name"));
        return product;
    }
}
