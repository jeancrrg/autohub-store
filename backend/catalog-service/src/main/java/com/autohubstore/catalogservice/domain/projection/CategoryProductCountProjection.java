package com.autohubstore.catalogservice.domain.projection;

import java.util.UUID;

public interface CategoryProductCountProjection {

    UUID getCategoryId();

    Long getProductCount();

}
