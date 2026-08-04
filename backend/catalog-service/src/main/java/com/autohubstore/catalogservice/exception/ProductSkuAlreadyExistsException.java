package com.autohubstore.catalogservice.exception;

public class ProductSkuAlreadyExistsException extends RuntimeException {

    public ProductSkuAlreadyExistsException(String sku) {
        super("SKU de produto já cadastrado: " + sku);
    }

}
